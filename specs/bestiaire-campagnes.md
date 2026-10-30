# Spécification : bestiaire des adventure paths et campaign settings

**Cible** : le MJ qui veut, dans PCGen, tous les monstres des adventure paths et des campaign settings
en cochant une seule source au lieu de 116.
**Périmètre** : une source `Adventure Path and Campaign Setting Bestiary`, dans
`data/pathfinder/pcfinder/campaign_bestiary/campaign_bestiary.pcc`.

## 1. Données d'entrée

Les sources de `data/pathfinder/pcfinder/adventure_path/` et `data/pathfinder/pcfinder/campaign_setting/`,
importées depuis pcfinder-csr et Combat Manager. Chacune ne contient que des créatures : des lignes
`ABILITY`, `RACE`, `KIT` et `TEMPLATE` vers ses fichiers LST, et les mêmes prérequis (Core Rules,
Bestiary). Aucune clé de race n'est définie par deux de ces sources.

Les adventure paths officiels de PCGen (`data/pathfinder/paizo/adventure_path/`) ne sont pas repris :
leurs sources mêlent les monstres et des options de joueur.

## 2. Contenu de la source

Le `.pcc` reprend, dans l'ordre des dossiers, chaque ligne `ABILITY`, `RACE`, `KIT` et `TEMPLATE` des
sources d'entrée, le chemin réécrit en chemin `@/` depuis `data/`. Il ne copie aucun fichier LST : une
correction d'une source individuelle passe dans le bestiaire.

PCGen ne charge un fichier qu'une fois, même référencé par deux sources : charger le bestiaire avec
une adventure path individuelle ne duplique rien.

## 3. Script

`scripts/generate_campaign_bestiary.py` réécrit le `.pcc`. À relancer après l'ajout ou le retrait
d'une source dans les deux dossiers d'entrée.

## 4. Vérification

- Tests unitaires de la réécriture des lignes (`scripts/tests/`).
- Chargement de la source par le serveur HTTP, sans erreur LST, et nombre de races égal à la somme
  des races des sources d'entrée.
