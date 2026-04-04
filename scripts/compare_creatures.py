#!/usr/bin/env python3
"""Compare creatures between pcfinder-csr and PCGen to find missing entries.

Produces:
  - scripts/creature_comparison.json  — missing creatures grouped by source
  - scripts/pcfinder_all_creatures.json — all deduplicated pcfinder creatures
  - scripts/pcgen_all_creatures.json   — all pcgen Pathfinder creatures
  - Console summary table

Usage:
    python3 scripts/compare_creatures.py \
        --source ../pcfinder/apps/pcfinder-csr/static-data/creatures.json
"""

import argparse
import json
import os
import re
import sys
from collections import defaultdict
from pathlib import Path

# ─── Source Normalization ───────────────────────────────────────────────────

# Map pcfinder source strings (after stripping page refs) to canonical names
SOURCE_NORMALIZE = {
    'PFRPG Bestiary': 'Bestiary 1',
    'PFRPG Bestiary 2': 'Bestiary 2',
    'PFRPG Bestiary 3': 'Bestiary 3',
    'PFRPG Bestiary 4': 'Bestiary 4',
    'PFRPG Bestiary 6': 'Bestiary 6',
    'Bestiary 3': 'Bestiary 3',
    'Bestiary 4': 'Bestiary 4',
    'Bestiary 5': 'Bestiary 5',
    'Bestiary 6': 'Bestiary 6',
    'Core Race': 'Core Rulebook',
    'RotRL-AE-Appendix': 'AP Rise of the Runelords',
    'TOH1': 'Tome of Horrors Complete',
    'Tome of Horrors Revised': 'Tome of Horrors Complete',
    'Book of the Damned V1': 'Book of the Damned Volume 1',
    'Book of the Damned V2': 'Book of the Damned Volume 2',
    'Book of the Damned': 'Book of the Damned Volume 1',
    'Pathfinder #137': 'AP 137',
    'Misfit Monsters': 'Misfit Monsters Redeemed',
}

# Sources to exclude entirely
EXCLUDED_SOURCES = {
    'd20pfsrd',           # No identifiable origin
    '(no source)',        # Missing source field
    'Player Character',   # PC entries
}

# Source prefixes to exclude (custom/modified creatures)
EXCLUDED_SOURCE_PREFIXES = (
    'Custom',
    'PJ ',
    'Homebrew',
    'Advanced Kyton',
    'Elite Kyton',
    'Elite Mythic',
    'Advanced Eremite',
    'Advanced Kyton',
    'Young Kyton',
    'Void-Touched',
)

# Sources already fully covered in PCGen (hand-crafted, don't generate)
SKIP_GENERATION_SOURCES = {
    'Bestiary 1', 'Bestiary 2', 'Bestiary 3', 'Bestiary 4',
    'Bestiary 5', 'Bestiary 6',
}


def normalize_source(raw_source):
    """Normalize a pcfinder source string to a canonical name."""
    if not raw_source:
        return '(no source)'
    # Strip page references
    source = re.sub(r'\s*pg\..*$', '', raw_source).strip()
    # Apply known mappings
    if source in SOURCE_NORMALIZE:
        return SOURCE_NORMALIZE[source]
    return source


def is_excluded_source(source):
    """Check if a source should be excluded from comparison."""
    if source in EXCLUDED_SOURCES:
        return True
    for prefix in EXCLUDED_SOURCE_PREFIXES:
        if source.startswith(prefix):
            return True
    return False


# ─── PCfinder Extraction ───────────────────────────────────────────────────

def extract_pcfinder(source_path):
    """Extract and deduplicate creatures from pcfinder creatures.json."""
    print(f"Loading pcfinder creatures from {source_path}...")
    with open(source_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    print(f"  Loaded {len(data)} total entries")

    # Filter and normalize
    creatures_by_source = defaultdict(dict)  # {source: {name_lower: creature_info}}
    skipped = defaultdict(int)

    for c in data:
        name = c.get('name', '').strip()
        if not name:
            skipped['no_name'] += 1
            continue

        # Must have type and hit dice to be a valid creature
        if not c.get('type'):
            skipped['no_type'] += 1
            continue
        if not c.get('numericHitDice'):
            skipped['no_hd'] += 1
            continue

        source = normalize_source(c.get('source', ''))
        if is_excluded_source(source):
            skipped['excluded_source'] += 1
            continue

        name_lower = name.lower()
        # Deduplicate: keep first occurrence per source
        if name_lower not in creatures_by_source[source]:
            creatures_by_source[source][name_lower] = {
                'name': name,
                'source': source,
                'raw_source': c.get('source', ''),
                'cr': c.get('cr', ''),
                'type': c.get('type', ''),
                'size': c.get('size', ''),
                'alignment': c.get('alignment', ''),
            }

    # Flatten
    all_creatures = []
    for source, creatures in sorted(creatures_by_source.items()):
        for info in creatures.values():
            all_creatures.append(info)

    total_sources = len(creatures_by_source)
    total_creatures = len(all_creatures)

    print(f"  After filtering/dedup: {total_creatures} creatures across {total_sources} sources")
    if skipped:
        print(f"  Skipped: {dict(skipped)}")

    return all_creatures, creatures_by_source


# ─── PCGen Extraction ──────────────────────────────────────────────────────

def extract_pcgen(data_dir):
    """Extract all creature/race names from PCGen Pathfinder LST files."""
    pathfinder_dir = os.path.join(data_dir, 'pathfinder')
    if not os.path.isdir(pathfinder_dir):
        print(f"ERROR: Pathfinder data directory not found: {pathfinder_dir}", file=sys.stderr)
        sys.exit(1)

    print(f"Scanning PCGen race files in {pathfinder_dir}...")

    all_creatures = []
    files_parsed = 0

    for root, dirs, files in os.walk(pathfinder_dir):
        for fname in files:
            if not fname.endswith('_races.lst') and not fname.endswith('_race.lst'):
                # Also check for files that contain race definitions
                if 'races' not in fname.lower():
                    continue
                if not fname.endswith('.lst'):
                    continue

            fpath = os.path.join(root, fname)
            creatures = parse_race_lst(fpath)
            if creatures:
                files_parsed += 1
                all_creatures.extend(creatures)

    print(f"  Parsed {files_parsed} race files, found {len(all_creatures)} creature entries")
    return all_creatures


def parse_race_lst(filepath):
    """Parse a PCGen race LST file and extract creature names."""
    creatures = []
    source_long = ''

    try:
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            lines = f.readlines()
    except (IOError, UnicodeDecodeError):
        return creatures

    rel_path = filepath
    try:
        rel_path = os.path.relpath(filepath)
    except ValueError:
        pass

    for line in lines:
        line = line.rstrip('\n\r')

        # Extract SOURCE header
        if line.startswith('SOURCELONG:'):
            m = re.match(r'SOURCELONG:([^\t]+)', line)
            if m:
                source_long = m.group(1).strip()
            continue

        # Skip comments, empty lines
        if not line or line.startswith('#') or line.startswith('SOURCE'):
            continue

        # Split by tabs
        fields = line.split('\t')
        name = fields[0].strip()

        # Skip .MOD, .COPY, .FORGET entries
        if any(name.endswith(suffix) for suffix in ('.MOD', '.COPY', '.FORGET')):
            continue

        # Skip empty names
        if not name:
            continue

        # Extract KEY if present
        key = ''
        for field in fields[1:]:
            if field.startswith('KEY:'):
                key = field[4:].strip()
                break

        creatures.append({
            'name': name,
            'key': key,
            'source': source_long,
            'file': rel_path,
        })

    return creatures


# ─── Matching ──────────────────────────────────────────────────────────────

def normalize_name(name):
    """Normalize a creature name for matching."""
    n = name.lower().strip()
    # Remove parenthetical qualifiers like "(human form)"
    n = re.sub(r'\s*\([^)]*\)\s*', ' ', n).strip()
    # Collapse whitespace
    n = re.sub(r'\s+', ' ', n)
    return n


def reverse_comma_name(name):
    """Reverse comma-separated name: 'Dragon, Adult Black' → 'adult black dragon'."""
    if ', ' in name:
        parts = name.split(', ', 1)
        return f"{parts[1]} {parts[0]}".lower().strip()
    return None


def build_pcgen_name_set(pcgen_creatures):
    """Build sets of normalized names and keys from pcgen creatures."""
    names = set()
    keys = set()
    for c in pcgen_creatures:
        names.add(normalize_name(c['name']))
        if c.get('key'):
            keys.add(normalize_name(c['key']))
        # Also add reversed comma names
        rev = reverse_comma_name(c['name'])
        if rev:
            names.add(rev)
    return names, keys


def creature_exists_in_pcgen(name, pcgen_names, pcgen_keys):
    """Check if a creature name matches any pcgen entry."""
    norm = normalize_name(name)

    # Exact match
    if norm in pcgen_names:
        return True
    if norm in pcgen_keys:
        return True

    # Try reversed comma name
    rev = reverse_comma_name(name)
    if rev and rev in pcgen_names:
        return True

    # Try unreversing: if pcgen has "Dragon, Adult Black" and pcfinder has "Adult Black Dragon"
    # This is handled by adding reversed names to pcgen_names in build_pcgen_name_set

    return False


# ─── Comparison ────────────────────────────────────────────────────────────

def compare(pcfinder_by_source, pcgen_creatures):
    """Compare pcfinder creatures against pcgen and find missing ones."""
    pcgen_names, pcgen_keys = build_pcgen_name_set(pcgen_creatures)

    missing_by_source = {}
    matched_count = 0
    missing_count = 0
    skipped_count = 0

    for source in sorted(pcfinder_by_source.keys()):
        creatures = pcfinder_by_source[source]

        # Skip sources already fully covered
        if source in SKIP_GENERATION_SOURCES:
            skipped_count += len(creatures)
            continue

        missing = []
        for name_lower, info in sorted(creatures.items()):
            if creature_exists_in_pcgen(info['name'], pcgen_names, pcgen_keys):
                matched_count += 1
            else:
                missing.append(info)
                missing_count += 1

        if missing:
            missing_by_source[source] = missing

    return missing_by_source, matched_count, missing_count, skipped_count


# ─── Output ────────────────────────────────────────────────────────────────

def print_summary(missing_by_source, matched, missing, skipped):
    """Print a console summary table."""
    print(f"\n{'='*70}")
    print(f"COMPARISON SUMMARY")
    print(f"{'='*70}")
    print(f"  Matched (already in pcgen):  {matched}")
    print(f"  Missing (to generate):       {missing}")
    print(f"  Skipped (B1-6):              {skipped}")
    print(f"{'='*70}")
    print(f"\n{'Source':<45} {'Missing':>8}")
    print(f"{'-'*45} {'-'*8}")

    for source in sorted(missing_by_source.keys(), key=lambda s: -len(missing_by_source[s])):
        count = len(missing_by_source[source])
        print(f"  {source:<43} {count:>6}")

    total = sum(len(v) for v in missing_by_source.values())
    print(f"{'-'*45} {'-'*8}")
    print(f"  {'TOTAL':<43} {total:>6}")
    print()


def save_reports(missing_by_source, pcfinder_all, pcgen_all, output_dir):
    """Save JSON reports."""
    # Missing creatures report
    comparison_path = os.path.join(output_dir, 'creature_comparison.json')
    # Convert to serializable format
    comparison_data = {}
    for source, creatures in sorted(missing_by_source.items()):
        comparison_data[source] = sorted(creatures, key=lambda c: c['name'])

    with open(comparison_path, 'w', encoding='utf-8') as f:
        json.dump(comparison_data, f, indent=2, ensure_ascii=False)
    print(f"Wrote missing creatures report: {comparison_path}")

    # All pcfinder creatures
    pcfinder_path = os.path.join(output_dir, 'pcfinder_all_creatures.json')
    with open(pcfinder_path, 'w', encoding='utf-8') as f:
        json.dump(sorted(pcfinder_all, key=lambda c: (c['source'], c['name'])),
                  f, indent=2, ensure_ascii=False)
    print(f"Wrote pcfinder creatures: {pcfinder_path}")

    # All pcgen creatures
    pcgen_path = os.path.join(output_dir, 'pcgen_all_creatures.json')
    with open(pcgen_path, 'w', encoding='utf-8') as f:
        json.dump(sorted(pcgen_all, key=lambda c: (c['source'], c['name'])),
                  f, indent=2, ensure_ascii=False)
    print(f"Wrote pcgen creatures: {pcgen_path}")


# ─── Main ──────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description='Compare pcfinder and PCGen creature lists to find missing entries')
    parser.add_argument('--source', required=True,
                        help='Path to pcfinder creatures.json')
    parser.add_argument('--pcgen-data', default=None,
                        help='Path to PCGen data/ directory')
    parser.add_argument('--output-dir', default=None,
                        help='Directory for output reports (default: scripts/)')
    args = parser.parse_args()

    # Determine paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    if args.pcgen_data:
        pcgen_data = args.pcgen_data
    else:
        pcgen_data = os.path.join(os.path.dirname(script_dir), 'data')

    output_dir = args.output_dir or script_dir

    if not os.path.isdir(pcgen_data):
        print(f"ERROR: PCGen data directory not found: {pcgen_data}", file=sys.stderr)
        sys.exit(1)

    # Extract
    pcfinder_all, pcfinder_by_source = extract_pcfinder(args.source)
    pcgen_all = extract_pcgen(pcgen_data)

    # Compare
    missing_by_source, matched, missing, skipped = compare(pcfinder_by_source, pcgen_all)

    # Output
    print_summary(missing_by_source, matched, missing, skipped)
    save_reports(missing_by_source, pcfinder_all, pcgen_all, output_dir)

    print("Done!")


if __name__ == '__main__':
    main()
