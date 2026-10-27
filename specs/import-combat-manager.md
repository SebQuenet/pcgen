# Spécification : import des PNJ de Combat Manager

**Cible** : le MJ qui veut, dans PCGen, les PNJ publiés par Paizo sous forme de personnages.
**Périmètre** : un script qui lit les données de Combat Manager 1.77 et reconstruit chaque PNJ
à classe comme un `.pcg`, calculé par PCGen, avec un rapport des écarts au bloc publié.

---

## 1. Données d'entrée

L'installeur `CMInstaller177.msi` s'extrait avec `7z` en deux temps (le MSI, puis son cabinet).
Les fichiers extraits portent des noms hachés ; le script les reconnaît à leur contenu :

- quatre XML `<ArrayOfMonster>` : identité et statistiques chiffrées (race, classes, caractéristiques,
  PV, CA, jets, BBA, langues, archétypes, lignage, domaines, écoles interdites, source) ;
- une base SQLite, table `Bestiary`, jointe par `id` : texte des dons, compétences, équipement
  (`Gear`, `OtherGear`), sorts préparés et connus, tactiques.

Un **PNJ** est une entrée dont le champ `Class` n'est pas vide (4 466 entrées). Les monstres sans
classe deviennent des races (§ 7).

## 2. Décisions

1. **PCGen calcule, le bloc publié sert de cible.** Le personnage est construit par le serveur HTTP
   (`./gradlew runServer`), opération par opération, comme les PNJ Zirnakaynin. Ses valeurs
   peuvent différer du bloc : l'écart est mesuré et rapporté, jamais masqué.
2. **Les caractéristiques publiées sont finales.** Le script construit avec elles comme valeurs de
   base, lit les valeurs calculées, puis fixe `base = publiée − (calculée − base)` pour retirer les
   bonus raciaux, de niveau et d'objets.
3. **Les compétences publiées sont des totaux.** Le script investit un rang, lit le bonus hors rangs,
   puis déduit les rangs voulus, bornés à `[0, niveau]`. Les rangs se placent niveau par niveau
   selon les points de chaque niveau ; un rang en trop est rendu à la compétence suivante.
4. **Les dons publiés sont la liste complète.** Chacun va dans une réserve qui l'accepte, réserve de
   bonus de classe d'abord, réserve générale ensuite. Une réserve de dons restée ouverte le reste et
   figure au rapport : la remplir par défaut changerait les valeurs du PNJ publié. Les autres
   réserves (archétype, lignage, domaine, école, bonus racial) suivent les indications du bloc ;
   un archétype que le bloc ne nomme pas n'est jamais choisi.
5. **Les PV suivent la moyenne**, comme les PNJ de Paizo : le serveur d'import tourne sur une copie
   des réglages où `hpRollMethod` vaut 2 (`--write-settings`), et le script vérifie cette règle sur
   un guerrier témoin avant de construire. Le bonus de classe de prédilection donne un PV par PV
   manquant, un rang de compétence sinon.
6. **Un objet introuvable est rapporté, pas inventé.** Les noms d'objets sont rapprochés des clés
   du catalogue d'équipement (pluriel, « potion of », quantité entre parenthèses) ; une arme ou
   une armure `+N` est créée par `customize_equipment`. Les pièces d'or vont dans la bourse.
7. **Reprise sans recommencer.** Un PNJ dont le rapport existe est sauté ; `--force` le reconstruit.

## 3. Sortie

```
characters/combat-manager/<livre source>/<classe principale>/<nom>.pcg
characters/combat-manager/<livre source>/<classe principale>/<nom>.report.json
```

La classe principale est celle qui a le plus de niveaux. Le rapport donne, pour PV, CA, jets,
BBA, BMO, DMD, initiative et chaque compétence, la valeur publiée et la valeur calculée, ainsi que
la liste des dons, objets, sorts et choix que PCGen a refusés ou n'a pas trouvés.

## 4. Commande

```
python3 scripts/combat_manager/import_npcs.py --write-settings
./gradlew runServer --args="--settingsdir build/combat-manager-settings"
python3 scripts/combat_manager/import_npcs.py <dossier extrait> [--source "NPC Codex"] [--name "Cautious Mage"] [--force]
```

## 5. Tests

Le découpage des textes (classes, caractéristiques, dons, compétences, équipement, sorts) et le
rapprochement avec le catalogue sont testés unitairement (`scripts/tests/`). La construction
elle-même se vérifie sur un premier livre, le NPC Codex, dont le rapport agrégé chiffre les écarts.

## 6. Hors périmètre

- les compagnons animaux et familiers décrits en annexe du bloc ;
- les eidolons, publiés avec une classe « eidolon » : PCGen les construit comme compagnons d'un invocateur ;
- les capacités mythiques au-delà du choix de la voie ;
- la traduction des noms de PNJ.

## 7. Monstres sans classe

**But** : que chaque monstre sans classe de Combat Manager existe comme race dans PCGen.

**Script** : `scripts/combat_manager/import_monsters.py <dossier extrait> [--dry-run]`. Il ne parle
pas au serveur : il lit les fichiers de `data/pathfinder` et écrit des fichiers LST.

1. **Ce qui existe déjà est sauté.** Un monstre est présent si son nom correspond à une race ou à
   un gabarit de `data/pathfinder`, directement ou sous une forme de PCGen : famille entre
   parenthèses (« Ghoul (Ghast) »), virgule inversée (« Amoeba, Giant » → « Amoeba (Giant) »),
   mots inversés (« Solar Pitri » → « Pitri (Solar) »), âge de dragon (« Adult Red Dragon » →
   « Dragon (Red) »), taille d'élémentaire (« Large Ice Elemental » → « Elemental (Ice/Large) »),
   race suivie ou précédée d'un gabarit (« Giant Cockroach Exoskeleton »). Une liste d'équivalences
   écrite à la main couvre les noms que ces règles ne rapprochent pas (« Weremantis » →
   « Entothrope (Weremantis (Hybrid Form)) »).
2. **Un nom, une race.** Un monstre publié dans plusieurs livres est créé une fois, dans le premier
   livre rencontré selon l'ordre des identifiants de Combat Manager.
3. **Rangement mixte.** Un monstre va dans le dossier du livre d'où il vient quand PCGen a déjà un
   `.pcc` pour ce livre (bestiaires, Adventure Paths existants) ; sinon dans une source unique,
   `data/pathfinder/pcfinder/combat_manager/` (campagne « Combat Manager ~ Creatures »), où chaque
   race garde son livre d'origine dans `SOURCELONG`.
4. **Fichiers à part.** Le script écrit `<préfixe>_cm_races.lst`, `<préfixe>_cm_kits_race.lst` et
   `<préfixe>_cm_abilities_race.lst`, réécrits en entier à chaque passage, et ajoute leurs
   références au `.pcc` s'il ne les a pas. Les fichiers existants d'un livre ne sont jamais modifiés :
   le mode « ajout » de `generate_bestiary.py` efface le bloc auto-généré déjà présent.
5. **Même traduction que `generate_bestiary.py`.** Le bloc de Combat Manager est converti dans la
   forme des créatures de pcfinder (caractéristiques, CA, vitesses, sens, attaques naturelles,
   immunités, RD, RM, dons, compétences, langues, pouvoirs magiques), puis passe par les fonctions
   de génération de ligne de race, de kit et de traits raciaux de ce script. Le type est ramené au
   type de base (« advanced magical beast » → créature magique, « elemental » → extérieur). Un don
   marqué `[M]` ou nommé « Mythic … » donne le don de base et, dans le kit, le don de catégorie
   `Mythic Feat` du même nom.
6. **Une race ne référence que ce que PCGen définit.** Chaque don, choix d'arme, compétence et sort
   est rapproché des clés de `data/pathfinder` (clé `KEY:` quand elle existe, armes naturelles
   comprises) ; ce qui n'y est pas est retiré, et le script en donne le compte. « Ability Focus »
   est toujours retiré : il vise une capacité de la race que la race générée ne définit pas. Les
   langues accordées se limitent à celles des livres de règles et de l'Inner Sea World Guide, que
   tout livre de monstres charge.
7. **Hors de cette passe** : les entrées marquées gabarit (`IsTemplate`) ou compagnon
   (`CompanionFlag`) de Combat Manager.

**Tests** : la conversion d'un bloc de Combat Manager et le rapprochement des noms sont testés
unitairement (`scripts/tests/test_combat_manager_monsters.py`). Le chargement des sources produites
se vérifie avec `DataLoadTest` sur un `.pcc` temporaire qui regroupe les sources modifiées et leurs
livres de référence, comparé au même chargement sans les fichiers `_cm_` ; puis avec le serveur HTTP :
un personnage construit sur une race créée a les caractéristiques, la CA et la RD du bloc publié.
