"""Tests for generate_campaign_bestiary."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import generate_campaign_bestiary  # noqa: E402

AP25 = generate_campaign_bestiary.DATA_DIR / 'pathfinder/pcfinder/adventure_path/ap25/pathfinder_adventure_path_25.pcc'
PCC = """CAMPAIGN:Pathfinder Adventure Path #25
SOURCELONG:Pathfinder Adventure Path #25
PRECAMPAIGN:1,INCLUDESBOOKTYPE=Core Rules

ABILITY:ap25_abilities_race.lst
RACE:ap25_races.lst
KIT:ap25_kits_race.lst
"""


class FileLines(unittest.TestCase):

    def test_points_each_file_line_at_its_file_from_the_data_folder(self):
        self.assertEqual(generate_campaign_bestiary.file_lines(PCC, AP25), [
            'ABILITY:@/pathfinder/pcfinder/adventure_path/ap25/ap25_abilities_race.lst',
            'RACE:@/pathfinder/pcfinder/adventure_path/ap25/ap25_races.lst',
            'KIT:@/pathfinder/pcfinder/adventure_path/ap25/ap25_kits_race.lst',
        ])

    def test_keeps_a_template_line_and_its_include_filter(self):
        lines = generate_campaign_bestiary.file_lines('TEMPLATE:ap25_templates.lst|(INCLUDE:Ghost)\n', AP25)
        self.assertEqual(lines, ['TEMPLATE:@/pathfinder/pcfinder/adventure_path/ap25/ap25_templates.lst|(INCLUDE:Ghost)'])


if __name__ == '__main__':
    unittest.main()
