# Spécification : PCGen en français

**Cible** : un joueur francophone qui crée un personnage Pathfinder 1 dans le GUI, la webapp ou
par MCP, et lit tout en français : interface, noms des objets de jeu, descriptions, fiche.
**Périmètre** : Pathfinder 1 (`data/pathfinder`) et le homebrew Celmae/Udaeus (`data/homebrew`).
Les autres systèmes de jeu restent en anglais.

---

## 1. Objectif

L'utilisateur choisit la langue dans les préférences (`LanguagePanel`, déjà présent). Ce choix
vaut pour l'interface **et** pour les données. Quand une traduction manque, PCGen affiche le
texte anglais : un trou de traduction ne bloque jamais rien.

## 2. Principe : la clé ne change jamais

PCGen identifie un objet par sa clé : `KEY:` s'il est déclaré, sinon son nom
(`CDOMObject.getKeyName()` se rabat sur `getDisplayName()`). Les prérequis (`PREFEAT:1,Power
Attack`), les `.MOD`/`.COPY`, les choix (`CHOOSE`) et les personnages `.pcg` enregistrés font
référence à cette clé.

Conséquences, non négociables :

- les fichiers `.lst` ne sont pas modifiés ;
- `getDisplayName()`, `getKeyName()` et `toString()` utilisé comme identifiant gardent la
  valeur anglaise ;
- la traduction est une couche d'**affichage**, consultée aux seuls endroits où un texte
  part vers un humain ;
- un `.pcg` enregistré en français se rouvre à l'identique en anglais, et inversement.

## 3. Interface

- Compléter `LanguageBundle_fr.properties` : 79 clés manquantes, 152 valeurs identiques à
  l'anglais à relire.
- Sortir vers le bundle les libellés écrits en dur dans `gui2`/`gui3` (environ 60, dans
  31 fichiers : `setText("…")`, `new JLabel("…")`, `new Label("…")`…).
- `HeadlessBootstrap` applique la langue réglée, comme `Main.configureUI()` le fait pour le
  GUI. Une langue par processus : pas de choix par requête HTTP.
- Changer de langue demande un redémarrage, comme aujourd'hui pour l'interface.

## 4. Table de traduction

### Format

Un fichier par campagne, à côté de son `.pcc` : `<nom-du-pcc>.fr.tsv`, en UTF-8, séparé par
tabulations, une ligne par objet, `#` pour les commentaires.

```
TYPE	CATEGORY	KEY	FIELD	TEXT
ABILITY	FEAT	Power Attack	NAME	Attaque en puissance
ABILITY	FEAT	Power Attack	BENEFIT	Vous pouvez choisir de subir un malus de -%1 …
SPELL		Magic Missile	NAME	Projectile magique
EQUIPMENT		Longsword	NAME	Épée longue
```

- `TYPE` : le type d'objet PCGen (`ABILITY`, `SPELL`, `EQUIPMENT`, `EQMOD`, `RACE`, `CLASS`,
  `SKILL`, `DEITY`, `DOMAIN`, `TEMPLATE`, `LANGUAGE`, `KIT`…).
- `CATEGORY` : la catégorie d'ability (`FEAT`, `Special Ability`…), vide pour les autres
  types. Elle est nécessaire : deux abilities de catégories différentes peuvent partager une
  clé.
- `FIELD` : `NAME`, `OUTPUTNAME`, `DESC`, `BENEFIT`, `TEMPDESC`, `SPROP`. `ASPECT` est hors
  périmètre de la première version (§ 9).
- `TEXT` : le texte français. Pour `DESC`, `BENEFIT`, `SPROP`, il remplace le gabarit
  (le texte avant le premier `|`) et doit contenir exactement les mêmes marqueurs `%1`…`%n`
  que l'original. Les variables qui suivent le `|` ne sont pas reprises : PCGen garde
  celles de l'objet anglais.

Le format est en TSV et non en `.properties` : les clés contiennent des espaces, des
parenthèses et des virgules, que `.properties` obligerait à échapper.

### Chargement

- À la fin du chargement des sources, pour chaque campagne chargée, PCGen lit le fichier
  `<nom-du-pcc>.<langue>.tsv` s'il existe. Pour une langue sans fichier, ou pour l'anglais, il
  ne lit rien.
- Une ligne qui désigne un objet absent, ou un `TEXT` dont les marqueurs `%n` diffèrent de
  l'original, est ignorée et signalée dans le journal de chargement, comme les erreurs de
  `.lst`. Elle ne fait pas échouer le chargement.
- Les traductions sont rangées dans un `TranslationCatalog` attaché au jeu de données chargé :

```java
public interface TranslationCatalog
{
	String name(CDOMObject object);                  // falls back to getDisplayName()
	String outputName(CDOMObject object);            // falls back to OutputNameFormatting
	Optional<String> text(CDOMObject object, TranslatedField field);
}
```

  L'anglais utilise une implémentation qui renvoie toujours l'original. Les points
  d'affichage reçoivent le catalogue par injection, jamais par un singleton.

## 5. Points d'affichage

| Couche | Où brancher le catalogue |
|---|---|
| Fiches FreeMarker | `DisplayNameActor`, `OutputNameActor`, `DescriptionActor`, `CDOMObjectModel` |
| Anciens jetons d'export (`.htm`) | `OutputNameFormatting.getOutputName` pour l'affichage, `BenefitFormatting`, jetons `DESC` |
| GUI | `PObject.toString()` (utilisé par les tables et les listes), `Gui2InfoFactory` (panneau d'information) |
| Services headless | chaque modèle renvoyé garde `name` (la clé, à repasser en argument) et gagne `displayName` (le nom traduit) |
| Webapp | affiche `displayName`, envoie `name` |

`PObject.toString()` est un cas à vérifier objet par objet : s'il sert quelque part de clé
(tri, comparaison, sérialisation), ce site doit passer à `getKeyName()` avant d'activer la
traduction. L'inventaire de ces sites fait partie de l'étape 2 (§ 8).

`Description.getDescription()` applique le gabarit traduit quand le catalogue en fournit un,
sinon l'original. La substitution des variables ne change pas.

## 6. Fiches de personnage

Seules les fiches utilisées pour Pathfinder sont traduites : `csheet_fantasy_std.htm.ftl`,
`csheet_fantasy_statblock_pathfinder.htm.ftl`, `csheet_llm.json.ftl`, `csheet_llm.md.ftl`,
`csheet_llm.toon.ftl`. Leurs libellés passent par le bundle (`LanguageBundle`) et non par une
copie de fichier par langue. Les clés JSON de `csheet_llm.json.ftl` restent en anglais : seules
les valeurs sont traduites.

## 7. Production des traductions

### Glossaire

`data/pathfinder/i18n/glossaire.fr.tsv` : terme anglais → terme français, pour les termes de
règles (caractéristiques, conditions, types de bonus, écoles de magie, tailles, types de
créatures, termes d'action). Il s'appuie sur la terminologie de l'édition française officielle
(Black Book Éditions) et sur les traductions de pcfinder (`~/Documents/projects/pcfinder`).
Il est construit avec Opus et relu à la main.

### Reprise de pcfinder

Les 2 817 dons de `pcfinder-csr/static-data/feats_i18n.json` donnent les noms français des
dons. Leurs descriptions ne sont pas reprises : ce sont des résumés générés, pas des
traductions, et certaines contiennent encore des restes `:contentReference[oaicite:…]`.
La correspondance se fait sur `name_en` ↔ nom PCGen, à l'identique après normalisation
(casse, apostrophes) ; ce qui ne correspond pas est traduit comme le reste.

### Traduction en masse

Un script (`scripts/translate_data.py`) :

1. extrait de chaque campagne les champs traduisibles et produit les lignes à traduire ;
2. envoie des lots à Sonnet par l'API, avec le glossaire, en sortie structurée (JSON schema),
   jamais en texte libre analysé par regex ;
3. vérifie chaque ligne renvoyée : marqueurs `%n` identiques, termes du glossaire respectés,
   aucun champ vide ; une ligne qui échoue repart dans le lot suivant ;
4. écrit le `.fr.tsv` ; une ligne déjà présente n'est jamais retraduite, pour que les
   relectures humaines survivent à une nouvelle passe.

Les tests du script rejouent des réponses enregistrées, sans appeler l'API.

### Relecture

Ordre de priorité : Core Rulebook, Advanced Player's Guide, Ultimate Magic, Ultimate Combat,
Celmae/Udaeus. Opus relit un échantillon de chaque lot traduit ; la relecture humaine
complète vise d'abord ces livres. Le reste est livré en traduction automatique non relue.

## 8. Étapes

Chaque étape est une PR de moins de 500 lignes, utilisable seule.

1. **Interface** : bundle complété, libellés en dur sortis, langue appliquée en headless.
2. **Catalogue** : format TSV, chargement, `TranslationCatalog`, inventaire des usages de
   `toString()` comme clé.
3. **Affichage GUI et fiches** : branchement du § 5, fiches du § 6.
4. **Affichage headless et webapp** : `displayName` dans les modèles de service.
5. **Glossaire et script** : glossaire relu, script testé sur le Core Rulebook.
6. **Contenu** : passes de traduction livre par livre, un ou plusieurs livres par PR de
   données (les `.fr.tsv` ne comptent pas dans la limite de lignes, qui vise le code).

## 9. Hors périmètre

- `ASPECT` (9 037 occurrences, de formes hétérogènes), traité dans une version suivante.
- Les textes produits par le code Java à l'exécution (messages de prérequis non remplis,
  choix `CHOOSE` générés, noms d'objets composés comme les armes de maître ou les
  équipements personnalisés), sauf s'ils passent déjà par le bundle.
- Les autres systèmes de jeu et Pathfinder 2.
- Les langues autres que le français : le format les permet (`<pcc>.<langue>.tsv`), mais
  aucun contenu n'est prévu.

## 10. Tests

- Charger une campagne de test avec un `.fr.tsv` : le nom traduit est affiché, la clé est
  inchangée, un prérequis écrit en anglais est toujours satisfait.
- Une ligne au marqueur `%n` manquant est ignorée et journalisée ; l'original reste affiché.
- Un `.pcg` enregistré en français et rouvert en anglais donne le même personnage
  (`BasicSaveRestoreTest`).
- Le service renvoie `name` en anglais et `displayName` en français.
- Le script de traduction rejette une réponse qui perd un marqueur ou viole le glossaire.
