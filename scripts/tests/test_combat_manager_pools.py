"""Tests of the pool choices in scripts/combat_manager/pools.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))

from pools import preferred_options, worth_filling  # noqa: E402

PREFERENCES = {'hints': ['sniper', 'Fire'], 'highest_stat': 'INT', 'favored_class': 'Rogue'}


class Pools(unittest.TestCase):

    def test_an_archetype_named_by_the_stat_block_comes_first(self):
        options = ['Rogue Archetype ~ Acrobat', 'Rogue Archetype ~ Sniper']
        self.assertEqual(preferred_options('Rogue Archetype', options, PREFERENCES)[0], 'Rogue Archetype ~ Sniper')

    def test_an_archetype_the_stat_block_does_not_name_is_left_open(self):
        self.assertEqual(preferred_options('Fighter Archetype', ['Fighter Archetype ~ Archer'], PREFERENCES), [])

    def test_the_racial_bonus_goes_to_the_highest_score(self):
        options = ['+2 Charisma', '+2 Intelligence']
        self.assertEqual(preferred_options('Ability Bonus', options, PREFERENCES)[0], '+2 Intelligence')

    def test_the_favored_class_is_the_primary_class(self):
        self.assertEqual(preferred_options('Favored Class', ['Fighter', 'Rogue'], PREFERENCES)[0], 'Rogue')

    def test_the_standard_class_unless_the_stat_block_says_unchained(self):
        options = ['Rogue ~ Unchained Class', 'Rogue ~ Standard Class']
        self.assertEqual(preferred_options('Rogue Class Selection', options, PREFERENCES)[0], 'Rogue ~ Standard Class')

    def test_a_region_is_left_alone(self):
        self.assertFalse(worth_filling('Region of Origin'))


if __name__ == '__main__':
    unittest.main()
