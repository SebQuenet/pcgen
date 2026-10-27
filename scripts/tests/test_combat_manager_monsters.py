"""Tests of the conversion of Combat Manager's monsters in scripts/combat_manager/monsters.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'combat_manager'))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import catalog  # noqa: E402
import generate_bestiary  # noqa: E402
import monsters  # noqa: E402

GLAISTIG = {
    'Name': 'Glaistig', 'CR': '21', 'Alignment': 'CN', 'Size': 'Medium', 'Type': 'fey', 'SubType': '(earth, mythic)',
    'Senses': 'low-light vision, tremorsense 120 ft.; Perception +37', 'AC': '40, touch 32, flat-footed 28',
    'HD': '(25d6+335)', 'DR': '15/cold iron and epic', 'Immune': 'daze, mind-affecting effects, stagger, stun',
    'SR': '32;', 'Speed': '60 ft., burrow 60 ft., climb 60 ft.', 'Reach': '5 ft. (10 ft. with earth whip or leaf whip)',
    'Melee': 'earth whip +26/+21/+16 (10d6+20/19-20 bludgeoning, piercing, or slashing plus hex)',
    'AbilitiyScores': 'Str 28, Dex 35, Con 30, Int 27, Wis 28, Cha 31', 'Languages': 'Common, Elven, Sylvan, Terran',
    'Source': 'PFRPG Bestiary 5',
    'Feats': 'Combat Casting, Combat Reflexes[M], Skill Focus (Bluff)',
    'Skills': 'Acrobatics +40, Knowledge (geography) +21',
    'SpellLikeAbilities': 'Spell-Like Abilities (CL 25th; concentration +35)  Constant-freedom of movement, '
                          'pass without trace  At Will-create water  3/day-flesh to stone (DC 26), move earth',
}
DRAGON = {
    'Name': 'Wyvern', 'Size': 'Large', 'Type': 'dragon', 'HD': '(7d12+14)', 'AC': '19, touch 9, flat-footed 19',
    'Speed': '20 ft., fly 60 ft. (poor)', 'AbilitiyScores': 'Str 19, Dex 12, Con 15, Int 6, Wis 12, Cha 9',
    'Melee': 'sting +10 (1d6+4 plus poison), bite +10 (2d6+4 plus grab), 2 wings +5 (1d6+2)',
}
SKELETON = {'Name': 'Bone Heap', 'Type': 'undead', 'HD': '(4d8)', 'AbilitiyScores': 'Str 15, Dex 14, Con -, Int -, Wis 10, Cha 10'}


def natural_attacks(fields):
    creature = monsters.to_creature(fields)
    return [(attack['name'], attack['quantity'], attack['damage']['dice']['count'], attack['damage']['dice']['sides'],
             [effect['effectType'] for effect in attack.get('specialEffects', [])])
            for group in creature['parsedAttacks']['melee'].values() for attack in group['attacks']]


class ToCreature(unittest.TestCase):

    def test_reads_the_ability_scores(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['abilityScores'],
                         {'str': 28, 'dex': 35, 'con': 30, 'int': 27, 'wis': 28, 'cha': 31})

    def test_a_dash_score_is_absent(self):
        self.assertIsNone(monsters.to_creature(SKELETON)['abilityScores']['int'])

    def test_counts_the_hit_dice(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['numericHitDice'], 25)

    def test_reads_every_speed(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['speed'],
                         [{'moveType': 'walk', 'value': 60}, {'moveType': 'burrow', 'value': 60}, {'moveType': 'climb', 'value': 60}])

    def test_keeps_the_fly_maneuverability(self):
        self.assertIn({'moveType': 'fly', 'value': 60, 'maneuverability': 'poor'}, monsters.to_creature(DRAGON)['speed'])

    def test_reads_the_armor_class(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['armorClass'], {'normal': 40, 'touch': 32, 'flatFooted': 28})

    def test_drops_a_word_around_the_type(self):
        self.assertEqual(monsters.to_creature({'Type': 'advanced magical beast'})['type'], 'magical beast')

    def test_an_elemental_is_an_outsider(self):
        self.assertEqual(monsters.to_creature({'Type': 'elemental'})['type'], 'outsider')

    def test_splits_the_subtypes(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['subTypes'], ['earth', 'mythic'])

    def test_reads_the_spell_resistance_without_its_punctuation(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['spellResistance'], 32)

    def test_splits_the_immunities(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['immunities'], ['daze', 'mind-affecting effects', 'stagger', 'stun'])

    def test_drops_the_mythic_mark_from_feats(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['feats'], ['Combat Casting', 'Combat Reflexes', 'Skill Focus (Bluff)'])

    def test_keeps_the_base_of_a_mythic_feat(self):
        self.assertEqual(monsters.to_creature({'Feats': 'Mythic Power Attack, Dodge[M], Bull Rush Strike*'})['feats'],
                         ['Power Attack', 'Dodge', 'Bull Rush Strike'])

    def test_lists_the_mythic_feats(self):
        self.assertEqual(monsters.to_creature({'Feats': 'Mythic Power Attack, Dodge[M], Bull Rush Strike*'})['mythicFeats'],
                         ['Power Attack', 'Dodge'])

    def test_drops_an_ability_focus(self):
        self.assertEqual(monsters.to_creature({'Feats': 'Ability Focus (poison), Dodge'})['feats'], ['Dodge'])

    def test_cleans_the_spell_like_abilities(self):
        self.assertEqual(monsters.to_creature({'SpellLikeAbilities': 'At Will-fireball*, clairaudience/ clairvoyance'})['spellLikeAbilities'],
                         'At Will-fireball, clairaudience/clairvoyance')

    def test_reads_the_natural_attacks(self):
        self.assertEqual(natural_attacks(DRAGON), [('sting', 1, 1, 6, ['poison']), ('bite', 1, 2, 6, ['grab']), ('wings', 2, 1, 6, [])])

    def test_keeps_the_page_less_source(self):
        self.assertEqual(monsters.to_creature(GLAISTIG)['source'], 'PFRPG Bestiary 5')


class GeneratedLines(unittest.TestCase):

    def test_the_race_line_carries_the_damage_reduction(self):
        line = generate_bestiary.generate_race_line(monsters.to_creature(GLAISTIG), {}, set())
        self.assertIn('DR:15/cold iron and epic', line.split('\t'))

    def test_the_spell_like_abilities_reach_the_racial_traits(self):
        creature = monsters.to_creature(GLAISTIG)
        line = generate_bestiary.generate_racial_traits_line(creature, generate_bestiary.get_effective_d20_data(creature, {}))
        self.assertIn('SPELLS:Innate|TIMES=3|CASTERLEVEL=25|Flesh to Stone,26|Move Earth', line.split('\t'))

    def test_the_kit_grants_a_mythic_feat(self):
        creature = monsters.to_creature(GLAISTIG)
        kit = generate_bestiary.generate_kit_block(creature, {}, generate_bestiary.get_effective_d20_data(creature, {}))
        self.assertIn('ABILITY:CATEGORY=Mythic Feat|Combat Reflexes', kit.split('\n'))

    def test_the_kit_takes_the_skills(self):
        creature = monsters.to_creature(GLAISTIG)
        kit = generate_bestiary.generate_kit_block(creature, {}, generate_bestiary.get_effective_d20_data(creature, {}))
        self.assertIn('SKILL:Knowledge (Geography)', kit)


RACES = catalog.KeyIndex(['Ghoul (Ghast)', 'Amoeba (Giant)', 'Pitri (Solar)', 'Dragon (Red)', 'Elemental (Ice/Large)',
                          'Cockroach (Giant)', 'Entothrope (Weremantis (Hybrid Form))'])
TEMPLATES = catalog.KeyIndex(['Exoskeleton', 'Advanced'])


class IsKnown(unittest.TestCase):

    def known(self, name):
        return monsters.is_known(name, RACES, TEMPLATES)

    def test_finds_a_family_member(self):
        self.assertTrue(self.known('Ghast'))

    def test_finds_an_inverted_comma_name(self):
        self.assertTrue(self.known('Amoeba, Giant'))

    def test_finds_inverted_words(self):
        self.assertTrue(self.known('Solar Pitri'))

    def test_finds_a_dragon_by_its_age(self):
        self.assertTrue(self.known('Great Wyrm Red Dragon'))

    def test_finds_an_elemental_by_its_size(self):
        self.assertTrue(self.known('Large Ice Elemental'))

    def test_finds_a_race_with_a_template(self):
        self.assertTrue(self.known('Giant Cockroach Exoskeleton'))

    def test_finds_a_hand_written_equivalent(self):
        self.assertTrue(self.known('Weremantis'))

    def test_a_new_monster_is_unknown(self):
        self.assertFalse(self.known('Glaistig'))


class BookPlacement(unittest.TestCase):

    BOOKS = {'bestiary5': Path('paizo/bestiary_5/_bestiary_5.pcc'), 'pathfinderadventurepath117': Path('pcfinder/ap117/ap117.pcc')}

    def test_a_bestiary_goes_to_its_book(self):
        self.assertEqual(monsters.book_pcc('PFRPG Bestiary 5', self.BOOKS), Path('paizo/bestiary_5/_bestiary_5.pcc'))

    def test_an_adventure_path_goes_to_its_book(self):
        self.assertEqual(monsters.book_pcc('AP 117', self.BOOKS), Path('pcfinder/ap117/ap117.pcc'))

    def test_a_book_pcgen_lacks_has_no_directory(self):
        self.assertIsNone(monsters.book_pcc('PFS S2-01', self.BOOKS))


CATALOG = monsters.Catalog(
    feats=catalog.KeyIndex(['Power Attack', 'Weapon Focus', 'Skill Focus', 'Dodge', 'Scarring Spell-Like Ability']),
    choosing_feats=frozenset({'Weapon Focus', 'Skill Focus'}),
    mythic_feats=catalog.KeyIndex(['Power Attack', 'Weapon Focus']),
    weapon_profs=catalog.KeyIndex(['Chain (Spiked)', 'Sting']),
    skills=catalog.KeyIndex(['Stealth', 'Knowledge (Geography)']),
    spells=catalog.KeyIndex(['Fireball', "Summon Nature's Ally IX"]),
)


def pruned(feats=(), mythic_feats=(), skills='', spell_like_abilities=''):
    creature = {'name': 'Brute', 'feats': list(feats), 'mythicFeats': list(mythic_feats), 'skills': skills,
                'spellLikeAbilities': spell_like_abilities, 'numericHitDice': 5}
    return monsters.prune(creature, generate_bestiary.get_effective_d20_data(creature, {}) or {}, CATALOG)


class Prune(unittest.TestCase):

    def test_spells_a_weapon_as_pcgen_does(self):
        creature, abilities, dropped = pruned(feats=['Weapon Focus (spiked chain)'])
        self.assertEqual(abilities['feats'], ['Weapon Focus (Chain (Spiked))'])

    def test_finds_a_plural_natural_weapon(self):
        creature, abilities, dropped = pruned(feats=['Weapon Focus (stings)'])
        self.assertEqual(abilities['feats'], ['Weapon Focus (Sting)'])

    def test_drops_a_weapon_pcgen_lacks(self):
        creature, abilities, dropped = pruned(feats=['Weapon Focus (petal spike)', 'Dodge'])
        self.assertEqual(abilities['feats'], ['Dodge'])

    def test_drops_a_feat_pcgen_lacks(self):
        creature, abilities, dropped = pruned(feats=['Vermin Heart', 'Dodge'])
        self.assertEqual(dropped, ['feat Vermin Heart'])

    def test_drops_a_choice_the_feat_does_not_offer(self):
        creature, abilities, dropped = pruned(feats=['Scarring Spell-Like Ability (crushing despair)'])
        self.assertEqual(abilities['feats'], [])

    def test_drops_a_skill_focus_on_a_missing_skill(self):
        creature, abilities, dropped = pruned(feats=['Skill Focus (Perform)'])
        self.assertEqual(abilities['feats'], [])

    def test_drops_a_mythic_feat_pcgen_lacks(self):
        creature, abilities, dropped = pruned(mythic_feats=['Power Attack', 'Blind-Fight'])
        self.assertEqual(creature['mythicFeats'], ['Power Attack'])

    def test_resolves_the_choice_of_a_mythic_feat(self):
        creature, abilities, dropped = pruned(mythic_feats=['Weapon Focus (stings)'])
        self.assertEqual(creature['mythicFeats'], ['Weapon Focus (Sting)'])

    def test_drops_a_skill_pcgen_lacks(self):
        creature, abilities, dropped = pruned(skills='Stealth +10, Craft (drawing) +8, Knowledge (geography) +7')
        self.assertEqual(sorted(abilities['skills']), ['Knowledge (geography)', 'Stealth'])

    def test_drops_a_spell_pcgen_lacks(self):
        creature, abilities, dropped = pruned(spell_like_abilities="CL 9th\n3/day-fireball (DC 15), call magma, summon nature's ally ix")
        self.assertEqual([spell['spell'] for spell in abilities['slas']['entries']['3/day']], ['fireball', "summon nature's ally ix"])


if __name__ == '__main__':
    unittest.main()
