"""Tests of the bestiary merge and the pawn framing in scripts/generate_bestiary_pawns.py."""

import sys
import unittest
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import generate_bestiary_pawns  # noqa: E402
from generate_bestiary_pawns import Creature  # noqa: E402

STONE_GIANT_PCGEN = Creature(name='Giant (Stone)', size='Large', creature_type='Humanoid (Giant)', source='Bestiary')
STONE_GIANT_PCFINDER = Creature(name='Stone Giant', size='Large', creature_type='humanoid (giant)',
                                source='PFRPG Bestiary', description='Grey-skinned and hairless.')
MOLE_PCFINDER = Creature(name='Mole', size='Tiny', creature_type='animal', source='d20pfsrd')


class Merge(unittest.TestCase):

    def test_draws_a_creature_both_bestiaries_hold_once(self):
        pawns = generate_bestiary_pawns.merge_bestiaries([STONE_GIANT_PCGEN], [STONE_GIANT_PCFINDER])
        self.assertEqual(list(pawns), ['giant_stone'])

    def test_keeps_the_pcfinder_details_of_a_shared_creature(self):
        pawns = generate_bestiary_pawns.merge_bestiaries([STONE_GIANT_PCGEN], [STONE_GIANT_PCFINDER])
        self.assertEqual(pawns['giant_stone'].description, 'Grey-skinned and hairless.')

    def test_knows_a_race_by_its_qualifier_alone(self):
        akhana = Creature(name='Akhana', size='Medium', creature_type='outsider', source='PFRPG Bestiary 2')
        aeon = Creature(name='Aeon (Akhana)', size='Medium', creature_type='Outsider', source='Bestiary 2')
        self.assertEqual(list(generate_bestiary_pawns.merge_bestiaries([aeon], [akhana])), ['aeon_akhana'])

    def test_keeps_a_creature_only_pcfinder_holds(self):
        pawns = generate_bestiary_pawns.merge_bestiaries([STONE_GIANT_PCGEN], [MOLE_PCFINDER])
        self.assertEqual(sorted(pawns), ['giant_stone', 'mole'])


class Framing(unittest.TestCase):

    def test_pads_a_landscape_drawing_to_a_portrait_pawn(self):
        framed = generate_bestiary_pawns.frame_as_pawn(Image.new('RGB', (1448, 1086), 'red'))
        self.assertEqual(framed.size, (1448, 2172))

    def test_pads_with_white(self):
        framed = generate_bestiary_pawns.frame_as_pawn(Image.new('RGB', (1448, 1086), 'red'))
        self.assertEqual(framed.getpixel((0, 0)), (255, 255, 255))

    def test_keeps_a_drawing_already_in_proportion(self):
        framed = generate_bestiary_pawns.frame_as_pawn(Image.new('RGB', (1024, 1536), 'red'))
        self.assertEqual(framed.size, (1024, 1536))

    def test_pads_a_drawing_too_tall_on_its_sides(self):
        framed = generate_bestiary_pawns.frame_as_pawn(Image.new('RGB', (600, 1500), 'red'))
        self.assertEqual(framed.size, (1000, 1500))


if __name__ == '__main__':
    unittest.main()
