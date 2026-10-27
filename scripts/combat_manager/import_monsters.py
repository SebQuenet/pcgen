"""Write Combat Manager's class-less monsters that PCGen lacks as PCGen races.

    python3 scripts/combat_manager/import_monsters.py <extracted dir> [--dry-run]

Spec: specs/import-combat-manager.md, section 7
"""

from __future__ import annotations

import argparse
import collections
import re
import sys
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPTS_DIR))

import catalog  # noqa: E402
import generate_bestiary  # noqa: E402
import monsters  # noqa: E402
import records  # noqa: E402

REPO_DIR = SCRIPTS_DIR.parent
DATA_DIR = REPO_DIR / 'data' / 'pathfinder'
GENERATED_MARK = '_cm_'
GROUPED_DIR = DATA_DIR / 'pcfinder' / 'combat_manager'
GROUPED_PCC = GROUPED_DIR / 'combat_manager.pcc'
GROUPED_PREFIX = 'combat_manager'
GROUPED_CAMPAIGN = 'Combat Manager'
# A race's NATURALATTACKS makes the weapon proficiency of each natural weapon it has
NATURAL_WEAPONS = {pcgen_name for pcgen_name, _ in generate_bestiary.NATURAL_WEAPON_MAP.values()}
LANGUAGE_BOOKS = (DATA_DIR / 'paizo' / 'roleplaying_game', DATA_DIR / 'paizo' / 'campaign_setting' / 'inner_sea_world_guide')
SOURCE_TAGS = ('SOURCELONG', 'SOURCESHORT', 'SOURCEWEB', 'SOURCEDATE')
GROUPED_PCC_TEXT = f"""CAMPAIGN:{GROUPED_CAMPAIGN}
KEY:{GROUPED_CAMPAIGN} ~ Creatures
GAMEMODE:Pathfinder|Pathfinder_PFS
TYPE:Paizo Publishing.Pathfinder RPG
STATUS:ALPHA
GENRE:Fantasy
BOOKTYPE:Supplement
SETTING:Pathfinder
PRECAMPAIGN:1,INCLUDESBOOKTYPE=Core Rules
PRECAMPAIGN:1,INCLUDES=Bestiary,INCLUDES=Bestiary ~ Player Options Only
PUBNAMELONG:Paizo Inc.
PUBNAMESHORT:Paizo
PUBNAMEWEB:http://paizo.com
SOURCELONG:{GROUPED_CAMPAIGN}
SOURCESHORT:CM
SOURCEWEB:http://paizo.com
SOURCEDATE:2019-08
RANK:201908
ISOGL:YES

INFOTEXT:Monsters of Paizo's adventures, Pathfinder Society scenarios and other books PCGen has no dataset for, taken from Combat Manager 1.77. Each race names its book in its source. This dataset uses trademarks and/or copyrights owned by Paizo Inc., which are used under Paizo's Community Use Policy. We are expressly prohibited from charging you to use or access this content. This dataset is not published, endorsed, or specifically approved by Paizo Publishing.
COPYRIGHT:Open Game License v 1.0a Copyright 2000, Wizards of the Coast, Inc.
COPYRIGHT:System Reference Document Copyright 2000, Wizards of the Coast, Inc; Authors: Jonathan Tweet, Monte Cook, Skip Williams, based on material by E. Gary Gygax and Dave Arneson.
"""


def first_column_names(path: Path) -> set[str]:
    """The name and key of every object a LST file defines."""
    names = set()
    for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
        columns = line.split('\t')
        name = columns[0].strip()
        if name and not name.startswith('#') and ':' not in name and not re.search(r'\.(MOD|FORGET|COPY)', name):
            names.add(name)
            names.update(column[4:].strip() for column in columns if column.startswith('KEY:'))
    return names


def object_keys(path: Path) -> set[str]:
    """The key of every object a LST file defines: its KEY, or its name when it has none."""
    keys = set()
    for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
        columns = line.split('\t')
        name = columns[0].strip()
        if name and not name.startswith('#') and ':' not in name and not re.search(r'\.(MOD|FORGET|COPY)', name):
            keys.add(next((column[4:].strip() for column in columns if column.startswith('KEY:')), name))
    return keys


def kit_names(path: Path) -> set[str]:
    """The sample monsters kits stand for: the Bestiary's "Dracolisk" is a kit on the basilisk."""
    found = re.findall(r'^STARTPACK:([^\t]+)', path.read_text(encoding='utf-8', errors='replace'), re.M)
    return {name.strip() for name in found if not name.strip().endswith(' Default')}


def objects_of_category(category: str, needs_choice: bool = False) -> set[str]:
    """The key of every ability of a category, "FEAT" or "Mythic Feat", or only of those that make a choice."""
    names = set()
    for path in pcgen_files('*.lst'):
        text = path.read_text(encoding='utf-8', errors='replace')
        if f'CATEGORY:{category}' not in text:
            continue
        for line in text.splitlines():
            columns = line.split('\t')
            if needs_choice and not any(column.startswith('CHOOSE:') for column in columns):
                continue
            if f'CATEGORY:{category}' in columns and ':' not in columns[0] and '.MOD' not in columns[0]:
                names.add(next((column[4:].strip() for column in columns if column.startswith('KEY:')), columns[0].strip()))
    return names


def rulebook_languages() -> dict[str, str]:
    """The languages a race may grant: those of the rulebooks and the Inner Sea World Guide, which every monster book
    loads with, and not those of a player companion it may not."""
    defined = set().union(*(object_keys(path) for directory in LANGUAGE_BOOKS for path in directory.rglob('*languages*.lst')))
    return {lowered: key for lowered, key in generate_bestiary.load_known_languages(str(REPO_DIR / 'data')).items() if key in defined}


def load_catalog() -> monsters.Catalog:
    def index(names: set[str]) -> catalog.KeyIndex:
        return catalog.KeyIndex(sorted(names))

    def defined(pattern: str) -> set[str]:
        return set().union(*map(object_keys, pcgen_files(pattern)))

    return monsters.Catalog(feats=index(objects_of_category('FEAT')),
                            choosing_feats=frozenset(objects_of_category('FEAT', needs_choice=True)), mythic_feats=index(objects_of_category('Mythic Feat')),
                            weapon_profs=index(defined('*prof*weapon*.lst') | defined('*weaponprof*.lst') | NATURAL_WEAPONS),
                            skills=index(defined('*skill*.lst')),
                            spells=index(defined('*spell*.lst')))


def pcgen_files(pattern: str) -> list[Path]:
    return [path for path in DATA_DIR.rglob(pattern) if GENERATED_MARK not in path.name]


def load_indexes() -> tuple[catalog.KeyIndex, catalog.KeyIndex]:
    races = set().union(*map(first_column_names, pcgen_files('*races*.lst')), *map(kit_names, pcgen_files('*kits*.lst')))
    templates = set().union(*map(first_column_names, pcgen_files('*template*.lst')))
    return catalog.KeyIndex(sorted(races)), catalog.KeyIndex(sorted(templates))


def load_books() -> dict[str, Path]:
    """Each full Pathfinder book PCGen has, by normalized title, to its .pcc."""
    books = {}
    for pcc in sorted(DATA_DIR.rglob('*.pcc')):
        text = pcc.read_text(encoding='utf-8', errors='replace')
        title, key = re.search(r'^CAMPAIGN:(.+)$', text, re.M), re.search(r'^KEY:(.+)$', text, re.M)
        if not title or pcc == GROUPED_PCC:
            continue
        campaign_key = (key or title).group(1)
        if 'Player Options' in campaign_key or campaign_key.startswith('PFS ~') or 'for_players' in pcc.name:
            continue
        books.setdefault(monsters.normalized(title.group(1)), pcc)
    return books


def missing_monsters(extracted_dir: Path) -> list[records.NpcRecord]:
    """Combat Manager's monsters PCGen lacks, one per name, companions and templates left out."""
    races, templates = load_indexes()
    seen, missing = set(), []
    for record in sorted(records.load_monsters(extracted_dir), key=lambda record: record.identifier):
        name = record.get('Name') or ''
        if record.get('IsTemplate') == '1' or record.get('CompanionFlag') == '1' or name.lower() in seen:
            continue
        seen.add(name.lower())
        if not monsters.is_known(name, races, templates):
            missing.append(record)
    return missing


def book_name(source: str) -> str:
    adventure_path = re.match(r'^AP (\d+)$', source)
    return f'Pathfinder Adventure Path #{adventure_path.group(1)}' if adventure_path else re.sub(r'^PFRPG ', '', source)


def file_prefix(pcc: Path) -> str:
    """The prefix the book's own race file uses ("b5" for b5_races.lst), else the .pcc's name."""
    own = sorted(path.name[:-len('_races.lst')] for path in pcc.parent.glob('*_races.lst') if GENERATED_MARK not in path.name)
    return own[0] if own else pcc.stem.strip('_')


def header(pcc: Path) -> str:
    text = pcc.read_text(encoding='utf-8', errors='replace')
    tags = [match.group(0) for tag in SOURCE_TAGS for match in [re.search(rf'^{tag}:.+$', text, re.M)] if match]
    return '\t'.join(tags)


def write_destination(pcc: Path, creatures: list[dict], known_languages: set[str], pcgen: monsters.Catalog,
                      names_book: bool) -> list[str]:
    """Write the book's generated files and list what PCGen lacked."""
    prefix = f'{file_prefix(pcc)}{GENERATED_MARK.rstrip("_")}'
    race_lines, kit_blocks, trait_lines, dropped = [], [], [], []
    for creature in sorted(creatures, key=lambda creature: creature['name']):
        creature, abilities, lacking = monsters.prune(creature, generate_bestiary.get_effective_d20_data(creature, {}) or {}, pcgen)
        dropped += lacking
        race_line = generate_bestiary.generate_race_line(creature, {}, known_languages, abilities)
        if names_book:
            race_line += f'\tSOURCELONG:{book_name(creature["source"])}'
        race_lines.append(race_line)
        kit_blocks.append(generate_bestiary.generate_kit_block(creature, {}, abilities) + '\n')
        trait_lines.append(generate_bestiary.generate_racial_traits_line(creature, abilities))
    files = {f'{prefix}_abilities_race.lst': ('ABILITY', trait_lines), f'{prefix}_races.lst': ('RACE', race_lines),
             f'{prefix}_kits_race.lst': ('KIT', kit_blocks)}
    source_header = header(pcc)
    for file_name, (_, lines) in files.items():
        (pcc.parent / file_name).write_text('\n'.join([source_header, '', *lines]) + '\n', encoding='utf-8')
    pcc_text = pcc.read_text(encoding='utf-8')
    references = [f'{tag}:{file_name}' for file_name, (tag, _) in files.items() if f'{tag}:{file_name}' not in pcc_text]
    if references:
        pcc.write_text(pcc_text.rstrip('\n') + '\n' + '\n'.join(references) + '\n', encoding='utf-8')
    return dropped


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('extracted_dir', type=Path)
    parser.add_argument('--dry-run', action='store_true', help='count the monsters per book without writing')
    arguments = parser.parse_args()

    books = load_books()
    destinations: dict[Path, list[dict]] = collections.defaultdict(list)
    for record in missing_monsters(arguments.extracted_dir):
        creature = monsters.to_creature(record.fields)
        destinations[monsters.book_pcc(creature['source'], books) or GROUPED_PCC].append(creature)
    for pcc, creatures in sorted(destinations.items(), key=lambda entry: -len(entry[1])):
        print(f'{len(creatures):4} {pcc.relative_to(REPO_DIR)}')
    print(f'{sum(map(len, destinations.values()))} monstres dans {len(destinations)} sources')
    if arguments.dry_run:
        return 0

    GROUPED_DIR.mkdir(parents=True, exist_ok=True)
    if not GROUPED_PCC.exists():
        GROUPED_PCC.write_text(GROUPED_PCC_TEXT, encoding='utf-8')
    known_languages = rulebook_languages()
    pcgen = load_catalog()
    dropped = collections.Counter()
    for pcc, creatures in destinations.items():
        dropped.update(write_destination(pcc, creatures, known_languages, pcgen, names_book=pcc == GROUPED_PCC))
    print(f'{sum(dropped.values())} dons, compétences ou sorts absents de PCGen retirés :')
    for reference, count in dropped.most_common():
        print(f'{count:4} {reference}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
