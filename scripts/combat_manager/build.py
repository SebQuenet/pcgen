"""Build one Combat Manager NPC as a PCGen character, and measure it against its stat block."""

from __future__ import annotations

import json
import re
from dataclasses import dataclass

import catalog
import equipment
import pcgen_client as pc
import statblock
from generate_bestiary import normalize_languages
from pools import fill_feats, fill_open_choices, open_pools
from skill_ranks import headband_skill, obsessive_bonus, plan_ranks
from records import NpcRecord

ALIGNMENT_KEYS = {'N': 'TN'}
SHEET_STAT_KEYS = {'FOR': 'STR', 'STR': 'STR', 'DEX': 'DEX', 'CON': 'CON', 'INT': 'INT', 'SAG': 'WIS', 'WIS': 'WIS', 'CHA': 'CHA'}
DEFAULT_SCORE = 10
CALIBRATION_PASSES = 2
SKILL_PASSES = 3
FAVORED_CLASS_BONUS_POOL = 'Favored Class Bonus'
FAVORED_HIT_POINT = 'Favored Class Bonus ~ Hit Point'
FAVORED_SKILL_RANK = 'Favored Class Bonus ~ Skill Rank'
# A wizard knows every cantrip, and PCGen says so when one is added
LYCANTHROPE_TEMPLATE, LYCANTHROPE_TYPE_POOL, LYCANTHROPE_KIND_POOL = 'Lycanthrope', 'Lycanthrope Type', 'Lycanthrope Kind'
HEADBAND_SKILL_POOL, HEADBAND_SKILL_ABILITY = 'Headband of Intellect Skill Choice', 'Headband of Intellect Skill Selection'
OBSESSIVE_SKILL_POOL = 'Gnome Obsessive Skill Bonus'
ALREADY_KNOWN_MARK = 'contains all spells'
NOT_FOUND_MARK = 'Spell not found: '
# Classes that prepare from a book of their own, so a spell must be known before it is prepared
SPELLBOOK_CLASSES = {'Wizard', 'Magus', 'Witch', 'Alchemist', 'Arcanist', 'Investigator'}


@dataclass(frozen=True)
class Catalogs:
    classes: catalog.KeyIndex
    races: catalog.KeyIndex
    templates: catalog.KeyIndex
    items: catalog.KeyIndex
    known_languages: dict[str, str]


def read_sheet(character_id: str) -> dict:
    sheet = pc.call('get_character_sheet', character_id=character_id, format='json')
    content = sheet.get('content', sheet)
    return json.loads(content) if isinstance(content, str) else content


def sheet_scores(sheet: dict) -> dict[str, dict]:
    return {SHEET_STAT_KEYS[name]: values for name, values in sheet['abilityScores'].items() if name in SHEET_STAT_KEYS}


def calibrate_scores(character_id: str, published: dict[str, int | None], report: dict) -> None:
    """Find the base scores that, once PCGen adds race, levels and items, give the published ones."""
    wanted = {stat: score for stat, score in published.items() if score is not None}
    for _ in range(CALIBRATION_PASSES):
        stats = pc.call('get_character_details', character_id=character_id)['stats']
        # PCGen shows "*" for a score the creature lacks, such as an undead's Constitution
        gaps = {stat: wanted[stat] - int(stats[stat]['total']) for stat in wanted
                if stat in stats and str(stats[stat]['total']).lstrip('-').isdigit()}
        if not any(gaps.values()):
            return
        bases = {stat: stats[stat]['base'] + gaps.get(stat, 0) for stat in stats}
        pc.call('set_all_ability_scores', character_id=character_id, scores=bases)
    report['refused'].append('caractéristiques : écart restant après recalibrage')


def invest_skills(character_id: str, published: dict[str, int], total_level: int, report: dict) -> None:
    """Move each skill's ranks until its total is the published one.

    One rank shows a skill's bonus besides ranks; a skill whose published total needs no
    rank (Alertness, a racial bonus) gives that rank back for the skills after it.
    """
    if not published or total_level == 0:
        return
    placed: dict[str, list[int]] = {}
    modifiers: dict[str, int] = {}
    errors: list[str] = []
    for _ in range(SKILL_PASSES):
        summary = pc.call('get_skill_summary', character_id=character_id)
        modifiers.update({entry['key'].lower(): entry['modifier'] for entry in summary['trained_skills']})
        wanted = {}
        for skill, total in published.items():
            modifier = modifiers.get(skill.lower())
            wanted[skill] = max(0, min(total_level, total - modifier)) if modifier is not None else 1
        investments = plan_ranks([level['remaining_skill_points'] for level in summary['levels']], placed, wanted)
        if not investments:
            break
        answer = pc.call('batch_invest_skills', character_id=character_id, investments=investments)
        errors = answer.get('errors', [])
    report['refused'].extend(errors)


def fill_skill_choices(character_id: str, published: dict[str, int], report: dict) -> None:
    """Name the skills a headband of vast intelligence and a gnome's obsession apply to, before ranks are placed."""
    remaining = {pool['key']: pool['remaining'] for pool in open_pools(character_id)}
    wanted = []
    unassigned = dict(published)
    for _ in range(int(remaining.get(HEADBAND_SKILL_POOL, 0))):
        skill = headband_skill(unassigned)
        if skill:
            wanted.append({'category_key': HEADBAND_SKILL_POOL, 'ability_key': HEADBAND_SKILL_ABILITY, 'choice': [skill]})
            unassigned.pop(skill)
    obsession = obsessive_bonus(published) if remaining.get(OBSESSIVE_SKILL_POOL) else None
    if obsession:
        wanted.append({'category_key': OBSESSIVE_SKILL_POOL, 'ability_key': obsession[0], 'choice': [obsession[1]]})
    if wanted:
        report['refused'].extend(pc.call('batch_add_abilities', character_id=character_id, abilities=wanted).get('errors', []))


def fill_favored_class_bonus(character_id: str, published_hp: int | None, report: dict) -> None:
    """A hit point for each one the build lacks, a skill rank for every other level."""
    pool = next((entry for entry in pc.call('list_ability_categories', character_id=character_id)
                 if entry['key'] == FAVORED_CLASS_BONUS_POOL), None)
    if not pool or pool['remaining'] <= 0:
        return
    missing_hp = max(0, (published_hp or 0) - read_sheet(character_id)['hp'])
    hit_points = min(pool['remaining'], missing_hp)
    choices = [FAVORED_HIT_POINT] * hit_points + [FAVORED_SKILL_RANK] * (pool['remaining'] - hit_points)
    answer = pc.call('batch_add_abilities', character_id=character_id,
                     abilities=[{'category_key': FAVORED_CLASS_BONUS_POOL, 'ability_key': key} for key in choices])
    report['refused'].extend(answer.get('errors', []))


def add_spells(character_id: str, blocks: list[statblock.SpellBlock], classes: catalog.KeyIndex, report: dict) -> None:
    for block in blocks:
        class_key, _ = catalog.resolve_class(statblock.ClassLevels(block.class_name.lower(), 1), classes)
        if not class_key:
            report['missing_spells'].append(f'classe de sorts inconnue : {block.class_name}')
            continue
        spells = [(name, count) for level in block.spells.values() for name, count in level.items()]
        if not block.is_prepared or class_key in SPELLBOOK_CLASSES:
            for name, _ in spells:
                try:
                    pc.call('add_known_spell', character_id=character_id, spell_name=name, class_key=class_key)
                except pc.PcgenError as error:
                    if ALREADY_KNOWN_MARK not in error.message:
                        report['missing_spells'].append(f'{name} ({class_key}) : {error.message[:300]}')
        if block.is_prepared:
            answer = pc.call('batch_add_prepared_spells', character_id=character_id, spells=[
                {'spell_name': name, 'class_key': class_key} for name, count in spells for _ in range(count)])
            errors = answer.get('errors', [])
            not_found = [error[len(NOT_FOUND_MARK):] for error in errors if error.startswith(NOT_FOUND_MARK)]
            report['missing_spells'].extend(error for error in errors if not error.startswith(NOT_FOUND_MARK))
            if not_found:
                retried = pc.call('batch_add_prepared_spells', character_id=character_id,
                                  spells=[{'spell_name': name} for name in not_found])
                report['missing_spells'].extend(retried.get('errors', []))


def compare(record: NpcRecord, sheet: dict, published_skills: dict[str, int], character_id: str) -> dict:
    """The published value and PCGen's, side by side, for the numbers a GM reads."""
    armor_class = (record.get('AC') or '').split(',')
    saves = {save['name']: save['total'] for save in sheet['saves']}
    save_totals = list(saves.values())
    trained = {entry['key']: entry['total'] for entry in pc.call('get_skill_summary', character_id=character_id)['trained_skills']}
    trained_by_lower = {key.lower(): total for key, total in trained.items()}
    comparison = {
        'hp': [statblock.parse_signed(record.get('HP')), sheet['hp']],
        'ac': [statblock.parse_signed(armor_class[0] if armor_class else None), sheet['ac']['total']],
        'touch': [statblock.parse_signed(armor_class[1] if len(armor_class) > 1 else None), sheet['ac']['touch']],
        'flat_footed': [statblock.parse_signed(armor_class[2] if len(armor_class) > 2 else None), sheet['ac']['flatFooted']],
        'fort': [statblock.parse_signed(record.get('Fort')), save_totals[0] if save_totals else None],
        'ref': [statblock.parse_signed(record.get('Ref')), save_totals[1] if len(save_totals) > 1 else None],
        'will': [statblock.parse_signed(record.get('Will')), save_totals[2] if len(save_totals) > 2 else None],
        'bab': [statblock.parse_signed(record.get('BaseAtk')), statblock.parse_signed(sheet['attack']['bab'])],
        'cmb': [statblock.parse_signed(record.get('CMB')), sheet['attack']['cmb']],
        'cmd': [statblock.parse_signed(record.get('CMD')), sheet['attack']['cmd']],
        'init': [statblock.parse_signed(record.get('Init')), sheet['initiative']],
        'scores': {stat: [published, sheet_scores(sheet).get(stat, {}).get('score')]
                   for stat, published in statblock.parse_unaffected_scores(record.get('AbilitiyScores'), record.get('BaseStatistics')).items()},
        'skills': {skill: [total, trained_by_lower.get(skill.lower())] for skill, total in published_skills.items()},
    }
    return with_base_statistics(comparison, record.get('BaseStatistics'))


def with_base_statistics(comparison: dict, base_statistics: str | None) -> dict:
    """PCGen builds the NPC without the spells, potions or rage the published numbers include: compare to those without."""
    for name, published in statblock.parse_base_statistics(base_statistics).items():
        comparison[name][0] = published
    return comparison


def add_lycanthropy(character_id: str, template_name: str, lycanthropy: tuple[str, str], report: dict) -> None:
    """PCGen hides the lycanthrope subraces: the Lycanthrope template asks for a type and a kind, which apply them."""
    lycanthrope_type, kind = lycanthropy
    try:
        pc.call('add_template', character_id=character_id, template_key=LYCANTHROPE_TEMPLATE)
        answer = pc.call('batch_add_abilities', character_id=character_id, abilities=[
            {'category_key': LYCANTHROPE_TYPE_POOL, 'ability_key': lycanthrope_type},
            {'category_key': LYCANTHROPE_KIND_POOL, 'ability_key': kind}])
    except pc.PcgenError as error:
        report['refused'].append(error.message[:200])
        return
    if answer.get('accepted_count', 0) < 2:
        report['missing_templates'].append(template_name)


def build_npc(record: NpcRecord, catalogs: Catalogs, report: dict) -> str:
    """Build the NPC and return its character id; what PCGen refused goes into `report`."""
    class_entries = statblock.parse_classes(record.get('Class') or '', set(catalogs.classes.by_reading))
    race_key, gender = catalog.resolve_race(record.get('Race') or '', catalogs.races)
    # "worm that walks" names a template: the race is the subtype's "(augmented human)", or a people's "(kuru)"
    race_template = None if race_key else catalog.resolve_template(re.sub(r'^(?:male|female) ', '', (record.get('Race') or '').lower()), catalogs.templates)
    if not race_key:
        race_key = catalog.race_of_subtype(record.get('SubType'), catalogs.races)
    if not race_key:
        raise LookupError(f'race inconnue : {record.get("Race")}')
    published_scores = statblock.parse_unaffected_scores(record.get('AbilitiyScores'), record.get('BaseStatistics'))

    character_id = pc.call('create_character', name=record.get('Name'))['character_id']
    pc.call('set_race', character_id=character_id, race_key=race_key)
    alignment = record.get('Alignment')
    if alignment:
        try:
            pc.call('set_alignment', character_id=character_id, alignment_key=ALIGNMENT_KEYS.get(alignment, alignment))
        except pc.PcgenError as error:
            report['refused'].append(error.message[:200])
    pc.call('set_all_ability_scores', character_id=character_id,
            scores={stat: score if score is not None else DEFAULT_SCORE for stat, score in published_scores.items()})

    total_level, hints = 0, []
    for entry in class_entries:
        class_key, school = catalog.resolve_class(entry, catalogs.classes)
        if not class_key:
            report['missing_classes'].append(f'{entry.name} {entry.levels}')
            continue
        pc.call('add_class_level', character_id=character_id, class_key=class_key, levels=entry.levels,
                **({'subclass': school} if school else {}))
        total_level += entry.levels
        hints += list(entry.archetypes) + (['Unchained'] if entry.is_unchained else [])
        if entry.deity:
            try:
                pc.call('set_deity', character_id=character_id, deity_key=entry.deity)
            except pc.PcgenError as error:
                report['refused'].append(error.message[:200])
    if total_level == 0:
        raise LookupError('aucune classe reconnue')
    for field_name in ('ClassArchetypes', 'Bloodline', 'Mystery', 'Patron', 'Spirit', 'ProhibitedSchools', 'SpellDomains'):
        hints += statblock.split_list(record.get(field_name))
    if race_template:
        try:
            pc.call('add_template', character_id=character_id, template_key=race_template)
        except pc.PcgenError as error:
            report['refused'].append(error.message[:200])
    for template_name in statblock.parse_templates(record.get('TemplatesApplied')):
        lycanthropy = statblock.parse_lycanthropy(template_name)
        if lycanthropy:
            add_lycanthropy(character_id, template_name, lycanthropy, report)
            continue
        template = catalog.resolve_template(template_name, catalogs.templates)
        if not template:
            report['missing_templates'].append(template_name)
            continue
        try:
            pc.call('add_template', character_id=character_id, template_key=template)
        except pc.PcgenError as error:
            report['refused'].append(error.message[:200])
    for domain in statblock.split_list(record.get('SpellDomains')):
        try:
            pc.call('add_domain', character_id=character_id, domain_key=domain)
        except pc.PcgenError as error:
            report['refused'].append(error.message[:200])

    primary_class = max(class_entries, key=lambda entry: entry.levels)
    highest_stat = max((stat for stat in published_scores if published_scores[stat] is not None), key=lambda stat: published_scores[stat])
    preferences = {'hints': hints, 'highest_stat': highest_stat,
                   'favored_class': catalog.resolve_class(primary_class, catalogs.classes)[0]}

    gear = statblock.parse_gear(record.get('Gear'), record.get('OtherGear'))
    equipment.buy_and_wear(character_id, gear, catalogs.items, record.get('Melee') or '', report)
    # the familiar of an arcane bond brings Alertness, so the class choices go before the feats
    fill_open_choices(character_id, preferences, report, include_feats=False)
    sheet = read_sheet(character_id)
    owned_names = {feat['name'] for feat in sheet['feats'] + sheet['autoFeats']}
    fill_feats(character_id, statblock.parse_feats(record.get('Feats')), owned_names, report)
    fill_open_choices(character_id, preferences, report, include_feats=False)
    calibrate_scores(character_id, published_scores, report)
    unaffected_hp = statblock.parse_base_statistics(record.get('BaseStatistics')).get('hp')
    fill_favored_class_bonus(character_id, unaffected_hp or statblock.parse_signed(record.get('HP')), report)

    published_skills = statblock.parse_skills(record.get('Skills'))
    fill_skill_choices(character_id, published_skills, report)
    invest_skills(character_id, published_skills, total_level, report)
    add_spells(character_id, statblock.parse_spells(record.get('SpellsKnown'), record.get('SpellsPrepared')), catalogs.classes, report)

    for language in normalize_languages(statblock.split_list(record.get('Languages')), catalogs.known_languages):
        try:
            pc.call('add_language', character_id=character_id, language_key=language)
        except pc.PcgenError:
            pass  # a language the race already grants is refused, and says nothing
    tactics = '\n\n'.join(f'{title}: {record.get(field)}' for field, title in
                          (('BeforeCombat', 'Avant le combat'), ('DuringCombat', 'Pendant le combat'), ('Morale', 'Moral'))
                          if record.get(field))
    biography = {key: value for key, value in (('gender', record.get('Gender') or gender), ('bio', tactics)) if value}
    if biography:
        pc.call('set_biography', character_id=character_id, **biography)

    report['comparison'] = compare(record, read_sheet(character_id), published_skills, character_id)
    return character_id
