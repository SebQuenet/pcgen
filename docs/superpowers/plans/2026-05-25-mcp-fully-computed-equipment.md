# MCP Fully-Computed Custom Equipment — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the MCP `customize_equipment` tool so a custom item can carry activated spell-like abilities and conditional/tracked bonuses, modelled with PCGen's existing engine tokens (`SPELLS:` / `DEFINE:` / `BONUS:`) — no engine or save-format changes.

**Architecture:** A new package-private helper `EquipmentTokenSupport` applies raw LST token strings to an in-memory `Equipment` via `LoadContext.processToken(...)` + `commit()` (the same path the data loader uses, with the same validation), and builds `SPELLS:` lines from structured spell-ability input. `customize_equipment` gains two optional params (`spell_abilities`, `extra_tokens`) that call this helper after applying the existing `modifier_keys`.

**Tech Stack:** Java 25, JUnit 5, Gradle wrapper. PCGen token infrastructure (`pcgen.rules.context.LoadContext`, `plugin.lsttokens.*`). MCP tool layer (`pcgen.mcp.tools`).

**Spec:** `docs/superpowers/specs/2026-05-25-mcp-fully-computed-equipment-design.md`

---

## File Structure

- **Create** `code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java` — pure helper: build `SPELLS:` strings, validate spells, apply token strings to an `Equipment`, return failure messages. No MCP/JSON knowledge; depends only on PCGen core + token context. Testable in isolation.
- **Modify** `code/src/java/pcgen/mcp/tools/CustomEquipmentTools.java` — extend the `customize_equipment` tool schema with `spell_abilities` and `extra_tokens`, parse them, and call `EquipmentTokenSupport` against `Globals.getContext()`.
- **Create** `code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java` — unit tests for the helper (string building, token application happy/error paths, spell validation).

The helper holds all the new logic so it can be unit-tested without booting the MCP server or loading full Pathfinder data. `CustomEquipmentTools` only does argument plumbing.

---

## Task 1: `buildSpellsToken` — construct a SPELLS: line

**Files:**
- Create: `code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java`
- Test: `code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java`

- [ ] **Step 1: Write the failing test**

Create `code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java`:

```java
package pcgen.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EquipmentTokenSupportTest
{
	@Test
	void buildSpellsTokenProducesCanonicalLine()
	{
		String token = EquipmentTokenSupport.buildSpellsToken(
			"Sceptre de Timéon", "Holy Smite", "1", "SceptreCL");
		assertEquals(
			"SPELLS:Sceptre de Timéon|TIMES=1|CASTERLEVEL=SceptreCL|Holy Smite",
			token);
	}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "pcgen.mcp.tools.EquipmentTokenSupportTest"`
Expected: COMPILE FAILURE / FAIL — `EquipmentTokenSupport` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java`:

```java
package pcgen.mcp.tools;

final class EquipmentTokenSupport
{
	private EquipmentTokenSupport()
	{
	}

	/**
	 * Build a SPELLS: token value granting one spell-like ability.
	 * Format per plugin/lsttokens/SpellsLst.java:60 —
	 * SPELLS:&lt;spellbook&gt;|TIMES=&lt;formula&gt;|CASTERLEVEL=&lt;formula&gt;|&lt;spell&gt;
	 *
	 * @param itemId      stable per-item label (the custom item name)
	 * @param spell       spell key
	 * @param timesPerDay integer or formula; -1 = at will
	 * @param casterLevel integer, variable, or formula
	 */
	static String buildSpellsToken(String itemId, String spell, String timesPerDay, String casterLevel)
	{
		return "SPELLS:" + itemId
			+ "|TIMES=" + timesPerDay
			+ "|CASTERLEVEL=" + casterLevel
			+ "|" + spell;
	}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "pcgen.mcp.tools.EquipmentTokenSupportTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java
git commit -m "feat(mcp): add EquipmentTokenSupport.buildSpellsToken"
```

---

## Task 2: `applyTokens` — apply raw LST tokens to an Equipment

**Files:**
- Modify: `code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java`
- Test: `code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java`

- [ ] **Step 1: Write the failing tests**

Add imports and a `@BeforeEach` plus tests to `EquipmentTokenSupportTest.java`. Final file head:

```java
package pcgen.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pcgen.core.Equipment;
import pcgen.persistence.TokenRegistration;
import pcgen.rules.context.ConsolidatedListCommitStrategy;
import pcgen.rules.context.LoadContext;
import pcgen.rules.context.RuntimeLoadContext;
import pcgen.rules.context.RuntimeReferenceContext;
import plugin.lsttokens.BonusLst;
import plugin.lsttokens.DefineLst;

class EquipmentTokenSupportTest
{
	private LoadContext context;

	@BeforeEach
	void setUp()
	{
		TokenRegistration.clearTokens();
		TokenRegistration.register(new DefineLst());
		TokenRegistration.register(new BonusLst());
		context = new RuntimeLoadContext(
			RuntimeReferenceContext.createRuntimeReferenceContext(),
			new ConsolidatedListCommitStrategy());
	}
```

Then add the test methods (keep the existing `buildSpellsTokenProducesCanonicalLine` test):

```java
	@Test
	void applyTokensAppliesValidTokensWithNoFailures()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		List<String> failures = EquipmentTokenSupport.applyTokens(
			context, equip, List.of("DEFINE:SceptreCL|10", "BONUS:VAR|SceptreCL|5"));
		assertTrue(failures.isEmpty(), () -> "unexpected failures: " + failures);
	}

	@Test
	void applyTokensReportsNonTokenString()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		List<String> failures = EquipmentTokenSupport.applyTokens(
			context, equip, List.of("GARBAGE_NO_COLON"));
		assertEquals(1, failures.size());
		assertTrue(failures.get(0).contains("GARBAGE_NO_COLON"));
	}

	@Test
	void applyTokensReportsMalformedTokenValue()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		// DEFINE requires name|formula; a bare name is rejected by the parser.
		List<String> failures = EquipmentTokenSupport.applyTokens(
			context, equip, List.of("DEFINE:OnlyAName"));
		assertFalse(failures.isEmpty());
	}

	@Test
	void applyTokensReturnsEmptyForNull()
	{
		Equipment equip = new Equipment();
		equip.setName("Test Item");
		assertTrue(EquipmentTokenSupport.applyTokens(context, equip, null).isEmpty());
	}
```

> Token registration uses `pcgen.persistence.TokenRegistration` (`clearTokens()` + `register(...)`),
> the same API as the unit-test base `plugin/lsttokens/testsupport/AbstractGlobalTokenTestCase.java`.
> The behaviour under test is the failures-list contract, independent of any specific token.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "pcgen.mcp.tools.EquipmentTokenSupportTest"`
Expected: COMPILE FAILURE / FAIL — `applyTokens` does not exist.

- [ ] **Step 3: Write minimal implementation**

Add to `EquipmentTokenSupport.java` (new imports + method):

```java
import java.util.ArrayList;
import java.util.List;

import pcgen.core.Equipment;
import pcgen.rules.context.LoadContext;
```

```java
	/**
	 * Apply a list of raw LST token strings ("NAME:value") to the equipment using the same
	 * parser + validation as data loading. Returns a list of human-readable failure messages;
	 * an empty list means every token applied. Tokens that parse are committed onto the item.
	 */
	static List<String> applyTokens(LoadContext context, Equipment equip, List<String> tokens)
	{
		List<String> failures = new ArrayList<>();
		if (tokens == null || tokens.isEmpty())
		{
			return failures;
		}
		for (String raw : tokens)
		{
			int colon = raw.indexOf(':');
			if (colon < 0)
			{
				failures.add(raw + " (not a NAME:value token)");
				continue;
			}
			String name = raw.substring(0, colon);
			String value = raw.substring(colon + 1);
			try
			{
				if (!context.processToken(equip, name, value))
				{
					failures.add(raw + " (rejected by token parser)");
				}
			}
			catch (Exception e)
			{
				String m = e.getMessage();
				failures.add(raw + " (" + (m != null ? m : e.getClass().getSimpleName()) + ")");
			}
		}
		context.commit();
		return failures;
	}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "pcgen.mcp.tools.EquipmentTokenSupportTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java
git commit -m "feat(mcp): apply raw LST tokens to equipment via LoadContext"
```

---

## Task 3: `applySpellAbilities` — validate spells and grant them

**Files:**
- Modify: `code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java`
- Test: `code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java`

- [ ] **Step 1: Write the failing tests**

Add imports to the test file:

```java
import java.util.Map;

import pcgen.core.spell.Spell;
import plugin.lsttokens.SpellsLst;
```

In `setUp()`, also register the SPELLS token (add after the `BonusLst` registration):

```java
		TokenRegistration.register(new SpellsLst());
```

Add tests:

```java
	@Test
	void applySpellAbilitiesReportsUnknownSpell()
	{
		Equipment equip = new Equipment();
		equip.setName("Sceptre");
		List<String> failures = EquipmentTokenSupport.applySpellAbilities(
			context, equip, "Sceptre",
			List.of(Map.of("spell", "No Such Spell",
				"times_per_day", "1", "caster_level", "10")));
		assertEquals(1, failures.size());
		assertTrue(failures.get(0).contains("No Such Spell"));
	}

	@Test
	void applySpellAbilitiesGrantsKnownSpell()
	{
		context.getReferenceContext().constructCDOMObject(Spell.class, "Holy Smite");
		Equipment equip = new Equipment();
		equip.setName("Sceptre");
		List<String> failures = EquipmentTokenSupport.applySpellAbilities(
			context, equip, "Sceptre",
			List.of(Map.of("spell", "Holy Smite",
				"times_per_day", "1", "caster_level", "SceptreCL")));
		assertTrue(failures.isEmpty(), () -> "unexpected failures: " + failures);
	}

	@Test
	void applySpellAbilitiesReportsMissingField()
	{
		Equipment equip = new Equipment();
		equip.setName("Sceptre");
		List<String> failures = EquipmentTokenSupport.applySpellAbilities(
			context, equip, "Sceptre",
			List.of(Map.of("spell", "Holy Smite", "times_per_day", "1")));
		assertEquals(1, failures.size());
	}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "pcgen.mcp.tools.EquipmentTokenSupportTest"`
Expected: COMPILE FAILURE / FAIL — `applySpellAbilities` does not exist.

- [ ] **Step 3: Write minimal implementation**

Add to `EquipmentTokenSupport.java` (new imports + method):

```java
import java.util.Map;

import pcgen.core.spell.Spell;
```

```java
	/**
	 * Validate each spell exists in the loaded data, build a SPELLS: line per ability, and apply
	 * them. Each ability map must contain string entries "spell", "times_per_day" and
	 * "caster_level". Returns failure messages (empty == all granted).
	 */
	static List<String> applySpellAbilities(LoadContext context, Equipment equip, String itemId,
		List<Map<String, Object>> abilities)
	{
		List<String> failures = new ArrayList<>();
		if (abilities == null || abilities.isEmpty())
		{
			return failures;
		}
		List<String> spellTokens = new ArrayList<>();
		for (Map<String, Object> ability : abilities)
		{
			Object spell = ability.get("spell");
			Object times = ability.get("times_per_day");
			Object casterLevel = ability.get("caster_level");
			if (spell == null || times == null || casterLevel == null)
			{
				failures.add(ability + " (spell, times_per_day and caster_level are required)");
				continue;
			}
			Spell found = context.getReferenceContext()
				.silentlyGetConstructedCDOMObject(Spell.class, spell.toString());
			if (found == null)
			{
				failures.add(spell + " (spell not found in loaded data)");
				continue;
			}
			spellTokens.add(buildSpellsToken(itemId, found.getKeyName(),
				times.toString(), casterLevel.toString()));
		}
		failures.addAll(applyTokens(context, equip, spellTokens));
		return failures;
	}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "pcgen.mcp.tools.EquipmentTokenSupportTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java code/src/utest/pcgen/mcp/tools/EquipmentTokenSupportTest.java
git commit -m "feat(mcp): validate and grant spell-like abilities to equipment"
```

---

## Task 4: Wire `spell_abilities` + `extra_tokens` into `customize_equipment`

**Files:**
- Modify: `code/src/java/pcgen/mcp/tools/CustomEquipmentTools.java:102-126` (schema) and `:204-225` (invocation)

- [ ] **Step 1: Extend the tool JSON schema**

In `CustomEquipmentTools.customizeEquipment`, inside the `"properties"` object of the schema text block (currently ending after the `"choices"` property at line 122), add two properties after `choices`:

```json
								"spell_abilities": {
									"type": "array",
									"description": "Activated spell-like abilities granted by the item. Each entry needs spell (key), times_per_day (int or formula; -1=at will) and caster_level (int, variable or formula).",
									"items": {
										"type": "object",
										"properties": {
											"spell": { "type": "string" },
											"times_per_day": { "type": "string" },
											"caster_level": { "type": "string" }
										},
										"required": ["spell", "times_per_day", "caster_level"]
									}
								},
								"extra_tokens": {
									"type": "array",
									"items": { "type": "string" },
									"description": "Raw LST token strings applied to the item (e.g. 'DEFINE:SceptreCL|10', 'BONUS:VAR|SceptreCL|5|PREEQUIP:1,Diadème de Lyra', 'SPROP|...')."
								}
```

(Add a comma after the `choices` property's closing brace so the JSON stays valid.)

- [ ] **Step 2: Apply the new params during item creation**

In the lambda, locate the block (around lines 205-212):

```java
						if (customName != null && !customName.isBlank())
						{
							builder.setName(customName);
						}

						Equipment finalEquip = (Equipment) builder.getEquipment();

						character.getDataSet().addEquipment(finalEquip);
```

Replace it with:

```java
						if (customName != null && !customName.isBlank())
						{
							builder.setName(customName);
						}

						Equipment finalEquip = (Equipment) builder.getEquipment();

						String itemId = (customName != null && !customName.isBlank())
							? customName : finalEquip.toString();
						pcgen.rules.context.LoadContext loadContext = Globals.getContext();

						@SuppressWarnings("unchecked")
						List<Map<String, Object>> spellAbilities = args.containsKey("spell_abilities")
							? (List<Map<String, Object>>) args.get("spell_abilities") : null;
						failed.addAll(EquipmentTokenSupport.applySpellAbilities(
							loadContext, finalEquip, itemId, spellAbilities));

						@SuppressWarnings("unchecked")
						List<String> extraTokens = args.containsKey("extra_tokens")
							? (List<String>) args.get("extra_tokens") : null;
						failed.addAll(EquipmentTokenSupport.applyTokens(
							loadContext, finalEquip, extraTokens));

						character.getDataSet().addEquipment(finalEquip);
```

The existing result-building code already surfaces a non-empty `failed` list under
`"failedModifiers"`, so token/spell failures are reported to the caller without further change.

- [ ] **Step 3: Compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run the existing unit suite + the helper tests**

Run: `./gradlew test --tests "pcgen.mcp.tools.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add code/src/java/pcgen/mcp/tools/CustomEquipmentTools.java
git commit -m "feat(mcp): customize_equipment accepts spell_abilities and extra_tokens"
```

---

## Task 5: End-to-end verification — rebuild the Sceptre de Timéon via the live MCP

There is no MCP integration-test harness in this repo, so the engine-level wiring (reference
resolution of granted spells, `PREEQUIP` evaluation) is verified end-to-end against the running
server using the real Pathfinder data.

- [ ] **Step 1: Open Miros via the MCP**

Using the MCP tools (Claude Code MCP session):
- `load_sources(game_mode="Pathfinder_RPG", campaigns=["Core Rulebook"])`
- `open_character(file_path=".../characters/Miros.pcg")` → note the returned `character_id`.

- [ ] **Step 2: Create the fully-computed Sceptre**

Call `customize_equipment` with:

```jsonc
{
  "character_id": "<id>",
  "equipment_key": "Morningstar",
  "custom_name": "Sceptre de Timéon (complet)",
  "modifier_keys": [
    "Special Ability ~ +4 ~ Weapon",
    "Special Ability ~ Holy ~ Weapon",
    "Special Ability ~ Bane ~ Weapon"
  ],
  "choices": { "Special Ability ~ Bane ~ Weapon": ["Evil Outsider Bane"] },
  "spell_abilities": [
    { "spell": "Holy Smite",   "times_per_day": "1", "caster_level": "SceptreCL" },
    { "spell": "Holy Word",    "times_per_day": "1", "caster_level": "SceptreCL" },
    { "spell": "Holy Sword",   "times_per_day": "1", "caster_level": "SceptreCL" },
    { "spell": "Holy Javelin", "times_per_day": "1", "caster_level": "SceptreCL" }
  ],
  "extra_tokens": [
    "DEFINE:SceptreCL|10",
    "BONUS:VAR|SceptreCL|5|PREEQUIP:1,Diadème de Lyra",
    "BONUS:VAR|SceptreDispelEvil|2|PREALIGN:LG,NG,CG,LN,TN,CN",
    "SPROP|+2 sainteté aux tests de NLS pour dissiper la magie du Mal"
  ]
}
```

Expected: `status: ok`, empty `failedModifiers`. If any `spell not found` appears, fix the spell
key (verify with: `grep -rhiE "^Holy (Smite|Word|Sword|Javelin)" data/pathfinder --include=*.lst`).

- [ ] **Step 3: Add, equip, save and inspect**

- `buy_equipment(character_id, "Sceptre de Timéon (complet)", free=true)`
- `equip_item(character_id, "Sceptre de Timéon (complet)", slot="Equipped")` (or via GUI; both
  hands may be full — that is expected and not a failure of this feature).
- `save_character(character_id)`
- Inspect the saved `.pcg`:

Run: `grep -n "Sceptre de Timéon (complet)" characters/Miros.pcg`
Expected: an `EQUIPNAME:` line whose `CUSTOMIZATION` block contains the `SPELLS`, `DEFINE`,
`BONUS` and `SPROP` data (serialised as CDOM/EQMOD tokens).

- [ ] **Step 4: Confirm the spell-like abilities resolve on the character**

Run: `get_character_sheet(character_id, format="markdown")` (MCP) and confirm Holy Smite / Holy
Word / Holy Sword / Holy Javelin appear as item-granted spell-like abilities at caster level 10.

If the granted spells do NOT appear (unresolved reference at runtime), add reference resolution to
`EquipmentTokenSupport.applyTokens` immediately after `context.commit();`:

```java
		context.getReferenceContext().resolveReferences(null);
```

Then re-run `./gradlew test --tests "pcgen.mcp.tools.*"` (the helper tests must still pass) and
repeat Steps 2-4. Commit the fix:

```bash
git add code/src/java/pcgen/mcp/tools/EquipmentTokenSupport.java
git commit -m "fix(mcp): resolve granted-spell references after committing tokens"
```

- [ ] **Step 5: Confirm the conditional caster level (optional, strongest check)**

If a "Diadème de Lyra" item exists in loaded data (or is created the same way), equip both and
confirm via the character sheet that the spell-like abilities' caster level rises to 15 and their
DCs/damage update. This exercises the `PREEQUIP`-gated `BONUS:VAR|SceptreCL|5`. If the item does
not exist in data, this step is informational only — the unit tests already prove the token is
applied; the engine evaluates `PREEQUIP` natively.

---

## Self-Review

**Spec coverage:**
- `spell_abilities` param → Tasks 1, 3, 4. ✓
- `extra_tokens` param → Tasks 2, 4. ✓
- `LoadContext.processToken` + `commit` mechanism → Task 2. ✓
- Spell existence validation, failure reporting → Task 3; non-blocking `failed` list → Task 4. ✓
- Worked example (Sceptre, incl. conditional CL & dispel bonus) → Task 5. ✓
- "no engine / save-format change" → honoured; only `mcp/tools` touched. ✓

**Placeholder scan:** No TBD/TODO. The two conditional items (exact token-registration call in the
unit test; optional `resolveReferences` fallback) include exact code and a concrete decision
criterion, not vague instructions.

**Type consistency:** Helper signatures are stable across tasks — `buildSpellsToken(String,
String, String, String)`, `applyTokens(LoadContext, Equipment, List<String>) -> List<String>`,
`applySpellAbilities(LoadContext, Equipment, String, List<Map<String,Object>>) -> List<String>`.
`customize_equipment` calls match these exactly. `failed` is the existing `List<String>` in
`CustomEquipmentTools`.

**Open verification (flagged, resolved during execution):**
- Exact unit-test token-registration API — mirror `AbstractGlobalTokenTestCase` (Task 2 note).
- Runtime reference resolution for granted spells — Task 5 Step 4 fallback with exact code.
