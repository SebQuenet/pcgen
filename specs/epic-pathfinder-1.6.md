# Specifications : Epic Pathfinder 1.6 pour PCGen

**Source** : *Pathfinder Epic-Level Handbook, v1.6* par Jesse Jack Jones
**Format cible** : Fichiers LST pour PCGen (data module homebrew)
**Scope** : Niveaux 21+ pour les 19 classes de base Pathfinder

---

## Table des matieres

1. [Vue d'ensemble](#1-vue-densemble)
2. [Phase 1 : Infrastructure & Regles universelles](#2-phase-1--infrastructure--regles-universelles)
3. [Phase 2 : Progressions de classe epiques](#3-phase-2--progressions-de-classe-epiques)
4. [Phase 3 : Competences epiques](#4-phase-3--competences-epiques)
5. [Phase 4 : Dons epiques](#5-phase-4--dons-epiques)
6. [Phase 5 : Objets magiques epiques](#6-phase-5--objets-magiques-epiques)
7. [Phase 6 : Magie epique](#7-phase-6--magie-epique)
8. [Phase 7 : Regles maison](#8-phase-7--regles-maison)
9. [Structure des fichiers](#9-structure-des-fichiers)
10. [Criteres d'acceptation](#10-criteres-dacceptation)

---

## 1. Vue d'ensemble

### 1.1 Objectif

Implementer l'integralite du contenu du *Pathfinder Epic-Level Handbook v1.6* dans PCGen sous forme d'un module de donnees homebrew. Ce module permettra de creer et gerer des personnages Pathfinder au-dela du niveau 20, jusqu'au niveau 30+ (avec formules extensibles).

### 1.2 Perimetre

| Chapitre PDF | Contenu | Priorite |
|---|---|---|
| Ch. 2 - Universal Advancement | XP, bonus epiques, jets de sauvegarde, scores de carac, dons | P0 (bloquant) |
| Ch. 3 - Class Features | 19 classes epiques (niv 21-30+) | P0 |
| Ch. 3 - Companions | Compagnons, eidolons, familiers epiques | P1 |
| Ch. 3 - Spells per Day | Tables de sorts/jour epiques (Tables 3-21 a 3-24) | P0 |
| Ch. 4 - Epic Skills | Utilisations epiques des competences | P1 |
| Ch. 5 - Epic Feats | ~300+ dons epiques en 12 categories | P0 |
| Ch. 6 - Epic Magic Items | Armures, armes, anneaux, sceptres, batons, objets merveilleux | P2 |
| Ch. 7 - Epic Spellcasting | Dons metamagiques, vrais dweomers, creation de sorts | P2 |
| Ch. 8 - House Rules | Regles optionnelles | P3 |

### 1.3 Contraintes techniques

- Le module doit s'integrer comme un dataset homebrew standard de PCGen
- Necessite le Core Rulebook Pathfinder et l'Advanced Player's Guide comme prerequis
- Les classes existantes doivent etre etendues (pas recrees) via `MODIFYOTHER` ou fichiers `.MOD`
- Le `MAXLEVEL` des classes doit passer de 20 a un niveau suffisant (40+ ou illimite)
- Les formules de bonus epiques utilisent le systeme `BONUS:` existant de PCGen

### 1.4 Structure du module

```
data/homebrew/epic_pathfinder_1_6/
  epic_pathfinder.pcc              # Fichier campagne principal
  epic_advancement.lst             # Regles d'avancement universel
  epic_classes.lst                 # Modifications des 19 classes (MOD)
  epic_class_abilities.lst         # Capacites de classe epiques
  epic_class_abilities_alchemist.lst
  epic_class_abilities_barbarian.lst
  epic_class_abilities_bard.lst
  epic_class_abilities_cavalier.lst
  epic_class_abilities_cleric.lst
  epic_class_abilities_druid.lst
  epic_class_abilities_fighter.lst
  epic_class_abilities_gunslinger.lst
  epic_class_abilities_inquisitor.lst
  epic_class_abilities_magus.lst
  epic_class_abilities_monk.lst
  epic_class_abilities_oracle.lst
  epic_class_abilities_paladin.lst
  epic_class_abilities_ranger.lst
  epic_class_abilities_rogue.lst
  epic_class_abilities_sorcerer.lst
  epic_class_abilities_summoner.lst
  epic_class_abilities_witch.lst
  epic_class_abilities_wizard.lst
  epic_abilitycategories.lst       # Categories de capacites epiques
  epic_feats.lst                   # Tous les dons epiques
  epic_feats_alchemical.lst
  epic_feats_channeling.lst
  epic_feats_combat.lst
  epic_feats_general.lst
  epic_feats_item_creation.lst
  epic_feats_ki.lst
  epic_feats_magic.lst
  epic_feats_metamagic.lst
  epic_feats_morale.lst
  epic_feats_rage.lst
  epic_feats_skill.lst
  epic_feats_summoning.lst
  epic_feats_wild.lst
  epic_skills.lst                  # Utilisations epiques des competences
  epic_companionmods.lst           # Compagnons/eidolons/familiers epiques
  epic_spells_per_day.lst          # Tables de sorts/jour
  epic_equipment_armor.lst         # Armures epiques
  epic_equipment_weapons.lst       # Armes epiques
  epic_equipment_rings.lst         # Anneaux epiques
  epic_equipment_rods.lst          # Sceptres epiques
  epic_equipment_staves.lst        # Batons epiques
  epic_equipment_scrolls.lst       # Parchemins epiques
  epic_equipment_wondrous.lst      # Objets merveilleux epiques
  epic_spellcasting.lst            # Regles de magie epique
```

---

## 2. Phase 1 : Infrastructure & Regles universelles

**Source** : Chapitre 2 (pages 4-5) - Table 2-1 & Table 2-2

### 2.1 Table d'experience etendue

Implementer la Table 2-2 (Extended Experience Chart) pour les niveaux 21 a 125 avec les trois pistes (Slow, Median, Fast).

**Formule XP** : `Previous Total + (5 x XP for CR encounter equal to current level)`

**Implementation** :
- Fichier gameMode ou table de reference dans le PCC
- Les valeurs XP exactes pour les niveaux 21-40 sont dans la Table 2-2
- Au-dela, la formule continue

### 2.2 Bonus epique d'attaque (Epic Attack Bonus - Su)

A partir du niveau 21, les personnages gagnent un bonus epique d'attaque :
- **+1 au niveau 22**, puis **+1 supplementaire tous les 4 niveaux** (26, 30, 34...)
- Ce bonus s'ajoute a l'attaque, au BMO et au DMD
- Il est inclus dans le BBA pour les prerequis de dons
- Il **ne stack PAS** avec d'autres bonus epiques d'attaque
- Il ne donne **jamais** d'attaques supplementaires

```
BONUS:COMBAT|BASEAB|(floor((TL-18)/4))|PREVARGTEQ:TL,21|TYPE=Epic
```

### 2.3 Bonus epique de sauvegarde (Epic Saving Throw Bonus - Su)

Bonus epique aux trois jets de sauvegarde (Vigueur, Reflexes, Volonte) :
- **+1 au niveau 21**, puis **+1 supplementaire tous les 3 niveaux** (24, 27, 30...)
- Stack avec tous les autres bonus sauf les bonus epiques de sauvegarde

```
BONUS:SAVE|BASE.Fortitude,BASE.Reflex,BASE.Will|(floor((TL-18)/3))|PREVARGTEQ:TL,21|TYPE=Epic
```

### 2.4 Augmentation de score de caracteristique

Continuation du systeme standard :
- **+1 a un score de caracteristique au niveau 24**, puis **tous les 4 niveaux** (28, 32, 36...)

Implementation via le systeme de level-up existant de PCGen (ability score increase pool).

### 2.5 Rangs de competence maximum

Le maximum de rangs reste egal au niveau du personnage, sans plafond a 20.

### 2.6 Dons de personnage bonus

Continuation des dons bonus tous les niveaux impairs :
- A partir du niveau 21, les dons bonus peuvent etre des **dons epiques**
- Les dons de classe bonus restent limites par type de classe

**Table 2-1 : Experience and Level-Dependent Benefits (niveaux 21-30+)**

| Niveau | Epic Attack Bonus | Epic Save Bonus | Ability Score Increase | Max Skill Ranks | Bonus Feats |
|---|---|---|---|---|---|
| 21 | -- | +1 | -- | 21 | 11e |
| 22 | +1 | +1 | -- | 22 | -- |
| 23 | +1 | +1 | -- | 23 | 12e |
| 24 | +1 | +2 | +1 | 24 | -- |
| 25 | +1 | +2 | -- | 25 | 13e |
| 26 | +2 | +2 | -- | 26 | -- |
| 27 | +2 | +3 | +1 | 27 | 14e |
| 28 | +2 | +3 | -- | 28 | -- |
| 29 | +2 | +3 | -- | 29 | 15e |
| 30 | +3 | +4 | -- | 30 | -- |
| +1 | +1/4 per level | +1/3 per level | +1/4 per level | +1 per level | +1 every odd level |

---

## 3. Phase 2 : Progressions de classe epiques

**Source** : Chapitre 3 (pages 5-30) - Tables 3-1 a 3-28

### 3.1 Capacites universelles de classe (Table 3-1)

Toutes les classes partagent ces regles au-dela du niveau 20 :

| Capacite | Regle |
|---|---|
| **Hit Die (HD)** | Continue au meme rythme (d6 a d12 selon la classe) |
| **Skill Points** | Continue au meme rythme (2+Int a 8+Int selon la classe) |
| **Favored Class** | Les bonus de classe de predilection continuent |
| **BAB / Saves** | Ne progressent plus automatiquement (remplaces par bonus epiques) |
| **Class Formulas** | Les capacites basees sur le niveau de classe continuent (rage, ki, etc.) |
| **DC abilities** | Les DC basees sur le niveau continuent d'augmenter |
| **Caster Level** | Continue d'augmenter, ainsi que les sorts/jour et sorts connus |
| **Companions** | Les compagnons continuent de progresser (Table 3-26) |

#### 3.1.1 Favored Class Bonus

Si un personnage epique monte de niveau dans sa classe de predilection, il continue a recevoir +1 PV, +1 rang de competence, ou une option raciale.

#### 3.1.2 Bonus Class Feat (Don de classe bonus)

Chaque classe gagne un don de classe bonus au **niveau 22** et un supplementaire tous les **2, 3 ou 4 niveaux** selon la classe. Chaque classe est limitee dans les types de dons epiques disponibles.

#### 3.1.3 Prowess (Prouesse)

Toutes les classes gagnent une prouesse de combat croissante. Selon la classe, +1 bonus a une attaque iterative tous les **1, 2 ou 4 niveaux**.

**Table 3-1 : Universal Class Features**

| Classe | HD | Skill Points | Prowess | Damage Bonus |
|---|---|---|---|---|
| Alchemist | d8 | 4+Int | +1 per 2 levels | -- |
| Barbarian | d12 | 4+Int | +1 per level | +1 per 2 levels |
| Bard | d8 | 6+Int | +1 per 2 levels | +1 per 4 levels |
| Cavalier | d10 | 4+Int | +1 per level | +1 per 3 levels |
| Cleric | d8 | 2+Int | +1 per 2 levels | +1 per 3 levels |
| Druid | d8 | 4+Int | +1 per 2 levels | +1 per 3 levels |
| Fighter | d10 | 2+Int | +1 per level | +1 per 2 levels |
| Gunslinger | d10 | 4+Int | +1 per level | -- |
| Inquisitor | d8 | 6+Int | +1 per 2 levels | +1 per 3 levels |
| Magus | d8 | 2+Int | +1 per 2 levels | +1 per 3 levels |
| Monk | d8 | 4+Int | +1 per 2 levels | +1 per 2 levels |
| Oracle | d8 | 4+Int | +1 per 2 levels | -- |
| Paladin | d10 | 2+Int | +1 per level | +1 per 2 levels |
| Ranger | d10 | 6+Int | +1 per level | +1 per 2 levels |
| Rogue | d8 | 8+Int | +1 per 2 levels | +1 per 4 levels |
| Sorcerer | d6 | 2+Int | +1 per 4 levels | -- |
| Summoner | d8 | 2+Int | +1 per 4 levels | -- |
| Witch | d6 | 2+Int | +1 per 4 levels | -- |
| Wizard | d6 | 2+Int | +1 per 4 levels | -- |

#### 3.1.4 Damage Bonus (Ex)

Nombreuses classes gagnent un bonus aux degats physiques. Ce bonus :
- Stack avec tous les autres bonus aux degats
- S'applique au corps-a-corps et a distance (armes de competence)
- Est multiplie sur un critique reussi
- S'applique meme aux degats non-letaux (si arme non-letale)

### 3.2 Alchemist epique (Table 3-2, pages 8-9)

**Progression niveaux 21-30 :**

| Niv | Prowess | Special |
|---|---|---|
| 21 | -- | Bomb (11d6), epic alchemy |
| 22 | +1 | Bonus class feat, epic alchemy |
| 23 | +1 | Bomb (12d6), epic alchemy |
| 24 | +2 | Epic alchemy |
| 25 | +2 | Bomb (13d6), bonus class feat, epic alchemy |
| 26 | +3 | Epic alchemy |
| 27 | +3 | Bomb (14d6), epic alchemy |
| 28 | +4 | Bonus class feat, epic alchemy |
| 29 | +4 | Bomb (15d6), epic alchemy |
| 30 | +5 | Epic alchemy |

**Capacites specifiques :**

- **Extracts (Su)** : Les niveaux epiques comptent pour le niveau de lanceur. Continue a gagner des extraits/jour (Table 3-22).
- **Discovery (Su)** : Ne gagne plus automatiquement de decouvertes. Peut encore en obtenir via le don Extra Discovery.
- **Bomb (Su)** : Continue a augmenter de +1d6 tous les niveaux impairs.
- **Epic Alchemy (Ex)** : A partir du niv 21, peut ameliorer des objets alchimiques normaux. Selectionne 3 objets au niv 21 et leur applique un amelioration parmi :
  - *Boosted* : Degats = Bomb class ability du niveau - 10
  - *Distilled* : DC = 10 + 1/2 niveau + mod Int
  - *Explosive* : Rayon d'effet double (primaire et splash)
  - *Incalescent* : Brule la cible primaire (rounds = mod Int)
  - *Marinade* : Application permanente (10 jours)
  - *Perfect* : Immunite a la condition + duree divisee
  - *Vitriolic* : Degats acide = niveau - 20 (rounds = mod Int)
  - *Voltaic* : Paralysie (rounds = mod Int)
  - Au niv 22 et chaque niveau pair : +1 objet ameliore
  - Au niv 25 : peut empiler un 2e amelioration (DC et cout augmentent)
  - Au niv 30/+5 niveaux : +1 amelioration supplementaire

**Bonus Class Feat** : au niv 22, puis tous les 3 niveaux (25, 28...). Types : Alchemical, Item Creation, Magic, Metamagic.

### 3.3 Barbarian epique (Table 3-3, pages 9-10)

**Progression niveaux 21-30 :**

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | +1 | +1 | Improved power, trap sense +7 |
| 22 | +2 | +1 | Bonus class feat, DR 6/-- |
| 23 | +3 | +2 | Epic rage |
| 24 | +4 | +2 | Improved power, trap sense +8 |
| 25 | +5 | +3 | Bonus class feat, DR 7/-- |
| 26 | +6 | +3 | Epic rage |
| 27 | +7 | +4 | Improved power, trap sense +9 |
| 28 | +8 | +4 | Bonus class feat, DR 8/-- |
| 29 | +9 | +5 | Epic rage |
| 30 | +10 | +5 | Improved power, trap sense +10 |

**Capacites specifiques :**

- **Rage (Ex)** : +2 rounds/jour par niveau au-dela du 20e.
- **Rage Power** : Ne gagne plus automatiquement. Peut via Extra Rage Power.
- **Improved Power** : Au niv 21 et tous les 3 niveaux, ameliore un rage power connu :
  - Single-use activable 1x/rage par 5 niveaux
  - DC du jet de sauvegarde +1
  - Bonus numeriques augmentes de 50%
  - Action requise reduite d'un cran (full > standard > move > swift > free)
  - Au niv 24/+3 niveaux : peut ameliorer un rage power supplementaire ou re-ameliorer
- **Trap Sense (Ex)** : +1 au niv 21, puis +1 tous les 3 niveaux.
- **Damage Reduction (Ex)** : +1 au niv 22, puis +1 tous les 3 niveaux.
- **Epic Rage (Ex)** : Au niv 23 et tous les 3 niveaux, selectionne un bonus permanent en rage :
  - Weapon damage die +1 step
  - +4 CMB and CMD
  - +15 temporary hit points
  - +2 to Will saves
  - Au niv 26/+3 niveaux : bonus supplementaire (stackable)

**Bonus Class Feat** : au niv 22, puis tous les 3 niveaux. Types : Combat, Rage.

### 3.4 Bard epique (Table 3-4, pages 10-11)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | -- | +1 | Epic performance |
| 22 | +1 | +1 | Bonus class feat |
| 23 | +1 | +1 | Inspire competence +8, inspire courage +5, lore master (4/day) |
| 24 | +2 | +1 | Bonus class feat |
| 25 | +2 | +2 | Epic performance |
| 26 | +3 | +2 | Bonus class feat |
| 27 | +3 | +2 | Inspire competence +10 |
| 28 | +4 | +2 | Bonus class feat |
| 29 | +4 | +3 | Epic performance, inspire courage +6, lore master (5/day) |
| 30 | +5 | +3 | Bonus class feat |

**Capacites specifiques :**

- **Spells** : Niveaux epiques comptent pour le caster level. +1 spell known par niveau (Table 3-22). Continue a gagner des sorts/jour.
- **Bardic Knowledge (Ex)** : Niveaux epiques comptent.
- **Bardic Performance** : +2 rounds/jour par niveau. DCs incluent les niveaux epiques.
- **Epic Performance (Su)** : Au niv 21, transcende les performances. 3 groupes de performances ameliorables :
  - **Groupe 1 (allies)** : Inspire Courage/Competence/Greatness/Heroics - bonus au choix :
    - +2 moral aux degats / +2 moral CMB/CMD / +1 moral AC / +10 PV temp / +1 moral saves / +10 ft vitesse
  - **Groupe 2 (ennemis)** : Fascinate/Suggestion/Frightening Tune/Mass Suggestion/Deadly Performance - bonus au choix :
    - +1 DC resistance / +1 nombre de cibles / +10 ft rayon / Dirge of Doom => Frightened, Frightening Tune => Panicked / Retry possible dans les 24h
  - **Groupe 3 (Soothing Performance)** : Bonus au choix :
    - Cure Nausea / Cure Exhaustion / Cure Frightened or Panicked / Cure poison / Cure disease / Upgrade healing to mass cure critical wounds / Rounds -1 (min 1)
  - Au niv 25/+4 niveaux : selectionne un groupe supplementaire ou ameliore un existant

- **Inspire Competence (Su)** : +2 au niv 23, +2 tous les 4 niveaux.
- **Inspire Courage (Su)** : +1 au niv 23, +1 tous les 6 niveaux.
- **Lore Master (Ex)** : +1 utilisation/jour au niv 23, +1 tous les 6 niveaux.

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux (24, 26...). Types : Magic, Morale, Skill.

### 3.5 Cavalier epique (Table 3-5, pages 11-12)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | +1 | +1 | Epic challenge |
| 22 | +2 | +1 | Bonus class feat, challenge (8/day) |
| 23 | +3 | +1 | Epic banner |
| 24 | +4 | +2 | Bonus class feat |
| 25 | +5 | +2 | Challenge (9/day), epic challenge, tactician (6/day) |
| 26 | +6 | +2 | Bonus class feat |
| 27 | +7 | +3 | Epic banner |
| 28 | +8 | +3 | Bonus class feat, challenge (10/day) |
| 29 | +9 | +3 | Epic challenge |
| 30 | +10 | +4 | Bonus class feat, tactician (7/day) |

**Capacites specifiques :**

- **Mount (Ex)** : Continue de progresser (Table 3-26).
- **Order (Ex)** : Niveaux epiques comptent pour les bonus de l'Ordre.
- **Banner (Ex)** : Au niv 25, les bonus augmentent de +1, puis +1 tous les 5 niveaux.
- **Epic Challenge (Su)** : Au niv 21, ajoute des effets au Challenge (DC 10 + 1/2 niveau + mod Cha). Conditions infligeables :
  - Bleed 5 (Fort) / Dazzled (Will) / Deafened (Fort) / Fatigued (Fort) / Shaken (Will) / Sickened (Fort)
  - Au niv 25/+4 niveaux : amelioration (select effect, inflict with 2nd attack, improve condition tier)
  - Conditions ameliorables : Bleed 5>10>15, Dazzled>Dazed>Paralyzed, etc.
- **Challenge (Ex)** : +1 utilisation/jour au niv 22, +1 tous les 3 niveaux. Degats incluent niveaux epiques.
- **Tactician (Ex)** : +1 utilisation/jour au niv 25, +1 tous les 5 niveaux.

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Combat, Morale.

### 3.6 Cleric epique (Table 3-6, page 12)

| Niv | Prowess | Special |
|---|---|---|
| 21 | -- | Channel energy (11d6) |
| 22 | +1 | Bonus class feat, epic aura +1 |
| 23 | +1 | Channel energy (12d6) |
| 24 | +2 | Epic aura +2 |
| 25 | +2 | Bonus class feat, channel energy (13d6) |
| 26 | +3 | Epic aura +3 |
| 27 | +3 | Channel energy (14d6) |
| 28 | +4 | Bonus class feat, epic aura +4 |
| 29 | +4 | Channel energy (15d6) |
| 30 | +5 | Epic aura +5 |

**Capacites specifiques :**

- **Spells** : Niveaux epiques comptent pour le caster level. Sorts/jour : Table 3-21. Pas de bonus spells au-dela du 9e niveau.
- **Domain** : Niveaux epiques comptent pour les pouvoirs de domaine.
- **Channel Energy (Su)** : +1d6 au niv 21, +1d6 tous les niveaux impairs.
- **Epic Aura (Su)** : Au niv 22, aura de 30 ft donnant +1 AC et saves (selon circonstances). +1 supplementaire a chaque niveau pair.
  - Channel positive => bonus sacre contre attaques/sorts mauvais, creatures mauvaises, undead
  - Channel negative => bonus impie contre attaques/sorts bons, creatures bonnes

**Bonus Class Feat** : au niv 22, puis tous les 3 niveaux (25, 28, 31...). Types : Channeling, Item Creation, Magic, Metamagic.

### 3.7 Druid epique (Table 3-7, page 13)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | -- | +1 | Wild feat |
| 22 | +1 | +1 | Bonus class feat |
| 23 | +1 | +1 | Epic ally (+2HD) |
| 24 | +2 | +2 | Wild feat |
| 25 | +2 | +2 | Bonus class feat |
| 26 | +3 | +2 | Epic ally (+4HD) |
| 27 | +3 | +3 | Wild feat |
| 28 | +4 | +3 | Bonus class feat |
| 29 | +4 | +3 | Epic ally (+6HD) |
| 30 | +5 | +4 | Wild feat |

**Capacites specifiques :**

- **Nature Bond (Ex)** : Domaine ou compagnon continue de progresser.
- **Wild Empathy (Ex)** : Niveaux epiques comptent.
- **Wild Feat (Ex)** : Au niv 21, selectionne un don Combat ou Monster non-epique dont il remplit les prerequis dans au moins une wild shape. Au niv 24/+3 niveaux : +1 don. Les dons peuvent servir de prerequis.
- **Epic Ally (Su)** : Au niv 23, summon nature's ally invoque des creatures avec +2 bonus HD (et tous les bonus associes) + un don supplementaire. Au niv 26/+3 niveaux : +2 HD supplementaires.

**Bonus Class Feat** : au niv 22, puis tous les 3 niveaux. Types : Magic, Metamagic, Wild.

### 3.8 Fighter epique (Table 3-8, page 13-14)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | +1 | +1 | Warrior (1/day), weapon training |
| 22 | +2 | +1 | Bonus class feat, bravery +6 |
| 23 | +3 | +2 | Warrior (2/day) |
| 24 | +4 | +2 | Bonus class feat |
| 25 | +5 | +3 | Warrior (3/day), weapon training |
| 26 | +6 | +3 | Bonus class feat, bravery +7 |
| 27 | +7 | +4 | Warrior (4/day) |
| 28 | +8 | +4 | Bonus class feat |
| 29 | +9 | +5 | Warrior (5/day), weapon training |
| 30 | +10 | +5 | Bonus class feat, bravery +8 |

**Capacites specifiques :**

- **Bravery (Ex)** : +1 au niv 22, +1 tous les 4 niveaux.
- **Armor Training (Ex)** : Cesse de s'ameliorer apres le 20e.
- **Damage Bonus (Ex)** : +1 au niv 21, +1 tous les 2 niveaux.
- **Warrior (Ex)** : 1x/jour au niv 21, +1 au niv 23 et +1 tous les 2 niveaux. En swift action, active un pouvoir parmi :
  - Attaque supplementaire au BBA le plus haut (stacks avec haste/speed)
  - Appliquer Weapon Mastery a n'importe quelle arme qualifiante pour 1h
  - Lancer 2x les des d'attaque et garder le meilleur (jusqu'au prochain tour)
  - Ignorer les attaques d'opportunite jusqu'au prochain tour
  - Reduire tous les degats de 50% (min 1 PV) jusqu'au prochain tour
  - Reussir automatiquement 1 save + annuler les effets secondaires
  - Cleave : free Bull Rush sur chaque cible (bonus CMB si Great Cleave)
  - Vital Strike : free Trip sur la cible (bonus CMB si Improved/Greater)
  - Precise Shot : free Dirty Trick (bonus si Improved)
- **Weapon Training (Ex)** : +1 groupe d'armes tous les 4 niveaux. Au niv 21 : +1 au nouveau groupe, +1 aux groupes precedents (max +4 par groupe).

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Combat.

### 3.9 Gunslinger epique (Table 3-9, page 14-15)

| Niv | Prowess | Special |
|---|---|---|
| 21 | +1 | Tinker |
| 22 | +2 | Bonus class feat |
| 23 | +3 | Shooter's stance +1d6 |
| 24 | +4 | Tinker |
| 25 | +5 | Bonus class feat |
| 26 | +6 | Shooter's stance +2d6 |
| 27 | +7 | Tinker |
| 28 | +8 | Bonus class feat |
| 29 | +9 | Shooter's stance +3d6 |
| 30 | +10 | Tinker |

**Capacites specifiques :**

- **Deeds (Ex)** : Niveaux epiques comptent pour les DCs (Menacing Shot, Death Shot, Stunning Shot).
- **Nimble (Ex)** : Cesse de s'ameliorer apres le 20e.
- **Gun Training (Ex)** : Cesse de s'ameliorer apres le 20e.
- **Tinker (Ex)** : Au niv 21 et tous les 3 niveaux, amelioration permanente d'arme a feu (1 semaine) :
  - +5' increment de portee
  - +1 confirmation critique
  - +2 durete arme
  - Double PV arme
  - Au niv 24/+3 niveaux : bonus supplementaire (stackable)
- **Shooter's Stance (Ex)** : Au niv 23, sacrifie le dodge bonus AC de Nimble pour +1d6 degats/attaque avec arme a feu. +1d6 au niv 26/+3 niveaux. Recupere 1 grit par attaque reussie ce round.

**Bonus Class Feat** : au niv 22, puis tous les 3 niveaux. Types : Combat.

### 3.10 Inquisitor epique (Table 3-10, pages 15-16)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | -- | +1 | Reckoning (1/day) |
| 22 | +1 | +1 | Bonus class feat, judgment (8/day) |
| 23 | +1 | +1 | Epic judgement |
| 24 | +2 | +2 | Bonus class feat |
| 25 | +2 | +2 | Judgment (9/day), epic judgement |
| 26 | +3 | +2 | Bonus class feat, epic judgement |
| 27 | +3 | +3 | Reckoning (2/day) |
| 28 | +4 | +3 | Bonus class feat, judgment (10/day) |
| 29 | +4 | +3 | Epic judgement |
| 30 | +5 | +4 | Bonus class feat |

**Capacites specifiques :**

- **Spells** : Caster level inclut niveaux epiques. Sorts/jour : Table 3-22. +1 spell known/level.
- **Domain / Stern Gaze / Track / Bane / Discern Lies** : Niveaux epiques comptent.
- **Judgment (Su)** : +1/jour au niv 22/+3 niveaux. Cesse de s'ameliorer automatiquement.
- **Epic Judgement (Su)** : Au niv 23, selectionne un bonus permanent aux Judgements :
  - +10 PV temporaires / +2 sacre aux skill checks / +2 sacre CMB & CMD / +1 sacre DC sorts / Reroll 1 save (doit garder le 2e) / Immunite Confused / Immunite Dazzled / Immunite Fatigue / Immunite Shaken / Immunite Sickened
  - Au niv 26/+3 niveaux : bonus supplementaire (stackable)
- **Reckoning (Su)** : 1x/jour au niv 21. Standard action (provoque AoO), termine tous les Judgements, touche de melee. Si touche, inflige une penalite basee sur le Judgement actif :
  - Destruction / Healing / Justice / Piercing / Protection / Purity / Resiliency / Resistance / Smiting
  - Peut tenter save (DC 10 + 1/2 niveau + mod Wis) pour reduire a 1 round.
  - Au niv 27/+6 niveaux : +1 Reckoning/jour

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Combat, Magic, Skill.

### 3.11 Magus epique (Table 3-11, pages 16-17)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | -- | +1 | Epic spell access, epic arcana |
| 22 | +1 | +1 | Bonus class feat |
| 23 | +1 | +1 | Epic spell access |
| 24 | +2 | +2 | Bonus class feat, epic arcana |
| 25 | +2 | +2 | Epic spell access |
| 26 | +3 | +2 | Bonus class feat |
| 27 | +3 | +3 | Epic spell access, epic arcana |
| 28 | +4 | +3 | Bonus class feat |
| 29 | +4 | +3 | Epic spell access |
| 30 | +5 | +4 | Bonus class feat, epic arcana |

**Capacites specifiques :**

- **Spells** : Caster level inclut niveaux epiques. Sorts/jour : Table 3-22.
- **Arcane Pool (Su)** : Max enhancement +6 au niv 21, +1 tous les 4 niveaux (max +10).
- **Fighter Training (Ex)** : Niveaux epiques comptent.
- **Magus Arcana** : Ne gagne plus automatiquement. Peut via Extra Arcana.
- **Epic Spell Access (Su)** : Au niv 21, ajoute 1 sort de la liste wizard de meme niveau. Au niv 23/+2 niveaux : +1 sort.
- **Epic Arcana (Su)** : Au niv 21, selectionne un bonus permanent pour l'arcane pool :
  - Enhancing weapon : duree 1min => 10min (Enduring Blade : +5 min/magus level)
  - Enhancing weapon : cout reduit de 1 (min 1)
  - Improved Spell Recall : DC +1
  - Arcane Redoubt/Greater : bonus touch AC +2 et Reflex saves +2
  - Dispelling Strike : dispel check +2, peut dissiper sorts de niveau = 2x points depenses
  - Pool Strike : degats +1d6
  - Au niv 24/+3 niveaux : bonus supplementaire (stackable)

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Item Creation, Magic, Metamagic.

### 3.12 Monk epique (Table 3-12, pages 17-18)

| Niv | Prowess | Damage Bonus | AC Bonus | Fast Movement | Special |
|---|---|---|---|---|---|
| 21 | -- | +1 | +5 | +70 ft | Epic insight |
| 22 | +1 | +1 | +5 | +70 ft | Bonus class feat |
| 23 | +1 | +2 | +5 | +70 ft | Epic insight |
| 24 | +2 | +2 | +6 | +80 ft | Bonus class feat |
| 25 | +2 | +3 | +6 | +80 ft | Epic insight |
| 26 | +3 | +3 | +6 | +80 ft | Bonus class feat |
| 27 | +3 | +4 | +6 | +90 ft | Epic insight |
| 28 | +4 | +4 | +7 | +90 ft | Bonus class feat |
| 29 | +4 | +5 | +7 | +90 ft | Epic insight |
| 30 | +5 | +5 | +7 | +100 ft | Bonus class feat |

**Capacites specifiques :**

- **AC Bonus (Ex)** : +1 au niv 24/+4 niveaux.
- **Flurry of Blows (Ex)** : Cesse de s'ameliorer automatiquement. Les bonus epiques d'attaque s'appliquent au flurry mais ne donnent pas d'attaques supplementaires.
- **Unarmed Strike (Ex)** : Cesse de s'ameliorer automatiquement apres le 20e.
- **Stunning Fist (Ex)** : Niveaux epiques comptent pour les utilisations/jour et le DC.
- **Fast Movement (Ex)** : +10 ft au niv 21/+3 niveaux.
- **Maneuver Training (Ex)** : Niveaux epiques comptent pour le CMB.
- **Ki Pool (Su)** : Niveaux epiques comptent.
- **High Jump (Ex)** : Niveaux epiques comptent.
- **Wholeness of Body (Su)** : Niveaux epiques comptent.
- **Abundant Step (Su)** : Niveaux epiques comptent.
- **Diamond Soul (Ex)** : Niveaux epiques comptent.
- **Quivering Palm (Su)** : Niveaux epiques comptent.
- **Epic Insight (Su)** : Au niv 21 et tous les 2 niveaux, si au moins 1 point de ki, selectionne un bonus permanent parmi :
  - +8 insight a 1 competence parmi Acrobatics/Climb/Escape Artist/Fly/Heal/Perception/Perform/Ride/Sense Motive/Sleight of Hand/Stealth/Survival/Swim (1x par 12 niveaux)
  - +4 insight CMB pour 1 manoeuvre de combat
  - +1 insight aux degats avec flurry of blows
  - +1 Stunning Fist DC (1x par 8 niveaux)
  - Augmenter duree Sickness/Stagger/Paralysis de Stunning Fist +1 round (1x par 6 niveaux par condition)
  - Ki pool dodge bonus AC +2 (1x par 8 niveaux)
  - Ki pool speed bonus +10 ft
  - Wholeness of Body guerit +8 PV et 2 degats de carac
  - +1 SR de Diamond Soul (1x par 8 niveaux)
  - Augmenter DR/chaos de 2
  - Au niv 23/+2 niveaux : bonus supplementaire

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Combat, Ki, Skill.

### 3.13 Oracle epique (Table 3-13, page 18-19)

| Niv | Prowess | Special |
|---|---|---|
| 21 | -- | Unraveled mystery (-1/+1) |
| 22 | +1 | Bonus class feat |
| 23 | +1 | Epic revelation |
| 24 | +2 | Unraveled mystery (-2/+1) |
| 25 | +2 | Bonus class feat |
| 26 | +3 | Epic revelation |
| 27 | +3 | Unraveled mystery (-3/+2) |
| 28 | +4 | Bonus class feat |
| 29 | +4 | Epic revelation |
| 30 | +5 | Unraveled mystery (-4/+2) |

**Capacites specifiques :**

- **Spells** : Caster level inclut niveaux epiques. Sorts/jour : Table 3-23. +1 spell known/level.
- **Revelation** : Ne gagne plus automatiquement. Peut via Extra Revelation.
- **Unraveled Mystery (Su)** : Au niv 21, peut ajouter des metamagic feats a ses Mystery Spells en free action. Ajustement de niveau combine reduit (de 1 au niv 21, puis -1 supplementaire au niv 24/+3 niveaux). Mystery Spells gagnent +1 bonus aux DC/caster level checks/Spell Resistance/opposed rolls. Au niv 27/+6 niveaux : ce bonus augmente de +1.
- **Epic Revelation (Su)** : Au niv 23 et tous les 3 niveaux, ameliore une Revelation :
  - Duree double / Portee +50% / Zone +5 ft rayon ou +10 ft longueur / DC saves +1 / Degats infliges ou gueris +4 / Bonus AC/CMD/saves/initiative +1 / Bonus skill checks +2

**Bonus Class Feat** : au niv 22, puis tous les 3 niveaux (25, 28...). Types : Item Creation, Magic, Metamagic.

### 3.14 Paladin epique (Table 3-14, pages 19-20)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | +1 | +1 | Epic smite (1/day) |
| 22 | +2 | +1 | Bonus class feat, smite evil (8/day) |
| 23 | +3 | +2 | Divine mercy |
| 24 | +4 | +2 | Bonus class feat |
| 25 | +5 | +3 | Epic smite (2/day), smite evil (9/day) |
| 26 | +6 | +3 | Bonus class feat |
| 27 | +7 | +4 | Divine mercy |
| 28 | +8 | +4 | Bonus class feat, smite evil (10/day) |
| 29 | +9 | +5 | Epic smite (2/day) |
| 30 | +10 | +5 | Bonus class feat |

**Capacites specifiques :**

- **Spells** : Caster level inclut niveaux epiques. Sorts/jour : Table 3-24.
- **Aura of Good (Ex)** : Niveaux epiques comptent.
- **Smite Evil (Su)** : +1/jour au niv 22/+3 niveaux. Niveaux epiques comptent pour les degats.
- **Lay on Hands (Su)** : Niveaux epiques comptent.
- **Mercy (Su)** : Ne gagne plus automatiquement. Peut via Extra Mercy.
- **Channel Energy (Su)** : Niveaux epiques comptent pour les PV gueris/infliges.
- **Divine Bond (Sp)** : Continue de progresser. Enhancement max +7 au niv 23, +1/3 niveaux (max +10).
- **Holy Champion (Su)** : Niveaux epiques comptent pour banishment.
- **Epic Smite (Su)** : Au niv 21, 1x/jour, en declarant Smite Evil, selectionne un chemin pour des bonus :
  - **Diligence** : Ignore terrain difficile, ne peut etre immobilise, freedom of movement, +10 ft vitesse (+10/2 niveaux)
  - **Faith** : Bonus saves +2 de Divine Grace, aura bonuses +1 (+1/2 niveaux)
  - **Glory** : CMB/CMD = niveau de classe, manoeuvres +2 categories de taille, au niv 23/+2 niveaux : +1 taille max
  - **Honor** : Bonus Cha a AC (touch + flat-footed), +1 au niv 23/+2 niveaux, PV temp = niveau de classe
  - **Temperance** : DR = niveau de classe (Holy Champion), resistance energie = niveau de classe (acid, cold, elec, fire)
  - Duree : rounds = bonus Cha du paladin (min 1) ou jusqu'a mort de la cible Smite Evil
  - Au niv 25/+4 niveaux : peut utiliser +1/jour

- **Divine Mercy (Su)** : Au niv 23/+4 niveaux, Lay on Hands guerit 1 condition negative additionnelle. Au niv 27/+4 niveaux : +1 immunite. Iterations ulterieures : +1 holy bonus aux saves du Lay on Hands.

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Channeling, Combat, Morale.

### 3.15 Ranger epique (Table 3-15, pages 20-21)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | +1 | +1 | Epic combat style |
| 22 | +2 | +1 | Bonus class feat |
| 23 | +3 | +2 | Wild hunter (1/day) |
| 24 | +4 | +2 | Bonus class feat |
| 25 | +5 | +3 | Epic combat style |
| 26 | +6 | +3 | Bonus class feat |
| 27 | +7 | +4 | Wild hunter (2/day) |
| 28 | +8 | +4 | Bonus class feat |
| 29 | +9 | +5 | Epic combat style |
| 30 | +10 | +5 | Bonus class feat |

**Capacites specifiques :**

- **Spells** : Sorts/jour : Table 3-24.
- **Favored Enemy (Ex)** : Cesse de gagner automatiquement. Au niv 25/+5 niveaux : selectionne 1 ennemi existant, +2.
- **Track / Wild Empathy / Favored Terrain** : Similaire (niveaux epiques comptent ou cessent avec ameliorations possibles).
- **Hunter's Bond (Ex)** : Continue de progresser.
- **Epic Combat Style (Ex)** : Au niv 21, lorsqu'il utilise une arme de son style de combat, gagne un bonus permanent (au choix, ne peut etre change) :
  - +2 degats / +2 CMB et CMD / +4 confirmation critique / +10 ft increment portee (archery/crossbow/thrown) / +1 AC (mounted/natural/two-handed/weapon & shield) / +2 Reflex (archery/combat/natural/mounted/thrown/two-weapon) / +2 Fortitude (mounted/natural/two-handed/weapon & shield) / -1 attaque penalty (mounted/two-weapon/weapon & shield) / +1 DC instant death du master hunter
  - Au niv 25/+4 niveaux : bonus supplementaire (stackable)
- **Wild Hunter (Ex)** : Au niv 23, 1x/jour, apres Quarry, active : true seeing + bonus Wis a saves et CMD + confirmation critiques automatique. Duree : 1/2 niveau + Wis rounds. Au niv 27/+4 niveaux : +1/jour.

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Combat, Skill.

### 3.16 Rogue epique (Table 3-16, pages 21-22)

| Niv | Prowess | Damage Bonus | Special |
|---|---|---|---|
| 21 | -- | +1 | Epic skill, sneak attack +11d6, trap sense +7 |
| 22 | +1 | +1 | Bonus class feat |
| 23 | +1 | +1 | Epic skill, sneak attack +12d6 |
| 24 | +2 | +1 | Bonus class feat, trap sense +8 |
| 25 | +2 | +2 | Epic skill, sneak attack +13d6 |
| 26 | +3 | +2 | Bonus class feat |
| 27 | +3 | +2 | Epic skill, sneak attack +14d6, trap sense +9 |
| 28 | +4 | +2 | Bonus class feat |
| 29 | +4 | +3 | Epic skill, sneak attack +15d6 |
| 30 | +5 | +3 | Bonus class feat |

**Capacites specifiques :**

- **Sneak Attack** : +1d6 au niv 21, +1d6 tous les niveaux impairs.
- **Trapfinding** : Niveaux epiques comptent.
- **Trap Sense (Ex)** : +1 au niv 21/+3 niveaux.
- **Rogue Talent** : Ne gagne plus automatiquement. Peut via Extra Rogue Talent.
- **Epic Skill (Ex)** : Au niv 21 et tous les 2 niveaux, selectionne un bonus permanent :
  - +2 a 1 class skill (1x par 6 niveaux par skill)
  - +2 CMB pour 1 manoeuvre de combat
  - +2 confirmation critique
  - +1 DC poisons utilises
  - +2 sneak attack degats
  - +10 ft vitesse (1x par 6 niveaux)
  - 1 AoO supplementaire/round (1x par 6 niveaux)

**Bonus Class Feat** : au niv 22, puis tous les 2 niveaux. Types : Combat, Skill.

### 3.17 Sorcerer epique (Table 3-17, pages 22-23)

| Niv | Prowess | Special |
|---|---|---|
| 21 | -- | Arcane conversion |
| 22 | -- | Bonus class feat, epic bloodline +2 |
| 23 | -- | Arcane conversion |
| 24 | +1 | Epic bloodline +3 |
| 25 | +1 | Arcane conversion |
| 26 | +1 | Bonus class feat, epic bloodline +4 |
| 27 | +1 | Arcane conversion |
| 28 | +2 | Epic bloodline +5 |
| 29 | +2 | Arcane conversion |
| 30 | +2 | Bonus class feat, epic bloodline +6 |

**Capacites specifiques :**

- **Spells** : Caster level inclut niveaux epiques. Sorts/jour : Table 3-23. +1 spell known/level.
- **Bloodline** : Niveaux epiques comptent. Ne gagne plus automatiquement de bloodline spells/feats/powers.
- **Arcane Conversion (Su)** : Au niv 21, designe des emplacements de sorts convertibles (nombre = bonus Cha, min 1). Niveaux 1 a 4. En activant un convertible (swift action) lors d'un sort, gagne un bonus au choix :
  - Metamagic feat (max level adjustment = 1/2 niveau du slot converti)
  - Bonus DC = 1/2 niveau du slot / Bonus caster level = niveau du slot / Bonus penetrate SR = 2x niveau du slot / Bonus DC pour dissiper = 2x niveau du slot / Si effets level-dependent (degats, HD max, etc.) : cap augmente = 2x niveau du slot
  - Au niv 23/+2 niveaux : +2 slots convertibles et +1 niveau max eligible
- **Epic Bloodline (Su)** : Au niv 22, ameliore les bloodline powers. En swift action, applique des metamagic feats (ajustement combine +2 ou moins). Au niv 24/+2 niveaux : ajustement augmente de +1. Utilisations/jour = mod Cha (min 1).

**Bonus Class Feat** : au niv 22, puis tous les 4 niveaux (26, 30...). Types : Item Creation, Magic, Metamagic.

### 3.18 Summoner epique (Table 3-18, pages 23-24)

| Niv | Prowess | Special |
|---|---|---|
| 21 | -- | Improved summons +1 |
| 22 | +1 | Bonus class feat |
| 23 | +1 | Epic summons (1/day) |
| 24 | +2 | Improved summons +2 |
| 25 | +2 | Bonus class feat |
| 26 | +3 | Epic summons (2/day) |
| 27 | +3 | Improved summons +3 |
| 28 | +4 | Bonus class feat |
| 29 | +4 | Epic summons (3/day) |
| 30 | +5 | Improved summons +4 |

**Capacites specifiques :**

- **Spells** : Table 3-22. +1 spell known/level.
- **Eidolon (Su)** : Continue de progresser (Table 3-27).
- **Summon Monster (Sp)** : Niveaux epiques comptent (duree, HD max, gate).
- **Bond Senses / Maker's Call / Merge Forms / Twin Eidolon** : Niveaux epiques comptent.
- **Improved Summons (Sp)** : Au niv 21 et tous les 3 niveaux, +1 creature invoquee par summon monster. Ou forgo pour appliquer des metamagic feats. Au niv 24/+3 niveaux : +1 creature, -2000 gp gate.
- **Epic Summons (Sp)** : Au niv 23, 1x/jour en swift action, les creatures invoquees gagnent : max HP/HD, bonus HP = niveau du summoner, attaques bypass DR/epic, bonus insight (attaque/CMB/degats) = prowess, deflection AC/CMD = mod Cha, insight saves = mod Cha.
  - Au niv 26/+3 niveaux : +1/jour.

**Bonus Class Feat** : au niv 22, puis tous les 3 niveaux (25, 28...). Types : Item Creation, Magic, Metamagic, Summoning.

### 3.19 Witch epique (Table 3-19, pages 24-25)

| Niv | Prowess | Special |
|---|---|---|
| 21 | -- | Epic hex |
| 22 | -- | Bonus class feat, metahex (+1, 3/day) |
| 23 | -- | Epic hex |
| 24 | +1 | Metahex (+2, 4/day) |
| 25 | +1 | Epic hex |
| 26 | +1 | Bonus class feat, metahex (+2, 5/day) |
| 27 | +1 | Epic hex |
| 28 | +2 | Metahex (+3, 6/day) |
| 29 | +2 | Epic hex |
| 30 | +2 | Bonus class feat, metahex (+3, 7/day) |

**Capacites specifiques :**

- **Spells** : Table 3-21 (Clerics/Druids/Witches/Wizards).
- **Hex** : Niveaux epiques comptent pour les DCs et effets. Ne gagne plus automatiquement. Peut via Extra Hex.
- **Patron Spells** : Ne gagne plus automatiquement.
- **Witch's Familiar (Ex)** : Continue de progresser (Table 3-28).
- **Epic Hex (Su)** : Au niv 21, ameliore un hex avec tous les bonus applicables :
  - +1 utilisation/jour / DC +1 / Portee et zone +10 ft / Duree +1 round / Penalites infligees -2 / Bonus +2 / Degats infliges +2 / Degats de carac +1
  - Au niv 23/+2 niveaux : hex supplementaire (ou re-ameliorer, stackable)
- **Metahex (Su)** : Au niv 22, applique metamagic feats aux hex passifs (full-round action). Ajustement max combine +1, 3x/jour. Au niv 24/+2 niveaux : +1/jour et +1 ajustement par 4 niveaux.

**Bonus Class Feat** : au niv 22, puis tous les 4 niveaux (26, 30...). Types : Item Creation, Magic, Metamagic.

### 3.20 Wizard epique (Table 3-20, pages 25-26)

| Niv | Prowess | Special |
|---|---|---|
| 21 | -- | Spell boost, epic cantrip (1st) |
| 22 | -- | Bonus class feat, epic cantrip (1st) |
| 23 | -- | Spell boost, epic cantrip (1st) |
| 24 | +1 | Epic cantrip (2nd) |
| 25 | +1 | Spell boost, epic cantrip (2nd) |
| 26 | +1 | Bonus class feat, epic cantrip (2nd) |
| 27 | +1 | Spell boost, epic cantrip (2nd) |
| 28 | +2 | Epic cantrip (3rd) |
| 29 | +2 | Spell boost, epic cantrip (3rd) |
| 30 | +2 | Bonus class feat, epic cantrip (3rd) |

**Capacites specifiques :**

- **Spells** : Table 3-21. Niveaux epiques comptent pour le caster level.
- **Arcane Bond (Ex or Sp)** : Continue de progresser (objet lie ou familier).
- **Arcane School** : Niveaux epiques comptent.
- **Spell Boost (Su)** : Au niv 21, selectionne un sort du spellbook. Ce sort gagne tous les bonus :
  - DC = 10 + 1/2 niveau + mod Int / Bonus grants (ability scores, AC, attack, saves) +2 / Bonus grants (CMB, CMD, damage, skill checks) +4 / +4 penetrate SR / +4 concentration checks
  - Au niv 23/+2 niveaux : sort supplementaire (chaque sort = different)
- **Epic Cantrip (Su)** : Au niv 21, convertit un sort de niveau 1 en cantrip (slot de niveau 0). Au niv 22/+1 niveaux : +1 cantrip. Au niv 24 : peut convertir des sorts de niveaux 1 ou 2. Au niv 28/+4 niveaux : +1 niveau convertible max.

**Bonus Class Feat** : au niv 22, puis tous les 4 niveaux (26, 30...). Types : Item Creation, Magic, Metamagic.

### 3.21 Tables de sorts/jour epiques

**4 tables distinctes a implementer :**

- **Table 3-21** : Spells per Day - Epic Clerics, Druids, Witches, Wizards (prepared, 9-level, niv 21-40)
- **Table 3-22** : Extracts or Spells per Day - Epic Alchemists, Bards, Inquisitors, Magi, Summoners (6-level, niv 21-40)
- **Table 3-23** : Spells per Day - Epic Oracles and Sorcerers (spontaneous 9-level, niv 21-40)
- **Table 3-24** : Spells per Day - Epic Paladins and Rangers (4-level, niv 21-40)

**Regles de progression** :
- Prepared casters (9 niveaux) : nouveau niveau de sort quand character level = 2x spell level + 1. Gain 1 slot, puis +1 tous les 1-3 niveaux.
- 6-level casters : nouveau niveau de sort quand character level = 3x spell level. Gain 1 slot, puis idem.
- Spontaneous 9-level : nouveau niveau de sort quand character level = 2x spell level + 2.
- 4-level casters : nouveau niveau de sort quand character level = 6 + 3x spell level.
- **Table 3-25** : Ability Modifiers and Epic Bonus Spells (scores 30-61+)

### 3.22 Compagnons epiques

**Table 3-26** : Animal Companions and Mounts at Epic Levels (niv 21-30+)
**Table 3-27** : Eidolons at Epic Levels (niv 21-30+)
**Table 3-28** : Familiars at Epic Levels (niv 21-30+)

---

## 4. Phase 3 : Competences epiques

**Source** : Chapitre 4 (pages 31-35)

Utilisations extraordinaires des competences au-dela des DCs normaux. Ce sont des descriptions narratives et des DCs de reference, pas des mecaniques de bonus dans PCGen. A implementer comme reference/description dans les competences.

### 4.1 Competences avec utilisations epiques

| Competence | Exemples DC |
|---|---|
| **Acrobatics** | DC 35 : Free Stand, DC 40 : Balance 1 inch, DC 50 : Climb vertical, DC 90 : Balance on liquid, DC 120 : Balance on cloud |
| **Appraise** | DC 50 : Detect magic on item |
| **Bluff** | Instill suggestion, Display false alignment (DC 70), Disguise surface thoughts (DC 100) |
| **Climb** | DC 70 : Perfectly smooth flat, DC 100 : Overhang/ceiling |
| **Craft** | Accelerer la fabrication (+DC multiple de 10) |
| **Diplomacy** | Shift attitude +2 steps, Fanatic attitude (+5/+10/+20/+40/+50 DC) |
| **Disable Device** | DC +20 a +100 : Accelerer le deverrouillage |
| **Escape Artist** | DC 80 : Extremely tight space, DC 120 : Pass through wall of force |
| **Handle Animal** | DC 30-120 : Rear/train non-animals, accelerer l'entrainement |
| **Heal** | DC 50-125 : Cure light/moderate/serious/critical wounds, Quicken/Perfect recovery |
| **Linguistics** | DC 50+spell level x5 : Decipher without read magic. DC +50 : Forge without sample |
| **Perception** | DC 60 : Sense magic, DC 70+spell DC : Defeat illusion |
| **Perform** | Sway Audience (Diplomacy +DC 20) |
| **Ride** | DC 40 : Stand on mount, DC 50 : Unconscious control, DC 60 : Attack from cover |
| **Sense Motive** | DC 60 : Partial alignment, DC 80 : Full alignment, DC 100 : Detect surface thoughts |
| **Sleight of Hand** | DC 50-80 : Lift objects from person |
| **Spellcraft** | DC +60 : Identify magic item without detect magic |
| **Stealth** | Hide Another (penalite -30) |
| **Survival** | DC 40-60 : Ignore terrain/weather, find direction, identify tracked races |
| **Swim** | DC 80 : Swim up waterfall. +DC 20 : Speed swimming |

**Competences sans utilisation epique** : Disguise, Fly, Intimidate, Knowledge, Profession, Use Magic Device.

---

## 5. Phase 4 : Dons epiques

**Source** : Chapitre 5 (pages 36-105)

### 5.1 Regles generales des dons epiques

- Ne peuvent JAMAIS etre acquis avant le niveau 21
- Deux methodes d'acquisition :
  1. Dons de personnage bonus (niveaux impairs 21+)
  2. Dons de classe bonus epiques (selon la classe)
- Les prerequis sont des minimums (toute valeur superieure qualifie)
- Prerequis perdus = perte d'acces au don (retrouve quand prerequis regagnes)

### 5.2 Categories de dons epiques et quantites

| Categorie | Nombre de dons | Classes bonus associees |
|---|---|---|
| **Alchemical** | ~17 dons | Alchemist |
| **Channeling** | ~22 dons | Cleric, Paladin |
| **Combat** | ~120+ dons | Fighter, Barbarian, Cavalier, Gunslinger, Inquisitor, Magus, Monk, Paladin, Ranger, Rogue |
| **General** | ~60+ dons | Toutes classes (pas en bonus class feat) |
| **Item Creation** | ~15 dons | Alchemist, Cleric, Magus, Oracle, Sorcerer, Summoner, Witch, Wizard |
| **Ki** | ~20 dons | Monk |
| **Magic** | ~50+ dons | Bard, Cleric, Druid, Inquisitor, Magus, Oracle, Sorcerer, Summoner, Witch, Wizard |
| **Metamagic** | ~40+ dons | Alchemist, Cleric, Druid, Magus, Oracle, Sorcerer, Summoner, Witch, Wizard |
| **Morale** | ~25 dons | Bard, Cavalier, Paladin |
| **Rage** | ~20 dons | Barbarian |
| **Skill** | ~25 dons | Bard, Inquisitor, Ranger, Rogue |
| **Summoning** | ~15 dons | Summoner |
| **Wild** | ~10+ dons | Druid |

### 5.3 Dons epiques - Implementation detaillee

Chaque don doit etre implemente comme un `ABILITY` dans PCGen avec :
- `TYPE:Epic.{Categorie}` (ex: `TYPE:Epic.Combat`)
- `PRECLASS:1,ANYPC=21` (prerequis niveau 21 minimum)
- Les prerequis specifiques via `PRESTAT:`, `PREABILITY:`, `PRESKILL:`, `PREBASESIZEEQ:`, `PREATTACKBASE:`, etc.
- Les effets mecaniques via `BONUS:` tokens
- `MULT:YES` et `STACK:YES` pour les dons selectionnables plusieurs fois
- `DESC:` pour la description du benefice

**Exemples de dons a fort impact mecanique :**

#### 5.3.1 Alchemical Feats (17 dons)
- Artillery, Augmented Alchemy, Bomb Mastery, Compound Bomb, Deadly Bomb, Epic Extracts, Epic Mutagen, Extract Bomb, Meta-Extract, Meta-Extract Bomb, Flawless Mutagen, Improved Bomb, Mutagen Bomb, Persistent Bomb, Poisoner, Prismatic Bomb, Second Breakthrough

#### 5.3.2 Channeling Feats (22 dons)
- Absolute Channel, Bolster the Faithful, Divine Interposition, Empower Channel, Enlarge Channel, Flesh and Bones, Heaven's Song, Indomitable Channel, Instant Channel, Light of Blessing/Faith/Fervor, Negative/Positive Energy Aura, Overchannel, Perfect Channeler, Presence of Fear, Shadow of Night, Spectral Strike, Widen Channel, Winds of Agony, Wings of Fire/Light, Word of Panacea, Colloquy of Nostrum

#### 5.3.3 Combat Feats (~120 dons)
Liste complete dans Table 5-3 (pages 45-46). Inclut des arbres de dons profonds :
- **Weapon Focus** : Epic Weapon Focus > Epic Weapon Specialization > Weapon Supremacy > Warlord > Battlegod > Thousand Arms > Tide of Iron / Comet Shot
- **Vital Strike** : Perfect Vital Strike > Vicious Vital > Power Overwhelming
- **Cleave** : Supreme Cleave
- **Trip** : Anklebreaker > Last One Standing
- **Earth Breaker** : Shaking the Foundations > World Breaker > God Breaker

#### 5.3.4 General Feats (~60 dons)
- Great Strength/Dexterity/Constitution/Intelligence/Wisdom/Charisma (stackable, +1 chaque)
- Epic Fortitude/Reflexes/Will (+4 save, stackable avec Enduring/Agile/Disciplined)
- Armor Skin (+2 natural armor, stackable)
- Damage Reduction (DR 3/-, stackable)
- Energy Resistance (+10, stackable)
- Epic Toughness (+1 HP/level, stackable)
- Epic Speed (+30 ft), Lightning Speed (x6), Stormstride (x10), Limitless Speed (+10 ft stackable)
- Ascent > Greater Ascent > Absolute Ascent (transcendance mortelle)
- Blinding Speed > Void Step > Void Dodge > Walk on Water
- Inviolate > Improved Inviolate > Inured > Improved Inured (immunites)

#### 5.3.5 Item Creation Feats (15 dons)
- Cannibal Crafting, Craft Epic Magic Arms and Armor, Craft Epic Rod/Staff/Wondrous Item, Efficient Item Creation, Epic Cooperation, Forge Epic Ring, Master Brewer, Prudent Crafter, Scribe Epic Scroll, Skillful Augmentation, Wand Savant

#### 5.3.6 Ki Feats (20 dons)
- Absolute Ki Strike, Channel Positive Ki, Font of Ki, Hardened Aura, Improved/Keen/Shattering/Transfixing Strike, Ki Armor/Blast/Bomb/Wave, Lion's Roar, Nirvana, Poison Ki, Zen Focus

#### 5.3.7 Magic Feats (~50 dons)
- Arcane/Divine Savant, Bonus Domain, Deep/Vast/Fathomless Pool, Dual Spell, Epic Spell Focus/Penetration, Expanded Spell List (per class), Familiar Spell, Improved Combat/Alignment-Based Casting, Improved Metamagic/Spell Capacity, Master Staff/Wand, Ring/Scroll/Wand/Wondrous Item Lore/Mastery, Magic Item Focus/Specialization/Mastery/Internalize Power, Spell Knowledge/Opportunity/Stowaway/Superiority/Supremacy, Spontaneous Domain Access/Metamagic, Tenacious Magic, Vicious Hex, Warmage/Warcaster/Vatic Bastion, Wide Pool

#### 5.3.8 Metamagic Feats (~40 dons)
- Absolute/Perfect Spell, Anathema/Aura/Armoring/Banishing/Blackened/Bolstering/Carnage/Cataclysmic/Collateral/Cutting/Deadly/Delay/Distort/Enduring/Enhance/Force/Gilded/Golembane/Grand/Guardian/Hasten/Improved Heighten/Imprisoning/Ineffable/Linked/Living/Macabre/Maelstrom/Pestilent/Prismatic/Project/Shredding/Split/Stealth/Summoned/True Death/Unseen/Vast/Verdigris/Warp Spell

#### 5.3.9 Morale Feats (~25 dons)
- Aura of Conviction/Glory/Gold/Misery/Protection/Shadow/Truth/Winds (stackable, chacun unique)
- Banner of Crowns/Golden Sun/Red Path/White Blossom
- Boundless Bond > Depthless Bond, Deafening Song > Hindering Song
- Dual Aura, Epic/Group Inspiration, Improved Aura of Courage, Inspire Excellence, Lasting/Rapid/Ranged Inspiration, Music of the Gods, Reactive Countersong, Widen Aura

#### 5.3.10 Rage Feats (~20 dons)
- Bear's/Bull's/Cat's Rage (stackable, +2 stat en rage)
- Enduring Vitality, Gentle Fury, Guillotine Grapple, Immaculate/Perfect Rage
- Incite Rage, Mad Movement, Mighty Surge, Monstrous Rage, Mountain Hurler
- Primal Rage > Arctic/Chaotic/Obdurate/Incandescent/Thundering/Towering/Titan Rage
- Cursebreaker/Ruinous/Shattering/Terrifying Rage

#### 5.3.11 Skill Feats (~25 dons)
- Epic Reputation, Epic Skill Focus > Skill Mastery > Skill Perfection
- Epic Trapfinding, Expert Aid, Legendary Athlete/Climber/Craftsman/Leaper/Merchant/Rider/Scholar/Tracker
- Master Mage/Thief, Outdoor Master, Second Skin, Self-Concealment, Thousand Faces
- Weightless Step > Meteor Landing

#### 5.3.12 Summoning Feats (~15 dons)
- Assemble Summons, Enhanced Summoning, Epic Eidolon > Empower Eidolon
- Prolonged Summons > Summon Companion
- Summoning Perfection > Chivalric/Colossal/Draconic/Ebon/Devilish/Guardian/Divine/Sylvan Summoning

#### 5.3.13 Wild Feats (~10 dons)
A extraire du chapitre 5 (pages non encore lues en detail, section "Wild Feats" p.103-105).

---

## 6. Phase 5 : Objets magiques epiques

**Source** : Chapitre 6 (pages 106-167)

### 6.1 Creation d'objets magiques epiques

- Necessite les dons Craft Epic correspondants
- Prix de base : formules specifiques detaillees dans le chapitre
- Les proprietes epiques ont des niveaux de lanceur et de prix differents

### 6.2 Armures et boucliers epiques

- **Prix de base** : Bonus +6 = 360,000 gp, +7 = 490,000 gp, etc. (formule : bonus^2 x 10,000)
- **Proprietes speciales epiques d'armure** : Descriptions pages 107-110
- **Armures epiques specifiques** : Pages 111-112

### 6.3 Armes epiques

- **Prix de base** : Bonus +6 = 720,000 gp, +7 = 980,000 gp, etc. (formule : bonus^2 x 20,000)
- **Proprietes speciales epiques d'arme** : Descriptions pages 113-122
- **Armes epiques specifiques** : Pages 123-125

### 6.4 Anneaux epiques

- Descriptions pages 126-127

### 6.5 Sceptres (Rods) epiques

- Descriptions pages 128-132

### 6.6 Parchemins (Scrolls) epiques

- Page 133

### 6.7 Batons (Staves) epiques

- Descriptions pages 134-135

### 6.8 Objets merveilleux (Wondrous Items) epiques

- Descriptions pages 136-167 (section la plus volumineuse)

**Implementation** : Chaque objet comme entree dans le fichier LST d'equipement correspondant, avec les proprietes `COST:`, `BONUS:`, `SPROP:`, `PRETYPE:`, etc.

---

## 7. Phase 6 : Magie epique

**Source** : Chapitre 7 (pages 168-178)

### 7.1 Dons metamagiques epiques

Deja couverts dans la Phase 4 (section Metamagic Feats). Chaque metamagic feat epique augmente le niveau de sort effectif d'un nombre specifique de niveaux.

### 7.2 True Dweomers

- Sorts de niveau 10+ crees par les joueurs
- Systeme de creation de sorts base sur des "seeds"
- Regles pages 170-178

### 7.3 Creation de sorts epiques

- Facteurs de cout (DC du Spellcraft check)
- Seeds de sort : descriptions detaillees
- Exemples de sorts epiques

**Implementation** : Principalement descriptif. Les sorts epiques specifiques peuvent etre implementes comme des `SPELL` entries avec des niveaux de sort > 9.

---

## 8. Phase 7 : Regles maison

**Source** : Chapitre 8 (pages 179-181)

### 8.1 Naming Magic Items

Regles pour nommer les objets magiques (descriptif).

### 8.2 Death and Dying

Regles alternatives pour la mort et l'agonie (optionnel).

### 8.3 Spell Power and Magic Item Crafting

Regles optionnelles pour la puissance des sorts et la fabrication d'objets (page 180).

### 8.4 Metamagic and Magic DCs

Ajustements optionnels des DCs de metamagie (page 181).

**Implementation** : Ces regles sont optionnelles. A implementer comme un sous-module activable separement dans le PCC.

---

## 9. Structure des fichiers

### 9.1 Fichier PCC principal

```
CAMPAIGN:Epic Pathfinder v1.6
GAMEMODE:Pathfinder
TYPE:Homebrew
RANK:9
BOOKTYPE:Supplement
SETTING:Generic
STATUS:ALPHA
SOURCELONG:Epic Pathfinder Handbook v1.6
SOURCESHORT:EPH
SOURCEWEB:http://www.jessejackjones.com
SOURCEDATE:2015-07
ISOITHERTHANANPC:YES

# Prerequisites
!PRECAMPAIGN:1,INCLUDES=Core Rulebook
!PRECAMPAIGN:1,INCLUDES=Advanced Player's Guide

# Data files
ABILITY:epic_class_abilities.lst
ABILITY:epic_class_abilities_alchemist.lst
ABILITY:epic_class_abilities_barbarian.lst
# ... (1 fichier par classe)
ABILITY:epic_feats.lst
ABILITY:epic_feats_alchemical.lst
ABILITY:epic_feats_channeling.lst
ABILITY:epic_feats_combat.lst
ABILITY:epic_feats_general.lst
ABILITY:epic_feats_item_creation.lst
ABILITY:epic_feats_ki.lst
ABILITY:epic_feats_magic.lst
ABILITY:epic_feats_metamagic.lst
ABILITY:epic_feats_morale.lst
ABILITY:epic_feats_rage.lst
ABILITY:epic_feats_skill.lst
ABILITY:epic_feats_summoning.lst
ABILITY:epic_feats_wild.lst
ABILITYCATEGORY:epic_abilitycategories.lst
CLASS:epic_classes.lst
COMPANIONMOD:epic_companionmods.lst
EQUIPMENT:epic_equipment_armor.lst
EQUIPMENT:epic_equipment_weapons.lst
EQUIPMENT:epic_equipment_rings.lst
EQUIPMENT:epic_equipment_rods.lst
EQUIPMENT:epic_equipment_staves.lst
EQUIPMENT:epic_equipment_wondrous.lst
SKILL:epic_skills.lst|(INCLUDE:...)
```

### 9.2 Conventions de nommage

- Noms de variables : `EpicAttackBonus`, `EpicSaveBonus`, `Epic{Class}LVL`
- Categories d'abilities : `Epic Class Feature`, `Epic Feat`, `Epic {Class} Choice`
- Types de dons : `Epic.Alchemical`, `Epic.Channeling`, `Epic.Combat`, etc.

---

## 10. Criteres d'acceptation

### 10.1 Phase 1 (Infrastructure)

- [ ] Un personnage peut monter au-dela du niveau 20
- [ ] Le bonus epique d'attaque se calcule correctement (+1 au niv 22, +1/4 niveaux)
- [ ] Le bonus epique de sauvegarde se calcule correctement (+1 au niv 21, +1/3 niveaux)
- [ ] Les augmentations de caracteristiques continuent (niv 24, 28, 32...)
- [ ] Les rangs de competence max = niveau du personnage
- [ ] Les dons bonus de personnage continuent aux niveaux impairs

### 10.2 Phase 2 (Classes epiques)

- [ ] Les 19 classes ont une progression epique fonctionnelle (niveaux 21-30)
- [ ] Les bonus de prouesse s'appliquent correctement selon la classe
- [ ] Les bonus aux degats s'appliquent correctement selon la classe
- [ ] Les dons de classe bonus sont disponibles aux bons niveaux avec les bonnes categories
- [ ] Les capacites de classe specifiques fonctionnent (epic alchemy, epic rage, epic performance, etc.)
- [ ] Les tables de sorts/jour sont correctes pour chaque type de lanceur
- [ ] Les compagnons/eidolons/familiers progressent correctement

### 10.3 Phase 3 (Competences epiques)

- [ ] Les descriptions d'utilisations epiques sont presentes dans les competences concernees

### 10.4 Phase 4 (Dons epiques)

- [ ] Tous les dons epiques (~300+) sont implementes avec leurs prerequis corrects
- [ ] Les dons stackables sont marques `MULT:YES|STACK:YES`
- [ ] Les dons avec choix (arme, competence, etc.) utilisent `CHOOSE:` correctement
- [ ] Les effets mecaniques (BONUS:) sont implementes quand applicable
- [ ] Les arbres de dons (prerequis en chaine) fonctionnent correctement

### 10.5 Phase 5 (Objets magiques)

- [ ] Les armures/armes epiques avec bonus > +5 sont disponibles
- [ ] Les proprietes speciales epiques sont implementees
- [ ] Les objets epiques specifiques (anneaux, sceptres, batons, etc.) sont presents
- [ ] Les prix sont corrects selon les formules du chapitre

### 10.6 Phase 6 (Magie epique)

- [ ] Les dons metamagiques epiques fonctionnent avec le systeme de sorts
- [ ] Les sorts epiques exemples sont implementes

### 10.7 Criteres generaux

- [ ] Le module se charge sans erreur dans PCGen
- [ ] Aucun conflit avec les donnees du Core Rulebook ou de l'APG
- [ ] Les personnages multi-classes gagnent les bonus epiques basees sur le niveau total (pas le niveau de classe)
- [ ] La feuille de personnage exporte correctement les informations epiques
