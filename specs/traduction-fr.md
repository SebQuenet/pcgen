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

### Unités

En français, PCGen affiche les unités métriques, comme l'édition française : le jeu d'unités
`Metric` du mode de jeu Pathfinder (`miscinfo.lst`) convertit déjà 5 ft en 1,5 m et 1 lb en
0,5 kg. Le choix se fait dans le panneau de préférences de la langue (`LanguagePanel`), qui
propose déjà les jeux d'unités. Quand aucun jeu d'unités n'est enregistré pour le mode de jeu,
le français démarre en `Metric`. PCGen enregistre le jeu d'unités de chaque mode de jeu à chaque
sauvegarde des options, choisi ou non : une installation existante garde donc le sien, et
l'utilisateur passe en `Metric` une fois, dans ce panneau.

Les distances et poids écrits dans les textes (`DESC`, `BENEFIT` : « within 30 ft. ») ne passent
pas par le jeu d'unités : la traduction les convertit elle-même (« à 9 m »), selon une règle
du glossaire (§ 7).

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
- `ENGLISH`, sixième colonne facultative, réservée à `DESC` et `BENEFIT` : le gabarit anglais
  que la ligne traduit. Un objet peut porter plusieurs `DESC` ou `BENEFIT`, chacun sous ses
  prérequis, et souvent parce qu'un autre livre lui en ajoute un par `.MOD` (le Core Rulebook
  décrit la lignée Aberrant, Ultimate Magic lui ajoute la description de sa variante). La
  position d'une description dépend des livres chargés ; son texte anglais, non. L'export écrit
  cette colonne pour chaque description d'un objet qui en porte plusieurs, et la laisse vide
  pour un objet qui n'en porte qu'une.

Le format est en TSV et non en `.properties` : les clés contiennent des espaces, des
parenthèses et des virgules, que `.properties` obligerait à échapper.

### Chargement

- À la fin du chargement des sources, pour chaque campagne chargée, PCGen lit le fichier
  `<nom-du-pcc>.<langue>.tsv` s'il existe. Pour une langue sans fichier, ou pour l'anglais, il
  ne lit rien.
- Une ligne qui désigne un objet absent, ou un `TEXT` dont les marqueurs `%n` diffèrent de
  l'original, est ignorée et signalée dans le journal de chargement, comme les erreurs de
  `.lst`. Elle ne fait pas échouer le chargement.
- La traduction est rangée sur l'objet lui-même, à côté de l'anglais, comme `OUTPUTNAME` :
  `StringKey.LOCALIZED_NAME`, `LOCALIZED_OUTPUT_NAME`, `LOCALIZED_TEMP_DESCRIPTION`, et
  `ObjectKey.LOCALIZED_DESCRIPTIONS`, `LOCALIZED_BENEFITS` pour les gabarits, qui gardent les
  variables et les prérequis de l'original (`Description.withTemplate`). Un catalogue injecté
  était prévu ; il ne pouvait pas atteindre `PObject.toString()`, qui n'a pas de dépendances.
  Ranger la traduction sur l'objet la rend aussi propre au jeu de données chargé, sans état global.
- `CDOMObject.getLocalizedName()` renvoie le nom traduit, sinon le nom anglais.
- Renommer un objet (`setDisplayName`) efface son nom traduit, qui traduisait l'ancien nom :
  un équipement agrandi ou personnalisé reprend son nom anglais complet.
- Une ligne `DESC` ou `BENEFIT` sans colonne `ENGLISH` traduit la description unique de
  l'objet ; si l'objet en porte plusieurs, elle est signalée. Une ligne avec `ENGLISH` traduit
  la description dont le gabarit anglais lui est identique. Si aucune ne l'est, la ligne est
  ignorée sans être signalée : le livre qui apporte cette description n'est pas chargé.
  Contrepartie : une ligne dont l'anglais a changé depuis l'export est ignorée en silence ;
  seule une nouvelle comparaison avec l'export la révèle.
- Chaque description traduite est rangée sous son gabarit anglais
  (`ObjectKey.LOCALIZED_DESCRIPTIONS`, `LOCALIZED_BENEFITS`). L'affichage remplace chaque
  description qui a une traduction et garde l'anglais des autres.

### Objets du système

Les caractéristiques, jets de sauvegarde, alignements et catégories de taille ne viennent pas du
mode de jeu mais des données (`cr__stats.lst`, `cr__saves.lst`, `cr__align.lst` du Core
Rulebook, `ce__sizes.lst` des Core Essentials) : les tables de campagne les couvrent, sous les
types `STAT`, `CHECK`, `ALIGNMENT` et `SIZEADJUSTMENT`. Un champ `ABB` traduit leur abréviation
(STR → FOR, NG → NB), que les jetons `STAT.x.NAME`, `ALIGNMENT.SHORT` et `SIZE` affichent ; leur
clé, qui est aussi leur abréviation anglaise, ne change pas.

Les types de déplacement (Walk, Fly, Swim, Climb, Burrow) ne sont pas des objets : ce sont des
mots des données. Ils passent par le bundle (`in_move_walk`…), consulté par les jetons `MOVE` et
`MOVEMENT`. `BASEMOVEMENT` les lit comme des clés et les garde en anglais.

## 5. Points d'affichage

| Couche | Où lire la traduction |
|---|---|
| Fiches FreeMarker | `DisplayNameActor`, `OutputNameActor`, `DescriptionActor`, `CDOMObjectModel` |
| Anciens jetons d'export (`.htm`) | `OutputNameFormatting.getOutputName` pour l'affichage, `BenefitFormatting`, jetons `DESC` |
| GUI | `PObject.toString()` (utilisé par les tables et les listes), `Gui2InfoFactory` (panneau d'information) |
| Services headless | les modèles qui listent des objets portent déjà `key` (à repasser en argument) et `name` (à afficher) : `name` devient le nom traduit ; les arguments acceptent la clé, le nom anglais ou le nom traduit (`DataSetLookup.answersTo`) |
| Webapp | affiche `name`, envoie `key` |

`PObject.toString()` (et ses redéfinitions dans `Spell`, `Equipment`, `EquipmentModifier`)
renvoie le nom traduit. L'inventaire des endroits où ce nom servait de clé est fait par
`PseudoTranslationTest` : il donne un nom factice à chaque objet chargé, rouvre les personnages
Pathfinder de `code/testsuite` et vérifie que le `.pcg` réenregistré est identique. Constats :

- `Equipment` écrivait le nom affiché dans `CUSTOMIZATION:[…DATA:NAME=…]` ; il écrit désormais
  le nom anglais (le même défaut existait avec l'option « nom de sortie »).
- La recherche d'un bonus temporaire par nom (`TacticalSheetFacadeImpl`, `TemplateService`)
  n'acceptait que le nom affiché ; elle accepte aussi la clé.
- Les services headless renvoient tantôt `toString()`, tantôt `getDisplayName()` : c'est
  l'objet de l'étape 4.

`Description.getDescription()` applique le gabarit traduit quand l'objet en porte un,
sinon l'original. La substitution des variables ne change pas.

## 6. Fiches de personnage

Seules les fiches lues par un joueur sont traduites : `csheet_fantasy_std.htm.ftl` et
`csheet_fantasy_statblock_pathfinder.htm.ftl`. Leurs libellés passent par le bundle, avec la
fonction `i18n` des modèles, et non par une copie de fichier par langue.

Les fiches LLM (`csheet_llm.json.ftl`, `.md.ftl`, `.toon.ftl`) gardent leurs intitulés et leurs
clés en anglais : leur lecteur est un modèle, et les outils qui les lisent attendent des
intitulés stables. Les valeurs qu'elles contiennent (noms, descriptions) sortent dans la langue
réglée, comme partout.

## 7. Production des traductions

### Glossaire

`data/pathfinder/i18n/glossaire.fr.tsv` : terme anglais → terme français, pour les termes de
règles (caractéristiques, conditions, types de bonus, écoles de magie, tailles, types de
créatures, termes d'action). Il s'appuie sur la terminologie de l'édition française officielle
(Black Book Éditions) et sur les traductions de pcfinder (`~/Documents/projects/pcfinder`).
Il est construit avec Opus et relu à la main.

Le glossaire porte aussi les règles de forme : distances converties en mètres (5 ft = 1,5 m,
30 ft = 9 m), poids en kilos (1 lb = 0,5 kg), abréviations françaises (DD, NLS, BBA, BMO, DMD,
PV, CA).

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
2. **Chargement** : format TSV, chargement, rangement sur l'objet, nom traduit dans
   `toString()`, inventaire des usages de ce nom comme clé.
3. **Affichage GUI et fiches** : branchement du § 5, fiches du § 6.
3 bis. **Objets du système et unités** : abréviations, types de déplacement, `Metric` par
   défaut en français (§ 3 et § 4).
4. **Affichage headless et webapp** : nom traduit dans `name`, noms traduits acceptés en argument.
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
