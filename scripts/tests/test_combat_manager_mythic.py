"""Tests of how scripts/combat_manager/mythic.py reads a mythic NPC's stat block."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import mythic  # noqa: E402
from records import NpcRecord  # noqa: E402
from statblock import ClassLevels  # noqa: E402

MYTHIC_FEATS = {'Dodge', 'Power Attack', 'Weapon Focus', 'Mythic Spell Lore', 'Extra Mythic Power'}


class Paths(unittest.TestCase):

    def test_a_mythic_npc_names_its_path_as_a_class(self):
        entries = [ClassLevels('alchemist', 11), ClassLevels('trickster', 4)]
        self.assertEqual(mythic.path_entry(NpcRecord(1, {'Mythic': '1'}), entries), entries[1])

    def test_an_archetype_named_like_a_path_is_no_path(self):
        entries = [ClassLevels('druid', 12)]
        self.assertIsNone(mythic.path_entry(NpcRecord(1, {'Mythic': '0'}), entries))


class MythicFeats(unittest.TestCase):

    def test_a_marked_feat_is_requested_in_its_mythic_version(self):
        self.assertEqual(mythic.requested_feats('Power Attack[M], Iron Will', [], MYTHIC_FEATS), [('Power Attack', None)])

    def test_a_prefixed_feat_implies_the_ordinary_feat(self):
        self.assertEqual(mythic.implied_feats('Mythic Dodge, Iron Will', MYTHIC_FEATS), [('Dodge', None)])

    def test_a_mythic_only_feat_left_unplaced_is_requested(self):
        self.assertEqual(mythic.requested_feats('Extra Mythic Power', [('Extra Mythic Power', None)], MYTHIC_FEATS),
                         [('Extra Mythic Power', None)])


if __name__ == '__main__':
    unittest.main()
