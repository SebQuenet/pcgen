# Spécification : serveur PCGen headless pour un front web

**Cible** : un seul utilisateur, en local, avec une interface web qui remplace le GUI JavaFX.
**Périmètre** : extraire la logique métier des outils MCP vers une couche service, puis l'exposer
en HTTP sans casser le serveur MCP existant.

---

## Table des matières

1. [Objectif](#1-objectif)
2. [Contraintes mesurées](#2-contraintes-mesurées)
3. [Architecture](#3-architecture)
4. [Contrat de la couche service](#4-contrat-de-la-couche-service)
5. [Registre des opérations](#5-registre-des-opérations)
6. [API HTTP](#6-api-http)
7. [Choix en attente et interblocage](#7-choix-en-attente-et-interblocage)
8. [Sécurité](#8-sécurité)
9. [Rendu de la fiche](#9-rendu-de-la-fiche)
10. [Stratégie de test](#10-stratégie-de-test)
11. [Découpage en incréments](#11-découpage-en-incréments)
12. [Critères d'acceptation](#12-critères-dacceptation)
13. [Hors périmètre](#13-hors-périmètre)

---

## 1. Objectif

`pcgen.mcp.McpMain` démarre déjà PCGen sans interface graphique et expose 77 opérations de
construction de personnage. Ces opérations sont écrites à l'intérieur de lambdas
`SyncToolSpecification` (voir `CharacterBuildTools.java:47`), donc inaccessibles à tout autre
appelant et difficiles à tester — le commentaire en tête de `TacticalSheetToolsTest` le dit :
« The tools are lambdas inside a protocol specification, so what is tested here is the
behaviour they wrap ».

Cette spécification décrit comment sortir cette logique des lambdas et la servir à la fois au
serveur MCP et à une interface web, sans la dupliquer.

## 2. Contraintes mesurées

Mesures prises sur ce dépôt, Pathfinder, daemon Gradle chaud :

| Étape | Temps | RSS après |
|---|---|---|
| Démarrage jusqu'au serveur prêt (modes de jeu + index des campagnes) | 3,4 s | — |
| `load_sources` sur Core Rulebook seul | 2,7 s | 510 MiB |
| `load_sources` sur 6 campagnes (Core + APG + Ultimate Campaign + Bestiary + 2 dépendances) | 28,9 s | 686 MiB |

Trois conséquences de conception :

- **Un processus persistant.** 29 s de chargement interdisent de démarrer un processus par
  requête. Le serveur garde son jeu de sources chargé pour toute la session.
- **Un seul jeu de sources à la fois.** `SourceFileLoader:224-227` appelle `Globals.emptyLists()`
  puis `SettingsHandler.setGame(...)` : charger un second jeu détruit le premier. Pour un seul
  utilisateur ce n'est pas bloquant, `SourceMerger` sait déjà fusionner (voir
  `McpSessionManager.openCharacter`), mais le front doit afficher une progression pendant les 29 s.
- **Un seul thread de travail.** Voir §7.

## 3. Architecture

Trois couches, chacune ignorant celle du dessus.

```
pcgen.web          HttpApiServer, routage, encodage JSON, gardes Host/Origin
pcgen.mcp          McpMain, McpServerBuilder, adaptateurs vers le SDK MCP
        \         /
pcgen.session.api  OperationRegistry : nom, description, schéma JSON, décodage des arguments
pcgen.session.service  17 services à méthodes typées, renvoyant ServiceResult
pcgen.session      PcgenSession, HeadlessUIDelegate, PendingChoice, HeadlessBootstrap
        |
pcgen.facade, pcgen.core, pcgen.cdom   (inchangés)
```

Renommages, par cohérence avec le fait que ces classes ne servent plus seulement MCP :

| Avant | Après |
|---|---|
| `pcgen.mcp.McpSessionManager` | `pcgen.session.PcgenSession` |
| `pcgen.mcp.McpUIDelegate` | `pcgen.session.HeadlessUIDelegate` |
| `pcgen.mcp.PendingChoice` | `pcgen.session.PendingChoice` |
| (amorçage inline dans `McpMain.main`) | `pcgen.session.HeadlessBootstrap` |

`pcgen.mcp` conserve `McpMain`, `McpServerBuilder`, `McpConfigInstaller` et les classes de
`config/`. Les 16 classes `pcgen.mcp.tools.*` disparaissent : leur schéma JSON et leur décodage
d'arguments partent dans le registre, leur corps dans le service correspondant.

## 4. Contrat de la couche service

Les erreurs sont des valeurs typées présentes dans la signature, jamais des exceptions qui
traversent les couches.

```java
public sealed interface ServiceResult<T>
{
	record Success<T>(T value) implements ServiceResult<T> { }

	record Failure<T>(ServiceError error) implements ServiceResult<T> { }
}

public sealed interface ServiceError
{
	record NoSourcesLoaded() implements ServiceError { }

	record CharacterNotFound(String characterId) implements ServiceError { }

	record EntryNotFound(String kind, String key) implements ServiceError { }

	record InvalidArgument(String field, String reason) implements ServiceError { }

	record NotAllowed(String reason) implements ServiceError { }

	record DataFailure(String message) implements ServiceError { }
}
```

Chaque service reçoit `PcgenSession` par constructeur. Une méthode par opération, arguments
typés, aucun `Map<String, Object>` :

```java
public final class CharacterBuildService
{
	private final PcgenSession session;

	public CharacterBuildService(PcgenSession session) { this.session = session; }

	public ServiceResult<CharacterSummary> setName(String characterId, String name) { ... }

	public ServiceResult<LevelAddition> addClassLevel(String characterId, String classKey, int levels) { ... }
}
```

Les types de retour sont des records dédiés (`CharacterSummary`, `LevelAddition`, …), pas des
`Map` construites à la volée : c'est ce qui rend la réponse JSON stable pour le front et
vérifiable en test.

Correspondance des erreurs :

| `ServiceError` | HTTP | MCP |
|---|---|---|
| `InvalidArgument` | 400 | `isError: true` |
| `CharacterNotFound`, `EntryNotFound` | 404 | `isError: true` |
| `NoSourcesLoaded` | 409 | `isError: true` |
| `NotAllowed` | 422 | `isError: true` |
| `DataFailure` | 500 | `isError: true` |

Le serveur MCP aplatit tout en `CallToolResult(message, true)`, ce que les outils font déjà
aujourd'hui : l'adaptateur MCP ne perd donc rien au passage.

## 5. Registre des opérations

Les schémas JSON des 77 opérations sont déjà écrits, dans les `new Tool(...)`. Ils deviennent
la source unique consommée par les deux transports.

```java
public record Operation(
	String name,
	String description,
	String inputSchema,
	Function<Map<String, Object>, ServiceResult<?>> invoke)
{
}
```

`invoke` est la frontière de validation : il décode le `Map` venu du transport, refuse un
argument manquant ou mal typé par `Failure(new InvalidArgument(...))`, puis appelle la méthode
typée du service. Aucun autre point du code ne manipule de `Map`.

`OperationRegistry` assemble les 77 `Operation` à partir des 17 services.
`McpServerBuilder` les convertit en `SyncToolSpecification` ; `HttpApiServer` les monte en routes.

Répartition (inchangée par rapport aux classes de tools actuelles) :

| Service | Opérations |
|---|---|
| `SourceService` | `list_game_modes`, `list_sources`, `load_sources` |
| `CharacterLifecycleService` | `create_character`, `get_character`, `open_character`, `save_character`, `close_character` |
| `CharacterBuildService` | `set_name`, `set_race`, `add_class_level`, `set_ability_score`, `set_alignment`, `set_all_ability_scores` |
| `AbilityService` | `list_ability_categories`, `list_abilities`, `add_ability`, `remove_ability`, `batch_add_abilities` |
| `SkillService` | `invest_skill_points`, `get_skill_summary`, `batch_invest_skills` |
| `SpellService` | `get_available_spells`, `get_known_spells`, `add_known_spell`, `remove_known_spell`, `get_prepared_spells`, `add_prepared_spell`, `remove_prepared_spell`, `get_spellbooks`, `add_to_spellbook`, `batch_add_prepared_spells` |
| `EquipmentService` | `buy_equipment`, `sell_equipment`, `get_inventory`, `set_funds`, `batch_buy_equipment` |
| `EquipmentSetService` | `list_equipment_sets`, `create_equipment_set`, `get_equipped_items`, `equip_item`, `unequip_item`, `equip_items` |
| `CustomEquipmentService` | `list_equipment_modifiers`, `customize_equipment` |
| `DeityDomainService` | `set_deity`, `add_domain`, `remove_domain` |
| `TemplateService` | `add_template`, `remove_template`, `get_templates`, `add_temp_bonus`, `remove_temp_bonus`, `list_temp_bonuses`, `add_kit` |
| `LanguageCompanionService` | `get_languages`, `add_language`, `remove_language`, `get_companions`, `add_companion`, `remove_companion` |
| `BiographyService` | `set_biography`, `get_biography`, `set_xp` |
| `TacticalSheetService` | `set_tactical_sheet`, `get_tactical_sheet`, `clear_tactical_sheet`, `list_tactical_references` |
| `ExportService` | `export_character`, `get_character_sheet` |
| `ChoiceService` | `get_pending_choices`, `resolve_choice` |
| `UtilityService` | `get_todo_list`, `is_qualified_for`, `roll_stats`, `is_dirty`, `get_character_details` |

## 6. API HTTP

**Serveur** : `com.sun.net.httpserver.HttpServer`, déjà présent dans la liste des modules jlink
(`build.gradle:343`). Aucune dépendance à ajouter.

**Style : RPC, pas REST.** Une route unique par opération :

```
POST /api/{operation}     corps JSON = les arguments, identiques au schéma MCP
GET  /api/operations      la liste des 77 opérations avec leur schéma
GET  /health              état du serveur et jeu de sources chargé
```

Réponse en succès :

```json
{ "ok": true, "data": { ... } }
```

Réponse en échec, avec le code HTTP du tableau §4 :

```json
{ "ok": false, "error": { "kind": "CharacterNotFound", "characterId": "…" } }
```

Compromis assumé : on perd les verbes HTTP, les URLs ressource et le cache. On gagne trois
choses. La traduction depuis les outils MCP est mécanique, donc sans décision d'interprétation à
prendre 77 fois. Le front et l'agent IA voient exactement la même surface, donc un seul jeu de
tests les couvre. Et `GET /api/operations` renvoyant les schémas déjà écrits, le client peut se
générer. Pour un consommateur unique en local, un découpage REST n'apporterait rien de ces
trois-là.

**Exécution** : `server.setExecutor(Executors.newSingleThreadExecutor())` — sauf pour les deux
opérations décrites en §7.

## 7. Choix en attente et interblocage

`HeadlessUIDelegate.interactiveSelect` bloque le thread appelant sur
`future.get(300, TimeUnit.SECONDS)` en attendant que `resolve_choice` complète le
`CompletableFuture`. Avec un exécuteur mono-thread, `resolve_choice` fait la queue derrière
l'opération qu'il est censé débloquer : interblocage garanti de 300 secondes.

Règle : **`get_pending_choices` et `resolve_choice` ne passent pas par l'exécuteur mono-thread.**
Ils s'exécutent directement sur le thread HTTP. C'est sûr parce qu'ils ne touchent pas l'état
PCGen : ils lisent une `ConcurrentHashMap` et complètent un `CompletableFuture`, tous deux
détenus par le délégué.

Le front apprend qu'un choix est en attente en interrogeant `GET /api/pending-choices` pendant
que sa requête d'origine est encore ouverte. Un flux SSE serait plus élégant ; pour un seul
utilisateur l'interrogation périodique suffit et coûte une route.

Le mode par défaut du délégué reste `autoChoose = true`, donc les opérations ne bloquent que si
le front a explicitement demandé à choisir lui-même.

## 8. Sécurité

Un serveur HTTP en écoute sur la boucle locale sans authentification est accessible à tout
processus de la machine, et un navigateur peut l'atteindre depuis n'importe quel site par
reliaison DNS. Trois gardes, toutes obligatoires :

- Écoute sur `127.0.0.1` uniquement, jamais `0.0.0.0`.
- En-tête `Host` rejeté s'il ne vaut pas `127.0.0.1:{port}` ou `localhost:{port}` — c'est ce qui
  bloque la reliaison DNS.
- En-tête `Origin`, quand il est présent, rejeté s'il ne désigne pas la boucle locale.

Les chemins de fichier reçus (`export_character.output_path`, `open_character.file`) sont des
entrées externes : ils doivent être résolus puis vérifiés comme descendant du répertoire de
personnages ou du répertoire de travail, jamais utilisés bruts.

## 9. Rendu de la fiche

`get_character_sheet` accepte déjà `markdown`, `toon`, `json`, `xml`, et son cas `default`
transmet la valeur reçue comme nom de template (`ExportTools.resolveBuiltInTemplate`). Le front
web consomme `format: "json"` (`csheet_llm.json.ftl`) et fait son propre rendu. Aucun travail
n'est nécessaire côté serveur ; c'est noté ici pour qu'on ne réimplémente pas d'exporteur.

## 10. Stratégie de test

Les services sont testés sur leur comportement observable, avec de vraies données chargées —
aucune doublure du code testé, conformément au découpage déjà retenu par
`TacticalSheetToolsTest`.

- **`code/src/test/` (slowtest)** : les tests de service. Ils chargent un jeu de sources réel une
  fois pour l'ensemble de la classe (2,7 s pour Core Rulebook, mesuré en §2) et exercent les
  opérations de bout en bout : créer un personnage, lui donner une race, monter un niveau, lire
  la fiche.
- **`code/src/utest/` (test)** : le décodage des arguments dans `OperationRegistry` (un argument
  manquant donne `InvalidArgument`), la correspondance `ServiceError` → code HTTP, les gardes
  `Host` et `Origin`. Aucune donnée de jeu, donc rapide.
- **Non-régression MCP** : un test vérifie que `OperationRegistry` expose exactement les 77 noms
  d'opération d'aujourd'hui, pour qu'aucune migration de domaine n'en perde un en route.
- **Interblocage** : un test déclenche une opération qui ouvre un choix interactif, puis résout
  ce choix par un second appel, et vérifie que le premier se termine.

Les défauts listés dans `mcp-to-fix.md` sont dans la couche métier et remonteront identiques dans
l'API web. Ils ne sont pas traités ici, mais chaque migration de domaine est l'occasion d'écrire
le test qui les caractérise.

## 11. Découpage en incréments

Un incrément par PR, moins de 500 lignes modifiées chacune.

| # | Contenu | Nature |
|---|---|---|
| 1 | Créer `pcgen.session` : extraire `HeadlessBootstrap` de `McpMain.main`, déplacer et renommer `PcgenSession`, `HeadlessUIDelegate`, `PendingChoice` | mécanique, aucun changement de comportement |
| 2 | `ServiceResult`, `ServiceError`, `Operation`, `OperationRegistry` vide, plus `SourceService` (3 opérations) et ses tests | valide le motif de bout en bout |
| 3 | `HttpApiServer` : `POST /api/{operation}`, `GET /api/operations`, `GET /health`, gardes §8, exécuteur mono-thread, chemin dérivé pour les choix | sur les 3 opérations du pilote |
| 4 | `CharacterLifecycleService` + `CharacterBuildService` (11 opérations) | migration |
| 5 | `AbilityService` + `SkillService` (8 opérations) | migration |
| 6 | `SpellService` (10 opérations) | migration |
| 7 | `EquipmentService` + `EquipmentSetService` + `CustomEquipmentService` (13 opérations) | migration |
| 8 | `DeityDomainService` + `TemplateService` + `LanguageCompanionService` (16 opérations) | migration |
| 9 | `BiographyService` + `TacticalSheetService` + `ExportService` + `ChoiceService` + `UtilityService` (16 opérations) | migration |
| 10 | Tâche Gradle `runServer`, service des fichiers statiques du front, `pcgen.web.WebMain` | mise en service |

Après l'incrément 3, le serveur MCP et le serveur HTTP tournent tous les deux, chacun sur une
partie du registre ; les incréments 4 à 9 les remplissent en parallèle sans jamais casser l'un
des deux.

## 12. Critères d'acceptation

- `./gradlew test`, `./gradlew slowtest` et `./gradlew allReports` passent sans nouvelle
  violation à chaque incrément.
- `OperationRegistry` expose les 77 mêmes noms d'opération qu'aujourd'hui.
- Le serveur MCP répond comme avant sur `initialize` puis `tools/call`, vérifié sur
  `load_sources` et `create_character`.
- Un enchaînement `curl` de `POST /api/load_sources` puis `POST /api/create_character` puis
  `POST /api/set_race` aboutit à un personnage lisible par `POST /api/get_character_sheet`.
- Une requête portant un en-tête `Host` étranger reçoit 403.
- Une opération qui ouvre un choix interactif se termine après résolution par un second appel,
  sans atteindre le délai de 300 s.

## 13. Hors périmètre

- Plusieurs utilisateurs simultanés. Cela demanderait un processus PCGen par jeu de sources et un
  routeur devant, parce que `Globals` et `ChooserFactory.delegate` (champ statique unique,
  `ChooserFactory.java:32`) sont partagés par tout le processus.
- L'authentification. La boucle locale et les gardes du §8 en tiennent lieu.
- L'écriture du front web lui-même.
- La correction des défauts de `mcp-to-fix.md`.
