#!/usr/bin/env python3
"""Copy Zirnakaynin's NPCs from the Pierrebougie scenario into data/homebrew/zirnakaynin, with one kit each.

    python3 scripts/import_zirnakaynin_npcs.py ravael-torai gharhaz   # add these NPCs
    python3 scripts/import_zirnakaynin_npcs.py --all                  # add every NPC of the scenario
    python3 scripts/import_zirnakaynin_npcs.py                        # only rewrite the kits and the .pcc

Spec: specs/donnees-zirnakaynin.md
"""

from __future__ import annotations

import argparse
import re
import shutil
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

import zirnakaynin_kit

REPOSITORY = Path(__file__).resolve().parent.parent
DATA = REPOSITORY / 'data'
PATHFINDER = DATA / 'pathfinder'
DESTINATION = DATA / 'homebrew' / 'zirnakaynin'
NPC_DIRECTORY = DESTINATION / 'pnj'
KIT_DIRECTORY = DESTINATION / 'kits'
PCC = DESTINATION / '_zirnakaynin.pcc'
SCENARIO_NPCS = Path.home() / 'Documents/projects/Pierrebougie/scenarios/2026-09-19-zirkayanin/module/npcs'
SCENARIO_FOLDERS = ('nommes', 'generiques')
# The kind of object each .pcc file token loads; FEAT files hold abilities
FILE_KINDS = {'RACE': 'RACE', 'CLASS': 'CLASS', 'ABILITY': 'ABILITY', 'FEAT': 'ABILITY', 'SKILL': 'SKILL',
              'EQUIPMENT': 'EQUIPMENT', 'SPELL': 'SPELL', 'TEMPLATE': 'TEMPLATE', 'DEITY': 'DEITY', 'LANGUAGE': 'LANGUAGE'}
NO_RANK = 999999
PCC_HEADER = """CAMPAIGN:Zirnakaynin
KEY:Zirnakaynin
GAMEMODE:Pathfinder
GENRE:Fantasy
BOOKTYPE:Homebrew
SETTING:Golarion
TYPE:Homebrew.Zirnakaynin
PUBNAMELONG:Pierrebougie Campaign
PUBNAMESHORT:Pierrebougie
SOURCELONG:Zirnakaynin
SOURCESHORT:ZIR
SOURCEDATE:2026-10
RANK:202610
ISOGL:NO
COPYRIGHT:Pierrebougie Campaign, Copyright 2026

INFOTEXT:The non-player characters of Zirnakaynin met in the Pierrebougie campaign, each as a kit. A kit appears once the books it needs are loaded. The characters themselves are in the pnj folder, as .pcg files.
"""


@dataclass(frozen=True)
class Book:
    key: str
    name: str
    rank: int


@dataclass(frozen=True)
class DataIndex:
    """Each object key of data/pathfinder by kind, with the books that load its definition."""
    books_by_key: dict
    visible_templates: frozenset
    racial_levels: dict
    prestige_classes: frozenset
    prestige_requirements: dict
    pools_opened: dict

    def is_defined(self, kind: str, key: str) -> bool:
        return key in self.books_by_key.get(kind, {})


def pcc_entries(pcc: Path) -> list[tuple[str, str]]:
    entries = []
    for line in pcc.read_text(encoding='utf-8', errors='replace').splitlines():
        tag, separator, value = line.partition(':')
        if separator and not line.startswith('#'):
            entries.append((tag.strip(), value.strip()))
    return entries


def resolve(pcc: Path, reference: str) -> Path | None:
    path = reference.split('|')[0].strip().replace('\\', '/')
    if path.startswith('@'):
        return (DATA / path[1:].lstrip('/')).resolve()
    if path.startswith(('*', '&')):
        return None
    return (pcc.parent / path).resolve()


def object_keys(path: Path, kind: str) -> list[tuple[str, bool]]:
    """The key of each object a LST file defines, and whether a player can see it."""
    found = []
    for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
        columns = line.split('\t')
        name = columns[0].strip()
        if kind == 'CLASS':
            if not name.startswith('CLASS:'):
                continue
            name = name.removeprefix('CLASS:')
        if not name or name.startswith('#') or re.search(r'\.(MOD|FORGET)$', name) or (':' in name and '.COPY=' not in name):
            continue
        name = name.split('.COPY=')[-1]
        key = next((column[4:].strip() for column in columns if column.startswith('KEY:')), name)
        visible = not any(column.strip() == 'VISIBLE:NO' for column in columns)
        found += [(key, visible), (name, visible)]
    return found


def racial_levels(path: Path) -> dict:
    """The monster class levels each race of a LST file grants, as race -> (class, levels)."""
    granted = {}
    for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
        columns = line.split('\t')
        monster_class = re.search(r'\tMONSTERCLASS:([^:\t]+):(\d+)', line)
        if monster_class and not columns[0].startswith('#'):
            race = next((column[4:].strip() for column in columns if column.startswith('KEY:')), columns[0].split('.')[0].strip())
            granted.setdefault(race, (monster_class.group(1), int(monster_class.group(2))))
    return granted


def prestige_classes(path: Path) -> set:
    """The classes of a LST file whose TYPE makes them prestige classes."""
    return {line.split('\t')[0].removeprefix('CLASS:').strip()
            for line in path.read_text(encoding='utf-8', errors='replace').splitlines()
            if line.startswith('CLASS:') and re.search(r'\tTYPE:[^\t]*Prestige', line)}


def required_abilities(path: Path, classes: set) -> dict:
    """Every ability or feat name the PREABILITY and PREFEAT tokens of each of these classes mention."""
    names = defaultdict(set)
    for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
        class_name = line.split('\t')[0].removeprefix('CLASS:').strip()
        if class_name in classes:
            for requirement in re.findall(r'PRE(?:ABILITY|FEAT):([^\t\]]+)', line):
                names[class_name] |= {name.strip() for name in re.split(r'[,|]', requirement)}
    return names


def pool_grants(path: Path) -> dict:
    """For each ability of a LST file, the pools its BONUS:ABILITYPOOL opens and the abilities it grants automatically."""
    grants = defaultdict(lambda: (set(), set()))
    for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
        columns = line.split('\t')
        name = columns[0].strip()
        if not name or name.startswith('#') or ':' in name.split('.')[0]:
            continue
        key = next((column[4:].strip() for column in columns if column.startswith('KEY:')), re.sub(r'\.(MOD|COPY=.*)$', '', name))
        pools, automatic = grants[key]
        for opened in re.findall(r'BONUS:ABILITYPOOL\|([^|\t]+)', line):
            pools |= set(opened.split(','))
        for granted in re.findall(r'\tABILITY:[^|\t]+\|AUTOMATIC\|([^\t]+)', line):
            automatic |= {entry for entry in granted.split('|') if not re.match(r'!?PRE|%|TYPE[.=]', entry)}
    return grants


def pools_opened(grants: dict) -> dict:
    """Every pool an ability opens, itself or through the abilities it grants automatically."""
    resolved: dict = {}

    def opened_by(key: str, visiting: frozenset) -> frozenset:
        if key in resolved:
            return resolved[key]
        pools, automatic = grants.get(key, ((), ()))
        found = frozenset(pools).union(*(opened_by(granted, visiting | {key}) for granted in automatic if granted not in visiting))
        resolved[key] = found
        return found

    return {key: pools for key in grants if (pools := opened_by(key, frozenset()))}


def build_index() -> DataIndex:
    pccs = {pcc.resolve(): pcc_entries(pcc) for pcc in PATHFINDER.rglob('*.pcc')}
    books = {}
    for pcc, entries in pccs.items():
        tags = dict(reversed(entries))
        # PRECAMPAIGN separates its books with commas, so a kit cannot require a book whose key has one
        if 'CAMPAIGN' in tags and ',' not in tags.get('KEY', tags['CAMPAIGN']):
            rank = int(tags['RANK']) if tags.get('RANK', '').isdigit() else NO_RANK
            books[pcc] = Book(tags.get('KEY', tags['CAMPAIGN']), tags['CAMPAIGN'], rank)

    def loaded_files(pcc: Path, seen: set) -> list[tuple[str, Path]]:
        if pcc in seen or pcc not in pccs:
            return []
        seen.add(pcc)
        files = []
        for tag, value in pccs[pcc]:
            target = resolve(pcc, value) if tag in FILE_KINDS or tag == 'PCC' else None
            if target is None:
                continue
            files += loaded_files(target, seen) if tag == 'PCC' else [(FILE_KINDS[tag], target)]
        return files

    books_by_key: dict = defaultdict(lambda: defaultdict(set))
    visible_templates = set()
    races_levels: dict = {}
    prestige: set = set()
    class_files = set()
    ability_grants: dict = defaultdict(lambda: (set(), set()))
    keys_cache: dict = {}
    for pcc, book in books.items():
        for kind, path in loaded_files(pcc, set()):
            if not path.is_file():
                continue
            if (kind, path) not in keys_cache:
                keys_cache[(kind, path)] = object_keys(path, kind)
                if kind == 'RACE':
                    races_levels = racial_levels(path) | races_levels
                if kind == 'CLASS':
                    prestige |= prestige_classes(path)
                    class_files.add(path)
                if kind == 'ABILITY':
                    for key, (pools, automatic) in pool_grants(path).items():
                        ability_grants[key][0].update(pools)
                        ability_grants[key][1].update(automatic)
            for key, visible in keys_cache[(kind, path)]:
                books_by_key[kind][key].add(book)
                if kind == 'TEMPLATE' and visible:
                    visible_templates.add(key)
    requirements: dict = defaultdict(set)
    for path in class_files:
        for class_name, names in required_abilities(path, prestige).items():
            requirements[class_name] |= names
    return DataIndex(books_by_key, frozenset(visible_templates), races_levels, frozenset(prestige),
                     {class_name: frozenset(names) for class_name, names in requirements.items()}, pools_opened(ability_grants))


def required_books(index: DataIndex, references: set, loaded: tuple[str, ...]) -> list[Book]:
    """One book per reference: a book the character loaded when one defines it, the lowest ranked otherwise."""
    required = set()
    for kind, key in references:
        candidates = index.books_by_key[kind][key]
        preferred = [book for book in candidates if book.name in loaded] or list(candidates)
        required.add(min(preferred, key=lambda book: (book.rank, book.key)))
    return sorted(required, key=lambda book: (book.rank, book.key))


def scenario_pcg(identifier: str) -> Path | None:
    return next((path for folder in SCENARIO_FOLDERS for path in [SCENARIO_NPCS / folder / f'{identifier}.pcg'] if path.is_file()), None)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('identifiers', nargs='*', help='NPCs to add, by file name without extension')
    parser.add_argument('--all', action='store_true', help='add every NPC of the scenario')
    arguments = parser.parse_args()

    identifiers = sorted(path.stem for folder in SCENARIO_FOLDERS for path in (SCENARIO_NPCS / folder).glob('*.pcg')) \
        if arguments.all else arguments.identifiers
    sources = {identifier: scenario_pcg(identifier) for identifier in identifiers}
    unknown = [identifier for identifier, source in sources.items() if source is None]
    if unknown:
        sys.exit(f'Unknown NPCs: {unknown}')
    NPC_DIRECTORY.mkdir(parents=True, exist_ok=True)
    KIT_DIRECTORY.mkdir(parents=True, exist_ok=True)
    for identifier, source in sources.items():
        shutil.copyfile(source, NPC_DIRECTORY / f'{identifier}.pcg')

    index = build_index()
    kit_references = []
    dropped_total = 0
    for pcg in sorted(NPC_DIRECTORY.glob('*.pcg')):
        npc = zirnakaynin_kit.parse_pcg(pcg.read_text(encoding='utf-8'))
        referenced = set()

        def is_defined(kind, key):
            defined = index.is_defined(kind, key)
            if defined:
                referenced.add((kind, key))
            return defined

        lines, dropped = zirnakaynin_kit.kit_lines(npc, zirnakaynin_kit.Catalog(is_defined, index.visible_templates, index.racial_levels, index.prestige_classes, index.prestige_requirements, index.pools_opened))
        kit_file = KIT_DIRECTORY / f'zir_kit_{pcg.stem}.lst'
        kit_file.write_text('SOURCELONG:Zirnakaynin\tSOURCESHORT:ZIR\tSOURCEDATE:2026-10\n\n' + '\n'.join(lines) + '\n', encoding='utf-8')
        books = required_books(index, referenced, npc.campaigns)
        condition = f'|PRECAMPAIGN:{len(books)},' + ','.join(f'INCLUDES={book.key}' for book in books) if books else ''
        kit_references.append(f'KIT:kits/{kit_file.name}{condition}')
        for kind, key in dropped:
            print(f'{pcg.stem}: {kind} {key} is defined by no book a kit can require, left out of the kit')
        dropped_total += len(dropped)
    PCC.write_text(PCC_HEADER + '\n' + '\n'.join(kit_references) + '\n', encoding='utf-8')
    print(f'{len(kit_references)} kits written, {dropped_total} references left out')
    return 0


if __name__ == '__main__':
    sys.exit(main())
