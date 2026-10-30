#!/usr/bin/env python3
"""Draw, by driving the Codex CLI, the pawn of each Zirnakaynin NPC the Pierrebougie scenario has none for.

Each pawn is saved as the scenario's other pawns are: module/assets/pawns/<id>.png, 1122x1402, on white.
import_zirnakaynin_npcs.py then copies every pawn into PCGen.

    python3 scripts/generate_zirnakaynin_pawns.py --dry-run
    python3 scripts/generate_zirnakaynin_pawns.py --workers 2
    python3 scripts/generate_zirnakaynin_pawns.py --only generic-garde-de-mine --force

Spec: specs/donnees-zirnakaynin.md
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent / "combat_manager"))

from generate_summon_portraits import draw_with_codex  # noqa: E402
from import_zirnakaynin_npcs import SCENARIO_FOLDERS, SCENARIO_NPCS, SCENARIO_PAWNS  # noqa: E402

SCENARIO_PAWN_SIZE = (1122, 1402)
WHITE = (255, 255, 255)
WORN_SLOTS = {"Equipped", "Primary Hand", "Secondary Hand", "Both Hands", "Double Weapon", "Shield", "Armor", "Body",
              "Head", "Shoulders", "Torso", "Robe", "Shirt", "Belt", "Cloak", "Neck", "Feet", "Hands", "Wrists"}

PROMPT = """Generate exactly one image with your image generation tool, in portrait orientation (2:3, 1024x1536),
then save it as the PNG file {target}.
Subject: {name}, a non-player character of a Pathfinder Roleplaying Game campaign set in Zirnakaynin, a drow city of the
Darklands: a {gender} {race}, {classes}. Role: {role}{district}.{trait}
Visible gear: {gear}.
Style: photorealistic AAA blockbuster fantasy rendering, dramatic cinematic key lighting, physically accurate
materials, high production-value film concept art.
Composition: Pathfinder-style tabletop pawn. One single character, the whole body visible from head to feet with no
cropping, centered, vertical composition, facing slightly toward the viewer. Plain uniform pure white background, no
scenery, no ground line, no cast shadow, no base plate, no name plate, no text of any kind.
Do nothing else."""


def front_matter(markdown: Path) -> dict:
    return json.loads(re.match(r"^---json\s*([\s\S]*?)\s*---", markdown.read_text(encoding="utf-8")).group(1))


def section(markdown: Path, title: str) -> str:
    found = re.search(rf"^## {title}\n+(.+?)(?:\n## |\Z)", markdown.read_text(encoding="utf-8"), re.S | re.M)
    return found.group(1).strip() if found else ""


def pcg_values(pcg: Path, tag: str) -> list[str]:
    return [line.split("|")[0].removeprefix(f"{tag}:") for line in pcg.read_text(encoding="utf-8").splitlines()
            if line.startswith(f"{tag}:")]


def worn_gear(pcg: Path) -> list[str]:
    worn = []
    for line in pcg.read_text(encoding="utf-8").splitlines():
        slot = re.match(r"EQUIPSET:([^|]+)\|ID:[^|]+\|VALUE:([^|]+)", line)
        if slot and slot.group(1) in WORN_SLOTS:
            worn.append(slot.group(2))
    return worn


def prompt_for(identifier: str, folder: Path, target: Path) -> str:
    pcg, markdown = folder / f"{identifier}.pcg", folder / f"{identifier}.md"
    header = front_matter(markdown)
    classes = ", ".join(f"{name} {level}" for name, level in re.findall(r"^CLASS:([^|]+)\|.*?LEVEL:(\d+)",
                                                                        pcg.read_text(encoding="utf-8"), re.M))
    trait = section(markdown, "Trait")
    return PROMPT.format(target=target, name=header["name"], gender=(pcg_values(pcg, "GENDER") or [""])[0].lower(),
                         race=pcg_values(pcg, "RACE")[0], classes=classes, role=header.get("role", ""),
                         district=f", {header['district']}" if header.get("district") else "",
                         trait=f"\nTrait (in French): {trait}" if trait else "",
                         gear=", ".join(worn_gear(pcg)) or "plain clothes")


def frame(drawing: Image.Image, size: tuple[int, int]) -> Image.Image:
    """The drawing padded with white to the proportion of `size`, centered, then scaled to it."""
    ratio = size[0] / size[1]
    width, height = drawing.size
    framed_size = (width, round(width / ratio)) if width / height > ratio else (round(height * ratio), height)
    framed = Image.new("RGB", framed_size, WHITE)
    framed.paste(drawing.convert("RGB"), ((framed_size[0] - width) // 2, (framed_size[1] - height) // 2))
    return framed.resize(size, Image.LANCZOS)


def generate(identifier: str, folder: Path) -> tuple[str, str | None]:
    with tempfile.TemporaryDirectory(prefix="pawn-") as work_directory:
        generated_png = Path(work_directory) / "pawn.png"
        failure = draw_with_codex(prompt_for(identifier, folder, generated_png), generated_png)
        if failure:
            return identifier, failure
        with Image.open(generated_png) as drawing:
            frame(drawing, SCENARIO_PAWN_SIZE).save(SCENARIO_PAWNS / f"{identifier}.png", "PNG", optimize=True)
    return identifier, None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--workers", type=int, default=2, help="parallel Codex runs")
    parser.add_argument("--only", action="append", help="only this NPC, by file name without extension (repeatable)")
    parser.add_argument("--force", action="store_true", help="redraw NPCs that already have a pawn")
    parser.add_argument("--dry-run", action="store_true", help="print the prompts without drawing")
    arguments = parser.parse_args()

    folders = {pcg.stem: pcg.parent for name in SCENARIO_FOLDERS for pcg in (SCENARIO_NPCS / name).glob("*.pcg")}
    selected = arguments.only or sorted(folders)
    unknown = [identifier for identifier in selected if identifier not in folders]
    if unknown:
        sys.exit(f"Unknown NPCs: {unknown}")
    missing = [identifier for identifier in selected if arguments.force or not (SCENARIO_PAWNS / f"{identifier}.png").exists()]
    print(f"{len(missing)} pawns to draw out of {len(selected)}", flush=True)
    if arguments.dry_run:
        for identifier in missing:
            print(f"--- {identifier}\n{prompt_for(identifier, folders[identifier], Path('pawn.png'))}")
        return 0

    failures = []
    with ThreadPoolExecutor(max_workers=arguments.workers) as pool:
        pending = [pool.submit(generate, identifier, folders[identifier]) for identifier in missing]
        for done, future in enumerate(as_completed(pending), start=1):
            identifier, failure = future.result()
            if failure:
                failures.append((identifier, failure))
            print(f"[{done}/{len(missing)}] {identifier}: {failure or 'ok'}", flush=True)
    for identifier, failure in failures:
        print(f"FAILED {identifier}: {failure}", file=sys.stderr)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
