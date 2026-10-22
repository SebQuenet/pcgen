"""Buy and wear a stat block's gear on a PCGen character.

The slot rules come from the Zirnakaynin NPC build
(Pierrebougie/scenarios/2026-09-19-zirkayanin/module/npcs/tools/build_npcs.py).
"""

from __future__ import annotations

import re

import catalog
import pcgen_client as pc
from statblock import Gear, GearItem, title_case

# A worn item goes to the slot its name announces. First match wins.
SLOT_KEYWORDS = [
    ('headband of', 'Headband'), ('circlet', 'Head'), ('hat of', 'Head'), ('helm', 'Head'), ('crown', 'Head'),
    ('mask', 'Head'), ('goggles', 'Eyes'), ('lenses', 'Eyes'), ('eyes of', 'Eyes'), ('amulet', 'Neck'),
    ('necklace', 'Neck'), ('brooch', 'Neck'), ('periapt', 'Neck'), ('torc', 'Neck'), ('scarab', 'Neck'),
    ('cloak', 'Shoulders'), ('cape', 'Shoulders'), ('mantle', 'Shoulders'), ('vest', 'Chest'), ('shirt of', 'Chest'),
    ('robe', 'Body'), ('bracers', 'Wrist'), ('bracelet', 'Wrist'), ('gauntlets', 'Hands'), ('gloves', 'Hands'),
    ('ring', 'Ring'), ('belt of', 'Belt'), ('girdle', 'Belt'), ('sash', 'Belt'), ('boots', 'Feet'),
    ('slippers', 'Feet'), ('shoes', 'Feet'), ('shield', 'Shield'), ('buckler', 'Shield'),
]
ARMOR_KEYWORDS = ('shirt', 'mail', 'plate', 'armor', 'leather', 'hide', 'breastplate', 'scale', 'lamellar', 'padded',
                  'haramaki', 'kikko', 'brigandine', 'splint', 'banded')
CARRIED_SLOT = 'Carried'
OUTFIT_SLOT = 'Body'
BOTH_HANDS_SLOT = 'Both Hands'


def slot_for(item_name: str) -> str | None:
    name = item_name.lower()
    for keyword, slot in SLOT_KEYWORDS:
        if re.search(rf'\b{re.escape(keyword)}', name):
            return slot
    if any(keyword in name for keyword in ARMOR_KEYWORDS):
        return 'Armor'
    return None


def inventory_key(wanted: str, inventory: list[str]) -> str:
    """The inventory's name for an item: one resized for a Small character gains a prefix."""
    if wanted in inventory:
        return wanted
    return next((key for key in inventory if key.endswith(wanted) or key.startswith(wanted)), wanted)


BOW_STRENGTH_MODIFIER = 'Special Quality ~ Composite Bow Strength Rating'


def modifier_stages(item: GearItem) -> list[list[str]]:
    """The modifiers in the order PCGen takes them: a special ability is offered only once the item is enhanced."""
    # a holy symbol's material is one of its variants, which buy() picks when PCGen refuses the bare symbol
    first = [item.material] if item.material and 'symbol' not in item.name.lower() else []
    if item.enhancement:
        first.append(f'+{item.enhancement}')
    elif item.is_masterwork:
        first.append('masterwork')
    return [stage for stage in (first, list(item.specials)) if stage]


def display_name(item: GearItem) -> str:
    words = ([f'+{item.enhancement}'] if item.enhancement else ['Masterwork'] if item.is_masterwork else [])
    words += list(item.specials) + ([item.material] if item.material else []) + [item.name]
    return title_case(' '.join(words))


def purchase_key(character_id: str, item: GearItem, base_key: str, report: dict) -> str:
    """The key to buy: the catalogue's, or a custom item's when the gear is magic or masterwork."""
    stages = modifier_stages(item)
    # a rating of +0 leaves the bow its base name, which buys the bare bow: buy_and_wear picks the catalogue's +0 bow instead
    strength_rating = item.strength_rating or None
    has_strength_rating = strength_rating is not None
    if not has_strength_rating and (not stages or (stages == [['masterwork']] and 'masterwork' in base_key.lower())):
        return base_key
    # The stages only find the modifiers' keys: the item bought is customised once from the catalogue's,
    # since a saved character names its custom item's base, and a custom base is gone when it is reopened
    key, chosen = base_key, []
    for position, labels in enumerate(stages):
        offered = [entry['key'] for entry in pc.call('list_equipment_modifiers', character_id=character_id, equipment_key=key)]
        modifiers = []
        for label in labels:
            modifier = catalog.pick_modifier(offered, label)
            if modifier:
                modifiers.append(modifier)
            else:
                report['missing_modifiers'].append(f'{label} ({item.name})')
        chosen += modifiers
        if modifiers and position < len(stages) - 1:
            key = pc.call('customize_equipment', character_id=character_id, equipment_key=key, modifier_keys=modifiers)['name']
    choices = {}
    if has_strength_rating:
        # a composite bow is sold only with its strength rating, chosen as "+3"
        chosen.append(BOW_STRENGTH_MODIFIER)
        choices[BOW_STRENGTH_MODIFIER] = [f'{strength_rating:+d}']
    if not chosen:
        return base_key
    # a name that says nothing more than the item's ("Composite Longbow") would make a purchase find the bare item
    name = display_name(item)
    custom_name = {'custom_name': name} if name.lower() != item.name.lower() else {}
    return pc.call('customize_equipment', character_id=character_id, equipment_key=base_key, modifier_keys=chosen,
                   choices=choices, **custom_name)['name']


def required_modifiers(offered: list[str], key: str, item: GearItem) -> list[str]:
    """What an item PCGen will not sell bare needs: one of its own variants, else a material; a leaf symbol needs both."""
    # an unholy symbol is offered the holy symbol's variants, "Holy Symbol (Wooden)": the last word is shared
    last_word = key.split(' ')[-1]
    variants = [modifier for modifier in offered if re.match(rf'^.*\b{re.escape(last_word)} \(', modifier)]
    named_variant = next((modifier for modifier in variants if item.material and f'({item.material})' in modifier.lower()), None)
    named = catalog.pick_modifier(offered, item.material) if item.material else None
    material = next((modifier for modifier in offered if modifier.startswith('Material ~')), None)
    if named_variant:
        return [named_variant]
    if variants:
        plainest = next((modifier for modifier in variants if '(wooden)' in modifier.lower()), variants[0])
        return [plainest] + ([named] if named else [])
    return [named or material] if named or material else []


def buy(character_id: str, purchases: list[tuple[GearItem, str, str]], report: dict) -> list[tuple[GearItem, str, str]]:
    """Buy everything; an item PCGen sells only customised (a holy symbol, a crown) is customised bare and bought again."""
    bought = pc.call('batch_buy_equipment', character_id=character_id, items=[
        {'equipment_key': key, 'quantity': item.quantity, 'free': True} for item, key, _ in purchases])
    errors = bought.get('errors', [])
    if not errors:
        return purchases
    # the errors name items in the interface language, so what failed is read from the inventory
    inventory = [entry['key'] for entry in pc.call('get_inventory', character_id=character_id)['items']]
    settled = []
    for item, key, base_key in purchases:
        if inventory_key(key, inventory) in inventory:
            settled.append((item, key, base_key))
            continue
        failure = f'Buy failed: {key}'
        try:
            offered = [entry['key'] for entry in pc.call('list_equipment_modifiers', character_id=character_id, equipment_key=key)]
            modifiers = required_modifiers(offered, key, item)
            # the variant names the item itself ("Holy Symbol (Wooden)"); a custom name would shadow the base item
            custom_key = pc.call('customize_equipment', character_id=character_id, equipment_key=key,
                                 modifier_keys=modifiers)['name']
            again = pc.call('batch_buy_equipment', character_id=character_id,
                            items=[{'equipment_key': custom_key, 'quantity': item.quantity, 'free': True}])
        except pc.PcgenError as error:
            report['refused'].append(f'{failure} ; {error.message[:160]}')
            continue
        if again.get('errors'):
            report['refused'].extend(again['errors'])
        else:
            settled.append((item, custom_key, base_key))
    return settled


def sellable_base(base_key: str | None, item: GearItem, items_index: catalog.KeyIndex) -> str | None:
    """A composite bow is sold only with its strength rating: one with none or +0 is the catalogue's "+0" bow."""
    plus_zero = f'{base_key[:-1]} +0)' if base_key and base_key.endswith(')') else None
    if plus_zero in items_index.keys and not item.strength_rating:
        return plus_zero
    return base_key


def buy_and_wear(character_id: str, gear: Gear, items_index: catalog.KeyIndex, melee_text: str, report: dict) -> None:
    pc.call('set_funds', character_id=character_id, amount=gear.gold)
    purchases = []
    for item in gear.items:
        base_key = sellable_base(items_index.find(item.name), item, items_index)
        if not base_key:
            report['missing_items'].append(item.name)
            continue
        try:
            purchases.append((item, purchase_key(character_id, item, base_key, report), base_key))
        except pc.PcgenError as error:
            report['missing_modifiers'].append(f'{item.name}: {error.message[:200]}')
    purchases = buy(character_id, purchases, report) if purchases else []

    inventory = [entry['key'] for entry in pc.call('get_inventory', character_id=character_id)['items']]
    has_primary_weapon = False
    worn = []
    for item, key, base_key in purchases:
        slot = slot_for(base_key)
        if slot is None and not has_primary_weapon and item.name.lower() in melee_text.lower():
            slot, has_primary_weapon = 'Primary Hand', True
        worn.append({'equipment_key': inventory_key(key, inventory), 'slot': slot or CARRIED_SLOT, 'quantity': item.quantity})
    # the outfit a class kit gives is worn too
    bought = {entry['equipment_key'] for entry in worn}
    worn += [{'equipment_key': key, 'slot': OUTFIT_SLOT, 'quantity': 1} for key in inventory if 'outfit' in key.lower() and key not in bought]
    if not worn:
        return
    equipped = pc.call('equip_items', character_id=character_id, items=worn)
    report['refused'].extend(equipped.get('errors', []))
    # a two-handed weapon does not go in one hand, and PCGen says nothing: it is put in both hands
    in_hand = {entry['equipment_key'] for entry in pc.call('get_equipped_items', character_id=character_id) if entry['equipment_key']}
    two_handed = [dict(entry, slot=BOTH_HANDS_SLOT) for entry in worn if entry['slot'] == 'Primary Hand' and entry['equipment_key'] not in in_hand]
    if two_handed:
        report['refused'].extend(pc.call('equip_items', character_id=character_id, items=two_handed).get('errors', []))
