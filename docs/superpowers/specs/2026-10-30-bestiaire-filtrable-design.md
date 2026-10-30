# Conception : un bestiaire filtrable de monstres et de PNJ

**Cible** : le MJ qui prépare une rencontre et cherche, parmi plusieurs milliers de créatures, celles
qui correspondent à ce qu'il veut (« un mort-vivant volant de FP 6 à 8 qui résiste au feu »).
**Complète** : `2026-10-23-table-de-jeu-design.md`, section 5, dont l'onglet **Bestiaire** ne cherchait
une race que par son nom.

---

## 1. Point de départ

L'onglet Bestiaire (`gui2/gametable/BestiaryPanel.java`) liste les races des sources chargées, filtrées
par leur nom ; il montre la fiche construite de la race choisie, l'envoie avec un nombre à la rencontre
sélectionnée ou au combat, et envoie son pion à Tabletop Simulator. Un PNJ n'entre dans une rencontre
que par « Depuis une fiche… », un fichier à la fois. Les retouches (gabarits ajoutés ou retirés, DV
ajoutés) existent dans `MonsterRecipe` et `RetouchDialog`, mais seulement depuis Rencontres et Combat.

## 2. Objectifs et hors périmètre

Objectifs :

1. Un seul catalogue liste les races de bestiaire, les PNJ enregistrés en `.pcg` et les kits de PNJ
   des livres.
2. Le MJ filtre ce catalogue sur n'importe quelle valeur de la fiche, combine ses critères et
   enregistre ses combinaisons.
3. Depuis le catalogue, il envoie une entrée à la rencontre sélectionnée, au combat ou à Tabletop
   Simulator.
4. Il fait d'une entrée une créature unique en lui ajoutant des gabarits et des DV, et la garde dans
   le scénario ou dans un `.pcg` réutilisable.

Hors périmètre : l'environnement et l'organisation des monstres, qu'aucune donnée chargée ne fournit.

## 3. Le catalogue

Une **entrée** du catalogue a une source parmi trois, chacune un cas de `MonsterSource` :

| Source | Cas de `MonsterSource` | Origine |
|---|---|---|
| race de bestiaire | `BestiaryRace` (existe) | races des sources chargées |
| PNJ en fichier | `CharacterFile` (existe) | `.pcg` des **dossiers de PNJ** |
| kit de PNJ | `NpcKit` (nouveau) | kits des sources chargées dont le `TYPE` contient `NPC` |

- **Dossiers de PNJ** : une liste réglée dans les préférences de la table de jeu, parcourue
  récursivement. Par défaut : `data/homebrew/*/pnj/` et `characters/`. Les PNJ de Combat Manager
  (`characters/combat-manager/`) en font donc partie.
- **Kit de PNJ** : PCGen crée un personnage vierge et lui applique le kit, comme `add_kit`. Les livres
  Pathfinder chargés en portent environ 60 (Core Rulebook, Dungeons of Golarion, adventure paths
  officiels) ; la source Zirnakaynin en porte 280. Un PNJ présent à la fois en `.pcg` et en kit
  apparaît deux fois, avec sa source affichée.
- Une race marquée `VISIBLE:NO` et les races de joueur (`FACT:IsPC`) restent listées : le filtre
  « catégorie » les écarte au besoin.

Le catalogue se recharge quand les sources chargées changent, à l'ouverture de l'onglet et par un
bouton « Actualiser », qui reparcourt les dossiers de PNJ.

## 4. L'index des valeurs

Un filtre ne porte que sur des valeurs connues pour chaque entrée. Elles viennent de deux endroits :

- **lues directement** dans les données, sans construire la créature : nom, source, type et
  sous-types, taille, FP, DV raciaux, vitesses, allonge, sens, langues, capacités, pouvoirs magiques,
  rôle, présence d'un pion et d'un plan tactique ;
- **calculées** en construisant la créature comme pour l'aperçu (`MonsterForge`) : CA, PV, jets, BBA,
  BMO, DMD, caractéristiques, compétences, RD, résistances, immunités, RM, attaques.

**Mesure** (30 octobre 2026, `preview_monster` par le serveur HTTP, `tous-les-monstres.pcg` chargé) :
une race se construit en 0,73 s (médiane ; moyenne 0,91 s, au pire 3 s), un `.pcg` en 0,85 s. Le
catalogue compte 4 542 races, 4 824 `.pcg` (dont 4 544 de Combat Manager) et environ 340 kits de PNJ :
environ 2 h 25 de construction en une fois. PCGen ne construit qu'une créature à la fois.

D'où deux index :

- **index livré**, pour les races et les kits des données : un script le calcule avec toutes les
  sources de bestiaire chargées et l'écrit dans les ressources de PCGen, clé par clé de race ou de kit
  et livre source. Il se régénère quand les données changent, comme les pions. Une race construite
  avec moins de sources peut différer légèrement de sa ligne d'index ; l'aperçu, lui, est toujours
  calculé sur les sources chargées.
- **index local**, pour les `.pcg` des dossiers de PNJ : construit en tâche de fond après le
  chargement, avec une barre de progression, et gardé sur disque. Un `.pcg` n'est recalculé que si sa
  date a changé. Le premier passage sur les PNJ de Combat Manager dure environ 1 h 05.

Tant qu'une entrée n'est pas calculée, un filtre sur une valeur calculée la marque « non calculée »
au lieu de l'écarter.

**Échecs de construction** : 3 races sur 150 font échouer la construction, parce que leurs données
exigent un don absent des sources chargées (`Alien Mindpaths`, `Ambush Squad`, `Technologist`) ou mal
écrit (`Fly-by Attack`, `Deadly Finish**`). L'exception remonte aujourd'hui jusqu'au serveur (erreur
500). Une construction qui échoue doit donner `ForgedMonster.Missing` avec la raison : l'entrée reste
au catalogue, marquée « non construite », avec cette raison dans l'aperçu.

## 5. Les filtres

Un **critère** est un champ, un opérateur et une ou plusieurs valeurs. Les opérateurs dépendent du
type du champ :

| Type de champ | Opérateurs |
|---|---|
| nombre | `=`, `≠`, `≥`, `≤`, entre |
| texte | contient, commence par, expression régulière |
| liste (sous-types, langues, capacités) | contient au moins un de, contient tous, ne contient aucun de |
| oui/non | oui, non |

Les critères se regroupent en **ET** ou en **OU**, et un groupe peut en contenir un autre. Un
**préréglage** est une combinaison nommée, enregistrée dans les préférences et proposée dans une liste
(« Sous-sol FP 5-8 volants »).

Champs proposés :

| Groupe | Champs |
|---|---|
| Identité | nom, livre source, source (race, fichier, kit), catégorie (monstre, PNJ, race de joueur), rôle (`ROLE`) |
| Nature | type, sous-types, taille, alignement (axe loi-chaos et axe bien-mal séparés) |
| Puissance | FP, DV, PV, rang mythique, gabarits portés |
| Défense | CA, CA de contact, CA pris au dépourvu, Réflexes, Vigueur, Volonté, RD (valeur et ce qui la passe), résistance par énergie, immunités, RM |
| Attaque | BBA, BMO, DMD, nombre d'attaques, attaques naturelles, allonge, attaque à distance |
| Déplacement | vitesse au sol, en vol, à la nage, en escalade, en fouissement ; manœuvrabilité en vol |
| Sens | vision dans le noir (portée), vision nocturne, perception des vibrations, vision aveugle, odorat |
| Magie | pouvoirs magiques, sort connu ou pouvoir magique précis, classe de lanceur, niveau de lanceur |
| Caractéristiques | For, Dex, Con, Int, Sag, Cha ; « sans Con », « sans Int » |
| Langues | langue parlée, télépathie |
| Capacités | capacité spéciale (texte) |
| Compétences | valeur totale d'une compétence |
| Table de jeu | a un pion, a un plan tactique |

## 6. L'onglet

- À gauche, la liste des critères et le choix du préréglage ; au centre, un **tableau** des entrées
  retenues, avec leur nombre ; à droite, l'aperçu de la fiche, comme aujourd'hui.
- Les colonnes du tableau se choisissent parmi les champs de la section 5 et se trient ; le choix est
  gardé dans les préférences. Par défaut : nom, FP, type, taille, CA, PV, source.
- La sélection peut porter sur plusieurs lignes ; le nombre s'applique à chacune.
- Actions : **→ rencontre** (sélectionnée), **→ combat**, **→ Tabletop Simulator** (pion, posé ou non
  sur la table), **Retoucher…**.
- La fenêtre « Depuis le bestiaire… » des onglets Rencontres et Combat ouvre le même tableau, avec les
  mêmes filtres.

## 7. Créatures uniques

**Retoucher…** ouvre `RetouchDialog` sur l'entrée choisie : gabarits ajoutés ou retirés, DV ajoutés,
et un nom propre. Les gabarits proposés sont ceux dont les prérequis sont remplis par la créature.
L'aperçu se recalcule à chaque changement, FP compris.

Le MJ garde la créature de deux façons, au choix :

- **dans le scénario** : une entrée de la liste des créatures uniques du `.pcgtable`, enregistrée
  comme recette (source, retouches, nom), comme les monstres de rencontre ;
- **dans un `.pcg`** : PCGen enregistre le personnage construit dans un dossier de PNJ choisi ; il
  apparaît ensuite dans le catalogue comme tout PNJ en fichier, d'un scénario à l'autre.

Les créatures uniques du scénario ouvert apparaissent dans le catalogue, avec la source « scénario ».

## 8. Vérification

- Tests unitaires de l'évaluation des critères (opérateurs, ET et OU imbriqués, valeur « non
  calculée »), sur des entrées construites à la main.
- Test de l'index : une entrée dont la source change est recalculée, les autres non.
- Test d'aller-retour du `.pcgtable` avec une créature unique.
- Test de construction d'un kit de PNJ (`NpcKit`) sur un kit de `dung_kits_npc.lst`.
- Vérification manuelle dans l'interface avec `characters/tous-les-monstres.pcg` et la source
  Zirnakaynin chargés.
