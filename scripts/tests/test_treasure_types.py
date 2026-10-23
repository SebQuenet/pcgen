"""Tests of scripts/treasure_types.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import treasure_types  # noqa: E402


class TreasureByName(unittest.TestCase):

    def test_keeps_the_treasure_of_each_named_creature(self):
        creatures = [{'name': 'Vrock', 'treasure': 'standard'}, {'name': 'Wolf', 'treasure': 'none'}]

        self.assertEqual(treasure_types.treasure_by_name(creatures), {'vrock': 'standard', 'wolf': 'none'})

    def test_leaves_out_a_creature_without_treasure(self):
        creatures = [{'name': 'Vrock'}, {'name': 'Wolf', 'treasure': '  '}]

        self.assertEqual(treasure_types.treasure_by_name(creatures), {})

    def test_the_first_entry_of_a_name_wins(self):
        creatures = [{'name': 'Goblin', 'treasure': 'NPC gear'}, {'name': 'goblin', 'treasure': 'standard'}]

        self.assertEqual(treasure_types.treasure_by_name(creatures), {'goblin': 'NPC gear'})


if __name__ == '__main__':
    unittest.main()
