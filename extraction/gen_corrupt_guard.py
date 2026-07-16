#!/usr/bin/env python3
"""Pilot generator: Corrupt Guard New Rules crunch -> Villain Codex source LST."""
import os

VC = "/home/squenet/Documents/projects/pcgen-seb-villain-codex/data/pathfinder/paizo/roleplaying_game/villain_codex"
T = "\t"
HDR = "SOURCELONG:Villain Codex\tSOURCESHORT:VC\tSOURCEDATE:2017-11"

def row(*toks):
    return T.join(toks)

def write(path, title, rows):
    body = ["# Villain Codex - " + title, HDR, ""] + rows + [""]
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(body))

# ---------------------------------------------------------------- FEATS
feats = [
    row(
        "Coordinated Capture", "CATEGORY:FEAT", "TYPE:Combat.Teamwork",
        "PRETOTALAB:1",
        "DESC:You work with allies to prevent foes from escaping.",
        "BENEFIT:Whenever you and one or more allies with this feat threaten the same enemy, the enemy takes a penalty on Acrobatics checks and concentration checks to avoid provoking attacks of opportunity equal to the number of creatures with this feat that are threatening him (maximum +5).",
        "SOURCEPAGE:p.44",
    ),
    row(
        "Favored Community", "CATEGORY:FEAT", "TYPE:General",
        "PRESKILL:1,Knowledge (Local)=2",
        "DESC:You know your local community like the back of your hand.",
        "BENEFIT:Select a permanent, stationary settlement. While in that settlement, you gain a +2 bonus on initiative checks and Knowledge (geography), Perception, Stealth, and Survival checks, as per the ranger's favored terrain class feature. If you already have that class feature and apply its bonuses in your favored community, increase the bonus provided by favored terrain by 2. The bonuses from this feat do not stack with other effects that provide or augment favored terrain bonuses, such as boots of friendly terrain.",
        "SOURCEPAGE:p.44",
    ),
]

# ---------------------------------------------------------------- SPELLS
spells = [
    row(
        "Beacon of Guilt", "TYPE:Arcane.Divine",
        "CLASSES:Bard,Inquisitor,Occultist,Sorcerer,Wizard=2",
        "SCHOOL:Evocation", "DESCRIPTOR:Curse",
        "COMPS:V, S", "CASTTIME:1 standard action", "RANGE:Touch",
        "TARGETAREA:One object",
        "DURATION:24 hours or until discharged, then instantaneous",
        "SAVEINFO:Will negates (object); see text", "SPELLRES:Yes",
        "SOURCEPAGE:p.44",
        "DESC:You place an invisible ward upon an object that is triggered the first time a creature tries to move the object from its current location. The next creature to touch the object is cursed to become obvious to everyone around it. The creature must succeed at a Will save or be surrounded in an aura of twinkling red light that functions as faerie fire (spell resistance applies). The curse bestowed by this spell cannot be dispelled, but a break enchantment, limited wish, miracle, remove curse, or wish spell can remove it.",
    ),
    row(
        "Escape Alarm", "TYPE:Arcane.Divine",
        "CLASSES:Bard,Inquisitor,Occultist,Psychic,Ranger,Sorcerer,Wizard,Spiritualist,Summoner=2",
        "SCHOOL:Abjuration",
        "COMPS:V, S, F", "CASTTIME:1 standard action", "RANGE:Medium",
        "TARGETAREA:Ten 10-ft. cubes/level", "DURATION:24 hours",
        "SAVEINFO:None", "SPELLRES:No",
        "SOURCEPAGE:p.44",
        "DESC:You place a ward on an area that notifies you when a creature exits it. This functions as alarm, except as noted. It alerts you when a creature leaves, rather than enters, the area, and you can't select a password to bypass its effects. Instead, when you place an escape alarm, you can attune up to one additional creature per caster level to the spell. You are automatically attuned to your own escape alarm and don't count against the limit. Attuned creatures can enter and exit the spell's area without triggering the alarm. If you select a mental alarm rather than an audible one, all attuned creatures receive the mental alert when someone exits the warded area.",
    ),
]

# ------------------------------------------------------- MUNDANE EQUIPMENT
equip_general = [
    row(
        "Alchemical Cleaner", "TYPE:Goods.General.Alchemical", "COST:300", "WT:1",
        "SOURCEPAGE:p.44",
        "DESC:This caustic alchemical solution dissolves blood, hair, and small pieces of flesh. Distributing it over a 5-foot square takes one full-round action and increases the DC of Perception checks to find such materials by 20. One pot contains enough liquid to cover nine 5-foot squares and can be thrown as a splash weapon, functioning as an alkali flask when thrown. Crafting alchemical cleaner requires a successful DC 25 Craft (alchemy) check.",
    ),
    row(
        "Guard's Kit", "TYPE:Goods.General.Kit", "COST:20", "WT:20",
        "SOURCEPAGE:p.44",
        "DESC:A standard set of supplies issued to guards. This kit consists of a backpack, a belt pouch, a flint and steel, manacles, 50 feet of rope, a signal whistle, soap, and 5 torches.",
    ),
]

# ------------------------------------------------------- MAGIC ITEMS
magic_items = [
    row(
        "Badge of Authority", "TYPE:Magic.Wondrous", "COST:1500", "WT:0.5",
        "SOURCEPAGE:p.44",
        "SPROP:As a standard action, the wielder can transform its appearance into that of any badge or symbol of authority she legitimately holds. Once per day, she can present the badge as a standard action to reduce the base speed of up to three creatures within 30 feet, as per hobble (Fortitude DC 11 negates). If the wielder uses Intimidate to force a creature to act friendly and that creature recognizes her as a legitimate authority, its attitude doesn't change to unfriendly after 1d6x10 minutes, and it doesn't report her to local authorities.",
        "DESC:This solid metal badge lets its wielder impersonate authority and cow onlookers. Construction Requirements: Craft Wondrous Item, cause fear, silent image; Cost 750 gp.",
    ),
    row(
        "Erasing Book", "TYPE:Magic.Wondrous", "COST:400", "WT:3",
        "SOURCEPAGE:p.45",
        "SPROP:Once per day as a standard action, the holder can erase the non-material-component writing contained within this leather-bound book. It can be used as a spellbook or formula book; spells written in it with a material component cost are immune to the erasing effect. Erasing also removes smudges, stains, and minor water damage, but does not recover text lost to serious damage.",
        "DESC:An erasing book. Construction Requirements: Craft Wondrous Item, erase; Cost 200 gp.",
    ),
    row(
        "Pliability Elixir", "TYPE:Magic.Wondrous.Potion", "COST:500", "WT:0",
        "SOURCEPAGE:p.45",
        "SPROP:A creature that drinks this elixir becomes mentally and emotionally pliable for 10 minutes (Will DC 16 negates). Bluff, Diplomacy, and Intimidate checks automatically succeed against a pliable creature, except Diplomacy checks to improve its attitude (normal DC). After 1 minute the effect is noticeable with a DC 25 Sense Motive check. A vial appears to be an elixir of truth unless identified by 10 or more.",
        "DESC:Pliability elixir. Construction Requirements: Craft Wondrous Item, charm person, zone of truth; Cost 250 gp.",
    ),
    row(
        "Quill of Verification", "TYPE:Magic.Wondrous", "COST:1000", "WT:0",
        "SOURCEPAGE:p.45",
        "SPROP:This swan-feather quill verifies signatures. Store a signature by pressing the quill into it and speaking a command word, then tap another signature to check authenticity as if attaining a 20 on a Linguistics check; the quill glows blue if legitimate. It can validate a signature up to 3 times per day, and the stored signature can be switched any number of times between uses.",
        "DESC:Quill of verification. Construction Requirements: Craft Wondrous Item, identify; Cost 500 gp.",
    ),
]

os.makedirs(VC, exist_ok=True)
write(os.path.join(VC, "vc_feats.lst"), "feats", feats)
write(os.path.join(VC, "vc_spells.lst"), "spells", spells)
write(os.path.join(VC, "vc_equip_general.lst"), "mundane equipment", equip_general)
write(os.path.join(VC, "vc_equip_magic_items.lst"), "magic items", magic_items)
print("pilot LST written to", VC)
for fn in ("vc_feats.lst", "vc_spells.lst", "vc_equip_general.lst", "vc_equip_magic_items.lst"):
    print("  ", fn)
