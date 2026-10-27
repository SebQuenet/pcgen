"""Tests of the name normalisation in scripts/generate_bestiary.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import generate_bestiary  # noqa: E402


class FeatNames(unittest.TestCase):

    def test_drops_the_mythic_marker(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Power AttackM'), ['Power Attack'])

    def test_drops_the_bonus_marker_before_the_choice(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Improved CriticalB (rapier)'), ['Improved Critical (rapier)'])

    def test_drops_a_spaced_bonus_marker(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Improved Critical B (bite)'), ['Improved Critical (bite)'])

    def test_drops_the_marker_after_the_choice(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Weapon Focus (bite)M'), ['Weapon Focus (bite)'])

    def test_drops_a_bracketed_marker(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Improved Critical[M] (slam)'), ['Improved Critical (slam)'])

    def test_drops_a_choice_of_every_specialty(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Skill Focus (Perform [all] and Craft [all])'), [])

    def test_a_lone_marker_is_no_feat(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('M'), [])

    def test_joins_a_split_hyphen(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Point- Blank Shot'), ['Point-Blank Shot'])

    def test_splits_a_list_of_choices(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Improved Critical (bite, claw)'),
                         ['Improved Critical (bite)', 'Improved Critical (claw)'])

    def test_splits_two_feats_run_together(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Weapon Focus (bite) Weapon Focus (claw)'),
                         ['Weapon Focus (bite)', 'Weapon Focus (claw)'])

    def test_closes_an_unclosed_choice(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Weapon Focus (bite'), ['Weapon Focus (bite)'])

    def test_names_the_spell_of_a_quickened_spell_like_ability(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Quicken Spell-Like Ability (blur)'),
                         ['Quicken Spell-Like Ability ~ Spell (blur)'])

    def test_reads_quickened_as_quicken(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Quickened Spell-Like Ability (blur, mirror image)'),
                         ['Quicken Spell-Like Ability ~ Spell (blur)', 'Quicken Spell-Like Ability ~ Spell (mirror image)'])

    def test_names_the_spell_of_an_empowered_spell_like_ability(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Empower Spell-Like Ability (cone of cold)'),
                         ['Empower Spell-Like Ability ~ Spell (cone of cold)'])

    def test_names_a_greater_spell_by_its_key(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Quicken Spell-Like Ability (greater dispel magic)'),
                         ['Quicken Spell-Like Ability ~ Spell (dispel magic (Greater))'])

    def test_names_a_mass_spell_by_its_key(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Quicken Spell-Like Ability (mass inflict moderate wounds)'),
                         ['Quicken Spell-Like Ability ~ Spell (inflict moderate wounds (Mass))'])

    def test_leaves_a_named_spell_like_ability_alone(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Quicken Spell-Like Ability ~ Spell (feeblemind)'),
                         ['Quicken Spell-Like Ability ~ Spell (feeblemind)'])

    def test_uses_the_proficiency_key_of_a_weapon(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Weapon Focus (heavy mace)'), ['Weapon Focus (Mace (Heavy))'])

    def test_reads_a_composite_bow_as_its_bow(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Improved Critical (composite longbow)'), ['Improved Critical (Longbow)'])

    def test_names_a_bracketed_skill_by_its_key(self):
        self.assertEqual(generate_bestiary.normalize_feat_names('Skill Focus (Knowledge [religion])'), ['Skill Focus (Knowledge (Religion))'])


KNOWN_LANGUAGES = {name.lower(): name for name in ('Abyssal', 'Celestial', 'Common', 'Draconic', 'Infernal', 'Ancient Osiriani')}


def languages(*entries):
    return generate_bestiary.normalize_languages(list(entries), KNOWN_LANGUAGES)


class Languages(unittest.TestCase):

    def test_drops_telepathy(self):
        self.assertEqual(languages('Abyssal', 'Common; telepathy 100 ft.'), ['Abyssal', 'Common'])

    def test_drops_other_ways_of_speaking(self):
        self.assertEqual(languages('Celestial', 'Infernal;  truespeech', 'speak with animals', 'tongues'), ['Celestial', 'Infernal'])

    def test_keeps_a_language_the_creature_cannot_speak(self):
        self.assertEqual(languages("Common (can't speak)"), ['Common'])

    def test_keeps_a_language_the_creature_only_understands(self):
        self.assertEqual(languages('Understands Abyssal'), ['Abyssal'])

    def test_drops_words_that_name_no_language(self):
        self.assertEqual(languages('none', 'envisaging', '+2 additional', 'plus one more'), [])

    def test_drops_a_choice_left_to_the_reader(self):
        self.assertEqual(languages('Common', '(Celestial or Infernal 50% of either)'), ['Common'])

    def test_splits_alternatives(self):
        self.assertEqual(languages('Common and either Abyssal', 'Celestial', 'or Infernal'), ['Common', 'Abyssal', 'Celestial', 'Infernal'])

    def test_keeps_a_language_named_after_plus(self):
        self.assertEqual(languages('plus Common and Draconic'), ['Common', 'Draconic'])

    def test_drops_text_scraped_after_the_last_language(self):
        self.assertEqual(languages('Infernal  Special Abilities'), ['Infernal'])

    def test_names_a_language_by_its_key(self):
        self.assertEqual(languages('abyssal', 'ancient osiriani'), ['Abyssal', 'Ancient Osiriani'])

    def test_deduplicates(self):
        self.assertEqual(languages('Abyssal', 'Abyssal'), ['Abyssal'])

    def test_drops_a_language_no_data_defines(self):
        self.assertEqual(languages('Daemonic'), [])


class KitSkills(unittest.TestCase):

    def test_drops_an_unnamed_specialty(self):
        self.assertEqual(generate_bestiary.normalize_kit_skills('Craft (any one)'), [])

    def test_drops_every_specialty(self):
        self.assertEqual(generate_bestiary.normalize_kit_skills('Perform (all)'), [])

    def test_names_an_instrument_family(self):
        self.assertEqual(generate_bestiary.normalize_kit_skills('Perform (wind)'), ['Perform (Wind Instruments)'])

    def test_splits_joined_specialties(self):
        self.assertEqual(generate_bestiary.normalize_kit_skills('Knowledge (Arcana And Dungeoneering)'),
                         ['Knowledge (Arcana)', 'Knowledge (Dungeoneering)'])

    def test_leaves_a_plain_skill_alone(self):
        self.assertEqual(generate_bestiary.normalize_kit_skills('Stealth'), ['Stealth'])


def innate_spells(*spells):
    return generate_bestiary.build_sla_tags({'slas': {'cl': 5, 'entries': {'3/day': [{'spell': spell} for spell in spells]}}})


class SpellLikeAbilities(unittest.TestCase):

    def test_keeps_an_apostrophe_and_a_roman_numeral(self):
        self.assertEqual(innate_spells("summon nature's ally ix"), ["SPELLS:Innate|TIMES=3|CASTERLEVEL=5|Summon Nature's Ally IX"])

    def test_drops_a_metamagic_prefix(self):
        self.assertEqual(innate_spells('quickened confusion'), ['SPELLS:Innate|TIMES=3|CASTERLEVEL=5|Confusion'])

    def test_puts_the_modifier_after_the_spell(self):
        self.assertEqual(innate_spells('greater dispel magic'), ['SPELLS:Innate|TIMES=3|CASTERLEVEL=5|Dispel Magic (Greater)'])

    def test_capitalises_after_a_slash(self):
        self.assertEqual(innate_spells('geas/quest'), ['SPELLS:Innate|TIMES=3|CASTERLEVEL=5|Geas/Quest'])

    def test_leaves_a_small_word_in_lower_case(self):
        self.assertEqual(innate_spells('protection from evil'), ['SPELLS:Innate|TIMES=3|CASTERLEVEL=5|Protection from Evil'])


def damage_reduction_tags(text):
    creature = {'name': 'Brute', 'damageReduction': text, 'abilityScores': {}}
    return [tag for tag in generate_bestiary.generate_race_line(creature, {}, set()).split('\t') if tag.startswith('DR:')]


class DamageReduction(unittest.TestCase):

    def test_writes_one_tag_per_reduction(self):
        self.assertEqual(damage_reduction_tags('10/adamantine, 10/magic'), ['DR:10/adamantine', 'DR:10/magic'])

    def test_drops_a_note_in_parentheses(self):
        self.assertEqual(damage_reduction_tags('10/adamantine (110 points)'), ['DR:10/adamantine'])


def parsed_spells(text):
    entries = generate_bestiary.parse_pcfinder_sla_string(text)['slas']['entries']
    return [(spell['spell'], spell.get('dc')) for spells in entries.values() for spell in spells]


class SpellLikeAbilityText(unittest.TestCase):

    def test_a_semicolon_in_parentheses_does_not_split(self):
        self.assertEqual(parsed_spells('CL 20th\nAt Will-wind walk (DC 36; self only), dream'), [('wind walk', 36), ('dream', None)])

    def test_drops_a_lead_in_before_a_colon(self):
        self.assertEqual(parsed_spells('CL 12th\n1/week-any one of the following: symbol of fear (DC 23)'), [('symbol of fear', 23)])

    def test_keeps_a_colon_in_parentheses(self):
        self.assertEqual(parsed_spells('CL 14th\n1/day-summon (level 7, 1d3 daemons [Setting: Book, Vol. 3 56] 80%)'), [('summon', None)])


if __name__ == '__main__':
    unittest.main()
