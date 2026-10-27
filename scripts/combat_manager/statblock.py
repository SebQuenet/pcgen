"""Turn the text of a Combat Manager stat block into values a PCGen build can use.

Everything here is pure: a field of the stat block goes in, plain values come out.
Matching those values to PCGen keys is left to catalog.py.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

from generate_bestiary import WEAPON_PROFICIENCY_KEYS, normalize_feat_names, normalize_kit_skills, spell_key, title_case  # noqa: F401

STAT_KEYS = ('STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA')
COIN_VALUES_IN_GP = {'pp': 10, 'gp': 1, 'sp': 0.1, 'cp': 0.01}
MINUS_SIGNS = '-–—−'
SIGN_PATTERN = r'[+\-–—−]'
DASH_PATTERN = r'[\-–—−]'


def parse_templates(text: str | None) -> list[str]:
    """Read Combat Manager's "ghost|half-fiend|" into one name per template."""
    return [name.strip(' @') for name in re.split(r'[|,]', text or '') if name.strip(' @')]


def parse_lycanthropy(template_name: str) -> tuple[str, str] | None:
    """Read "natural wererat" into PCGen's lycanthrope type and kind; a bare "wererat" is natural."""
    match = re.match(r'^(?:(natural|afflicted) )?((?:dire )?were\S.*)$', template_name.strip().lower())
    if not match:
        return None
    return f'{(match.group(1) or "natural").title()} Lycanthrope', title_case(match.group(2))


def split_list(text: str | None) -> list[str]:
    """Split a stat block list on its commas, but not inside parentheses or a number."""
    if not text:
        return []
    pieces, depth, current = [], 0, ''
    for index, character in enumerate(text):
        if character in '([':
            depth += 1
        elif character in ')]':
            depth = max(0, depth - 1)
        is_thousands_separator = (character == ',' and index > 0 and text[index - 1].isdigit()
                                  and re.match(r'\d{3}\b', text[index + 1:index + 5]))
        if character == ',' and depth == 0 and not is_thousands_separator:
            pieces.append(current.strip())
            current = ''
        else:
            current += character
    pieces.append(current.strip())
    return [piece for piece in pieces if piece]


@dataclass(frozen=True)
class ClassLevels:
    name: str
    levels: int
    archetypes: tuple[str, ...] = ()
    deity: str | None = None
    is_unchained: bool = False


def parse_classes(text: str, class_names: set[str] = frozenset()) -> list[ClassLevels]:
    """Read "inquisitor (heretic) of Abadar 10/rogue 2" into one entry per class.

    `class_names` holds the lower-cased PCGen class names, so a class whose own
    name contains "of" (Student of War) keeps it rather than losing it to a deity.
    """
    entries = []
    for part in text.split('/'):
        # The deity comes after the class or, as in "inquisitor 8 of Zaebos", after its level
        match = re.match(r'^\s*(.+?)\s+(\d+)(?:\s+of\s+(.+?))?\s*$', part)
        if not match:
            continue
        name, levels = match.group(1), int(match.group(2))
        if match.group(3):
            name = f'{name} of {match.group(3)}'
        archetypes = ()
        archetype_match = re.search(r'\(([^)]*)\)', name)
        if archetype_match:
            archetypes = tuple(entry.strip() for entry in archetype_match.group(1).split(',') if entry.strip())
            name = (name[:archetype_match.start()] + name[archetype_match.end():]).strip()
        name = re.sub(r'\s+', ' ', name)
        deity = None
        deity_match = re.match(r'^(.+?) of (.+)$', name)
        if deity_match and name.lower() not in class_names:
            name, deity = deity_match.group(1), deity_match.group(2).strip()
        is_unchained = name.lower().startswith('unchained ')
        name = re.sub(r'^(unchained|ex-)\s*', '', name, flags=re.IGNORECASE)
        entries.append(ClassLevels(name.lower(), levels, archetypes, deity, is_unchained))
    return entries


def parse_ability_scores(text: str) -> dict[str, int | None]:
    scores = {}
    for stat in STAT_KEYS:
        match = re.search(rf'\b{stat[:1]}{stat[1:].lower()}\s+(\d+|{DASH_PATTERN})', text, re.IGNORECASE)
        scores[stat] = int(match.group(1)) if match and match.group(1).isdigit() else None
    return scores


BASE_STATISTIC_PATTERNS = {
    'ac': r'\bAC (\d+)', 'touch': r'\btouch (\d+)', 'flat_footed': r'\bflat-footed (\d+)', 'hp': r'\bhp (\d+)',
    'fort': rf'\bFort ({SIGN_PATTERN}?\d+)', 'ref': rf'\bRef ({SIGN_PATTERN}?\d+)', 'will': rf'\bWill ({SIGN_PATTERN}?\d+)',
    'init': rf'\bInit ({SIGN_PATTERN}?\d+)', 'cmb': rf'\bCMB ({SIGN_PATTERN}?\d+)', 'cmd': r'\bCMD (\d+)',
}


def parse_base_statistics(text: str | None) -> dict[str, int]:
    """The numbers a stat block gives without its pre-combat spells and potions, or rage.

    "Without mage armor, Ulla's stats are as follows: AC 23, touch 13, flat-footed 20." gives ac, touch and flat_footed.
    """
    numbers = {}
    for name, pattern in BASE_STATISTIC_PATTERNS.items():
        match = re.search(pattern, text or '')
        if match:
            numbers[name] = parse_signed(match.group(1))
    return numbers


def parse_unaffected_scores(ability_scores: str | None, base_statistics: str | None) -> dict[str, int | None]:
    """The ability scores without pre-combat effects: those the base statistics give replace the published ones."""
    scores = parse_ability_scores(ability_scores or '')
    without_effects = {stat: score for stat, score in parse_ability_scores(base_statistics or '').items() if score is not None}
    return scores | without_effects


NATURAL_ATTACK_WORDS = ('bite', 'claw', 'slam', 'gore', 'tail', 'wing', 'tentacle', 'hoof', 'hooves', 'sting', 'talon',
                        'pincer', 'unarmed', 'touch', 'ray', 'bomb', 'kinetic', 'blast', 'swarm', 'butt', 'rake', 'web')


def attack_weapons(*attack_fields: str | None) -> list[str]:
    """The manufactured weapons a stat block's attacks name: "mwk greatsword +6 (2d6+1/19-20)" gives "mwk greatsword"."""
    weapons = []
    for field_text in attack_fields:
        for attack in re.split(r',|\bor\b', re.sub(r'\([^)]*\)', '', field_text or '')):
            name = re.split(rf'\s{SIGN_PATTERN}\d', attack.strip())[0].strip()
            name = re.sub(r'^\d+\s+', '', name)
            words = name.lower().split()
            if words and not any(word.rstrip('s') in NATURAL_ATTACK_WORDS or word in NATURAL_ATTACK_WORDS for word in words) \
                    and name.lower() not in (weapon.lower() for weapon in weapons):
                weapons.append(name)
    return weapons


CLASS_CHOICE_LISTS = r'talents?|tricks?|powers?|discoveries|discovery|mercies|mercy|hexes|revelations?|arcanas?|exploits?|dares?|deeds?|insights?|graces?|stunts?'


def class_choices(special_qualities: str | None) -> list[str]:
    """The talents, tricks, rage powers, mercies or hexes a stat block lists: "rogue talents (charmer, fast fingers)"."""
    choices = []
    for listed in re.finditer(rf'\b(?:{CLASS_CHOICE_LISTS})\s*\(([^()]*(?:\([^()]*\)[^()]*)*)\)', special_qualities or '', re.I):
        for entry in split_list(listed.group(1)):
            name = re.sub(r'\s*\([^)]*\)|\s+\d+/(?:day|round|week)|[*]|\b(?:APG|UC|UM|ACG|ARG|OA)\b', '', entry).strip()
            if name:
                choices.append(name)
    return choices


def parse_signed(text: str | None) -> int | None:
    """The first signed number of a field such as "+7" or "+4 (+6 vs. poison)"."""
    if not text:
        return None
    match = re.search(rf'({SIGN_PATTERN}?)\s*(\d+)', text)
    if not match:
        return None
    return -int(match.group(2)) if match.group(1) and match.group(1) in MINUS_SIGNS else int(match.group(2))


def parse_feats(text: str | None) -> list[tuple[str, str | None]]:
    """Each feat as a PCGen key and the choice it carries, if any."""
    feats = []
    for entry in split_list(text):
        for name in normalize_feat_names(entry):
            match = re.match(r'^(.+?) \((.+)\)$', name)
            if not match:
                feats.append((name, None))
                continue
            key, choice = match.groups()
            choice = WEAPON_PROFICIENCY_KEYS.get(choice.lower(), choice)
            feats.append((key, choice[:1].upper() + choice[1:]))
    return feats


def parse_skills(text: str | None) -> dict[str, int]:
    """Each skill's published total, under the PCGen key of the skill."""
    totals = {}
    for entry in split_list(text):
        match = re.match(rf'^(.+?)\s+({SIGN_PATTERN}\d+)', entry)
        if not match:
            continue
        total = parse_signed(match.group(2))
        for key in normalize_kit_skills(match.group(1).strip()):
            totals[key] = total
    return totals


@dataclass(frozen=True)
class GearItem:
    name: str
    quantity: int = 1
    enhancement: int = 0
    specials: tuple[str, ...] = ()
    material: str | None = None
    is_masterwork: bool = False
    strength_rating: int | None = None


@dataclass(frozen=True)
class Gear:
    items: list[GearItem]
    gold: int


MATERIALS = ('alchemical silver', 'cold iron', 'mithral', 'adamantine', 'darkwood', 'dragonhide', 'silver', 'bronze', 'obsidian')
# Weapon and armour special abilities that come before the base item's name.
SPECIAL_ABILITY_WORDS = (
    'adaptive', 'agile', 'allying', 'anarchic', 'axiomatic', 'bane', 'brilliant energy', 'corrosive', 'corrosive burst',
    'dancing', 'defending', 'disruption', 'distance', 'flaming', 'flaming burst', 'frost', 'ghost touch', 'holy',
    'icy burst', 'keen', 'ki focus', 'merciful', 'mighty cleaving', 'returning', 'seeking', 'shock', 'shocking burst',
    'speed', 'spell storing', 'thundering', 'throwing', 'unholy', 'vicious', 'vorpal', 'wounding', 'light fortification',
    'moderate fortification', 'heavy fortification', 'fortification', 'glamered', 'shadow', 'improved shadow',
    'greater shadow', 'slick', 'improved slick', 'greater slick', 'spell resistance', 'arrow catching', 'arrow deflection',
    'animated', 'bashing', 'blinding', 'reflecting', 'invulnerability', 'energy resistance', 'wild', 'righteous', 'unrighteous',
)


def parse_gear_item(text: str) -> list[GearItem]:
    """One gear entry, which may carry its ammunition after "with"."""
    with_match = re.match(r'^(.+?) with (\d+) (.+)$', text)
    if with_match:
        return parse_gear_item(with_match.group(1)) + parse_gear_item(f'{with_match.group(3)} ({with_match.group(2)})')

    # a trailing "*" or "UC" marks the book an item comes from
    name, quantity = re.sub(r'(?<=[a-z)\]])(?:\*|APG|ARG|UC|UE|UM|ACG|OA)$', '', text.strip()), 1
    quantity_match = re.search(r'[(\[](\d+)[)\]]\s*$', name)
    leading_quantity_match = re.match(r'^(\d+) (\D.+)$', name)
    if quantity_match:
        quantity = int(quantity_match.group(1))
        name = name[:quantity_match.start()].strip()
    elif leading_quantity_match:
        quantity, name = int(leading_quantity_match.group(1)), leading_quantity_match.group(2)
    strength_match = re.search(r'\(([+-]\d+) Str\)', name, re.IGNORECASE)
    strength_rating = int(strength_match.group(1)) if strength_match else None
    # charges, a strength rating or a value in parentheses say nothing PCGen keys on
    name = re.sub(r'\s*\((?:\d+ charges?|[+-]\d+ Str|worth [^)]*|[^)]*\bgp\b[^)]*)\)', '', name, flags=re.IGNORECASE)
    name = re.sub(r'\s+', ' ', name).strip()

    enhancement = 0
    enhancement_match = re.match(r'^\+(\d+)\s+(.+)$', name)
    if enhancement_match:
        enhancement, name = int(enhancement_match.group(1)), enhancement_match.group(2)
    is_masterwork = False
    masterwork_match = re.match(r'^(?:masterwork|mwk)\s+(.+)$', name, re.IGNORECASE)
    if masterwork_match:
        is_masterwork, name = True, masterwork_match.group(1)

    specials = []
    if enhancement:
        found = True
        while found:
            found = False
            for word in sorted(SPECIAL_ABILITY_WORDS, key=len, reverse=True):
                if name.lower().startswith(word + ' '):
                    specials.append(word)
                    name = name[len(word) + 1:]
                    found = True
                    break
    material = None
    for candidate in MATERIALS:
        if name.lower().startswith(candidate + ' '):
            material, name = candidate, name[len(candidate) + 1:]
            break
    # "wooden unholy symbol of Urgathoa": PCGen sells a holy symbol by what it is made of
    symbol_match = re.match(r'^(\w+) ((?:un)?holy symbol\b.*)$', name, re.IGNORECASE)
    if not material and symbol_match:
        material, name = symbol_match.group(1).lower(), symbol_match.group(2)

    return [GearItem(name, quantity, enhancement, tuple(specials), material, is_masterwork, strength_rating)]


def parse_gear(*fields: str | None) -> Gear:
    items, gold = [], 0.0
    for field_text in fields:
        for entry in split_list(field_text):
            coin_match = re.match(r'^([\d,]+)\s*(pp|gp|sp|cp)$', entry.strip())
            if coin_match:
                gold += int(coin_match.group(1).replace(',', '')) * COIN_VALUES_IN_GP[coin_match.group(2)]
                continue
            items.extend(parse_gear_item(entry))
    return Gear(items, int(gold))


@dataclass(frozen=True)
class SpellBlock:
    class_name: str
    is_prepared: bool
    spells: dict[int, dict[str, int]] = field(default_factory=dict)


def parse_spell_list(text: str) -> dict[str, int]:
    spells = {}
    for entry in split_list(text):
        count = 1
        details = re.search(r'\(([^)]*)\)', entry)
        if details:
            count_match = re.match(r'^\s*(\d+)\b(?!\s*/)', details.group(1))
            if count_match and not details.group(1).strip().startswith('DC'):
                count = int(count_match.group(1))
            entry = entry[:details.start()] + entry[details.end():]
        entry = re.sub(r'([a-z]|\b[IVX]{1,4})[DS]$', r'\1', entry.strip())  # domain and school spell markers
        if entry:
            key = spell_key(entry)
            spells[key] = spells.get(key, 0) + count
    return spells


def parse_spells(*fields: str | None) -> list[SpellBlock]:
    """Read "Wizard Spells Prepared (CL 1st) 1st-burning hands, ..." blocks."""
    blocks = []
    for field_text in fields:
        if not field_text:
            continue
        for header in re.finditer(r'([A-Z][\w\' -]*?) (?:Spells|Extracts) (Prepared|Known)\s*\([^)]*\)', field_text):
            start = header.end()
            following = re.search(r'[A-Z][\w\' -]*? (?:Spells|Extracts) (?:Prepared|Known)\s*\(', field_text[start:])
            body = field_text[start:start + following.start()] if following else field_text[start:]
            body = re.split(r'\s+D Domain spells?|\s+S (?:School|Spirit|Bloodline) spells?|\s+Domains? |\s+Bloodline |\s+Mystery |\s+Patron ', body)[0]
            levels = {}
            parts = re.split(r'(?:^|\s{2,}|\s)(\d+)(?:st|nd|rd|th)?\s*(?:\([^)]*\))?\s*[-–]', ' ' + body)
            for index in range(1, len(parts) - 1, 2):
                levels[int(parts[index])] = parse_spell_list(parts[index + 1])
            blocks.append(SpellBlock(header.group(1).strip(), header.group(2) == 'Prepared', levels))
    return blocks
