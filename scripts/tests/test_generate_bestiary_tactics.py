"""Tests of the plan check and the stat block in scripts/generate_bestiary_tactics.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import generate_bestiary_tactics  # noqa: E402

WELL_FORMED_PLAN = """## Avant le combat

note: Profil
  Un rôdeur des marais.

## Ouverture

step: Ennemi au contact
  do: Morsure

## Rounds suivants

resource: Souffle | 1 | DD 14

## Repli

step: Points de vie sous 8
  do: Plonger
"""


class PlanCheck(unittest.TestCase):

    def test_accepts_the_four_sections_in_order(self):
        self.assertTrue(generate_bestiary_tactics.is_well_formed(WELL_FORMED_PLAN))

    def test_rejects_a_missing_section(self):
        self.assertFalse(generate_bestiary_tactics.is_well_formed(WELL_FORMED_PLAN.replace("## Repli", "")))

    def test_rejects_a_line_the_format_does_not_know(self):
        self.assertFalse(generate_bestiary_tactics.is_well_formed(WELL_FORMED_PLAN + "**Tactique** : fuir\n"))

    def test_rejects_two_resources_with_one_name(self):
        plan = WELL_FORMED_PLAN.replace("resource: Souffle | 1 | DD 14", "resource: Souffle | 1 | DD 14\nresource: Souffle | 2 | DD 16")
        self.assertFalse(generate_bestiary_tactics.is_well_formed(plan))

    def test_rejects_markdown_fences(self):
        self.assertFalse(generate_bestiary_tactics.is_well_formed("```\n" + WELL_FORMED_PLAN + "```\n"))


class StatBlock(unittest.TestCase):

    def test_keeps_the_filled_fields_but_not_the_bookkeeping_ones(self):
        fields = {"id": "12", "Name": "Abhominal", "Melee": "2 claws +9 (1d6+4)", "Aura": " ", "XP": "1600"}
        self.assertEqual(generate_bestiary_tactics.stat_block(fields), "Name: Abhominal\nMelee: 2 claws +9 (1d6+4)")


class PcfinderFields(unittest.TestCase):

    def test_writes_a_nested_value_as_json_and_leaves_out_the_parsed_attacks(self):
        creature = {"name": "Ghaggurath", "cr": "18", "speed": [{"moveType": "walk", "value": 20}],
                    "parsedAttacks": {"melee": {}}}
        self.assertEqual(generate_bestiary_tactics.pcfinder_fields(creature),
                         {"name": "Ghaggurath", "cr": "18", "speed": '[{"moveType": "walk", "value": 20}]'})


class BestiaryStatBlock(unittest.TestCase):

    def test_takes_combat_managers_stat_block_first(self):
        found = generate_bestiary_tactics.stat_block_of("Div (Aghash)", "Aghash", {"aghash": {"Name": "Aghash"}},
                                                        {"aghash": {"name": "Aghash", "cr": "5"}})
        self.assertEqual(found, {"Name": "Aghash"})

    def test_falls_back_on_pcfinders_by_the_race_key(self):
        found = generate_bestiary_tactics.stat_block_of("Ghaggurath", "Ghaggurath", {},
                                                        {"ghaggurath": {"name": "Ghaggurath", "cr": "18"}})
        self.assertEqual(found, {"name": "Ghaggurath", "cr": "18"})

    def test_has_none_when_neither_knows_the_monster(self):
        self.assertIsNone(generate_bestiary_tactics.stat_block_of("Unknown", "Unknown", {}, {}))


class Slug(unittest.TestCase):

    def test_matches_the_java_slug_of_a_qualified_key(self):
        self.assertEqual(generate_bestiary_tactics.slug_of("Carak (Blade of Zyphus)"), "carak_blade_of_zyphus")


if __name__ == "__main__":
    unittest.main()
