#!/usr/bin/env python3
"""Scrape d20pfsrd.com for Bestiary 5/6 creature data (feats, skills, languages, SLAs).

Extracts fields missing from pcfinder-csr: feats, skills, languages, spell-like
abilities, and special ability names.

Usage:
    # Build URL index + scrape all B5/B6 creatures:
    python3 scripts/scrape_d20pfsrd.py \
        --source ../pcfinder/apps/pcfinder-csr/static-data/creatures.json \
        --bestiary 5 6 \
        --output scripts/d20pfsrd_cache.json

    # Resume interrupted scraping (uses cached index):
    python3 scripts/scrape_d20pfsrd.py \
        --source ../pcfinder/apps/pcfinder-csr/static-data/creatures.json \
        --bestiary 5 6 \
        --output scripts/d20pfsrd_cache.json \
        --resume
"""

import argparse
import json
import os
import re
import sys
import time
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

# ─── Constants ───────────────────────────────────────────────────────────────

BASE_URL = "https://www.d20pfsrd.com"
LISTINGS_URL = f"{BASE_URL}/bestiary/monster-listings/"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
}

REQUEST_DELAY = 1.5  # seconds between requests

# d20pfsrd type page slugs
TYPE_SLUGS = {
    'aberration': 'aberrations',
    'animal': 'animals',
    'construct': 'constructs',
    'dragon': 'dragons',
    'fey': 'fey',
    'humanoid': 'humanoids',
    'magical beast': 'magical-beasts',
    'magical-beast': 'magical-beasts',
    'monstrous humanoid': 'monstrous-humanoids',
    'monstrous-humanoid': 'monstrous-humanoids',
    'ooze': 'oozes',
    'outsider': 'outsiders',
    'plant': 'plants',
    'undead': 'undead',
    'vermin': 'vermin',
}

# Creatures excluded from import (already in B5/B6 as companions/familiars/PC races)
B5_EXCLUDE = {
    'cameroceras', 'ceratosaurus', 'chalicotherium', 'digmaul', 'frog father',
    'goliath frog', 'kaprosuchus', 'megaprimatus', 'moa', 'narwhal',
    'plesiosaurus', 'polar bear', 'therizinosaurus', 'troodon', 'blue whale',
    'uintatherium', 'wolliped', 'chicken', 'flying fox', 'penguin', 'red panda',
    'seal', 'trilobite', 'clockwork familiar', 'liminal sprite', 'esipil', 'xiao',
    'aether wysp', 'air wysp', 'earth wysp', 'fire wysp', 'water wysp',
    'android', 'astomoi', 'caligni', 'deep one hybrid', 'ghoran',
    'orang-pendak', 'reptoid', 'shabti', 'skinwalker',
}

B6_EXCLUDE = {
    'amargasaurus', 'brontotherium', 'deinotherium', 'devil monkey',
    'dunkleosteus', 'elasmotherium', 'giganotosaurus', 'kentrosaurus',
    'mokele-mbembe', 'quetzalcoatlus', 'giant raven', 'titanoboa',
    'coral capuchin', 'mockingfey', 'monkey goblin',
}

# Manual name overrides for creatures whose d20pfsrd names differ significantly
NAME_OVERRIDES = {
    'giant belostomatid': 'belostomatid',
    'hivemind rat swarm': 'hivemind swarm',
    'hookfang worm': 'sea worm, hookfang',
    'makara vahana': 'vahana',
    'plagued beast, plagued horse': 'plagued steed',
    "siren's bed anemone": "sea anemone, siren\u00e2\u0080\u0099s bed",
    'empyreal lord, arshea': 'empyreal lord, spirit of abandon',
    'ant, megapon ant': 'ant, giant (megapon)',
}


# ─── HTTP Helpers ────────────────────────────────────────────────────────────

def create_session():
    """Create a requests session with retry support."""
    session = requests.Session()
    session.headers.update(HEADERS)
    adapter = requests.adapters.HTTPAdapter(max_retries=3)
    session.mount('https://', adapter)
    session.mount('http://', adapter)
    return session


def fetch_soup(url, session, delay=REQUEST_DELAY):
    """Fetch a URL and return BeautifulSoup, or None on error."""
    time.sleep(delay)
    try:
        resp = session.get(url, timeout=30)
        if resp.status_code == 200:
            return BeautifulSoup(resp.text, 'lxml')
        else:
            print(f"  HTTP {resp.status_code}: {url}", file=sys.stderr)
            return None
    except requests.RequestException as e:
        print(f"  Error: {url}: {e}", file=sys.stderr)
        return None


# ─── Phase 1: Build URL Index ───────────────────────────────────────────────

def get_type_page_urls():
    """Return the URLs for each creature type listing page."""
    urls = {}
    for ctype, slug in TYPE_SLUGS.items():
        url = f"{LISTINGS_URL}{slug}/"
        urls[slug] = url
    # Deduplicate (magical beast / magical-beast both map to same slug)
    return list(set(urls.values()))


def extract_creature_links(soup, base_url):
    """Extract all creature name→URL pairs from a listing page.

    Returns (creatures_dict, sub_listing_urls).
    Sub-listing URLs are pages that contain nested creature links.
    """
    creatures = {}
    sub_listings = []
    content = soup.find('div', class_='article-content') or soup.find('article') or soup

    base_depth = len(base_url.rstrip('/').split('/'))

    for a_tag in content.find_all('a', href=True):
        href = a_tag['href']
        text = a_tag.get_text(strip=True)

        if not text or len(text) < 2:
            continue

        full_url = urljoin(BASE_URL, href).rstrip('/') + '/'
        if '/bestiary/monster-listings/' not in full_url:
            continue
        if full_url.rstrip('/') == base_url.rstrip('/'):
            continue

        name_lower = text.strip().lower()
        if name_lower in ('back to top', 'home', 'index', 'next', 'previous'):
            continue

        # Check if this link has a nested <ul> (indicating it's a sub-listing parent)
        parent_li = a_tag.find_parent('li')
        has_nested_list = parent_li and parent_li.find('ul') if parent_li else False

        url_depth = len(full_url.rstrip('/').split('/'))

        # If it's at the same depth as child links AND has nested children,
        # it's likely a sub-listing page
        if has_nested_list and url_depth == base_depth + 1:
            sub_listings.append(full_url)

        creatures[name_lower] = {
            'name': text.strip(),
            'url': full_url,
        }

    return creatures, sub_listings


def build_url_index(session, index_path=None):
    """Build complete name→URL index by crawling type listing pages.

    Two-pass approach:
    1. Crawl top-level type pages
    2. Crawl sub-listing pages (e.g., /outsiders/empyreal-lord/) for
       creatures not linked from the type page
    """
    # Check for cached index
    if index_path and os.path.exists(index_path):
        print(f"Loading cached URL index from {index_path}")
        with open(index_path, 'r') as f:
            return json.load(f)

    print("Building URL index from d20pfsrd.com type listing pages...")
    type_urls = get_type_page_urls()
    print(f"  {len(type_urls)} type pages to crawl")

    all_creatures = {}
    all_sub_listings = set()

    # Pass 1: crawl type pages
    for i, url in enumerate(type_urls):
        slug = url.rstrip('/').split('/')[-1]
        print(f"  [{i+1}/{len(type_urls)}] {slug}...")
        soup = fetch_soup(url, session)
        if soup:
            creatures, sub_listings = extract_creature_links(soup, url)
            all_creatures.update(creatures)
            all_sub_listings.update(sub_listings)
            print(f"    Found {len(creatures)} links, {len(sub_listings)} sub-listings")

    # Pass 2: crawl sub-listing pages for additional creatures
    if all_sub_listings:
        print(f"\n  Crawling {len(all_sub_listings)} sub-listing pages...")
        for i, url in enumerate(sorted(all_sub_listings)):
            slug = url.rstrip('/').split('/')[-1]
            print(f"  [{i+1}/{len(all_sub_listings)}] {slug}...")
            soup = fetch_soup(url, session)
            if soup:
                creatures, _ = extract_creature_links(soup, url)
                new_count = sum(1 for k in creatures if k not in all_creatures)
                all_creatures.update(creatures)
                if new_count > 0:
                    print(f"    +{new_count} new creatures")

    print(f"  Total index: {len(all_creatures)} creatures")

    # Save index
    if index_path:
        with open(index_path, 'w') as f:
            json.dump(all_creatures, f, indent=2)
        print(f"  Saved index to {index_path}")

    return all_creatures


# ─── Phase 2: Name Matching ─────────────────────────────────────────────────

def slugify(name):
    """Convert name to URL-friendly slug."""
    s = name.lower().strip()
    s = re.sub(r'[^a-z0-9\s-]', '', s)
    s = re.sub(r'[\s]+', '-', s)
    s = re.sub(r'-+', '-', s)
    return s.strip('-')


def name_variants(name):
    """Generate matching variants for a creature name.

    Returns list ordered from most specific to least specific,
    so more precise matches are tried first.
    """
    n = name.lower().strip()
    # Ordered list: most specific first
    ordered = [n]
    more = []

    if ', ' in n:
        parts = n.split(', ', 1)
        ordered.append(f"{parts[1]} {parts[0]}")  # Reversed: "crypt dragon"
        more.append(parts[1])  # Second part: "crypt"
        more.append(parts[0])  # First part: "dragon" (least specific)

    ordered.append(slugify(n))

    # Remove parenthetical
    cleaned = re.sub(r'\s*\(.*?\)', '', n).strip()
    if cleaned != n:
        ordered.append(cleaned)

    if n.startswith('the '):
        ordered.append(n[4:])

    # Add less-specific variants at the end
    ordered.extend(more)

    # Deduplicate while preserving order
    seen = set()
    result = []
    for v in ordered:
        if v not in seen:
            seen.add(v)
            result.append(v)
    return result


def normalize_index_key(key):
    """Normalize an index key for matching (handle encoding issues)."""
    # Fix UTF-8 encoding issues (e.g., â\x80\x99 → ')
    key = key.replace('\u2019', "'").replace('\u2018', "'")
    key = key.replace('\u201c', '"').replace('\u201d', '"')
    key = key.replace('\u2014', '-').replace('\u2013', '-')
    return key


def match_creatures_to_urls(creatures, url_index):
    """Match pcfinder creature names to d20pfsrd URLs.

    Returns dict of pcfinder_name → {url, d20_name}.
    """
    matched = {}
    unmatched = []

    # Build normalized and slug-based lookups for the index
    norm_index = {}
    slug_index = {}
    url_slug_index = {}  # URL last component → entry
    for key, entry in url_index.items():
        norm_key = normalize_index_key(key)
        norm_index[norm_key] = entry
        slug_index[slugify(norm_key)] = entry
        # Store by URL slug (last path component)
        url_slug = entry['url'].rstrip('/').split('/')[-1]
        slug_index[url_slug] = entry
        url_slug_index[url_slug] = entry

    for creature in creatures:
        name = creature['name']
        name_lower = name.lower().strip()

        found = None

        # Check manual overrides first
        if name_lower in NAME_OVERRIDES:
            override = NAME_OVERRIDES[name_lower].lower()
            if override in url_index:
                found = url_index[override]
            elif override in norm_index:
                found = norm_index[override]

        # Try variants
        if not found:
            variants = name_variants(name)

            # Exact match against index keys (full name and reversed)
            # Only try the first few specific variants to avoid matching
            # generic category names like "empyreal lord" or "dragon"
            specific_variants = [v for v in variants if ', ' not in name.lower()
                                 or v != name.lower().split(', ')[0]]
            for v in specific_variants:
                if v in url_index:
                    found = url_index[v]
                    break
                if v in norm_index:
                    found = norm_index[v]
                    break

            # Slug match
            if not found:
                for v in specific_variants:
                    s = slugify(v)
                    if s in slug_index:
                        found = slug_index[s]
                        break

            # URL-slug match: check if creature name appears as a word segment
            # in a d20pfsrd URL slug (e.g., "ragathiel" in "empyreal-lord-ragathiel")
            if not found:
                for v in variants:
                    v_slug = slugify(v)
                    if len(v_slug) < 5:
                        continue
                    for url_s, entry in url_slug_index.items():
                        # Must match as a complete hyphen-separated segment
                        # "ragathiel" matches "empyreal-lord-ragathiel" (ends with it)
                        # but "ant" does NOT match "giant-ant" (too short/generic)
                        if url_s.endswith('-' + v_slug) or url_s.startswith(v_slug + '-'):
                            found = entry
                            break
                    if found:
                        break

            # Fallback: try all variants including generic first-part
            if not found:
                for v in variants:
                    if v in url_index:
                        found = url_index[v]
                        break
                    if v in norm_index:
                        found = norm_index[v]
                        break

            # Cross-variant match
            if not found:
                for v in variants:
                    if len(v) < 4:
                        continue
                    for key, entry in norm_index.items():
                        if v == key or slugify(v) == slugify(key):
                            found = entry
                            break
                        d20_variants = name_variants(entry['name'])
                        if v in d20_variants:
                            found = entry
                            break
                    if found:
                        break

        if found:
            matched[name] = {
                'url': found['url'],
                'd20_name': found['name'],
            }
        else:
            unmatched.append(name)

    return matched, unmatched


# ─── Phase 3: Page Parsing ──────────────────────────────────────────────────

def split_stat_sections(soup):
    """Split the stat block into named sections by divider headers.

    Handles two d20pfsrd formats:
    1. Direct content: section dividers are direct children of article-content
    2. Statblock div: everything is inside <div class="statblock">

    Returns dict: section_name → list of BeautifulSoup elements.
    """
    sections = {}
    current_section = 'header'
    sections[current_section] = []

    content = soup.find('div', class_='article-content') or soup.find('article') or soup

    # Check for statblock wrapper div
    statblock = content.find('div', class_='statblock')
    container = statblock if statblock else content

    section_names = {'DEFENSE', 'OFFENSE', 'STATISTICS', 'SPECIAL ABILITIES', 'ECOLOGY'}

    for el in container.children:
        if not hasattr(el, 'get_text'):
            continue

        text = el.get_text(strip=True).upper()

        # Check if this element is a section divider
        is_divider = False
        if text in section_names:
            is_divider = True
        elif hasattr(el, 'get') and 'divider' in (el.get('class', []) or []):
            if text in section_names:
                is_divider = True

        if is_divider:
            current_section = text
            sections[current_section] = []
            continue

        if current_section not in sections:
            sections[current_section] = []
        sections[current_section].append(el)

    return sections


def extract_labeled_text(elements, label):
    """Extract text following a bold label in a list of elements.

    Looks for <b>Label</b> and returns the text content that follows it,
    stopping at the next <b> tag (next field label) within the same parent.
    """
    for el in elements:
        if not hasattr(el, 'find_all'):
            continue
        for b_tag in el.find_all('b'):
            b_text = b_tag.get_text(strip=True)
            if b_text.lower().rstrip(':').strip() == label.lower():
                # Get text after this <b> tag until the next <b> tag
                parts = []
                for sibling in b_tag.next_siblings:
                    # Stop at the next <b> tag (next field label)
                    if hasattr(sibling, 'name') and sibling.name == 'b':
                        break
                    if hasattr(sibling, 'get_text'):
                        parts.append(sibling.get_text())
                    elif isinstance(sibling, str):
                        parts.append(sibling)
                text = ''.join(parts).strip()
                # Strip leading semicolons or colons
                text = text.lstrip(':;').strip()
                return text
    return ''


def parse_feats(elements):
    """Extract feat names from STATISTICS section."""
    text = extract_labeled_text(elements, 'Feats')
    if not text:
        return []

    feats = []
    # Split by comma, but respect parentheses
    # "Power Attack, Weapon Focus (claw), Improved InitiativeB"
    parts = re.split(r',\s*(?![^(]*\))', text)
    for part in parts:
        feat = part.strip()
        if not feat:
            continue
        # Remove bonus feat markers (superscript B)
        feat = re.sub(r'\s*[Bb]$', '', feat)
        # Remove trailing footnote markers
        feat = re.sub(r'\s*\d+$', '', feat)
        if feat:
            feats.append(feat)

    return feats


def parse_skills(elements):
    """Extract skill names and bonuses from STATISTICS section.

    Returns dict: skill_name → total_bonus (int).
    """
    text = extract_labeled_text(elements, 'Skills')
    if not text:
        return {}

    # Remove racial modifiers section
    text = re.split(r';\s*Racial Modifier', text, flags=re.IGNORECASE)[0]

    skills = {}
    # Pattern: "SkillName +N" or "SkillName (subskill) +N" or "SkillName -N"
    # Also handle conditional modifiers in parens: "Stealth +17 (+21 in forests)"
    # Split by comma, respecting parentheses
    parts = re.split(r',\s*(?![^(]*\))', text)
    for part in parts:
        part = part.strip()
        if not part:
            continue
        # Match: Name [+/-]N
        m = re.match(r'^(.+?)\s+([+-]\d+)', part)
        if m:
            skill_name = m.group(1).strip()
            bonus = int(m.group(2))
            # Clean up skill name
            skill_name = skill_name.strip('*')
            if skill_name and len(skill_name) > 1:
                skills[skill_name] = bonus

    return skills


def parse_languages(elements):
    """Extract language names from STATISTICS section."""
    text = extract_labeled_text(elements, 'Languages')
    if not text:
        return []

    # Remove parenthetical notes like "(can't speak)"
    # But keep language-qualifying parens like "Aklo (can't speak)" — actually remove those too
    # We just want the language names

    languages = []
    # Split by semicolon first (separates languages from special communication)
    main_part = text.split(';')[0]

    for part in re.split(r',\s*', main_part):
        part = part.strip()
        if not part:
            continue
        # Remove parenthetical notes
        lang = re.sub(r'\s*\(.*?\)', '', part).strip()
        # Skip special entries
        if lang.lower() in ('telepathy', 'truespeech', ''):
            continue
        if re.match(r'^\d', lang):  # Skip "100 ft." type entries
            continue
        if lang:
            languages.append(lang)

    return languages


def parse_special_qualities(elements):
    """Extract special quality names from STATISTICS section."""
    text = extract_labeled_text(elements, 'SQ')
    if not text:
        return []

    # Split by comma, respecting parentheses
    qualities = []
    parts = re.split(r',\s*(?![^(]*\))', text)
    for part in parts:
        part = part.strip()
        if part:
            qualities.append(part)
    return qualities


def parse_slas(elements):
    """Extract Spell-Like Abilities from OFFENSE section.

    d20pfsrd structure: the SLA header (CL, concentration) is inside a big <p>
    with other offense fields. The frequency lines (At will, 3/day, etc.) are
    all in a single <p style="margin-left: 20px;"> separated by <br/> tags.

    Returns dict with:
        'cl': caster level (int)
        'concentration': concentration bonus (int)
        'entries': dict of frequency → list of {spell, dc}
    """
    result = {'cl': 0, 'concentration': 0, 'entries': {}}

    # Find CL and concentration from the SLA header text
    for el in elements:
        if not hasattr(el, 'get_text'):
            continue
        text = el.get_text()
        if 'spell-like abilities' in text.lower():
            m = re.search(r'CL\s+(\d+)', text)
            if m:
                result['cl'] = int(m.group(1))
            m = re.search(r'concentration\s*([+-]\d+)', text)
            if m:
                result['concentration'] = int(m.group(1))
            break

    # Find the margin-left <p> element(s) containing frequency lines
    for el in elements:
        if not hasattr(el, 'get'):
            continue
        style = el.get('style', '') or ''
        if 'margin-left' not in style:
            continue

        # Parse frequencies by iterating children of this single element.
        # Structure: <b>frequency</b> em-dash <a>spell</a> (DC N), ... <br/> <b>freq2</b> ...
        current_freq = None
        current_spells = []

        for child in el.children:
            if hasattr(child, 'name') and child.name == 'b':
                # Save previous frequency group
                if current_freq and current_spells:
                    result['entries'][current_freq] = current_spells

                # Detect new frequency
                freq_text = child.get_text(strip=True).lower()
                current_spells = []

                if freq_text == 'constant':
                    current_freq = 'constant'
                elif 'at will' in freq_text:
                    current_freq = 'at_will'
                else:
                    m = re.match(r'(\d+)/(\w+)', freq_text)
                    if m:
                        current_freq = f'{m.group(1)}/{m.group(2)}'
                    else:
                        current_freq = freq_text

            elif hasattr(child, 'name') and child.name == 'a':
                # Only use spell links (class="spell" or italic style)
                classes = child.get('class', []) or []
                style = child.get('style', '') or ''
                href = child.get('href', '') or ''
                is_spell = ('spell' in classes
                            or 'italic' in style
                            or '/magic/' in href
                            or '/spells/' in href)
                if not is_spell:
                    continue
                spell_name = child.get_text(strip=True)
                if not spell_name or len(spell_name) < 2:
                    continue
                # Fix encoding issues (curly apostrophe)
                spell_name = spell_name.replace('\u2019', "'").replace('\u2018', "'")
                spell_name = spell_name.replace('\u00e2\u0080\u0099', "'")
                # Look for DC in the following text sibling
                dc = None
                next_sib = child.next_sibling
                if isinstance(next_sib, str):
                    m = re.search(r'\(DC\s+(\d+)\)', next_sib)
                    if m:
                        dc = int(m.group(1))
                current_spells.append({'spell': spell_name, 'dc': dc})

        # Save last frequency group
        if current_freq and current_spells:
            result['entries'][current_freq] = current_spells

    return result


def parse_spell_list(text, element=None):
    """Parse a comma-separated list of spells with optional DCs.

    Returns list of {spell: name, dc: int_or_none}.
    """
    spells = []

    # Try to use <a> tags for spell names if element is available
    if element and hasattr(element, 'find_all'):
        spell_links = element.find_all('a')
        if spell_links:
            for a_tag in spell_links:
                href = a_tag.get('href', '')
                # Only consider spell/magic links
                if '/magic/' in href or '/spells/' in href or 'class' in str(a_tag.get('class', [])) and 'spell' in str(a_tag.get('class', [])):
                    spell_name = a_tag.get_text(strip=True)
                    if spell_name:
                        # Look for DC in the text following the link
                        dc = None
                        next_text = ''
                        for sibling in a_tag.next_siblings:
                            if isinstance(sibling, str):
                                next_text += sibling
                            else:
                                break
                        m = re.search(r'\(DC\s+(\d+)\)', next_text)
                        if m:
                            dc = int(m.group(1))
                        spells.append({'spell': spell_name, 'dc': dc})
            if spells:
                return spells

    # Fallback: parse from text
    # Split by comma, respecting parentheses
    parts = re.split(r',\s*(?![^(]*\))', text)
    for part in parts:
        part = part.strip()
        if not part:
            continue
        # Extract DC
        dc = None
        m = re.search(r'\(DC\s+(\d+)\)', part)
        if m:
            dc = int(m.group(1))
        # Extract spell name (remove DC and other parens)
        spell_name = re.sub(r'\s*\(DC\s+\d+\)', '', part)
        spell_name = re.sub(r'\s*\(.*?\)', '', spell_name).strip()
        if spell_name and len(spell_name) > 1:
            spells.append({'spell': spell_name, 'dc': dc})

    return spells


def parse_special_abilities(elements):
    """Extract special ability names from SPECIAL ABILITIES section.

    Returns list of ability names (without (Ex)/(Su)/(Sp) suffixes).
    """
    abilities = []

    for el in elements:
        if not hasattr(el, 'name'):
            continue

        # Special abilities are typically in <h4> headings or <b> tags
        if el.name in ('h4', 'h3', 'h5'):
            text = el.get_text(strip=True)
            # Remove (Ex), (Su), (Sp) suffixes
            name = re.sub(r'\s*\((Ex|Su|Sp)\)\s*$', '', text, flags=re.IGNORECASE).strip()
            if name:
                abilities.append(name)
        elif el.name == 'p':
            # Some abilities use bold text in a paragraph
            b_tag = el.find('b')
            if b_tag:
                text = b_tag.get_text(strip=True)
                # Check if this looks like an ability name (short, followed by type marker)
                full_text = el.get_text(strip=True)
                if re.match(r'^.+?\s*\((Ex|Su|Sp)\)', full_text, re.IGNORECASE):
                    name = re.sub(r'\s*\((Ex|Su|Sp)\).*$', '', text, flags=re.IGNORECASE).strip()
                    if name and len(name) < 60:
                        abilities.append(name)

    return abilities


def parse_defensive_abilities(elements):
    """Extract defensive abilities from DEFENSE section."""
    text = extract_labeled_text(elements, 'Defensive Abilities')
    if not text:
        return []
    parts = re.split(r',\s*(?![^(]*\))', text)
    return [p.strip() for p in parts if p.strip()]


def parse_weaknesses(elements):
    """Extract weaknesses from DEFENSE section."""
    text = extract_labeled_text(elements, 'Weaknesses')
    if not text:
        text = extract_labeled_text(elements, 'Weakness')
    if not text:
        return []
    parts = re.split(r',\s*(?![^(]*\))', text)
    return [p.strip() for p in parts if p.strip()]


def parse_creature_page(soup):
    """Parse a creature page and extract all relevant data.

    Returns dict with feats, skills, languages, slas, special_abilities, etc.
    """
    sections = split_stat_sections(soup)

    data = {}

    # STATISTICS section
    stats_els = sections.get('STATISTICS', [])
    data['feats'] = parse_feats(stats_els)
    data['skills'] = parse_skills(stats_els)
    data['languages'] = parse_languages(stats_els)
    data['special_qualities'] = parse_special_qualities(stats_els)

    # OFFENSE section
    offense_els = sections.get('OFFENSE', [])
    data['slas'] = parse_slas(offense_els)

    # DEFENSE section
    defense_els = sections.get('DEFENSE', [])
    data['defensive_abilities'] = parse_defensive_abilities(defense_els)
    data['weaknesses'] = parse_weaknesses(defense_els)

    # SPECIAL ABILITIES section
    sa_els = sections.get('SPECIAL ABILITIES', [])
    data['special_abilities'] = parse_special_abilities(sa_els)

    return data


# ─── Phase 4: Scraping ──────────────────────────────────────────────────────

def scrape_creature(url, session):
    """Scrape a single creature page and return parsed data."""
    soup = fetch_soup(url, session)
    if not soup:
        return None

    try:
        return parse_creature_page(soup)
    except Exception as e:
        print(f"  Parse error for {url}: {e}", file=sys.stderr)
        return None


# ─── Main Logic ──────────────────────────────────────────────────────────────

def load_pcfinder_creatures(source_path, bestiary_nums):
    """Load B5/B6 creatures from pcfinder creatures.json."""
    print(f"Loading creatures from {source_path}...")
    with open(source_path, 'r') as f:
        all_creatures = json.load(f)

    result = {}
    for bn in bestiary_nums:
        prefix = f"Bestiary {bn}"
        exclude = B5_EXCLUDE if bn == 5 else B6_EXCLUDE
        creatures = []
        for c in all_creatures:
            source = c.get('source', '')
            if source.startswith(prefix) or source.startswith(f'PFRPG {prefix}'):
                name_lower = c['name'].lower().strip()
                if name_lower not in exclude:
                    creatures.append(c)
        result[bn] = creatures
        print(f"  Bestiary {bn}: {len(creatures)} creatures")

    return result


def save_cache(cache, output_path):
    """Save the scraped data cache to disk."""
    with open(output_path, 'w') as f:
        json.dump(cache, f, indent=2, ensure_ascii=False)
    print(f"  Saved cache ({len(cache)} entries) to {output_path}")


def load_cache(output_path):
    """Load existing cache from disk."""
    if os.path.exists(output_path):
        with open(output_path, 'r') as f:
            return json.load(f)
    return {}


def main():
    parser = argparse.ArgumentParser(description='Scrape d20pfsrd.com for B5/B6 creature data')
    parser.add_argument('--source', required=True, help='Path to pcfinder creatures.json')
    parser.add_argument('--bestiary', type=int, nargs='+', default=[5, 6], choices=[5, 6])
    parser.add_argument('--output', default='scripts/d20pfsrd_cache.json', help='Output cache file')
    parser.add_argument('--index', default='scripts/d20pfsrd_index.json', help='URL index cache file')
    parser.add_argument('--resume', action='store_true', help='Resume from existing cache')
    parser.add_argument('--limit', type=int, default=0, help='Limit number of creatures to scrape (0=all)')
    parser.add_argument('--test', type=str, help='Test scraping a single creature by name')
    args = parser.parse_args()

    session = create_session()

    # Test mode: scrape a single creature
    if args.test:
        print(f"Test mode: scraping '{args.test}'")
        url_index = build_url_index(session, args.index)
        variants = name_variants(args.test)
        found = None
        for v in variants:
            if v in url_index:
                found = url_index[v]
                break
        if not found:
            slug = slugify(args.test)
            for key, entry in url_index.items():
                if slugify(key) == slug:
                    found = entry
                    break
        if found:
            print(f"  Found URL: {found['url']}")
            data = scrape_creature(found['url'], session)
            if data:
                print(json.dumps(data, indent=2, ensure_ascii=False))
            else:
                print("  Failed to scrape/parse page")
        else:
            print(f"  Could not find URL for '{args.test}'")
            # Show similar names
            close = [k for k in url_index.keys() if args.test.lower()[:4] in k]
            if close:
                print(f"  Similar: {close[:10]}")
        return

    # Load pcfinder creatures
    bestiary_creatures = load_pcfinder_creatures(args.source, args.bestiary)
    all_creatures = []
    for bn in args.bestiary:
        all_creatures.extend(bestiary_creatures.get(bn, []))
    print(f"Total creatures to scrape: {len(all_creatures)}")

    # Build URL index
    url_index = build_url_index(session, args.index)

    # Match creatures to URLs
    matched, unmatched = match_creatures_to_urls(all_creatures, url_index)
    print(f"\nMatching results:")
    print(f"  Matched: {len(matched)}")
    print(f"  Unmatched: {len(unmatched)}")
    if unmatched:
        print(f"  Unmatched names: {unmatched[:20]}")
        if len(unmatched) > 20:
            print(f"  ... and {len(unmatched) - 20} more")

    # Load existing cache for resume
    cache = load_cache(args.output) if args.resume else {}
    already_scraped = set(cache.keys())

    # Scrape matched creatures
    to_scrape = {name: info for name, info in matched.items()
                 if name not in already_scraped}
    print(f"\nCreatures to scrape: {len(to_scrape)} (skipping {len(already_scraped)} already cached)")

    if args.limit > 0:
        items = list(to_scrape.items())[:args.limit]
        to_scrape = dict(items)
        print(f"  Limited to {len(to_scrape)} creatures")

    scraped = 0
    errors = 0
    for i, (name, info) in enumerate(to_scrape.items()):
        print(f"  [{i+1}/{len(to_scrape)}] {name}...")
        data = scrape_creature(info['url'], session)
        if data:
            data['url'] = info['url']
            data['d20_name'] = info.get('d20_name', name)
            cache[name] = data
            scraped += 1
        else:
            errors += 1

        # Save progress every 20 creatures
        if (i + 1) % 20 == 0:
            save_cache(cache, args.output)

    # Final save
    save_cache(cache, args.output)

    print(f"\nDone! Scraped {scraped} creatures ({errors} errors)")
    print(f"Cache has {len(cache)} total entries")

    # Report on data coverage
    has_feats = sum(1 for d in cache.values() if d.get('feats'))
    has_skills = sum(1 for d in cache.values() if d.get('skills'))
    has_langs = sum(1 for d in cache.values() if d.get('languages'))
    has_slas = sum(1 for d in cache.values() if d.get('slas', {}).get('entries'))
    print(f"\nData coverage:")
    print(f"  With feats: {has_feats}/{len(cache)}")
    print(f"  With skills: {has_skills}/{len(cache)}")
    print(f"  With languages: {has_langs}/{len(cache)}")
    print(f"  With SLAs: {has_slas}/{len(cache)}")


if __name__ == '__main__':
    main()
