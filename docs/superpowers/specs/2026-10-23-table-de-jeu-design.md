# Conception : la table de jeu du MJ

**Cible** : le MJ d'une campagne Pathfinder 1, qui mène ses combats en ligne.
**Périmètre** : un second espace dans l'application Java de PCGen, « Table de jeu », qui reprend de
Combat Manager le suivi de combat, la préparation des rencontres et la retouche des monstres.

Les passages marqués *(choix de conception)* ont été proposés à la rédaction puis validés par
l'utilisateur.

---

## 1. Pourquoi

Le MJ joue en ligne : les joueurs voient la carte et les jetons dans Tabletop Simulator sur
l'ordinateur de bureau, parlent sur Discord depuis le téléphone, et le MJ tient sur son portable
PCGen d'un côté, une feuille Excel de l'autre pour l'initiative. La table de jeu remplace cette
feuille et va chercher dans PCGen ce que le MJ y lisait à la main : CA, attaques, capacités des
monstres.

Le portable n'est vu que du MJ, il est petit, et le MJ parle en même temps qu'il s'en sert. Ce qui
compte est le nombre de gestes, pas la richesse de l'affichage.

## 2. Objectifs, hors périmètre, échecs

**Objectifs**

- suivre un combat : ordre d'initiative, tour courant, round, PV des monstres, états et bonus
  temporaires avec leur durée ;
- préparer les rencontres d'un scénario à l'avance, puis les lancer en un clic ;
- retoucher un monstre, en préparation comme en plein combat : DV raciaux, gabarits ;
- faire entrer en cours de combat des créatures conjurées, d'un camp comme de l'autre ;
- gérer la double initiative du gabarit mythique *Agile*.

**Hors périmètre** : cartes, brouillard de guerre, télécommande depuis un téléphone, générateur de
trésor, lanceur de dés, consultation des règles (le fork de PCGen la couvre déjà).

**Échecs, même si tout fonctionne**

- un geste courant de combat qui demande plus de deux clics ou ne s'atteint pas au clavier ;
- un écran qui ne tient pas sur un portable sans défilement horizontal ;
- l'espace Personnages modifié dans son usage actuel par un joueur.

## 3. Deux espaces dans la même fenêtre

La fenêtre principale de PCGen porte en haut un sélecteur à deux positions :

- **Personnages** : PCGen tel qu'aujourd'hui, inchangé ; le joueur y gère un ou plusieurs
  personnages ;
- **Table de jeu** : le MJ y gère un scénario de campagne.

On reste en général dans un espace toute une séance. Depuis la table de jeu, « Ouvrir dans
Personnages » affiche un monstre dans l'espace Personnages pour une retouche que la table de jeu ne
propose pas (dons, équipement, sorts) ; le retour à la table de jeu reflète la modification.

La table de jeu a quatre onglets : **Groupe**, **Rencontres**, **Combat**, **Bestiaire**. Une vue
web peut servir à l'intérieur, comme les onglets Conjurations et Tactique (`WebView` et pont
JavaScript), quand elle est plus simple à réaliser qu'un composant Swing.

## 4. Le scénario

Un scénario est un fichier à part, ouvert et enregistré comme un `.pcg` l'est pour un personnage. Il
contient :

- **le groupe** : les PJ, chacun avec un nom et, s'il en a une, le chemin de sa fiche `.pcg` ;
- **les rencontres**, dans l'ordre du scénario. Chacune a un **nom obligatoire**, ses monstres avec
  leur nombre, son FP et ses XP calculés, et des notes libres ;
- **le combat en cours**, s'il y en a un, pour reprendre la séance suivante à l'endroit exact ;
- *(choix de conception)* **ses sources**, comme un `.pcg` : ouvrir le scénario les charge, puisque
  les monstres en dépendent.

*(Choix de conception)* Un allié que le MJ joue (PNJ du groupe, monture) est un monstre de la
rencontre. Le bouton « Ajouter » du combat permet aussi d'y faire entrer un monstre non préparé.

*(Choix de conception)* Un monstre de rencontre est enregistré comme une **recette**, pas comme une
copie de personnage : sa source (race du bestiaire et gabarits, ou chemin d'un `.pcg`) et ses
retouches (DV ajoutés, gabarits ajoutés ou retirés). PCGen reconstruit le personnage à l'ouverture.
Un `.pcg` introuvable est signalé à l'ouverture du scénario, avec le nom de la rencontre touchée.
Retoucher un monstre de la table de jeu ne modifie jamais son `.pcg` source.

## 5. Les monstres

Trois sources donnent le même objet, un personnage PCGen dont PCGen calcule les valeurs :

- une race du bestiaire, avec ou sans gabarits, construite comme le fait déjà
  `pcgen.core.conjurations.SummonedCreatureBuilder` (race, kit, gabarits, PV moyens) ;
- un `.pcg` construit par le MJ dans PCGen, seul ou avec l'IA ;
- un PNJ importé de Combat Manager par `scripts/combat_manager/import_npcs.py`, qui est un `.pcg`.

L'onglet **Bestiaire** cherche une race par son nom et l'ajoute à une rencontre.

**Retouches** : ajouter des DV raciaux, appliquer ou retirer un gabarit. Les gabarits demandés
existent dans les données : *Advanced Creature* (évolué), *Celestial Creature*, *Fiendish
Creature*, *Chthonic Creature*, *Entropic Creature*, *Giant*, et les gabarits simples mythiques
`Mythic Simple Template ~ Agile` et `~ Savage`. *Invincible* est marqué « Not Implemented » dans les
données.

En plein combat, une retouche ne vise que le monstre sélectionné et lui garde les dégâts déjà
subis : ses PV restants valent le nouveau maximum moins ces dégâts.

## 6. L'onglet Combat

Disposition retenue : la liste d'initiative à gauche, la fiche du combattant sélectionné à droite.
Au-dessus, le round et la barre d'actions, chaque action ayant son raccourci clavier.

### Les combattants

| Combattant | PV | États et bonus | Fiche à droite |
|---|---|---|---|
| PJ | non suivis (le joueur les tient) | étiquettes avec durée | lecture rapide, si une fiche `.pcg` est liée |
| Créature conjurée par un PJ | non suivis (le joueur les tient dans son onglet Conjurations) | étiquettes avec durée | aucune |
| Monstre | suivis | recalculent ses valeurs | fiche complète |
| Créature conjurée par un monstre | suivis | recalculent ses valeurs | fiche complète |

**Fiche d'un monstre** : PV restants sur le total, CA normale, contact et pris au dépourvu, jets de
sauvegarde, attaques avec bonus et dégâts, capacités spéciales, boutons de retouche. Ce sont les
champs de `pcgen.core.conjurations.CreatureStats`.

**Lecture rapide d'un PJ** : *(choix de conception)* Perception, Psychologie et les trois jets de
sauvegarde, lus dans son `.pcg`. Pas la CA, qui varie trop en combat pour servir. Un PJ sans
fiche liée n'a pas de lecture rapide.

### Le déroulé

1. **Lancer une rencontre** depuis l'onglet Rencontres : ses monstres entrent dans le combat, les PJ
   du groupe aussi.
2. **Initiative** : les monstres identiques d'une rencontre (3 gobelins) partagent **une seule
   initiative**, lancée par PCGen, mais restent des combattants distincts, numérotés « Gobelin 1, 2,
   3 », chacun avec ses PV. Le MJ saisit l'initiative que chaque joueur annonce sur Discord.
   *(Choix de conception)* À égalité, le plus fort modificateur passe devant ; le MJ peut ensuite
   réordonner par glisser-déposer.
3. **Tour par tour** : passer au suivant, infliger des dégâts ou soigner un monstre, poser ou retirer
   un état, un bonus temporaire ou une propriété négative, retarder. Une action préparée est un état
   comme un autre.
4. **Changement de round** : les durées baissent d'un ; un état arrivé à zéro disparaît et PCGen le
   signale au MJ dans la barre d'état. Une créature conjurée dont la durée est écoulée quitte le
   combat.
5. **Fin du combat** : PCGen affiche les XP que rapporte la rencontre, par PJ. *(Choix de
   conception)* Rien n'est écrit dans les fiches des PJ, que tiennent les joueurs.

*(Choix de conception)* **Retarder** retire le combattant de l'ordre ; le MJ le réinsère quand le
joueur agit, et son initiative prend alors la valeur du combattant qui vient de jouer.

### États, bonus temporaires, propriétés négatives

- **Sur un monstre**, un état ou un bonus temporaire tiré des données recalcule ses valeurs. Les
  états existent déjà comme gabarits à bonus temporaires dans `cr_templates.lst` (bloc
  « Conditions » : *Shaken*, *Sickened*, *Fatigued*, *Grappled*…), les sorts en portent d'autres
  dans `cr_spells.lst` (Hâte, Bénédiction…).
- **Sur un PJ ou une créature conjurée par un PJ**, tout état ou bonus est une simple étiquette.
- **Une propriété négative sans équivalent dans les données** est une étiquette libre, sur
  n'importe quel combattant.

Chaque état, bonus ou étiquette porte une durée en rounds, ou aucune.

### Créatures conjurées en cours de combat

Une créature conjurée entre dans l'ordre d'initiative juste après son conjurateur, avec sa durée en
rounds. Plusieurs créatures conjurées ensemble sont numérotées comme des monstres identiques. Une
créature conjurée par un monstre se construit comme dans l'onglet Conjurations : une race, ses
gabarits (céleste, fiélon), et *Augment Summoning* si son conjurateur a le don.

### Double initiative

Un combattant doté de la capacité `Mythic Simple Template ~ Dual Initiative` (gabarit mythique
*Agile*, ou toute créature dont les données l'accordent) joue deux tours par round : à son
initiative et à son initiative − 20. Les deux entrées de la liste désignent le même monstre : mêmes
PV, mêmes états. Les durées ne baissent qu'une fois par round.

À vérifier dans Mythic Adventures avant de s'y fier : le gabarit `Mythic Simple Template ~ Agile`
porte aussi `BONUS:COMBAT|INITIATIVE|20`.

## 7. Ce qui reste au réalisateur

- l'architecture interne (services, modèle, persistance) et le format du fichier de scénario, dans
  les conventions du projet (`specs/conjurations.md`, `specs/serveur-headless.md`) ;
- l'exposition éventuelle des opérations de la table de jeu au serveur MCP et HTTP ;
- le choix, écran par écran, entre composant Swing et vue web ;
- les raccourcis clavier exacts ;
- l'ordre de réalisation, pourvu que chaque étape livre quelque chose d'utilisable en partie : le
  suivi de combat d'abord, puisqu'il remplace la feuille Excel.
