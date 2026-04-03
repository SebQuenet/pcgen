# Bestiary 5 & 6 Monster Integration - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add all ~420 monsters from Bestiary 5 (209) and Bestiary 6 (211) to PCGen as fully functional LST data, matching the quality and format of Bestiaries 1-4.

**Architecture:** Each bestiary gets 3 new files (races.lst, kits_race.lst, updated abilities_race.lst) plus PCC updates. Monster data is sourced from pcfinder TypeScript objects at `/home/squenet/Documents/projects/pcfinder/repos/pcfinder/apps/pcfinder-csr/src/domain/character-creation/definitions/creatures/bestiary/` and manually converted to PCGen LST format using reverse-engineering of calculated stats into PCGen's decomposed formula system.

**Tech Stack:** PCGen LST format (tab-delimited), PCC campaign files

---

## Source Data Location

All creature data comes from pcfinder:
```
/home/squenet/Documents/projects/pcfinder/repos/pcfinder/apps/pcfinder-csr/src/domain/character-creation/definitions/creatures/bestiary/
```
Subdirectories: `aberration/`, `animal/`, `construct/`, `dragon/`, `fey/`, `humanoid/`, `ooze/`, `outsider/` (split across multiple .ts files), `plant/`, `undead/`, `unknown/`, `vermin/`

## Target Files

### Bestiary 5
```
data/pathfinder/paizo/roleplaying_game/bestiary_5/
├── b5_races.lst              (CREATE - monster definitions)
├── b5_kits_race.lst          (CREATE - stat blocks, feats, skills)
├── b5_abilities_race.lst     (UPDATE - add monster special abilities)
├── _bestiary_5.pcc           (UPDATE - add file references, change title)
└── _bestiary_5_for_players.pcc (no change)
```

### Bestiary 6
```
data/pathfinder/paizo/roleplaying_game/bestiary_6/
├── b6_races.lst              (CREATE - monster definitions)
├── b6_kits_race.lst          (CREATE - stat blocks, feats, skills)
├── b6_abilities_race.lst     (UPDATE - add monster special abilities)
├── _bestiary_6.pcc           (UPDATE - add file references, change title)
└── _bestiary_6 _for_players.pcc (no change)
```

---

## Conversion Rules Reference

### Rule 1: Ability Score Decomposition
PCGen uses base 10 + racial bonus. From pcfinder's final score:
```
BONUS:STAT|STR|<finalScore - 10>
```
- Example: STR 22 → `BONUS:STAT|STR|12`
- Example: STR 8 → `BONUS:STAT|STR|-2`
- Scores of 10 can be omitted
- Group same-bonus stats: `BONUS:STAT|STR,CON|10`
- For null/— scores (constructs CON, mindless INT): omit that stat entirely

### Rule 2: Natural Armor Decomposition
From pcfinder's AC values:
```
Natural Armor = AC_normal - 10 - DEX_modifier - size_modifier
```
Size modifiers: Fine +8, Diminutive +4, Tiny +2, Small +1, Medium 0, Large -1, Huge -2, Gargantuan -4, Colossal -8

Then: `BONUS:VAR|AC_Natural_Armor|<value>|TYPE=Base`

Verify: touch AC should equal 10 + DEX_mod + size_mod + deflection (if any)

### Rule 3: Save DC Calculation
Standard monster DC formula:
```
DC = 10 + (HD/2) + ability_modifier
```
In LST:
```
DEFINE:AbilityDC|0
BONUS:VAR|AbilityDC|10+(HD/2)+CON|TYPE=Base
BONUS:VAR|AbilityDC|2|PREABILITY:1,CATEGORY=FEAT,Ability Focus (Ability Name)
```
If the pcfinder DC doesn't match the standard formula, use a fixed override or racial bonus.

### Rule 4: MONSTERCLASS Mapping
The `numericHitDice` from pcfinder maps to the MONSTERCLASS level. The creature type determines the class:

| pcfinder type | MONSTERCLASS | HD |
|---|---|---|
| aberration | Aberration | d8 |
| animal | Animal | d8 |
| construct | Construct or Construct (Mindless) | d10 |
| dragon | Dragon | d12 |
| fey | Fey | d6 |
| humanoid | Humanoid | d8 |
| magical-beast | Magical Beast | d10 |
| monstrous-humanoid | Monstrous Humanoid | d10 |
| ooze | Ooze or Ooze (Intelligent) | d8 |
| outsider | Outsider (Fort/Ref) or (Fort/Will) or (Ref/Will) | d10 |
| plant | Plant or Plant (Mindless) | d8 |
| undead | Undead or Undead (Mindless) | d8 |
| vermin | Vermin or Vermin (Intelligent) | d8 |
| unknown (magical) | Magical Beast | d10 |
| unknown (monstrous) | Monstrous Humanoid | d10 |

### Rule 5: Outsider Save Variant Selection
Determine good saves from pcfinder's save values:
```
Good save base = HD/2 + 2
Poor save base = HD/3
```
Compare pcfinder saves minus ability modifier to determine which saves are "good":
- Fort+Ref good → `Outsider (Fort/Ref)`
- Fort+Will good → `Outsider (Fort/Will)`  
- Ref+Will good → `Outsider (Ref/Will)`

### Rule 6: Skill Rank Decomposition
From pcfinder's skill text, extract ranks:
```
Rank = total_bonus - ability_modifier - 3 (if class skill) - racial_bonus (if any)
```
Class skills for each type are defined in the core class definition.

### Rule 7: SIZE Mapping
| pcfinder | LST |
|---|---|
| Fine | F |
| Diminutive | D |
| Tiny | T |
| Small | S |
| Medium | M |
| Large | L |
| Huge | H |
| Gargantuan | G |
| Colossal | C |

### Rule 8: Alignment Mapping for Kits
| Alignment | KIT |
|---|---|
| LG | LG |
| NG | NG |
| CG | CG |
| LN | LN |
| N | TN |
| CN | CN |
| LE | LE |
| NE | NE |
| CE | CE |

### Rule 9: Movement Encoding
```
MOVE:Walk,30,Fly,60,Swim,40
```
For fly maneuverability: add `BONUS:VAR|Maneuverability|<N>` with DEFINE:
- Clumsy=1, Poor=2, Average=3, Good=4, Perfect=5

### Rule 10: Spell-Like Abilities
```
SPELLS:Innate|TIMES=ATWILL|CASTERLEVEL=<CL>|SpellName,<baseDC>+CHA
SPELLS:Innate|TIMES=3|CASTERLEVEL=<CL>|SpellName,<baseDC>+CHA
SPELLS:Innate|TIMES=1|CASTERLEVEL=<CL>|SpellName,<baseDC>+CHA
```
Base DC = spell level + 10 (standard). Group by frequency.

### Rule 11: Natural Attacks
```
NATURALATTACKS:Name,Weapon.Natural.Weapon Group Natural.Melee.Finesseable.DamageType,*count,damageDie
```
Damage types: Bludgeoning, Piercing, Slashing (can combine with dots)

### Rule 12: Damage Reduction
```
DR:10/Cold Iron
DR:10/Good
DR:15/Cold Iron and Good
```

### Rule 13: races.lst Line Template
```
<Name>\tOUTPUTNAME:[NAME]\tSTARTFEATS:1\tSIZE:<S>\tMOVE:<types>\tREACH:<N>\t<BONUS:STAT lines>\t<BONUS:VAR lines>\t<BONUS:SKILL lines>\t<CSKILL lines>\t<AUTO:LANG>\t<ABILITY lines>\tLEGS:<N>\tHANDS:<N>\t<NATURALATTACKS>\t<DEFINE lines>\t<SR:N>\t<DR:type>\tMONSTERCLASS:<Type>:<HD>\tRACETYPE:<Type>\t<RACESUBTYPE:sub1|sub2>\tCR:<N>\tROLE:<role>\tSOURCEPAGE:p.<N>\t<SPELLS:Innate>\tKIT:1|<align>\tFACT:BaseSize|<S>
```

### Rule 14: Kit Template
```
STARTPACK:<Name> Default\tTYPE:Default Monster.<Type>\tVISIBLE:QUALIFY\tEQUIPBUY:0\tPREMULT:1,[PRERACE:1,<RaceName>],[!PRERACE:1,%]\tSOURCEPAGE:p.<N>
ALIGN:<align>
RACE:<RaceName>\t!PRERACE:1,%
NAME:<DisplayName>
STAT:STR=10|DEX=10|CON=10|INT=10|WIS=10|CHA=10
SKILL:<SkillName>\tRANK:<N>
ABILITY:CATEGORY=FEAT|<FeatName>
```

### Rule 15: Ability Template  
```
<AbilityName>\tKEY:<Monster> ~ <AbilityName>\tCATEGORY:Special Ability\tTYPE:<SpecialAttack|SpecialQuality>.<Ex|Su|Sp>\tDEFINE:<VarName>|0\tDESC:<description with %1 placeholders>|<VarName>\tBONUS:VAR|<VarName>|10+(HD/2)+CON|TYPE=Base\tSOURCEPAGE:p.<N>
```

---

## Creature Type Mapping for "unknown" Directory

### Bestiary 5 - Unknown → Magical Beast
Echeneis, Brain Mole, Ramidreju, Chuspiki, Giant Muckdweller, Xiao, Karkadann, Brain Mole Monarch, Cerynitis, Mngwa, Su, Ahool, Makara Vahana, Peuchen, Amarok, Cherufe, Ketesthius, Isonade

### Bestiary 5 - Unknown → Monstrous Humanoid
Ichthyocentaur, Thriae Dancer, Storm Hag, Thriae Constructor, Elder Deep One, Gegenees, Ningen

### Bestiary 6 - Unknown → Magical Beast
Taniwha, Nekomata, Hivemind Rat Swarm, Muhuru, Mapinguari, Skrimsl, Goezspall, Atuikakura, Ouroboros, Cipactli, Kaiju Yarthoon, Kaiju Varklops

### Bestiary 6 - Unknown → Monstrous Humanoid
Tenome, Fen Mauler, Ghole, Psoglav, Rawhead, Deathsnatcher, Krampus, Great Old One Yig

---

## Tasks

### Task 1: Setup B5 File Structure and PCC

**Files:**
- Create: `data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_races.lst`
- Create: `data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_kits_race.lst`
- Modify: `data/pathfinder/paizo/roleplaying_game/bestiary_5/_bestiary_5.pcc`
- Modify: `data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_abilities_race.lst`

- [ ] **Step 1: Create b5_races.lst with header**

```
# Pathfinder Bestiary 5 Monster Races
SOURCELONG:Bestiary 5	SOURCESHORT:B5	SOURCEWEB:http://paizo.com/products/btpy9g9x	SOURCEDATE:2015-12
```

- [ ] **Step 2: Create b5_kits_race.lst with header**

```
# Pathfinder Bestiary 5 Monster Kits
SOURCELONG:Bestiary 5	SOURCESHORT:B5	SOURCEWEB:http://paizo.com/products/btpy9g9x	SOURCEDATE:2015-12
```

- [ ] **Step 3: Update _bestiary_5.pcc**

Change line 1 from:
```
CAMPAIGN:Bestiary 5 (Only Player Options Implemented)
```
to:
```
CAMPAIGN:Bestiary 5
```

Change STATUS from BETA to RELEASE.

Add these lines after the existing RACE/KIT entries:
```
RACE:b5_races.lst
KIT:b5_kits_race.lst
```

- [ ] **Step 4: Commit setup**

```bash
git add data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_races.lst
git add data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_kits_race.lst
git add data/pathfinder/paizo/roleplaying_game/bestiary_5/_bestiary_5.pcc
git commit -m "feat(b5): setup file structure for Bestiary 5 monsters"
```

---

### Task 2: Setup B6 File Structure and PCC

**Files:**
- Create: `data/pathfinder/paizo/roleplaying_game/bestiary_6/b6_races.lst`
- Create: `data/pathfinder/paizo/roleplaying_game/bestiary_6/b6_kits_race.lst`
- Modify: `data/pathfinder/paizo/roleplaying_game/bestiary_6/_bestiary_6.pcc`
- Modify: `data/pathfinder/paizo/roleplaying_game/bestiary_6/b6_abilities_race.lst`

- [ ] **Step 1: Create b6_races.lst with header**

```
# Pathfinder Bestiary 6 Monster Races
SOURCELONG:Bestiary 6	SOURCESHORT:B6	SOURCEWEB:http://paizo.com/products/btpy9r1y	SOURCEDATE:2017-05
```

- [ ] **Step 2: Create b6_kits_race.lst with header**

```
# Pathfinder Bestiary 6 Monster Kits
SOURCELONG:Bestiary 6	SOURCESHORT:B6	SOURCEWEB:http://paizo.com/products/btpy9r1y	SOURCEDATE:2017-05
```

- [ ] **Step 3: Update _bestiary_6.pcc**

Change line 1 from:
```
CAMPAIGN:Bestiary 6 (Only Player Options Implemented)
```
to:
```
CAMPAIGN:Bestiary 6
```

Change STATUS from BETA to RELEASE.

Add these lines after the existing RACE/KIT entries:
```
RACE:b6_races.lst
KIT:b6_kits_race.lst
```

- [ ] **Step 4: Commit setup**

```bash
git add data/pathfinder/paizo/roleplaying_game/bestiary_6/b6_races.lst
git add data/pathfinder/paizo/roleplaying_game/bestiary_6/b6_kits_race.lst
git add data/pathfinder/paizo/roleplaying_game/bestiary_6/_bestiary_6.pcc
git commit -m "feat(b6): setup file structure for Bestiary 6 monsters"
```

---

### Task 3: B5 Aberrations (9 creatures)

**Source:** pcfinder `aberration/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

**Creatures to convert:**
Read each creature from pcfinder, apply conversion rules, and write entries in all 3 files.

For each creature:
- [ ] **Step 1: Read creature data from pcfinder** `aberration/index.ts`
- [ ] **Step 2: Write race entry** in `b5_races.lst` following Rule 13 template
- [ ] **Step 3: Write special abilities** in `b5_abilities_race.lst` following Rule 15 template (one entry per special ability with KEY:MonsterName ~ AbilityName)
- [ ] **Step 4: Write kit entry** in `b5_kits_race.lst` following Rule 14 template
- [ ] **Step 5: Verify** AC decomposition (Rule 2), save DCs (Rule 3), HP from hit dice
- [ ] **Step 6: Commit**

```bash
git add data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_races.lst
git add data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_abilities_race.lst
git add data/pathfinder/paizo/roleplaying_game/bestiary_5/b5_kits_race.lst
git commit -m "feat(b5): add aberration monsters"
```

---

### Task 4: B5 Animals (17 creatures)

**Source:** pcfinder `animal/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder** `animal/index.ts`
- [ ] **Step 2: Write race entry** - Note: Animals have INT 1-2, no languages, STARTFEATS:1 omitted for mindless
- [ ] **Step 3: Write special abilities** - Animals commonly have: Scent, Low-Light Vision, Poison, Disease
- [ ] **Step 4: Write kit entry** - ALIGN:TN for all animals, minimal skills (Perception, Stealth, Swim)
- [ ] **Step 5: Verify** stats consistency
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add animal monsters"
```

---

### Task 5: B5 Constructs (9 creatures)

**Source:** pcfinder `construct/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Note: Constructs have no CON (omit BONUS:STAT|CON), use Construct or Construct (Mindless) MONSTERCLASS, often BONUS:VAR|Mindless|1, no STARTFEATS
- [ ] **Step 3: Write special abilities** - Constructs commonly have: Immunity to Magic, Hardness
- [ ] **Step 4: Write kit entry** - ALIGN:TN, no skills for mindless, STAT line omits CON or sets to 10
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add construct monsters"
```

---

### Task 6: B5 Dragons (23 creatures)

**Source:** pcfinder `dragon/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Dragons use MONSTERCLASS:Dragon with d12 HD. Check for age variants (Young/Adult/Ancient = different CLASS levels in kit, same RACE entry). Add breath weapons, frightful presence, spell-like abilities. All 3 good saves.
- [ ] **Step 3: Write special abilities** - Breath weapons need DEFINE:BreathWeaponDice, BreathWeaponDC with formulas. Age-dependent abilities use PREVARGTEQ:DragonAgeCategory.
- [ ] **Step 4: Write kit entries** - Multiple kits per dragon if age variants exist: Dragon (Type/Young), Dragon (Type/Adult), Dragon (Type/Ancient). CLASS:Dragon LEVEL:N varies by age.
- [ ] **Step 5: Verify** - Dragon stats scale with age, verify each variant
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add dragon monsters"
```

---

### Task 7: B5 Fey (14 creatures)

**Source:** pcfinder `fey/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Fey use d6 HD, good Ref+Will saves, 6 skills/level. Often have DR/cold iron, spell-like abilities, Low-Light Vision.
- [ ] **Step 3: Write special abilities**
- [ ] **Step 4: Write kit entry**
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add fey monsters"
```

---

### Task 8: B5 Humanoids (14 creatures)

**Source:** pcfinder `humanoid/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Humanoids use d8 HD, good Fort save, 2 skills/level. May have weapon/armor proficiencies (AUTO:WEAPONPROF, AUTO:ARMORPROF). Often have class levels in kit.
- [ ] **Step 3: Write special abilities**
- [ ] **Step 4: Write kit entry** - May include GEAR for weapons/armor
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add humanoid monsters"
```

---

### Task 9: B5 Oozes (5 creatures)

**Source:** pcfinder `ooze/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Oozes: no INT (mindless), blind, immune to flanking/crits. Use Ooze or Ooze (Intelligent) MONSTERCLASS.
- [ ] **Step 3: Write special abilities** - Split, Engulf, Acid, Constrict
- [ ] **Step 4: Write kit entry**
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add ooze monsters"
```

---

### Task 10: B5 Outsiders (54 creatures)

**Source:** pcfinder `outsider/*.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

This is the largest batch. Check all outsider subdirectories: aeons.ts, angels.ts, azatas.ts, daemons.ts, demons.ts, devils.ts, elementals.ts, inevitables.ts, kytons.ts, other.ts, proteans.ts, psychopomps.ts.

For each creature:
- [ ] **Step 1: Read creature data from pcfinder** - Check ALL outsider/*.ts files
- [ ] **Step 2: Determine save variant** - Use Rule 5 to pick Outsider (Fort/Ref), (Fort/Will), or (Ref/Will)
- [ ] **Step 3: Write race entry** - Outsiders: d10 HD, full BAB, 6 skills/level. Often have DR, SR, energy resistances, spell-like abilities. Multiple RACESUBTYPE entries.
- [ ] **Step 4: Write special abilities** - Many unique abilities per outsider. Follow KEY:MonsterName ~ AbilityName pattern.
- [ ] **Step 5: Write kit entry** - Many skills to rank, many feats
- [ ] **Step 6: Verify** - Cross-check save variant, SLA DCs, DR stacking
- [ ] **Step 7: Commit**

```bash
git commit -m "feat(b5): add outsider monsters"
```

---

### Task 11: B5 Plants (8 creatures)

**Source:** pcfinder `plant/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Plants: d8 HD, good Fort, immune to mind-affecting/poison/sleep/paralysis/polymorph/stun. Use Plant or Plant (Mindless).
- [ ] **Step 3: Write special abilities**
- [ ] **Step 4: Write kit entry**
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add plant monsters"
```

---

### Task 12: B5 Undead (18 creatures)

**Source:** pcfinder `undead/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Undead: d8 HD, good Will, no CON (use CHA for HP bonus). Immune to mind-affecting, death effects, disease, paralysis, poison, sleep, stun.
- [ ] **Step 3: Write special abilities** - Channel resistance, energy drain, incorporeal (if applicable)
- [ ] **Step 4: Write kit entry** - No CON in STAT line (or CON=10 with no racial bonus)
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add undead monsters"
```

---

### Task 13: B5 Vermin (11 creatures)

**Source:** pcfinder `vermin/index.ts` - filter source="Bestiary 5"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Vermin: d8 HD, good Fort, mindless (no INT), darkvision 60ft. No feats unless intelligent variant.
- [ ] **Step 3: Write special abilities** - Poison, Web, Tremorsense
- [ ] **Step 4: Write kit entry** - Minimal: ALIGN:TN, no skills, no feats for mindless
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add vermin monsters"
```

---

### Task 14: B5 Magical Beasts from "unknown" (18 creatures)

**Source:** pcfinder `unknown/index.ts` - filter source="Bestiary 5" AND type is "magical"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

Creatures: Echeneis, Brain Mole, Ramidreju, Chuspiki, Giant Muckdweller, Xiao, Karkadann, Brain Mole Monarch, Cerynitis, Mngwa, Su, Ahool, Makara Vahana, Peuchen, Amarok, Cherufe, Ketesthius, Isonade

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Use MONSTERCLASS:Magical Beast, RACETYPE:Magical Beast. d10 HD, full BAB, good Fort+Ref.
- [ ] **Step 3: Write special abilities**
- [ ] **Step 4: Write kit entry**
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add magical beast monsters from unknown type"
```

---

### Task 15: B5 Monstrous Humanoids from "unknown" (7 creatures)

**Source:** pcfinder `unknown/index.ts` - filter source="Bestiary 5" AND type is "monstrous"
**Target files:** `b5_races.lst`, `b5_abilities_race.lst`, `b5_kits_race.lst`

Creatures: Ichthyocentaur, Thriae Dancer, Storm Hag, Thriae Constructor, Elder Deep One, Gegenees, Ningen

For each creature:
- [ ] **Step 1: Read creature data from pcfinder**
- [ ] **Step 2: Write race entry** - Use MONSTERCLASS:Monstrous Humanoid, RACETYPE:Monstrous Humanoid. d10 HD, full BAB, good Ref+Will.
- [ ] **Step 3: Write special abilities**
- [ ] **Step 4: Write kit entry**
- [ ] **Step 5: Verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b5): add monstrous humanoid monsters from unknown type"
```

---

### Task 16: B6 Aberrations (11 creatures)

**Source:** pcfinder `aberration/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

Same process as Task 3 but for B6. Aberrations: d8 HD, good Will.

- [ ] **Step 1-5: Read, write race, abilities, kit, verify** for each creature
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add aberration monsters"
```

---

### Task 17: B6 Animals (11 creatures)

**Source:** pcfinder `animal/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add animal monsters"
```

---

### Task 18: B6 Constructs (15 creatures)

**Source:** pcfinder `construct/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add construct monsters"
```

---

### Task 19: B6 Dragons (17 creatures)

**Source:** pcfinder `dragon/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify** - Handle age variants
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add dragon monsters"
```

---

### Task 20: B6 Fey (13 creatures)

**Source:** pcfinder `fey/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add fey monsters"
```

---

### Task 21: B6 Humanoids (15 creatures)

**Source:** pcfinder `humanoid/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add humanoid monsters"
```

---

### Task 22: B6 Oozes (12 creatures)

**Source:** pcfinder `ooze/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add ooze monsters"
```

---

### Task 23: B6 Outsiders (62 creatures)

**Source:** pcfinder `outsider/*.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

Largest batch for B6. Check all outsider subdirectories.

- [ ] **Step 1-6: Read, determine save variant, write race, abilities, kit, verify** for each creature
- [ ] **Step 7: Commit**

```bash
git commit -m "feat(b6): add outsider monsters"
```

---

### Task 24: B6 Plants (10 creatures)

**Source:** pcfinder `plant/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add plant monsters"
```

---

### Task 25: B6 Undead (13 creatures)

**Source:** pcfinder `undead/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add undead monsters"
```

---

### Task 26: B6 Vermin (11 creatures)

**Source:** pcfinder `vermin/index.ts` - filter source="Bestiary 6"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add vermin monsters"
```

---

### Task 27: B6 Magical Beasts from "unknown" (12 creatures)

**Source:** pcfinder `unknown/index.ts` - filter source="Bestiary 6" AND type is "magical"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

Creatures: Taniwha, Nekomata, Hivemind Rat Swarm, Muhuru, Mapinguari, Skrimsl, Goezspall, Atuikakura, Ouroboros, Cipactli, Kaiju Yarthoon, Kaiju Varklops

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add magical beast monsters from unknown type"
```

---

### Task 28: B6 Monstrous Humanoids from "unknown" (8 creatures)

**Source:** pcfinder `unknown/index.ts` - filter source="Bestiary 6" AND type is "monstrous"
**Target files:** `b6_races.lst`, `b6_abilities_race.lst`, `b6_kits_race.lst`

Creatures: Tenome, Fen Mauler, Ghole, Psoglav, Rawhead, Deathsnatcher, Krampus, Great Old One Yig

- [ ] **Step 1-5: Read, write race, abilities, kit, verify**
- [ ] **Step 6: Commit**

```bash
git commit -m "feat(b6): add monstrous humanoid monsters from unknown type"
```

---

### Task 29: Final Validation

**Files:** All created/modified files

- [ ] **Step 1: Count verification** - Verify total creature count in b5_races.lst matches 209, b6_races.lst matches 211
- [ ] **Step 2: Kit count** - Verify each creature in races.lst has a corresponding kit in kits_race.lst
- [ ] **Step 3: Ability references** - Verify every ABILITY:Special Ability|AUTOMATIC reference in races.lst has a matching KEY in abilities_race.lst
- [ ] **Step 4: PCC completeness** - Verify PCC files reference all new data files
- [ ] **Step 5: Format check** - Verify tab separation is correct (no spaces used as delimiters)
- [ ] **Step 6: Spot-check 10 creatures** - For 10 random creatures across types, manually verify:
  - AC decomposition: natural armor + dex + size = total AC
  - HP: HD × average die + CON bonus × HD = total HP
  - Save DCs match formulas
  - Skill ranks + ability mod + class skill bonus = total skill
- [ ] **Step 7: Final commit if any fixes**

```bash
git commit -m "fix(b5,b6): validation fixes for bestiary 5 and 6 monsters"
```
