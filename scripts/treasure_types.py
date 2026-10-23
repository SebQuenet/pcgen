#!/usr/bin/env python3
"""Extract the bestiary treasure type of each creature from pcfinder-csr creatures.json.

The game table reads the result to know what treasure a monster leaves.

Usage:
    python3 scripts/treasure_types.py \\
        --source ../pcfinder/apps/pcfinder-csr/static-data/creatures.json \\
        --output code/src/resources/pcgen/core/gametable/treasure-types.json
"""

import argparse
import json
from pathlib import Path


def treasure_by_name(creatures):
    """The treasure line of each creature, keyed by its lowercased name; the first entry of a name wins."""
    treasures = {}
    for creature in creatures:
        treasure = (creature.get('treasure') or '').strip()
        name = (creature.get('name') or '').strip().lower()
        if treasure and name and name not in treasures:
            treasures[name] = treasure
    return treasures


def main():
    parser = argparse.ArgumentParser(description='Extract the treasure type of each creature from creatures.json')
    parser.add_argument('--source', required=True, type=Path, help='Path to creatures.json')
    parser.add_argument('--output', required=True, type=Path, help='Path of the JSON file to write')
    args = parser.parse_args()

    creatures = json.loads(args.source.read_text(encoding='utf-8'))
    treasures = treasure_by_name(creatures)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(dict(sorted(treasures.items())), ensure_ascii=False, indent=1) + '\n',
                           encoding='utf-8')
    print(f'{len(treasures)} creatures written to {args.output}')


if __name__ == '__main__':
    main()
