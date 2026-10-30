"""Tests for the pawn side of import_zirnakaynin_npcs."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import import_zirnakaynin_npcs  # noqa: E402

PCG = "CHARACTERNAME:Nyr\nPORTRAIT:\nTACTICALPLAN:## Combat\n"


class Portrait(unittest.TestCase):

    def test_points_the_portrait_at_the_pawn(self):
        pcg = import_zirnakaynin_npcs.with_portrait(PCG, 'data/homebrew/zirnakaynin/pnj/pions/nyr.jpg')
        self.assertIn('\nPORTRAIT:data/homebrew/zirnakaynin/pnj/pions/nyr.jpg\n', pcg)

    def test_replaces_a_portrait_already_set(self):
        pcg = import_zirnakaynin_npcs.with_portrait(PCG.replace('PORTRAIT:', 'PORTRAIT:/home/someone/nyr.png'), 'pions/nyr.jpg')
        self.assertEqual(pcg.count('PORTRAIT:'), 1)


class PawnName(unittest.TestCase):

    def test_names_the_pawn_as_the_game_table_looks_it_up_for_a_character_file(self):
        self.assertEqual(import_zirnakaynin_npcs.game_table_pawn_name('generic-garde-de-mine'), 'generic_garde_de_mine.jpg')


if __name__ == '__main__':
    unittest.main()
