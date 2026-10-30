#!/usr/bin/env python3
"""Write a French tactical plan for each race imported from Combat Manager that has none, by driving the Codex CLI.

With --bestiary, the races are those of every monster book that have none, and the stat block is Combat Manager's
when it has the monster, pcfinder's otherwise.

Codex gets the monster's stat block and two existing plans as models, and writes the plan to
code/src/resources/pcgen/bestiary/tactics/<race slug>.txt, where BestiaryTactics reads it. A plan whose sections
are not the four expected ones is discarded. Races that already have a plan are skipped, so an interrupted run resumes.

Usage:
    python3 scripts/generate_bestiary_tactics.py <extracted dir> --dry-run
    python3 scripts/generate_bestiary_tactics.py <extracted dir> --workers 4 --limit 20
    python3 scripts/generate_bestiary_tactics.py <extracted dir> --only abhominal --force
    python3 scripts/generate_bestiary_tactics.py <extracted dir> --bestiary --workers 4
"""

import argparse
import json
import re
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "combat_manager"))

import import_monsters  # noqa: E402
import monsters  # noqa: E402
import records  # noqa: E402
from generate_bestiary import sanitize_name  # noqa: E402
from generate_summon_portraits import draw_with_codex as run_codex  # noqa: E402

REPOSITORY = Path(__file__).resolve().parent.parent
TACTICS = REPOSITORY / "code/src/resources/pcgen/bestiary/tactics"
MODEL_PLANS = ("adaro", "aboleth")
SECTIONS = ("## Avant le combat", "## Ouverture", "## Rounds suivants", "## Repli")
LINE_STARTS = ("## ", "note:", "step:", "resource:", "  ")
IGNORED_FIELDS = {"id", "XP", "Source", "MonsterSource"}
PATHFINDER_DATA = REPOSITORY / "data/pathfinder"
PCFINDER_CREATURES = REPOSITORY.parent / "pcfinder/apps/pcfinder-csr/static-data/creatures.json"
# pcfinder's attacks parsed from the melee and ranged lines, which the stat block already gives
PCFINDER_IGNORED_FIELDS = {"id", "parsedAttacks"}
# Race files of the players' races, their companions and familiars, which fight as characters do
PLAYER_RACE_FILE = re.compile(r"_(pc|companion|familiar)\.lst$")
# Books whose races are peoples, heralds or psionic beings rather than monsters
NOT_MONSTER_BOOKS = {"inner_sea_gods", "psionics_unleashed", "ultimate_psionics", "core_essentials", "remarkable_races",
                     "players_guide", "support", "book_of_arcane_magic"}

PROMPT = """Write the tactical plan of the Pathfinder Roleplaying Game monster "{name}" from {source}, in French,
and save it as the UTF-8 text file {target}. Do nothing else.

The plan tells a game master how the monster fights, using only what its stat block below gives it, plus its
official description in {source} if you know it. Distances in metres (1.50 m per square), hit points and DCs as
in the stat block.

Follow exactly the format of the two model plans: the four sections "## Avant le combat", "## Ouverture",
"## Rounds suivants" and "## Repli", in that order; inside them only "note: <title>" with indented text lines,
"step: <condition>" with indented "  do: <action>" and "  note: <detail>" lines, and "resource: <name> | <uses> | <detail>".
Start "Avant le combat" with "note: Profil" summarizing the monster and its main attacks. No other markup.

Stat block:
{stat_block}

Model plan 1:
{model_one}

Model plan 2:
{model_two}"""


def slug_of(race_key):
    """The plan's file stem; PathfinderSummonPortraits.slugOf derives the same in Java."""
    return re.sub(r"[^a-z0-9]+", "_", race_key.lower()).strip("_")


def stat_block(fields):
    return "\n".join(f"{name}: {text.strip()}" for name, text in fields.items()
                     if name not in IGNORED_FIELDS and text and text.strip())


def is_well_formed(plan):
    """True when the plan has the four sections in order, only lines the plan format knows, and distinct resource names."""
    lines = [line for line in plan.splitlines() if line.strip()]
    sections = tuple(line.strip() for line in lines if line.startswith("## "))
    resource_names = [line.removeprefix("resource:").split("|")[0].strip() for line in lines if line.startswith("resource:")]
    return (sections == SECTIONS and all(line.startswith(LINE_STARTS) for line in lines)
            and len(resource_names) == len(set(resource_names)))


def prompt_for(name, source, fields, target):
    model_one, model_two = ((TACTICS / f"{stem}.txt").read_text(encoding="utf-8") for stem in MODEL_PLANS)
    return PROMPT.format(name=name, source=source, target=target, stat_block=stat_block(fields),
                         model_one=model_one, model_two=model_two)


def write_plan(stem, name, source, fields):
    with tempfile.TemporaryDirectory(prefix="tactics-") as work_directory:
        generated_plan = Path(work_directory) / "plan.txt"
        failure = run_codex(prompt_for(name, source, fields, generated_plan), generated_plan)
        if failure:
            return stem, failure
        plan = generated_plan.read_text(encoding="utf-8").strip() + "\n"
    if not is_well_formed(plan):
        return stem, "malformed plan"
    (TACTICS / f"{stem}.txt").write_text(plan, encoding="utf-8")
    return stem, None


def imported_monsters(extracted_dir):
    """Each monster import_monsters.py turns into a race, by the slug of its race key."""
    found = {}
    for record in import_monsters.missing_monsters(extracted_dir):
        creature = monsters.to_creature(record.fields)
        found[slug_of(sanitize_name(creature["name"]))] = (creature["name"], import_monsters.book_name(creature["source"]),
                                                          record.fields)
    return found


def pcfinder_fields(creature):
    """pcfinder's creature as stat block fields, a nested value written as JSON."""
    return {name: text if isinstance(text, str) else json.dumps(text, ensure_ascii=False)
            for name, text in creature.items() if name not in PCFINDER_IGNORED_FIELDS}


def stat_block_of(race_key, race_name, combat_manager, pcfinder):
    """The race's stat block: Combat Manager's when it has the monster, pcfinder's otherwise; None when neither has."""
    stems = (slug_of(race_key), slug_of(race_name))
    for stem in stems:
        if stem in combat_manager:
            return combat_manager[stem]
    for stem in stems:
        if stem in pcfinder:
            return pcfinder_fields(pcfinder[stem])
    return None


def bestiary_races():
    """Each race of the monster books, as (key, name, book)."""
    races = []
    for races_file in sorted(PATHFINDER_DATA.rglob("*_races*.lst")):
        if PLAYER_RACE_FILE.search(races_file.name) or "abilities" in races_file.name \
                or NOT_MONSTER_BOOKS & set(races_file.relative_to(PATHFINDER_DATA).parts):
            continue
        book = "a Pathfinder book"
        for line in races_file.read_text(encoding="utf-8", errors="replace").splitlines():
            columns = line.split("\t")
            if line.startswith("SOURCELONG:"):
                book = columns[0].removeprefix("SOURCELONG:")
            name = columns[0].strip()
            if not name or line.startswith(("#", "SOURCE")) or re.search(r"\.(MOD|COPY|FORGET)", name):
                continue
            key = next((column[4:].strip() for column in columns if column.startswith("KEY:")), name)
            races.append((key, name, book))
    return races


def bestiary_monsters(extracted_dir, pcfinder_creatures):
    """Each race of the monster books that has a stat block, by the slug of its race key."""
    combat_manager = {}
    for record in records.load_monsters(extracted_dir) + records.load_npcs(extracted_dir):
        combat_manager.setdefault(slug_of(sanitize_name(record.fields.get("Name") or "")), record.fields)
    pcfinder = {slug_of(sanitize_name(creature["name"])): creature
                for creature in json.loads(pcfinder_creatures.read_text(encoding="utf-8"))}
    found = {}
    for key, name, book in bestiary_races():
        fields = stat_block_of(key, name, combat_manager, pcfinder)
        if fields:
            found.setdefault(slug_of(key), (name, book, fields))
    return found


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("extracted_dir", type=Path)
    parser.add_argument("--workers", type=int, default=2, help="parallel Codex runs")
    parser.add_argument("--only", action="append", help="only this plan file stem, e.g. abhominal (repeatable)")
    parser.add_argument("--force", action="store_true", help="rewrite plans that already exist")
    parser.add_argument("--limit", type=int, help="write at most this many plans")
    parser.add_argument("--dry-run", action="store_true", help="list the plans to write without writing them")
    parser.add_argument("--bestiary", action="store_true", help="every race of the monster books, not only those imported")
    parser.add_argument("--pcfinder", type=Path, default=PCFINDER_CREATURES, help="pcfinder's creatures.json")
    arguments = parser.parse_args()

    found = bestiary_monsters(arguments.extracted_dir, arguments.pcfinder) if arguments.bestiary \
        else imported_monsters(arguments.extracted_dir)
    selected = arguments.only or sorted(found)
    unknown = [stem for stem in selected if stem not in found]
    if unknown:
        sys.exit(f"Unknown monsters: {unknown}")
    missing = [stem for stem in selected if arguments.force or not (TACTICS / f"{stem}.txt").exists()][:arguments.limit]
    print(f"{len(missing)} plans to write out of {len(selected)}", flush=True)
    if arguments.dry_run:
        for stem in missing:
            print(f"{stem}\t{found[stem][0]}\t{found[stem][1]}")
        return

    failures = []
    with ThreadPoolExecutor(max_workers=arguments.workers) as pool:
        pending = [pool.submit(write_plan, stem, *found[stem]) for stem in missing]
        for done, future in enumerate(as_completed(pending), start=1):
            stem, failure = future.result()
            if failure:
                failures.append((stem, failure))
            print(f"[{done}/{len(missing)}] {stem}: {failure or 'ok'}", flush=True)
    for stem, failure in failures:
        print(f"FAILED {stem}: {failure}", file=sys.stderr)
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
