# Spécification : source de données Zirnakaynin

**Cible** : le MJ de la campagne Pierrebougie, qui veut dans PCGen les PNJ de Zirnakaynin que les
joueurs rencontrent, au fur et à mesure de la partie.
**Périmètre** : un dossier `data/homebrew/zirnakaynin/`, au même niveau que `celmae/`, qui contient
chaque PNJ rencontré sous deux formes : son personnage `.pcg` prêt à ouvrir, et un kit LST qui
permet de le créer dans PCGen comme n'importe quel PNJ publié.

---

## 1. Données d'entrée

Le scénario `~/Documents/projects/Pierrebougie/scenarios/2026-09-19-zirkayanin/module/npcs/` contient
une fiche Markdown et un `.pcg` par profil : 232 PNJ nommés dans `nommes/`, 48 profils génériques
dans `generiques/`. L'identifiant d'un profil est le nom de son fichier sans extension
(`ravael-torai`, `generic-drider-de-toraiyor`). Ces `.pcg` ont été construits par PCGen ; ils portent
la race, les niveaux de classe, les dons et capacités choisis, les rangs de compétence,
l'équipement, les sorts et le plan tactique.

## 2. Contenu du dossier

```
data/homebrew/zirnakaynin/
  _zirnakaynin.pcc            campagne « Zirnakaynin », un fichier KIT par PNJ
  pnj/<identifiant>.pcg       copie du .pcg du scénario
  kits/zir_kit_<identifiant>.lst
```

Un PNJ entre dans le dossier quand les joueurs le rencontrent. Le dossier `pnj/` fait foi : un PNJ
est dans la source si et seulement si son `.pcg` y est.

## 3. Script

`scripts/import_zirnakaynin_npcs.py [<identifiant>...]`

1. Copie le `.pcg` de chaque identifiant donné depuis le scénario vers `pnj/`, en écrasant une
   copie antérieure. Un identifiant inconnu arrête le script avant toute écriture ; `--all` copie tous les profils.
2. Régénère le kit de **chaque** `.pcg` de `pnj/`, puis réécrit `_zirnakaynin.pcc`. Lancé sans
   argument, il remet les kits à jour après une modification des `.pcg` dans le scénario.

## 4. Du `.pcg` au kit

Un kit par PNJ : `STARTPACK:<nom du personnage>`, `TYPE:NPC.Zirnakaynin`, `EQUIPBUY:0`, sur le modèle
des kits de PNJ des livres Paizo (`dung_kits_npc.lst`).

| `.pcg` | Kit |
|---|---|
| `CHARACTERNAME`, `GENDER`, `AGE` | `NAME`, `GENDER`, `AGE` |
| `RACE` | `RACE` |
| `ALIGN` | `ALIGN` |
| `DEITY` (sauf `None`) | `DEITY` |
| `STAT` | `STAT` (scores de base) |
| `CLASS` (une ligne par classe) | `CLASS` + `LEVEL`, sans les niveaux de classe monstrueuse que la race accorde (`MONSTERCLASS`) |
| `ABILITY:<catégorie>…KEY:<clé>…APPLIEDTO:<choix>` | `ABILITY:CATEGORY=<catégorie>\|<clé> (<choix>)` |
| capacité de sélection dont le choix est une capacité (`APPLIEDTO:CATEGORY=<X>&pipe;<Y>`, ex. lignage d'ensorceleur) | `ABILITY:CATEGORY=<X>\|<Y>` |
| `SKILL` : `RANKS` de chaque `CLASSBOUGHT` | `SKILL` + `RANK` + `CLASS`, une ligne par classe qui a payé les rangs |
| `EQUIPNAME` + `QUANTITY` + emplacement de l'`EQUIPSET` | `GEAR` + `QTY` + `LOCATION` (mains et `Shield` gardés, autres emplacements portés → `Equipped`) |
| `LANGUAGE` hors langues automatiques | `LANGBONUS` |
| `SPELLNAME` | `SPELLS` |
| gabarit choisi (`TEMPLATESAPPLIED` non accordé par la race) | `TEMPLATE` |

Un kit applique ses lignes dans l'ordre écrit et vérifie les prérequis de chaque classe, d'où l'ordre
suivant : identité et caractéristiques, classes de base, divinité et gabarit, capacités (chacune après
celle qui ouvre sa réserve par `BONUS:ABILITYPOOL`, directement ou par une capacité accordée
automatiquement : domaine de druide après Nature's Bond, découvertes après Extra Discovery,
choix mythiques après le rang mythique), rangs
payés par les classes de base, classes de prestige, dons que ces classes n'exigent pas, rangs payés
par les classes de prestige, langues, équipement, sorts.

Ce que le kit ne transporte pas, et que seul le `.pcg` garde : les points de vie tirés à chaque
niveau, les objets personnalisés au-delà de leur objet de base et de leurs `EQMOD`, le plan
tactique, la description, le portrait.

## 5. Sources requises

Le `.pcc` déclare une ligne `KIT:` par PNJ. Chaque ligne porte un `PRECAMPAIGN` qui liste les livres
définissant les objets que son kit référence (race, classes, capacités, compétences, objets, sorts,
gabarits). Le script trouve ces livres en indexant les clés des `.lst` de `data/pathfinder` et les
`.pcc` qui les chargent. Quand plusieurs livres définissent la même clé, il retient un livre que le
`.pcg` charge, celui de plus petit `RANK`. Un PNJ n'apparaît ainsi que lorsque ses livres sont
chargés.

Une clé qu'aucun livre ne définit est retirée du kit, et le script la signale avec le nom du PNJ :
le kit reste chargeable, et l'écart est visible.

## 6. Vérification

- Tests unitaires du passage `.pcg` → kit (`scripts/tests/`), sur des extraits de `.pcg` réels.
- Chargement : `DataLoadTest` sur la source avec les livres qu'elle requiert, sans erreur LST.
- Fidélité : par le serveur HTTP, créer un personnage, appliquer le kit (`add_kit`), et comparer au
  `.pcg` le niveau, le BBA, les jets de sauvegarde, la CA et les totaux de compétence. Les écarts
  sont rapportés, PNJ par PNJ.

## 7. Contenu initial

Les 280 profils du scénario, nommés et génériques. Les profils ajoutés plus tard au scénario entrent
par `scripts/import_zirnakaynin_npcs.py <identifiant>`.
