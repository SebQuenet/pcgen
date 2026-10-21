"""Spend the choices a class or race leaves open, the way the stat block made them.

The pool rules come from the Zirnakaynin NPC build
(Pierrebougie/scenarios/2026-09-19-zirkayanin/module/npcs/tools/build_npcs.py);
here the stat block's archetypes, bloodline, domains and schools come first.
"""

from __future__ import annotations

import pcgen_client as pc

FEAT_POOL = 'FEAT'
STAT_NAMES = {'STR': 'Strength', 'DEX': 'Dexterity', 'CON': 'Constitution', 'INT': 'Intelligence', 'WIS': 'Wisdom', 'CHA': 'Charisma'}
STEADY_FEATS = ['Toughness', 'Iron Will', 'Improved Initiative', 'Lightning Reflexes', 'Great Fortitude', 'Alertness',
                'Dodge', 'Combat Reflexes', 'Weapon Finesse', 'Endurance', 'Skill Focus']
POOL_IS_WORTH_FILLING = ('Feat', 'Talent', 'Arcana', 'Discovery', 'Hex', 'Revelation', 'Rage Power', 'Domain', 'Bond',
                         'Bloodline', 'Channel', 'Mystery', 'Curse', 'Judgment', 'Favored Enemy', 'Favored Terrain',
                         'Combat Style', 'Selection', 'Performance', 'Weapon Training', 'Armor Training', 'Class Feature',
                         'Inquisition', 'School', 'Archetype', 'Ability Bonus', 'Favored Class', 'Patron', 'Spirit')
POOLS_LEFT_ALONE = {'Retraining', 'GM Awards', 'Afflictions', 'Drawbacks', 'Optional Rules Systems',
                    'Permanency Applied Effects', 'Favored Class Bonus', 'Region of Origin', 'Race Version', 'Traits', 'Pact'}
# A pool whose first option would change the class is only filled when the stat block names the option
POOLS_FILLED_ONLY_BY_NAME = ('Archetype',)
FILL_PASSES = 6
FEATS_GRANTED_OUTSIDE_PCGEN = {'Alertness'}


def worth_filling(category: str) -> bool:
    if category in POOLS_LEFT_ALONE or 'Ability Adjustment' in category:
        return False
    folded = category.casefold()
    return any(mark.casefold() in folded for mark in POOL_IS_WORTH_FILLING)


def option_names(character_id: str, category: str) -> dict[str, str]:
    """Each option's key and the name PCGen shows for it."""
    listed = pc.call('list_abilities', character_id=character_id, category_key=category)
    entries = listed if isinstance(listed, list) else listed.get('abilities', [])
    return {entry['key']: entry.get('name', entry['key']) if isinstance(entry, dict) else entry for entry in entries}


def option_keys(character_id: str, category: str) -> list[str]:
    return list(option_names(character_id, category))


def open_pools(character_id: str) -> list[dict]:
    return [entry for entry in pc.call('list_ability_categories', character_id=character_id) if entry['remaining'] > 0]


def refused_keys(errors: list[str], requested: list[dict]) -> set[str]:
    return {wanted['ability_key'] for wanted in requested for error in errors if wanted['ability_key'] in error}


def fill_feats(character_id: str, feats: list[tuple[str, str | None]], owned_names: set[str], report: dict) -> None:
    """Put each published feat in a bonus pool that takes it, and the rest in the general pool.

    PCGen accepts more feats than a pool has slots and quietly drops the surplus, so each
    request asks for no more than the slots left. A feat the character already has from
    its race, class or familiar, shown under its translated name, is not asked for again.
    """
    # a familiar grants Alertness without PCGen knowing, so it is the feat to go without
    pending = sorted(feats, key=lambda feat: feat[0] in FEATS_GRANTED_OUTSIDE_PCGEN)
    pools = [pool for pool in open_pools(character_id) if 'feat' in pool['key'].casefold() and worth_filling(pool['key'])]
    pools.sort(key=lambda pool: pool['key'] == FEAT_POOL)
    for pool in pools:
        offered = option_names(character_id, pool['key'])
        pending = [feat for feat in pending if feat[1] or offered.get(feat[0]) not in owned_names]
        chosen = [feat for feat in pending if feat[0] in offered][:pool['remaining']]
        if not chosen:
            continue
        requested = [{'category_key': pool['key'], 'ability_key': key, **({'choice': [choice]} if choice else {})}
                     for key, choice in chosen]
        answer = pc.call('batch_add_abilities', character_id=character_id, abilities=requested)
        refused = refused_keys(answer.get('errors', []), requested)
        report['refused_feats'].extend(answer.get('errors', []) if pool['key'] == FEAT_POOL else [])
        pending = [feat for feat in pending if feat not in chosen or feat[0] in refused]
    report['refused_feats'].extend(f'sans emplacement : {key}' + (f' ({choice})' if choice else '') for key, choice in pending)


def matches_hint(option: str, hints: list[str]) -> bool:
    tail = option.split('~')[-1].strip().casefold()
    return any(hint.casefold() == tail or hint.casefold() in tail.split() for hint in hints if hint)


def preferred_options(category: str, options: list[str], preferences: dict) -> list[str]:
    """The options to try for a pool, best first; empty when the pool is better left open."""
    hints = preferences['hints']
    if category == 'Favored Class':
        wanted = [option for option in options if option == preferences['favored_class']]
    elif category == 'Ability Bonus':
        wanted = [f'+2 {STAT_NAMES[preferences["highest_stat"]]}']
    elif 'class selection' in category.casefold():
        mark = 'unchained' if any(hint.casefold() == 'unchained' for hint in hints) else 'standard'
        wanted = [option for option in options if mark in option.casefold()]
    else:
        wanted = [option for option in options if matches_hint(option, hints)]
        if not wanted and any(mark in category for mark in POOLS_FILLED_ONLY_BY_NAME):
            return []
        if 'feat' in category.casefold():
            wanted += STEADY_FEATS
    ranked = [option for option in wanted if option in options]
    return ranked + [option for option in options if option not in ranked]


def fill_open_choices(character_id: str, preferences: dict, report: dict, include_feats: bool = True) -> None:
    """Spend what is still open, one batch per pass: filling one pool often opens another."""
    exhausted: set[str] = set() if include_feats else {pool['key'] for pool in open_pools(character_id) if 'feat' in pool['key'].casefold()}
    for _ in range(FILL_PASSES):
        pools = [pool for pool in open_pools(character_id) if worth_filling(pool['key']) and pool['key'] not in exhausted]
        wanted = []
        for pool in pools:
            ranked = preferred_options(pool['key'], option_keys(character_id, pool['key']), preferences)
            if not ranked:
                exhausted.add(pool['key'])
                continue
            # more candidates than slots, since PCGen refuses those whose prerequisites are unmet
            wanted.extend({'category_key': pool['key'], 'ability_key': key} for key in ranked[:pool['remaining'] * 4 + 4])
        if not wanted:
            return
        answer = pc.call('batch_add_abilities', character_id=character_id, abilities=wanted)
        if answer.get('accepted_count', 0) == 0:
            exhausted.update(pool['key'] for pool in pools)
    report['open_pools'] = [pool['key'] for pool in open_pools(character_id) if worth_filling(pool['key'])]
