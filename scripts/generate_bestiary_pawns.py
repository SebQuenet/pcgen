#!/usr/bin/env python3
"""Generate one white-background pawn per bestiary creature by driving the Codex CLI.

Merges PCGen's Pathfinder bestiary races with PCFinder's creatures.json, a creature
both hold being drawn once under its PCGen key. Each pawn is kept twice: the drawing
at its native resolution as a PNG outside the repository, and a 400x600 JPEG in
code/src/resources/pcgen/bestiary/pawns/. Creatures that already have their PNG are
not drawn again, so an interrupted run resumes.

Usage:
    python3 scripts/generate_bestiary_pawns.py --dry-run
    python3 scripts/generate_bestiary_pawns.py --workers 4 --limit 20
    python3 scripts/generate_bestiary_pawns.py --only giant_stone --force
    python3 scripts/generate_bestiary_pawns.py --only penanggalen --description "a floating severed head..."
"""

import argparse
import json
import re
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, replace
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent / "combat_manager"))

from catalog import readings  # noqa: E402
from generate_summon_portraits import draw_with_codex, portrait_name  # noqa: E402

REPOSITORY = Path(__file__).resolve().parent.parent
PCGEN_BESTIARIES = REPOSITORY / "data/pathfinder/paizo/roleplaying_game"
PCFINDER_CREATURES = REPOSITORY.parent / "pcfinder/apps/pcfinder-csr/static-data/creatures.json"
NATIVE_PAWNS = REPOSITORY.parent / "pcfinder-images/bestiary"
PAWNS = REPOSITORY / "code/src/resources/pcgen/bestiary/pawns"
PAWN_SIZE = (400, 600)
PAWN_RATIO = PAWN_SIZE[0] / PAWN_SIZE[1]
JPEG_QUALITY = 85
WHITE = (255, 255, 255)
SIZE_NAMES = {"F": "Fine", "D": "Diminutive", "T": "Tiny", "S": "Small", "M": "Medium", "L": "Large",
              "H": "Huge", "G": "Gargantuan", "C": "Colossal"}

PROMPT = """Generate exactly one image with your image generation tool, in portrait orientation (2:3, 1024x1536),
then save it as the PNG file {target}.
Subject: the Pathfinder Roleplaying Game creature "{name}"{details}, faithful to its official description in {source}.{notes}
Style: photorealistic AAA blockbuster fantasy rendering, dramatic cinematic key lighting, physically accurate
materials, high production-value film concept art.
Composition: Pathfinder-style tabletop pawn. One single creature, its whole body visible with no cropping, centered,
vertical composition, facing slightly toward the viewer. Plain uniform pure white background, no scenery, no ground
line, no cast shadow, no base plate, no name plate, no text of any kind.
Do nothing else."""


@dataclass(frozen=True)
class Creature:
    name: str
    size: str
    creature_type: str
    source: str
    description: str = ""


def read_pcgen_races():
    """The races of PCGen's bestiary books, from their *_races.lst files."""
    creatures = []
    for races_file in sorted(PCGEN_BESTIARIES.glob("*bestiary*/*_races.lst")):
        source = "the Pathfinder Bestiary"
        for line in races_file.read_text(encoding="utf-8").splitlines():
            fields = line.split("\t")
            if line.startswith("SOURCELONG:"):
                source = fields[0].removeprefix("SOURCELONG:")
            if line.startswith(("#", "SOURCE")) or not line.strip() or re.search(r"\.(MOD|COPY|FORGET)", fields[0]):
                continue
            tokens = dict(field.split(":", 1) for field in fields[1:] if ":" in field)
            subtypes = tokens.get("RACESUBTYPE", "").replace("|", ", ")
            creature_type = tokens.get("RACETYPE", "") + (f" ({subtypes})" if subtypes else "")
            creatures.append(Creature(fields[0].strip(), SIZE_NAMES.get(tokens.get("SIZE", ""), ""), creature_type, source))
    return creatures


def read_pcfinder_creatures():
    creatures = []
    for entry in json.loads(PCFINDER_CREATURES.read_text(encoding="utf-8")):
        subtypes = ", ".join(entry.get("subTypes") or [])
        creature_type = (entry.get("type") or "") + (f" ({subtypes})" if subtypes else "")
        creatures.append(Creature(entry["name"].strip(), entry.get("size") or "", creature_type,
                                  entry.get("source") or "a Pathfinder bestiary", entry.get("description") or ""))
    return creatures


def race_readings(race_key):
    """Every lower-cased name PCFinder may give the race keyed "Aeon (Akhana)": also "akhana" and "aeon, akhana"."""
    names = readings(race_key)
    qualified = re.match(r"^([^(]+?) \(([^)]+)\)$", race_key.lower())
    if qualified:
        base, qualifiers = qualified.groups()
        names += [qualifiers.replace("/", " "), f"{base}, {qualifiers.replace('/', ', ')}"]
    return names


def merge_bestiaries(pcgen_races, pcfinder_creatures):
    """Each creature to draw by its pawn's file stem; a PCGen race and its PCFinder namesake are one."""
    race_keys = {}
    for race in sorted(pcgen_races, key=lambda race: (len(race.name), race.name)):
        for reading in race_readings(race.name):
            race_keys.setdefault(reading, race.name)
    pawns = {}
    for creature in pcfinder_creatures:
        race_key = race_keys.get(re.sub(r"\s+", " ", creature.name.lower()))
        pawns.setdefault(Path(portrait_name(race_key or creature.name)).stem, creature)
    for race in pcgen_races:
        pawns.setdefault(Path(portrait_name(race.name)).stem, race)
    return pawns


def frame_as_pawn(drawing):
    """The drawing padded with white to the pawn's 2:3 proportion, centered."""
    width, height = drawing.size
    framed_size = (width, round(width / PAWN_RATIO)) if width / height > PAWN_RATIO else (round(height * PAWN_RATIO), height)
    framed = Image.new("RGB", framed_size, WHITE)
    framed.paste(drawing.convert("RGB"), ((framed_size[0] - width) // 2, (framed_size[1] - height) // 2))
    return framed


def prompt_for(creature, target):
    details = ", ".join(part for part in (creature.size, creature.creature_type) if part)
    return PROMPT.format(target=target, name=creature.name, details=f" ({details})" if details else "",
                         source=creature.source,
                         notes=f"\nNotes from the bestiary (may be in French): {creature.description}" if creature.description else "")


def store_reduced(native_png, pawn_jpeg):
    with Image.open(native_png) as native:
        native.convert("RGB").resize(PAWN_SIZE, Image.LANCZOS).save(pawn_jpeg, "JPEG", quality=JPEG_QUALITY, optimize=True)


def generate(stem, creature):
    native_png = NATIVE_PAWNS / f"{stem}.png"
    with tempfile.TemporaryDirectory(prefix="pawn-") as work_directory:
        generated_png = Path(work_directory) / "pawn.png"
        failure = draw_with_codex(prompt_for(creature, generated_png), generated_png)
        if failure:
            return stem, failure
        with Image.open(generated_png) as drawing:
            frame_as_pawn(drawing).save(native_png, "PNG", optimize=True)
    store_reduced(native_png, PAWNS / f"{stem}.jpg")
    return stem, None


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--workers", type=int, default=2, help="parallel Codex runs")
    parser.add_argument("--only", action="append", help="only this pawn file stem, e.g. giant_stone (repeatable)")
    parser.add_argument("--force", action="store_true", help="redraw creatures that already have a pawn")
    parser.add_argument("--limit", type=int, help="draw at most this many creatures")
    parser.add_argument("--description", help="the appearance to draw instead of the bestiary's, for a refused drawing")
    parser.add_argument("--dry-run", action="store_true", help="list the creatures to draw without drawing them")
    arguments = parser.parse_args()

    NATIVE_PAWNS.mkdir(parents=True, exist_ok=True)
    PAWNS.mkdir(parents=True, exist_ok=True)
    pawns = merge_bestiaries(read_pcgen_races(), read_pcfinder_creatures())
    selected = arguments.only or sorted(pawns)
    unknown = [stem for stem in selected if stem not in pawns]
    if unknown:
        sys.exit(f"Unknown creatures: {unknown}")
    if arguments.description:
        pawns.update({stem: replace(pawns[stem], description=arguments.description) for stem in selected})

    for stem in selected:
        if (NATIVE_PAWNS / f"{stem}.png").exists() and not (PAWNS / f"{stem}.jpg").exists():
            store_reduced(NATIVE_PAWNS / f"{stem}.png", PAWNS / f"{stem}.jpg")
    missing = [stem for stem in selected if arguments.force or not (NATIVE_PAWNS / f"{stem}.png").exists()]
    missing = missing[:arguments.limit]
    print(f"{len(missing)} pawns to draw out of {len(selected)}", flush=True)
    if arguments.dry_run:
        for stem in missing:
            print(f"{stem}\t{pawns[stem].name}\t{pawns[stem].source}")
        return

    failures = []
    with ThreadPoolExecutor(max_workers=arguments.workers) as pool:
        pending = [pool.submit(generate, stem, pawns[stem]) for stem in missing]
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
