#!/usr/bin/env python3
"""Give the generated monsters that have no attack the attacks of their stat block.

    python3 scripts/combat_manager/repair_monster_attacks.py <Combat Manager extracted dir>

The races generated from pcfinder lost their attacks wherever pcfinder could not parse the melee line, as
"+1 wounding spear +21/+16/+11 (...), 2 claws +19 (1d6+6)", and their kits hold no weapon. For each race the bestiary
index builds without any attack, the stat block (Combat Manager's, pcfinder's otherwise) gives back its natural
attacks, written into the race line, and its weapons, written into its default kit: the first one in hand, the others
carried, at the creature's size. A weapon's enhancement and special abilities are left out.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPTS_DIR))
sys.path.insert(0, str(Path(__file__).resolve().parent))

import catalog  # noqa: E402
import monsters  # noqa: E402
import statblock  # noqa: E402
from generate_bestiary import NATURAL_WEAPON_MAP, attack_title, build_natural_attacks  # noqa: E402
from repair_kit_feats import named_exactly  # noqa: E402

TWO_HANDED = 'Both Hands'
ONE_HANDED = 'Primary Hand'
CARRIED = 'Carried'
MEDIUM = 'M'
REPOSITORY = SCRIPTS_DIR.parent
PATHFINDER_DATA = REPOSITORY / 'data' / 'pathfinder'
BESTIARY_INDEX = REPOSITORY / 'code/src/resources/pcgen/bestiary/index.json'
# Weapons held in both hands: two-handed melee weapons and bows
TWO_HANDED_TYPES = ('TwoHanded', 'Projectile')
# The last word of a ranged attack the creature makes with its own body: "earth blast", "sonic ray", "thorns"
RANGED_NATURAL_WORDS = ('blast', 'ray', 'rays', 'bolt', 'lash', 'thorns', 'spore', 'vortices')


def cleaned_attack_line(text: str) -> str:
    """The attack line without the slips of its stat block: "slam +7 melee (1d8+4)", "hook-blade +5 (1d4))",
    a last alternative left as a dangling "or"."""
    text = re.sub(r'\s+melee\s*\(', ' (', text or '').replace('))', ')')
    return re.sub(r'(?:[\s,]+or)?[\s,]*$', '', text)


def weapon_names(text: str) -> list[str]:
    """The name of every attack of an attack line, its bonus and damage left out when the line gives them."""
    names = []
    for alternative in re.split(r'\s+or\s+', cleaned_attack_line(text)):
        for piece in statblock.split_list(alternative):
            attack = monsters.parse_attack(piece)
            names.append(attack['name'] if attack else re.sub(r'\s+[+\-]\d.*$|\s*\(.*$', '', piece.strip()).lower())
    return [name for name in names if name]


def natural_attacks_tag(melee_text: str, ranged_text: str = '') -> str:
    """The NATURALATTACKS tag of the natural attacks of the melee and ranged lines; empty when they have none."""
    melee = build_natural_attacks({'parsedAttacks': {'melee': {'primary': {'attacks': monsters.natural_attacks(cleaned_attack_line(melee_text))}}}})
    melee_names = {entry.split(',')[0] for entry in melee.removeprefix('NATURALATTACKS:').split('|') if entry}
    ranged = [entry for entry in ranged_natural_attacks(ranged_text) if entry.split(',')[0] not in melee_names]
    entries = [entry for entry in melee.removeprefix('NATURALATTACKS:').split('|') if entry] + ranged
    return 'NATURALATTACKS:' + '|'.join(entries) if entries else ''


def ranged_natural_attacks(ranged_text: str) -> list[str]:
    """The entries of the ranged line's attacks the creature makes with its own body, as "earth blast +4 (2d6+6)"."""
    entries = []
    for attack in monsters.natural_attacks(cleaned_attack_line(ranged_text)):
        if attack['name'].split()[-1] not in RANGED_NATURAL_WORDS:
            continue
        dice = attack['damage']['dice']
        damage = f"{dice['count']}d{dice['sides']}" if dice else '0'
        touch = '.Touch' if attack.get('isTouch') else ''
        entry = f"{attack_title(attack['name'])},Weapon.Natural.Ranged{touch},*{attack['quantity']},{damage}"
        effects = [effect['name'] for effect in attack.get('specialEffects', [])]
        entries.append(entry + (f",SPROP=plus {' and '.join(effects)}" if effects else ''))
    return entries


def with_natural_attacks(race_line: str, tag: str) -> str:
    """The race line with the tag, before its monster class as the generated races write it."""
    columns = race_line.split('\t')
    position = next((index for index, column in enumerate(columns) if column.startswith('MONSTERCLASS:')), len(columns))
    return '\t'.join(columns[:position] + [tag] + columns[position:])


def weapon_key(name: str, weapons: dict[str, str]) -> str | None:
    """The weapon PCGen sells under that name, read without its leading words: "+1 wounding spear" is a Spear."""
    if name in NATURAL_WEAPON_MAP:
        return None
    index = catalog.KeyIndex(sorted(weapons))
    words = name.split()
    return next((key for key in (named_exactly(index, ' '.join(words[start:])) for start in range(len(words))) if key), None)


def gear_lines(melee_text: str, ranged_text: str, weapons: dict[str, str], size: str) -> list[str]:
    """The kit lines of the stat block's weapons: the first in hand, the others carried."""
    keys = []
    for name in weapon_names(melee_text) + weapon_names(ranged_text):
        key = weapon_key(name, weapons)
        if key and key not in keys:
            keys.append(key)
    sized = '' if size == MEDIUM else f'\tSIZE:{size}'
    return [f'GEAR:{key}\tQTY:1{sized}\tLOCATION:{weapons[key] if position == 0 else CARRIED}'
            for position, key in enumerate(keys)]


def load_weapons() -> dict[str, str]:
    """The weapons PCGen sells, each with the hands it is wielded in."""
    wield_of_base = {}
    copies = []
    for path in PATHFINDER_DATA.rglob('*equip*.lst'):
        for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
            columns = line.split('\t')
            key = next((column[4:].strip() for column in columns if column.startswith('KEY:')), columns[0].strip())
            types = next((column for column in columns if column.startswith('TYPE:')), '')
            if key.endswith('(Base)') and 'Weapon' in types and 'Natural' not in types:
                wield_of_base[key] = TWO_HANDED if any(kind in types for kind in TWO_HANDED_TYPES) else ONE_HANDED
            copied = re.match(r'^(.+?)\.COPY=', columns[0])
            if copied:
                copies.append((copied.group(1).strip(), key))
    return {key: wield_of_base[base] for base, key in copies if base in wield_of_base}


def races_without_attacks() -> list[str]:
    entries = json.loads(BESTIARY_INDEX.read_text(encoding='utf-8'))
    return [entry['key'] for entry in entries if entry['kind'] == 'race' and entry['values'].get('ATTACK_COUNT') == 0]


def main() -> None:
    import generate_bestiary_tactics as tactics
    import records

    extracted_dir = Path(sys.argv[1])
    combat_manager = {}
    for record in records.load_monsters(extracted_dir) + records.load_npcs(extracted_dir):
        combat_manager.setdefault(tactics.slug_of(tactics.sanitize_name(record.fields.get('Name') or '')), record.fields)
    pcfinder = {tactics.slug_of(tactics.sanitize_name(creature['name'])): creature
                for creature in json.loads(tactics.PCFINDER_CREATURES.read_text(encoding='utf-8'))}
    weapons = load_weapons()
    targets = set(races_without_attacks())
    natural = armed = unknown = 0
    for races_file in sorted(PATHFINDER_DATA.rglob('*races*.lst')):
        lines = races_file.read_text(encoding='utf-8').split('\n')
        kits_file = races_file.with_name(races_file.name.replace('races', 'kits_race'))
        kit_lines = kits_file.read_text(encoding='utf-8').split('\n') if kits_file.is_file() else []
        changed = False
        for number, line in enumerate(lines):
            columns = line.split('\t')
            key = next((column[4:].strip() for column in columns if column.startswith('KEY:')), columns[0].strip())
            if key not in targets:
                continue
            fields = tactics.stat_block_of(key, columns[0].strip(), combat_manager, pcfinder)
            if not fields:
                unknown += 1
                continue
            melee = fields.get('Melee') or fields.get('melee') or ''
            ranged = fields.get('Ranged') or fields.get('ranged') or ''
            tag = natural_attacks_tag(melee, ranged)
            if tag and 'NATURALATTACKS:' not in line:
                lines[number] = with_natural_attacks(line, tag)
                natural += 1
                changed = True
            size = next((column[5:].strip() for column in columns if column.startswith('SIZE:')), MEDIUM)
            gear = gear_lines(melee, ranged, weapons, size)
            start = next((index for index, kit_line in enumerate(kit_lines)
                          if kit_line.startswith(f'STARTPACK:{key} Default\t')), None)
            if gear and start is not None:
                end = next((index for index in range(start + 1, len(kit_lines)) if not kit_lines[index].strip()), len(kit_lines))
                if any(kit_line.startswith('GEAR:') for kit_line in kit_lines[start:end]):
                    continue
                kit_lines[end:end] = gear
                armed += 1
        if changed:
            races_file.write_text('\n'.join(lines), encoding='utf-8')
        if kit_lines and kits_file.is_file() and '\n'.join(kit_lines) != kits_file.read_text(encoding='utf-8'):
            kits_file.write_text('\n'.join(kit_lines), encoding='utf-8')
    print(f'{len(targets)} races without attack: {natural} given natural attacks, {armed} armed, {unknown} without stat block')


if __name__ == '__main__':
    main()
