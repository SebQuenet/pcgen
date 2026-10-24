#!/usr/bin/env python3
"""Generate one portrait per summonable race by driving the Codex CLI.

Reads the races from code/src/resources/pcgen/conjurations/pathfinder1-options.tsv,
asks `codex exec` to draw each missing one with its image generation tool, and
stores it as a 250x250 JPEG in code/src/resources/pcgen/conjurations/portraits/.
Races that already have a portrait are skipped, so an interrupted run resumes.

Usage:
    python3 scripts/generate_summon_portraits.py --workers 4
    python3 scripts/generate_summon_portraits.py --race "Archon (Hound)" --force
    python3 scripts/generate_summon_portraits.py --race "Protean (Naunet)" --force \\
        --description "a 12-foot serpentine creature with a reptilian face..."
"""

import argparse
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

from PIL import Image

REPOSITORY = Path(__file__).resolve().parent.parent
OPTIONS = REPOSITORY / "code/src/resources/pcgen/conjurations/pathfinder1-options.tsv"
PORTRAITS = REPOSITORY / "code/src/resources/pcgen/conjurations/portraits"
PORTRAIT_SIZE = (250, 250)
JPEG_QUALITY = 85
CODEX_TIMEOUT_SECONDS = 900
CODEX_POLL_SECONDS = 30
CODEX_GENERATED_IMAGES = Path.home() / ".codex/generated_images"

PROMPT = """Generate exactly one image with your image generation tool, then save it as the PNG file {target}.
Subject: the Pathfinder Roleplaying Game creature "{printed_name}" (bestiary entry "{race}"{race_type}),
faithful to its Pathfinder Bestiary description.{appearance}
Style: cinematic heroic fantasy blockbuster film concept art, epic and realistic, dramatic lighting on the creature.
Composition: one single creature, whole body visible, centered, isolated on a pure plain white background,
no scenery, no ground, no text, no frame, no border. Square format.
Do nothing else."""


def portrait_name(race):
    """The portrait's file name; PathfinderSummonPortraits derives the same name in Java."""
    return re.sub(r"[^a-z0-9]+", "_", race.lower()).strip("_") + ".jpg"


def read_races():
    """Each distinct race with the printed name and creature type of its first row."""
    races = {}
    for line in OPTIONS.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        fields = line.split("\t")
        race = fields[3] or fields[2]
        races.setdefault(race, (fields[2], fields[7] if len(fields) > 7 else ""))
    return races


def draw_with_codex(prompt, generated_png):
    """Has `codex exec` draw `prompt` into `generated_png`; the reason it failed, or None.

    The deadline is on the wall clock, so a run the computer slept through times out on waking,
    and a timed-out run is killed with its whole process group, the Codex binary the launcher started included.
    Codex keeps nothing: the session is ephemeral and the copy of the image it files away is removed."""
    codex = subprocess.Popen(
        ["codex", "exec", "--ephemeral", "--json", "--skip-git-repo-check", "-s", "workspace-write",
         "-C", str(generated_png.parent), prompt],
        stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, start_new_session=True)
    deadline = time.time() + CODEX_TIMEOUT_SECONDS
    while True:
        try:
            output, _ = codex.communicate(timeout=CODEX_POLL_SECONDS)
            break
        except subprocess.TimeoutExpired:
            if time.time() > deadline:
                os.killpg(codex.pid, signal.SIGKILL)
                remove_filed_images(codex.communicate()[0])
                return "timeout"
    remove_filed_images(output)
    if not generated_png.is_file():
        return f"no image (codex exit {codex.returncode}): {output[-300:]}"
    return None


def remove_filed_images(json_events):
    """Removes the copies Codex filed under its thread id, named by the run's thread.started event."""
    for line in json_events.splitlines():
        if '"thread.started"' in line:
            shutil.rmtree(CODEX_GENERATED_IMAGES / json.loads(line)["thread_id"], ignore_errors=True)
            return


def generate(race, printed_name, race_type, description=None):
    target_jpeg = PORTRAITS / portrait_name(race)
    with tempfile.TemporaryDirectory(prefix="portrait-") as work_directory:
        generated_png = Path(work_directory) / "portrait.png"
        prompt = PROMPT.format(target=generated_png, printed_name=printed_name, race=race,
                               race_type=f", type {race_type}" if race_type else "",
                               appearance=f"\nAppearance: {description}" if description else "")
        failure = draw_with_codex(prompt, generated_png)
        if failure:
            return race, failure
        with Image.open(generated_png) as drawn:
            drawn.convert("RGB").resize(PORTRAIT_SIZE, Image.LANCZOS).save(
                target_jpeg, "JPEG", quality=JPEG_QUALITY, optimize=True)
    return race, None


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--workers", type=int, default=2, help="parallel Codex runs")
    parser.add_argument("--race", action="append", help="only this race key (repeatable)")
    parser.add_argument("--force", action="store_true", help="redraw races that already have a portrait")
    parser.add_argument("--description", help="the creature's appearance, for a name the model misreads")
    arguments = parser.parse_args()

    PORTRAITS.mkdir(parents=True, exist_ok=True)
    races = read_races()
    selected = arguments.race or list(races)
    unknown = [race for race in selected if race not in races]
    if unknown:
        sys.exit(f"Unknown races: {unknown}")
    missing = [race for race in selected if arguments.force or not (PORTRAITS / portrait_name(race)).exists()]
    print(f"{len(missing)} portraits to draw out of {len(selected)}", flush=True)

    failures = []
    with ThreadPoolExecutor(max_workers=arguments.workers) as pool:
        pending = [pool.submit(generate, race, *races[race], arguments.description) for race in missing]
        for done, future in enumerate(as_completed(pending), start=1):
            race, failure = future.result()
            if failure:
                failures.append((race, failure))
            print(f"[{done}/{len(missing)}] {race}: {failure or 'ok'}", flush=True)
    for race, failure in failures:
        print(f"FAILED {race}: {failure}", file=sys.stderr)
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
