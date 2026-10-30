"""Tests of scripts/combat_manager/repair_kit_feats.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import catalog  # noqa: E402
import monsters  # noqa: E402
import repair_kit_feats  # noqa: E402

CATALOG = monsters.Catalog(
    feats=catalog.KeyIndex(['Power Attack', 'Weapon Focus', 'Improved Critical', 'Skill Focus', 'Point-Blank Shot',
                            'Flyby Attack', 'Multiattack', 'Charge Through', 'Combat Expertise', 'Ability Focus',
                            'Quicken Spell-Like Ability ~ Spell', 'Armor Proficiency (Light)']),
    choosing_feats=frozenset({'Weapon Focus', 'Improved Critical', 'Skill Focus', 'Ability Focus',
                              'Quicken Spell-Like Ability ~ Spell'}),
    mythic_feats=catalog.KeyIndex([]),
    weapon_profs=catalog.KeyIndex(['Chain (Spiked)', 'Bite', 'Rope Dart']),
    skills=catalog.KeyIndex(['Stealth', 'Perception']),
    spells=catalog.KeyIndex([]),
)


class RepairedLine(unittest.TestCase):

    def test_names_a_weapon_by_its_pcgen_key(self):
        self.assertEqual('ABILITY:CATEGORY=FEAT|Weapon Focus (Chain (Spiked))',
                         repair_kit_feats.repaired('ABILITY:CATEGORY=FEAT|Weapon Focus (spiked chain)', CATALOG))

    def test_drops_a_weapon_pcgen_does_not_define(self):
        self.assertIsNone(repair_kit_feats.repaired('ABILITY:CATEGORY=FEAT|Improved Critical (tail sting)', CATALOG))

    def test_does_not_take_a_weapon_whose_name_only_starts_alike(self):
        self.assertIsNone(repair_kit_feats.repaired('ABILITY:CATEGORY=FEAT|Weapon Focus (rope)', CATALOG))

    def test_keeps_a_weapon_pcgen_defines_whatever_its_case(self):
        self.assertEqual('ABILITY:CATEGORY=FEAT|Weapon Focus (bite)',
                         repair_kit_feats.repaired('ABILITY:CATEGORY=FEAT|Weapon Focus (bite)', CATALOG))

    def test_drops_a_skill_pcgen_does_not_define(self):
        self.assertIsNone(repair_kit_feats.repaired('ABILITY:CATEGORY=FEAT|Skill Focus (any craft)', CATALOG))

    def test_leaves_a_feat_without_choice_alone(self):
        self.assertEqual('ABILITY:CATEGORY=FEAT|Power Attack',
                         repair_kit_feats.repaired('ABILITY:CATEGORY=FEAT|Power Attack', CATALOG))

    def test_keeps_the_other_fields_of_the_line(self):
        self.assertEqual('ABILITY:CATEGORY=FEAT|Weapon Focus (Chain (Spiked))\tFREE:YES',
                         repair_kit_feats.repaired('ABILITY:CATEGORY=FEAT|Weapon Focus (spiked chain)\tFREE:YES', CATALOG))


ABILITY_FOCUSES = frozenset({'Paralysis', 'Poison'})


def repaired_kit(*feats):
    return repair_kit_feats.repaired_lines([f'ABILITY:CATEGORY=FEAT|{feat}' for feat in feats], CATALOG, ABILITY_FOCUSES)


class FeatName(unittest.TestCase):

    def test_reads_a_feat_spelt_with_other_spaces_and_hyphens(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Point-Blank Shot', 'ABILITY:CATEGORY=FEAT|Flyby Attack'],
                         repaired_kit('Point Blank Shot', 'Fly-by Attack'))

    def test_drops_the_mark_of_the_book_a_stat_block_glued_to_the_feat(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Charge Through', 'ABILITY:CATEGORY=FEAT|Power Attack'],
                         repaired_kit('Charge ThroughAPG', 'Power Attack*'))

    def test_corrects_a_misspelt_or_renamed_feat(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Multiattack', 'ABILITY:CATEGORY=FEAT|Combat Expertise'],
                         repaired_kit('Mulitattack', 'Expertise'))

    def test_gives_a_choice_cut_off_at_its_comma_to_the_feat_before_it(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Skill Focus (Perception)', 'ABILITY:CATEGORY=FEAT|Skill Focus (Stealth)'],
                         repaired_kit('Skill Focus (Perception)', 'Stealth)'))

    def test_names_a_spell_like_ability_feat_as_pcgen_keys_it(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Quicken Spell-Like Ability ~ Spell (scorching ray)'],
                         repaired_kit('Quicken Spell-like Ability (scorching ray)'))

    def test_drops_the_choice_a_stat_block_gave_to_a_feat_that_takes_none(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Power Attack'], repaired_kit('Power Attack (tail slap only)'))

    def test_drops_a_feat_no_book_defines(self):
        self.assertEqual([], repaired_kit('Death\'s Blessing'))

    def test_keeps_an_ability_focus_on_an_ability_its_category_holds(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Ability Focus (Poison)'], repaired_kit('Ability Focus (poison)'))

    def test_drops_an_ability_focus_on_an_ability_its_category_lacks(self):
        self.assertEqual([], repaired_kit('Ability Focus (lightning bolt)'))

    def test_keeps_a_feat_whose_key_holds_parentheses(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Armor Proficiency (Light)\tFREE:YES'],
                         repaired_kit('Armor Proficiency (Light)\tFREE:YES'))

    def test_leaves_a_choice_written_without_a_space_alone(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Weapon Focus(Flail)'], repaired_kit('Weapon Focus(Flail)'))

    def test_leaves_several_feats_named_on_one_line_alone(self):
        self.assertEqual(['ABILITY:CATEGORY=FEAT|Power Attack|Multiattack'], repaired_kit('Power Attack|Multiattack'))


if __name__ == '__main__':
    unittest.main()
