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
classe restent hors périmètre : le bestiaire de PCGen les porte déjà sous forme de races.

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

- les monstres sans classe, les compagnons animaux et familiers décrits en annexe du bloc ;
- les capacités mythiques au-delà du choix de la voie ;
- la traduction des noms de PNJ.
