"""Tests of scripts/combat_manager/repair_monster_attacks.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import repair_monster_attacks  # noqa: E402

WEAPONS = {'Spear': repair_monster_attacks.TWO_HANDED, 'Longsword': repair_monster_attacks.ONE_HANDED,
           'Longbow (Composite)': repair_monster_attacks.TWO_HANDED}
RACE_LINE = 'Aeshma Demon\tKEY:Aeshma Demon\tSIZE:L\tHANDS:2\tMONSTERCLASS:Outsider (Ref/Will):14\tCR:11'


class WeaponKey(unittest.TestCase):

    def test_drops_the_enhancement_and_the_special_ability(self):
        self.assertEqual('Spear', repair_monster_attacks.weapon_key('+1 wounding spear', WEAPONS))

    def test_reads_a_qualified_key_written_the_other_way_round(self):
        self.assertEqual('Longbow (Composite)', repair_monster_attacks.weapon_key('mwk composite longbow', WEAPONS))

    def test_has_none_for_a_natural_attack(self):
        self.assertIsNone(repair_monster_attacks.weapon_key('claws', WEAPONS))


class NaturalAttacks(unittest.TestCase):

    def test_writes_the_claws_of_the_melee_line_but_not_its_weapon(self):
        tag = repair_monster_attacks.natural_attacks_tag('+1 wounding spear +21/+16/+11 (2d6+10/19-20/x3), 2 claws +19 (1d6+6)')
        self.assertEqual('NATURALATTACKS:Claw,Weapon.Natural.Weapon Group Natural.Melee.Finesseable.Slashing,*2,1d6', tag)

    def test_goes_into_the_race_line_before_its_monster_class(self):
        line = repair_monster_attacks.with_natural_attacks(RACE_LINE, 'NATURALATTACKS:Claw,X,*2,1d6')
        self.assertEqual('Aeshma Demon\tKEY:Aeshma Demon\tSIZE:L\tHANDS:2\tNATURALATTACKS:Claw,X,*2,1d6'
                         '\tMONSTERCLASS:Outsider (Ref/Will):14\tCR:11', line)


    def test_reads_a_natural_attack_named_with_adjectives(self):
        tag = repair_monster_attacks.natural_attacks_tag('barbed tentacles +11 (3d6+6 plus bleed)')
        self.assertTrue(tag.startswith('NATURALATTACKS:Tentacle,'), tag)

    def test_reads_tendrils(self):
        tag = repair_monster_attacks.natural_attacks_tag('2 tendrils +9 (1d4+3 plus grab)')
        self.assertTrue(tag.startswith('NATURALATTACKS:Tendril,'), tag)

    def test_reads_a_named_touch_as_a_touch(self):
        tag = repair_monster_attacks.natural_attacks_tag('corrupting touch +8 touch (13d6)')
        self.assertTrue(tag.startswith('NATURALATTACKS:Touch,'), tag)

    def test_reads_incorporeal_touches(self):
        tag = repair_monster_attacks.natural_attacks_tag('2 incorporeal touches +9 touch (2d6 negative energy)')
        self.assertTrue(tag.startswith('NATURALATTACKS:Incorporeal Touch,'), tag)


class GearLines(unittest.TestCase):

    def test_wields_the_first_weapon_and_carries_the_next_at_the_creatures_size(self):
        lines = repair_monster_attacks.gear_lines('+1 wounding spear +21 (2d6+10)', 'longsword +15 (1d8)', WEAPONS, 'L')
        self.assertEqual(['GEAR:Spear\tQTY:1\tSIZE:L\tLOCATION:Both Hands', 'GEAR:Longsword\tQTY:1\tSIZE:L\tLOCATION:Carried'],
                         lines)

    def test_reads_a_weapon_named_without_its_bonus_or_damage(self):
        self.assertEqual(['GEAR:Spear\tQTY:1\tSIZE:S\tLOCATION:Both Hands'],
                         repair_monster_attacks.gear_lines('+1 spear', '', WEAPONS, 'S'))

    def test_names_no_size_for_a_medium_creature(self):
        self.assertEqual(['GEAR:Longsword\tQTY:1\tLOCATION:Primary Hand'],
                         repair_monster_attacks.gear_lines('longsword +15 (1d8)', '', WEAPONS, 'M'))


if __name__ == '__main__':
    unittest.main()
