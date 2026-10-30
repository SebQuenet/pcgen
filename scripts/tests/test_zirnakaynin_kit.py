"""Tests of the .pcg to kit translation in scripts/zirnakaynin_kit.py."""

import dataclasses
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import zirnakaynin_kit  # noqa: E402

PCG = """CAMPAIGN:Bestiary|CAMPAIGN:Core Rulebook
CHARACTERNAME:Ravael Torai
AGE:183
GENDER:Female
STAT:STR|SCORE:18
STAT:DEX|SCORE:14
STAT:CON|SCORE:14
STAT:INT|SCORE:12
STAT:WIS|SCORE:14
STAT:CHA|SCORE:11
ALIGN:LE
RACE:Drow
CLASS:Wizard|SUBCLASS:Conjurer|LEVEL:3|SKILLPOOL:0
CLASS:Fighter|LEVEL:2|SKILLPOOL:0|SPELLBASE:None|CANCASTPERDAY:
TEMPLATESAPPLIED:[NAME:Humanoid]
TEMPLATESAPPLIED:[NAME:Vampire]
SKILL:Perception|OUTPUTORDER:51|CLASSBOUGHT:[CLASS:Fighter|RANKS:2.0|COST:1|CLASSSKILL:Y]|CLASSBOUGHT:[CLASS:Wizard|RANKS:3.0|COST:1|CLASSSKILL:N]
SKILL:Appraise|OUTPUTORDER:40|
LANGUAGE:Elven|LANGUAGE:Undercommon
ABILITY:FEAT|TYPE:NORMAL|CATEGORY:FEAT|KEY:Alertness|TYPE:General|DESC:You often notice things.
ABILITY:FEAT|TYPE:NORMAL|CATEGORY:FEAT|KEY:Weapon Focus|APPLIEDTO:Crossbow (Heavy)|TYPE:Combat|DESC:x
ABILITY:Favored Class Bonus|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Favored Class Bonus ~ Hit Point|APPLIEDTO:|TYPE:FavoredClassBonus
ABILITY:Favored Class Bonus|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Favored Class Bonus ~ Skill Rank|APPLIEDTO:Acrobatics,Appraise|TYPE:FavoredClassBonus
EQUIPNAME:Crossbow, Heavy|OUTPUTORDER:3|COST:50.0|WT:8.0|QUANTITY:1.0|NOTE:|CUSTOMIZATION:[BASEITEM:Heavy Crossbow (Base)|DATA:NAME=Crossbow, Heavy$KEY=Crossbow (Heavy)$EQMOD=Material ~ Wood]
EQUIPNAME:Bolt (Drow/Poison)|OUTPUTORDER:7|COST:100.0|WT:0.1|QUANTITY:60.0|NOTE:|CUSTOMIZATION:[BASEITEM:Bolt (Drow/Poison)|DATA:NAME=Bolt (Drow/Poison)$EQMOD=Material ~ Steel]
EQUIPNAME:Ring of Protection +3|OUTPUTORDER:2|COST:18000|WT:0.0|QUANTITY:1.0|NOTE:
EQUIPSET:Tenue par défaut|ID:0.1|USETEMPMODS:Y
EQUIPSET:Both Hands|ID:0.1.01|VALUE:Crossbow, Heavy|QUANTITY:1.0|USETEMPMODS:Y
EQUIPSET:Ring|ID:0.1.03|VALUE:Ring of Protection +3|QUANTITY:1.0|USETEMPMODS:Y
DEITY:Haagenti|DEITYDOMAINS:[DOMAIN:Artifice|DOMAIN:Chaos]
DOMAIN:Evil|DOMAINGRANTS:x|SOURCE:[TYPE:PCClass|NAME:Cleric|LEVEL:11]
DOMAIN:Strength|DOMAINGRANTS:x|SOURCE:[TYPE:PCClass|NAME:Cleric|LEVEL:11]
SPELLNAME:Alarm|TIMES:1|CLASS:Wizard|BOOK:Prepared Spells|SPELLLEVEL:1|SOURCE:[TYPE:CLASS|NAME:Wizard]
SPELLNAME:Blur|TIMES:2|CLASS:Wizard|BOOK:Prepared Spells|SPELLLEVEL:2|SOURCE:[TYPE:CLASS|NAME:Wizard]
SPELLNAME:Blur|TIMES:1|CLASS:Wizard|BOOK:Known Spells|SPELLLEVEL:2|SOURCE:[TYPE:CLASS|NAME:Wizard]
SPELLNAME:Darkness|TIMES:1|CLASS:Drow|BOOK:Innate|SPELLLEVEL:2|SOURCE:[TYPE:RACE|NAME:Drow]
"""

EVERYTHING_KNOWN = zirnakaynin_kit.Catalog(lambda kind, key: True, frozenset({'Vampire'}), {'Drider': ('Drider', 9)}, frozenset({'Assassin'}), {'Assassin': frozenset({'Alertness'}), 'Arcane Archer': frozenset({'Weapon Focus'})},
                                             {'Combat Trick': frozenset({'Combat Trick Feat'}), 'Mythic Level 03': frozenset({'Mythic Path', 'Mythic Ability Bonus'}),
                                              "Nature's Bond ~ Druid Domain": frozenset({'Druid Domain'}), 'Extra Discovery': frozenset({'Alchemist Discovery'})})


def kit_of(pcg=PCG, catalog=EVERYTHING_KNOWN):
    lines, _ = zirnakaynin_kit.kit_lines(zirnakaynin_kit.parse_pcg(pcg), catalog)
    return lines


class Identity(unittest.TestCase):

    def test_opens_a_kit_named_after_the_character(self):
        self.assertEqual(kit_of()[0], 'STARTPACK:Ravael Torai\tTYPE:NPC.Zirnakaynin\tVISIBLE:QUALIFY\tEQUIPBUY:0')

    def test_keeps_race_alignment_gender_and_age(self):
        self.assertTrue({'RACE:Drow', 'ALIGN:LE', 'GENDER:Female', 'AGE:183'} <= set(kit_of()))

    def test_keeps_the_base_scores(self):
        self.assertIn('STAT:STR=18|DEX=14|CON=14|INT=12|WIS=14|CHA=11', kit_of())

    def test_keeps_the_deity_and_the_chosen_domains(self):
        self.assertIn('DEITY:Haagenti\tDOMAIN:Evil|Strength', kit_of())


class Classes(unittest.TestCase):

    def test_keeps_each_class_with_its_level_and_school(self):
        classes = [line for line in kit_of() if line.startswith('CLASS:')]
        self.assertEqual(classes, ['CLASS:Wizard\tLEVEL:3\tSUBCLASS:Conjurer', 'CLASS:Fighter\tLEVEL:2'])

    def test_leaves_out_the_hit_dice_the_race_grants(self):
        pcg = PCG.replace('RACE:Drow', 'RACE:Drider').replace('CLASS:Wizard|SUBCLASS:Conjurer|LEVEL:3', 'CLASS:Drider|LEVEL:9')
        classes = [line for line in kit_of(pcg) if line.startswith('CLASS:')]
        self.assertEqual(classes, ['CLASS:Fighter\tLEVEL:2'])

    def test_keeps_the_hit_dice_beyond_those_the_race_grants(self):
        pcg = PCG.replace('RACE:Drow', 'RACE:Drider').replace('CLASS:Wizard|SUBCLASS:Conjurer|LEVEL:3', 'CLASS:Drider|LEVEL:11')
        self.assertIn('CLASS:Drider\tLEVEL:2', kit_of(pcg))


class PrestigeClasses(unittest.TestCase):

    PCG = PCG.replace('CLASS:Fighter|LEVEL:2|', 'CLASS:Assassin|LEVEL:2|').replace(
        'CLASSBOUGHT:[CLASS:Fighter|RANKS:2.0', 'CLASSBOUGHT:[CLASS:Assassin|RANKS:2.0')

    def test_takes_a_prestige_class_once_the_skills_and_feats_it_requires_are_in(self):
        lines = kit_of(self.PCG)
        self.assertLess(max(lines.index('ABILITY:CATEGORY=FEAT|Alertness'), lines.index('SKILL:Perception\tRANK:3\tCLASS:Wizard')),
                        lines.index('CLASS:Assassin\tLEVEL:2'))

    def test_takes_the_other_feats_after_the_prestige_class_whose_levels_may_have_granted_them(self):
        lines = kit_of(self.PCG)
        self.assertGreater(lines.index('ABILITY:CATEGORY=FEAT|Weapon Focus (Crossbow (Heavy))'), lines.index('CLASS:Assassin\tLEVEL:2'))

    def test_takes_a_feat_opening_a_pool_the_character_uses_before_the_prestige_class(self):
        pcg = self.PCG + ("ABILITY:FEAT|TYPE:NORMAL|CATEGORY:FEAT|KEY:Extra Discovery|APPLIEDTO:\n"
                          "ABILITY:Alchemist Discovery|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Discovery ~ Infuse Mutagen\n")
        lines = kit_of(pcg)
        self.assertLess(lines.index('ABILITY:CATEGORY=FEAT|Extra Discovery'), lines.index('CLASS:Assassin\tLEVEL:2'))

    def test_buys_the_ranks_of_a_prestige_class_after_taking_it(self):
        skills = [line for line in kit_of(self.PCG) if line.startswith('SKILL:Perception')]
        self.assertEqual(skills, ['SKILL:Perception\tRANK:3\tCLASS:Wizard', 'SKILL:Perception\tRANK:2\tCLASS:Assassin'])


class Abilities(unittest.TestCase):

    def test_keeps_a_chosen_feat_in_its_pool(self):
        self.assertIn('ABILITY:CATEGORY=FEAT|Alertness', kit_of())

    def test_keeps_the_choice_of_a_feat(self):
        self.assertIn('ABILITY:CATEGORY=FEAT|Weapon Focus (Crossbow (Heavy))', kit_of())

    def test_takes_an_ability_once_per_choice(self):
        skill_ranks = [line for line in kit_of() if 'Skill Rank' in line]
        self.assertEqual(skill_ranks, ['ABILITY:CATEGORY=Favored Class Bonus|Favored Class Bonus ~ Skill Rank (Acrobatics)',
                                       'ABILITY:CATEGORY=Favored Class Bonus|Favored Class Bonus ~ Skill Rank (Appraise)'])

    def test_takes_the_chosen_ability_itself_when_the_choice_is_an_ability(self):
        pcg = PCG + ("ABILITY:Bloodline|TYPE:NORMAL|CATEGORY:Internal|KEY:Sorcerer ~ Standard Bloodline Selection"
                     "|APPLIEDTO:CATEGORY=Sorcerer Bloodline&pipe;Aberrant Bloodline|TYPE:Bloodline Selection\n")
        bloodlines = [line for line in kit_of(pcg) if 'Bloodline' in line]
        self.assertEqual(bloodlines, ['ABILITY:CATEGORY=Sorcerer Bloodline|Aberrant Bloodline\tFREE:YES'])

    def test_takes_a_feat_of_a_granted_pool_after_the_ability_granting_the_pool(self):
        pcg = PCG + ("ABILITY:Combat Trick Feat|TYPE:NORMAL|CATEGORY:FEAT|KEY:Agile Maneuvers|TYPE:Combat\n"
                     "ABILITY:Rogue Talent|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Combat Trick|TYPE:RogueTalent\n")
        abilities = [line for line in kit_of(pcg) if 'Combat Trick' in line or 'Agile' in line]
        self.assertEqual(abilities, ['ABILITY:CATEGORY=Rogue Talent|Combat Trick', 'ABILITY:CATEGORY=Combat Trick Feat|Agile Maneuvers'])

    def test_takes_an_ability_once_per_empty_choice(self):
        pcg = PCG + ("ABILITY:Inquisitor Teamwork Feat|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Bonus Teamwork Feat"
                     "|APPLIEDTO:,,|TYPE:Inquisitor Teamwork Feat\n")
        bonuses = [line for line in kit_of(pcg) if 'Bonus Teamwork Feat' in line]
        self.assertEqual(bonuses, ['ABILITY:CATEGORY=Inquisitor Teamwork Feat|Bonus Teamwork Feat'] * 3)

    def test_takes_an_ability_after_the_ability_opening_its_pool(self):
        pcg = PCG + ("ABILITY:Druid Domain|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Druid Domain ~ Plant\n"
                     "ABILITY:Nature Bond|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Nature's Bond ~ Druid Domain\n")
        lines = kit_of(pcg)
        self.assertGreater(lines.index('ABILITY:CATEGORY=Druid Domain|Druid Domain ~ Plant'),
                           lines.index("ABILITY:CATEGORY=Nature Bond|Nature's Bond ~ Druid Domain"))

    def test_takes_the_mythic_choices_after_the_mythic_tier_opening_their_pools(self):
        pcg = PCG + ("ABILITY:Mythic Ability Bonus|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Mythic Ability Bonus ~ +2 Wisdom|APPLIEDTO:\n"
                     "ABILITY:Mythic Path|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Mythic Path ~ Hierophant\n"
                     "ABILITY:Mythic Tier|TYPE:NORMAL|CATEGORY:Special Ability|KEY:Mythic Level 03\n")
        mythic = [line for line in kit_of(pcg) if 'Mythic' in line]
        self.assertEqual(mythic[0], 'ABILITY:CATEGORY=Mythic Tier|Mythic Level 03')

    def test_keeps_an_ability_without_choice(self):
        self.assertIn('ABILITY:CATEGORY=Favored Class Bonus|Favored Class Bonus ~ Hit Point', kit_of())


class Skills(unittest.TestCase):

    def test_buys_the_ranks_with_the_points_of_the_class_that_paid_for_them(self):
        skills = [line for line in kit_of() if line.startswith('SKILL:Perception')]
        self.assertEqual(skills, ['SKILL:Perception\tRANK:2\tCLASS:Fighter', 'SKILL:Perception\tRANK:3\tCLASS:Wizard'])

    def test_leaves_out_a_skill_without_ranks(self):
        self.assertFalse(any(line.startswith('SKILL:Appraise') for line in kit_of()))


class Gear(unittest.TestCase):

    def test_names_a_customized_item_by_its_key_and_keeps_its_location(self):
        self.assertIn('GEAR:Crossbow (Heavy)\tQTY:1\tLOCATION:Both Hands\tEQMOD:Material ~ Wood', kit_of())

    def test_equips_an_item_worn_on_the_body(self):
        self.assertIn('GEAR:Ring of Protection +3\tQTY:1\tLOCATION:Equipped', kit_of())

    def test_straps_a_shield_to_the_shield_slot(self):
        pcg = PCG + ("EQUIPNAME:Shield, Heavy Steel|OUTPUTORDER:6|COST:20.0|WT:15.0|QUANTITY:1.0|NOTE:|CUSTOMIZATION:[BASEITEM:Shield (Heavy/Steel)"
                     "|DATA:NAME=Shield, Heavy Steel$KEY=Shield (Heavy/Steel)]\n"
                     "EQUIPSET:Shield|ID:0.1.07|VALUE:Shield, Heavy Steel|QUANTITY:1.0|USETEMPMODS:Y\n")
        self.assertIn('GEAR:Shield (Heavy/Steel)\tQTY:1\tLOCATION:Shield', kit_of(pcg))

    def test_carries_an_item_outside_the_equipment_set(self):
        self.assertIn('GEAR:Bolt (Drow/Poison)\tQTY:60\tLOCATION:Carried\tEQMOD:Material ~ Steel', kit_of())


class CustomGear(unittest.TestCase):

    def test_builds_an_item_named_in_the_campaign_from_its_base_item(self):
        pcg = PCG + ("EQUIPNAME:Char|OUTPUTORDER:2|COST:72320.0|WT:12.0|QUANTITY:1.0|NOTE:|CUSTOMIZATION:[BASEITEM:Greataxe (Base)"
                     "|DATA:NAME=Char$EQMOD=Material ~ Steel.Special Ability ~ +4 ~ Weapon$SPROP=(+4 Greataxe)]\n")
        catalog = zirnakaynin_kit.Catalog(lambda kind, key: key != 'Char', frozenset(), {}, frozenset(), {}, {})
        self.assertIn('GEAR:Greataxe (Base)\tQTY:1\tLOCATION:Carried\tEQMOD:Material ~ Steel.Special Ability ~ +4 ~ Weapon',
                      kit_of(pcg, catalog))

    def test_writes_the_parameters_of_an_equipment_modifier_unescaped(self):
        pcg = PCG + ("EQUIPNAME:Healer's Kit|OUTPUTORDER:5|COST:50.0|WT:1.0|QUANTITY:3.0|NOTE:|CUSTOMIZATION:[BASEITEM:Healer's Kit"
                     "|DATA:NAME=Healer's Kit$EQMOD=Special Ability ~ Charged Item / 10 Maximum&pipe;CHARGES&lbracket;10&rbracket;]\n")
        self.assertIn("GEAR:Healer's Kit\tQTY:3\tLOCATION:Carried\tEQMOD:Special Ability ~ Charged Item / 10 Maximum|CHARGES[10]",
                      kit_of(pcg))

    def test_gives_a_resized_item_its_size(self):
        pcg = PCG + "EQUIPNAME:AUTOSIZESStudded Leather|OUTPUTORDER:4|COST:25.0|WT:10.0|QUANTITY:1.0|NOTE:\n"
        self.assertIn('GEAR:Studded Leather\tQTY:1\tLOCATION:Carried\tSIZE:S', kit_of(pcg))


class Spells(unittest.TestCase):

    def test_groups_the_prepared_spells_of_a_class_with_their_count(self):
        self.assertIn('SPELLS:SPELLBOOK=Prepared Spells|CLASS=Wizard|Alarm|Blur=2', kit_of())

    def test_leaves_out_spells_granted_by_the_race(self):
        self.assertFalse(any('Darkness' in line for line in kit_of()))


class Languages(unittest.TestCase):

    def test_names_every_known_language_as_a_bonus_language(self):
        self.assertIn('LANGBONUS:Elven|Undercommon', kit_of())


class Templates(unittest.TestCase):

    def test_keeps_a_template_a_player_can_choose(self):
        self.assertIn('TEMPLATE:Vampire', kit_of())

    def test_leaves_out_a_hidden_template(self):
        self.assertNotIn('TEMPLATE:Humanoid', kit_of())


class UnknownReferences(unittest.TestCase):

    def test_drops_a_feat_no_book_defines_and_reports_it(self):
        catalog = zirnakaynin_kit.Catalog(lambda kind, key: key != 'Alertness', frozenset(), {}, frozenset(), {}, {})
        lines, dropped = zirnakaynin_kit.kit_lines(zirnakaynin_kit.parse_pcg(PCG), catalog)
        self.assertEqual((any('Alertness' in line for line in lines), dropped), (False, [('ABILITY', 'Alertness')]))

    def test_drops_a_choice_of_a_background_skill_and_reports_it(self):
        catalog = dataclasses.replace(EVERYTHING_KNOWN, background_skills=frozenset({'Appraise'}))
        lines, dropped = zirnakaynin_kit.kit_lines(zirnakaynin_kit.parse_pcg(PCG), catalog)
        self.assertEqual((any('Skill Rank (Appraise)' in line for line in lines), dropped), (False, [('SKILL', 'Appraise')]))


class Campaigns(unittest.TestCase):

    def test_reads_the_campaigns_the_character_loaded(self):
        self.assertEqual(zirnakaynin_kit.parse_pcg(PCG).campaigns, ('Bestiary', 'Core Rulebook'))


if __name__ == '__main__':
    unittest.main()
