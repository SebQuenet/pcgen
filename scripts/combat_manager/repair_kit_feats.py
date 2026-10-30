#!/usr/bin/env python3
"""Give the feats of the generated kits a choice PCGen defines.

    python3 scripts/combat_manager/repair_kit_feats.py [--dry-run]

The kits written from stat blocks copy a feat's choice as the stat block words it: "Weapon Focus (spiked chain)",
"Improved Critical (tail sting)". PCGen logs every choice no weapon proficiency or skill of its data answers. A choice
is renamed after the key PCGen gives it, "Weapon Focus (Chain (Spiked))", and its line is dropped when PCGen has none.

The feat's own name is read the same way: spelt with other spaces and hyphens ("Fly-by Attack"), glued to the mark of
its book ("Charge ThroughAPG", "Outflank*"), misspelt or under its 3.5 name ("Mulitattack", "Expertise"). A choice the
stat block listed after a comma ("Skill Focus (Perception, Stealth)") was cut into a line of its own, "Stealth)": it
goes back to the feat before it. A feat no book defines is dropped, so that the creature can still be built.
"""

from __future__ import annotations

import dataclasses
import re
import sys
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPTS_DIR))
sys.path.insert(0, str(Path(__file__).resolve().parent))

import catalog  # noqa: E402
import monsters  # noqa: E402

FEAT_LINE = re.compile(r'^(ABILITY:CATEGORY=FEAT\|)([^\t]+)(.*)$', re.S)
CHOSEN = re.compile(r'^(.+?) \((.+)\)$')
# Feats whose choice is a skill
SKILL_FEATS = ('Skill Focus', 'Master Craftsman')
ABILITY_FOCUS = 'Ability Focus'
# The mark of the book a stat block glues to a feat, "OutflankB APG", "Rending Claws*"
BOOK_MARK = re.compile(r'(?:B\s*)?(?:APG|UC|UM|ARG|ACG)$|\*+$')
SEE_ELSEWHERE = re.compile(r'\s*\(see [^)]*\)$', re.IGNORECASE)
SPELL_LIKE_ABILITY_FEAT = re.compile(r'^(\w+) Spell-Like Abilit(?:y|ies)$', re.IGNORECASE)
# Feats a stat block misspells or names as 3.5 did, by the reading of their name without spaces and hyphens
RENAMED_FEATS = {
    'mulitattack': 'Multiattack', 'lightingreflexes': 'Lightning Reflexes', 'improveinitiative': 'Improved Initiative',
    'improvedinititiative': 'Improved Initiative', 'expertise': 'Combat Expertise', 'greatercleave': 'Great Cleave',
    'lightarmorproficiency': 'Armor Proficiency (Light)', 'mediumarmorproficiency': 'Armor Proficiency (Medium)',
}
NATURAL_ATTACKS = re.compile(r'NATURALATTACKS:([^\t\n]+)')


def repaired_lines(lines: list[str], pcgen: monsters.Catalog, ability_focuses: frozenset[str]) -> list[str]:
    """The kit's lines, each feat named as PCGen keys it and those PCGen defines no such feat or choice for left out."""
    kept = []
    previous_base = None
    for line in lines:
        feat = FEAT_LINE.match(line)
        # Several feats named on one line, or a choice glued to its feat as hand-written data does, are left as written
        if not feat or '|' in feat.group(2) or re.search(r'\S\(', feat.group(2)):
            previous_base = None
            kept.append(line)
            continue
        name = feat.group(2).strip()
        if name.endswith(')') and '(' not in name and previous_base:
            name = f'{previous_base} ({name[:-1].strip()})'
        named = feat_named(name, pcgen)
        repaired_line = repaired(f'{feat.group(1)}{named}{feat.group(3)}', pcgen, ability_focuses) if named else None
        if repaired_line is None:
            continue
        chosen = CHOSEN.match(named)
        previous_base = chosen.group(1) if chosen else None
        kept.append(repaired_line)
    return kept


def feat_named(name: str, pcgen: monsters.Catalog) -> str | None:
    """The feat as PCGen keys it, its choice kept; None when no book defines it."""
    name = BOOK_MARK.sub('', SEE_ELSEWHERE.sub('', name)).strip()
    whole = feat_key(name, pcgen.feats)
    if whole:
        return whole
    chosen = CHOSEN.match(name)
    base, choice = chosen.groups() if chosen else (name, None)
    spell_like = SPELL_LIKE_ABILITY_FEAT.match(base)
    if spell_like:
        base = f'{spell_like.group(1).title()} Spell-Like Ability ~ Spell'
    key = feat_key(base, pcgen.feats)
    if not key:
        return None
    # A stat block may qualify a feat that takes no choice, as "Awesome Blow (tail slap only)"
    return f'{key} ({choice})' if choice and key in pcgen.choosing_feats else key


def feat_key(name: str, feats: catalog.KeyIndex) -> str | None:
    squashed = squash(name)
    if squashed in RENAMED_FEATS:
        return RENAMED_FEATS[squashed]
    return next((key for key in feats.keys if squash(key) == squashed), None)


def squash(name: str) -> str:
    """The name read without case, spaces or hyphens: "Fly-by Attack" and "Flyby Attack" read alike."""
    return re.sub(r'[\s\-]', '', name.lower())


def repaired(line: str, pcgen: monsters.Catalog, ability_focuses: frozenset[str] = frozenset()) -> str | None:
    """The kit line with its feat's choice named as PCGen keys it; None when PCGen defines no such choice."""
    feat = FEAT_LINE.match(line)
    chosen = CHOSEN.match(feat.group(2)) if feat else None
    if not chosen:
        return line
    base, choice = chosen.groups()
    if base == ABILITY_FOCUS:
        focus = next((name for name in ability_focuses if name.lower() == choice.lower()), None)
        return f'{feat.group(1)}{base} ({focus}){feat.group(3)}' if focus else None
    if base in monsters.WEAPON_FEATS:
        index, resolve = pcgen.weapon_profs, lambda name: named_exactly(pcgen.weapon_profs, name)
    elif base in SKILL_FEATS:
        index, resolve = pcgen.skills, lambda name: monsters.exact(pcgen.skills, name)
    else:
        return line
    if is_defined(index, choice):
        return line
    key = resolve(choice)
    return f'{feat.group(1)}{base} ({key}){feat.group(3)}' if key else None


def named_exactly(index: catalog.KeyIndex, name: str) -> str | None:
    """The key one of the name's spellings reads as, never a key that only starts alike: "rope" is no Rope Dart."""
    return next((index.by_reading[candidate] for candidate in catalog.candidate_names(name) if candidate in index.by_reading),
                None)


def is_defined(index: catalog.KeyIndex, choice: str) -> bool:
    """True when the choice is a key of the index; PCGen matches it whatever its case."""
    return choice.lower() in {key.lower() for key in index.keys}


def natural_attacks(data_dir: Path) -> set[str]:
    """The name of every natural attack the data's races and templates give."""
    names = set()
    for path in data_dir.rglob('*.lst'):
        for attacks in NATURAL_ATTACKS.findall(path.read_text(encoding='utf-8', errors='replace')):
            names |= {attack.split(',')[0].strip() for attack in attacks.split('|') if attack.strip()}
    return names


def main() -> None:
    import import_monsters

    loaded = import_monsters.load_catalog()
    # PCGen makes a weapon proficiency of every natural attack a race or a template names
    pcgen = dataclasses.replace(loaded, weapon_profs=catalog.KeyIndex(
        sorted(set(loaded.weapon_profs.keys) | natural_attacks(import_monsters.DATA_DIR))))
    ability_focuses = frozenset(import_monsters.objects_of_category(ABILITY_FOCUS))
    is_dry_run = '--dry-run' in sys.argv
    renamed = dropped = 0
    for path in sorted(import_monsters.DATA_DIR.rglob('*kits*.lst')):
        lines = path.read_text(encoding='utf-8').split('\n')
        kept = repaired_lines(lines, pcgen, ability_focuses)
        if kept == lines:
            continue
        before, after = set(lines), set(kept)
        for line in sorted(before - after):
            print(f'{path.relative_to(import_monsters.DATA_DIR)}: - {line}')
        for line in sorted(after - before):
            print(f'{path.relative_to(import_monsters.DATA_DIR)}: + {line}')
        renamed += len(after - before)
        dropped += len(before - after) - len(after - before)
        if not is_dry_run:
            path.write_text('\n'.join(kept), encoding='utf-8')
    print(f'{renamed} lines renamed, {dropped} lines dropped')


if __name__ == '__main__':
    main()
