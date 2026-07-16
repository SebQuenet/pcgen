# Fang Monastery — Crunch Inventory

Source: *Pathfinder RPG Villain Codex*, New Rules pp. 104–105.
Every element below is marked `*` (new to this book) and is a candidate for PCGen LST.

Counts: 1 archetype, 2 class options (1 oracle mystery — already implemented; 1 oracle curse), 3 feats. (No new spells, mundane equipment, magic items, or rituals.)

---

## Archetypes

### Hunting Serpent (Ninja) *
- **Base class:** Ninja (*Pathfinder RPG Ultimate Combat*).
- **Concept:** The order dispatches specially trained killers to hunt relentlessly and fight from the shadows.
- **Availability:** Available to ninjas from Fang Monastery.
- **Source page:** 105.

Level-by-level class-feature changes (each new/altered feature's full text follows):

| Ninja level | New/altered feature | Replaces / alters |
|---|---|---|
| — | Class Skills | Alters class skills |
| 3rd | Relentless Pursuit | Replaces no trace |
| 4th | Death Mark | Replaces uncanny dodge, improved uncanny dodge, and the ninja tricks gained at 12th and 16th levels |
| 10th | Certain Demise | Replaces the ninja trick gained at 10th level |

**Class Skills:** A hunting serpent gains Survival as a class skill but loses Knowledge (nobility).
*This ability alters the ninja's class skills.*

**Relentless Pursuit (Ex):** At 3rd level, a hunting serpent gains a +1 bonus on Diplomacy checks to gather information and on Survival checks to identify or follow tracks. The bonus increases by 1 for every 3 ninja levels beyond 3rd.
*This ability replaces no trace.*

**Death Mark (Ex):** At 4th level, as a swift action once per day, a hunting serpent can mark an opponent damaged by her sneak attack for death. A hunting serpent gains a +1 competence bonus on attack and damage rolls against an opponent affected by death mark as well as on Survival checks to track the marked creature. The bonuses from death mark and its number of uses per day increase by 1 at 8th, 12th, and 16th levels. The effects of death mark last for 24 hours, until the marked creature is slain, or until the hunting serpent chooses to mark another creature, whichever comes first.
*This ability replaces uncanny dodge, improved uncanny dodge, and the ninja tricks gained at 12th and 16th levels.*

**Certain Demise (Ex):** At 10th level, the hunting serpent's sneak attack damage applies on the first attack she makes each round against the target of her death mark ability, even if it normally wouldn't. Other effects that trigger on a sneak attack do not apply with this ability.
*This ability replaces the ninja trick gained at 10th level.*

---

## Class Options

### Ascetic (Oracle Mystery) *
- **Category:** Oracle mystery (*Pathfinder RPG Advanced Player's Guide* oracle class).
- **Note:** already implemented in `vc_abilities_class.lst`.
- **Deities:** Gozreh, Irori.
- **Source page:** 104.

**Class Skills:** An oracle with the ascetic mystery adds Acrobatics (Dex), Climb (Str), Escape Artist (Dex), and Swim (Str) to her list of class skills.

**Bonus Spells:** *stone fist* [APG] (2nd), *glide* [APG] (4th), *force punch* [UM] (6th), *ethereal fists* [OA] (8th), *contact other plane* (10th), *legend lore* (12th), *vision* (14th), *frightful aspect* [UC] (16th), *iron body* (18th).

**Revelations:** An oracle with the ascetic mystery can choose from any of the following:

- **Absence of Body (Su):** You require half as much food and water to survive as a normal member of your race. Additionally, each time you hold your breath, you can do so for an additional number of rounds equal to your oracle level. At 15th level, you no longer need to eat or drink and can hold your breath for 10 times as long (2 minutes per point of Constitution, plus 1 additional minute per oracle level).
- **Absence of Form (Sp):** You gain *feather fall* as a spell-like ability, which you can use a number of rounds per day equal to your oracle level. The rounds do not need to be used consecutively. If you are in mid fall when this ability's duration expires, you take falling damage as if you fell from the altitude you were at when the spell ended. At 10th level, you can also apply your daily rounds of this ability to use *air walk*.
- **Ascetic Armor (Su):** You can use meditative techniques that temporarily cause attacks to bounce off your skin, as long as you aren't wearing armor or carrying a medium or heavy load. These techniques grant you a +4 armor bonus to AC. At 7th level, and every 4 levels thereafter, this bonus increases by 2. At 13th level, the techniques also grant DR 5/unarmed strikes or natural attacks. You can gain the benefits of ascetic armor for 1 hour per day per oracle level. The hours do not need to be consecutive, but you must spend them in 1-hour increments.
- **Fleet (Ex):** You gain a +10-foot enhancement bonus to your base land speed. You lose this extra speed if you wear any armor or carry a medium or heavy load. At 7th level, and every 6 levels thereafter, this bonus increases by 10 feet. Oracles with the lame oracle curse can't select this revelation.
- **Martial Disciple (Su):** Through meditation and study of monastic forms of combat, you have learned a lethal form of unarmed combat. You gain Improved Unarmed Strike as a bonus feat, even if you do not meet the prerequisites. You gain the unarmed strike damage of a monk of your oracle level. If you also have monk levels, those levels stack for the purpose of determining your unarmed strike damage.
- **Oracular Spellstrike (Su):** By harnessing a synergy of unarmed combat and spellcasting, you have uncovered a means of channeling spell energy quickly through your unarmed strikes. This resembles the magus spellstrike ability, except it works with unarmed strikes only, no matter what other abilities you have. Additionally, it uses spells from the cleric spell list, not the magus spell list. You must be at least 7th level before selecting this revelation.
- **Rapid Convalescence (Su):** You reduce the number of consecutive successful saves needed to recover from a disease or poison by 1 (to a minimum of 1). Additionally, you can sacrifice one of your unused spell slots for the day to gain an enhancement bonus equal to that spell slot's level on your next save to resist the effects of poison or disease, provided the save is attempted within 1 minute per level of spell expended.
- **Spell Deflection (Su):** You can use a readied action to counterspell any spell being cast on you or an adjacent ally that requires a ranged touch attack (such as a ray). In order to successfully counter the spell, you must make an unarmed attack roll. If the result of this attack roll is higher than 20 + the spell's caster level, the spell is negated with no effect. At 17th level, any spell you successfully negate is reflected back at the caster with the same ranged touch attack result. You must be at least 11th level before selecting this revelation.

### Toxic Blood (Oracle Curse) *
- **Category:** Oracle curse (*Pathfinder RPG Advanced Player's Guide* oracle class); most common among oracles of Fang Monastery.
- **Source page:** 104.

**Effect:** Your body is ravaged by a potent, slow-acting poison that resists all treatment. Whenever you must attempt a Fortitude save to resist a poison effect, roll twice and take the lowest result. Additionally, you need one more consecutive successful save to end an ongoing poison. You lose any immunity to poison you have upon receiving this curse, and you become immune to *delay poison* and *neutralize poison*, as well as other attempts to suppress or remove the poison from which you are suffering. You gain the poison use ability.

At 5th level, you are so envenomed that your touch becomes poisonous. Once per day for every 4 oracle levels you have, you can deliver a unique contact poison as a touch attack or via an unarmed strike or natural weapon. This poison deals 1d2 points of Dexterity damage per round for 5 rounds. Poisoned creatures can attempt a Fortitude save (DC = 10 + 1/2 your oracle level + your Charisma modifier) each round. Success negates the damage and ends the affliction.

At 10th level, the poison deals 1d3 points of Dexterity damage per round for 7 rounds.

At 15th level, you can expend a use of your poison touch to transfer any poison effect you are currently experiencing from yourself to another creature. The touched creature must immediately attempt a saving throw against the poison (using the poison's initial DC and duration for a single dose, regardless of how many doses are currently affecting you). If the creature fails its save, the poison ceases to act on you, as if you had succeeded at all the necessary saves (this bypasses your immunity to effects that remove poison).

---

## Feats

### Twin Fang Style (Combat, Style) *
- **Type:** Combat, Style.
- **Flavor:** Your paired blades rend through armor.
- **Prerequisites:** Dex 15, Quick Draw, Two-Weapon Fighting, base attack bonus +1, Acrobatics 1 rank.
- **Source page:** 105.

**Benefit:** Each time you make a full attack wielding a pair of daggers or a pair of kama and hit a creature with both weapons, you reduce its armor bonus to AC by 1 until the beginning of your next turn. This reduction stacks to a minimum armor bonus of +0. For example, if you hit a creature wearing a chain shirt with two primary attacks and two off-hand attacks, you would reduce its armor bonus to AC from +4 to +2.

### Twin Fang Strike (Combat) *
- **Type:** Combat.
- **Flavor:** You strike at your enemies with both weapons simultaneously.
- **Prerequisites:** Dex 15; Quick Draw; Twin Fang Style*; Two-Weapon Fighting; Acrobatics 6 ranks; base attack bonus +6 or monk level 6th.
- **Source page:** 105.

**Benefit:** While using Twin Fang Style, when you take an attack action while wielding two daggers or two kama, you can strike with both your primary and off-hand weapons. If you do so, both attacks take a –4 penalty and you apply precision damage and effects that occur when you hit only once, even if you hit with both attacks.

### Twin Fang Lunge (Combat) *
- **Type:** Combat.
- **Flavor:** You strike with the force of a serpent's lunge.
- **Prerequisites:** Dex 15; Quick Draw; Twin Fang Strike*; Twin Fang Style*; Two-Weapon Fighting; Acrobatics 8 ranks; base attack bonus +8 or monk level 8th.
- **Source page:** 105.

**Benefit:** While using Twin Fang Style, you can spend a full-round action to move up to twice your speed and then use Twin Fang Strike as if you were taking the attack action.
