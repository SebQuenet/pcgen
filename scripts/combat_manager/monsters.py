"""Turn Combat Manager's class-less monsters into the creatures generate_bestiary.py writes as races.

Spec: specs/import-combat-manager.md, section 7
"""

from __future__ import annotations

import functools
import re
from dataclasses import dataclass
from pathlib import Path

import catalog
import statblock
from generate_bestiary import normalize_feat_names, normalize_kit_skills, spell_key

STAT_KEYS = ('str', 'dex', 'con', 'int', 'wis', 'cha')
MOVE_TYPES = ('fly', 'swim', 'climb', 'burrow')
DRAGON_AGES = r'(wyrmling|very young|young adult|young|juvenile|mature adult|adult|very old|old|ancient|great wyrm|wyrm)'
ELEMENTAL_SIZES = r'(small|medium|large|huge|greater|elder)'
CREATURE_TYPES = ('magical beast', 'monstrous humanoid', 'aberration', 'animal', 'construct', 'dragon', 'fey', 'humanoid',
                  'ooze', 'outsider', 'plant', 'undead', 'vermin', 'elemental')
# Feats whose choice is a weapon proficiency
WEAPON_FEATS = ('Weapon Focus', 'Greater Weapon Focus', 'Weapon Specialization', 'Greater Weapon Specialization',
                'Improved Critical', 'Exotic Weapon Proficiency', 'Martial Weapon Proficiency', 'Improved Natural Attack')
# Names no rule brings to the race PCGen already has
EQUIVALENTS = {'nessian warhound': 'Hell Hound (Nessian)'}
SIGNED_NUMBER = r'[+\-–—−]\d+'


@dataclass(frozen=True)
class Catalog:
    """What the data of PCGen defines, for a generated race to refer to only that."""
    feats: catalog.KeyIndex
    choosing_feats: frozenset[str]
    mythic_feats: catalog.KeyIndex
    weapon_profs: catalog.KeyIndex
    skills: catalog.KeyIndex
    spells: catalog.KeyIndex


def to_creature(fields: dict[str, str]) -> dict:
    """A Combat Manager stat block in the shape of a pcfinder-csr creature."""
    def field(name: str) -> str:
        return (fields.get(name) or '').strip()

    scores = statblock.parse_ability_scores(field('AbilitiyScores'))
    spell_resistance = re.match(r'\d+', field('SR'))
    # Ability Focus picks one of the race's abilities, and the generated races define none
    feats = [feat for feat in statblock.split_list(field('Feats')) if not feat.startswith('Ability Focus')]
    return {
        'name': field('Name'),
        'cr': field('CR') or '1',
        'alignment': field('Alignment') or 'N',
        'size': field('Size') or 'Medium',
        'type': creature_type(field('Type')),
        'subTypes': statblock.split_list(field('SubType').strip('()')),
        'abilityScores': {key: scores[key.upper()] for key in STAT_KEYS},
        'numericHitDice': hit_dice(field('HD')),
        'armorClass': armor_class(field('AC')),
        'speed': speeds(field('Speed')),
        'reach': field('Reach'),
        'senses': field('Senses'),
        'immunities': statblock.split_list(field('Immune')),
        'spellResistance': int(spell_resistance.group()) if spell_resistance else None,
        'damageReduction': field('DR'),
        'parsedAttacks': {'melee': {'primary': {'attacks': natural_attacks(field('Melee'))}}},
        'feats': [feat_key(feat) for feat in feats],
        'mythicFeats': [feat_key(feat) for feat in feats if is_mythic(feat)],
        'skills': field('Skills'),
        'languages': statblock.split_list(field('Languages').split(';')[0]),
        # pcfinder puts each frequency on its own line; Combat Manager separates them with two spaces
        'spellLikeAbilities': re.sub(r'\s{2,}', '\n', re.sub(r'/\s+', '/', strip_marks(field('SpellLikeAbilities')))),
        'source': field('Source'),
    }


def creature_type(text: str) -> str:
    """The Pathfinder type a stat block names, "advanced magical beast" being a magical beast."""
    lowered = text.lower()
    found = next((creature_type for creature_type in CREATURE_TYPES if creature_type in lowered), lowered)
    return 'outsider' if found == 'elemental' else found


def strip_marks(text: str) -> str:
    """Combat Manager flags a feat or spell with "*" or "†" for a footnote."""
    return re.sub(r'[*†]', '', text)


def is_mythic(feat: str) -> bool:
    return '[M]' in feat or feat.startswith('Mythic ')


def feat_key(feat: str) -> str:
    """The feat a stat block names, without its marks; a mythic feat is keyed by the feat it improves."""
    return re.sub(r'^Mythic ', '', strip_marks(feat.replace('[M]', ''))).strip()


def hit_dice(text: str) -> int:
    declared = re.search(r'(\d+)\s*HD', text)
    if declared:
        return int(declared.group(1))
    return sum(int(count) for count in re.findall(r'(\d+)\s*d\s*\d+', text)) or 1


def armor_class(text: str) -> dict[str, int]:
    values = {'normal': r'^(\d+)', 'touch': r'touch (\d+)', 'flatFooted': r'flat-footed (\d+)'}
    found = {key: re.search(pattern, text) for key, pattern in values.items()}
    return {key: int(match.group(1)) for key, match in found.items() if match}


def speeds(text: str) -> list[dict]:
    moves = []
    for piece in statblock.split_list(text):
        match = re.match(rf'^(?:({"|".join(MOVE_TYPES)})\s+)?(\d+)\s*ft\.?\s*(?:\((\w+)\))?', piece, re.IGNORECASE)
        if not match:
            continue
        move = {'moveType': (match.group(1) or 'walk').lower(), 'value': int(match.group(2))}
        if match.group(3):
            move['maneuverability'] = match.group(3).lower()
        moves.append(move)
    return moves


def natural_attacks(text: str) -> list[dict]:
    """Every attack of the melee line, once per name across its alternatives."""
    attacks, names = [], set()
    for alternative in re.split(r'\s+or\s+', text):
        for piece in statblock.split_list(alternative):
            attack = parse_attack(piece)
            if attack and attack['name'] not in names:
                names.add(attack['name'])
                attacks.append(attack)
    return attacks


def parse_attack(text: str) -> dict | None:
    match = re.match(rf'^(?:(\d+)\s+)?(.+?)\s+{SIGNED_NUMBER}(?:/{SIGNED_NUMBER})*(?:\s+touch)?\s*\((.*)\)\s*$', text.strip())
    # A swarm's attack hits automatically, so its stat block gives no bonus: "swarm (2d6 plus distraction)"
    match = match or re.match(r'^()(swarm)\s*\((.*)\)\s*$', text.strip(), re.IGNORECASE)
    if not match:
        return None
    count, name, damage = match.groups()
    dice = re.match(r'^(\d+)d(\d+)', damage.strip())
    attack = {'name': name.lower(), 'quantity': int(count or 1),
              'damage': {'dice': {'count': int(dice.group(1)), 'sides': int(dice.group(2))} if dice else {}}}
    effects = re.split(r'\s+and\s+|,\s*', damage.split(' plus ', 1)[1]) if ' plus ' in damage else []
    if effects:
        attack['specialEffects'] = [{'name': effect.strip(), 'effectType': effect.strip().lower()} for effect in effects if effect.strip()]
    return attack


def exact(index: catalog.KeyIndex, name: str) -> str | None:
    return index.by_reading.get(name.strip().lower())


def feat_with_choice(name: str, feats: catalog.KeyIndex, pcgen: Catalog) -> str | None:
    """The PCGen key of a feat of `feats` and its choice, or None when PCGen lacks either."""
    whole = exact(feats, name)
    if whole:
        return whole
    chosen = re.match(r'^(.+?) \((.+)\)$', name)
    base = exact(feats, chosen.group(1)) if chosen else None
    if not base or (feats is pcgen.feats and base not in pcgen.choosing_feats):
        return None
    choice = chosen.group(2)
    if base in WEAPON_FEATS:
        choice = pcgen.weapon_profs.find(choice)
    elif base == 'Skill Focus':
        choice = exact(pcgen.skills, choice)
    return f'{base} ({choice})' if choice else None


def prune(creature: dict, abilities: dict, pcgen: Catalog) -> tuple[dict, dict, list[str]]:
    """The creature and its abilities rid of the feats, skills and spells PCGen does not define, and what went."""
    dropped = []
    feats = []
    for name in (name for feat in abilities.get('feats', []) for name in normalize_feat_names(feat)):
        key = feat_with_choice(name, pcgen.feats, pcgen)
        if key:
            feats.append(key)
        else:
            dropped.append(f'feat {name}')
    mythic_feats = [feat_with_choice(feat, pcgen.mythic_feats, pcgen) for feat in creature.get('mythicFeats', [])]
    dropped += [f'mythic feat {feat}' for feat, key in zip(creature.get('mythicFeats', []), mythic_feats) if not key]
    skills = {name: bonus for name, bonus in abilities.get('skills', {}).items()
              if any(exact(pcgen.skills, key) for key in normalize_kit_skills(name))}
    dropped += [f'skill {name}' for name in abilities.get('skills', {}) if name not in skills]
    pruned_abilities = {**abilities, 'feats': feats, 'skills': skills}
    if 'slas' in abilities:
        entries = {}
        for frequency, spells in abilities['slas']['entries'].items():
            known = [spell for spell in spells if exact(pcgen.spells, spell_key(spell['spell']))]
            dropped += [f"spell {spell['spell']}" for spell in spells if spell not in known]
            if known:
                entries[frequency] = known
        pruned_abilities['slas'] = {**abilities['slas'], 'entries': entries}
    return {**creature, 'mythicFeats': [key for key in mythic_feats if key]}, pruned_abilities, dropped


@functools.lru_cache(maxsize=None)
def key_segments(races: catalog.KeyIndex) -> frozenset[str]:
    """Each name a race key holds: "Rakshasa, Amanusya" holds "rakshasa" and "amanusya"."""
    segments = set()
    for key in races.keys:
        segments.update(piece.strip() for piece in re.split(r'[(),/]', key.lower()) if piece.strip())
    return frozenset(segments)


def is_race(name: str, races: catalog.KeyIndex) -> bool:
    lowered = name.strip().lower()
    return bool(races.find(name) or catalog.family_member(lowered, races) or lowered in key_segments(races))


def is_known(name: str, races: catalog.KeyIndex, templates: catalog.KeyIndex) -> bool:
    """Whether PCGen already has this monster, as a race, a template, or a race with a template."""
    name = name.strip()
    if is_race(name, races) or templates.find(name) or name.lower() in EQUIVALENTS:
        return True
    words = name.split()
    forms = [f"{' '.join(words[cut:])} ({' '.join(words[:cut])})" for cut in range(1, len(words))]
    comma = re.match(r'^(.+), (.+)$', name)
    if comma:
        forms.append(f'{comma.group(1)} ({comma.group(2)})')
    dragon = re.match(rf'^{DRAGON_AGES} (.+) dragon$', name, re.IGNORECASE)
    if dragon:
        forms.append(f'Dragon ({dragon.group(2)})')
    elemental = re.match(rf'^{ELEMENTAL_SIZES} (.+) elemental$', name, re.IGNORECASE)
    if elemental:
        forms += [f'Elemental ({elemental.group(2)}/{elemental.group(1)})', f'Elemental ({elemental.group(2)})']
    if any(is_race(form, races) for form in forms):
        return True
    return any((is_race(' '.join(words[:cut]), races) and catalog.resolve_template(' '.join(words[cut:]), templates))
               or (catalog.resolve_template(' '.join(words[:cut]), templates) and is_race(' '.join(words[cut:]), races))
               for cut in range(1, len(words)))


def normalized(name: str) -> str:
    return re.sub(r'[^a-z0-9]', '', name.lower().replace('&', 'and'))


def book_pcc(source: str, books: dict[str, Path]) -> Path | None:
    """The .pcc file of the PCGen book a Combat Manager source names, if PCGen has that book."""
    adventure_path = re.match(r'^AP (\d+)$', source.strip())
    name = f'Pathfinder Adventure Path #{adventure_path.group(1)}' if adventure_path else re.sub(r'^PFRPG ', '', source.strip())
    return books.get(normalized(name))
