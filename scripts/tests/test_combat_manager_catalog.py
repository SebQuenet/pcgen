"""Tests of the PCGen key matching in scripts/combat_manager/catalog.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import catalog  # noqa: E402
from statblock import ClassLevels  # noqa: E402

EQUIPMENT_KEYS = [
    'Shield (Heavy/Steel)', 'Bolt (Crossbow)', 'Bolt (Crossbow/Repeating)', 'Antitoxin (Vial)', 'Longbow (Composite)',
    'Longbow', 'Scroll of Invisibility', 'Scroll of Invisibility (Greater)', 'Scroll of Dispel Magic (Greater) (Arcane)',
    'Potion of Shield of Faith +2', 'Potion of Shield of Faith +3', 'Belt of Physical Might (STR/CON) +2',
    'Cloak of Resistance +2', 'Potion of Cure Light Wounds', 'Saddle (Military)',
]


class Equipment(unittest.TestCase):

    def setUp(self):
        self.index = catalog.KeyIndex(EQUIPMENT_KEYS)

    def test_finds_an_exact_name_whatever_its_case(self):
        self.assertEqual(self.index.find('potion of cure light wounds'), 'Potion of Cure Light Wounds')

    def test_reads_the_parentheses_as_leading_words(self):
        self.assertEqual(self.index.find('heavy steel shield'), 'Shield (Heavy/Steel)')

    def test_falls_back_to_the_plainest_variant(self):
        self.assertEqual(self.index.find('bolt'), 'Bolt (Crossbow)')

    def test_moves_greater_behind_the_spell(self):
        self.assertEqual(self.index.find('scroll of greater invisibility'), 'Scroll of Invisibility (Greater)')

    def test_takes_the_first_variant_of_a_prefix(self):
        self.assertEqual(self.index.find('scroll of greater dispel magic'), 'Scroll of Dispel Magic (Greater) (Arcane)')

    def test_takes_the_weakest_bonus_when_none_is_given(self):
        self.assertEqual(self.index.find('potion of shield of faith'), 'Potion of Shield of Faith +2')

    def test_moves_the_abilities_of_a_belt_before_its_bonus(self):
        self.assertEqual(self.index.find('belt of physical might +2 (Str, Con)'), 'Belt of Physical Might (STR/CON) +2')

    def test_reads_a_plural(self):
        self.assertEqual(self.index.find('potions of cure light wounds'), 'Potion of Cure Light Wounds')

    def test_reads_a_qualifier_with_a_length_as_a_leading_word(self):
        self.assertEqual(catalog.KeyIndex(['Rope (Silk/50 ft)']).find('silk rope (50 ft.)'), 'Rope (Silk/50 ft)')

    def test_drops_the_deity_of_a_holy_symbol(self):
        self.assertEqual(catalog.KeyIndex(['Holy Symbol']).find('holy symbol of Iomedae'), 'Holy Symbol')

    def test_drops_armor_after_a_material(self):
        self.assertEqual(catalog.KeyIndex(['Leather']).find('leather armor'), 'Leather')

    def test_reads_a_metamagic_rod(self):
        self.assertEqual(catalog.KeyIndex(['Rod (Extend/Lesser)']).find('lesser extend metamagic rod'), 'Rod (Extend/Lesser)')

    def test_reads_a_plural_axe(self):
        self.assertEqual(catalog.KeyIndex(['Axe (Throwing)']).find('throwing axes'), 'Axe (Throwing)')

    def test_reads_a_plural_in_is(self):
        self.assertEqual(catalog.KeyIndex(['Kukri']).find('kukris'), 'Kukri')

    def test_an_unknown_item_is_none(self):
        self.assertIsNone(self.index.find('heavy horse'))


class Classes(unittest.TestCase):

    def setUp(self):
        self.index = catalog.KeyIndex(['Wizard', 'Fighter', 'Hellknight Signifer'])

    def test_a_specialist_is_a_wizard_with_a_school(self):
        self.assertEqual(catalog.resolve_class(ClassLevels('enchanter', 2), self.index), ('Wizard', 'Enchanter'))

    def test_a_class_is_found_whatever_its_case(self):
        self.assertEqual(catalog.resolve_class(ClassLevels('hellknight signifer', 4), self.index), ('Hellknight Signifer', None))

    def test_an_unknown_class_is_none(self):
        self.assertEqual(catalog.resolve_class(ClassLevels('gray gardener', 3), self.index), (None, None))


class Races(unittest.TestCase):

    def setUp(self):
        self.index = catalog.KeyIndex(['Human', 'Half-Elf', 'Tiefling'])

    def test_drops_the_ethnicity(self):
        self.assertEqual(catalog.resolve_race('human (Ulfen)', self.index), ('Human', None))

    def test_reads_the_gender(self):
        self.assertEqual(catalog.resolve_race('female human', self.index), ('Human', 'Female'))

    def test_falls_back_to_the_last_word(self):
        self.assertEqual(catalog.resolve_race('demon-spawn tiefling', self.index), ('Tiefling', None))


class Modifiers(unittest.TestCase):

    KEYS = ['Special Ability ~ +1 ~ Weapon', 'Special Ability ~ Axiomatic ~ Weapon', 'Material ~ Mithril ~ Weapon / Item',
            'Material ~ Alchemical Silver', 'Special Quality ~ Masterwork ~ Weapon']

    def test_finds_a_special_ability(self):
        self.assertEqual(catalog.pick_modifier(self.KEYS, 'axiomatic'), 'Special Ability ~ Axiomatic ~ Weapon')

    def test_spells_mithral_as_pcgen_does(self):
        self.assertEqual(catalog.pick_modifier(self.KEYS, 'mithral'), 'Material ~ Mithril ~ Weapon / Item')

    def test_silver_is_alchemical_silver(self):
        self.assertEqual(catalog.pick_modifier(self.KEYS, 'silver'), 'Material ~ Alchemical Silver')

    def test_takes_silver_itself_when_offered(self):
        self.assertEqual(catalog.pick_modifier(self.KEYS + ['Material ~ Silver'], 'silver'), 'Material ~ Silver')

    def test_finds_an_enhancement(self):
        self.assertEqual(catalog.pick_modifier(self.KEYS, '+1'), 'Special Ability ~ +1 ~ Weapon')


if __name__ == '__main__':
    unittest.main()
