#!/usr/bin/env python3
"""Generate PCGen LST files from pcfinder-csr creatures.json.

Supports two modes:
  1. Legacy bestiary mode:  --bestiary 5 6
  2. Generic source mode:   --sources "Monster Codex" "Occult Bestiary" --comparison report.json
     Or generate all:       --all --comparison scripts/creature_comparison.json

Usage:
    # Legacy (Bestiary 5/6 from pcfinder source):
    python3 scripts/generate_bestiary.py --source creatures.json --bestiary 5 6

    # From comparison report (all missing creatures):
    python3 scripts/generate_bestiary.py --source creatures.json --all --comparison scripts/creature_comparison.json

    # Specific sources from comparison report:
    python3 scripts/generate_bestiary.py --source creatures.json \\
        --sources "Monster Codex" "Occult Bestiary" --comparison scripts/creature_comparison.json
"""

import argparse
import json
import math
import os
import re
import sys
from collections import defaultdict
from pathlib import Path

# ─── Constants ───────────────────────────────────────────────────────────────

SIZE_MAP = {
    'Fine': 'F', 'Diminutive': 'D', 'Tiny': 'T', 'Small': 'S',
    'Medium': 'M', 'Large': 'L', 'Huge': 'H', 'Gargantuan': 'G', 'Colossal': 'C',
}

SIZE_AC_MOD = {
    'Fine': 8, 'Diminutive': 4, 'Tiny': 2, 'Small': 1,
    'Medium': 0, 'Large': -1, 'Huge': -2, 'Gargantuan': -4, 'Colossal': -8,
}

SIZE_REACH = {
    'Fine': 0, 'Diminutive': 0, 'Tiny': 0, 'Small': 5,
    'Medium': 5, 'Large': 10, 'Huge': 15, 'Gargantuan': 20, 'Colossal': 30,
}

MANEUVERABILITY_MAP = {
    'clumsy': 1, 'poor': 2, 'average': 3, 'good': 4, 'perfect': 5,
}

ALIGNMENT_KIT_MAP = {
    'LG': 'LG', 'NG': 'NG', 'CG': 'CG',
    'LN': 'LN', 'N': 'TN', 'CN': 'CN',
    'LE': 'LE', 'NE': 'NE', 'CE': 'CE',
}

STAT_NAMES = ['STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA']
STAT_KEYS = ['str', 'dex', 'con', 'int', 'wis', 'cha']

# Type to MONSTERCLASS (simple 1:1 mappings)
SIMPLE_TYPE_MAP = {
    'aberration': ('Aberration', 'Aberration'),
    'animal': ('Animal', 'Animal'),
    'construct': ('Construct', 'Construct'),
    'dragon': ('Dragon', 'Dragon'),
    'fey': ('Fey', 'Fey'),
    'magical-beast': ('Magical Beast', 'Magical Beast'),
    'monstrous-humanoid': ('Monstrous Humanoid', 'Monstrous Humanoid'),
    'ooze': ('Ooze', 'Ooze'),
    'plant': ('Plant', 'Plant'),
    'undead': ('Undead', 'Undead'),
    'vermin': ('Vermin', 'Vermin'),
}

# Default LEGS/HANDS by creature type
TYPE_LEGS_HANDS = {
    'aberration': (0, 0),
    'animal': (4, 0),
    'construct': (2, 2),
    'dragon': (4, 0),
    'fey': (2, 2),
    'humanoid': (2, 2),
    'magical-beast': (4, 0),
    'monstrous-humanoid': (2, 2),
    'ooze': (0, 0),
    'outsider': (2, 2),
    'plant': (0, 0),
    'undead': (2, 2),
    'vermin': (6, 0),
}

# Natural weapon name → (PCGen display name, damage types)
NATURAL_WEAPON_MAP = {
    'bite': ('Bite', 'Bludgeoning.Piercing.Slashing'),
    'bites': ('Bite', 'Bludgeoning.Piercing.Slashing'),
    'claw': ('Claw', 'Slashing'),
    'claws': ('Claw', 'Slashing'),
    'slam': ('Slam', 'Bludgeoning'),
    'slams': ('Slam', 'Bludgeoning'),
    'gore': ('Gore', 'Piercing'),
    'tail slap': ('Tail Slap', 'Bludgeoning'),
    'tail': ('Tail', 'Bludgeoning'),
    'sting': ('Sting', 'Piercing'),
    'stings': ('Sting', 'Piercing'),
    'wing': ('Wing', 'Bludgeoning'),
    'wings': ('Wing', 'Bludgeoning'),
    'hoof': ('Hoof', 'Bludgeoning'),
    'hooves': ('Hoof', 'Bludgeoning'),
    'tentacle': ('Tentacle', 'Bludgeoning'),
    'tentacles': ('Tentacle', 'Bludgeoning'),
    'talon': ('Talons', 'Slashing'),
    'talons': ('Talons', 'Slashing'),
    'tongue': ('Tongue', 'Bludgeoning'),
    'pincer': ('Pincer', 'Bludgeoning'),
    'pincers': ('Pincer', 'Bludgeoning'),
    'touch': ('Touch', 'Bludgeoning'),
    'incorporeal bite': ('Incorporeal Bite', 'Bludgeoning.Piercing.Slashing'),
    'incorporeal touch': ('Incorporeal Touch', 'Bludgeoning'),
    'incorporeal claw': ('Incorporeal Claw', 'Slashing'),
    'incorporeal claws': ('Incorporeal Claw', 'Slashing'),
    'slam': ('Slam', 'Bludgeoning'),
}

# Standard PCGen immunity ability names
IMMUNITY_MAP = {
    'acid': 'Immunity to Acid',
    'cold': 'Immunity to Cold',
    'electricity': 'Immunity to Electricity',
    'fire': 'Immunity to Fire',
    'sonic': 'Immunity to Sonic',
    'poison': 'Immunity to Poison',
    'disease': 'Immunity to Disease',
    'sleep': 'Immunity to Sleep',
    'paralysis': 'Immunity to Paralysis',
    'fear': 'Immunity to Fear',
    'stun': 'Immunity to Stunning',
    'stunning': 'Immunity to Stunning',
    'petrification': 'Immunity to Petrification',
    'bleed': 'Immunity to Bleed',
    'charm': 'Immunity to Charm',
    'compulsion': 'Immunity to Compulsion',
    'death effects': 'Immunity to Death Effects',
    'energy drain': 'Immunity to Energy Drain',
    'exhaustion': 'Immunity to Exhaustion',
    'fatigue': 'Immunity to Fatigue',
    'negative energy': 'Immunity to Negative Energy',
    'ability damage': 'Immunity to Ability Damage',
    'ability drain': 'Immunity to Ability Drain',
    'mind-affecting effects': 'Immunity to Mind-Affecting Effects',
    'mind-affecting': 'Immunity to Mind-Affecting Effects',
}

# Creatures already defined as companions/familiars/PC races in existing B5 LST files
B5_EXCLUDE_NAMES = {
    # Companions
    'cameroceras', 'ceratosaurus', 'chalicotherium', 'digmaul', 'frog father',
    'frog, goliath', 'goliath frog', 'kaprosuchus', 'megaprimatus', 'moa',
    'narwhal', 'plesiosaurus', 'polar bear', 'therizinosaurus', 'troodon',
    'blue whale', 'whale, blue', 'uintatherium', 'wolliped',
    # Familiars
    'chicken', 'flying fox', 'penguin', 'red panda', 'seal', 'trilobite',
    'clockwork familiar', 'liminal sprite', 'esipil', 'xiao',
    'aether wysp', 'air wysp', 'earth wysp', 'fire wysp', 'water wysp',
    'wysp, aether', 'wysp, air', 'wysp, earth', 'wysp, fire', 'wysp, water',
    # PC races
    'android', 'astomoi', 'caligni', 'deep one hybrid', 'ghoran',
    'orang-pendak', 'reptoid', 'shabti', 'skinwalker',
}

B6_EXCLUDE_NAMES = {
    # Companions
    'amargasaurus', 'brontotherium', 'deinotherium', 'devil monkey',
    'dunkleosteus', 'elasmotherium', 'giganotosaurus', 'kentrosaurus',
    'mokele-mbembe', 'quetzalcoatlus', 'giant raven', 'raven, giant',
    'titanoboa',
    # Familiars
    'coral capuchin', 'mockingfey',
    # PC races
    'monkey goblin',
}

BESTIARY_CONFIG = {
    5: {
        'source_prefix': 'Bestiary 5',
        'source_short': 'B5',
        'source_web': 'http://paizo.com/products/btpy9g9x',
        'source_date': '2015-12',
        'exclude_names': B5_EXCLUDE_NAMES,
        'prefix': 'b5',
        'dir': 'bestiary_5',
    },
    6: {
        'source_prefix': 'Bestiary 6',
        'source_short': 'B6',
        'source_web': 'http://paizo.com/products/btpy9r1y',
        'source_date': '2017-05',
        'exclude_names': B6_EXCLUDE_NAMES,
        'prefix': 'b6',
        'dir': 'bestiary_6',
    },
}

# ─── Source directory mapping ───────────────────────────────────────────────
# Maps normalized source names → output directory config.
# 'existing': True means the dir and PCC already exist in pcgen.
# Sources not listed here get auto-generated config via build_source_config().

KNOWN_SOURCE_CONFIG = {
    'Bestiary 5': {
        'source_long': 'Bestiary 5', 'source_short': 'B5',
        'source_web': 'http://paizo.com/products/btpy9g9x',
        'source_date': '2015-12', 'prefix': 'b5',
        'subdir': 'paizo/roleplaying_game/bestiary_5',
        'exclude_names': B5_EXCLUDE_NAMES, 'existing': True,
    },
    'Bestiary 6': {
        'source_long': 'Bestiary 6', 'source_short': 'B6',
        'source_web': 'http://paizo.com/products/btpy9r1y',
        'source_date': '2017-05', 'prefix': 'b6',
        'subdir': 'paizo/roleplaying_game/bestiary_6',
        'exclude_names': B6_EXCLUDE_NAMES, 'existing': True,
    },
    'Monster Codex': {
        'source_long': 'Monster Codex', 'source_short': 'MC',
        'source_web': 'http://paizo.com/products/btpy9926',
        'source_date': '2014-11', 'prefix': 'mc',
        'subdir': 'paizo/roleplaying_game/monster_codex',
        'exclude_names': set(), 'existing': True,
    },
    'Mythic Adventures': {
        'source_long': 'Mythic Adventures', 'source_short': 'MA',
        'source_web': 'http://paizo.com/products/btpy8ywe',
        'source_date': '2013-08', 'prefix': 'ma',
        'subdir': 'paizo/roleplaying_game/mythic_adventures',
        'exclude_names': set(), 'existing': True,
    },
    'Occult Bestiary': {
        'source_long': 'Occult Bestiary', 'source_short': 'OB',
        'source_web': 'http://paizo.com/products/btpy9toq',
        'source_date': '2015-12', 'prefix': 'ob',
        'subdir': 'paizo/roleplaying_game/occult_bestiary',
        'exclude_names': set(), 'existing': False,
    },
    'Inner Sea Bestiary': {
        'source_long': 'Inner Sea Bestiary', 'source_short': 'ISB',
        'source_web': 'http://paizo.com/products/btpy8v2x',
        'source_date': '2013-06', 'prefix': 'isb',
        'subdir': 'paizo/campaign_setting/inner_sea_bestiary',
        'exclude_names': set(), 'existing': True,
    },
    'Inner Sea Monster Codex': {
        'source_long': 'Inner Sea Monster Codex', 'source_short': 'ISMC',
        'source_web': 'http://paizo.com/products/btpy9elc',
        'source_date': '2015-05', 'prefix': 'ismc',
        'subdir': 'paizo/campaign_setting/inner_sea_monster_codex',
        'exclude_names': set(), 'existing': False,
    },
    'Inner Sea Gods': {
        'source_long': 'Inner Sea Gods', 'source_short': 'ISG',
        'source_web': 'http://paizo.com/products/btpy94wj',
        'source_date': '2014-04', 'prefix': 'isg',
        'subdir': 'paizo/campaign_setting/inner_sea_gods',
        'exclude_names': set(), 'existing': True,
    },
    'Tome of Horrors Complete': {
        'source_long': 'Tome of Horrors Complete', 'source_short': 'ToHC',
        'source_web': 'https://froggodgames.com',
        'source_date': '2011-01', 'prefix': 'tohc',
        'subdir': 'frog_god_games/tome_of_horrors_complete',
        'exclude_names': set(), 'existing': False,
        'publisher_long': 'Frog God Games', 'publisher_short': 'FGG',
        'pcc_type': 'Frog God Games.Pathfinder RPG',
    },
    'Tome of Horrors 4': {
        'source_long': 'Tome of Horrors 4', 'source_short': 'ToH4',
        'source_web': 'https://froggodgames.com',
        'source_date': '2013-01', 'prefix': 'toh4',
        'subdir': 'frog_god_games/tome_of_horrors_4',
        'exclude_names': set(), 'existing': False,
        'publisher_long': 'Frog God Games', 'publisher_short': 'FGG',
        'pcc_type': 'Frog God Games.Pathfinder RPG',
    },
    'Inner Sea World Guide': {
        'source_long': 'Inner Sea World Guide', 'source_short': 'ISWG',
        'source_web': 'http://paizo.com/products/btpy8ief',
        'source_date': '2011-03', 'prefix': 'iswg',
        'subdir': 'paizo/campaign_setting/inner_sea_world_guide',
        'exclude_names': set(), 'existing': True,
    },
    'Isles Of The Shackles': {
        'source_long': 'Isles of the Shackles', 'source_short': 'IotS',
        'source_web': 'http://paizo.com/products/btpy8qzx',
        'source_date': '2012-08', 'prefix': 'iots',
        'subdir': 'paizo/campaign_setting/isles_of_the_shackles',
        'exclude_names': set(), 'existing': False,
    },
    'Numeria Land Of Fallen Stars': {
        'source_long': 'Numeria, Land of Fallen Stars', 'source_short': 'NLoFS',
        'source_web': 'http://paizo.com/products/btpy978l',
        'source_date': '2014-07', 'prefix': 'nlfs',
        'subdir': 'paizo/campaign_setting/numeria_land_of_fallen_stars',
        'exclude_names': set(), 'existing': False,
    },
    'The Worldwound': {
        'source_long': 'The Worldwound', 'source_short': 'TWW',
        'source_web': 'http://paizo.com/products/btpy8yvk',
        'source_date': '2013-07', 'prefix': 'tww',
        'subdir': 'paizo/campaign_setting/the_worldwound',
        'exclude_names': set(), 'existing': False,
    },
    'Horsemen Of The Apocalypse': {
        'source_long': 'Horsemen of the Apocalypse', 'source_short': 'HotA',
        'source_web': 'http://paizo.com/products/btpy8odg',
        'source_date': '2011-11', 'prefix': 'hota',
        'subdir': 'paizo/campaign_setting/horsemen_of_the_apocalypse',
        'exclude_names': set(), 'existing': False,
    },
    'Andoran Birthplace Of Freedom': {
        'source_long': 'Andoran, Birthplace of Freedom', 'source_short': 'ABoF',
        'source_web': 'http://paizo.com/products/btpy8bc3',
        'source_date': '2015-03', 'prefix': 'abof',
        'subdir': 'paizo/campaign_setting/andoran_birthplace_of_freedom',
        'exclude_names': set(), 'existing': True,
    },
    'Osirion, Legacy Of Pharaohs': {
        'source_long': 'Osirion, Legacy of Pharaohs', 'source_short': 'OLoP',
        'source_web': 'http://paizo.com/products/btpy93n8',
        'source_date': '2014-02', 'prefix': 'olop',
        'subdir': 'paizo/campaign_setting/osirion_legacy_of_pharaohs',
        'exclude_names': set(), 'existing': False,
    },
    'Magnimar City Of Monuments': {
        'source_long': 'Magnimar, City of Monuments', 'source_short': 'MCoM',
        'source_web': 'http://paizo.com/products/btpy8slp',
        'source_date': '2012-07', 'prefix': 'mcom',
        'subdir': 'paizo/campaign_setting/magnimar_city_of_monuments',
        'exclude_names': set(), 'existing': False,
    },
    'Irrisen Land Of Eternal Winter': {
        'source_long': 'Irrisen, Land of Eternal Winter', 'source_short': 'ILoEW',
        'source_web': 'http://paizo.com/products/btpy8w7f',
        'source_date': '2013-03', 'prefix': 'ilew',
        'subdir': 'paizo/campaign_setting/irrisen_land_of_eternal_winter',
        'exclude_names': set(), 'existing': False,
    },
    'Belkzen Hold Of The Orc Hordes': {
        'source_long': 'Belkzen, Hold of the Orc Hordes', 'source_short': 'BHoOH',
        'source_web': 'http://paizo.com/products/btpy97lw',
        'source_date': '2015-05', 'prefix': 'bhoh',
        'subdir': 'paizo/campaign_setting/belkzen_hold_of_the_orc_hordes',
        'exclude_names': set(), 'existing': False,
    },
    'Lands Of The Linnorm Kings': {
        'source_long': 'Lands of the Linnorm Kings', 'source_short': 'LotLK',
        'source_web': 'http://paizo.com/products/btpy8ode',
        'source_date': '2011-11', 'prefix': 'lotlk',
        'subdir': 'paizo/campaign_setting/lands_of_the_linnorm_kings',
        'exclude_names': set(), 'existing': False,
    },
    'Lost Kingdoms': {
        'source_long': 'Lost Kingdoms', 'source_short': 'LK',
        'source_web': 'http://paizo.com/products/btpy8sa7',
        'source_date': '2012-06', 'prefix': 'lk',
        'subdir': 'paizo/campaign_setting/lost_kingdoms',
        'exclude_names': set(), 'existing': False,
    },
    'Heart of the Jungle': {
        'source_long': 'Heart of the Jungle', 'source_short': 'HotJ',
        'source_web': 'http://paizo.com/products/btpy8evh',
        'source_date': '2010-07', 'prefix': 'hotj',
        'subdir': 'paizo/campaign_setting/heart_of_the_jungle',
        'exclude_names': set(), 'existing': True,
    },
    'Chronicle Of The Righteous': {
        'source_long': 'Chronicle of the Righteous', 'source_short': 'CotR',
        'source_web': 'http://paizo.com/products/btpy8xe9',
        'source_date': '2013-05', 'prefix': 'cotr',
        'subdir': 'paizo/campaign_setting/chronicle_of_the_righteous',
        'exclude_names': set(), 'existing': True,
    },
    'Book of the Damned Volume 1': {
        'source_long': 'Book of the Damned, Vol. 1: Princes of Darkness', 'source_short': 'BotD1',
        'source_web': 'http://paizo.com/products/btpy8a6f',
        'source_date': '2009-10', 'prefix': 'botd1',
        'subdir': 'paizo/campaign_setting/book_of_the_damned_volume_1',
        'exclude_names': set(), 'existing': True,
    },
    'Book of the Damned Volume 2': {
        'source_long': 'Book of the Damned, Vol. 2: Lords of Chaos', 'source_short': 'BotD2',
        'source_web': 'http://paizo.com/products/btpy8hij',
        'source_date': '2010-12', 'prefix': 'botd2',
        'subdir': 'paizo/campaign_setting/book_of_the_damned_volume_2',
        'exclude_names': set(), 'existing': True,
    },
    'Familiar Folio': {
        'source_long': 'Familiar Folio', 'source_short': 'FF',
        'source_web': 'http://paizo.com/products/btpy9bx1',
        'source_date': '2015-01', 'prefix': 'ff',
        'subdir': 'paizo/player_companion/familiar_folio',
        'exclude_names': set(), 'existing': True,
    },
    'Ultimate Magic': {
        'source_long': 'Ultimate Magic', 'source_short': 'UM',
        'source_web': 'http://paizo.com/products/btpy8g7s',
        'source_date': '2011-05', 'prefix': 'um',
        'subdir': 'paizo/roleplaying_game/ultimate_magic',
        'exclude_names': set(), 'existing': True,
    },
}


def slugify(name):
    """Convert a source name to a filesystem-safe slug."""
    s = name.lower()
    s = re.sub(r"[''']", '', s)
    s = re.sub(r'[^a-z0-9]+', '_', s)
    s = s.strip('_')
    return s


def make_short_name(name):
    """Generate a short source abbreviation from a name."""
    words = name.split()
    if len(words) == 1:
        return name[:4].upper()
    abbr = ''.join(w[0].upper() for w in words if w[0].isalpha())
    return abbr[:5]


def build_source_config(source_name):
    """Build a source config for an unknown source based on naming conventions."""
    ap_match = re.match(r'^AP\s+(\d+)$', source_name)
    if ap_match:
        ap_num = ap_match.group(1)
        return {
            'source_long': f'Pathfinder Adventure Path #{ap_num}',
            'source_short': f'AP{ap_num}',
            'source_web': 'http://paizo.com',
            'source_date': '2010-01',
            'prefix': f'ap{ap_num}',
            'subdir': f'paizo/adventure_path/ap{ap_num}',
            'exclude_names': set(),
            'existing': False,
        }

    slug = slugify(source_name)
    short = make_short_name(source_name)
    prefix = slug[:8]

    if any(kw in source_name.lower() for kw in ['tome of horrors', 'frog god']):
        subdir = f'frog_god_games/{slug}'
    elif any(kw in source_name.lower() for kw in ['kobold', 'midgard']):
        subdir = f'kobold_press/{slug}'
    else:
        subdir = f'paizo/campaign_setting/{slug}'

    return {
        'source_long': source_name,
        'source_short': short,
        'source_web': 'http://paizo.com',
        'source_date': '2010-01',
        'prefix': prefix,
        'subdir': subdir,
        'exclude_names': set(),
        'existing': False,
    }


def get_source_config(source_name):
    """Get or generate config for a source."""
    if source_name in KNOWN_SOURCE_CONFIG:
        return KNOWN_SOURCE_CONFIG[source_name]
    return build_source_config(source_name)


# ─── Pcfinder Data Parsing (fallback when no d20pfsrd cache) ────────────────

def parse_pcfinder_skills(skills_str):
    """Parse pcfinder skills string like 'Perception +5, Stealth +10' into dict."""
    if not skills_str:
        return {}

    skills = {}
    for m in re.finditer(
        r'([A-Z][a-zA-Z]*(?:\s+[a-zA-Z]+)*(?:\s*\([^)]+\))?)\s+([+-]?\d+)',
        skills_str
    ):
        skill_name = m.group(1).strip()
        bonus = int(m.group(2))
        skills[skill_name] = bonus

    return skills


def parse_pcfinder_sla_string(sla_str, creature=None):
    """Parse pcfinder SLA string into structured format compatible with build_sla_tags()."""
    if not sla_str or not isinstance(sla_str, str):
        return {}

    result = {'entries': {}, 'cl': 0}

    cl_match = re.search(r'CL\s+(\d+)', sla_str, re.IGNORECASE)
    if cl_match:
        result['cl'] = int(cl_match.group(1))
    elif creature:
        result['cl'] = creature.get('numericHitDice', 1)

    if result['cl'] == 0:
        return {}

    lines = re.split(r'[;\n]', sla_str)

    for line in lines:
        line = line.strip()
        if not line:
            continue

        freq = None
        spells_part = None

        m = re.match(r'(?:at\s+will|At\s+Will)\s*[\u2014\u2013\-]\s*(.+)', line, re.IGNORECASE)
        if m:
            freq = 'at_will'
            spells_part = m.group(1)

        if not freq:
            m = re.match(r'[Cc]onstant\s*[\u2014\u2013\-]\s*(.+)', line)
            if m:
                freq = 'constant'
                spells_part = m.group(1)

        if not freq:
            m = re.match(r'(\d+)/day\s*[\u2014\u2013\-]\s*(.+)', line, re.IGNORECASE)
            if m:
                freq = f'{m.group(1)}/day'
                spells_part = m.group(2)

        if not freq:
            m = re.match(r'(\d+)/week\s*[\u2014\u2013\-]\s*(.+)', line, re.IGNORECASE)
            if m:
                freq = f'{m.group(1)}/week'
                spells_part = m.group(2)

        if not freq or not spells_part:
            continue

        spells = []
        for spell_chunk in re.split(r',\s*(?![^(]*\))', spells_part):
            spell_chunk = spell_chunk.strip()
            if not spell_chunk:
                continue
            dc = None
            dc_match = re.search(r'\(DC\s+(\d+)\)', spell_chunk)
            if dc_match:
                dc = int(dc_match.group(1))
                spell_name = re.sub(r'\s*\(DC\s+\d+\)', '', spell_chunk).strip()
            else:
                spell_name = spell_chunk.strip()
            spell_name = re.sub(r'\s*\(.*?\)\s*$', '', spell_name).strip()
            if spell_name and len(spell_name) > 1:
                entry = {'spell': spell_name}
                if dc:
                    entry['dc'] = dc
                spells.append(entry)

        if spells:
            if freq in result['entries']:
                result['entries'][freq].extend(spells)
            else:
                result['entries'][freq] = spells

    if not result['entries']:
        return {}

    return {'slas': result}


def get_effective_d20_data(creature, d20_cache):
    """Get d20pfsrd data or build equivalent from pcfinder creature data."""
    d20_data = d20_cache.get(creature['name']) if d20_cache else None

    if d20_data:
        return d20_data

    # Build from pcfinder data
    synthetic = {}

    skills_str = creature.get('skills', '')
    if skills_str:
        synthetic['skills'] = parse_pcfinder_skills(skills_str)

    feats = creature.get('feats', [])
    if feats:
        synthetic['feats'] = feats

    langs = creature.get('languages', [])
    if langs:
        synthetic['languages'] = langs

    sla_str = creature.get('spellLikeAbilities', '')
    if sla_str:
        sla_data = parse_pcfinder_sla_string(sla_str, creature)
        if sla_data:
            synthetic.update(sla_data)

    return synthetic if synthetic else None


# ─── Helper Functions ────────────────────────────────────────────────────────

def ability_mod(score):
    """Calculate ability modifier from score."""
    if score is None:
        return 0
    return (score - 10) // 2


def good_save(hd):
    """Good save progression: 2 + HD/2."""
    return 2 + hd // 2


def poor_save(hd):
    """Poor save progression: HD/3."""
    return hd // 3


def determine_outsider_class(creature):
    """Determine which Outsider variant based on saves."""
    hd = creature.get('numericHitDice', 1)
    saves = creature.get('saves', {})
    scores = creature.get('abilityScores', {})

    # Base saves (subtract ability mods)
    con_mod = ability_mod(scores.get('con'))
    dex_mod = ability_mod(scores.get('dex'))
    wis_mod = ability_mod(scores.get('wis'))

    fort_base = saves.get('fortitude', 0) - con_mod
    ref_base = saves.get('reflex', 0) - dex_mod
    will_base = saves.get('will', 0) - wis_mod

    expected_good = good_save(hd)
    expected_poor = poor_save(hd)

    # Distance from good save progression (lower = more likely good)
    fort_dist = abs(fort_base - expected_good)
    ref_dist = abs(ref_base - expected_good)
    will_dist = abs(will_base - expected_good)

    # Pick the two saves closest to good progression
    saves_ranked = sorted([
        (fort_dist, 'Fort'),
        (ref_dist, 'Ref'),
        (will_dist, 'Will'),
    ])

    good_saves = sorted([saves_ranked[0][1], saves_ranked[1][1]])

    if good_saves == ['Fort', 'Ref']:
        return 'Outsider (Fort/Ref)'
    elif good_saves == ['Fort', 'Will']:
        return 'Outsider (Fort/Will)'
    else:
        return 'Outsider (Ref/Will)'


def determine_humanoid_class(creature):
    """Determine which Humanoid variant based on saves."""
    hd = creature.get('numericHitDice', 1)
    saves = creature.get('saves', {})
    scores = creature.get('abilityScores', {})

    con_mod = ability_mod(scores.get('con'))
    dex_mod = ability_mod(scores.get('dex'))
    wis_mod = ability_mod(scores.get('wis'))

    fort_base = saves.get('fortitude', 0) - con_mod
    ref_base = saves.get('reflex', 0) - dex_mod
    will_base = saves.get('will', 0) - wis_mod

    expected_good = good_save(hd)

    fort_dist = abs(fort_base - expected_good)
    ref_dist = abs(ref_base - expected_good)
    will_dist = abs(will_base - expected_good)

    # Default is Fort good (Humanoid)
    best = min(fort_dist, ref_dist, will_dist)
    if best == ref_dist and ref_dist < fort_dist:
        return 'Humanoid (Reflex)'
    elif best == will_dist and will_dist < fort_dist:
        return 'Humanoid (Will)'
    return 'Humanoid'


def get_monster_class(creature):
    """Get the MONSTERCLASS string for a creature."""
    ctype = creature.get('type', '').lower().replace(' ', '-')
    hd = creature.get('numericHitDice', 1)

    if ctype in SIMPLE_TYPE_MAP:
        class_name = SIMPLE_TYPE_MAP[ctype][0]
        # Undead with no INT are mindless
        if ctype == 'undead':
            int_score = creature.get('abilityScores', {}).get('int')
            if int_score is None:
                class_name = 'Undead (Mindless)'
        return f'{class_name}:{hd}'

    if ctype == 'outsider':
        return f'{determine_outsider_class(creature)}:{hd}'
    if ctype == 'humanoid':
        return f'{determine_humanoid_class(creature)}:{hd}'

    # Fallback: try matching partial
    for key, (class_name, _) in SIMPLE_TYPE_MAP.items():
        if key.startswith(ctype) or ctype.startswith(key):
            return f'{class_name}:{hd}'

    # Unknown type - use Monstrous Humanoid as generic fallback
    print(f"  WARNING: Unknown type '{ctype}' for {creature.get('name')}, defaulting to Monstrous Humanoid", file=sys.stderr)
    return f'Monstrous Humanoid:{hd}'


def get_race_type(creature):
    """Get RACETYPE value."""
    ctype = creature.get('type', '').lower().replace(' ', '-')
    if ctype in SIMPLE_TYPE_MAP:
        return SIMPLE_TYPE_MAP[ctype][1]
    if ctype == 'outsider':
        return 'Outsider'
    if ctype == 'humanoid':
        return 'Humanoid'
    for key, (_, rt) in SIMPLE_TYPE_MAP.items():
        if key.startswith(ctype) or ctype.startswith(key):
            return rt
    return 'Monstrous Humanoid'


def compute_natural_armor(creature):
    """Compute natural armor bonus from AC data."""
    ac = creature.get('armorClass', {})
    normal = ac.get('normal', 10)
    scores = creature.get('abilityScores', {})
    size = creature.get('size', 'Medium')

    dex_score = scores.get('dex')
    dex_mod = ability_mod(dex_score)
    size_mod = SIZE_AC_MOD.get(size, 0)

    nat_armor = normal - 10 - dex_mod - size_mod
    return max(0, nat_armor)


def compute_deflection(creature):
    """Detect deflection/dodge bonus from touch AC."""
    ac = creature.get('armorClass', {})
    touch = ac.get('touch', 10)
    scores = creature.get('abilityScores', {})
    size = creature.get('size', 'Medium')

    dex_score = scores.get('dex')
    dex_mod = ability_mod(dex_score)
    size_mod = SIZE_AC_MOD.get(size, 0)

    expected_touch = 10 + dex_mod + size_mod
    deflection = touch - expected_touch
    return max(0, deflection)


def parse_senses(senses_str):
    """Parse senses string into BONUS:VAR entries."""
    if not senses_str:
        return []
    bonuses = []
    s = senses_str.lower()

    m = re.search(r'darkvision\s+(\d+)\s*ft', s)
    if m:
        bonuses.append(f'BONUS:VAR|DarkvisionRange|{m.group(1)}|TYPE=Base')

    if 'low-light vision' in s:
        bonuses.append('BONUS:VAR|HasLowlightVision|1|TYPE=Base')

    m = re.search(r'blindsight\s+(\d+)\s*ft', s)
    if m:
        bonuses.append(f'BONUS:VAR|BlindsightRange|{m.group(1)}|TYPE=Base')

    m = re.search(r'blindsense\s+(\d+)\s*ft', s)
    if m:
        bonuses.append(f'BONUS:VAR|BlindsenseRange|{m.group(1)}|TYPE=Base')

    m = re.search(r'tremorsense\s+(\d+)\s*ft', s)
    if m:
        bonuses.append(f'BONUS:VAR|TremorsenseRange|{m.group(1)}|TYPE=Base')

    return bonuses


def parse_senses_abilities(senses_str):
    """Parse senses string into ability references."""
    if not senses_str:
        return []
    abilities = []
    s = senses_str.lower()
    if 'scent' in s:
        abilities.append('Scent')
    if 'all-around vision' in s:
        abilities.append('All-Around Vision')
    if 'see in darkness' in s:
        abilities.append('See in Darkness')
    return abilities


def build_move_string(speed_array):
    """Build MOVE tag from speed array."""
    if not speed_array:
        return 'MOVE:Walk,30'
    parts = []
    type_map = {
        'walk': 'Walk', 'fly': 'Fly', 'swim': 'Swim',
        'climb': 'Climb', 'burrow': 'Burrow',
    }
    for entry in speed_array:
        mt = entry.get('moveType', 'walk').lower()
        pcgen_type = type_map.get(mt, mt.capitalize())
        parts.append(f'{pcgen_type},{entry.get("value", 30)}')
    return 'MOVE:' + ','.join(parts)


def get_fly_maneuverability(speed_array):
    """Get fly maneuverability bonus if creature can fly."""
    if not speed_array:
        return None
    for entry in speed_array:
        if entry.get('moveType', '').lower() == 'fly':
            man = entry.get('maneuverability', '').lower()
            if man in MANEUVERABILITY_MAP:
                return MANEUVERABILITY_MAP[man]
    return None


def build_natural_attacks(creature):
    """Build NATURALATTACKS tags from parsed attack data."""
    parsed = creature.get('parsedAttacks', {})
    melee = parsed.get('melee', {})
    attacks = []

    for group_key, group_val in melee.items():
        if not isinstance(group_val, dict):
            continue
        attack_list = group_val.get('attacks', [])
        for atk in attack_list:
            name = atk.get('name', '').lower().strip()
            # Strip leading enhancement (e.g., "+1 flaming")
            cleaned = re.sub(r'^\+?\d+\s+(?:flaming\s+|frost\s+|shock\s+|holy\s+|unholy\s+|keen\s+|vorpal\s+)*', '', name)
            if not cleaned:
                cleaned = name

            if cleaned not in NATURAL_WEAPON_MAP:
                continue

            pcgen_name, damage_types = NATURAL_WEAPON_MAP[cleaned]
            qty = atk.get('quantity', 1)
            damage = atk.get('damage', {})
            dice = damage.get('dice', {})
            count = dice.get('count', 1)
            sides = dice.get('sides', 4)
            damage_str = f'{count}d{sides}'

            # Collect special effects for SPROP
            sprop_parts = []
            for effect in atk.get('specialEffects', []):
                ename = effect.get('name', '')
                if ename and ename.lower() not in ('', damage_str):
                    sprop_parts.append(ename)

            weapon_types = f'Weapon.Natural.Weapon Group Natural.Melee.Finesseable.{damage_types}'

            # Handle incorporeal
            if 'incorporeal' in cleaned:
                weapon_types = f'Weapon.Natural.Weapon Group Natural.Melee.Finesseable.Incorporeal.Touch.{damage_types}'

            entry = f'{pcgen_name},{weapon_types},*{qty},{damage_str}'
            if sprop_parts:
                sprop = 'plus ' + ' and '.join(sprop_parts)
                entry += f',SPROP={sprop}'

            attacks.append(entry)

    if not attacks:
        return ''

    # Combine multiple attacks with pipe separator in a single NATURALATTACKS tag
    return 'NATURALATTACKS:' + '|'.join(attacks)


def parse_source_page(source_str):
    """Extract page number from source string like 'Bestiary 5 pg. 22'."""
    m = re.search(r'pg\.\s*(\d+)', source_str or '')
    if m:
        return f'p.{m.group(1)}'
    return ''


def get_reach(creature):
    """Get reach value, using explicit data or size default."""
    # Check if reach is explicitly in the data
    reach_str = creature.get('reach', '')
    if reach_str:
        m = re.search(r'(\d+)', str(reach_str))
        if m:
            return int(m.group(1))
    # Use size-based default
    size = creature.get('size', 'Medium')
    return SIZE_REACH.get(size, 5)


def clean_dr(dr_raw):
    """Clean and validate a DR string. Returns cleaned string or None if invalid."""
    # Valid DR examples: "10/evil", "5/—", "15/epic and good", "10/cold iron and magic",
    #                    "20/epic, good, and silver", "5/-"
    # Invalid: "20/", "10/ silver in hybrid or vermin form...", "20/epic, goo"
    dr = dr_raw.strip()
    # Repeatedly strip trailing junk
    for _ in range(5):
        prev = dr
        dr = dr.rstrip(',').rstrip().rstrip(' and').rstrip()
        if dr == prev:
            break
    # Must be N/something
    m = re.match(r'^(\d+)/(.+)$', dr)
    if not m:
        return None
    num = m.group(1)
    dr_type = m.group(2).strip()
    if not dr_type:
        return None
    # Reject if it contains sentence-like content (periods, long text)
    if '.' in dr_type or len(dr_type) > 40:
        return None
    # Reject if type ends with an incomplete word fragment (< 4 chars after last separator)
    # e.g. "epic, goo" → fragment "goo" (should be "good")
    # Allow: "—", "-", "----", single words, proper compounds
    last_part = re.split(r'[, ]+', dr_type)[-1]
    if len(last_part) < 4 and last_part not in ('-', '—', '---', '----'):
        return None
    return f'{num}/{dr_type}'


def normalize_spell_name(name):
    """Convert spell names from natural English to PCGen convention.
    'Greater Dispel Magic' -> 'Dispel Magic (Greater)'
    'Lesser Restoration' -> 'Restoration (Lesser)'
    'Mass Heal' -> 'Heal (Mass)'
    'Communal Stoneskin' -> 'Stoneskin (Communal)'
    """
    for qualifier in ('Greater', 'Lesser', 'Mass', 'Communal'):
        if name.startswith(qualifier + ' '):
            base = name[len(qualifier) + 1:]
            return f'{base} ({qualifier})'
    return name


def sanitize_name(name):
    """Clean creature name for use as PCGen KEY.
    Converts comma-separated names like 'Giant, Moon Giant' to
    parenthesized form 'Giant (Moon Giant)' since commas are
    prohibited in PCGen keys."""
    name = name.strip()
    if ',' in name:
        parts = [p.strip() for p in name.split(',', 1)]
        name = f'{parts[0]} ({parts[1]})'
    return name


def get_inferred_abilities(creature):
    """Infer abilities from creature data for Racial Traits entry."""
    abilities = []

    # Immunities
    for imm in creature.get('immunities', []):
        imm_lower = imm.lower().strip()
        # Skip type-wide trait immunities like "undead traits", "construct traits"
        if 'traits' in imm_lower:
            continue
        if imm_lower in IMMUNITY_MAP:
            abilities.append(IMMUNITY_MAP[imm_lower])

    # Senses-based abilities
    abilities.extend(parse_senses_abilities(creature.get('senses', '')))

    # Subtypes
    subtypes = [s.lower() for s in creature.get('subTypes', [])]
    if 'incorporeal' in subtypes:
        abilities.append('Incorporeal')

    # Parsed attack special effects
    parsed = creature.get('parsedAttacks', {})
    melee = parsed.get('melee', {})
    for group_val in melee.values():
        if not isinstance(group_val, dict):
            continue
        for atk in group_val.get('attacks', []):
            for effect in atk.get('specialEffects', []):
                ename = effect.get('effectType', '').lower()
                if ename == 'grab':
                    if 'Grab' not in abilities:
                        abilities.append('Grab')
                elif ename == 'trip':
                    if 'Trip' not in abilities:
                        abilities.append('Trip')
                elif ename == 'constrict':
                    if 'Constrict' not in abilities:
                        abilities.append('Constrict')
                elif ename == 'poison':
                    if 'Poison' not in abilities:
                        abilities.append('Poison')

    # Can't be tripped (legless creatures)
    legs, _ = TYPE_LEGS_HANDS.get(creature.get('type', '').lower().replace(' ', '-'), (2, 2))
    if legs == 0:
        abilities.append("Can't Be Tripped")

    return abilities


def should_exclude(creature, exclude_names):
    """Check if creature should be excluded (already defined as companion/familiar/PC race)."""
    name = creature.get('name', '').lower().strip()

    # Direct match
    if name in exclude_names:
        return True

    # Strip prefix patterns like "Dinosaur, " or "Clockwork, "
    if ', ' in name:
        parts = name.split(', ', 1)
        suffix = parts[1].strip()
        if suffix in exclude_names:
            return True
        # Also check reversed
        reversed_name = f"{parts[1].strip()}, {parts[0].strip()}"
        if reversed_name in exclude_names:
            return True

    return False


# ─── Generators ──────────────────────────────────────────────────────────────

def generate_race_line(creature, config, d20_data=None):
    """Generate a single race LST line for a creature."""
    name = sanitize_name(creature['name'])
    size = creature.get('size', 'Medium')
    size_code = SIZE_MAP.get(size, 'M')
    ctype = creature.get('type', '').lower().replace(' ', '-')
    scores = creature.get('abilityScores', {})

    parts = [name]
    parts.append(f'KEY:{name}')

    # STARTFEATS - constructs and mindless undead don't get feats
    int_score = scores.get('int')
    if int_score is not None:
        parts.append('STARTFEATS:1')

    parts.append(f'SIZE:{size_code}')
    parts.append(build_move_string(creature.get('speed', [])))

    reach = get_reach(creature)
    parts.append(f'REACH:{reach}')

    # Deflection bonus (if touch AC implies one)
    deflection = compute_deflection(creature)
    if deflection > 0:
        parts.append(f'BONUS:COMBAT|AC|{deflection}|TYPE=Deflection')

    # Ability score bonuses
    for stat_name, stat_key in zip(STAT_NAMES, STAT_KEYS):
        score = scores.get(stat_key)
        if score is not None:
            bonus = score - 10
            if bonus != 0:
                parts.append(f'BONUS:STAT|{stat_name}|{bonus}')

    # Natural armor
    nat_armor = compute_natural_armor(creature)
    # Subtract deflection from nat armor estimate (deflection inflates the computation)
    nat_armor = max(0, nat_armor - deflection)
    if nat_armor > 0:
        parts.append(f'BONUS:VAR|AC_Natural_Armor|{nat_armor}|TYPE=Base')

    # Vision and senses bonuses
    for sense_bonus in parse_senses(creature.get('senses', '')):
        parts.append(sense_bonus)

    # Fly maneuverability
    man = get_fly_maneuverability(creature.get('speed', []))
    if man is not None:
        parts.append(f'BONUS:VAR|Maneuverability|{man}')

    # Ability reference to Racial Traits
    parts.append(f'ABILITY:Internal|AUTOMATIC|Racial Traits ~ {name}')

    # LEGS / HANDS
    legs, hands = TYPE_LEGS_HANDS.get(ctype, (2, 2))
    parts.append(f'LEGS:{legs}')
    parts.append(f'HANDS:{hands}')

    # Natural attacks
    nat_attacks = build_natural_attacks(creature)
    if nat_attacks:
        parts.append(nat_attacks)

    # DEFINESTAT for null ability scores
    for stat_name, stat_key in zip(STAT_NAMES, STAT_KEYS):
        if scores.get(stat_key) is None:
            parts.append(f'DEFINESTAT:NONSTAT|{stat_name}')

    # Spell Resistance
    sr = creature.get('spellResistance')
    if sr:
        parts.append(f'SR:{sr}')

    # Damage Reduction
    dr_raw = creature.get('damageReduction', '')
    if dr_raw and '/' in dr_raw:
        dr_clean = clean_dr(dr_raw)
        if dr_clean:
            parts.append(f'DR:{dr_clean}')

    # Monster class, race type, subtypes
    parts.append(f'MONSTERCLASS:{get_monster_class(creature)}')
    race_type = get_race_type(creature)
    parts.append(f'RACETYPE:{race_type}')

    subtypes = creature.get('subTypes', [])
    if subtypes:
        valid = [s.title() for s in subtypes if s and not re.match(r'^\+?\d', s)]
        if valid:
            parts.append('RACESUBTYPE:' + '|'.join(valid))

    parts.append(f'TYPE:{race_type}')

    # CR
    cr = creature.get('cr', '1')
    parts.append(f'CR:{cr}')

    # Source page
    sp = parse_source_page(creature.get('source', ''))
    if sp:
        parts.append(f'SOURCEPAGE:{sp}')

    # Kit (alignment)
    alignment = creature.get('alignment', 'N')
    kit_align = ALIGNMENT_KIT_MAP.get(alignment, 'TN')
    parts.append(f'KIT:1|{kit_align}')

    # Languages from d20pfsrd cache
    if d20_data and d20_data.get('languages'):
        langs = [l for l in d20_data['languages'] if l and len(l) > 1]
        if langs:
            parts.append('AUTO:LANG|' + '|'.join(langs))

    # BaseSize fact
    parts.append(f'FACT:BaseSize|{size_code}')

    return '\t'.join(parts)


def compute_skill_ranks(creature, skill_name, total_bonus):
    """Compute skill ranks from total bonus.

    For monsters, listed skills are class skills (+3 bonus).
    ranks = total_bonus - ability_mod - 3 (class skill)
    Clamped to 1..HD.
    """
    hd = creature.get('numericHitDice', 1)
    scores = creature.get('abilityScores', {})

    # Determine which ability score the skill uses
    skill_ability_map = {
        'acrobatics': 'dex', 'appraise': 'int', 'bluff': 'cha',
        'climb': 'str', 'craft': 'int', 'diplomacy': 'cha',
        'disable device': 'dex', 'disguise': 'cha', 'escape artist': 'dex',
        'fly': 'dex', 'handle animal': 'cha', 'heal': 'wis',
        'intimidate': 'cha', 'knowledge': 'int', 'linguistics': 'int',
        'perception': 'wis', 'perform': 'cha', 'profession': 'wis',
        'ride': 'dex', 'sense motive': 'wis', 'sleight of hand': 'dex',
        'spellcraft': 'int', 'stealth': 'dex', 'survival': 'wis',
        'swim': 'str', 'use magic device': 'cha',
    }

    # Find the ability key for this skill
    skill_lower = skill_name.lower()
    ability_key = None
    for prefix, akey in skill_ability_map.items():
        if skill_lower.startswith(prefix):
            ability_key = akey
            break

    if ability_key is None:
        ability_key = 'int'  # Fallback

    score = scores.get(ability_key)
    mod = ability_mod(score)

    # ranks = total - mod - 3 (class skill bonus)
    ranks = total_bonus - mod - 3
    ranks = max(1, min(hd, ranks))
    return ranks


def normalize_feat_name(feat):
    """Normalize a feat name for PCGen."""
    # PCGen uses specific formatting for some feats
    feat = feat.strip()
    # Remove trailing B (bonus feat marker from d20pfsrd)
    feat = re.sub(r'\s*[Bb]$', '', feat)
    return feat


def generate_kit_block(creature, config, d20_data=None):
    """Generate a STARTPACK kit block for a creature."""
    name = sanitize_name(creature['name'])
    race_type = get_race_type(creature)
    scores = creature.get('abilityScores', {})
    alignment = creature.get('alignment', 'N')
    kit_align = ALIGNMENT_KIT_MAP.get(alignment, 'TN')
    sp = parse_source_page(creature.get('source', ''))

    lines = []

    # STARTPACK header
    header_parts = [
        f'STARTPACK:{name} Default',
        f'TYPE:Default Monster.{race_type}',
        'VISIBLE:QUALIFY',
        'EQUIPBUY:0',
        f'PREMULT:1,[PRERACE:1,{name}],[!PRERACE:1,%]',
    ]
    if sp:
        header_parts.append(f'SOURCEPAGE:{sp}')
    lines.append('\t'.join(header_parts))

    lines.append(f'ALIGN:{kit_align}')
    lines.append(f'RACE:{name}\t!PRERACE:1,%')
    lines.append(f'NAME:{name}')

    # STAT line - only include non-null stats
    stat_parts = []
    for stat_key in STAT_KEYS:
        if scores.get(stat_key) is not None:
            stat_parts.append(f'{stat_key.upper()}=10')
    if stat_parts:
        lines.append('STAT:' + '|'.join(stat_parts))

    # Skills from d20pfsrd cache
    if d20_data and d20_data.get('skills'):
        for skill_name, bonus in sorted(d20_data['skills'].items()):
            # Expand condensed Knowledge skills: "Know. (arcana, planes)" → multiple entries
            if skill_name.lower().startswith('know'):
                m = re.match(r'know\w*\.?\s*\(([^)]+)\)', skill_name, re.IGNORECASE)
                if m:
                    subtypes = [s.strip() for s in m.group(1).split(',')]
                    for sub in subtypes:
                        pcgen_skill = f'Knowledge ({sub.title()})'
                        ranks = compute_skill_ranks(creature, pcgen_skill, bonus)
                        lines.append(f'SKILL:{pcgen_skill}\tRANK:{ranks}')
                    continue
            ranks = compute_skill_ranks(creature, skill_name, bonus)
            lines.append(f'SKILL:{skill_name}\tRANK:{ranks}')

    # Feats from d20pfsrd cache
    if d20_data and d20_data.get('feats'):
        for feat in d20_data['feats']:
            feat_name = normalize_feat_name(feat)
            if feat_name:
                lines.append(f'ABILITY:CATEGORY=FEAT|{feat_name}')

    return '\n'.join(lines)


def build_sla_tags(d20_data):
    """Build SPELLS:Innate tags from scraped SLA data.

    Returns list of SPELLS:Innate tag strings.
    """
    slas = d20_data.get('slas', {})
    entries = slas.get('entries', {})
    cl = slas.get('cl', 0)
    if not entries or cl == 0:
        return []

    tags = []

    # Map d20pfsrd frequency keys to PCGen TIMES values
    for freq, spells in entries.items():
        if not spells:
            continue

        # Build spell list
        spell_parts = []
        for s in spells:
            spell_name = s.get('spell', '')
            if not spell_name:
                continue
            # Fix encoding issues
            spell_name = spell_name.replace('\u2019', "'").replace('\u2018', "'")
            spell_name = spell_name.replace('\u00e2\u0080\u0099', "'")
            # Capitalize spell name (PCGen convention: Title Case)
            spell_name = spell_name.title()
            # Convert "Greater/Lesser/Mass X" to "X (Greater/Lesser/Mass)" (PCGen convention)
            spell_name = normalize_spell_name(spell_name)
            dc = s.get('dc')
            if dc:
                spell_parts.append(f'{spell_name},{dc}')
            else:
                spell_parts.append(spell_name)

        if not spell_parts:
            continue

        # Determine TIMES value
        if freq == 'constant':
            # Constant SLAs: use TIMES=-1 (always active)
            tag = f'SPELLS:Innate|TIMES=-1|CASTERLEVEL={cl}|{"|".join(spell_parts)}'
        elif freq == 'at_will':
            tag = f'SPELLS:Innate|TIMES=ATWILL|CASTERLEVEL={cl}|{"|".join(spell_parts)}'
        elif freq.endswith('/day'):
            times = freq.split('/')[0]
            if times == '1':
                # 1/day: no TIMES= prefix in B4 convention
                tag = f'SPELLS:Innate|CASTERLEVEL={cl}|{"|".join(spell_parts)}'
            else:
                tag = f'SPELLS:Innate|TIMES={times}|CASTERLEVEL={cl}|{"|".join(spell_parts)}'
        elif freq.endswith('/week'):
            times = int(freq.split('/')[0])
            # Convert to daily equivalent or use raw
            tag = f'SPELLS:Innate|TIMES={times}|TIMEUNIT=Week|CASTERLEVEL={cl}|{"|".join(spell_parts)}'
        elif freq.endswith('/month'):
            times = int(freq.split('/')[0])
            tag = f'SPELLS:Innate|TIMES={times}|TIMEUNIT=Month|CASTERLEVEL={cl}|{"|".join(spell_parts)}'
        else:
            # Unknown frequency, try to parse number
            m = re.match(r'(\d+)', freq)
            if m:
                tag = f'SPELLS:Innate|TIMES={m.group(1)}|CASTERLEVEL={cl}|{"|".join(spell_parts)}'
            else:
                continue

        tags.append(tag)

    return tags


def generate_racial_traits_line(creature, d20_data=None):
    """Generate a Racial Traits ability entry for a creature."""
    name = sanitize_name(creature['name'])
    abilities = get_inferred_abilities(creature)

    parts = [f'Racial Traits ~ {name}']
    parts.append('CATEGORY:Internal')

    # SLAs from d20pfsrd cache
    if d20_data:
        sla_tags = build_sla_tags(d20_data)
        for tag in sla_tags:
            parts.append(tag)

    if abilities:
        parts.append('ABILITY:Special Ability|AUTOMATIC|' + '|'.join(abilities))

    return '\t'.join(parts)


# ─── File Writers ────────────────────────────────────────────────────────────

def get_source_long(config):
    """Get the source_long field from config, supporting both legacy and new format."""
    return config.get('source_long') or config.get('source_prefix', '')


def write_races_file(creatures, config, output_dir, d20_cache=None, append=False):
    """Write or append to the {prefix}_races.lst file."""
    d20_cache = d20_cache or {}
    prefix = config['prefix']
    source_long = get_source_long(config)
    filepath = os.path.join(output_dir, f'{prefix}_races.lst')

    new_lines = []
    for creature in sorted(creatures, key=lambda c: c['name']):
        d20_data = get_effective_d20_data(creature, d20_cache)
        new_lines.append(generate_race_line(creature, config, d20_data))

    if append and os.path.exists(filepath):
        # Append auto-generated block to existing file
        existing = ''
        with open(filepath, 'r', encoding='utf-8') as f:
            existing = f.read()

        marker = '###Block: Monster Races (auto-generated)'
        if marker in existing:
            existing = existing[:existing.index(marker)].rstrip()

        block = ['\n', marker, '']
        block.extend(new_lines)

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(existing.rstrip() + '\n' + '\n'.join(block) + '\n')
    else:
        lines = []
        lines.append(f'SOURCELONG:{source_long}\tSOURCESHORT:{config["source_short"]}\tSOURCEWEB:{config["source_web"]}\tSOURCEDATE:{config["source_date"]}')
        lines.append('')
        lines.extend(new_lines)

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')

    action = "Appended" if append else "Wrote"
    print(f"  {action} {len(creatures)} races to {filepath}")


def write_kits_file(creatures, config, output_dir, d20_cache=None, append=False):
    """Write or append to the {prefix}_kits_race.lst file."""
    d20_cache = d20_cache or {}
    prefix = config['prefix']
    source_long = get_source_long(config)
    filepath = os.path.join(output_dir, f'{prefix}_kits_race.lst')

    new_blocks = []
    for creature in sorted(creatures, key=lambda c: c['name']):
        d20_data = get_effective_d20_data(creature, d20_cache)
        new_blocks.append(generate_kit_block(creature, config, d20_data))

    if append and os.path.exists(filepath):
        existing = ''
        with open(filepath, 'r', encoding='utf-8') as f:
            existing = f.read()

        marker = '###Block: Monster Kits (auto-generated)'
        if marker in existing:
            existing = existing[:existing.index(marker)].rstrip()

        block = ['\n', marker, '']
        for b in new_blocks:
            block.append(b)
            block.append('')

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(existing.rstrip() + '\n' + '\n'.join(block) + '\n')
    else:
        lines = []
        lines.append(f'SOURCELONG:{source_long}\tSOURCESHORT:{config["source_short"]}\tSOURCEWEB:{config["source_web"]}\tSOURCEDATE:{config["source_date"]}')
        lines.append('')
        for b in new_blocks:
            lines.append(b)
            lines.append('')

        with open(filepath, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')

    action = "Appended" if append else "Wrote"
    print(f"  {action} {len(creatures)} kits to {filepath}")


def append_abilities_file(creatures, config, output_dir, d20_cache=None):
    """Append Racial Traits entries to existing {prefix}_abilities_race.lst."""
    d20_cache = d20_cache or {}
    prefix = config['prefix']
    filepath = os.path.join(output_dir, f'{prefix}_abilities_race.lst')

    # Read existing content
    existing = ''
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            existing = f.read()

    # Build new entries
    new_lines = []
    new_lines.append('')
    new_lines.append('###Block: Monster Racial Traits (auto-generated)')
    new_lines.append('')

    for creature in sorted(creatures, key=lambda c: c['name']):
        d20_data = get_effective_d20_data(creature, d20_cache)
        new_lines.append(generate_racial_traits_line(creature, d20_data))

    # Remove any previous auto-generated block
    marker = '###Block: Monster Racial Traits (auto-generated)'
    if marker in existing:
        existing = existing[:existing.index(marker)].rstrip()

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(existing.rstrip() + '\n' + '\n'.join(new_lines) + '\n')

    print(f"  Appended {len(creatures)} racial traits to {filepath}")


def update_pcc_file(config, output_dir):
    """Update the PCC file to reference new monster files."""
    prefix = config['prefix']
    source_long = get_source_long(config)

    # Legacy bestiary naming
    if 'dir' in config:
        pcc_path = os.path.join(output_dir, f'_bestiary_{config["dir"].split("_")[1]}.pcc')
        if not os.path.exists(pcc_path):
            pcc_path = os.path.join(output_dir, f'_{config["dir"]}.pcc')
    else:
        pcc_path = None

    # Generic: find any .pcc file in the directory
    if not pcc_path or not os.path.exists(pcc_path):
        pcc_files = [f for f in os.listdir(output_dir) if f.endswith('.pcc')]
        if pcc_files:
            pcc_path = os.path.join(output_dir, pcc_files[0])
        else:
            print(f"  WARNING: No PCC file found in {output_dir}", file=sys.stderr)
            return

    with open(pcc_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Rename CAMPAIGN if it still says "Only Player Options"
    content = content.replace(
        f'CAMPAIGN:{source_long} (Only Player Options Implemented)',
        f'CAMPAIGN:{source_long}'
    )
    content = content.replace(
        f'CAMPAIGN:{source_long} (Player Options Only)',
        f'CAMPAIGN:{source_long}'
    )

    # Check if race/kit references already exist
    race_ref = f'RACE:{prefix}_races.lst'
    kit_ref = f'KIT:{prefix}_kits_race.lst'

    if race_ref in content:
        print(f"  PCC already contains {race_ref}, skipping update")
        with open(pcc_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return

    # Insert before the "# ENTRY DATE" comment block
    insert_marker = '# ENTRY DATE'
    if insert_marker in content:
        idx = content.index(insert_marker)
        line_start = content.rfind('\n', 0, idx)
        insert_point = line_start if line_start >= 0 else idx

        new_refs = f'\n{race_ref}\n{kit_ref}\n\n'
        content = content[:insert_point] + new_refs + content[insert_point:]
    else:
        content = content.rstrip() + f'\n\n{race_ref}\n{kit_ref}\n'

    with open(pcc_path, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"  Updated PCC: {pcc_path}")


def create_pcc_file(config, output_dir):
    """Create a new PCC file for a source that doesn't have one."""
    source_long = get_source_long(config)
    prefix = config['prefix']
    slug = slugify(source_long)
    pcc_path = os.path.join(output_dir, f'{slug}.pcc')

    publisher_long = config.get('publisher_long', 'Paizo Inc.')
    publisher_short = config.get('publisher_short', 'Paizo')
    pcc_type = config.get('pcc_type', 'Paizo Publishing.Pathfinder RPG')

    content = f"""CAMPAIGN:{source_long}
KEY:{source_long} ~ Creatures
GAMEMODE:Pathfinder|Pathfinder_PFS
TYPE:{pcc_type}
STATUS:ALPHA
GENRE:Fantasy
BOOKTYPE:Supplement
SETTING:Pathfinder
PRECAMPAIGN:1,INCLUDESBOOKTYPE=Core Rules
PRECAMPAIGN:1,INCLUDES=Bestiary,INCLUDES=Bestiary ~ Player Options Only
PUBNAMELONG:{publisher_long}
PUBNAMESHORT:{publisher_short}
PUBNAMEWEB:{config['source_web']}
SOURCELONG:{source_long}
SOURCESHORT:{config['source_short']}
SOURCEWEB:{config['source_web']}
SOURCEDATE:{config['source_date']}
RANK:{config['source_date'].replace('-', '')}
ISOGL:YES

INFOTEXT:This dataset uses trademarks and/or copyrights owned by Paizo Inc., which are used under Paizo's Community Use Policy. We are expressly prohibited from charging you to use or access this content. This dataset is not published, endorsed, or specifically approved by Paizo Publishing. For more information about Paizo's Community Use Policy, please visit paizo.com/communityuse. For more information about Paizo Publishing and Paizo products, please visit paizo.com.
COPYRIGHT:Open Game License v 1.0a Copyright 2000, Wizards of the Coast, Inc.
COPYRIGHT:System Reference Document Copyright 2000, Wizards of the Coast, Inc; Authors: Jonathan Tweet, Monte Cook, Skip Williams, based on material by E. Gary Gygax and Dave Arneson.
COPYRIGHT:PCGen dataset conversion for "{source_long}" Copyright 2025, PCGen Data Team (auto-generated from pcfinder-csr)

ABILITY:{prefix}_abilities_race.lst
RACE:{prefix}_races.lst
KIT:{prefix}_kits_race.lst
"""

    with open(pcc_path, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"  Created PCC: {pcc_path}")


# ─── Main ────────────────────────────────────────────────────────────────────

def load_creatures(source_path):
    """Load all creatures from JSON file."""
    print(f"Loading creatures from {source_path}...")
    with open(source_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    print(f"  Loaded {len(data)} total creatures")
    return data


def filter_bestiary(creatures, bestiary_num, config):
    """Filter creatures belonging to a specific bestiary (legacy mode)."""
    prefix = config['source_prefix']
    filtered = []
    for c in creatures:
        source = c.get('source', '')
        if source.startswith(prefix) or source.startswith(f'PFRPG {prefix}'):
            if not should_exclude(c, config['exclude_names']):
                filtered.append(c)
    return filtered


def filter_by_names(creatures, creature_names):
    """Filter pcfinder creatures by a set of names (from comparison report)."""
    names_lower = {n.lower() for n in creature_names}
    return [c for c in creatures if c.get('name', '').lower() in names_lower]


def generate_bestiary(creatures, bestiary_num, pcgen_data_dir, d20_cache=None):
    """Generate all files for one bestiary (legacy mode)."""
    d20_cache = d20_cache or {}
    config = BESTIARY_CONFIG[bestiary_num]
    output_dir = os.path.join(pcgen_data_dir, 'pathfinder', 'paizo', 'roleplaying_game', config['dir'])

    print(f"\n{'='*60}")
    print(f"Generating Bestiary {bestiary_num}")
    print(f"{'='*60}")

    filtered = filter_bestiary(creatures, bestiary_num, config)
    print(f"  {len(filtered)} creatures after filtering (excluded companions/familiars/PC races)")

    if not filtered:
        print("  No creatures found! Check source field format.")
        return

    cache_hits = sum(1 for c in filtered if c['name'] in d20_cache)
    print(f"  {cache_hits}/{len(filtered)} creatures have d20pfsrd scraped data")

    write_races_file(filtered, config, output_dir, d20_cache)
    write_kits_file(filtered, config, output_dir, d20_cache)
    append_abilities_file(filtered, config, output_dir, d20_cache)
    update_pcc_file(config, output_dir)

    print(f"\nBestiary {bestiary_num} complete: {len(filtered)} creatures generated")


def generate_source(source_name, creature_names, all_creatures, pcgen_data_dir, d20_cache=None):
    """Generate LST files for a source from comparison report."""
    d20_cache = d20_cache or {}
    config = get_source_config(source_name)
    output_dir = os.path.join(pcgen_data_dir, 'pathfinder', config['subdir'])

    print(f"\n{'='*60}")
    print(f"Generating: {source_name} ({len(creature_names)} creatures)")
    print(f"{'='*60}")

    # Filter pcfinder creatures to just the ones we need
    filtered = filter_by_names(all_creatures, creature_names)

    # Also apply exclusions
    exclude = config.get('exclude_names', set())
    if exclude:
        filtered = [c for c in filtered if not should_exclude(c, exclude)]

    if not filtered:
        print(f"  No matching creatures found in pcfinder data for {source_name}")
        return

    print(f"  {len(filtered)} creatures to generate")

    # Create output directory if needed
    os.makedirs(output_dir, exist_ok=True)

    # Check if this is an existing directory with existing files
    pcc_files = [f for f in os.listdir(output_dir) if f.endswith('.pcc')] if os.path.isdir(output_dir) else []
    is_existing = bool(pcc_files)

    if is_existing:
        update_pcc_file(config, output_dir)
    else:
        create_pcc_file(config, output_dir)

    # For existing directories, append to existing files rather than overwriting
    write_races_file(filtered, config, output_dir, d20_cache, append=is_existing)
    write_kits_file(filtered, config, output_dir, d20_cache, append=is_existing)
    append_abilities_file(filtered, config, output_dir, d20_cache)

    print(f"\n{source_name} complete: {len(filtered)} creatures generated in {output_dir}")


def load_d20_cache(cache_path):
    """Load scraped d20pfsrd data cache."""
    if not cache_path or not os.path.exists(cache_path):
        return {}
    print(f"Loading d20pfsrd cache from {cache_path}...")
    with open(cache_path, 'r', encoding='utf-8') as f:
        cache = json.load(f)
    print(f"  Loaded data for {len(cache)} creatures")
    return cache


def main():
    parser = argparse.ArgumentParser(
        description='Generate PCGen LST files from pcfinder creatures.json')
    parser.add_argument('--source', required=True, help='Path to creatures.json')
    parser.add_argument('--pcgen-data', default=None, help='Path to PCGen data/ directory')
    parser.add_argument('--cache', default=None,
                        help='Path to d20pfsrd_cache.json (scraped feats/skills/SLAs)')

    # Legacy bestiary mode
    parser.add_argument('--bestiary', type=int, nargs='+', choices=[5, 6],
                        help='Legacy mode: which bestiaries to generate')

    # New source mode
    parser.add_argument('--sources', nargs='+',
                        help='Source names to generate (from comparison report)')
    parser.add_argument('--all', action='store_true',
                        help='Generate all sources from comparison report')
    parser.add_argument('--comparison', default=None,
                        help='Path to creature_comparison.json (from compare_creatures.py)')

    args = parser.parse_args()

    # Determine PCGen data directory
    if args.pcgen_data:
        pcgen_data = args.pcgen_data
    else:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        pcgen_data = os.path.join(os.path.dirname(script_dir), 'data')

    if not os.path.isdir(pcgen_data):
        print(f"ERROR: PCGen data directory not found: {pcgen_data}", file=sys.stderr)
        sys.exit(1)

    creatures = load_creatures(args.source)
    d20_cache = load_d20_cache(args.cache)

    # New source mode (comparison-based)
    if args.all or args.sources:
        comparison_path = args.comparison
        if not comparison_path:
            script_dir = os.path.dirname(os.path.abspath(__file__))
            comparison_path = os.path.join(script_dir, 'creature_comparison.json')

        if not os.path.exists(comparison_path):
            print(f"ERROR: Comparison report not found: {comparison_path}", file=sys.stderr)
            print("  Run compare_creatures.py first to generate it.", file=sys.stderr)
            sys.exit(1)

        print(f"Loading comparison report from {comparison_path}...")
        with open(comparison_path, 'r', encoding='utf-8') as f:
            comparison = json.load(f)

        if args.all:
            sources_to_generate = sorted(comparison.keys())
        else:
            sources_to_generate = args.sources
            # Validate
            for s in sources_to_generate:
                if s not in comparison:
                    print(f"WARNING: Source '{s}' not found in comparison report", file=sys.stderr)

        total_creatures = 0
        for source_name in sources_to_generate:
            if source_name not in comparison:
                continue
            creature_list = comparison[source_name]
            creature_names = [c['name'] for c in creature_list]
            generate_source(source_name, creature_names, creatures, pcgen_data, d20_cache)
            total_creatures += len(creature_names)

        print(f"\n{'='*60}")
        print(f"TOTAL: Generated {total_creatures} creatures across {len(sources_to_generate)} sources")
        print(f"{'='*60}")

    # Legacy bestiary mode
    elif args.bestiary:
        for bn in args.bestiary:
            generate_bestiary(creatures, bn, pcgen_data, d20_cache)

    # Default: legacy mode with B5/B6
    else:
        for bn in [5, 6]:
            generate_bestiary(creatures, bn, pcgen_data, d20_cache)

    print("\nDone!")


if __name__ == '__main__':
    main()
