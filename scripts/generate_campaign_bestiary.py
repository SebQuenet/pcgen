#!/usr/bin/env python3
"""Write one source holding the creatures of every adventure path and campaign setting imported from pcfinder.

    python3 scripts/generate_campaign_bestiary.py

Spec: specs/bestiaire-campagnes.md
"""

from __future__ import annotations

import re
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / 'data'
PCFINDER_DIR = DATA_DIR / 'pathfinder' / 'pcfinder'
INPUT_DIRS = (PCFINDER_DIR / 'adventure_path', PCFINDER_DIR / 'campaign_setting')
BESTIARY_PCC = PCFINDER_DIR / 'campaign_bestiary' / 'campaign_bestiary.pcc'
FILE_LINE = re.compile(r'^(ABILITY|RACE|KIT|TEMPLATE):([^|@*&]+)(\|.*)?$')

HEADER = """CAMPAIGN:Adventure Path and Campaign Setting Bestiary
KEY:Adventure Path and Campaign Setting Bestiary ~ Creatures
GAMEMODE:Pathfinder|Pathfinder_PFS
TYPE:Paizo Publishing.Pathfinder RPG
STATUS:ALPHA
GENRE:Fantasy
BOOKTYPE:Supplement
SETTING:Pathfinder
PRECAMPAIGN:1,INCLUDESBOOKTYPE=Core Rules
PRECAMPAIGN:1,INCLUDES=Bestiary,INCLUDES=Bestiary ~ Player Options Only
PUBNAMELONG:Paizo Inc.
PUBNAMESHORT:Paizo
PUBNAMEWEB:http://paizo.com
SOURCELONG:Adventure Path and Campaign Setting Bestiary
SOURCESHORT:APCSB
SOURCEWEB:http://paizo.com
ISOGL:YES

INFOTEXT:The creatures of every Pathfinder Adventure Path and Campaign Setting source imported from pcfinder-csr, in one source. It references their files and copies none. This dataset uses trademarks and/or copyrights owned by Paizo Inc., which are used under Paizo's Community Use Policy. We are expressly prohibited from charging you to use or access this content. This dataset is not published, endorsed, or specifically approved by Paizo Publishing. For more information about Paizo's Community Use Policy, please visit paizo.com/communityuse. For more information about Paizo Publishing and Paizo products, please visit paizo.com.
COPYRIGHT:Open Game License v 1.0a Copyright 2000, Wizards of the Coast, Inc.
COPYRIGHT:System Reference Document Copyright 2000, Wizards of the Coast, Inc; Authors: Jonathan Tweet, Monte Cook, Skip Williams, based on material by E. Gary Gygax and Dave Arneson.
COPYRIGHT:PCGen dataset conversion Copyright 2025, PCGen Data Team (auto-generated from pcfinder-csr)
"""


def file_lines(pcc_text: str, pcc: Path) -> list[str]:
    """The pcc's file lines, each path rewritten from the data folder."""
    folder = pcc.parent.relative_to(DATA_DIR).as_posix()
    return [f'{tag}:@/{folder}/{file_name}{include or ""}'
            for tag, file_name, include in (FILE_LINE.match(line).groups()
                                            for line in pcc_text.splitlines() if FILE_LINE.match(line))]


def main() -> None:
    pccs = sorted(pcc for input_dir in INPUT_DIRS for pcc in input_dir.glob('*/*.pcc'))
    lines = [line for pcc in pccs for line in file_lines(pcc.read_text(encoding='utf-8'), pcc)]
    BESTIARY_PCC.parent.mkdir(exist_ok=True)
    BESTIARY_PCC.write_text(HEADER + '\n' + '\n'.join(lines) + '\n', encoding='utf-8')
    print(f'{BESTIARY_PCC.relative_to(DATA_DIR)}: {len(pccs)} sources, {len(lines)} files')


if __name__ == '__main__':
    main()
