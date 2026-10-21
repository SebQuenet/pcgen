"""Tests of the stat block parsing in scripts/combat_manager/statblock.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import statblock  # noqa: E402
from statblock import ClassLevels, GearItem, SpellBlock  # noqa: E402


class SplitList(unittest.TestCase):

    def test_keeps_commas_inside_parentheses(self):
        self.assertEqual(statblock.split_list('belt of physical might +2 (Str, Con), cloak'),
                         ['belt of physical might +2 (Str, Con)', 'cloak'])

    def test_keeps_a_thousands_separator(self):
        self.assertEqual(statblock.split_list('holy symbol, 2,010 gp'), ['holy symbol', '2,010 gp'])

    def test_an_absent_field_is_an_empty_list(self):
        self.assertEqual(statblock.split_list(None), [])


class Classes(unittest.TestCase):

    def test_reads_a_single_class(self):
        self.assertEqual(statblock.parse_classes('wizard 1'), [ClassLevels('wizard', 1)])

    def test_reads_several_classes(self):
        self.assertEqual(statblock.parse_classes('fighter 4/rogue 3'),
                         [ClassLevels('fighter', 4), ClassLevels('rogue', 3)])

    def test_reads_the_archetype_in_parentheses(self):
        self.assertEqual(statblock.parse_classes('rogue (sniper) 3'),
                         [ClassLevels('rogue', 3, archetypes=('sniper',))])

    def test_reads_the_deity_after_of(self):
        self.assertEqual(statblock.parse_classes('inquisitor (heretic) of Abadar 10'),
                         [ClassLevels('inquisitor', 10, archetypes=('heretic',), deity='Abadar')])

    def test_reads_the_unchained_prefix(self):
        self.assertEqual(statblock.parse_classes('unchained summoner 4'),
                         [ClassLevels('summoner', 4, is_unchained=True)])

    def test_an_ex_paladin_is_a_paladin(self):
        self.assertEqual(statblock.parse_classes('ex-paladin of Iomedae 8'),
                         [ClassLevels('paladin', 8, deity='Iomedae')])

    def test_keeps_of_inside_a_class_name(self):
        self.assertEqual(statblock.parse_classes('student of war 3', class_names={'student of war'}),
                         [ClassLevels('student of war', 3)])


class AbilityScores(unittest.TestCase):

    def test_reads_the_six_scores(self):
        self.assertEqual(statblock.parse_ability_scores('Str 10, Dex 13, Con 14, Int 17, Wis 12, Cha 8'),
                         {'STR': 10, 'DEX': 13, 'CON': 14, 'INT': 17, 'WIS': 12, 'CHA': 8})

    def test_a_dash_is_no_score(self):
        self.assertEqual(statblock.parse_ability_scores('Str 10, Dex 13, Con —, Int 17, Wis 12, Cha 8')['CON'], None)


class SignedNumbers(unittest.TestCase):

    def test_reads_an_unsigned_number(self):
        self.assertEqual(statblock.parse_signed('15, touch 11, flat-footed 14'), 15)

    def test_reads_a_negative_number(self):
        self.assertEqual(statblock.parse_signed('-1'), -1)


class Feats(unittest.TestCase):

    def test_separates_the_choice(self):
        self.assertEqual(statblock.parse_feats('Weapon Focus (longsword), Toughness'),
                         [('Weapon Focus', 'Longsword'), ('Toughness', None)])

    def test_uses_the_pcgen_weapon_key(self):
        self.assertEqual(statblock.parse_feats('Weapon Focus (short sword)'), [('Weapon Focus', 'Sword (Short)')])

    def test_drops_the_bonus_marker(self):
        self.assertEqual(statblock.parse_feats('Improved CriticalB (rapier)'), [('Improved Critical', 'Rapier')])


class Skills(unittest.TestCase):

    def test_reads_the_total(self):
        self.assertEqual(statblock.parse_skills('Perception +17 (+18 to notice stonework), Swim +0'),
                         {'Perception': 17, 'Swim': 0})

    def test_capitalises_the_specialty(self):
        self.assertEqual(statblock.parse_skills('Knowledge (arcana) +7'), {'Knowledge (Arcana)': 7})

    def test_reads_a_negative_total(self):
        self.assertEqual(statblock.parse_skills('Stealth –2'), {'Stealth': -2})


class Gear(unittest.TestCase):

    def test_reads_the_quantity(self):
        self.assertEqual(statblock.parse_gear('potions of cure light wounds (2)').items,
                         [GearItem('potions of cure light wounds', quantity=2)])

    def test_reads_the_enhancement_and_special_abilities(self):
        self.assertEqual(statblock.parse_gear('+1 axiomatic longsword').items,
                         [GearItem('longsword', enhancement=1, specials=('axiomatic',))])

    def test_reads_the_material(self):
        self.assertEqual(statblock.parse_gear('+4 mithral chainmail').items,
                         [GearItem('chainmail', enhancement=4, material='mithral')])

    def test_reads_masterwork(self):
        self.assertEqual(statblock.parse_gear('masterwork heavy pick').items,
                         [GearItem('heavy pick', is_masterwork=True)])

    def test_splits_the_ammunition_off(self):
        self.assertEqual(statblock.parse_gear('light crossbow with 10 masterwork bolts').items,
                         [GearItem('light crossbow'), GearItem('bolts', quantity=10, is_masterwork=True)])

    def test_drops_the_book_marker(self):
        self.assertEqual(statblock.parse_gear('katana*, alchemical cartridges (paper)UC').items,
                         [GearItem('katana'), GearItem('alchemical cartridges (paper)')])

    def test_reads_a_leading_quantity(self):
        self.assertEqual(statblock.parse_gear('20 bolts').items, [GearItem('bolts', quantity=20)])

    def test_reads_a_quantity_in_brackets(self):
        self.assertEqual(statblock.parse_gear('potions of cure light wounds [2]').items,
                         [GearItem('potions of cure light wounds', quantity=2)])

    def test_reads_the_masterwork_abbreviation(self):
        self.assertEqual(statblock.parse_gear('mwk dagger').items, [GearItem('dagger', is_masterwork=True)])

    def test_drops_the_charges(self):
        self.assertEqual(statblock.parse_gear('brooch of shielding (10 charges)').items,
                         [GearItem('brooch of shielding')])

    def test_drops_the_strength_rating(self):
        self.assertEqual(statblock.parse_gear('composite shortbow (+3 Str)').items,
                         [GearItem('composite shortbow')])

    def test_counts_the_gold(self):
        self.assertEqual(statblock.parse_gear('holy symbol, 2,010 gp, 5 pp').gold, 2060)

    def test_keeps_the_plus_of_a_wondrous_item(self):
        self.assertEqual(statblock.parse_gear('cloak of resistance +2').items,
                         [GearItem('cloak of resistance +2')])


class Spells(unittest.TestCase):

    BLOCK = ('Wizard Spells Prepared (CL 1st; concentration +4)   1st-burning hands (DC 14), mage armor   '
             '0 (at will)-bleed (DC 13), detect magic, resistance')

    def test_reads_the_class_and_kind(self):
        block = statblock.parse_spells(self.BLOCK)[0]
        self.assertEqual((block.class_name, block.is_prepared), ('Wizard', True))

    def test_reads_the_spells_by_level(self):
        self.assertEqual(statblock.parse_spells(self.BLOCK)[0].spells,
                         {1: {'Burning Hands': 1, 'Mage Armor': 1}, 0: {'Bleed': 1, 'Detect Magic': 1, 'Resistance': 1}})

    def test_reads_a_count_and_reorders_greater(self):
        block = statblock.parse_spells('Paladin Spells Prepared (CL 16th)   3rd-greater magic weapon, eagle\'s splendor (2)')[0]
        self.assertEqual(block.spells, {3: {'Magic Weapon (Greater)': 1, "Eagle's Splendor": 2}})

    def test_reads_the_per_day_of_a_spontaneous_caster(self):
        block = statblock.parse_spells('Bard Spells Known (CL 17th)   6th (2/day)-find the path, summon monster VI')[0]
        self.assertEqual(block, SpellBlock('Bard', False, {6: {'Find the Path': 1, 'Summon Monster VI': 1}}))

    def test_drops_the_domain_marker(self):
        block = statblock.parse_spells('Cleric Spells Prepared (CL 5th)   3rd-fireballD, prayer   D Domain spell; Domains Fire, Sun')[0]
        self.assertEqual(block.spells, {3: {'Fireball': 1, 'Prayer': 1}})

    def test_drops_a_metamagic_prefix(self):
        block = statblock.parse_spells('Druid Spells Prepared (CL 19th)   9th-empowered fire storm, elemental body IVD')[0]
        self.assertEqual(block.spells, {9: {'Fire Storm': 1, 'Elemental Body IV': 1}})


if __name__ == '__main__':
    unittest.main()
