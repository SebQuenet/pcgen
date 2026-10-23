"""Find the PCGen key a stat block's name stands for.

PCGen keys qualified items as "Shield (Heavy/Steel)" and "Scroll of Invisibility
(Greater)" where a stat block writes "heavy steel shield" and "scroll of greater
invisibility". A KeyIndex knows every reading of each key and tries the stat
block's name against them, plainest first.
"""

from __future__ import annotations

import re

from statblock import ClassLevels

WIZARD_SCHOOLS = {
    'abjurer': 'Abjurer', 'conjurer': 'Conjurer', 'diviner': 'Diviner', 'enchanter': 'Enchanter',
    'evoker': 'Evoker', 'illusionist': 'Illusionist', 'necromancer': 'Necromancer',
    'transmuter': 'Transmuter', 'universalist': 'Universalist', 'wizard': None,
}
# Peoples of Golarion a stat block names in place of human
HUMAN_ETHNICITIES = {
    'azlanti', 'bonuwat', 'chelaxian', 'erutaki', 'garundi', 'kellid', 'keleshite', 'kuru', 'mwangi', 'mwangi-zenji',
    'shoanti', 'taldan', 'tian', 'ulfen', 'varisian', 'vudrani', 'zenj',
}
# A Thassilonian specialist's school is one of the sins: a "thassilonian conjurer" is a Sloth wizard
THASSILONIAN_SINS = {
    'abjurer': 'Envy', 'conjurer': 'Sloth', 'enchanter': 'Lust', 'evoker': 'Wrath',
    'illusionist': 'Pride', 'necromancer': 'Gluttony', 'transmuter': 'Greed',
}
# PCGen's spelling of the materials and abilities a stat block names differently
MODIFIER_NAMES = {'mithral': 'mithril', 'silver': 'alchemical silver'}


def readings(key: str) -> list[str]:
    """Every lower-cased name a stat block may give the item keyed `key`."""
    lowered = key.lower()
    names = [lowered]
    qualified = re.match(r'^([^(]+?) \(([^)]+)\)$', lowered)
    if qualified:
        base, qualifiers = qualified.groups()
        parts = qualifiers.split('/')
        names.append(' '.join(parts) + ' ' + base)
        # "Rope (Silk/50 ft)" is a "silk rope", "Rations (Trail/Per Day)" "trail rations"
        names.append(parts[0] + ' ' + base)
    return names


class KeyIndex:

    def __init__(self, keys: list[str]) -> None:
        self.keys = sorted(keys, key=lambda key: (len(key), key))
        self.by_reading: dict[str, str] = {}
        for key in self.keys:
            for reading in readings(key):
                self.by_reading.setdefault(reading, key)

    def find(self, name: str) -> str | None:
        for candidate in candidate_names(name):
            if candidate in self.by_reading:
                return self.by_reading[candidate]
        for candidate in candidate_names(name):
            # "bolt" stands for the plainest bolt, "potion of shield of faith" for the weakest
            for key in self.keys:
                lowered = key.lower()
                if lowered.startswith(candidate + ' ') or lowered.startswith(candidate + ' ('):
                    return key
        return None


def singular(word: str) -> str:
    if re.search(r'(ss|us)$', word) or len(word) <= 3:
        return word
    if word.endswith('ies'):
        return word[:-3] + 'y'
    if word.endswith('axes'):
        return word[:-1]
    if re.search(r'(ches|shes|xes)$', word):
        return word[:-2]
    return word[:-1] if word.endswith('s') else word


def numbered_forms(name: str) -> list[str]:
    """The singular and plural of the word that names the item, before any "of"."""
    words = name.split(' ')
    of_index = words.index('of') if 'of' in words else len(words)
    if of_index == 0:
        return []
    noun = words[of_index - 1]
    forms = []
    for form in (singular(noun), noun + 's'):
        if form != noun:
            forms.append(' '.join(words[:of_index - 1] + [form] + words[of_index:]))
    return forms


def rewritten_forms(name: str) -> list[str]:
    """The ways PCGen may spell what a stat block calls `name`."""
    forms = []
    belt = re.match(r'^(.+?) (\+\d+) \(([^)]+)\)$', name)
    if belt:
        abilities = '/'.join(part.strip() for part in belt.group(3).split(','))
        forms.append(f'{belt.group(1)} ({abilities}) {belt.group(2)}')
    reordered = re.match(r'^(.+? of )(greater|lesser|mass|communal) (.+)$', name)
    if reordered:
        forms.append(f'{reordered.group(1)}{reordered.group(3)} ({reordered.group(2)})')
    rod = re.match(r'^(?:(lesser|greater) )?(.+?) metamagic rod$', name)
    if rod:
        forms.append(f'{rod.group(2)} {rod.group(1)} rod' if rod.group(1) else f'{rod.group(2)} rod')
    unit = re.match(r'^(dose|flask|vial|keg|pint|pound|bottle)s? of (.+)$', name)
    if unit:
        forms.append(f'{unit.group(1)} {unit.group(2)}')
    symbol = re.match(r'^((?:un)?holy symbol) of .+$', name)
    if symbol:
        forms.append(symbol.group(1))
    if re.search(r' \([^)]*\)$', name):
        forms.append(re.sub(r' \([^)]*\)$', '', name))
    if name.endswith(' armor') and ' of ' not in name:
        forms.append(name[:-len(' armor')])
    forms.append(re.sub(r'\bax\b', 'axe', name))
    return [form for form in forms if form != name]


def candidate_names(name: str) -> list[str]:
    candidates = [re.sub(r'\s+', ' ', name.strip().lower())]
    for rewrite in (rewritten_forms, numbered_forms):
        for candidate in list(candidates):
            candidates.extend(form for form in rewrite(candidate) if form not in candidates)
    return candidates


def resolve_class(entry: ClassLevels, classes: KeyIndex) -> tuple[str | None, str | None]:
    """The PCGen class key and, for a specialist wizard, the school."""
    if entry.name in WIZARD_SCHOOLS and classes.find('wizard'):
        return classes.find('wizard'), WIZARD_SCHOOLS[entry.name]
    elementalist = re.match(r'^(air|earth|fire|water) elementalist(?: wizard)?$', entry.name)
    if elementalist:
        return classes.find('wizard'), elementalist.group(1).title()
    thassilonian = re.match(r'^thassilonian (\w+)$', entry.name)
    if thassilonian and thassilonian.group(1) in THASSILONIAN_SINS:
        return classes.find('wizard'), THASSILONIAN_SINS[thassilonian.group(1)]
    if entry.name.endswith('kineticist'):
        return classes.by_reading.get('kineticist'), None
    key = classes.by_reading.get(entry.name)
    return key, None


def resolve_race(text: str, races: KeyIndex) -> tuple[str | None, str | None]:
    """The PCGen race key and the gender a stat block may put in front of the race."""
    name = re.sub(r'\s+', ' ', text.strip().lower())
    gender = None
    gender_match = re.match(r'^(male|female) (.+)$', name)
    if gender_match:
        gender, name = gender_match.group(1).title(), gender_match.group(2)
    if any(word in HUMAN_ETHNICITIES for word in name.split(' ')):
        return races.find('human'), gender
    without_parentheses = re.sub(r'\s*\([^)]*\)', '', name).strip()
    parenthesized = re.findall(r'\(([^)]*)\)', name)
    in_parentheses = [parenthesized[-1].split(' ')[-1]] if parenthesized else []
    for candidate in (name, without_parentheses, without_parentheses.split(' ')[-1], *in_parentheses):
        key = races.find(candidate) or family_member(candidate, races)
        if key:
            return key, gender
    # "human magus", "balor lord", "efreeti malik": a class, rank or template written after the race
    first_word = name.split(' ')[0]
    return races.by_reading.get(first_word) or family_member(first_word, races), gender


def race_of_subtype(subtype: str | None, races: KeyIndex) -> str | None:
    """The race a subtype names exactly: "(augmented human)", or a people of Golarion such as "(kuru)"."""
    words = (subtype or '').strip('() ').lower()
    augmented = re.match(r'^augmented (.+)$', words)
    if augmented:
        return races.by_reading.get(augmented.group(1))
    return races.find('human') if words in HUMAN_ETHNICITIES else None


def family_member(name: str, races: KeyIndex) -> str | None:
    """PCGen lists many monsters under their family: "Demon (Succubus)", "Ghoul (Ghast)", "Genie (Efreeti)"."""
    return next((key for key in races.keys if re.search(r'\(' + re.escape(name) + r'\)$', key.lower())), None)


# Fiends whose offspring the stat blocks name ("half-succubus"); PCGen has one Half-Fiend template
HALF_FIEND_PATTERN = re.compile(r'^half-(succubus|babau|incubus|glabrezu|vrock|hezrou|nalfeshnee|marilith|balor|devil|demon)$')


def resolve_template(name: str, templates: KeyIndex) -> str | None:
    """The PCGen template a stat block names, however Combat Manager spells it."""
    name = name.strip().lower()
    variant = re.match(r'^(.+)@(.+)$', name)
    forms = [f'{variant.group(2)} ({variant.group(1)})', f'{variant.group(1)} {variant.group(2)}'] if variant else [name]
    if HALF_FIEND_PATTERN.match(name):
        forms.append('half-fiend')
    forms.append(re.sub(r'-bound$', ' bound', re.sub(r'-devil-bound$', ' devil bound', name)))
    exact = next((templates.by_reading[form] for form in forms if form in templates.by_reading), None)
    return exact or next((key for key in map(templates.find, forms) if key), None)


def pick_modifier(modifier_keys: list[str], label: str) -> str | None:
    """The equipment modifier whose name, between its "~", is `label`."""
    for wanted in (label.lower(), MODIFIER_NAMES.get(label.lower())):
        for key in modifier_keys:
            parts = [part.strip().lower() for part in key.split('~')]
            if wanted and len(parts) > 1 and parts[1] == wanted:
                return key
    return None


MEMORIZE_DECLARATION = re.compile(r'^CLASS:([^\t.]+)[^\t]*\t.*\bMEMORIZE:(YES|NO)\b', re.M)


def limited_known_classes(class_files_text: str) -> frozenset[str]:
    """The casters PCGen holds to a number of known spells: those that cast without preparing (MEMORIZE:NO)."""
    casts_unprepared = {name: flag == 'NO' for name, flag in MEMORIZE_DECLARATION.findall(class_files_text)}
    return frozenset(name for name, unprepared in casts_unprepared.items() if unprepared)
