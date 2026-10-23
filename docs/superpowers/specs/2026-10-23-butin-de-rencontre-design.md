# Conception : le butin de rencontre

**Cible** : le MJ qui mène ses combats dans la table de jeu de PCGen
(`docs/superpowers/specs/2026-10-23-table-de-jeu-design.md`).
**Périmètre** : à la fin d'un combat, PCGen dresse la liste exacte du butin, objets portés et
trésor tiré au sort, et l'affiche sous le groupe dans l'onglet **Groupe**.

Ce document lève un point que la conception de la table de jeu avait mis hors périmètre : le
générateur de trésor.

Les passages marqués *(choix de conception)* ont été proposés à la rédaction et non contestés par
l'utilisateur. Ceux marqués *(choix du rédacteur)* sont des décisions prises en relisant le document,
à vérifier à la lecture.

---

## 1. Pourquoi

Aujourd'hui, à la fin d'un combat, rien n'est fait : les joueurs notent ce qu'ils trouvent, puis se
répartissent la somme. Le MJ ne peut pas leur donner une liste exhaustive et exacte du butin sans y
passer des heures.

Le MJ **lit** la liste aux joueurs. Rien n'est écrit dans les fiches des PJ, que tiennent les
joueurs.

## 2. Objectifs, hors périmètre, échecs

**Objectifs**

- lister tous les objets que portaient les monstres et PNJ du combat, avec leur valeur ;
- tirer au sort, pour les monstres qui ont du trésor sans le porter, des objets compatibles avec le
  type de créature, jusqu'au budget de trésor de son FP ;
- donner pour chaque objet sa valeur et sa valeur à la revente ;
- laisser le MJ cocher ou décocher chaque objet, les totaux suivant la sélection ;
- garder le butin de chaque combat dans le scénario, d'une séance à l'autre.

**Hors périmètre** : répartir le butin entre les PJ, l'ajouter à leurs fiches, identifier les
objets magiques en jeu, les objets nommés dans le texte d'un trésor de bestiaire (« standard
(+1 scimitar, other treasure) ») au-delà de leur type.

**Échecs, même si tout fonctionne**

- un objet porté par un monstre vaincu qui manque à la liste ;
- un prix faux ou nul pour un objet qui en a un dans les règles ;
- une liste que le MJ ne peut pas lire d'un trait aux joueurs, ou qui demande du calcul de sa part ;
- un butin perdu à la fermeture du scénario.

## 3. L'onglet Groupe

Disposition retenue (A) : une seule page, le groupe en haut, tel qu'aujourd'hui (liste et boutons
Ajouter, Ajouter depuis une fiche, Lier une fiche, Retirer), la partie **Butin** en dessous.

La partie Butin empile un **bloc par combat terminé**, le plus récent en bas *(choix du
rédacteur)*. Un bloc porte :

- en titre, le nom de la rencontre lancée et son FP ;
- un tableau, une ligne par objet : case à cocher, objet, porté par, origine (« équipé » ou « tiré
  (standard) »…), valeur, revente ;
- une ligne de total de la sélection : valeur et revente, avec le budget de trésor du FP en rappel ;
- la part de revente par personnage, soit la revente sélectionnée divisée par la taille du groupe ;
- les boutons **Relancer le tirage**, **Copier la liste** et **Supprimer le bloc** *(choix de
  conception)*.

En bas de la partie Butin, un total général de tous les blocs *(choix de conception)*.

Une ligne décochée reste visible, barrée et grisée, pour que le MJ puisse la recocher.

**Copier la liste** met dans le presse-papiers le texte à lire : les seules lignes cochées, nom et
valeur de revente, puis le total et la part par personnage *(choix du rédacteur)*.

**Relancer le tirage** refait les seules lignes tirées au sort ; les lignes « équipé » et l'état de
leurs cases restent.

**Supprimer le bloc** demande confirmation : c'est le seul geste de la partie Butin qui ne se
défait pas.

## 4. Fin de combat

À la fin d'un combat, la table de jeu affiche les XP comme aujourd'hui, et crée en plus le bloc de
butin. Le butin se calcule **avant** que le combat soit vidé : `GameTableSession.endCombat()` oublie
les monstres construits (`forgetBuilt()`), et le butin a besoin de leurs inventaires.

**Combattants pris en compte** : les monstres du combat, pas les PJ ni les créatures conjurées, qui
disparaissent à la fin du sort.

*(Choix du rédacteur)* Une ligne est **cochée par défaut** si le monstre qui la porte a des PV à 0
ou moins ; sinon elle est listée mais décochée. Un monstre qui s'est enfui, ou un allié que joue le
MJ, ne laisse donc rien par défaut, et le MJ le corrige d'un clic.

**Titre du bloc** : aujourd'hui, `Combat` ne retient pas quelle rencontre l'a lancé, et
`launch()` peut en verser plusieurs dans le même combat. Le combat retient donc, et enregistre, les
noms des rencontres lancées ; le titre les joint (« 4. Le camp du ritualiste + renforts »). Un
combat sans rencontre lancée, monté avec « Ajouter », s'intitule « Combat sans rencontre »
*(choix du rédacteur)*. Le FP affiché est celui des XP totales du combat
(`Experience.challengeRatingOf`).

## 5. Objets portés

Tout l'équipement de l'inventaire du monstre construit par PCGen, équipé ou non : armes, armures,
objets magiques, consommables, pièces. Cela couvre les PNJ importés de Combat Manager, dont le
`.pcg` porte l'équipement du codex, et les monstres de race dont le kit donne un équipement.

La valeur est le prix que PCGen calcule pour l'objet, modificateurs compris (un *+2 breastplate*
vaut son prix d'arme magique, pas celui de la cuirasse de base). Un objet en plusieurs exemplaires
est une ligne avec sa quantité.

L'argent que PCGen tient hors de l'inventaire (l'or du personnage) devient une ligne « Pièces ».

Les armes naturelles et les objets sans prix ne sont pas listés.

## 6. Trésor tiré au sort

### Combien

Chaque monstre a un **type de trésor**, lu dans les données du bestiaire (§ 7). Son budget est la
valeur de trésor pour **son** FP, progression **moyenne** de la table *Treasure Values per
Encounter* (*Core Rulebook*, table 12-5), multipliée selon le type :

| Type | Multiplicateur |
|---|---|
| aucun (`none`) | 0 |
| fortuit (`incidental`) | ½ |
| standard | 1 |
| double | 2 |
| triple | 3 |
| équipement de PNJ (`NPC gear`) | aucun tirage : son inventaire est son trésor |

Les variantes rencontrées dans les données se ramènent à ces types : « double standard » = double,
« triple standard » = triple, « half standard » et « 50% standard » = ½, « ? » et absence = standard
*(choix de conception)*.

La valeur des objets que le monstre **porte déjà** est déduite de son budget ; si elle le dépasse,
rien n'est tiré *(choix de conception)*.

Un monstre construit depuis un `.pcg` (PNJ de codex) est traité en « équipement de PNJ », quel que
soit le type de sa race *(choix du rédacteur)*.

Progression moyenne (*Core Rulebook* p. 399, vérifiée sur aonprd) : FP 1 = 260 po, 2 = 550,
3 = 800, 4 = 1 150, 5 = 1 550, 6 = 2 000, 7 = 2 600, 8 = 3 350, 9 = 4 250, 10 = 5 450,
11 = 7 000, 12 = 9 000, 13 = 11 600, 14 = 15 000, 15 = 19 500, 16 = 25 000, 17 = 32 000,
18 = 41 000, 19 = 53 000, 20 = 67 000. La table s'arrête à 20 : au-delà, la valeur de 20 ; un FP
fractionnaire prend sa part de la valeur de 1 *(choix du rédacteur)*.

### Quoi

La méthode du chapitre *Treasure Generation* d'*Ultimate Equipment* :

1. le **type de créature** (aberration, dragon, extérieur, humanoïde…) donne les familles de trésor
   qu'elle peut avoir, parmi A à I : A pièces, B pièces et gemmes, C objets d'art, D pièces et petits
   objets, E armes et armures, F équipement de combattant, G équipement de lanceur de sorts, H trésor
   de repaire, I trésor amassé ;
2. on tire une famille, puis dans la famille un **lot de valeur fixe** qui tient dans ce qui reste du
   budget ;
3. on recommence jusqu'à ce que le reste soit trop petit pour un lot, et ce reste devient des pièces.

Le contenu d'un lot devient des lignes de la liste : pièces, gemmes, objets d'art, objets magiques,
armes et armures.

### Prix des objets tirés

*(Choix du rédacteur, révisé au plan)* Un objet tiré prend **le prix imprimé dans les tables
d'*Ultimate Equipment***, qui en donnent un pour chaque objet de chaque lot. Ce prix suit les mêmes
formules que PCGen (potion, parchemin, baguette, arme +N). PCGen n'a ni potion, ni parchemin, ni
baguette, ni arme +1 tout faits : il faudrait les construire un par un d'un objet de base et d'un
modificateur, sans gain d'exactitude. Les objets **portés** gardent le prix que PCGen calcule (§ 5).

## 7. Données

**Type de trésor par monstre.** Les données PCGen n'en ont pas. `creatures.json` de pcfinder
(`../pcfinder/apps/pcfinder-csr/static-data/creatures.json`), déjà source de
`scripts/generate_bestiary.py`, porte un champ `treasure` pour 3 373 des 3 684 créatures. Un script
en tire une table nom de race → type de trésor, versée dans le dépôt ; la table de jeu ne lit
jamais pcfinder. Le rattachement aux races PCGen se fait par le nom, comme
`scripts/creature_comparison.json` le fait déjà : environ 1 400 des 1 734 races des bestiaires
trouvent leur nom. Une race sans correspondance prend « aucun » si elle est de type animal ou
vermine, « standard » sinon *(choix du rédacteur, révisé au plan : les loups et ours sans
correspondance n'ont pas de trésor)*.

**Tables d'*Ultimate Equipment*** : familles par type de créature et lots de chaque famille, saisis
dans un fichier de données du dépôt. Le fichier `magic-item-tables.json` du scraper pcfinder n'est
pas réutilisable : les noms y sont rangés dans le champ `aura` et tous les prix valent 0.

**Revente** : la moitié de la valeur, sauf les pièces, gemmes, objets d'art et marchandises, revendus
à leur valeur pleine (*Core Rulebook*, « Selling Treasure »).

## 8. Le scénario

Le fichier `.pcgtable` garde la liste des blocs de butin, chacun avec ses lignes telles qu'affichées :
nom, quantité, porteur, origine, valeur, revente, cochée ou non. Les valeurs sont enregistrées, pas
recalculées : rouvrir le scénario redonne exactement la liste lue aux joueurs, même si les données
ont changé entre-temps.

Un scénario enregistré avant cette fonction s'ouvre avec une partie Butin vide.

## 9. MCP et HTTP

Comme le reste de la table de jeu (commit `e645524`), le butin passe par le registre d'opérations :
lire le butin, cocher ou décocher une ligne, relancer le tirage d'un bloc, supprimer un bloc. Le jeu
de la table renvoyé par chaque opération inclut les blocs de butin *(choix de conception)*. La fin
de combat (`end_combat`) renvoie, en plus des XP, le bloc créé.

## 10. Tests

- le tirage dépend d'un générateur aléatoire injecté, pour que les tests fixent le résultat ;
- le budget : un monstre de chaque type, à un FP donné, reçoit le montant de la table, déduction
  faite de ce qu'il porte ;
- un PNJ de codex (un `.pcg` de `characters/combat-manager/`) donne en lignes exactement son
  inventaire, avec les prix de PCGen ;
- la revente : objet magique à moitié, gemme et pièces à plein ;
- l'aller-retour du scénario : un bloc enregistré puis relu est identique, cases comprises ;
- un scénario sans butin, écrit avant cette fonction, se relit.

## 11. Ce qui reste au réalisateur

- l'architecture interne, dans les conventions de `pcgen.core.gametable` et de
  `pcgen.session.service.GameTableService` ;
- le format exact des fichiers de données (types de trésor, tables d'*Ultimate Equipment*) et leur
  emplacement ;
- la transcription des tables d'*Ultimate Equipment*, vérifiée contre le livre ou d20pfsrd ;
- le choix entre composant Swing et vue web pour la partie Butin ;
- l'ordre de réalisation, pourvu que chaque étape livre quelque chose d'utilisable : les objets
  portés d'abord, puisqu'ils ne demandent aucune table, puis le tirage.
