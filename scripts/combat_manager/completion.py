"""Complete what a stat block leaves out, in keeping with the NPC: spare feats, bonus languages,
unspent skill points and spells still to learn.

A published NPC rarely uses every slot PCGen counts. Each gap is filled with a choice its class
and stat block suggest (a fighter's spare feat is Weapon Focus in the weapon it attacks with),
never with a published skill, so the totals the report compares stay the published ones.
"""

from __future__ import annotations

import collections
import re

import pcgen_client as pc
from pools import FEAT_POOL, IDENTITY_POOLS, POOLS_FILLED_ONLY_BY_NAME, POOLS_LEFT_ALONE, open_pools, option_keys

THEMES = {
    'martial': ('fighter', 'barbarian', 'ranger', 'paladin', 'antipaladin', 'cavalier', 'samurai', 'warrior', 'slayer',
                'brawler', 'swashbuckler', 'gunslinger', 'monk', 'bloodrager', 'skald', 'hellknight'),
    'arcane': ('wizard', 'sorcerer', 'witch', 'magus', 'arcanist', 'bard', 'summoner', 'alchemist', 'psychic', 'occultist'),
    'divine': ('cleric', 'druid', 'oracle', 'inquisitor', 'shaman', 'adept', 'warpriest', 'hunter', 'medium', 'spiritualist'),
    'skilled': ('rogue', 'expert', 'aristocrat', 'commoner', 'investigator', 'ninja', 'vigilante', 'mesmerist'),
}
GENERAL_FEATS = ['Toughness', 'Improved Initiative', 'Iron Will', 'Great Fortitude', 'Lightning Reflexes']
THEME_FEATS = {
    'martial': ['Power Attack', 'Dodge', 'Combat Reflexes', 'Endurance'],
    'arcane': ['Combat Casting', 'Spell Penetration', 'Extend Spell'],
    'divine': ['Combat Casting', 'Extra Channel', 'Selective Channeling'],
    'skilled': ['Dodge', 'Alertness', 'Weapon Finesse'],
}
THEME_LANGUAGES = {
    'martial': ['Orc', 'Giant', 'Goblin', 'Dwarven', 'Elven', 'Halfling'],
    'arcane': ['Draconic', 'Aklo', 'Elven', 'Infernal', 'Abyssal', 'Celestial', 'Sylvan'],
    'divine': ['Celestial', 'Infernal', 'Abyssal', 'Elven', 'Dwarven', 'Sylvan'],
    'skilled': ['Elven', 'Halfling', 'Gnome', 'Dwarven', 'Orc', 'Goblin'],
}
NATURE_CLASSES = ('druid', 'ranger', 'hunter', 'shaman')
NATURE_LANGUAGES = ['Sylvan', 'Elven', 'Gnome', 'Aquan', 'Terran', 'Auran']
CLASS_SKILLS = {
    'fighter': ['Climb', 'Intimidate', 'Ride', 'Survival', 'Swim', 'Handle Animal'],
    'barbarian': ['Survival', 'Intimidate', 'Climb', 'Acrobatics', 'Swim', 'Handle Animal'],
    'rogue': ['Stealth', 'Perception', 'Disable Device', 'Sleight of Hand', 'Escape Artist', 'Knowledge (Local)'],
    'wizard': ['Knowledge (Arcana)', 'Spellcraft', 'Knowledge (History)', 'Knowledge (Planes)', 'Linguistics', 'Appraise'],
    'sorcerer': ['Spellcraft', 'Knowledge (Arcana)', 'Bluff', 'Intimidate', 'Use Magic Device'],
    'cleric': ['Knowledge (Religion)', 'Heal', 'Diplomacy', 'Sense Motive', 'Spellcraft', 'Knowledge (History)'],
    'paladin': ['Diplomacy', 'Sense Motive', 'Heal', 'Knowledge (Religion)', 'Ride', 'Handle Animal'],
    'ranger': ['Survival', 'Perception', 'Stealth', 'Knowledge (Nature)', 'Handle Animal', 'Climb'],
    'druid': ['Knowledge (Nature)', 'Survival', 'Handle Animal', 'Heal', 'Perception'],
    'bard': ['Diplomacy', 'Bluff', 'Knowledge (Local)', 'Knowledge (History)', 'Sense Motive'],
    'monk': ['Acrobatics', 'Perception', 'Sense Motive', 'Knowledge (Religion)', 'Escape Artist', 'Climb'],
    'alchemist': ['Knowledge (Nature)', 'Spellcraft', 'Heal', 'Disable Device', 'Perception'],
    'inquisitor': ['Sense Motive', 'Intimidate', 'Knowledge (Religion)', 'Perception', 'Survival'],
    'oracle': ['Knowledge (History)', 'Diplomacy', 'Sense Motive', 'Heal', 'Knowledge (Religion)'],
    'witch': ['Knowledge (Arcana)', 'Heal', 'Spellcraft', 'Knowledge (Nature)', 'Intimidate'],
    'magus': ['Knowledge (Arcana)', 'Spellcraft', 'Climb', 'Intimidate', 'Swim'],
    'cavalier': ['Ride', 'Handle Animal', 'Diplomacy', 'Intimidate', 'Sense Motive'],
}
DEFAULT_SKILLS = ['Perception', 'Sense Motive', 'Diplomacy', 'Knowledge (Local)', 'Survival', 'Climb', 'Swim', 'Heal',
                  'Knowledge (Geography)', 'Knowledge (History)', 'Knowledge (Nobility)', 'Ride', 'Handle Animal', 'Appraise',
                  'Acrobatics', 'Intimidate', 'Bluff', 'Stealth', 'Linguistics']
EXPERT_SKILLS = ['Appraise', 'Diplomacy', 'Knowledge (Local)', 'Perception', 'Sense Motive', 'Bluff', 'Linguistics',
                 'Sleight of Hand', 'Use Magic Device', 'Heal']
CLASS_SKILL_POOL, CLASS_SKILL_ABILITY = 'Expert Class Skills', 'Expert Class Skills'
SPELL_TODO = 'in_splTodoRemain'
HIGHEST_SPELL_LEVEL = 9
# how many options a pool is tried with before it is left open
CANDIDATES_TRIED = 400
SPARE_PASSES = 3


def theme_of(class_names: list[str]) -> str:
    """The NPC's profile, from its class with the most levels (first in the list)."""
    for name in class_names:
        for theme, members in THEMES.items():
            if any(member in name.lower() for member in members):
                return theme
    return 'skilled'


def feat_candidates(theme: str, weapon: str | None, skills_by_total: list[str]) -> list[tuple[str, str | None]]:
    """Feats in keeping with the NPC, the most telling first: Weapon Focus in its weapon, Skill Focus in its best skill."""
    weapon_feats = [('Weapon Focus', weapon)] if weapon and theme in ('martial', 'skilled') else []
    skill_feats = [('Skill Focus', skill) for skill in skills_by_total[:3]]
    ordered = weapon_feats + ([] if theme == 'martial' else skill_feats[:1])
    ordered += [(feat, None) for feat in THEME_FEATS[theme] + GENERAL_FEATS]
    ordered += skill_feats + ([('Weapon Focus', weapon)] if weapon and theme not in ('martial', 'skilled') else [])
    return list(dict.fromkeys(ordered))


def language_ranking(theme: str, class_names: list[str]) -> list[str]:
    nature = any(name.lower().startswith(NATURE_CLASSES) for name in class_names)
    return list(dict.fromkeys((NATURE_LANGUAGES if nature else []) + THEME_LANGUAGES[theme]))


def skill_ranking(class_names: list[str], published: dict[str, int]) -> list[str]:
    """Skills the NPC's classes favour, leaving out the published ones so their totals do not move."""
    favoured = [skill for name in class_names for key, skills in CLASS_SKILLS.items() if key in name.lower() for skill in skills]
    return [skill for skill in dict.fromkeys(favoured + DEFAULT_SKILLS) if skill not in published]


def fillable(pool_key: str) -> bool:
    """A pool the import may complete: not an archetype, an ability adjustment, an identity or a pool left alone."""
    return (pool_key not in POOLS_LEFT_ALONE and not pool_key.endswith(IDENTITY_POOLS)
            and not any(mark in pool_key for mark in POOLS_FILLED_ONLY_BY_NAME)
            and 'Ability Adjustment' not in pool_key and pool_key != CLASS_SKILL_POOL)


def fill_spare_feats(character_id: str, theme: str, weapon: str | None, skills_by_total: list[str], report: dict) -> None:
    """Put a choice in keeping with the NPC in each slot its stat block leaves empty.

    The general feat pool takes the feats the NPC's profile suggests; a class pool (combat style,
    bloodline feat, mercy, ninja trick) takes one of those if it offers it, else its own options in order.
    """
    wanted = feat_candidates(theme, weapon, skills_by_total)
    # a choice can open another pool (a regular talent asks for the talent): a few passes fill those too
    for _ in range(SPARE_PASSES):
        if not fill_spare_pass(character_id, wanted, report):
            return


def fill_spare_pass(character_id: str, wanted: list[tuple[str, str | None]], report: dict) -> bool:
    """One pass over the open pools; True when something was added."""
    added_before = len(report['added_feats'])
    for pool in [pool for pool in open_pools(character_id) if fillable(pool['key'])]:
        if pool['key'] == FEAT_POOL:
            candidates = wanted
        else:
            option_of = {key.split('~')[-1].strip().casefold(): key for key in option_keys(character_id, pool['key'])}
            themed = [(option_of[key.casefold()], choice) for key, choice in wanted if key.casefold() in option_of]
            candidates = themed + [(key, None) for key in option_of.values() if (key, None) not in themed]
        slots = pool['remaining']
        for key, choice in candidates[:CANDIDATES_TRIED]:
            if slots <= 0:
                break
            pc.call('batch_add_abilities', character_id=character_id,
                    abilities=[{'category_key': pool['key'], 'ability_key': key, **({'choice': [choice]} if choice else {})}])
            # PCGen takes any choice, then drops one whose prerequisites are unmet: the slot tells which
            left = next((entry['remaining'] for entry in open_pools(character_id) if entry['key'] == pool['key']), 0)
            if left < slots:
                report['added_feats'].append(key + (f' ({choice})' if choice else ''))
                slots = left
    return len(report['added_feats']) > added_before


def fill_class_skill_choices(character_id: str, published: dict[str, int], report: dict) -> None:
    """An expert picks ten class skills: the published ones first, the trade's usual ones after."""
    remaining = next((pool['remaining'] for pool in open_pools(character_id) if pool['key'] == CLASS_SKILL_POOL), 0)
    chosen = list(dict.fromkeys(sorted(published, key=published.get, reverse=True) + EXPERT_SKILLS))[:remaining]
    if chosen:
        answer = pc.call('batch_add_abilities', character_id=character_id, abilities=[
            {'category_key': CLASS_SKILL_POOL, 'ability_key': CLASS_SKILL_ABILITY, 'choice': [skill]} for skill in chosen])
        report['refused'].extend(answer.get('errors', []))


def spend_skill_points(character_id: str, class_names: list[str], published: dict[str, int], report: dict) -> None:
    """Spend what the published skills leave, one rank a level in each skill the classes favour."""
    ranking = skill_ranking(class_names, published)
    if not ranking:
        return
    investments = []
    for index, level in enumerate(pc.call('get_skill_summary', character_id=character_id)['levels']):
        investments += [{'skill_key': ranking[point % len(ranking)], 'points': 1, 'level_index': index}
                        for point in range(int(level['remaining_skill_points']))]
    if investments:
        answer = pc.call('batch_invest_skills', character_id=character_id, investments=investments)
        report['added_skills'] = sorted({entry['skill_key'] for entry in investments})
        report['refused'].extend(answer.get('errors', [])[:5])


def learn_bonus_languages(character_id: str, theme: str, class_names: list[str], report: dict) -> None:
    """Learn the bonus languages Intelligence allows, those the NPC's profile suggests first."""
    choosers = pc.call('get_languages', character_id=character_id)['choosers']
    offered = [language for chooser in choosers if chooser['remaining'] > 0 for language in chooser['available']]
    # a race may offer only some languages: what PCGen offers comes after the profile's own
    for language in language_ranking(theme, class_names) + offered:
        choosers = pc.call('get_languages', character_id=character_id)['choosers']
        if not any(chooser['remaining'] > 0 for chooser in choosers):
            return
        try:
            pc.call('add_language', character_id=character_id, language_key=language)
            report['added_languages'].append(language)
        except pc.PcgenError:
            continue


def learn_missing_spells(character_id: str, class_keys: list[str], report: dict) -> None:
    """Learn the spells a spontaneous caster still has room for, from the school it knows best."""
    if SPELL_TODO not in pc.call('get_todo_list', character_id=character_id)['todo_list']:
        return
    known = pc.call('get_known_spells', character_id=character_id)
    known_names = {spell.get('key') or spell.get('name') for spell in (known if isinstance(known, list) else known.get('spells', []))}
    for class_key in class_keys:
        available = {level: pc.call('get_available_spells', character_id=character_id, class_filter=class_key, level_filter=str(level))
                     for level in range(HIGHEST_SPELL_LEVEL + 1)}
        schools = collections.Counter(spell['school'] for spells in available.values() for spell in spells if spell['key'] in known_names)
        for level, spells in available.items():
            ranked = sorted((spell for spell in spells if spell['key'] not in known_names), key=lambda spell: -schools[spell['school']])
            for spell in ranked:
                try:
                    pc.call('add_known_spell', character_id=character_id, spell_name=spell['key'], class_key=class_key, spell_level=str(level))
                    report['added_spells'].append(spell['key'])
                except pc.PcgenError as error:
                    if 'qualify' in error.message:
                        continue
                    break  # no room left at this level
        if SPELL_TODO not in pc.call('get_todo_list', character_id=character_id)['todo_list']:
            return


COMPANION_PATTERN = re.compile(r"(familiar|arcane bond|animal companion|nature bond|hunter.s bond|bonded mount|mount)\s*[\(\[]([^\)\]]*)[\)\]]", re.I)
COMPANION_TYPES = {'familiar': 'Familiar', 'arcane bond': 'Familiar', 'animal companion': 'Animal Companion',
                   'nature bond': 'Animal Companion', "hunter's bond": 'Animal Companion', 'bonded mount': 'Mount', 'mount': 'Mount'}
BONDED_OBJECTS = ('amulet', 'ring', 'staff', 'wand', 'weapon', 'sword', 'dagger', 'rod', 'object', 'item', 'quarterstaff',
                  'mace', 'holy symbol', 'headband', 'crown', 'orb', 'book', 'none', 'companion', 'effective', 'level', 'weapons')
# what a class keeps when its stat block names no animal
DEFAULT_COMPANIONS = {'Familiar': 'Raven', 'Companion': 'Wolf', 'Mount': 'Horse'}
COMPANION_TODO = 'in_companionTodoRemain'


def named_companion(special_qualities: str | None) -> tuple[str, str] | None:
    """The companion a stat block names: "familiar (toad)", "arcane bond (raven named Cain)", "animal companion [hawk]"."""
    for match in COMPANION_PATTERN.finditer(special_qualities or ''):
        # "mount (Jinfu [horse])" names the animal in brackets
        inner = match.group(2).split('[')[-1]
        animal = re.split(r'\bnamed\b|,|;', inner)[0].strip().lower()
        if animal and not any(word in animal for word in BONDED_OBJECTS):
            return COMPANION_TYPES[match.group(1).lower().replace('\u2019', "'")], animal
    return None


def bond_hint(special_qualities: str | None) -> str | None:
    """A wizard's arcane bond: the familiar its stat block names, else the object."""
    bond = re.search(r'arcane bond\s*[\(\[]([^\)\]]*)', special_qualities or '', re.I)
    if not bond:
        return None
    return 'Bonded Object' if any(word in bond.group(1).lower() for word in BONDED_OBJECTS) else 'Familiar'


def add_companion(character_id: str, special_qualities: str | None, report: dict) -> None:
    """Add the companion the stat block names, or a usual one when PCGen expects a companion it does not name."""
    named = named_companion(special_qualities)
    if not named and COMPANION_TODO not in pc.call('get_todo_list', character_id=character_id)['todo_list']:
        return
    companions = pc.call('get_companions', character_id=character_id)
    # the types the class grants count their companions; a follower or a bought mount is unlimited (-1)
    granted = [kind for kind, most in companions.get('max_companions', {}).items() if most and most > 0]
    def granted_type(kind: str) -> str | None:
        return next((offered for offered in granted if offered == kind), None) or next((offered for offered in granted if kind.split()[-1] in offered), None)
    defaults = [(kind, DEFAULT_COMPANIONS.get(kind, DEFAULT_COMPANIONS.get(kind.split()[-1], 'Wolf')).lower()) for kind in granted]
    # an animal PCGen does not offer (a dust mephit needs Improved Familiar) gives way to the usual one
    wanted = ([(granted_type(named[0]), named[1])] if named else []) + defaults
    for kind, animal in wanted:
        if not kind:
            continue
        for race in (animal.title(), f'Companion ({animal.title()})', f'{animal.title()} (Familiar)', f'{animal.title()} (Animal Companion)', f'{animal.title()} ({kind})'):
            try:
                pc.call('add_companion', character_id=character_id, companion_type=kind, companion_race=race)
                report['added_companion'] = f'{kind}: {race}'
                if named and animal != named[1]:
                    report['refused'].append(f'compagnon remplacé : {named[1]} par {race}')
                return
            except pc.PcgenError:
                continue
    if named or granted:
        report['refused'].append(f'compagnon introuvable : {named or granted}')


ADJUSTMENT_POOLS = (('Class Level +4 Ability Adjustment', 'best'), ('Class Level +2 Ability Adjustment', 'best'),
                    ('Class Level -2 Ability Adjustment', 'worst'))
STAT_NAMES = {'STR': 'Strength', 'DEX': 'Dexterity', 'CON': 'Constitution', 'INT': 'Intelligence', 'WIS': 'Wisdom', 'CHA': 'Charisma'}


def adjustment_order(published_scores: dict[str, int | None]) -> list[str]:
    """The abilities from the best published score to the worst; one the creature lacks comes last."""
    return [STAT_NAMES[stat] for stat in sorted(STAT_NAMES, key=lambda stat: -(published_scores.get(stat) or 0))]


def fill_class_level_adjustments(character_id: str, published_scores: dict[str, int | None], report: dict) -> None:
    """A creature with class levels raises its best abilities and lowers its worst; calibration then sets the scores."""
    order = adjustment_order(published_scores)
    used = set()
    for pool_key, end in ADJUSTMENT_POOLS:
        remaining = next((pool['remaining'] for pool in open_pools(character_id) if pool['key'] == pool_key), 0)
        ranked = order if end == 'best' else list(reversed(order))
        options = option_keys(character_id, pool_key) if remaining else []
        for ability in ranked:
            if remaining <= 0:
                break
            option = next((key for key in options if f' {ability} ' in f' {key} '), None)
            # each ability takes one adjustment: +4 on the best, +2 on the next ones, -2 on the worst
            if option and ability not in used:
                pc.call('batch_add_abilities', character_id=character_id, abilities=[{'category_key': pool_key, 'ability_key': option}])
                used.add(ability)
                remaining -= 1
