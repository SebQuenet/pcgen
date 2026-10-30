"""Translate a PCGen character (.pcg) into a kit (LST) that builds the same character.

Spec: specs/donnees-zirnakaynin.md, section 4
"""

from __future__ import annotations

import re
from collections import defaultdict
import dataclasses
from dataclasses import dataclass
from typing import Callable

STAT_ORDER = ('STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA')
# The equipment set slots a kit names as they are; any other slot becomes Equipped
KEPT_LOCATIONS = {'Both Hands', 'Primary Hand', 'Secondary Hand', 'Double Weapon', 'Shield', 'Carried', 'Not Carried'}
EQUIPPED = 'Equipped'
CARRIED = 'Carried'
NO_DEITY = 'None'
CLASS_SPELL_SOURCE = 'TYPE:CLASS'
FEAT = 'FEAT'
ESCAPED_PIPE = '&pipe;'
ABILITY_CHOICE = r'^CATEGORY=(.+?)&pipe;(.+)$'
PCG_ESCAPES = {ESCAPED_PIPE: '|', '&lbracket;': '[', '&rbracket;': ']', '&colon;': ':', '&comma;': ','}
# PCGen names an item resized for a creature "AUTOSIZE" + the size's letter + the item's key
AUTOSIZE_PREFIX = r'^AUTOSIZE([FDTSMLHGC])(.+)$'


@dataclass(frozen=True)
class ChosenAbility:
    category: str
    key: str
    choices: tuple[str, ...]
    is_feat: bool = False
    # Granted by a selection ability rather than bought from its category's pool
    is_free: bool = False


@dataclass(frozen=True)
class Item:
    key: str
    quantity: int
    eqmods: tuple[str, ...]
    display_name: str
    base_item: str
    size: str


@dataclass(frozen=True)
class PreparedSpell:
    book: str
    caster_class: str
    spell: str
    times: int


@dataclass(frozen=True)
class Npc:
    name: str
    gender: str
    age: str
    race: str
    alignment: str
    deity: str
    domains: tuple[str, ...]
    stats: dict
    classes: tuple[tuple[str, str, int], ...]
    abilities: tuple[ChosenAbility, ...]
    skill_ranks: dict
    items: tuple[Item, ...]
    item_locations: dict
    languages: tuple[str, ...]
    spells: tuple[PreparedSpell, ...]
    templates: tuple[str, ...]
    campaigns: tuple[str, ...]


@dataclass(frozen=True)
class Catalog:
    """What the loaded data defines: is_defined(kind, key), the templates a player can choose,
    the monster class levels each race grants, as race -> (class, levels), the prestige classes, and the
    abilities each prestige class requires, as class -> abilities, the ability pools each ability opens,
    as ability -> pools, and the background skills, which only Pathfinder Unchained's background skills rule offers."""
    is_defined: Callable[[str, str], bool]
    visible_templates: frozenset
    racial_levels: dict
    prestige_classes: frozenset
    prestige_requirements: dict
    pools_opened: dict
    background_skills: frozenset = frozenset()


def fields_of(line: str) -> dict[str, str]:
    """The TAG:value fields of a .pcg line, bracketed values kept whole; repeated tags keep the first."""
    fields: dict[str, str] = {}
    for field in split_top_level(line):
        tag, _, text = field.partition(':')
        fields.setdefault(tag, text)
    return fields


def split_top_level(line: str) -> list[str]:
    parts, depth, current = [], 0, ''
    for character in line:
        if character == '|' and depth == 0:
            parts.append(current)
            current = ''
            continue
        depth += {'[': 1, ']': -1}.get(character, 0)
        current += character
    parts.append(current)
    return parts


def parse_pcg(text: str) -> Npc:
    lines = text.splitlines()
    single = {}
    for line in lines:
        tag, _, rest = line.partition(':')
        single.setdefault(tag, rest)

    def tagged(tag):
        return [line for line in lines if line.startswith(tag + ':')]

    return Npc(
        name=single.get('CHARACTERNAME', ''),
        gender=single.get('GENDER', ''),
        age=single.get('AGE', ''),
        race=single.get('RACE', ''),
        alignment=single.get('ALIGN', ''),
        deity=single.get('DEITY', '').split('|')[0],
        domains=tuple(fields_of(line)['DOMAIN'] for line in tagged('DOMAIN')),
        stats={fields_of(line)['STAT']: fields_of(line)['SCORE'] for line in tagged('STAT')},
        classes=tuple(parse_class(line) for line in tagged('CLASS')),
        abilities=tuple(ability for line in tagged('ABILITY') for ability in parse_ability(line)),
        skill_ranks={name: ranks for line in tagged('SKILL') for name, ranks in [parse_skill(line)] if ranks},
        items=tuple(parse_item(line) for line in tagged('EQUIPNAME')),
        item_locations=equipped_locations(tagged('EQUIPSET')),
        languages=tuple(field.removeprefix('LANGUAGE:') for line in tagged('LANGUAGE') for field in line.split('|')),
        spells=tuple(spell for line in tagged('SPELLNAME') for spell in [parse_spell(line)] if spell),
        templates=tuple(re.match(r'TEMPLATESAPPLIED:\[NAME:([^|\]]+)', line).group(1) for line in tagged('TEMPLATESAPPLIED')),
        campaigns=tuple(field.removeprefix('CAMPAIGN:') for line in tagged('CAMPAIGN') for field in line.split('|')),
    )


def parse_class(line: str) -> tuple[str, str, int]:
    fields = fields_of(line)
    return fields['CLASS'], fields.get('SUBCLASS', ''), int(fields['LEVEL'])


def parse_ability(line: str) -> list[ChosenAbility]:
    """The ability a line holds; when its choice is another ability (CATEGORY=x&pipe;y), that ability instead,
    since a kit splits ABILITY on pipes and cannot write such a choice."""
    fields = fields_of(line)
    applied_to = fields['APPLIEDTO'].split(',') if 'APPLIEDTO' in fields else []
    # A bonus taken several times without a choice keeps one empty choice per time taken
    choices = [choice for choice in applied_to if choice] or (applied_to if len(applied_to) > 1 else [])
    chosen_abilities = [re.match(ABILITY_CHOICE, choice) for choice in choices]
    if any(chosen_abilities):
        return [ChosenAbility(match.group(1), match.group(2), (), is_free=True) for match in chosen_abilities if match]
    return [ChosenAbility(fields['ABILITY'], fields['KEY'], tuple(choices), fields.get('CATEGORY') == FEAT)]


def parse_skill(line: str) -> tuple[str, dict[str, float]]:
    """A skill and the ranks bought with each class."""
    ranks: dict[str, float] = defaultdict(float)
    for class_name, bought in re.findall(r'CLASSBOUGHT:\[CLASS:([^|\]]+)\|RANKS:([0-9.]+)', line):
        ranks[class_name] += float(bought)
    return fields_of(line)['SKILL'], {class_name: bought for class_name, bought in ranks.items() if bought}


def parse_item(line: str) -> Item:
    fields = fields_of(line)
    name = fields['EQUIPNAME']
    customization = re.search(r'DATA:([^\]]*)', fields.get('CUSTOMIZATION', ''))
    data = dict(entry.split('=', 1) for entry in customization.group(1).split('$') if '=' in entry) if customization else {}
    eqmods = tuple(unescape(eqmod) for eqmod in re.findall(r'EQMOD=([^$\]]+)', customization.group(1))) if customization else ()
    base_item = re.search(r'BASEITEM:([^|\]]+)', fields.get('CUSTOMIZATION', ''))
    resized = re.match(AUTOSIZE_PREFIX, data.get('KEY', name))
    key, size = (resized.group(2), resized.group(1)) if resized else (data.get('KEY', name), '')
    return Item(key, round(float(fields.get('QUANTITY', '1'))), eqmods, name, base_item.group(1) if base_item else '', size)


def unescape(text: str) -> str:
    """The characters a .pcg escapes inside a field, restored."""
    for escaped, character in PCG_ESCAPES.items():
        text = text.replace(escaped, character)
    return text


def equipped_locations(equipset_lines: list[str]) -> dict[str, str]:
    """Where the first equipment set puts each item, by item name."""
    locations = {}
    for line in equipset_lines:
        fields = fields_of(line)
        if 'VALUE' in fields and fields['ID'].startswith('0.1.'):
            slot = fields['EQUIPSET']
            locations.setdefault(fields['VALUE'], slot if slot in KEPT_LOCATIONS else EQUIPPED)
    return locations


def parse_spell(line: str) -> PreparedSpell | None:
    fields = fields_of(line)
    if CLASS_SPELL_SOURCE not in fields.get('SOURCE', ''):
        return None
    return PreparedSpell(fields['BOOK'], fields['CLASS'], fields['SPELLNAME'], int(fields.get('TIMES', '1')))


def number(value: float) -> str:
    return str(int(value)) if float(value).is_integer() else str(value)


def kit_lines(npc: Npc, catalog: Catalog) -> tuple[list[str], list[tuple[str, str]]]:
    """The kit's lines, and the references left out because no loaded book defines them."""
    dropped: list[tuple[str, str]] = []

    def known(kind: str, key: str) -> bool:
        if catalog.is_defined(kind, key):
            return True
        dropped.append((kind, key))
        return False

    lines = [f'STARTPACK:{npc.name}\tTYPE:NPC.Zirnakaynin\tVISIBLE:QUALIFY\tEQUIPBUY:0']
    if known('RACE', npc.race):
        lines.append(f'RACE:{npc.race}')
    lines += [f'NAME:{npc.name}'] + [f'{tag}:{value}' for tag, value in (('GENDER', npc.gender), ('AGE', npc.age)) if value]
    lines.append(f'ALIGN:{npc.alignment}')
    lines.append('STAT:' + '|'.join(f'{stat}={npc.stats[stat]}' for stat in STAT_ORDER if stat in npc.stats))
    lines += class_lines(npc, catalog, known, prestige=False)
    if npc.deity and npc.deity != NO_DEITY and known('DEITY', npc.deity):
        lines.append(f'DEITY:{npc.deity}' + ('\tDOMAIN:' + '|'.join(npc.domains) if npc.domains else ''))
    lines += [f'TEMPLATE:{template}' for template in npc.templates if template in catalog.visible_templates]
    # A prestige class requires feats and ranks, so it comes after them; its levels bring feats and ranks
    # of their own, so the other feats and the ranks bought at its levels come after it
    chosen = [without_background_skills(ability, catalog.background_skills, dropped)
              for ability in npc.abilities if known('ABILITY', ability.key)]
    abilities = pools_opened_first([ability for ability in chosen if ability], catalog.pools_opened)
    prestige = class_lines(npc, catalog, known, prestige=True)
    required = set().union(*(catalog.prestige_requirements.get(class_name, ()) for class_name, _, _ in npc.classes))
    used_pools = {ability.category for ability in abilities}
    later = [ability for ability in abilities if prestige and ability.is_feat and ability.key not in required
             and not used_pools & catalog.pools_opened.get(ability.key, frozenset())]
    skills = {skill: ranks for skill, ranks in npc.skill_ranks.items() if known('SKILL', skill)}
    lines += ability_lines([ability for ability in abilities if ability not in later])
    lines += skill_lines(skills, lambda class_name: class_name not in catalog.prestige_classes, known)
    lines += prestige + ability_lines(later)
    lines += skill_lines(skills, lambda class_name: class_name in catalog.prestige_classes, known)
    languages = [language for language in npc.languages if known('LANGUAGE', language)]
    if languages:
        lines.append('LANGBONUS:' + '|'.join(languages))
    for item in npc.items:
        key = item.key if catalog.is_defined('EQUIPMENT', item.key) or not item.base_item else item.base_item
        if known('EQUIPMENT', key):
            location = npc.item_locations.get(item.display_name, CARRIED)
            lines.append(f'GEAR:{key}\tQTY:{item.quantity}\tLOCATION:{location}'
                         + (f'\tSIZE:{item.size}' if item.size else '')
                         + (f'\tEQMOD:{".".join(item.eqmods)}' if item.eqmods else ''))
    lines += spell_lines(npc.spells, known)
    return lines, dropped


def without_background_skills(ability: ChosenAbility, background_skills: frozenset, dropped: list) -> ChosenAbility | None:
    """The ability less its choices of a background skill, which a kit loaded without that rule cannot select;
    None when no choice is left."""
    left_out = [choice for choice in ability.choices if choice in background_skills]
    if not left_out:
        return ability
    dropped += [('SKILL', choice) for choice in left_out]
    choices = tuple(choice for choice in ability.choices if choice not in background_skills)
    return dataclasses.replace(ability, choices=choices) if choices else None


def class_lines(npc: Npc, catalog: Catalog, known, prestige: bool) -> list[str]:
    """The base classes, or the prestige classes, less the monster levels the race grants."""
    lines = []
    racial_class, racial_level = catalog.racial_levels.get(npc.race, ('', 0))
    for class_name, subclass, level in npc.classes:
        if (class_name in catalog.prestige_classes) != prestige:
            continue
        if class_name == racial_class:
            level -= racial_level
        if level > 0 and known('CLASS', class_name):
            lines.append(f'CLASS:{class_name}\tLEVEL:{level}' + (f'\tSUBCLASS:{subclass}' if subclass else ''))
    return lines


def ability_lines(abilities: list[ChosenAbility]) -> list[str]:
    return [f'ABILITY:CATEGORY={ability.category}|{ability.key}' + (f' ({choice})' if choice else '') + ('\tFREE:YES' if ability.is_free else '')
            for ability in abilities for choice in ability.choices or ('',)]


def skill_lines(skills: dict, bought_with: Callable[[str], bool], known) -> list[str]:
    """One line per class that paid for ranks, so the kit spends that class's skill points."""
    lines = []
    for skill, ranks_by_class in skills.items():
        for class_name, ranks in ranks_by_class.items():
            if ranks and bought_with(class_name):
                paid_by = f'\tCLASS:{class_name}' if known('CLASS', class_name) else ''
                lines.append(f'SKILL:{skill}\tRANK:{number(ranks)}{paid_by}')
    return lines


def pools_opened_first(abilities: list[ChosenAbility], pools_opened: dict) -> list[ChosenAbility]:
    """The abilities in their order, except that a kit applies its lines in order and can take an ability only
    once the ability opening its pool, such as Nature's Bond for a druid domain, is taken."""
    ordered: list[ChosenAbility] = []
    waiting = list(abilities)
    while waiting:
        ready = next((ability for ability in waiting
                      if not any(ability.category in pools_opened.get(other.key, ()) for other in waiting if other is not ability)),
                     waiting[0])
        ordered.append(ready)
        waiting.remove(ready)
    return ordered


def spell_lines(spells, known) -> list[str]:
    grouped: dict[tuple[str, str], dict[str, int]] = defaultdict(dict)
    for spell in spells:
        if known('SPELL', spell.spell):
            counts = grouped[(spell.book, spell.caster_class)]
            counts[spell.spell] = counts.get(spell.spell, 0) + spell.times
    return [f'SPELLS:SPELLBOOK={book}|CLASS={caster_class}|'
            + '|'.join(spell if count == 1 else f'{spell}={count}' for spell, count in counts.items())
            for (book, caster_class), counts in grouped.items()]
