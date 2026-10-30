# Bestiaire filtrable — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** L'onglet Bestiaire de la table de jeu liste races, PNJ en `.pcg`, kits de PNJ et créatures uniques
du scénario, les filtre sur toute valeur de leur fiche, et fait d'une entrée une créature unique gardée dans
le scénario ou dans un `.pcg`.

**Architecture:** Un nouveau paquet `pcgen.core.gametable.catalog` porte le modèle sans Swing : les entrées,
leurs valeurs (`EntryFacts`), les critères et leur évaluation à trois issues, l'index (livré en ressource
pour les races et les kits, local sur disque pour les `.pcg`). `MonsterSource` gagne le cas `NpcKit`.
`BestiaryPanel` devient un tableau filtrable branché sur ce modèle.

**Tech Stack:** Java 25, Swing (`gui2`), Jackson (déjà utilisé par `ScenarioJson`), JUnit 5, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-30-bestiaire-filtrable-design.md`

## Global Constraints

- Style Checkstyle du projet : accolade ouvrante sur sa propre ligne, accolades toujours, pas d'import `*`, lignes ≤ 201 caractères.
- Jamais `System.exit()` ; `GracefulExit.exit()` si besoin.
- Textes d'interface dans `LanguageBundle.properties` et `LanguageBundle_fr.properties`, clés préfixées `in_gametable_`.
- Une construction de créature se fait une à la fois (`MonsterForge.FORGE_LOCK`) ; jamais deux constructions en parallèle.
- Tests rapides (sans données) dans `code/src/utest/`, tests qui chargent les données dans `code/src/test/` (`./gradlew slowtest`), source de test `pcgen.core.conjurations.BestiarySources`.
- Dossiers de PNJ par défaut : `data/homebrew/*/pnj/` et `characters/`, parcourus récursivement.
- Kit de PNJ : un kit des sources chargées dont le `TYPE` contient `NPC`.
- Une entrée non calculée n'est jamais écartée par un filtre sur une valeur calculée : elle est marquée « non calculée ».

## Review Focus

- Un `.pcg` illisible ou supprimé entre deux passages de l'index : l'entrée reste listée, marquée « non construite » avec la raison ; elle ne bloque pas le reste de l'index (test, tâche 6).
- Une race dont la construction lève une exception (données qui exigent un don absent) : `Missing` avec la raison, pas d'erreur qui remonte (test, tâche 1).
- Changer de sources pendant que l'index local tourne : l'index en cours s'arrête et repart sur les nouvelles sources, sans mélanger les deux (test, tâche 6).
- Un préréglage enregistré qui nomme un champ disparu d'une version suivante : le critère est ignoré et signalé, le préréglage s'ouvre (test, tâche 4).
- Un scénario enregistré avant cette fonctionnalité (sans `uniques`) s'ouvre avec une liste vide ; un scénario avec un kit de PNJ dans une rencontre fait l'aller-retour (tests, tâches 2 et 8).

---

### Task 1: Une construction qui échoue donne `Missing`

**Files:**
- Modify: `code/src/java/pcgen/core/gametable/MonsterForge.java` (méthode `forge`)
- Modify: `code/src/resources/pcgen/lang/LanguageBundle.properties`, `LanguageBundle_fr.properties`
- Test: `code/src/test/pcgen/core/gametable/MonsterForgeFailureTest.java`

**Interfaces:**
- Produces: `MonsterForge.forge(MonsterRecipe)` ne lève plus d'exception d'exécution ; elle renvoie `ForgedMonster.Missing(reason)`, `reason` formée par la clé `in_gametable_buildFailed` (« {0} ne se construit pas : {1} », `{0}` = nom de la race ou du fichier, `{1}` = message de l'exception).

- [ ] **Step 1: Write the failing test**

Le test charge `Core Rulebook`, `Bestiary`, `Pathfinder Adventure Path #60 ~ Creatures` (même mécanisme que `BestiarySources`, nouvelle liste de sources) :

```java
@Test
void aRaceWhoseDataFailToBuildIsMissingWithTheReason()
{
	ForgedMonster built = forge.forge(MonsterRecipe.of(new MonsterSource.BestiaryRace("Ugash-iram")));
	ForgedMonster.Missing missing = assertInstanceOf(ForgedMonster.Missing.class, built);
	assertTrue(missing.reason().contains("Ugash-iram"), missing.reason());
}
```

- [ ] **Step 2: Run it and see it fail on the exception**

Run: `./gradlew slowtest --tests "pcgen.core.gametable.MonsterForgeFailureTest"`
Expected: FAIL avec `IllegalArgumentException` ou `IllegalStateException` (si la race se construit avec ces seules sources, remplacer par `Vapor Wasp` avec `Tome of Horrors Complete ~ Creatures`, ou `Reclamation Robot` avec `Pathfinder Adventure Path #88 ~ Creatures`).

- [ ] **Step 3: Catch `RuntimeException` around the construction in `forge`**

Dans le bloc `synchronized`, rattraper `RuntimeException` et renvoyer `Missing` avec `in_gametable_buildFailed`, après avoir journalisé l'exception (`Logging.errorPrint`).

- [ ] **Step 4: Run the test and the existing forge tests**

Run: `./gradlew slowtest --tests "pcgen.core.gametable.MonsterForge*"`
Expected: PASS

- [ ] **Step 5: Commit** — `fix(gametable): report a monster whose data fail to build instead of throwing`

### Task 2: Le kit de PNJ, troisième source d'un monstre

**Files:**
- Modify: `code/src/java/pcgen/core/gametable/MonsterSource.java`
- Modify: `code/src/java/pcgen/core/gametable/MonsterForge.java` (`fromKit`)
- Modify: `code/src/java/pcgen/core/gametable/ScenarioJson.java` (clé `"kit"`)
- Modify: `code/src/java/pcgen/core/gametable/BestiaryPawn.java`, `BestiaryTactics.java` (cas `NpcKit` : pas de pion ni de plan)
- Test: `code/src/test/pcgen/core/gametable/NpcKitForgeTest.java`, `code/src/utest/pcgen/core/gametable/ScenarioJsonTest.java`

**Interfaces:**
- Produces: `record MonsterSource.NpcKit(String kitKey)` (clé non vide). `MonsterForge.forge` construit un personnage vierge (`new PlayerCharacter(campaigns)`, retiré de `Globals.getPCList()`), lui applique le kit par `addKit`, sous le `UIDelegate` de la forge, avec PV moyens comme `touched`. Kit introuvable → `Missing` (`in_gametable_missingKit`).

- [ ] **Step 1: Write the failing tests**

```java
// NpcKitForgeTest, sources: Core Rulebook, Bestiary, Dungeons of Golarion
@Test
void aBookNpcKitBuildsItsCharacter()
{
	String kit = /* first kit of dung_kits_npc.lst, read from the data at test time */;
	ForgedMonster built = forge.forge(MonsterRecipe.of(new MonsterSource.NpcKit(kit)));
	PlayerCharacter npc = assertInstanceOf(ForgedMonster.Forged.class, built).character();
	assertTrue(npc.getDisplay().getTotalLevels() > 0);
}

@Test
void anUnknownKitIsMissing()
{
	assertInstanceOf(ForgedMonster.Missing.class, forge.forge(MonsterRecipe.of(new MonsterSource.NpcKit("No Such Kit"))));
}

// ScenarioJsonTest
@Test
void anEncounterMonsterBuiltFromAKitComesBack()
{
	// encounter with MonsterRecipe.of(new MonsterSource.NpcKit("Guard Captain")) → write → read → equal recipe
}
```

- [ ] **Step 2: Run them and see them fail** — `./gradlew test --tests "*ScenarioJsonTest"` et `./gradlew slowtest --tests "*NpcKitForgeTest"` : échec de compilation sur `NpcKit`.
- [ ] **Step 3: Add `NpcKit`, `fromKit`, the `"kit"` key, and the `NpcKit` cases of every `switch` on `MonsterSource`**
- [ ] **Step 4: Run the tests** — Expected: PASS, et `./gradlew test` sans régression.
- [ ] **Step 5: Commit** — `feat(gametable): build a monster from a book's NPC kit`

### Task 3: Les valeurs d'une entrée et l'évaluation des critères

**Files:**
- Create: `code/src/java/pcgen/core/gametable/catalog/CatalogField.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/FieldValue.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/EntryFacts.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/Criterion.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/Match.java`
- Test: `code/src/utest/pcgen/core/gametable/catalog/CriterionTest.java`

**Interfaces:**
- Produces:
  - `enum CatalogField` : un cas par champ du tableau de la spec, section 5, chacun avec `group()` (`CatalogGroup`), `kind()` (`FieldKind.NUMBER | TEXT | ITEMS | FLAG`), `origin()` (`Origin.DIRECT | COMPUTED`), `labelKey()` (`in_gametable_field_<name>`). L'alignement est deux champs (`LAW_CHAOS`, `GOOD_EVIL`, valeurs `L/N/C` et `G/N/E`). La taille est un `NUMBER` ordonné (Fine = 0 … Colossal = 8).
  - `sealed interface FieldValue` : `Number(int value)`, `Text(String value)`, `Items(Set<String> values)`, `Flag(boolean value)`.
  - `record EntryFacts(Map<CatalogField, FieldValue> values, Optional<String> buildFailure)` ; `Optional<FieldValue> value(CatalogField)`.
  - `sealed interface Criterion` : `Test(CatalogField field, Operator operator, List<String> operands)` et `Group(Joiner joiner, List<Criterion> members)`, `Joiner.ALL | ANY` ; `Match evaluate(EntryFacts facts)`.
  - `enum Operator` : `EQUALS, NOT_EQUALS, AT_LEAST, AT_MOST, BETWEEN, CONTAINS, STARTS_WITH, MATCHES, ANY_OF, ALL_OF, NONE_OF, IS_TRUE, IS_FALSE` ; `boolean appliesTo(FieldKind)`.
  - `enum Match { YES, NO, UNKNOWN }` ; `Match.and(Match)`, `Match.or(Match)`.

Règles d'évaluation (non déterminées par les signatures) :

| Cas | Résultat |
|---|---|
| champ absent des valeurs | `UNKNOWN` (« non calculée ») |
| `ALL` | un `NO` → `NO` ; sinon un `UNKNOWN` → `UNKNOWN` ; sinon `YES` ; groupe vide → `YES` |
| `ANY` | un `YES` → `YES` ; sinon un `UNKNOWN` → `UNKNOWN` ; sinon `NO` ; groupe vide → `YES` |
| texte | comparaison insensible à la casse et aux accents (`Normalizer` NFD sans diacritiques) |
| `MATCHES` | expression régulière invalide → `NO` (l'interface la signale à la saisie, tâche 7) |

- [ ] **Step 1: Write the failing tests** (`CriterionTest`), une assertion logique chacun :
  - `aNumberBetweenItsBoundsMatches` : CR 7, `BETWEEN [5, 8]` → `YES` ; CR 9 → `NO`.
  - `aMissingComputedValueIsUnknown` : pas d'`ARMOR_CLASS`, `AT_LEAST [20]` → `UNKNOWN`.
  - `allOfAnUnknownAndAYesIsUnknown`, `allOfAnUnknownAndANoIsNo`, `anyOfAnUnknownAndAYesIsYes`.
  - `nestedGroupsCombine` : `ALL[ type ANY_OF [Undead], ANY[ fly speed AT_LEAST [1], swim speed AT_LEAST [1] ] ]` sur un mort-vivant volant → `YES`.
  - `textIgnoresCaseAndAccents` : nom « Élémentaire de feu », `CONTAINS ["elementaire"]` → `YES`.
  - `noneOfExcludesASubtype` : sous-types `{Aquatic}`, `NONE_OF [Aquatic]` → `NO`.
  - `anInvalidPatternMatchesNothing` : `MATCHES ["("]` → `NO`.
- [ ] **Step 2: Run** — `./gradlew test --tests "pcgen.core.gametable.catalog.CriterionTest"` → échec de compilation.
- [ ] **Step 3: Implement the five types**
- [ ] **Step 4: Run** — Expected: PASS
- [ ] **Step 5: Commit** — `feat(gametable): describe a bestiary entry's values and evaluate criteria on them`

### Task 4: Préréglages, colonnes et dossiers de PNJ dans les préférences

**Files:**
- Create: `code/src/java/pcgen/core/gametable/catalog/CriterionJson.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/BestiaryPreferences.java`
- Test: `code/src/utest/pcgen/core/gametable/catalog/CriterionJsonTest.java`

**Interfaces:**
- Consumes: `Criterion`, `CatalogField` (tâche 3).
- Produces:
  - `CriterionJson.write(Criterion) -> String`, `CriterionJson.read(String) -> CriterionReading` avec `record CriterionReading(Criterion criterion, List<String> ignoredFields)`.
  - `record Preset(String name, Criterion criterion)`.
  - `BestiaryPreferences` sur `PCGenSettings.OPTIONS_CONTEXT`, clés `gametable.bestiary.presets` (JSON), `gametable.bestiary.columns` (noms de `CatalogField` séparés par `,`), `gametable.npcFolders` (chemins séparés par `File.pathSeparator`) : `List<Preset> presets()`, `void savePresets(List<Preset>)`, `List<CatalogField> columns()` (défaut : `NAME, CHALLENGE_RATING, TYPE, SIZE, ARMOR_CLASS, HIT_POINTS, SOURCE`), `void saveColumns(List<CatalogField>)`, `List<Path> npcFolders()` (défaut de la section Global Constraints, `*` résolu par `Files.newDirectoryStream`), `void saveNpcFolders(List<Path>)`.

- [ ] **Step 1: Write the failing tests**
  - `aNestedCriterionComesBack` : aller-retour d'un `Group` à deux niveaux → égal.
  - `aFieldThatNoLongerExistsIsIgnoredAndReported` : JSON avec `"field": "ENVIRONMENT"` → critère absent, `ignoredFields == ["ENVIRONMENT"]`.
- [ ] **Step 2: Run** — `./gradlew test --tests "*CriterionJsonTest"` → échec.
- [ ] **Step 3: Implement `CriterionJson` (Jackson `ObjectMapper`, comme `ScenarioJson`) and `BestiaryPreferences`**
- [ ] **Step 4: Run** — Expected: PASS
- [ ] **Step 5: Commit** — `feat(gametable): keep the bestiary's presets, columns and NPC folders in the preferences`

### Task 5: Lire et calculer les valeurs d'une entrée

**Files:**
- Create: `code/src/java/pcgen/core/gametable/catalog/DirectFacts.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/ComputedFacts.java`
- Test: `code/src/test/pcgen/core/gametable/catalog/FactsTest.java`

**Interfaces:**
- Consumes: `EntryFacts`, `CatalogField`, `FieldValue` (tâche 3) ; `CombatantSheets.stats/sheet/challengeRating` ; `BestiaryPawn.of`, `BestiaryTactics.of`.
- Produces:
  - `DirectFacts.ofRace(Race race) -> Map<CatalogField, FieldValue>` : champs `Origin.DIRECT` lus sur la race (`RACETYPE`, `RACESUBTYPE`, taille, `CR`, niveau de `MONSTERCLASS`, `MOVE`, `REACH`, `ROLE`, `AUTO:LANG`, capacités `AUTOMATIC`, `SPELLS` innés, `FACT:IsPC`, `SOURCELONG`, pion, plan tactique).
  - `DirectFacts.ofKit(Kit kit)` et `DirectFacts.ofFile(Path pcg)` : nom, livre source, catégorie PNJ, pion, plan tactique.
  - `ComputedFacts.of(PlayerCharacter monster, CombatantSheets sheets) -> Map<CatalogField, FieldValue>` : tous les champs `Origin.COMPUTED`, plus type, sous-types, taille, alignement, FP et DV recalculés sur le personnage (ils valent pour les `.pcg` et les kits).

- [ ] **Step 1: Write the failing tests** (sources `BestiarySources`) :
  - `theAbolethsDirectFactsComeFromItsRaceLine` : `TYPE` contient `Aberration`, `SUBTYPES` contient `Aquatic`, `CHALLENGE_RATING == 7`, `HIT_DICE == 8`, `SWIM_SPEED == 60`, `REACH == 15`.
  - `theBuiltWolfsArmorClassIsItsSheets` : `ComputedFacts.of(wolf).get(ARMOR_CLASS)` = `sheets.stats(wolf).armorClass()` (14 dans le Bestiary).
  - `aCreatureWithoutConstitutionHasNoConValue` : un squelette humain → `NO_CONSTITUTION` = `Flag(true)`.
- [ ] **Step 2: Run** — `./gradlew slowtest --tests "*catalog.FactsTest"` → échec.
- [ ] **Step 3: Implement `DirectFacts` and `ComputedFacts`**
- [ ] **Step 4: Run** — Expected: PASS
- [ ] **Step 5: Commit** — `feat(gametable): read and compute the values the bestiary filters on`

### Task 6: L'index livré et l'index local

**Files:**
- Create: `code/src/java/pcgen/core/gametable/catalog/BestiaryIndex.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/LocalIndexer.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/BestiaryIndexWriter.java` (avec `main`)
- Modify: `build.gradle` (tâche `bestiaryIndex`, `JavaExec` sur `BestiaryIndexWriter`, heap 8 Go)
- Create (généré): `code/src/resources/pcgen/bestiary/index.json`
- Test: `code/src/utest/pcgen/core/gametable/catalog/BestiaryIndexTest.java`, `code/src/test/pcgen/core/gametable/catalog/LocalIndexerTest.java`

**Interfaces:**
- Consumes: `DirectFacts`, `ComputedFacts` (tâche 5), `MonsterForge` (tâches 1-2).
- Produces:
  - `record IndexKey(String kind, String key)` — `kind` ∈ `race | kit | file` ; pour un fichier, `key` = chemin absolu.
  - `BestiaryIndex` : `Optional<EntryFacts> facts(IndexKey)`, `BestiaryIndex with(IndexKey, EntryFacts, long stamp)`, `long stamp(IndexKey)`, `static BestiaryIndex delivered()` (ressource `/pcgen/bestiary/index.json`, vide si absente), `static BestiaryIndex read(Path)`, `void write(Path)`.
  - `LocalIndexer(MonsterForge forge, CombatantSheets sheets, Path cacheFile)` : `void start(List<Path> pcgFiles, IntConsumer progress, Runnable done)`, `void cancel()`, `BestiaryIndex current()`. Un seul fil ; il saute un fichier dont la date (`Files.getLastModifiedTime`) égale le `stamp` gardé ; une construction `Missing` donne `EntryFacts(Map.of(), Optional.of(reason))`.
  - Cache local : `<dossier des réglages PCGen>/gametable-bestiary-index.json`.
  - `BestiaryIndexWriter.main` : charge les sources de `characters/tous-les-monstres.pcg`, construit chaque race et chaque kit de PNJ, écrit l'index livré.

- [ ] **Step 1: Write the failing tests**
  - `BestiaryIndexTest.anIndexComesBackFromItsFile` : aller-retour `write`/`read` d'une entrée avec chaque sorte de `FieldValue`.
  - `LocalIndexerTest.anUnchangedFileIsNotBuiltAgain` : deux `start` sur le même `.pcg` → le second ne construit rien (compteur de constructions passé par un `MonsterForge` réel, compté par le `progress` des fichiers construits).
  - `LocalIndexerTest.aChangedFileIsBuiltAgain` : `Files.setLastModifiedTime` entre les deux → reconstruit.
  - `LocalIndexerTest.anUnreadableFileIsKeptWithItsFailure` : un `.pcg` vide → `buildFailure` présent, l'index continue sur le fichier suivant.
  - `LocalIndexerTest.cancellingStopsBeforeTheNextFile` : `cancel()` dans le premier `progress` → un seul fichier indexé.
- [ ] **Step 2: Run** — `./gradlew test --tests "*BestiaryIndexTest"` et `./gradlew slowtest --tests "*LocalIndexerTest"` → échec.
- [ ] **Step 3: Implement `BestiaryIndex`, `LocalIndexer`, `BestiaryIndexWriter`, the Gradle task**
- [ ] **Step 4: Run the tests** — Expected: PASS
- [ ] **Step 5: Generate the delivered index** — `./gradlew bestiaryIndex` ; Expected : environ 4 900 entrées (4 542 races et environ 340 kits), durée de l'ordre d'1 h 15, les échecs comptés et listés en fin d'exécution.
- [ ] **Step 6: Commit** — `feat(gametable): index the bestiary's values, delivered for the data and local for character files`

### Task 7: Le catalogue et l'onglet filtrable

**Files:**
- Create: `code/src/java/pcgen/core/gametable/catalog/CatalogEntry.java`
- Create: `code/src/java/pcgen/core/gametable/catalog/Catalog.java`
- Create: `code/src/java/pcgen/gui2/gametable/CriteriaPanel.java`
- Create: `code/src/java/pcgen/gui2/gametable/CatalogTableModel.java`
- Modify: `code/src/java/pcgen/gui2/gametable/BestiaryPanel.java` (réécrit), `GameTablePanel.java` (lance l'index local au chargement des sources, l'annule au changement)
- Modify: `LanguageBundle.properties`, `LanguageBundle_fr.properties` (libellés des champs, opérateurs, « non calculée », « non construite »)
- Test: `code/src/utest/pcgen/core/gametable/catalog/CatalogTest.java`

**Interfaces:**
- Consumes: tâches 3 à 6.
- Produces:
  - `record CatalogEntry(String name, MonsterSource source, EntryKind kind, EntryFacts facts)`, `EntryKind.RACE | FILE | KIT | UNIQUE`.
  - `Catalog.of(List<CatalogEntry> entries)` ; `List<Filtered> filter(Criterion criterion)` avec `record Filtered(CatalogEntry entry, Match match)`, sans les `NO`, dans l'ordre des entrées.
  - `BestiaryPanel` : à gauche `CriteriaPanel` (critères éditables, ET/OU, groupes, liste des préréglages, « Enregistrer sous… ») ; au centre `JTable` sur `CatalogTableModel` (colonnes de `BestiaryPreferences.columns()`, tri par `TableRowSorter`, sélection multiple, nombre de lignes affiché, `UNKNOWN` en italique avec « non calculée » dans l'infobulle) ; à droite l'aperçu existant. Boutons : nombre, → rencontre, → combat, pion (enregistrer, → Tabletop Simulator, posé ou non), Retoucher…
  - `BestiaryPanel.pickRace` ouvre le même tableau dans la fenêtre modale.
  - Boutons de réglage : « Colonnes… » (cases à cocher sur les `CatalogField`, `saveColumns`), « Dossiers de PNJ… » (liste de dossiers, ajout par `JFileChooser` en mode répertoire, `saveNpcFolders`), « Actualiser ».
  - Le catalogue se reconstruit au chargement des sources, à l'ouverture de l'onglet et par « Actualiser » : il reparcourt les dossiers de PNJ et relance l'index local, qui saute les fichiers inchangés.

- [ ] **Step 1: Write the failing tests** (`CatalogTest`)
  - `aFilterKeepsMatchesAndUnknownsInOrder` : trois entrées (YES, NO, UNKNOWN) → deux `Filtered`, la seconde `UNKNOWN`.
  - `anEntryThatFailedToBuildStaysListed` : `buildFailure` présent, filtre sur `NAME` → gardée.
- [ ] **Step 2: Run** — `./gradlew test --tests "*catalog.CatalogTest"` → échec.
- [ ] **Step 3: Implement `CatalogEntry`, `Catalog`, then the Swing classes**
- [ ] **Step 4: Run** — Expected: PASS ; `./gradlew compileJava allReports` sans nouvel avertissement Checkstyle.
- [ ] **Step 5: Check by hand** — `./gradlew run`, ouvrir `characters/tous-les-monstres.pcg` avec la source Zirnakaynin chargée, table de jeu → Bestiaire : filtre `TYPE ANY_OF [Undead]` ET `FLY_SPEED AT_LEAST [1]` ET `CHALLENGE_RATING BETWEEN [6, 8]` ; envoyer deux lignes à une rencontre et une au combat ; envoyer un pion à Tabletop Simulator ; un PNJ de Zirnakaynin apparaît (source fichier) ; un kit de `dung_kits_npc.lst` apparaît (source kit).
- [ ] **Step 6: Commit** — `feat(gametable): filter the bestiary's races, NPC files and NPC kits on any value`

### Task 8: Créatures uniques

**Files:**
- Modify: `code/src/java/pcgen/core/gametable/Scenario.java` (composant `List<UniqueCreature> uniques`, `withUniques`)
- Create: `code/src/java/pcgen/core/gametable/UniqueCreature.java`
- Modify: `code/src/java/pcgen/core/gametable/ScenarioJson.java` (clé `"uniques"`, absente → liste vide)
- Modify: `code/src/java/pcgen/core/gametable/GameTableSession.java` (`addUnique`, `saveRecipe`)
- Modify: `code/src/java/pcgen/gui2/gametable/RetouchDialog.java` (champ nom, gabarits filtrés par prérequis), `BestiaryPanel.java` (Retoucher… → « Garder dans le scénario » / « Enregistrer en .pcg… »)
- Test: `code/src/utest/pcgen/core/gametable/ScenarioJsonTest.java`, `code/src/test/pcgen/core/gametable/GameTableSessionTest.java`

**Interfaces:**
- Consumes: `MonsterRecipe`, `MonsterForge`, `saveMonster` (code existant), `CatalogEntry` (tâche 7, `EntryKind.UNIQUE`).
- Produces:
  - `record UniqueCreature(String name, MonsterRecipe recipe)` (nom non vide).
  - `GameTableSession.addUnique(UniqueCreature)` ; `Optional<String> saveRecipe(MonsterRecipe recipe, String name, File target, GameMode gameMode, List<Campaign> campaigns)` — construit puis enregistre comme `saveMonster`, renvoie l'erreur s'il y en a une.
  - Le catalogue de la tâche 7 ajoute une `CatalogEntry(name, recipe.source(), EntryKind.UNIQUE, facts)` par créature unique du scénario ouvert, ses valeurs calculées à l'ajout (construction de la recette).
  - Gabarits proposés : ceux de `CombatantSheets.templates()` dont `PrereqHandler.passesAll(template, monster, template)` est vrai sur la créature construite.

- [ ] **Step 1: Write the failing tests**
  - `ScenarioJsonTest.aUniqueCreatureComesBack` : `UniqueCreature("Sombre-croc", WOLF.withTemplate("Advanced Creature").withHitDice(2))` → aller-retour égal.
  - `ScenarioJsonTest.aScenarioWithoutUniquesOpensWithNone` : JSON d'un scénario d'avant → `uniques()` vide.
  - `GameTableSessionTest.aSavedUniqueOpensAsTheSameMonster` : `saveRecipe` d'un loup évolué dans un dossier temporaire, puis `forge(CharacterFile)` → mêmes PV maximum que la recette.
- [ ] **Step 2: Run** — `./gradlew test --tests "*ScenarioJsonTest"` et `./gradlew slowtest --tests "*GameTableSessionTest"` → échec.
- [ ] **Step 3: Implement**
- [ ] **Step 4: Run** — Expected: PASS ; puis `./gradlew test slowtest` sans régression.
- [ ] **Step 5: Check by hand** — un loup évolué + 2 DV nommé « Sombre-croc » gardé dans le scénario (il apparaît au catalogue, source « scénario », après réouverture du `.pcgtable`) et enregistré en `.pcg` dans `characters/` (il apparaît comme PNJ en fichier).
- [ ] **Step 6: Commit** — `feat(gametable): make a bestiary entry a unique creature, kept in the scenario or as a character file`

### Task 9: Spec de la table de jeu

**Files:**
- Modify: `docs/superpowers/specs/2026-10-23-table-de-jeu-design.md` (section 5 : renvoi vers la spec du bestiaire filtrable, `NpcKit` parmi les sources)

- [ ] **Step 1: Edit the section and commit** — `docs(gametable): point the game table spec at the filterable bestiary`
