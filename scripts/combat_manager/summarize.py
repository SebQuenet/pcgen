"""Add up the import reports: how often PCGen's numbers match the stat block, and what was missing.

    python3 scripts/combat_manager/summarize.py [characters/combat-manager/NPC Codex]
"""

from __future__ import annotations

import collections
import json
import sys
from pathlib import Path

DEFAULT_DIR = Path(__file__).resolve().parents[2] / 'characters' / 'combat-manager'
NUMBERS = ('hp', 'ac', 'touch', 'flat_footed', 'fort', 'ref', 'will', 'bab', 'cmb', 'cmd', 'init')
MISSING_LISTS = ('missing_classes', 'missing_templates', 'missing_items', 'missing_modifiers', 'missing_spells', 'refused_feats')
MOST_COMMON = 15


def main() -> int:
    folder = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_DIR
    reports = [json.loads(path.read_text(encoding='utf-8')) for path in folder.rglob('*.report.json')]
    print(f'{len(reports)} PNJ construits sous {folder}')
    if not reports:
        return 0

    matches = collections.Counter()
    skills_matching = skills_total = scores_matching = scores_total = 0
    for report in reports:
        comparison = report.get('comparison', {})
        matches.update(name for name in NUMBERS if comparison.get(name, [None, 0])[0] == comparison.get(name, [0, None])[1])
        for published, computed in comparison.get('skills', {}).values():
            skills_total += 1
            skills_matching += published == computed
        for published, computed in comparison.get('scores', {}).values():
            scores_total += 1
            scores_matching += published == computed
    print('\nValeur calculée égale à la valeur publiée :')
    for name in NUMBERS:
        print(f'  {name:<12} {matches[name] * 100 // len(reports):>3} %')
    print(f'  {"caract.":<12} {scores_matching * 100 // max(1, scores_total):>3} %')
    print(f'  {"compétences":<12} {skills_matching * 100 // max(1, skills_total):>3} %')

    for list_name in MISSING_LISTS:
        counted = collections.Counter(entry[:90] for report in reports for entry in report.get(list_name, []))
        print(f'\n{list_name} ({sum(counted.values())}) :')
        for entry, count in counted.most_common(MOST_COMMON):
            print(f'  {count:>4}  {entry}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
