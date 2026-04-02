# Release Notes - Fork PCGen (branche update-feats)

Changements depuis le fork initial (`e093948bc7`).

---

## Nouvelles fonctionnalités

### Token ADDSPELLTOCLASSLIST
Nouveau token LST (`AddSpellToClassListLst.java`) permettant d'ajouter des sorts choisis via `CHOOSE:SPELLS` à la liste de sorts d'une classe cible du personnage. Syntaxe : `ADDSPELLTOCLASSLIST:ClasseSource|ClasseCibleOuTypeSort`. Ce token est utilisé par Mystic Past Life et Dreamed Secrets.

### Mystic Past Life (Advanced Race Guide)
Implémentation complète du trait racial Samsaran "Mystic Past Life" : les sorts choisis sont désormais ajoutés à la liste de sorts connue du personnage via `ADDSPELLTOCLASSLIST`, pour toutes les classes de lanceur de sorts (Bard, Sorcerer, Oracle, Wizard, Cleric, Druid, etc.).

### Dreamed Secrets (Inner Sea Gods)
Implémentation du don "Dreamed Secrets" : ajout d'un pool de 2 choix de sorts depuis la liste du Wizard, ajoutés à la liste divine du personnage via `ADDSPELLTOCLASSLIST:Wizard|Divine`.

### Wayang Spellhunter (Dragon Empires Primer)
Refonte du trait "Wayang Spellhunter" avec un vrai `CHOOSE:SPELLS` pour sélectionner le sort bénéficiant de la réduction de niveau de métamagie, et ajout du don automatique de métamagie associé.

---

## Corrections de bugs

### Sélection de langues (LanguageChooserDialog)
Correction d'un problème de threading Swing/JavaFX : les actions du dialogue de sélection de langues (ajout, suppression, validation, annulation) sont désormais exécutées sur le thread Swing via `SwingUtilities.invokeLater()`.

### JFXPanelFromResource - Dialogue modal
Réécriture de `showAndBlock()` : le `FXMLLoader` est recréé correctement pour les dialogues modaux, avec gestion d'erreurs et un cache du contrôleur via `volatile`. Corrige un crash au démarrage.

### Rafraîchissement des sorts après ajout/retrait de capacité
Ajout de `spellSupportFacade.refreshAvailableKnownSpells()` dans `CharacterFacadeImpl` lors de l'ajout et du retrait d'une capacité, pour que les listes de sorts se mettent à jour dynamiquement.

### Divine Metamastery (Mythic Adventures)
Correction : le pouvoir mythique "Divine Metamastery" est désormais sélectionnable plusieurs fois (`STACK:YES MULT:YES CHOOSE:NOCHOICE`), conformément aux règles.

### Stormlord / Lightning Subdomain (Advanced Players Guide)
Ajout de la variable manquante `DruidLightningSubdomainAllowed` pour le sous-domaine Lightning du Druid.

### Objets magiques - Manuels et Tomes (Ultimate Equipment)
Correction des Manuels (Bodily Health, Gainful Exercise, Quickness of Action) et Tomes (Clear Thought, Leadership and Influence, Understanding) : ajout des bonus de statistiques inhérents (`BONUS:STAT|XXX|N|TYPE=Inherent`) qui étaient absents.

---

## Bonus temporaires (TEMPBONUS)

### Blessing of Fervor (Advanced Players Guide)
Ajout de 3 variantes TEMPBONUS pour le sort "Blessing of Fervor" (les choix du joueur chaque round) :
- **Speed** : +30 ft vitesse (enhancement)
- **Extra Attack** : +1 attaque supplémentaire au BBA max
- **Attack/AC/Ref** : +2 attaque, +2 esquive CA, +2 esquive Réflexes

### Protection from [Alignment] (Core Rulebook)
Ajout de TEMPBONUS pour les 4 sorts Protection from Evil/Good/Chaos/Law : +2 déflexion CA, +2 résistance saves.

### Magic Circle against [Alignment] (Core Rulebook)
Ajout de TEMPBONUS pour les 4 sorts Magic Circle against Evil/Good/Chaos/Law : +2 déflexion CA, +2 résistance saves.

### Slow (Core Rulebook)
Ajout de TEMPBONUS : -1 attaque, -1 CA, -1 Réflexes.

### Death Ward (Core Rulebook)
Ajout de TEMPBONUS : +4 moral à tous les jets de sauvegarde (vs mort et effets de mort).

### Invisibility / Greater Invisibility (Core Rulebook)
Ajout de TEMPBONUS : +2 attaque, +20 Discrétion.

### Conditions et actions de combat (Core Rulebook - templates)
Ajout de TEMPBONUS pour les conditions et actions manquantes :
- **Nauseated** : condition (ne peut attaquer/lancer de sorts)
- **Staggered** : condition (une seule action par round)
- **Charge** : +2 attaque, -2 CA
- **Flanking** : +2 attaque corps à corps
- **Lunge** : -2 CA
- **Cavalier Banner** : bonus moral saves/attaque pour les alliés (variable)
- **Smite Evil** : bonus attaque (CHA) et déflexion CA
- **Smite Good** : bonus attaque (CHA) et déflexion CA

### Elemental Body I-IV (Core Rulebook - Wild Shape / sorts)
Ajout de TEMPBONUS pour les 4 niveaux d'Elemental Body avec les 4 variantes élémentaires chacun (16 entrées) :
- **Elemental Body I** (Small) : Air (+2 DEX, +2 NA, fly 60), Earth (+2 STR, +4 NA), Fire (+2 DEX, +2 NA), Water (+2 CON, +4 NA, swim 60)
- **Elemental Body II** (Medium) : Air (+4 DEX, +3 NA), Earth (+4 STR, +5 NA), Fire (+4 DEX, +3 NA), Water (+4 CON, +5 NA)
- **Elemental Body III** (Large) : Air (+2 STR/+4 DEX, +4 NA), Earth (+6 STR/-2 DEX/+2 CON, +6 NA), Fire (+4 DEX/+2 CON, +4 NA), Water (+2 STR/-2 DEX/+6 CON, +6 NA)
- **Elemental Body IV** (Huge) : Air (+4 STR/+6 DEX, +4 NA, fly 120), Earth (+8 STR/-2 DEX/+4 CON, +6 NA), Fire (+6 DEX/+4 CON, +4 NA), Water (+4 STR/-2 DEX/+8 CON, +6 NA, swim 120)

### Sorts mythiques - TEMPBONUS (Mythic Adventures)
Ajout de TEMPBONUS pour les versions mythiques des sorts de buff suivants :
- **Mythic Barkskin** : armure naturelle (variable CL) + DR/magic
- **Mythic Haste** : +1 attaque/CA/Réflexes, +50ft vitesse, +1 attaque supp.
- **Mythic Heroism** : +4 moral (attaque, saves, skills)
- **Mythic Mage Armor** : +6 armure CA, 50% fortification
- **Mythic Divine Favor** : bonus luck étendu aux saves et skills
- **Mythic Prayer** : +2 luck (attaque, dégâts, saves, skills)
- **Mythic Shield of Faith** : déflexion + half tier
- **Mythic Bless** : +1 moral (attaque, dégâts, saves)
- **Mythic Fly** : 120ft vol, maneuvrabilité parfaite
- **Mythic Good Hope** : +3 moral (attaque, dégâts, saves, skills)
- **Mythic Stoneskin** : DR 10/adamantine, +4 Fort vs maladie/poison/stun
- **Mythic Enlarge Person** : +2 catégories taille, +4 STR/-4 DEX/-2 att&CA
- **Mythic Elemental Body IV** : 4 variantes (Air/Earth/Fire/Water) avec stats de base +2, armure naturelle +1, penalties réduits de 2, 50% fortification

### Corrections Elemental Body mythique (Mythic Adventures)
- Correction du bug de syntaxe `Elemental Body IIIMOD` → `Elemental Body III.MOD`
- Correction des PREABILITY cassées : `#Elemental Body (All)` → noms individuels corrects (Elemental Body I/II/III/IV)
- Correction typo `chanc eany` → `chance any` dans la DESC d'Elemental Body IV

---

## Build et infrastructure

### Gradle - Plugin Foojay
Ajout du plugin `org.gradle.toolchains.foojay-resolver-convention` (v0.10.0) dans `settings.gradle` pour la résolution automatique des toolchains JDK.

### Gradle - Fallback Adoptium API
Ajout d'un try/catch dans `build.gradle` pour la récupération de la version Java depuis l'API Adoptium : en cas d'échec (mode offline), un fallback `{major}.0.0` est utilisé.

### RUNME.md
Ajout d'un fichier d'instructions de build rapide (prérequis JDK 25, commandes Gradle).
