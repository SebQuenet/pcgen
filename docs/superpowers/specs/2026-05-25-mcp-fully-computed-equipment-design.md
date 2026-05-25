# Design — MCP capability: fully-computed custom equipment

**Date:** 2026-05-25
**Status:** Approved (design), pending implementation plan
**Author:** Seb + Claude

## Problem

The MCP `customize_equipment` tool can apply stock **equipment modifiers** (EqMods) to a base
item — e.g. `+4`, `Holy`, `Bane (Evil Outsider)` on a Morningstar. This covers passive weapon
enchantments, but it cannot model an artifact's richer mechanics:

- **Activated spell-like abilities** (e.g. Holy Smite / Holy Word / Holy Sword / Holy Javelin,
  1×/day each).
- **Conditional bonuses** that depend on the wielder or on possessing another item
  (e.g. caster level rises from 10 to 15 when a second item is also equipped).
- **Tracked situational bonuses** (e.g. +2 to caster-level checks to dispel evil magic).

The concrete driver is the campaign artifact **"Sceptre de Timéon"** (a `+4` Holy
Evil-Outsider-Bane morningstar with four 1/day divine spell-like abilities, a +2 dispel bonus
versus evil, and a conditional caster-level boost when wielded together with the "Diadème de
Lyra").

## Goal

Extend the MCP so such an item is created **fully computed by PCGen's existing calculation
engine** — with **no changes to the engine** and **no changes to the save format**.

## Key insight

PCGen's engine **already** supports every required mechanic through data-native LST tokens:

| Mechanic | Engine feature (already exists) |
|---|---|
| N×/day spell-like abilities with a formula caster level | `SPELLS:<id>\|TIMES=<formula>\|CASTERLEVEL=<formula>\|<SpellKey>` (see `plugin/lsttokens/SpellsLst.java:66`) |
| Conditional caster level (e.g. +5 when another item equipped) | `DEFINE:SceptreCL\|10` + `BONUS:VAR\|SceptreCL\|5\|PREEQUIP:1,<Item>` |
| Tracked situational bonus, alignment-gated | `BONUS:VAR\|SceptreDispelEvil\|2\|PREALIGN:LG,NG,CG,LN,TN,CN` |
| Descriptive special property | `SPROP\|<text>` |
| `+4` / Holy / Bane | stock EqMods (already applied) |

Rewriting the engine would add **no player-facing capability** here while introducing large
regression and save-format risk. The real gap is that the **MCP cannot construct these
data-native tokens** on a custom item.

## Mechanism

Reuse PCGen's own LST token parser at runtime:

```
LoadContext.processToken(equipment, tokenName, tokenValue);   // validates + stages
context.commit();                                              // applies
```

`processToken` (`pcgen/rules/context/LoadContextInst.java:330`, delegating to
`TokenSupport.processToken`) is the exact path the data loader uses. It validates input the same
way as data loading — a malformed token yields a `ParseResult` error that we surface to the
caller. No calculation logic is reimplemented.

## API changes — `customize_equipment`

Two new **optional** parameters, applied **after** the existing `modifier_keys` so the base
enchantments (and any `WeaponEnhancement` prerequisites) are in place first.

### 1. `spell_abilities` (structured, ergonomic)

A list of objects:

| Field | Type | Required | Notes |
|---|---|---|---|
| `spell` | string | yes | Spell key/name; validated against loaded data |
| `times_per_day` | string | yes | Integer or formula (becomes `TIMES=`; `-1` = at will) |
| `caster_level` | string | yes | Integer, variable, or formula (becomes `CASTERLEVEL=`) |

For each ability the tool emits and applies one
`SPELLS:<item-identifier>|TIMES=<times_per_day>|CASTERLEVEL=<caster_level>|<SpellKey>` line
(format per `plugin/lsttokens/SpellsLst.java:60`). The `<item-identifier>` is a stable per-item
label (the custom item name) so the granted spells are grouped under the item. The `SPELLS:`
token has no Arcane/Divine field — the spell-like ability's save DC uses the engine default for
the spell, so no `spell_type` parameter is needed.

### 2. `extra_tokens` (general escape hatch)

A list of raw LST token strings (e.g. `"DEFINE:SceptreCL|10"`,
`"BONUS:VAR|SceptreCL|5|PREEQUIP:1,Diadème de Lyra"`, `"SPROP|..."`). Each string is split on the
first `:` into `tokenName` / `tokenValue` and applied via `processToken`. This covers `DEFINE`,
`BONUS`, `SPROP`, and any other equipment-legal token without bespoke per-token API surface.

Both parameters are optional and independent of each other and of `modifier_keys`.

## Worked example — the Sceptre de Timéon

```jsonc
customize_equipment(
  equipment_key = "Morningstar",
  custom_name   = "Sceptre de Timéon",
  modifier_keys = [
    "Special Ability ~ +4 ~ Weapon",
    "Special Ability ~ Holy ~ Weapon",
    "Special Ability ~ Bane ~ Weapon"
  ],
  choices = { "Special Ability ~ Bane ~ Weapon": ["Evil Outsider Bane"] },
  spell_abilities = [
    { spell: "Holy Smite",   times_per_day: "1", caster_level: "SceptreCL" },
    { spell: "Holy Word",    times_per_day: "1", caster_level: "SceptreCL" },
    { spell: "Holy Sword",   times_per_day: "1", caster_level: "SceptreCL" },
    { spell: "Holy Javelin", times_per_day: "1", caster_level: "SceptreCL" }
  ],
  extra_tokens = [
    "DEFINE:SceptreCL|10",
    "BONUS:VAR|SceptreCL|5|PREEQUIP:1,Diadème de Lyra",
    "BONUS:VAR|SceptreDispelEvil|2|PREALIGN:LG,NG,CG,LN,TN,CN",
    "SPROP|+2 sainteté aux tests de NLS pour dissiper la magie du Mal / effets d'extérieurs Mauvais"
  ]
)
```

Result when equipped by Miros (Lawful Good):

- Holy Smite / Holy Word / Holy Sword / Holy Javelin — **1×/day each at caster level 10**.
- If the **Diadème de Lyra** is also equipped, `SceptreCL` resolves to **15**, and the spell DCs
  and damage recompute automatically (the `CASTERLEVEL` formula is re-evaluated by the engine).
- `SceptreDispelEvil` = **+2** tracked variable (only for good/neutral wielders via `PREALIGN`).
- `+4` / Holy / Bane combat mechanics unchanged (already in place).

All of the above is computed by the existing engine; nothing is a static note.

## Components / files

- `code/src/java/pcgen/mcp/tools/CustomEquipmentTools.java`
  - Extend the tool JSON schema with `spell_abilities` and `extra_tokens`.
  - After applying `modifier_keys`, build `SPELLS:` lines from `spell_abilities` and apply
    `extra_tokens`, all via `LoadContext.processToken` + `commit`.
- A small private helper (in the same file, or a focused new class) for:
  - resolving a `Spell` by key from the loaded data context,
  - constructing the `SPELLS:` token value,
  - applying a token string and collecting `ParseResult` errors.
- Tests (`code/src/utest/` or `code/src/itest/`): build the Sceptre and assert that the `SPELLS`
  abilities, the `SceptreCL` define, and the bonuses are present, and that the granted caster
  level resolves to 10 without the Diadème and 15 with it equipped.

## Error handling

- **Unknown spell key** → entry added to a `failed` list in the response (consistent with the
  existing `failedModifiers` behaviour); other abilities still applied.
- **Malformed token** → the `ParseResult` error text is surfaced in the `failed` list.
- **Non-blocking**: the tool returns the created item plus the list of any failures rather than
  aborting the whole call.

## Out of scope

- No changes to PCGen's calculation engine or to `EqModSpellInfo` / the spell-builder GUI path.
- No save-format changes (the produced item serialises as a normal custom equipment item with
  CDOM tokens).
- No automatic output-sheet redesign; niche values (e.g. the dispel bonus) surface as tracked
  variables / `SPROP` text, which existing templates already render.

## Risks & mitigations

- **Token applies to the wrong object / not committed** → mitigate by mirroring the data-loader
  sequence (`processToken` then `commit`) and covering it with the integration test.
- **`PREEQUIP` item-name matching** (accents, exact key) → the test equips both items and asserts
  the caster-level change, catching mismatches.
- **Equipment-context vs. global context** for `processToken` → resolved during planning by
  confirming which `LoadContext` the MCP session exposes for the active data set.
