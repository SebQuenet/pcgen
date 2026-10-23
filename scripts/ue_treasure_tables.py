#!/usr/bin/env python3
"""Turn the treasure generation tables of Ultimate Equipment into the game table's data.

Reads pages of the Ultimate Equipment section of legacy.aonprd.com, saved beforehand, and writes
code/src/resources/pcgen/core/gametable/treasure-tables.json:

- familiesByCreatureType: the treasure types (A to I) each creature type may have;
- lotsByFamily: the rewards of each treasure type, with their value and what they hold;
- pools: for each kind of random item a reward names ("lesser minor potion", "grade 3 gemstone"),
  every item it can turn into, with its price and its weight, the chained d% rolls multiplied out.

Usage:
    python3 scripts/ue_treasure_tables.py --pages <directory of saved pages> \\
        --output code/src/resources/pcgen/core/gametable/treasure-tables.json

Pages expected in the directory (named after their path, '/' turned into '_'):
    appendix.html, ringsRodsStaves_{rings,rods,staves}.html,
    magicArmsAndArmor_{specificMagicArmorShields,specificMagicWeapons}.html,
    wondrousItems_{belts,body,chest,eyes,feet,hands,head,headbands,neck,shoulders,wrists,slotless}.html
"""

import argparse
import html
import json
import re
from pathlib import Path

GRADES = ['lesser minor', 'greater minor', 'lesser medium', 'greater medium', 'lesser major', 'greater major']
LEVELS = ['0', '1st', '2nd', '3rd', '4th', '5th', '6th', '7th', '8th', '9th']
FAMILIES = {
    'aberration': 'ABDE', 'animal': 'ABDE', 'construct': 'EF', 'dragon': 'ABCHI', 'fey': 'BCDG',
    'humanoid': 'ABDEFG', 'magical beast': 'ABDE', 'monstrous humanoid': 'ABCDEH', 'ooze': 'ABD',
    'outsider': 'ABCDEFGHI', 'plant': 'ABDE', 'undead': 'ABDE', 'vermin': 'ABD',
}
WONDROUS_SLOTS = {
    'belts': 'Belt', 'body': 'Body', 'chest': 'Chest', 'eyes': 'Eye', 'feet': 'Feet', 'hands': 'Hand',
    'head': 'Head', 'headbands': 'Headband', 'neck': 'Neck', 'shoulders': 'Shoulders', 'wrists': 'Wrists',
    'slotless': 'Slotless',
}
GEM_HEADERS = ['Least Semi-Precious Gem', 'Lesser Semi-Precious Gem', 'Semi-Precious Gem', 'Greater Semi-Precious Gem',
               'Lesser Precious Gem', 'Greater Precious Gem']
# Core Rulebook prices of the base armor, shields and weapons the Random Armor or Shield and Random
# Weapon tables name; masterwork adds 150 gp to armor and shields, 300 gp to weapons.
LIGHT_ARMOR = {'Padded armor': 5, 'Leather armor': 10, 'Studded leather armor': 25, 'Chain shirt': 100}
MEDIUM_ARMOR = {'Hide': 15, 'Scale mail': 50, 'Chainmail': 150, 'Breastplate': 200}
HEAVY_ARMOR = {'Splint mail': 200, 'Banded mail': 250, 'Half-plate': 600, 'Full plate': 1500}
SHIELDS = {'Buckler': 5, 'Light wooden shield': 3, 'Light steel shield': 9, 'Heavy wooden shield': 7,
           'Heavy steel shield': 20, 'Tower shield': 30}
WEAPONS = {
    'Bastard sword': 35, 'Battleaxe': 10, 'Bolas': 5, 'Club': 0, 'Composite longbow': 100, 'Composite shortbow': 75,
    'Dagger': 2, 'Dart': 0, 'Dwarven waraxe': 30, 'Falchion': 75, 'Gauntlet': 2, 'Glaive': 8, 'Greataxe': 20,
    'Greatclub': 5, 'Greatsword': 50, 'Halberd': 10, 'Handaxe': 6, 'Heavy crossbow': 50, 'Heavy flail': 15,
    'Heavy mace': 12, 'Lance': 10, 'Light crossbow': 35, 'Light flail': 8, 'Light hammer': 1, 'Light mace': 5,
    'Light pick': 4, 'Longbow': 75, 'Longspear': 5, 'Longsword': 15, 'Morningstar': 8, 'Nunchaku': 2,
    'Quarterstaff': 0, 'Rapier': 20, 'Spear': 2, 'Trident': 15, 'Warhammer': 12, 'Whip': 1,
}
COUNTS = {'a': 1, 'an': 1, 'one': 1, 'two': 2, 'three': 3, 'four': 4, 'five': 5, 'six': 6, 'seven': 7, 'eight': 8,
          'nine': 9, 'ten': 10}
COUNT = r'(?:(\d+|' + '|'.join(COUNTS) + r')\s+)?'
COINS = re.compile(r'^(\d+)d(\d+)\s*(?:[x×]\s*([\d,]+)\s*)?(cp|sp|gp|pp)$')
ITEM = re.compile('^' + COUNT + r'(lesser|greater) (minor|medium|major) '
                  r'(potion|scroll|wand|ring|rod|staff|wondrous item|armor|weapon|shield)s?$')
MASTERWORK = re.compile('^' + COUNT + r'masterwork (light armor or shield|medium armor|heavy armor|weapon|shield)s?$')
GRADED = re.compile('^' + COUNT + r'grade (\d) (gemstone|art object)s?$')
RANGE = re.compile(r'^(\d+)(?:\s*[–-]\s*(\d+))?$')


def clean(cell):
    text = html.unescape(re.sub(r'<[^>]+>', '', cell))
    return re.sub(r'\s+', ' ', text).strip().rstrip('*').strip()


def count_of(word):
    if not word:
        return 1
    return int(word) if word.isdigit() else COUNTS[word]


def parse_reward(text):
    """The parts of a reward, such as '2d4 × 10 gp, two grade 3 gemstones'; ValueError for a part it cannot read."""
    parts = []
    for raw in re.split(r',\s+', text):
        part = raw.strip().lower()
        if not part:
            continue
        coins = COINS.match(part)
        item = ITEM.match(part)
        masterwork = MASTERWORK.match(part)
        graded = GRADED.match(part)
        if coins:
            parts.append({'kind': 'coins', 'count': 1, 'dice': int(coins.group(1)), 'sides': int(coins.group(2)),
                          'multiplier': int((coins.group(3) or '1').replace(',', '')), 'coin': coins.group(4)})
        elif item:
            kind = 'wondrous' if item.group(4) == 'wondrous item' else item.group(4)
            parts.append({'kind': 'pool', 'count': count_of(item.group(1)),
                          'pool': f'{kind}/{item.group(2)} {item.group(3)}'})
        elif masterwork:
            parts.append({'kind': 'pool', 'count': count_of(masterwork.group(1)), 'pool': f'masterwork/{masterwork.group(2)}'})
        elif graded:
            kind = 'gem' if graded.group(3) == 'gemstone' else 'art'
            parts.append({'kind': 'pool', 'count': count_of(graded.group(1)), 'pool': f'{kind}/{graded.group(2)}'})
        else:
            raise ValueError(f'Cannot read the reward part "{part}" of "{text}"')
    return parts


def parse_price(text):
    """The gold pieces of a price such as '2,500 gp' or '12 gp, 5 sp', silver and copper left out; None for text."""
    found = re.match(r'^\s*([\d,]+)\s*gp', text)
    return int(found.group(1).replace(',', '')) if found else None


def table_chunks(page):
    """The text of each table, cut at the next table when one is left unclosed."""
    return [chunk.split('</table>')[0] for chunk in re.split(r'<table\b', page)[1:]]


def read_tables(page):
    """Every d% table of a page, keyed by (caption, header of its name column), as (weight, other cells) rows."""
    found = {}
    for table in table_chunks(page):
        caption_match = re.search(r'(?s)<caption>(.*?)</caption>', table)
        caption = clean(caption_match.group(1)) if caption_match else ''
        key = None
        for row in re.findall(r'(?s)<tr[^>]*>(.*?)</tr>', table):
            headers = [clean(cell) for cell in re.findall(r'(?s)<th[^>]*>(.*?)</th>', row)]
            if headers:
                key = (caption, headers[1] if len(headers) > 1 else headers[0])
                found.setdefault(key, [])
                continue
            cells = [clean(cell) for cell in re.findall(r'(?s)<td[^>]*>(.*?)(?=<td|$)', row)]
            span = RANGE.match(cells[0]) if cells else None
            if key and span:
                low = int(span.group(1))
                high = int(span.group(2) or low)
                found[key].append((high - low + 1, cells[1:]))
    return found


def read_rows(page, caption):
    """The plain rows of the table with that caption, every cell cleaned."""
    for table in table_chunks(page):
        caption_match = re.search(r'(?s)<caption>(.*?)</caption>', table)
        if caption_match and clean(caption_match.group(1)) == caption:
            rows = []
            for row in re.findall(r'(?s)<tr[^>]*>(.*?)</tr>', table):
                cells = [clean(cell) for cell in re.findall(r'(?s)<td[^>]*>(.*?)(?=<td|$)', row)]
                if cells:
                    rows.append(cells)
            return rows
    raise ValueError(f'No table "{caption}"')


def by_header(tables, header):
    """The rows of the one table whose name column has that header."""
    matching = [rows for (caption, found), rows in tables.items() if found == header]
    if len(matching) != 1:
        raise ValueError(f'{len(matching)} tables headed "{header}"')
    return matching[0]


def priced_pool(rows, prefix, resold_in_full=False):
    """A pool of the rows that carry a price, each row weighing its share of the d%."""
    pool = []
    for weight, cells in rows:
        price = parse_price(cells[-1]) if len(cells) > 1 else None
        if price is not None:
            entry = {'name': prefix + cells[0], 'price': price, 'weight': weight}
            if resold_in_full:
                entry['resoldInFull'] = True
            pool.append(entry)
    return pool


def grade_distribution(page, caption):
    """For each grade, the spell levels its items have, with their weights."""
    distribution = {grade: [] for grade in GRADES}
    for cells in read_rows(page, caption):
        if len(cells) < len(GRADES) + 1 or cells[len(GRADES)] not in LEVELS:
            continue
        for grade, cell in zip(GRADES, cells):
            span = RANGE.match(cell)
            if span:
                low = int(span.group(1))
                distribution[grade].append((int(span.group(2) or low) - low + 1, cells[len(GRADES)]))
    return distribution


def chained_pool(distribution, kinds, table_for, name_for):
    """Multiply out a level roll, a kind roll and the item roll into one weighted pool per grade."""
    pools = {}
    for grade, levels in distribution.items():
        pool = []
        for level_weight, level in levels:
            for kind_weight, kind in kinds(level):
                rows = table_for(level, kind)
                for entry in priced_pool(rows, ''):
                    entry['name'] = name_for(entry['name'], kind)
                    entry['weight'] *= level_weight * kind_weight
                    pool.append(entry)
        if pool:
            pools[grade] = pool
    return pools


def magic_item_pools(pages):
    appendix = pages['appendix']
    tables = read_tables(appendix)
    pools = {}

    def common_or_uncommon(level):
        return [(100, 'Common')] if level == '0' else [(75, 'Common'), (25, 'Uncommon')]

    potions = chained_pool(grade_distribution(appendix, 'Random Potions and Oils'), common_or_uncommon,
                           lambda level, kind: tables.get((f'{level}-Level Potions and Oils', f'{kind} Potion or Oil'), []),
                           lambda name, kind: f'Potion of {name}')
    scroll_kinds = [(weight, cells[0]) for weight, cells in tables[('Type of Scroll', 'Scroll Type')]]
    scrolls = chained_pool(grade_distribution(appendix, 'Random Scrolls'), lambda level: scroll_kinds,
                           lambda level, kind: tables.get((f'{level}-Level {kind.split()[1].capitalize()} Scrolls',
                                                           f'{kind.split()[0]} Scroll'), []),
                           lambda name, kind: f'Scroll of {name} ({kind.split()[1]})')
    wands = chained_pool(grade_distribution(appendix, 'Random Wands'), lambda level: [(75, 'Common'), (25, 'Uncommon')],
                         lambda level, kind: tables.get((f'{level}-Level Wands', f'{kind} Wand'), []),
                         lambda name, kind: f'Wand of {name}')
    for kind, chained in (('potion', potions), ('scroll', scrolls), ('wand', wands)):
        for grade, pool in chained.items():
            pools[f'{kind}/{grade}'] = pool

    for page, item, prefix in (('ringsRodsStaves_rings', 'Ring', ''), ('ringsRodsStaves_rods', 'Rod', ''),
                               ('ringsRodsStaves_staves', 'Staff', ''),
                               ('magicArmsAndArmor_specificMagicArmorShields', 'Armor', ''),
                               ('magicArmsAndArmor_specificMagicArmorShields', 'Shield', ''),
                               ('magicArmsAndArmor_specificMagicWeapons', 'Specific Weapons', '')):
        page_tables = read_tables(pages[page])
        for grade in GRADES:
            header = f'{grade.title()} {item}'
            if any(found == header for (_, found) in page_tables):
                kind = 'weapon' if item == 'Specific Weapons' else item.lower()
                pool = priced_pool(by_header(page_tables, header), prefix)
                if pool:
                    pools[f'{kind}/{grade}'] = pool

    for grade in GRADES:
        pool = []
        for page, slot in WONDROUS_SLOTS.items():
            pool.extend(priced_pool(by_header(read_tables(pages['wondrousItems_' + page]), f'{grade.title()} {slot} Item'), ''))
        pools[f'wondrous/{grade}'] = pool
    return pools


def gem_and_art_pools(appendix):
    tables = read_tables(appendix)
    pools = {}
    for grade, prefix in enumerate(GEM_HEADERS, start=1):
        rows = next(rows for (caption, header), rows in tables.items() if header.startswith(prefix + ' ('))
        pool = []
        for weight, cells in rows:
            added = re.match(r'^\+(\d+)d(\d+)(?:\s*[x×]\s*([\d,]+))?\s*gp$', cells[2])
            pool.append({'name': cells[0], 'price': parse_price(cells[1]), 'weight': weight, 'resoldInFull': True,
                         'dice': {'count': int(added.group(1)), 'sides': int(added.group(2)),
                                  'multiplier': int((added.group(3) or '1').replace(',', ''))}})
        pools[f'gem/{grade}'] = pool
    for grade in range(1, 7):
        rows = next(rows for (caption, header), rows in tables.items() if header.startswith(f'Grade {grade} Art Object'))
        pools[f'art/{grade}'] = priced_pool(rows, '', resold_in_full=True)
    return pools


def masterwork_pools(appendix):
    tables = read_tables(appendix)
    armor_weights = {cells[0]: weight for weight, cells in by_header(tables, 'Type of Armor or Shield')}
    weapon_weights = {cells[0]: weight for weight, cells in by_header(tables, 'Type of Weapon')}

    def pool(prices, weights, extra):
        return [{'name': f'Masterwork {name.lower()}', 'price': price + extra, 'weight': weights[name]}
                for name, price in prices.items() if name in weights]

    return {
        'masterwork/light armor or shield': pool(LIGHT_ARMOR, armor_weights, 150) + pool(SHIELDS, armor_weights, 150),
        'masterwork/medium armor': pool(MEDIUM_ARMOR, armor_weights, 150),
        'masterwork/heavy armor': pool(HEAVY_ARMOR, armor_weights, 150),
        'masterwork/shield': pool(SHIELDS, armor_weights, 150),
        'masterwork/weapon': pool(WEAPONS, weapon_weights, 300),
    }


def lots(appendix):
    found = {}
    for family in 'ABCDEFGHI':
        caption = next(caption for caption in re.findall(r'(?s)<caption>(.*?)</caption>', appendix)
                       if clean(caption).startswith(f'Type {family} Treasure'))
        found[family] = [{'value': int(re.sub(r'[^\d]', '', cells[0])), 'reward': parse_reward(cells[1])}
                         for cells in read_rows(appendix, clean(caption)) if len(cells) >= 2]
    return found


def main():
    parser = argparse.ArgumentParser(description='Extract the treasure generation tables of Ultimate Equipment')
    parser.add_argument('--pages', required=True, type=Path, help='Directory of the saved pages')
    parser.add_argument('--output', required=True, type=Path, help='Path of the JSON file to write')
    args = parser.parse_args()

    pages = {path.stem: path.read_text(encoding='utf-8', errors='replace') for path in args.pages.glob('*.html')}
    pools = {**magic_item_pools(pages), **gem_and_art_pools(pages['appendix']), **masterwork_pools(pages['appendix'])}
    found_lots = lots(pages['appendix'])
    named = {part['pool'] for family in found_lots.values() for lot in family for part in lot['reward']
             if part['kind'] == 'pool'}
    missing = sorted(name for name in named if not pools.get(name))
    if missing:
        raise SystemExit(f'Rewards name pools that hold nothing: {missing}')
    tables = {'familiesByCreatureType': {kind: list(letters) for kind, letters in FAMILIES.items()},
              'lotsByFamily': found_lots, 'pools': dict(sorted(pools.items()))}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(tables, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(f'{sum(len(family) for family in found_lots.values())} rewards and {len(pools)} pools written to {args.output}')


if __name__ == '__main__':
    main()
