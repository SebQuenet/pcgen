"""Mythic NPCs: Combat Manager lists a mythic path as a class ("alchemist 11/trickster 4").

In PCGen a path is no class: each tier is one "Mythic Level" ability, and the path, its
abilities and the mythic feats are chosen from the pools the tiers open.
"""

from __future__ import annotations

import re

import pcgen_client as pc
import statblock

MYTHIC_PATHS = ('archmage', 'champion', 'guardian', 'hierophant', 'marshal', 'trickster')
TIER_POOL = 'Mythic Tier'
TIER_KEY = 'Mythic Level'
PATH_POOL = 'Mythic Path'
FEAT_POOL = 'Mythic Feat'
# the stat block fields that name the abilities a path grants; Combat Manager sometimes runs SQ into Gear
ABILITY_FIELDS = ('SQ', 'SpecialAttacks', 'DefensiveAbilities', 'Gear')
MARK = re.compile(r'\s*[\[(]M[\])]')
PREFIX = re.compile(r'^mythic (.+)$', re.I)


def path_entry(record, class_entries: list[statblock.ClassLevels]) -> statblock.ClassLevels | None:
    """The class entry that is a mythic path; "druid (ancient guardian)" names an archetype, not one."""
    if record.get('Mythic') != '1':
        return None
    return next((entry for entry in class_entries if entry.name.lower() in MYTHIC_PATHS), None)


def marked(entry: str) -> tuple[bool, str]:
    """Whether a feat entry is a mythic version, and the entry without its mark: "Dodge[M]", "Mythic Dodge"."""
    prefixed = PREFIX.match(entry)
    return bool(MARK.search(entry) or prefixed), MARK.sub('', prefixed.group(1) if prefixed else entry)


def implied_feats(feats_text: str | None, options: set[str]) -> list[tuple[str, str | None]]:
    """"Mythic Dodge" alone still means Dodge, which the mythic feat requires."""
    feats = []
    for entry in statblock.split_list(feats_text):
        is_mythic, base = marked(entry)
        if is_mythic and PREFIX.match(entry) and statblock.parse_feats(entry)[0][0] not in options:
            feats += statblock.parse_feats(base)
    return feats


def requested_feats(feats_text: str | None, unplaced: list[tuple[str, str | None]], options: set[str]) -> list[tuple[str, str | None]]:
    """The mythic versions the stat block marks, and the mythic-only feats no ordinary pool took."""
    feats = []
    for entry in statblock.split_list(feats_text):
        is_mythic, base = marked(entry)
        if PREFIX.match(entry) and statblock.parse_feats(entry)[0][0] in options:
            continue  # "Mythic Spell Lore" is a feat of its own, found among the unplaced
        if is_mythic:
            feats += statblock.parse_feats(base)
    return feats + [feat for feat in unplaced if feat[0] in options and feat not in feats]


def ability_hints(record) -> list[str]:
    return [hint for field_name in ABILITY_FIELDS for hint in statblock.split_list(record.get(field_name))]


def add_tiers(character_id: str, path: statblock.ClassLevels, report: dict) -> None:
    try:
        for _ in range(path.levels):
            pc.call('add_ability', character_id=character_id, category_key=TIER_POOL, ability_key=TIER_KEY)
        pc.call('add_ability', character_id=character_id, category_key=PATH_POOL,
                ability_key=f'{PATH_POOL} ~ {path.name.title()}')
    except pc.PcgenError as error:
        report['refused'].append(error.message[:200])


def add_feats(character_id: str, feats: list[tuple[str, str | None]], report: dict) -> None:
    """Each mythic feat in the mythic feat pool, keyed as its ordinary feat."""
    if not feats:
        return
    requested = [{'category_key': FEAT_POOL, 'ability_key': key, **({'choice': [choice]} if choice else {})} for key, choice in feats]
    answer = pc.call('batch_add_abilities', character_id=character_id, abilities=requested)
    report['refused_feats'].extend(f'mythique : {error}' for error in answer.get('errors', []))
