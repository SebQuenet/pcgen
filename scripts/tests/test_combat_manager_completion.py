"""Tests of the choices scripts/combat_manager/completion.py makes for what a stat block leaves out."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))

from completion import adjustment_order, bond_hint, feat_candidates, fillable, language_ranking, named_companion, skill_ranking, theme_of  # noqa: E402


class Themes(unittest.TestCase):

    def test_the_class_with_most_levels_sets_the_theme(self):
        self.assertEqual(theme_of(['Wizard', 'Fighter']), 'arcane')

    def test_an_unknown_class_is_skilled(self):
        self.assertEqual(theme_of(['Loremaster']), 'skilled')


class SpareFeats(unittest.TestCase):

    def test_a_fighter_first_focuses_on_the_weapon_it_attacks_with(self):
        self.assertEqual(feat_candidates('martial', 'Greatsword', ['Intimidate'])[0], ('Weapon Focus', 'Greatsword'))

    def test_a_wizard_first_focuses_on_its_best_skill(self):
        self.assertEqual(feat_candidates('arcane', 'Dagger', ['Knowledge (Arcana)', 'Spellcraft'])[0],
                         ('Skill Focus', 'Knowledge (Arcana)'))


class FillablePools(unittest.TestCase):

    def test_a_combat_style_is_completed(self):
        self.assertTrue(fillable('Ranger Combat Style Feat'))

    def test_an_archetype_is_never_chosen_for_the_npc(self):
        self.assertFalse(fillable('Fighter Archetype'))

    def test_a_mythic_path_ability_is_never_chosen_for_the_npc(self):
        self.assertFalse(fillable('Mythic Path Feature ~ Trickster Attack'))


class BonusLanguages(unittest.TestCase):

    def test_a_druid_learns_sylvan_first(self):
        self.assertEqual(language_ranking('divine', ['Druid'])[0], 'Sylvan')

    def test_a_wizard_learns_draconic_first(self):
        self.assertEqual(language_ranking('arcane', ['Wizard'])[0], 'Draconic')


class SpareSkillPoints(unittest.TestCase):

    def test_published_skills_are_left_alone(self):
        self.assertNotIn('Spellcraft', skill_ranking(['Wizard'], {'Spellcraft': 12}))

    def test_the_class_favoured_skills_come_first(self):
        self.assertEqual(skill_ranking(['Cleric'], {})[0], 'Knowledge (Religion)')



class Companions(unittest.TestCase):

    def test_a_named_familiar(self):
        self.assertEqual(named_companion('arcane bond (raven named Cain), cantrips'), ('Familiar', 'raven'))

    def test_an_animal_companion_in_brackets(self):
        self.assertEqual(named_companion('animal companion [hawk]'), ('Animal Companion', 'hawk'))

    def test_a_bonded_object_is_no_companion(self):
        self.assertIsNone(named_companion('arcane bond (amulet)'))

    def test_the_bond_follows_what_is_named(self):
        self.assertEqual((bond_hint('arcane bond (raven)'), bond_hint('arcane bond (ring)')), ('Familiar', 'Bonded Object'))


    def test_a_mount_named_in_brackets(self):
        self.assertEqual(named_companion('challenge +2, mount (Jinfu [horse]), resolve'), ('Mount', 'horse'))


class ClassLevelAdjustments(unittest.TestCase):

    def test_the_best_published_ability_comes_first(self):
        scores = {'STR': 18, 'DEX': 12, 'CON': 14, 'INT': 8, 'WIS': 10, 'CHA': None}
        self.assertEqual(adjustment_order(scores)[0], 'Strength')


if __name__ == '__main__':
    unittest.main()
