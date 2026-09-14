# Spécification : interface web PCGen

**Cible** : un seul utilisateur, en local, dans un navigateur, face au serveur HTTP de
`specs/serveur-headless.md`.
**Périmètre** : créer et modifier un personnage de bout en bout, sans jamais ouvrir le GUI JavaFX.

---

## 1. Objectif

Le serveur expose 77 opérations sur `POST /api/{operation}`. L'interface les met à portée d'un
joueur : choisir ses sources, créer un personnage, lui donner une race et des niveaux, répartir
ses caractéristiques et ses compétences, prendre des dons, acheter son équipement, préparer ses
sorts, et lire sa fiche.

## 2. Stack

- **TypeScript + React**, construits par **Vite**. Source dans `webapp/`, build vers `web/`,
  que `HttpApiServer` sert déjà.
- **Zod** valide chaque réponse du serveur. Une réponse qui ne correspond pas au schéma attendu
  est une erreur affichée, jamais un état incohérent propagé dans l'interface.
- **Vitest** pour les tests. Ils portent sur ce qui décide : le client d'API, la lecture des
  réponses, l'enchaînement des étapes. Pas sur le rendu.
- Aucune bibliothèque de composants : le style tient dans une feuille CSS.

## 3. Le client d'API

Un seul point de passage vers le serveur :

```ts
callOperation<T>(name: string, args: object, schema: ZodType<T>): Promise<Result<T>>
```

Il envoie `POST /api/{name}`, lit l'enveloppe `{ok, data, error}`, et renvoie une union
discriminée : `{ok: true, data: T}` ou `{ok: false, error: {kind, message}}`. Aucun `throw`
ne traverse les couches, comme côté serveur.

Les schémas Zod sont écrits à la main pour les opérations utilisées, à partir de ce que
`GET /api/operations` déclare. Les générer serait de la complexité sans besoin actuel.

## 4. Écrans

| Écran | Ce qu'il fait | Opérations |
|---|---|---|
| Sources | Choisir le mode de jeu et les campagnes, lancer le chargement | `list_game_modes`, `list_sources`, `load_sources` |
| Personnages | Créer, ouvrir, enregistrer, fermer | `create_character`, `open_character`, `save_character`, `close_character` |
| Identité | Nom, race, alignement, divinité, biographie | `set_name`, `set_race`, `set_alignment`, `set_deity`, `set_biography`, `get_biography` |
| Niveaux | Ajouter des niveaux de classe, voir la progression | `add_class_level`, `get_character` |
| Caractéristiques | Saisir ou tirer les six scores | `set_all_ability_scores`, `roll_stats`, `get_character_details` |
| Compétences | Répartir les points, niveau par niveau | `get_skill_summary`, `invest_skill_points` |
| Dons | Parcourir les catégories, prendre et retirer | `list_ability_categories`, `list_abilities`, `add_ability`, `remove_ability` |
| Équipement | Acheter, vendre, équiper | `get_inventory`, `buy_equipment`, `sell_equipment`, `get_equipped_items`, `equip_item`, `unequip_item`, `set_funds` |
| Sorts | Apprendre et préparer | `get_available_spells`, `get_known_spells`, `add_known_spell`, `get_prepared_spells`, `add_prepared_spell` |
| Fiche | Lire la fiche rendue | `get_character_sheet` |

Un bandeau permanent montre le personnage courant, ses points en attente (`get_todo_list`) et
l'état d'enregistrement (`is_dirty`).

## 5. Choix en attente

Une opération peut ouvrir un choix côté serveur et rester bloquée jusqu'à la réponse. Pendant
qu'une requête est en vol, l'interface interroge `GET /api/pending-choices` chaque seconde ; dès
qu'un choix apparaît, elle l'affiche et poste la sélection à `resolve_choice`. La requête
d'origine se termine alors d'elle-même.

C'est le seul endroit où l'interface fait deux requêtes de front. Le serveur le permet parce que
ces deux opérations ne passent pas par son fil de travail exclusif.

## 6. Chargement des sources

Charger un jeu de sources réaliste prend une trentaine de secondes. L'écran Sources affiche une
attente explicite pendant ce temps, et rien d'autre n'est possible tant qu'il dure : sans sources
chargées, le serveur refuse toute création de personnage.

## 7. Stratégie de test

- **Client d'API** : l'enveloppe de succès est lue, celle d'erreur aussi, une réponse hors schéma
  devient une erreur et non un plantage, un code HTTP non-200 est rapporté.
- **Schémas** : chaque réponse que l'interface lit est validée contre un exemple réel capturé du
  serveur.
- **Enchaînements** : la file des choix en attente, et l'ordre créer → race → niveaux.

Les tests appellent un serveur factice (`fetch` remplacé), pas le vrai serveur : ce qui est testé
est la lecture des réponses, pas PCGen.

## 8. Build

`npm run build` produit `web/`. La tâche Gradle `buildWebapp` l'enchaîne, et `testWebapp` lance
les tests.

`runServer` ne dépend volontairement pas de `buildWebapp` : le serveur doit démarrer sur une
machine sans npm, et une interface déjà construite n'a pas besoin d'être reconstruite pour lancer
le serveur. Quand `web/` est absent, le démarrage le dit en une ligne.

## 9. Hors périmètre

- Les compagnons, les objets personnalisés, les bonus temporaires, les kits, les modèles et la
  fiche tactique. Leurs opérations existent côté serveur ; l'interface les ajoutera quand le
  parcours de base sera éprouvé.
- L'affichage hors ligne : l'interface ne garde rien, le serveur est la seule source.
