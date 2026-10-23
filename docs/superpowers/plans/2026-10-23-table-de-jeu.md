# Table de jeu — plan de réalisation

> Réalisation dans la session, sans sous-agent (règle de l'utilisateur), dans l'arborescence principale,
> sans commit : l'utilisateur commite lui-même.

**Objectif** : l'espace « Table de jeu » de la spec, dans l'application Java.

**Architecture** : un modèle pur et immuable (`pcgen.core.gametable`) porte le combat, le scénario et sa
lecture JSON ; il se teste sans PCGen. Une couche qui touche PCGen (`MonsterForge`, `CombatantSheets`)
construit les monstres, lit leurs fiches et pose leurs bonus temporaires, en réutilisant
`SummonedCreatureBuilder`, `TemporaryBonusSources` et `TemporaryBonusApplier`. Une session
(`GameTableSession`) relie les deux pour l'interface Swing (`pcgen.gui2.gametable`).

**Pile** : Java 25, Swing, Jackson (`jackson-databind`, déjà en dépendance), JUnit 5.

**Spec** : `docs/superpowers/specs/2026-10-23-table-de-jeu-design.md`

## Contraintes globales

- Style Checkstyle du projet : accolades Allman, tabulations, pas d'import étoile, ligne ≤ 201.
- Textes d'interface dans `LanguageBundle.properties` et `LanguageBundle_fr.properties`, clés `in_gametable_*`.
- Erreurs prévisibles (fichier introuvable, race absente des données) rendues comme valeurs, jamais levées
  à travers les couches.
- L'espace Personnages ne change pas.

## Points de vigilance

1. Un `.pcg` de monstre déplacé : l'ouverture du scénario le signale avec le nom de la rencontre, le reste
   s'ouvre.
2. Un combattant supprimé ou retardé alors qu'il a le tour : le tour passe au suivant sans sauter personne.
3. Une double initiative dont la seconde entrée tombe sous toutes les autres (initiative − 20 négative) :
   elle joue en dernier, et les durées ne baissent qu'une fois par round.
4. Des dégâts supérieurs aux PV restants, des soins au-delà du maximum : PV bornés à `[−∞ ; max]` côté
   affichage, dégâts jamais négatifs.
5. Un combat sauvegardé puis rouvert après une retouche du `.pcg` source : le monstre est reconstruit à
   partir de la recette, ses dégâts restent ceux enregistrés.

---

### Tâche 1 : le modèle du combat

**Fichiers** : `code/src/java/pcgen/core/gametable/{Condition,Combatant,CombatantKind,Turn,Combat,CombatAdvance,
MonsterRecipe,MonsterSource,Health,Initiative}.java`, test `code/src/utest/pcgen/core/gametable/CombatTest.java`.

**Produit** :
- `MonsterSource` scellé : `BestiaryRace(String raceKey)`, `CharacterFile(String path)`.
- `MonsterRecipe(MonsterSource source, List<String> addedTemplates, List<String> removedTemplates, int addedHitDice)`,
  `withTemplate(String)`, `withoutTemplate(String)`, `withHitDice(int)`.
- `Condition(String name, Optional<BonusSourceKey> bonus, OptionalInt roundsLeft)`, `ticked()` → `Optional<Condition>`.
- `Initiative(OptionalInt score, int modifier, boolean dual)` ; `Health(int maximum, int damage)`.
- `Combatant(int id, String name, String group, Initiative initiative, Optional<Health> health,
  List<Condition> conditions, Optional<MonsterRecipe> recipe, Optional<String> characterFile,
  OptionalInt summonerId, OptionalInt roundsLeft, boolean delaying)` ; `kind()` → `CombatantKind`
  (`PLAYER`, `PLAYER_SUMMON`, `MONSTER`, `MONSTER_SUMMON`) ; un monstre a toujours une `Health`.
- `Turn(int combatantId, boolean second)`.
- `Combat(int round, List<Combatant> order, Optional<Turn> current)` : `empty()`, `with(Combatant)`,
  `nextId()`, `withInitiative(int id, int score)` (toute la troupe), `rollingMissing(IntUnaryOperator d20)`,
  `turns()`, `started()`, `next()` → `CombatAdvance(Combat combat, List<String> expired)`, `damaged`,
  `healed`, `withCondition`, `withoutCondition`, `delaying`, `resuming`, `moved(int id, int index)`,
  `removed`, `summoned(int summonerId, List<Combatant>)`, `replaced(Combatant)`, `combatant(int id)`.

**Tests** (un comportement chacun) : tri par score puis modificateur ; une troupe partage son initiative ;
`next` fait le tour puis change de round ; durées décrémentées une fois par round et états expirés listés ;
double initiative à score − 20 ; une créature conjurée arrive juste après son conjurateur et part à
l'expiration ; retarder sort de l'ordre, reprendre réinsère après celui qui vient de jouer avec son
score ; suppression du combattant courant ; dégâts et soins bornés.

### Tâche 2 : FP et XP

**Fichiers** : `pcgen/core/gametable/Experience.java`, test `ExperienceTest.java`.
**Produit** : `static long experienceOf(String challengeRating)` (table Pathfinder, de 1/8 à 25),
`static String challengeRatingOf(long totalExperience)`, `static long share(long total, int players)`.

### Tâche 3 : le scénario et son fichier

**Fichiers** : `pcgen/core/gametable/{Scenario,PartyMember,Encounter,EncounterMonster,ScenarioJson,ScenarioReading}.java`,
test `ScenarioJsonTest.java`.
**Produit** : `Scenario(String gameMode, List<String> campaigns, List<PartyMember> party, List<Encounter>
encounters, Optional<Combat> combat)` ; `Encounter(String name, List<EncounterMonster> monsters, String notes)`
refuse un nom vide ; `EncounterMonster(String name, MonsterRecipe recipe, int count)` ;
`ScenarioJson.write(Scenario) → String`, `ScenarioJson.read(String) → ScenarioReading` (scellé : `Read(Scenario)`,
`Unreadable(String reason)`). Extension de fichier `.pcgtable`.
**Tests** : aller-retour complet, combat compris ; un JSON malformé donne `Unreadable`.

### Tâche 4 : construire et lire un monstre

**Fichiers** : `pcgen/core/gametable/{MonsterForge,ForgedMonster,CombatantSheets,PlayerGlance}.java`,
test lent `code/src/test/pcgen/core/gametable/MonsterForgeTest.java` (sources de `BestiarySources`).
**Produit** : `MonsterForge(Collection<Campaign>, UIDelegate)` ; `forge(MonsterRecipe) → ForgedMonster`
(scellé : `Forged(PlayerCharacter)`, `Missing(String reason)`) ; DV ajoutés par la classe de monstre de la
race, PV moyens ; `CombatantSheets.stats(PlayerCharacter) → CreatureStats`,
`glance(PlayerCharacter) → PlayerGlance(perception, senseMotive, fortitude, reflex, will)`,
`hasDualInitiative(PlayerCharacter)`, `lay(PlayerCharacter, List<Condition>)` (pose ou retire les bonus) ;
`monsterRaces()`, `conditionSources()`.
**Tests** : un gobelin évolué a plus de PV et de CA ; +2 DV augmente les DV ; *Shaken* baisse l'attaque de 2 ;
un loup *Agile* a la double initiative ; un fichier absent donne `Missing`.

### Tâche 5 : la session de table de jeu

**Fichiers** : `pcgen/core/gametable/GameTableSession.java`, test lent `GameTableSessionTest.java`.
**Produit** : état courant (scénario, combat, fichier), monstres construits par identifiant de combattant ;
`launch(Encounter, IntUnaryOperator d20)`, `addMonster(String name, MonsterRecipe, int count)`,
`summon(int summonerId, String name, Optional<MonsterRecipe>, int count, OptionalInt rounds)`,
`retouch(int id, MonsterRecipe)` (garde les dégâts), `addCondition`, `removeCondition`, `sheet(int id)`,
`glance(int id)`, `open(File)`, `save(File)`, `missing()` (monstres introuvables à l'ouverture).

### Tâche 6 : l'interface

**Fichiers** : `pcgen/gui2/gametable/{GameTablePanel,PartyPanel,EncountersPanel,CombatPanel,CombatantSheetPanel,
BestiaryPanel,ConditionDialog,SummonDialog,RetouchDialog}.java`, `pcgen/gui2/PCGenFrame.java` (sélecteur
d'espace, `CardLayout`), bundles de langue.
**Raccourcis** : Espace suivant, D dégâts, S soins, E état, R retarder, Suppr retirer.
**Vérification** : `./gradlew compileJava`, `./gradlew test`, `./gradlew slowtest --tests "pcgen.core.gametable.*"`,
lancement de l'application.
