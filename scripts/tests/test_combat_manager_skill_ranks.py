"""Tests of the skill rank placement in scripts/combat_manager/skill_ranks.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))

from skill_ranks import plan_ranks, headband_skill, obsessive_bonus  # noqa: E402


def placement_after(budgets, placed, wanted):
    plan_ranks(budgets, placed, wanted)
    return placed


class SkillRanks(unittest.TestCase):

    def test_a_level_spends_no_more_than_its_points(self):
        self.assertEqual(placement_after([1, 1], {}, {'Stealth': 1, 'Climb': 1}), {'Stealth': [0], 'Climb': [1]})

    def test_a_skill_holds_no_more_ranks_than_levels(self):
        self.assertEqual(placement_after([2, 0, 0], {}, {'Stealth': 3}), {'Stealth': [0]})

    def test_the_skill_needing_most_ranks_comes_first(self):
        self.assertEqual(placement_after([1, 1], {}, {'Climb': 1, 'Stealth': 2}), {'Climb': [], 'Stealth': [0, 1]})

    def test_gives_back_the_highest_rank(self):
        self.assertEqual(plan_ranks([0, 0], {'Stealth': [0, 1]}, {'Stealth': 1}),
                         [{'skill_key': 'Stealth', 'points': -1, 'level_index': 1}])

    def test_a_point_given_back_serves_another_skill(self):
        self.assertEqual(placement_after([0], {'Sense Motive': [0]}, {'Sense Motive': 0, 'Spellcraft': 1}),
                         {'Sense Motive': [], 'Spellcraft': [0]})



class SkillChoices(unittest.TestCase):

    def test_the_headband_goes_to_the_highest_skill(self):
        self.assertEqual(headband_skill({'Perception': 9, 'Knowledge (Arcana)': 17}), 'Knowledge (Arcana)')

    def test_a_gnome_obsesses_over_its_best_craft(self):
        self.assertEqual(obsessive_bonus({'Craft (Pottery)': 8, 'Craft (Traps)': 3, 'Perception': 12}),
                         ('Obsessive Craft Bonus', 'Craft (Pottery)'))

    def test_no_craft_nor_profession_leaves_the_bonus_open(self):
        self.assertIsNone(obsessive_bonus({'Perception': 12}))


if __name__ == '__main__':
    unittest.main()
