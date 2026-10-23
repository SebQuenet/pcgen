"""Import Combat Manager's NPCs into PCGen as .pcg characters.

    python3 scripts/combat_manager/import_npcs.py <extracted dir> --source "NPC Codex"
    python3 scripts/combat_manager/import_npcs.py <extracted dir> --name "Cautious Mage" --force

Needs the HTTP server running with average hit points, which PCGen's own settings
do not give (they roll). Write those settings once, then start the server on them:

    python3 scripts/combat_manager/import_npcs.py --write-settings
    ./gradlew runServer --args="--settingsdir build/combat-manager-settings"

Spec: specs/import-combat-manager.md
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
import time
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPTS_DIR))

import build  # noqa: E402
import catalog  # noqa: E402
import pcgen_client as pc  # noqa: E402
import records  # noqa: E402
import statblock  # noqa: E402
from generate_bestiary import load_known_languages  # noqa: E402

REPO_DIR = SCRIPTS_DIR.parent
SETTINGS_DIR = REPO_DIR / 'settings'
IMPORT_SETTINGS_DIR = REPO_DIR / 'build' / 'combat-manager-settings'
HIT_POINT_RULE_OPTION = 'pcgen.options.hpRollMethod'
# Constants.HP_AVERAGE: the die's average, with the extra point on even levels, as Paizo's NPCs have
AVERAGE_HIT_POINTS = 2
PCG_BACKUP_OPTION = 'pcgen.options.createPcgBackup'
# A fighter 2 with Constitution 10 has 10 + 6 hit points under that rule
PROBE_CLASS, PROBE_LEVELS, PROBE_HIT_POINTS = 'Fighter', 2, 16
DEFAULT_OUTPUT_DIR = REPO_DIR / 'characters' / 'combat-manager'
DATA_DIR = REPO_DIR / 'data' / 'pathfinder'
FAILURES_FILE = 'failures.json'
# Exit status telling the caller to restart the server before going on
SERVER_STUCK = 75
INTERRUPTED = 'interrompu'
INTERRUPTED_TWICE = 'interrompu deux fois'  # a retry never builds it again, so it cannot stall every round
GAME_MODE = 'Pathfinder_RPG'
CAMPAIGNS = [
    'Core Rulebook', 'Pathfinder RPG Core Supplements', "Advanced Player's Guide", 'Advanced Class Guide',
    'Ultimate Magic', 'Ultimate Combat', 'Ultimate Equipment', 'Ultimate Campaign', 'Ultimate Intrigue',
    'Ultimate Wilderness', 'Advanced Race Guide', 'Occult Adventures', 'Pathfinder Unchained', 'Mythic Adventures',
    'Horror Adventures', 'Villain Codex', 'Monster Codex', 'Inner Sea World Guide', 'Inner Sea Gods', 'Inner Sea Races',
    'Inner Sea Magic', 'Inner Sea Combat', 'Bestiary', 'Bestiary 2', 'Bestiary 3', 'Bestiary 4', 'Bestiary 5',
    'Bestiary 6', 'Inner Sea Bestiary', 'Occult Bestiary ~ Creatures', 'Tome of Horrors Complete ~ Creatures',
    # templates the adventures apply: Advanced Bestiary's dread and -clad, devil-bound, winter-touched
    'Green Ronin - Advanced Bestiary', 'Bestiary 4 ~ Player Options Only', 'Pathfinder Adventure Path #67',
    'Dungeons of Golarion', 'Rival Guide',
    'Plunder & Peril ~ Creatures', 'The Witchwar Legacy ~ Creatures', 'Pathfinder Adventure Path #113 ~ Creatures',
    'Doom Comes to Dustpawn ~ Creatures',
    # monsters some NPCs are built on: eidolon forms, ja noi, grioth, malenti, skin stealer, clockwork servant
    'Bestiary 3 ~ Player Options Only', "The Dragon's Demand ~ Creatures", 'Tome of Horrors 4 ~ Creatures',
    'The Worldwound ~ Creatures', 'Pathfinder Adventure Path #37 ~ Creatures', 'Pathfinder Adventure Path #44 ~ Creatures',
    'Pathfinder Adventure Path #52 ~ Creatures', 'Pathfinder Adventure Path #56 ~ Creatures',
    'Pathfinder Adventure Path #111 ~ Creatures', 'Down the Blighted Path ~ Creatures',
    'Pathfinder Adventure Path #54 ~ Creatures', 'Pathfinder Adventure Path #90 ~ Creatures',
]
UNSAFE_PATH_CHARACTERS = r'[\\/:*?"<>|]'
# Combat Manager lists animal companions with their master; they are not NPCs
COMPANION_CLASSES = ('animal companion', 'eidolon')


def write_settings() -> None:
    """Copy PCGen's settings with the hit point rule set to the average, and no backup left in characters/."""
    if IMPORT_SETTINGS_DIR.exists():
        shutil.rmtree(IMPORT_SETTINGS_DIR)
    shutil.copytree(SETTINGS_DIR, IMPORT_SETTINGS_DIR)
    set_option(IMPORT_SETTINGS_DIR / 'legacy.ini', HIT_POINT_RULE_OPTION, str(AVERAGE_HIT_POINTS))
    # rebuilding an NPC saves over its .pcg, and PCGen would keep the old one as a .pcg.bak in characters/
    for file_name in ('legacy.ini', 'options.ini'):
        set_option(IMPORT_SETTINGS_DIR / file_name, PCG_BACKUP_OPTION, 'false')


def set_option(settings_file: Path, option: str, value: str) -> None:
    lines = [line for line in settings_file.read_text(encoding='utf-8').splitlines() if not line.startswith(option + '=')]
    lines.append(f'{option}={value}')
    settings_file.write_text('\n'.join(lines) + '\n', encoding='utf-8')


def require_average_hit_points() -> None:
    probe = pc.call('create_character', name='hit point probe')['character_id']
    try:
        pc.call('set_race', character_id=probe, race_key='Human')
        pc.call('set_all_ability_scores', character_id=probe, scores={stat: 10 for stat in statblock.STAT_KEYS})
        pc.call('add_class_level', character_id=probe, class_key=PROBE_CLASS, levels=PROBE_LEVELS)
        hit_points = build.read_sheet(probe)['hp']
    finally:
        pc.call('close_character', character_id=probe)
    if hit_points != PROBE_HIT_POINTS:
        raise SystemExit(f'le serveur ne donne pas les PV moyens ({hit_points} au lieu de {PROBE_HIT_POINTS} sur un guerrier 2) : '
                         'lancez-le avec --settingsdir build/combat-manager-settings (voir --write-settings)')


def load_sources(npc_sources: set[str] = frozenset()) -> None:
    pc.call('load_sources', game_mode=GAME_MODE, campaigns=CAMPAIGNS + source_campaigns(npc_sources, DATA_DIR))


def normalized(name: str) -> str:
    return re.sub(r'[^a-z0-9]', '', name.lower().replace('&', 'and'))


def source_campaigns(npc_sources: set[str], data_dir: Path) -> list[str]:
    """The PCGen books the NPCs come from: "Andoran Birthplace Of Freedom" is "Andoran - Birthplace of Freedom"."""
    books = {}
    for pcc in sorted(data_dir.rglob('*.pcc')):
        text = pcc.read_text(encoding='utf-8', errors='replace')
        title, key = re.search(r'^CAMPAIGN:(.+)$', text, re.M), re.search(r'^KEY:(.+)$', text, re.M)
        if not title or not re.search(r'^GAMEMODE:.*\bPathfinder\b', text, re.M):
            continue
        campaign_key = (key or title).group(1).strip()
        # the full book rather than its player options or its Society copy
        if 'Player Options' in campaign_key or campaign_key.startswith('PFS ~'):
            continue
        books.setdefault(normalized(title.group(1)), campaign_key)
    wanted = []
    for source in npc_sources:
        adventure_path = re.match(r'^AP (\d+)$', source)
        name = f'Pathfinder Adventure Path #{adventure_path.group(1)}' if adventure_path else re.sub(r'^PFRPG ', '', source)
        if normalized(name) in books:
            wanted.append(books[normalized(name)])
    return sorted(set(wanted) - set(CAMPAIGNS))


def keys_of(kind: str) -> list[str]:
    return [entry['key'] for entry in pc.call('list_dataset_entries', kind=kind, limit=100000)]


def load_catalogs() -> build.Catalogs:
    return build.Catalogs(
        classes=catalog.KeyIndex(keys_of('class')),
        races=catalog.KeyIndex(keys_of('race')),
        templates=catalog.KeyIndex(keys_of('template')),
        items=catalog.KeyIndex(keys_of('equipment')),
        known_languages=load_known_languages(str(REPO_DIR / 'data')),
        limited_known_classes=catalog.limited_known_classes(
            '\n'.join(path.read_text(encoding='utf-8', errors='replace') for path in DATA_DIR.rglob('*classes*.lst'))),
    )


def safe_name(text: str) -> str:
    return re.sub(r'\s+', ' ', re.sub(UNSAFE_PATH_CHARACTERS, '-', text)).strip(' .') or 'sans nom'


def character_path(record: records.NpcRecord, catalogs: build.Catalogs, output_dir: Path) -> Path:
    """<book>/<primary class>/<name>.pcg; a second NPC of the same name gets its id."""
    entries = statblock.parse_classes(record.get('Class') or '', set(catalogs.classes.by_reading))
    primary = max(entries, key=lambda entry: entry.levels) if entries else None
    class_key = catalog.resolve_class(primary, catalogs.classes)[0] if primary else None
    folder = output_dir / safe_name(record.get('Source') or 'Source inconnue') / safe_name(class_key or (primary.name.title() if primary else 'Sans classe'))
    path = folder / f'{safe_name(record.get("Name") or str(record.identifier))}.pcg'
    report_path = path.with_suffix('.report.json')
    if report_path.exists() and json.loads(report_path.read_text(encoding='utf-8')).get('id') != record.identifier:
        path = folder / f'{path.stem} ({record.identifier}).pcg'
    return path


def is_npc(record: records.NpcRecord) -> bool:
    """A summoner's eidolon, like an animal companion, is a companion PCGen builds on its master, not a character."""
    class_text = (record.get('Class') or '').lower()
    return not any(companion in class_text for companion in COMPANION_CLASSES)


def new_report(record: records.NpcRecord) -> dict:
    return {'id': record.identifier, 'name': record.get('Name'), 'source': record.get('Source'), 'class': record.get('Class'),
            'missing_classes': [], 'missing_templates': [], 'missing_items': [], 'missing_modifiers': [], 'missing_spells': [],
            'refused_feats': [], 'refused': [], 'open_pools': [],
            'added_feats': [], 'added_languages': [], 'added_spells': [], 'added_skills': [], 'added_companion': None}


def write_failures(failures_path: Path, failures: dict) -> None:
    failures_path.parent.mkdir(parents=True, exist_ok=True)
    failures_path.write_text(json.dumps(failures, ensure_ascii=False, indent=2), encoding='utf-8')


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('extracted_dir', type=Path, nargs='?', help="directory holding the files unpacked from Combat Manager's installer")
    parser.add_argument('--write-settings', action='store_true', help=f'write the server settings to {IMPORT_SETTINGS_DIR} and stop')
    parser.add_argument('--source', action='append', help='only the NPCs of this book (repeatable)')
    parser.add_argument('--name', action='append', help='only the NPC of this name (repeatable)')
    parser.add_argument('--force', action='store_true', help='rebuild an NPC that already has a report')
    parser.add_argument('--ids-file', type=Path, help='only the NPCs whose identifiers this file lists, one per line')
    parser.add_argument('--retry-failures', action='store_true', help=f'retry the NPCs listed in {FAILURES_FILE}')
    parser.add_argument('--output', type=Path, default=DEFAULT_OUTPUT_DIR)
    arguments = parser.parse_args()
    if arguments.write_settings:
        write_settings()
        print(f'réglages écrits dans {IMPORT_SETTINGS_DIR}')
        return 0
    if arguments.extracted_dir is None:
        parser.error('extracted_dir is required')

    npcs = [record for record in records.load_npcs(arguments.extracted_dir) if is_npc(record)]
    if arguments.source:
        npcs = [record for record in npcs if record.get('Source') in arguments.source]
    if arguments.ids_file:
        wanted_ids = set(arguments.ids_file.read_text(encoding='utf-8').split())
        npcs = [record for record in npcs if str(record.identifier) in wanted_ids]
    if arguments.name:
        npcs = [record for record in npcs if record.get('Name') in arguments.name]
    load_sources({record.get('Source') for record in npcs})
    require_average_hit_points()
    catalogs = load_catalogs()

    failures_path = arguments.output / FAILURES_FILE
    failures = json.loads(failures_path.read_text(encoding='utf-8')) if failures_path.exists() else {}
    failed_this_run = 0
    for position, record in enumerate(npcs, start=1):
        path = character_path(record, catalogs, arguments.output)
        report_path = path.with_suffix('.report.json')
        if report_path.exists() and not arguments.force:
            continue
        previous_failure = failures.get(str(record.identifier), '')
        if previous_failure and (not arguments.retry_failures or previous_failure.endswith(INTERRUPTED_TWICE)):
            continue
        report, started = new_report(record), time.monotonic()
        character_id = None
        # Recorded before building, so a round killed on this NPC skips it next time
        interruption = INTERRUPTED_TWICE if previous_failure.endswith(INTERRUPTED) else INTERRUPTED
        failures[str(record.identifier)] = f'{record.get("Name")} — {interruption}'
        write_failures(failures_path, failures)
        try:
            character_id = build.build_npc(record, catalogs, report)
            path.parent.mkdir(parents=True, exist_ok=True)
            pc.call('save_character', character_id=character_id, file_path=str(path))
        except pc.PcgenUnreachable:
            failures.pop(str(record.identifier), None)
            write_failures(failures_path, failures)
            character_id = None
            return SERVER_STUCK  # the next round restarts the server and builds this NPC again
        except (pc.PcgenError, LookupError, KeyError, ValueError) as error:
            failed_this_run += 1
            print(f'[{position}/{len(npcs)}] ÉCHEC {record.get("Name")} ({record.identifier}) — {error}', file=sys.stderr, flush=True)
            failures[str(record.identifier)] = f'{record.get("Name")} — {error}'
            write_failures(failures_path, failures)
            if isinstance(error, pc.PcgenInterrupted):
                character_id = None  # closing it would wait on the same busy server
                return SERVER_STUCK
            continue
        finally:
            if character_id:
                try:
                    pc.call('close_character', character_id=character_id)
                except pc.PcgenError:
                    pass
        report['seconds'] = round(time.monotonic() - started, 1)
        report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
        print(f'[{position}/{len(npcs)}] ok {path.relative_to(arguments.output)} ({report["seconds"]} s)', flush=True)
        failures.pop(str(record.identifier), None)
        write_failures(failures_path, failures)
    return 1 if failed_this_run else 0


if __name__ == '__main__':
    raise SystemExit(main())
