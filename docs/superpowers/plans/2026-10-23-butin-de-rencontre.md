# Butin de rencontre — plan de réalisation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** À la fin d'un combat, la table de jeu dresse le butin (objets portés + trésor tiré selon
*Ultimate Equipment*), le garde dans le scénario et l'affiche sous le groupe, lignes cochables, avec
valeur et revente.

**Architecture:** Le calcul vit dans `pcgen.core.gametable`, en petits types immuables (budget,
tirage, objets portés, blocs de butin) assemblés par `GameTableSession.endCombat()`. Le scénario
enregistre les blocs tels qu'affichés. `GameTableService` et `GameTableOperations` les servent à MCP
et HTTP ; `PartyPanel` les affiche en Swing.

**Tech Stack:** Java 25, JUnit 5, Jackson (déjà utilisé par `ScenarioJson`), Swing, Python 3 pour le
script de données.

**Spec:** `docs/superpowers/specs/2026-10-23-butin-de-rencontre-design.md`

## Global Constraints

- Style Checkstyle du projet : accolade ouvrante sur sa propre ligne, accolades toujours, pas
  d'import `*`, ligne ≤ 201 caractères, saut de ligne final.
- Identifiants et commentaires en anglais ; textes affichés dans `LanguageBundle.properties` (anglais)
  et `LanguageBundle_fr.properties` (français), clés préfixées `in_gametable_loot`.
- Montants en pièces d'or entières (`long`), jamais en virgule flottante.
- Budget : progression **moyenne** de la table 12-5 du *Core Rulebook*.
- Revente : moitié de la valeur, valeur pleine pour pièces, gemmes, objets d'art, marchandises.
- Les fiches `.pcg` des PJ ne sont jamais modifiées.
- Travail dans l'arborescence principale, pas de worktree. **Aucun commit** sans demande explicite de
  l'utilisateur : chaque tâche s'arrête sur des tests verts, l'utilisateur commite.
- Les fichiers `GameTableSession.java`, `Combat.java`, `ScenarioJson.java`, `GameTableService.java`
  portent déjà des modifications non commitées à la date du plan : demander à l'utilisateur de les
  commiter avant la tâche 2.
- Préfixer les commandes shell par `rtk`.

## Review Focus

- Un monstre sans PV suivis ou vivant (fuite, allié du MJ) : ses lignes existent, décochées.
- Une créature conjurée encore dans le combat : aucune ligne.
- Un FP fractionnaire (« 1/2 ») ou supérieur à 20 : budget défini, pas d'exception.
- Un scénario `.pcgtable` écrit avant cette fonction : il s'ouvre, butin vide, combat sans rencontre.
- Un PNJ dont l'inventaire dépasse son budget : aucun tirage, pas de montant négatif.

Chaque ligne a son test dans la tâche indiquée entre crochets : [T1] FP, [T2] ancien scénario,
[T5] budget dépassé, [T6] conjurée et monstre vivant.

---

### Task 1: Types de trésor et budget par FP

**Files:**
- Create: `code/src/java/pcgen/core/gametable/TreasureType.java`
- Create: `code/src/java/pcgen/core/gametable/TreasureBudget.java`
- Test: `code/src/utest/pcgen/core/gametable/TreasureBudgetTest.java`

**Interfaces:**
- Produces:
  - `enum TreasureType { NONE, INCIDENTAL, STANDARD, DOUBLE, TRIPLE, NPC_GEAR }` with
    `static TreasureType parse(String bestiaryText)` and `long apply(long creatureBudget)`.
  - `final class TreasureBudget` with `static long forChallengeRating(String challengeRating)`.

- [ ] **Step 1: Write the failing tests**

```java
@Test void standardIsTheValueOfItsChallengeRating()
{ assertEquals(6_400, TreasureType.STANDARD.apply(TreasureBudget.forChallengeRating("9"))); }
@Test void incidentalIsHalf()       { assertEquals(3_200, TreasureType.INCIDENTAL.apply(6_400)); }
@Test void tripleIsThreeTimes()     { assertEquals(19_200, TreasureType.TRIPLE.apply(6_400)); }
@Test void noneAndNpcGearDrawNothing()
{ assertEquals(List.of(0L, 0L), List.of(TreasureType.NONE.apply(6_400), TreasureType.NPC_GEAR.apply(6_400))); }
@Test void variantsReadAsTheirType()
{
	assertEquals(List.of(TreasureType.DOUBLE, TreasureType.TRIPLE, TreasureType.INCIDENTAL, TreasureType.INCIDENTAL,
		TreasureType.STANDARD, TreasureType.STANDARD, TreasureType.NPC_GEAR, TreasureType.STANDARD),
		Stream.of("double standard", "Triple standard", "half standard", "50% standard", "?", "",
			"NPC Gear (leather armor, other treasure)", "standard (+1 scimitar, other treasure)")
			.map(TreasureType::parse).toList());
}
@Test void aFractionalChallengeRatingTakesItsShareOfTheFirst()   // [Review Focus]
{ assertEquals(200, TreasureBudget.forChallengeRating("1/2")); }
@Test void aboveTwentyKeepsTheValueOfTwenty()                     // [Review Focus]
{ assertEquals(100_000, TreasureBudget.forChallengeRating("23")); }
```

- [ ] **Step 2: Run them to see them fail**

Run: `rtk ./gradlew test --tests "pcgen.core.gametable.TreasureBudgetTest"` — Expected: compilation FAIL.

- [ ] **Step 3: Implement both types**

`TreasureBudget` holds the medium column as a `Map<Integer, Long>`; check it against *Core Rulebook*
p. 399 before typing: 1→400, 2→800, 3→1 200, 4→1 700, 5→2 300, 6→3 000, 7→3 900, 8→5 000,
9→6 400, 10→8 200, 11→10 500, 12→13 500, 13→17 500, 14→22 000, 15→29 000, 16→38 000, 17→48 000,
18→62 000, 19→79 000, 20→100 000. « 1/N » → value of 1 divided by N; above 20 → value of 20 (plan
decision: the table stops at 20). `parse` lowercases, strips what follows « ( », then: starts with
« npc gear » → NPC_GEAR, « none » → NONE, « incidental »/« half »/« 50% » → INCIDENTAL, « double » →
DOUBLE, « triple » → TRIPLE, anything else → STANDARD. Multipliers: 0, ½, 1, 2, 3, 0.

- [ ] **Step 4: Run them to see them pass** — same command, Expected: PASS.

---

### Task 2: Blocs de butin dans le scénario

**Files:**
- Create: `code/src/java/pcgen/core/gametable/LootLine.java`, `LootBlock.java`, `LootDraw.java`
- Modify: `code/src/java/pcgen/core/gametable/Scenario.java` (component `List<LootBlock> loot`)
- Modify: `code/src/java/pcgen/core/gametable/Combat.java` (component `List<String> encounters`)
- Modify: `code/src/java/pcgen/core/gametable/ScenarioJson.java`
- Test: `code/src/utest/pcgen/core/gametable/LootBlockTest.java`, `ScenarioJsonTest.java`

**Interfaces:**
- Consumes: `TreasureType` (Task 1).
- Produces:
  - `record LootLine(String name, int quantity, String carrier, Optional<TreasureType> drawnAs, long value, boolean resoldInFull, boolean selected)`
    — `drawnAs` empty means « équipé » ; `long resale()` = value, or value / 2 rounded down unless
    `resoldInFull` ; `LootLine selecting(boolean)`.
  - `record LootDraw(String carrier, String creatureType, TreasureType type, long amount)` — what a
    reroll needs, kept in the block.
  - `record LootBlock(int id, String title, String challengeRating, List<LootLine> lines, List<LootDraw> draws)`
    with `long selectedValue()`, `long selectedResale()`, `long budget()`
    (`TreasureBudget.forChallengeRating(challengeRating)`, the reminder shown in the totals row),
    `long sharePerPlayer(int players)`, `LootBlock selecting(int line, boolean)`,
    `LootBlock withDrawnLines(List<LootLine>)` (keeps carried lines and their boxes, replaces drawn ones).
  - `Scenario.loot()`, `Scenario withLoot(List<LootBlock>)`, `int Scenario.nextLootId()`.
  - `Combat.encounters()`, `Combat withEncounter(String name)`; `Combat.empty()` has none.

- [ ] **Step 1: Write the failing tests**

`LootBlockTest`: `aMagicItemResellsForHalf` (value 9 000 → resale 4 500) ; `coinsResellInFull`
(3 210 → 3 210) ; `anUncheckedLineLeavesTheTotals` (lines 4 350 + 9 000, second unchecked →
`selectedValue()` 4 350) ; `theShareIsTheResaleSplitAmongThePlayers` (11 035 / 4 → 2 758) ;
`aRerollKeepsTheCarriedLinesAndTheirBoxes`.
`ScenarioJsonTest`: extend `prepared()` with one block (one carried line unchecked, one drawn line,
one draw) and a combat `withEncounter("Goblin ambush")` so `aScenarioReadsBackAsItWasWritten` covers
them ; add `aScenarioSavedBeforeLootReadsWithNone` — remove `"loot"` and `combat.encounters` from the
written JSON, read it back, expect `loot()` empty and `combat().encounters()` empty. [Review Focus]

- [ ] **Step 2: Run them to see them fail**

Run: `rtk ./gradlew test --tests "pcgen.core.gametable.LootBlockTest" --tests "pcgen.core.gametable.ScenarioJsonTest"` — Expected: compilation FAIL.

- [ ] **Step 3: Implement the records, the new components and their JSON**

JSON: root array `"loot"`, each block `{id, title, challengeRating, lines:[{name, quantity, carrier,
drawnAs?, value, resoldInFull, selected}], draws:[{carrier, creatureType, type, amount}]}` ; combat
array `"encounters"`. Missing arrays read as empty, as the existing readers do for spells. Update the
13 `new Combat(...)` in `Combat.java` and the one in `ScenarioJson.readCombat`.

- [ ] **Step 4: Run them to see them pass** — same command, Expected: PASS ; then
`rtk ./gradlew test --tests "pcgen.core.gametable.*"` — Expected: PASS (no regression).

---

### Task 3: Type de trésor de chaque race

**Files:**
- Create: `scripts/treasure_types.py`, `scripts/tests/test_treasure_types.py`
- Create (generated): `code/src/resources/pcgen/core/gametable/treasure-types.json`
- Create: `code/src/java/pcgen/core/gametable/TreasureTypes.java`
- Test: `code/src/utest/pcgen/core/gametable/TreasureTypesTest.java`

**Interfaces:**
- Consumes: `TreasureType.parse` (Task 1).
- Produces: `final class TreasureTypes` with `static TreasureTypes bundled()` (reads the resource) and
  `TreasureTypes(Map<String, String> byCreatureName)` ; `TreasureType of(String raceKey, String raceName, String creatureType)`.

- [ ] **Step 1: Write the failing tests**

Python: `test_keeps_the_treasure_of_each_named_creature` — two creatures in, `{"vrock": "standard",
"wolf": "none"}` out (names lowercased, creatures without `treasure` left out).
Java (map injected): `aParenthesisedKeyFindsItsCreature` (`"Demon (Vrock)"` → STANDARD from
`"vrock"`) ; `theKeyItselfComesFirst` ; `anUnknownAnimalHasNone` (`"Bear (Grizzly)"`, type `"Animal"`)
; `anUnknownVerminHasNone` ; `anUnknownOutsiderHasStandard` ; `theBundledFileKnowsTheTreant`
(`bundled().of("Treant", "Sylvanien", "Plant")` → STANDARD).

- [ ] **Step 2: Run them to see them fail**

Run: `rtk python3 -m pytest scripts/tests/test_treasure_types.py` and
`rtk ./gradlew test --tests "pcgen.core.gametable.TreasureTypesTest"` — Expected: FAIL.

- [ ] **Step 3: Write the script, generate the file, write the lookup**

`scripts/treasure_types.py --source ../pcfinder/apps/pcfinder-csr/static-data/creatures.json --output code/src/resources/pcgen/core/gametable/treasure-types.json`,
same argument style as `generate_bestiary.py` ; output sorted, one name per key. Lookup order for a
race key `"A (B)"` : `a (b)`, `b`, `b a`, `a, b`, then the localized `raceName` ; no hit → NONE for
creature type animal or vermin, STANDARD otherwise. Check that `code/src/resources` is on the
classpath (`bundled()` test fails otherwise).

- [ ] **Step 4: Run them to see them pass** — same commands, Expected: PASS.

---

### Task 4: Tables d'*Ultimate Equipment* et tirage

**Files:**
- Create: `code/src/resources/pcgen/core/gametable/treasure-tables.json`
- Create: `code/src/java/pcgen/core/gametable/TreasureTables.java`, `TreasureDraw.java`
- Test: `code/src/utest/pcgen/core/gametable/TreasureDrawTest.java`

**Interfaces:**
- Consumes: `LootLine`, `LootDraw` (Task 2).
- Produces:
  - `record TreasureTables(Map<String, List<String>> familiesByCreatureType, Map<String, List<Lot>> lotsByFamily)`
    with nested `record Lot(long value, List<Item> items)` and `record Item(String name, int quantity, long price, boolean resoldInFull)` ;
    `static TreasureTables bundled()`.
  - `final class TreasureDraw` : `TreasureDraw(TreasureTables tables, RandomGenerator dice)` ;
    `List<LootLine> draw(LootDraw request, boolean selected)`.

- [ ] **Step 1: Transcribe the tables**

Source : *Ultimate Equipment*, chapter « Treasure Generation » (d20pfsrd, « Treasure Generation »
page). `familiesByCreatureType` keys are PCGen race types lowercased (`aberration` … `vermin`),
values letters A–I ; `lotsByFamily` holds every lot of each family with its value and its items at
their printed price. A lot whose content is « roll on table X » is expanded into the items of a fixed
result, so every item carries a price.

- [ ] **Step 2: Write the failing tests** (small tables built in the test, seeded `new Random(42)`)

`theDrawNeverExceedsTheAmount` ; `theRemainderBecomesCoins` (amount 1 050, only lot 1 000 → one
line « Pièces » 50, `resoldInFull`) ; `onlyTheCreaturesFamiliesAreDrawn` ; `aZeroAmountDrawsNothing` ;
`theSameSeedDrawsTheSameLines` ; on the bundled tables : `everyCreatureTypeHasFamilies`,
`everyLotIsWorthItsItems` (sum of price × quantity = lot value).

- [ ] **Step 3: Run them to see them fail**

Run: `rtk ./gradlew test --tests "pcgen.core.gametable.TreasureDrawTest"` — Expected: compilation FAIL.

- [ ] **Step 4: Implement `TreasureDraw.draw`**

```
remaining = request.amount()
loop: lots = lots of the creature type's families with value <= remaining
      if none: break
      lot = lots[dice.nextInt(lots.size())]; add its items as lines (carrier, drawnAs = request.type()); remaining -= lot.value
if remaining > 0: add line "Pièces" (value remaining, resoldInFull)
```

- [ ] **Step 5: Run them to see them pass** — same command, Expected: PASS.

---

### Task 5: Butin d'un monstre construit

**Files:**
- Create: `code/src/java/pcgen/core/gametable/CreatureLoot.java`
- Test: `code/src/test/pcgen/core/gametable/CreatureLootTest.java` (slow test: needs data)

**Interfaces:**
- Consumes: `TreasureTypes`, `TreasureBudget`, `TreasureType` (T1, T3), `LootLine`, `LootDraw` (T2).
- Produces: `final class CreatureLoot` : `CreatureLoot(TreasureTypes types)` ;
  `List<LootLine> carried(PlayerCharacter monster, String carrier, boolean selected)` ;
  `LootDraw draw(PlayerCharacter monster, String carrier, String challengeRating, boolean fromCharacterFile, long carriedValue)`.

- [ ] **Step 1: Write the failing tests**

Build monsters with `MonsterForge` as `GameTableSessionTest` does (`BestiarySources.campaigns()`).
`aCodexNpcGivesItsWholeInventory` — `characters/combat-manager/NPC Codex/Rogue/Contract Killer.pcg` :
the lines' names are the inventory's, each value equals `Equipment.getCost(pc)` × quantity ;
`aWolfCarriesNothingAndDrawsNothing` (amount 0) ; `aTreantDrawsItsStandardValue` (amount
`TreasureBudget.forChallengeRating(cr)`) ; `aCodexNpcDrawsNothing` (`fromCharacterFile` → NPC_GEAR,
amount 0) ; `carriedGoodsAreDeductedAndNeverBelowZero` — carried value above the budget → amount 0
[Review Focus].

- [ ] **Step 2: Run them to see them fail**

Run: `rtk ./gradlew slowtest --tests "pcgen.core.gametable.CreatureLootTest"` — Expected: compilation FAIL.

- [ ] **Step 3: Implement `CreatureLoot`**

`carried` : `monster.getEquipmentMasterList()`, natural weapons and zero-cost items skipped,
quantity from `getQty()`, value `getCost(monster)` × quantity ; `resoldInFull` when the item's type
is Gem, Art, Coin or TradeGood ; plus a « Pièces » line for `monster.getGold()` when above zero.
`draw` : type = NPC_GEAR when `fromCharacterFile`, else `types.of(race key, race name,
display.getRaceType())` ; amount = max(0, `type.apply(budget)` − carriedValue).

- [ ] **Step 4: Run them to see them pass** — same command, Expected: PASS.

---

### Task 6: Fin de combat, relance, cases, suppression

**Files:**
- Modify: `code/src/java/pcgen/core/gametable/GameTableSession.java` (`launch`, `endCombat`, new methods, constructor)
- Modify: `code/src/java/pcgen/gui2/gametable/GameTablePanel.java:89`, `code/src/java/pcgen/session/api/OperationRegistry.java:100` (constructor)
- Test: `code/src/test/pcgen/core/gametable/GameTableSessionTest.java`

**Interfaces:**
- Consumes: Tasks 2–5.
- Produces on `GameTableSession` :
  - constructor `GameTableSession(IntSupplier d20, RandomGenerator treasureDice)` ; the existing
    one-argument constructor delegates with `RandomGenerator.getDefault()` ;
  - `endCombat()` unchanged in signature, appends a `LootBlock` to `scenario().loot()` before
    `forgetBuilt()` ;
  - `void selectLoot(int blockId, int line, boolean selected)`, `void rerollLoot(int blockId)`,
    `void removeLoot(int blockId)` — unknown id or line: `IllegalArgumentException`.

- [ ] **Step 1: Write the failing tests** (session seeded with `new Random(42)`)

`endingAFightKeepsItsLoot` — launch WOLVES, kill both, end → one block titled « Wolves in the
woods » ; `aLivingMonstersLinesAreUnchecked` — `add_monsters` of the Contract Killer `.pcg`, left
alive → its lines exist, unchecked [Review Focus] ; `aConjuredCreatureLeavesNoLoot` — summon then end → no line carried by
it [Review Focus] ; `aFightWithoutEncounterHasADefaultTitle` (« Combat sans rencontre », from the
bundle key `in_gametable_lootNoEncounter`) ; `twoFightsMakeTwoBlocks` ; `aRerollKeepsTheBoxes` ;
`removingABlockForgetsIt` ; `theLootSurvivesSavingAndReopening` (alongside the existing reopen test).

- [ ] **Step 2: Run them to see them fail**

Run: `rtk ./gradlew slowtest --tests "pcgen.core.gametable.GameTableSessionTest"` — Expected: FAIL.

- [ ] **Step 3: Implement**

`launch` records the encounter name with `Combat.withEncounter`. `endCombat` : for every combatant of
kind monster without `summonerId`, from its built `PlayerCharacter` : carried lines then draw ;
`selected` = health damage ≥ maximum ; drawn lines from `TreasureDraw` ; title = encounters joined
with « + », or the default ; challenge rating = `Experience.challengeRatingOf(combat.experience())`.
Each method calls `changed()`.

- [ ] **Step 4: Run them to see them pass** — same command, Expected: PASS ; then
`rtk ./gradlew test` — Expected: PASS.

---

### Task 7: MCP et HTTP

**Files:**
- Create: `code/src/java/pcgen/session/service/model/LootBlockEntry.java`, `LootLineEntry.java`
- Modify: `code/src/java/pcgen/session/service/model/GameTableState.java` (component `List<LootBlockEntry> loot`)
- Modify: `code/src/java/pcgen/session/service/model/CombatEnded.java` (component `LootBlockEntry loot`)
- Modify: `code/src/java/pcgen/session/service/GameTableService.java`, `code/src/java/pcgen/session/api/GameTableOperations.java`
- Test: `code/src/utest/pcgen/session/api/OperationRegistryTest.java`

**Interfaces:**
- Consumes: Task 6's session methods.
- Produces: operations `select_loot` (`block_id`, `line`, `selected`), `reroll_loot` (`block_id`),
  `remove_loot` (`block_id`), all `Operation.exclusive`, answering the game table state ;
  `LootLineEntry(name, quantity, carrier, origin, value, resale, selected)` with origin
  `"carried"` or the treasure type lowercased ; `LootBlockEntry(id, title, challengeRating, budget,
  lines, selectedValue, selectedResale, sharePerPlayer)`.

- [ ] **Step 1: Write the failing test** — add `"select_loot", "reroll_loot", "remove_loot"` to the
list in `offersTheGameTable`.

- [ ] **Step 2: Run it to see it fail**

Run: `rtk ./gradlew test --tests "pcgen.session.api.OperationRegistryTest"` — Expected: FAIL on « served: ».

- [ ] **Step 3: Implement** the three service methods (`withSources` like the others, unknown id →
`ServiceResult` failure, never a thrown exception), the operations with their JSON schemas, the
state mapping, and `end_combat`'s description mentioning the loot block.

- [ ] **Step 4: Run it to see it pass** — same command, Expected: PASS.

- [ ] **Step 5: Check the server end to end**

Run `rtk ./gradlew runServer -Pport=8421` (port 8420 is taken by a long NPC import), then `POST /api/open_scenario` with
`characters/ambassadeur-disparu-kyonin.pcgtable`, `launch_encounter` « 4. Le camp du ritualiste »,
`damage_combatant` each monster to 0, `end_combat` — Expected: `loot.lines` lists the ritualist's
inventory and drawn lines for both vrocks, `loot.budget` = 15 000 (FP 14, medium column).

---

### Task 8: Partie Butin de l'onglet Groupe

**Files:**
- Create: `code/src/java/pcgen/gui2/gametable/LootPanel.java`
- Modify: `code/src/java/pcgen/gui2/gametable/PartyPanel.java` (layout A), `CombatPanel.java:495-508` (message mentions the loot)
- Modify: `code/src/resources/pcgen/lang/LanguageBundle.properties`, `LanguageBundle_fr.properties`

**Interfaces:**
- Consumes: `GameTableSession.scenario().loot()`, `selectLoot`, `rerollLoot`, `removeLoot` (Task 6).
- Produces: `final class LootPanel extends JPanel` with `LootPanel(GameTablePanel table)` and
  `void refresh()`, called from `PartyPanel.refresh()`.

- [ ] **Step 1: Build the panel**

`PartyPanel` becomes a vertical split : current list and buttons on top, `LootPanel` below. One
block per `LootBlock` : title with challenge rating, a `JTable` (box, object, carried by, origin,
value, resale ; an unchecked row greyed and struck through), totals row with the budget, share per
player (`scenario().party().size()`), buttons « Relancer le tirage », « Copier la liste »,
« Supprimer le bloc » (confirmation dialog). Grand total at the bottom. « Copier la liste » puts on
the clipboard the checked lines as `name ×quantity — resale po`, then the total and the share. All
labels from bundle keys `in_gametable_loot*`, both languages.

- [ ] **Step 2: Check it in the application**

Run `rtk ./gradlew run`, open `characters/ambassadeur-disparu-kyonin.pcgtable`, play encounter 1 to
its end. Expected: the block appears under the party ; unchecking a line updates both totals ;
« Relancer » changes only drawn lines ; « Copier » pastes the checked lines ; after saving and
reopening the scenario the block and its boxes are back ; nothing scrolls horizontally on a laptop
screen.

- [ ] **Step 3: Run the quality reports**

Run: `rtk ./gradlew test allReports` — Expected: PASS, no new Checkstyle or PMD violation in the
touched files.
