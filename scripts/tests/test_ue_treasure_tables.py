"""Tests of scripts/ue_treasure_tables.py."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import ue_treasure_tables as tables  # noqa: E402


class Rewards(unittest.TestCase):

    def test_reads_rolled_coins(self):
        self.assertEqual(tables.parse_reward('2d4 × 10 sp'),
                         [{'kind': 'coins', 'count': 1, 'dice': 2, 'sides': 4, 'multiplier': 10, 'coin': 'sp'}])

    def test_reads_coins_without_multiplier(self):
        self.assertEqual(tables.parse_reward('8d6 pp')[0]['multiplier'], 1)

    def test_reads_counted_gemstones(self):
        self.assertEqual(tables.parse_reward('two grade 3 gemstones'), [{'kind': 'pool', 'count': 2, 'pool': 'gem/3'}])

    def test_reads_a_graded_magic_item(self):
        self.assertEqual(tables.parse_reward('Three lesser major potions'),
                         [{'kind': 'pool', 'count': 3, 'pool': 'potion/lesser major'}])

    def test_reads_a_wondrous_item(self):
        self.assertEqual(tables.parse_reward('greater minor wondrous items'),
                         [{'kind': 'pool', 'count': 1, 'pool': 'wondrous/greater minor'}])

    def test_reads_masterwork_gear(self):
        self.assertEqual(tables.parse_reward('masterwork light armor or shield'),
                         [{'kind': 'pool', 'count': 1, 'pool': 'masterwork/light armor or shield'}])

    def test_keeps_thousands_whole(self):
        self.assertEqual(tables.parse_reward('2d4 × 1,000 gp, 12d8 × 10 pp')[0]['multiplier'], 1000)

    def test_reads_a_whole_reward(self):
        self.assertEqual(len(tables.parse_reward('2d4 × 10 gp, 2d4 pp, lesser medium potion, two greater medium scrolls')), 4)

    def test_refuses_what_it_cannot_read(self):
        with self.assertRaises(ValueError):
            tables.parse_reward('a bag of holding, probably')


class Prices(unittest.TestCase):

    def test_reads_gold_and_silver(self):
        self.assertEqual(tables.parse_price('12 gp, 5 sp'), 12)

    def test_reads_thousands(self):
        self.assertEqual(tables.parse_price('2,500 gp'), 2500)

    def test_has_no_price_for_text(self):
        self.assertIsNone(tables.parse_price('varies'))


class Rows(unittest.TestCase):

    HTML = ('<table><caption>Rings</caption><thead><tr><th>d%</th><th>Lesser Minor Ring</th><th>Price</th></tr></thead>'
            '<tr><td>01&ndash;07</td><td><i>Dungeon ring, prisoner&#39;s</td><td></i>250 gp</td></tr>'
            '<tr><td>08&ndash;100</td><td><i>Ring of arcane signets</td><td></i>1,000 gp</td></tr></table>')

    def test_reads_each_table_under_its_header(self):
        found = tables.read_tables(self.HTML)

        self.assertEqual(found[('Rings', 'Lesser Minor Ring')],
                         [(7, ["Dungeon ring, prisoner's", '250 gp']), (93, ['Ring of arcane signets', '1,000 gp'])])


class Weights(unittest.TestCase):

    def test_a_pool_keeps_only_priced_rows(self):
        rows = [(50, ['Mithral shirt', '1,100 gp']), (50, ['Masterwork or special material armor', '—'])]

        self.assertEqual(tables.priced_pool(rows, ''), [{'name': 'Mithral shirt', 'price': 1100, 'weight': 50}])


if __name__ == '__main__':
    unittest.main()
