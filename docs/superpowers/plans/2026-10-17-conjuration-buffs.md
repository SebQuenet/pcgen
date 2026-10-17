# Conjuration Buffs — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Conjured creatures become real PCGen creatures built from their race, so group and per-creature temporary bonuses (Prayer, Haste, Bull's Strength, Inspire Courage…) are counted by PCGen's own bonus engine.

**Architecture:** The catalog text shrinks to `summon: <race key>` + `template:`. A `SummonedCreatureBuilder` in `pcgen.core.conjurations` builds a transient `PlayerCharacter` (race, `<race> Default` kit, templates, `Augment Summoning` when the conjurer has the feat, then temporary bonuses) and reads its numbers through export tokens into a `CreatureStats` record. The fight state (`ConjurationSession`) gains group bonuses and per-creature bonuses, saved in two new `.pcg` lines. The page shows computed stats and bonus chips (mockup A); the bridge returns refreshed stats as JSON so the page never redraws.

**Tech Stack:** Java 25, JUnit 5, Gradle wrapper, FreeMarker, JavaFX WebView bridge, Jackson (`com.fasterxml.jackson.core:jackson-databind`, already a dependency).

**Spec:** `docs/superpowers/specs/2026-10-17-conjuration-buffs-design.md` (amends `specs/conjurations.md`)

## Global Constraints

- Checkstyle: Allman braces, no star imports, max line 201, braces always, newline at EOF; never `System.exit()`.
- Records validate in their compact constructor and throw `IllegalArgumentException`, like the existing `pcgen.core.conjurations` records; services return `ServiceResult`, never throw.
- The page writes only through `textContent` and attributes, never `innerHTML`; the template keeps `<#ftl output_format="HTML">`.
- Building a creature never opens a dialog: a `HeadlessUIDelegate` answers choices, as in `LanguageCompanionService.addCompanion`.
- A transient creature never stays in `Globals.getPCList()` and is never saved as a character.
- Bonuses of the conjurer never reach a creature, and the reverse.
- No MCP/HTTP operation writes bonuses (`specs/conjurations.md` decision 4).
- Real-data tests load `Pathfinder_RPG` with campaigns `Core Rulebook` and `Bestiary`, via `TestHelper.loadGameModes` + `PcgenSession.loadSources`, as `PseudoTranslationTest` does; never a mock of PCGen.
- Do not commit unless the user asks (user rule); "Commit" steps below mean "stage and stop".

## Review Focus

1. Two catalog entries sharing a race with different templates (Celestial vs Fiendish Eagle) must stay distinct creatures with distinct bonuses — Task 1 test `sameRaceWithDifferentTemplatesAreTwoEntries`.
2. A saved bonus whose source vanished from the loaded sources must be dropped with a warning, not crash the load — Task 3 test `dropsABonusWhoseSourceIsNotLoaded`.
3. Removing a group bonus must not remove the same source applied to one creature on its own — Task 2 test `removingAGroupBonusKeepsTheSameBonusOnACreature`.
4. Two luck bonuses (Prayer + a second luck source) must not stack — Task 5 test `twoLuckBonusesDoNotStack`.
5. A creature conjured after a group bonus was set arrives with it — Task 6 test `aCreatureConjuredLaterReceivesTheGroupBonuses`.

---

## File Structure

- `core/conjurations/SummonTemplate.java` — modify: `(String raceKey, List<String> templateKeys, int line)`, derived `name()`.
- `core/conjurations/SummonCatalogParser.java` — modify: only `summon:` and `template:`.
- `core/conjurations/SummonField.java` — delete; `SummonCatalog.roles()` — delete.
- `core/conjurations/BonusSourceKey.java` — create: identity of a bonus source in the loaded data.
- `core/conjurations/AppliedBonus.java` — create: a source key plus its chosen value.
- `core/conjurations/ConjuredCreature.java`, `ConjurationSession.java` — modify: bonuses.
- `core/conjurations/SummonedCreatureBuilder.java` — create: builds a transient creature.
- `core/conjurations/CreatureStats.java` — create: numbers read from a built creature.
- `core/conjurations/CatalogResolution.java` — create: checks race/template keys against loaded data.
- `core/bonus/TemporaryBonusSources.java` — create: lists temp-bonus sources (moved out of `CharacterFacadeImpl`).
- `core/bonus/TemporaryBonusApplier.java` — create: applies a source's bonuses to a character without a dialog.
- `io/PCGVer2Creator.java`, `io/PCGVer2Parser.java`, `io/IOConstants.java` — modify: two lines.
- `io/conjurations/ConjurationOutputModel.java`, `outputsheets/conjurations/conjurations.htm.ftl` — modify.
- `facade/core/ConjurationSheetFacade.java`, `gui2/facade/ConjurationSheetFacadeImpl.java`, `gui2/tabs/conjurations/ConjurationBridge.java` — modify.
- `session/service/ConjurationService.java`, `session/service/model/ConjurationSheet.java` (+ `ConjuredCreatureEntry`) — modify.
- `characters/Shalendra-12.pcg`, `specs/conjurations.md` — modify.

All Java paths are under `code/src/java/pcgen/`.

---

### Task 1: Catalog names a race and its templates

**Files:**
- Modify: `core/conjurations/SummonTemplate.java`, `SummonCatalog.java`, `SummonCatalogParser.java`
- Delete: `core/conjurations/SummonField.java`
- Modify (compile only): `io/conjurations/ConjurationOutputModel.java`, `outputsheets/conjurations/conjurations.htm.ftl` — drop `fields`, `roles`, `columns`, `details`, `cardFields` and the role filter; catalog rows and cards show only the name until Task 7
- Test: `code/src/utest/pcgen/core/conjurations/SummonCatalogParserTest.java` (rewrite), fix `ConjurationCharacterTest`, `ConjurationSheetRenderTest`, `ConjurationServiceTest`, `ConjurationSheetFacadeImplTest`, `BasicSaveRestoreTest` fixtures to the new syntax

**Interfaces:**
- Produces: `record SummonTemplate(String raceKey, List<String> templateKeys, int line)` with `String name()` = race key, plus `" (" + String.join(", ", templateKeys) + ")"` when templates exist; `SummonCatalog.find(String name)` unchanged (case-insensitive on `name()`).

- [ ] **Step 1: Write the failing tests** in `SummonCatalogParserTest`:
  - `readsARaceWithoutTemplate`: `"## SM IV\nsummon: Archon (Hound)\n"` → one entry, `raceKey()` `"Archon (Hound)"`, `templateKeys()` empty, `name()` `"Archon (Hound)"`, `line()` 2.
  - `readsTemplatesSeparatedByCommas`: `summon: Eagle` / `  template: Celestial Creature, Augment Summoning` → `templateKeys()` `["Celestial Creature", "Augment Summoning"]`, `name()` `"Eagle (Celestial Creature, Augment Summoning)"`.
  - `sameRaceWithDifferentTemplatesAreTwoEntries`: Eagle with `Celestial Creature` and Eagle with `Fiendish Creature` → success, two entries.
  - `refusesTheSameNameTwice`: two bare `summon: Eagle` → failure, error on line of the second.
  - `refusesAnOldField`: `  hp: 51` under a summon → failure naming `hp` on its line.
  - Keep the existing structural failure tests (no group first, indented line without summon, empty group, no group at all), adapted.
- [ ] **Step 2: Run** `./gradlew test --tests "pcgen.core.conjurations.SummonCatalogParserTest"` — expected: compilation failure.
- [ ] **Step 3: Implement** the new record and parser (sub-key set `{template}`; `template:` written twice is an error, like a doubled field today), delete `SummonField`, `SummonCatalog.roles()`, `SummonTemplate.field/maximumHitPoints`; adapt the output model and template to compile.
- [ ] **Step 4: Run** `./gradlew test` and `./gradlew slowtest --tests "pcgen.*Conjuration*"` — expected: PASS.
- [ ] **Step 5: Stage.**

### Task 2: Fight state carries bonuses

**Files:**
- Create: `core/conjurations/BonusSourceKey.java`, `core/conjurations/AppliedBonus.java`
- Modify: `core/conjurations/ConjuredCreature.java`, `core/conjurations/ConjurationSession.java`
- Test: `code/src/utest/pcgen/core/conjurations/ConjurationSessionTest.java`, new `BonusSourceKeyTest.java`

**Interfaces:**
- Produces:
  - `record BonusSourceKey(String kind, String category, String key)`; kinds in `public static final` constants `SPELL`, `ABILITY`, `TEMPLATE`, `EQUIPMENT`; `category` non-empty only for `ABILITY`. `String text()` → `"SPELL=Prayer"`, `"ABILITY=FEAT=Power Attack"`; `static Optional<BonusSourceKey> parse(String text)`.
  - `record AppliedBonus(BonusSourceKey source, OptionalInt chosenValue)`.
  - `ConjuredCreature(String creature, int number, int damage, OptionalInt roundsLeft, List<AppliedBonus> bonuses)`.
  - `ConjurationSession(int duration, List<ConjuredCreature> conjured, List<AppliedBonus> groupBonuses)`; methods `withGroupBonus(AppliedBonus)`, `withoutGroupBonus(BonusSourceKey)`, `withBonus(String creature, int number, AppliedBonus)`, `withoutBonus(String creature, int number, BonusSourceKey)`. Adding a source already present replaces its value. `isEmpty()` also requires no group bonus.

- [ ] **Step 1: Write the failing tests:** `keyTextRoundTrips` (each kind, `parse(text()).get()` equals the key), `parseRefusesAnUnknownKind`, `addingAGroupBonusTwiceKeepsOne`, `removingAGroupBonusKeepsTheSameBonusOnACreature`, `aCreatureBonusStaysOnThatCreatureOnly`, `dismissingACreatureDropsItsBonuses`, `dismissAllKeepsTheGroupBonuses` (group bonuses outlive the creatures: they apply to the next ones), `aSessionWithOnlyAGroupBonusIsNotEmpty`.
- [ ] **Step 2: Run** `./gradlew test --tests "pcgen.core.conjurations.*"` — expected: compilation failure.
- [ ] **Step 3: Implement;** update every `new ConjuredCreature(...)`/`new ConjurationSession(...)` call site (`git grep`).
- [ ] **Step 4: Run** `./gradlew test` — expected: PASS.
- [ ] **Step 5: Stage.**

### Task 3: Bonuses survive a save

**Files:**
- Modify: `io/IOConstants.java` (`TAG_CONJUREDGROUPBONUS = "CONJUREDGROUPBONUS"`, `TAG_CONJUREDBONUS = "CONJUREDBONUS"`), `io/PCGVer2Creator.java:1690-1715`, `io/PCGVer2Parser.java:1196-1210, 3355-3400`
- Test: `code/src/itest/pcgen/io/BasicSaveRestoreTest.java`

**Interfaces:**
- Line formats (spec §9), fields encoded with `EntityEncoder`: `CONJUREDGROUPBONUS:<key text>|<value or empty>`, `CONJUREDBONUS:<creature name>|<number>|<key text>|<value or empty>`.
- Consumes: `BonusSourceKey.parse`, `TemporaryBonusSources.find(BonusSourceKey)` from Task 5 — **so this task runs after Task 5**; until then the unknown-source check is the only part that depends on it.

- [ ] **Step 1: Write the failing tests:** `conjurationBonusesRoundTrip` (a group bonus with value 2, a creature bonus without value, names containing `|` and `:`), `dropsABonusWhoseSourceIsNotLoaded` (a `CONJUREDGROUPBONUS:SPELL=No Such Spell|` line → loads, no group bonus, one warning), `dropsACreatureBonusForACreatureNotInPlay`.
- [ ] **Step 2: Run** `./gradlew itest --tests "pcgen.io.BasicSaveRestoreTest"` — expected: FAIL.
- [ ] **Step 3: Implement** writing after the `CONJURED` lines and reading in `parseConjuredLines`, attaching creature bonuses after all `CONJURED` lines are read.
- [ ] **Step 4: Run** the same command — expected: PASS.
- [ ] **Step 5: Stage.**

### Task 4: Build a creature from its race

**Files:**
- Create: `core/conjurations/SummonedCreatureBuilder.java`, `core/conjurations/CreatureStats.java`, `core/conjurations/CatalogResolution.java`
- Test: `code/src/test/pcgen/core/conjurations/SummonedCreatureBuilderTest.java` (real data, slowtest)

**Interfaces:**
- Produces:
  - `record CreatureStats(String speed, int maximumHitPoints, int armorClass, int touchArmorClass, int flatFootedArmorClass, List<CreatureAttack> attacks, String combatManeuverBonus, int combatManeuverDefense, String reflex, String fortitude, String will, String initiative)` and `record CreatureAttack(String name, String toHit, String damage)`; values read with `ExportHandler.getTokenString(pc, token)` using the tokens of `outputsheets/d20/fantasy/htmlxml/csheet_llm.toon.ftl` (`HP`, `AC.Total`, `AC.Touch`, `AC.Flatfooted`, `WEAPON.MERGENONE.n.NAME/TOTALHIT/DAMAGE`, `VAR.CMB.INTVAL.SIGN`, `VAR.CMD.INTVAL`, `CHECK.n.TOTAL`, `INITIATIVEMOD`, `MOVEMENT`).
  - `final class SummonedCreatureBuilder` with constructor `(Collection<Campaign> campaigns)` and `PlayerCharacter build(SummonTemplate summon, boolean augmented)`: new `PlayerCharacter(campaigns)`, `setRace`, `Kit.applyKit` of `"<raceKey> Default"` when that kit exists, `addTemplate` for each key, then `Augment Summoning` template when `augmented`. `CreatureStats statsOf(PlayerCharacter creature)`.
  - `static boolean SummonedCreatureBuilder.augments(PlayerCharacter conjurer)` — true when the conjurer has the `FEAT` ability keyed `Augment Summoning`.
  - `CatalogResolution.unresolvedIn(SummonCatalog catalog) → List<SummonParseError>`: one error per unknown race or template key, on the entry's `line()`.

- [ ] **Step 1: Write the failing tests** (`@BeforeAll` loads the sources of Global Constraints):
  - `aHoundArchonHasItsBestiaryNumbers`: AC 19 / touch 10 / flat-footed 19, first attack named `Greatsword` (masterwork) with to-hit `+9/+4` and damage `2d6+3`, HP 39. If PCGen's monster HP rule gives another value, pin PCGen's value and note it in the test name's comment.
  - `augmentSummoningRaisesStrengthAndConstitution`: augmented Hound Archon → to-hit `+11/+6`, damage `2d6+6`, HP 51 (Shalendra's spreadsheet values).
  - `aCelestialEagleCarriesItsTemplate`: `Eagle` with `Celestial Creature` → creature's template list contains it.
  - `buildingLeavesNoCharacterBehind`: `Globals.getPCList()` size unchanged after `build`.
  - `unresolvedRaceIsReportedOnItsLine` / `unresolvedTemplateIsReportedOnItsLine`.
  - `timesTheShalendraCatalog`: builds the 45 entries of Task 9's catalog once, prints the elapsed time (no assertion; the number goes in the task report).
- [ ] **Step 2: Run** `./gradlew slowtest --tests "pcgen.core.conjurations.SummonedCreatureBuilderTest"` — expected: compilation failure.
- [ ] **Step 3: Implement.** Set a `HeadlessUIDelegate` on `ChooserFactory` around the build; remove the creature from `Globals.getPCList()` if the constructor added it.
- [ ] **Step 4: Run** the same command — expected: PASS; report the timing.
- [ ] **Step 5: Stage.**

### Task 5: Temporary bonuses on a creature

**Files:**
- Create: `core/bonus/TemporaryBonusSources.java`, `core/bonus/TemporaryBonusApplier.java`
- Modify: `gui2/facade/CharacterFacadeImpl.java:1036-1115` to delegate its scans to `TemporaryBonusSources` (keep its cache)
- Test: `code/src/test/pcgen/core/bonus/TemporaryBonusOnCreatureTest.java` (real data)

**Interfaces:**
- Produces:
  - `TemporaryBonusSources.castableOnOthers() → List<CDOMObject>`: abilities, spells and templates with `ListKey.BONUS_ANYPC` (today's `getGlobalTempBonuses` set).
  - `TemporaryBonusSources.availableTo(PlayerCharacter pc) → List<CDOMObject>`: the pc's own objects, spells and innate spells with any character temp bonus, plus `castableOnOthers()` (today's `refreshAvailableTempBonuses` set).
  - `TemporaryBonusSources.keyOf(CDOMObject) → BonusSourceKey`, `TemporaryBonusSources.find(BonusSourceKey) → Optional<CDOMObject>`.
  - `TemporaryBonusApplier.apply(PlayerCharacter target, CDOMObject source, OptionalInt chosenValue)`: same bonus loop as `TempBonusHelper.applyBonusToCharacter` (`%CHOICE` replaced by the value), target = source creature; no dialog. `TemporaryBonusApplier.choiceRange(PlayerCharacter, CDOMObject) → Optional<IntRange>` from `TEMPVALUE` (`MIN=`, `MAX=`, `TITLE=`), with `record IntRange(int minimum, int maximum, String title)`.
- Consumes: Task 4 `SummonedCreatureBuilder`.

- [ ] **Step 1: Write the failing tests** on a built Hound Archon: `prayerAddsOneToHitAndDamage` (`+10/+5`, `2d6+4`), `bullsStrengthAddsThreeDamageTwoHanded` (`+11/+6`, `2d6+6`), `hasteAddsAnAttackAndADodgePoint` (AC 20, to-hit `+10/+10/+5`), `twoLuckBonusesDoNotStack` (Prayer + Divine Favor with value 3, both `TYPE=Luck` at +1 → to-hit `+10/+5`, not `+11/+6`), `inspireCourageUsesTheChosenValue` (value 2 → `+11/+6`, `2d6+5`), `keyOfFindRoundTrips` for a spell and a feat, `castableOnOthersHoldsPrayerAndNotRage` and `availableToACreatureHoldsItsOwnAbilities` (pick a bestiary race whose ability has `TEMPBONUS:PC`; if none in Core + Bestiary, use a barbarian-classed character from `AbstractCharacterTestCase`).
- [ ] **Step 2: Run** `./gradlew slowtest --tests "pcgen.core.bonus.TemporaryBonusOnCreatureTest"` — expected: compilation failure.
- [ ] **Step 3: Implement;** rewire `CharacterFacadeImpl`.
- [ ] **Step 4: Run** that test plus `./gradlew test` — expected: PASS (the GUI's temp-bonus list is unchanged).
- [ ] **Step 5: Stage.**

### Task 6: Facade and bridge set bonuses

**Files:**
- Modify: `facade/core/ConjurationSheetFacade.java`, `gui2/facade/ConjurationSheetFacadeImpl.java`, `gui2/tabs/conjurations/ConjurationBridge.java`
- Test: `code/src/test/pcgen/gui2/facade/ConjurationSheetFacadeImplTest.java` (real data for the bonus tests)

**Interfaces:**
- Produces (facade):
  - `record BonusChoice(String key, String label, Optional<IntRange> range)` in `pcgen.facade.core`.
  - `List<BonusChoice> groupBonusChoices()`, `List<BonusChoice> bonusChoices(String creature, int number)`.
  - `Map<String, CreatureStats> addGroupBonus(String key, OptionalInt value)`, `removeGroupBonus(String key)`, `addBonus(String creature, int number, String key, OptionalInt value)`, `removeBonus(String creature, int number, String key)` — each returns the refreshed stats of every creature it changed, keyed `"<name> <number>"`.
  - `CreatureStats statsOf(String creature, int number)`, `CreatureStats baseStatsOf(String creature, int number)` (no bonus: for the struck-through value).
  - `conjure` unchanged in signature; the creature arrives with the current group bonuses.
- Bridge: the same methods, taking `int value` with `-1` meaning no value, and returning JSON strings written with Jackson (`ObjectMapper`).

- [ ] **Step 1: Write the failing tests:** `aCreatureConjuredLaterReceivesTheGroupBonuses` (group Prayer, then conjure → its stats show `+10/+5`), `addingAGroupBonusRefreshesEveryCreature` (two archons → map has both keys), `aCreatureBonusRefreshesOnlyThatCreature`, `groupChoicesHoldPrayer`, `anUnknownKeyChangesNothing` (returns empty map, session unchanged), `bonusesAreSaved` (the character is marked dirty).
- [ ] **Step 2: Run** `./gradlew slowtest --tests "pcgen.gui2.facade.ConjurationSheetFacadeImplTest"` — expected: FAIL.
- [ ] **Step 3: Implement.** The facade rebuilds a creature from session state (`SummonTemplate` + `augments(conjurer)` + group bonuses + its bonuses) whenever asked; no cache in this task.
- [ ] **Step 4: Run** the same command — expected: PASS.
- [ ] **Step 5: Stage.**

### Task 7: The page shows computed stats and bonus chips

**Files:**
- Modify: `io/conjurations/ConjurationOutputModel.java`, `outputsheets/conjurations/conjurations.htm.ftl`, `code/src/java/pcgen/gui2/tabs/conjurations/*` only if the bridge wiring needs it, language bundles (`LanguageBundle.properties`, `LanguageBundle_fr.properties`: `in_conjurations_group` = "Whole group" / "Tout le groupe", `in_conjurations_own_bonuses` = "Its bonuses" / "Ses bonus", `in_conjurations_add_bonus` = "+ bonus")
- Test: `code/src/test/pcgen/io/conjurations/ConjurationSheetRenderTest.java`

**Interfaces:**
- Consumes: Task 4 builder and stats, Task 6 facade methods (through `window.pcgen`).
- Model additions: per catalog entry `stats` (arrival values, augmented when the conjurer has the feat); per card `stats`, `baseStats`, `bonuses` (`key`, `label`); top-level `groupBonuses`.
- Page (mockup A): group strip above the cards with a chip per group bonus (× removes), `+ bonus` opening an `<input list>` + `<datalist>` filled from `groupBonusChoices()` / `bonusChoices(...)` (a number input appears when the choice has a range); each card shows its own chips; a value differing from `baseStats` renders as `<span class="was">old</span><span class="buffed">new</span>` (classes already in the tactical sheet's palette — add them here). Repaint cards from the JSON the bridge returns.

- [ ] **Step 1: Write the failing tests:** `catalogRowShowsComputedArmorClass` (Hound Archon row contains `19`), `aBuffedCardStrikesTheOldValue` (session with group Prayer → card contains `class="was"` with `+9/+4`), `aGroupBonusChipIsRendered`, `bonusLabelIsEscaped` keeps the existing `<script>`-escaping test meaningful for chip labels, `cloneForExportKeepsBonuses`.
- [ ] **Step 2: Run** `./gradlew slowtest --tests "pcgen.io.conjurations.ConjurationSheetRenderTest"` — expected: FAIL.
- [ ] **Step 3: Implement.** If Task 4's timing for 45 entries exceeds 3 s, memoise catalog stats in the output model keyed by `(catalog text, campaign keys, augmented)` in a single-entry cache; otherwise none.
- [ ] **Step 4: Run** the same command — expected: PASS. Then `./gradlew run`, open Shalendra, check by eye: group Prayer, Bull's Strength on archon 2, conjure a third archon, save, reopen.
- [ ] **Step 5: Stage.**

### Task 8: Service reports races and bonuses

**Files:**
- Modify: `session/service/ConjurationService.java`, `session/service/model/ConjurationSheet.java`, `ConjuredCreatureEntry.java`, the operation help text for `set_conjuration_sheet` (new syntax)
- Test: `code/src/test/pcgen/session/service/ConjurationServiceTest.java`

**Interfaces:**
- `set_conjuration_sheet` also refuses a text with `CatalogResolution.unresolvedIn` errors, naming their lines.
- `ConjurationSheet(String source, int duration, List<ConjuredCreatureEntry> conjured, List<String> groupBonuses)`; `ConjuredCreatureEntry` gains `List<String> bonuses` (key texts).

- [ ] **Step 1: Write the failing tests:** `refusesAnUnknownRace` (error names the line), `getReportsGroupAndCreatureBonuses`.
- [ ] **Step 2: Run** `./gradlew slowtest --tests "pcgen.session.service.ConjurationServiceTest"` — expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** that test and `./gradlew test --tests "pcgen.session.api.OperationRegistryTest"` — expected: PASS.
- [ ] **Step 5: Stage.**

### Task 9: Shalendra and the spec

**Files:**
- Modify: `characters/Shalendra-12.pcg` (through `set_conjuration_sheet` on a running server, then `save_character`), `specs/conjurations.md` (decisions 2 and 5, §3 syntax, §4 model, §5 lines, §6 page, §10)

- [ ] **Step 1:** Map the 45 entries to race and template keys (`Vulnudaemon`, `Protean (Naunet)`, `Rhinoceros (Woolly)`, `Archon (Hound)`, `Herd Animal (Aurochs)`, celestial/fiendish ones with `Celestial Creature` / `Fiendish Creature`…); list in the task report every entry whose key is a guess.
- [ ] **Step 2:** Write the catalog; `get_conjuration_sheet` returns it with no error.
- [ ] **Step 3:** Update `specs/conjurations.md` to match the design document.
- [ ] **Step 4: Run** `./gradlew test itest` and the conjuration slow tests — expected: PASS.
- [ ] **Step 5: Stage.**

Order: 1, 2, 4, 5, 3, 6, 7, 8, 9.
