"""Read Combat Manager's NPCs out of the files its installer unpacks.

The installer names its files by hash, so they are told apart by their content:
the stat blocks' numbers live in XML files whose root is <ArrayOfMonster>, their
text (feats, skills, gear, spells) in a SQLite table keyed by the same id.
"""

from __future__ import annotations

import sqlite3
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

SQLITE_HEADER = b'SQLite format 3\x00'
MONSTER_LIST_MARK = b'<ArrayOfMonster'
TEXT_COLUMNS = ('Gear', 'OtherGear', 'Feats', 'Skills', 'SpellsKnown', 'SpellsPrepared', 'BeforeCombat', 'DuringCombat', 'Morale')


@dataclass(frozen=True)
class NpcRecord:
    identifier: int
    fields: dict[str, str]

    def get(self, name: str) -> str | None:
        text = self.fields.get(name)
        return text.strip() if text and text.strip() else None


def find_files(extracted_dir: Path) -> tuple[list[Path], Path]:
    monster_lists, database = [], None
    for path in sorted(extracted_dir.iterdir()):
        if not path.is_file():
            continue
        with path.open('rb') as handle:
            head = handle.read(200)
        if head.startswith(SQLITE_HEADER):
            database = path
        elif MONSTER_LIST_MARK in head:
            monster_lists.append(path)
    if database is None or not monster_lists:
        raise FileNotFoundError(f'no Combat Manager data in {extracted_dir}')
    return monster_lists, database


def load_npcs(extracted_dir: Path) -> list[NpcRecord]:
    """Every stat block that has class levels, with its text joined in."""
    monster_lists, database = find_files(extracted_dir)
    connection = sqlite3.connect(f'file:{database}?mode=ro', uri=True)
    connection.row_factory = sqlite3.Row
    npcs = []
    for path in monster_lists:
        for monster in ElementTree.parse(path).getroot():
            fields = {child.tag: child.text or '' for child in monster}
            if not fields.get('Class', '').strip():
                continue
            identifier = int(fields['id'])
            row = connection.execute(f'select {", ".join(TEXT_COLUMNS)} from Bestiary where ID = ?', (identifier,)).fetchone()
            if row:
                fields.update({column: row[column] or '' for column in TEXT_COLUMNS})
            npcs.append(NpcRecord(identifier, fields))
    connection.close()
    return npcs
