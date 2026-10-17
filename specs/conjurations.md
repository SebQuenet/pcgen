# Spécification : onglet Conjurations

**Cible** : le joueur d'un conjurateur, en partie, face à PCGen.
**Périmètre** : un onglet qui montre les créatures que le personnage peut conjurer et suit celles
qu'il a conjurées pendant un combat ; le même texte s'écrit à la main ou par un agent IA.

---

## 1. Objectif

Le joueur d'un conjurateur tient aujourd'hui ses créatures dans un tableur : une ligne par
créature, groupées par niveau de convocation, avec les nombres qui servent en combat. En
partie, il lui faut deux choses à la fois :

- **le catalogue** : quelles créatures il peut appeler, et ce qu'elles valent ;
- **les créatures en jeu** : celles qu'il vient de conjurer, chacune suivie à part. Trois
  archontes molosses conjurés ensemble ont trois réserves de points de vie distinctes.

L'onglet reprend le principe de l'onglet Tactique : le texte source à gauche, la page rendue à
droite, le tout enregistré dans le `.pcg`, écrit par un agent à travers le serveur MCP ou HTTP,
et exportable comme fiche de sortie.

## 2. Décisions

1. **Une syntaxe dédiée, pas une section de la fiche tactique.** Le bloc `creature:` de la fiche
   tactique porte des lignes libres (`row: étiquette | contenu`), qui ne permettent ni un tableau
   où l'on compare les créatures, ni un filtre par rôle. Une conjuration a des champs fixes : ceux
   du tableur.
2. **Le texte source fait foi**, comme pour la fiche tactique. Le modèle objet est le résultat
   d'une lecture et n'est jamais réécrit en texte.
3. **La partie droite est coupée en deux, haut et bas** : le catalogue en haut, les créatures en
   jeu en bas, séparés par une barre que l'on déplace. C'est une seule page HTML, donc un seul
   gabarit, un seul pont vers Java et une seule exportation. Conjurer une créature ou la blesser
   ne redessine pas la page : le script met la page à jour lui-même et enregistre par le pont,
   si bien que le défilement et le filtre du catalogue restent en place.
4. **L'état du combat est enregistré dans le `.pcg`**, parce qu'un combat survit à une pause, et
   chaque clic marque le personnage modifié. Aucune opération ne l'écrit : c'est le joueur qui
   clique, comme pour la fiche tactique. `get_conjuration_sheet` le renvoie en lecture.
5. **Les valeurs sont écrites, jamais calculées.** Une conjuration ne pointe pas vers une race du
   bestiaire de PCGen ; les nombres sont ceux que l'auteur a écrits.

## 3. Syntaxe

Orientée ligne, comme la fiche tactique : `## ` ouvre un groupe, une clé en colonne zéro ouvre un
bloc, une sous-clé indentée de deux espaces complète le bloc au-dessus.

```
## Convocation de monstres IV

summon: Hound Archon
  role: Tank, Buff, Debuff
  speed: 8
  hp: 51
  ac: 19 / 10 / 19
  attack: +11/+6 épée\n+5 morsure
  damage: 2x2D6+6 / 1D8+4
  special: beast shape II
  cmb: 10
  cmd: 20
  ref: 5 / +2 mal
  fort: 8 / +2 mal / +4 poison
  will: 5 / +2 mal
  init: 4
  spells: greater teleport / aid +6
  note: SR 15\nimmunité électricité et pétrification
```

| Ligne | Forme |
|---|---|
| groupe | `## <titre>`, en général un niveau de convocation |
| créature | `summon: <nom>`, puis ses champs en sous-lignes |
| champ | `  <clé>: <valeur>` |

Les clés de champ forment un ensemble fermé, dans l'ordre du tableur : `role`, `speed`, `hp`, `ac`,
`attack`, `damage`, `special`, `cmb`, `cmd`, `ref`, `fort`, `will`, `init`, `spells`, `note`.

- Tous les champs sont facultatifs ; le nom ne l'est pas.
- `role:` liste des étiquettes séparées par des virgules. Elles deviennent le filtre du catalogue,
  sans tenir compte de la casse.
- Dans une valeur, les deux caractères `\n` valent un saut de ligne, comme dans la fiche tactique.
- `hp:` est du texte libre ; le suivi des points de vie prend son premier entier comme maximum.
  Sans entier, la carte compte les dégâts sans barre de vie.
- Les lignes vides sont ignorées.

Une lecture qui échoue renvoie toutes ses erreurs, chacune avec son numéro de ligne (base 1),
sous forme de valeurs typées et non d'exceptions. Cas d'échec :

- un texte qui ne s'ouvre pas sur un groupe ;
- une clé inconnue en colonne zéro, ou une sous-clé inconnue ;
- une ligne indentée sans créature au-dessus ;
- un champ écrit deux fois pour la même créature ;
- un nom vide, ou un nom déjà pris (sans tenir compte de la casse), puisque les créatures en jeu
  désignent leur modèle par son nom ;
- un groupe sans créature ;
- un texte sans aucun groupe.

## 4. Modèle

Paquet `pcgen.core.conjurations`, des records immuables qui se valident à la construction.

```java
record SummonCatalog(List<SummonGroup> groups)
record SummonGroup(String title, List<SummonTemplate> summons)
record SummonTemplate(String name, List<String> roles, Map<SummonField, String> fields)
enum SummonField { SPEED, HIT_POINTS, ARMOR_CLASS, ATTACK, DAMAGE, SPECIAL, CMB, CMD,
                   REFLEX, FORTITUDE, WILL, INITIATIVE, SPELLS, NOTE }

sealed interface SummonParseResult permits SummonParseSuccess, SummonParseFailure
record SummonParseSuccess(SummonCatalog catalog)
record SummonParseFailure(List<SummonParseError> errors)
record SummonParseError(int line, String message)
```

L'état du combat est une structure à part, parce qu'un catalogue n'est pas un compteur :

```java
record ConjuredCreature(String creature, int number, int damage, OptionalInt roundsLeft)
record ConjurationSession(int duration, List<ConjuredCreature> conjured)
```

- Une créature en jeu se désigne par le nom de son modèle et son numéro : « Hound Archon 2 ».
  Conjurer donne le plus petit numéro libre pour ce nom, de sorte qu'après un combat vidé les
  numéros repartent de 1.
- `duration` est le nombre de rounds donné à une créature au moment où elle arrive ; 0 veut dire
  que les rounds ne sont pas comptés, et `roundsLeft` est alors vide.
- « Round suivant » retire un round à chaque créature comptée, sans descendre sous zéro. Une
  créature à zéro reste affichée, marquée partie, jusqu'à ce que le joueur la renvoie.
- Les dégâts ne sont jamais négatifs ; ils peuvent dépasser le maximum, la carte affiche alors des
  points de vie négatifs.
- Une créature en jeu dont le nom n'est plus dans le catalogue garde son état et s'affiche avec un
  avertissement.

## 5. Stockage

| Ligne `.pcg` | Contenu |
|---|---|
| `CONJURATIONS` | tout le texte source, encodé par `EntityEncoder` |
| `CONJURATIONDURATION` | la durée donnée aux nouvelles créatures, quand elle n'est pas 0 |
| `CONJURED` | `<nom encodé>\|<numéro>\|<dégâts>\|<rounds restants, vide si non comptés>`, une ligne par créature en jeu, dans l'ordre d'arrivée |

Deux facettes, chacune avec un seul rôle, toutes deux déclarées dans `applicationContext.xml` :
une facette absente de ce fichier ne survit pas à `cloneForExport`, et la page ne la verrait pas.

- `ConjurationCatalogFacet` garde le texte source ;
- `ConjurationSessionFacet` garde la `ConjurationSession`.

Accès en lecture par `CharacterDisplay`, en écriture par `PlayerCharacter`, comme la fiche
tactique. La lecture d'un texte qui ne se lit plus le garde tel quel et ajoute un avertissement
par erreur ; une ligne `CONJURED` illisible est écartée avec un avertissement.

## 6. La page

Le gabarit `outputsheets/conjurations/conjurations.htm.ftl` reçoit le modèle `conjurations`,
construit par `pcgen.io.conjurations.ConjurationOutputModel` et ajouté par
`FreeMarkerExportHandler` à côté de `tactics`. Il ouvre sur `<#ftl output_format="HTML">` : le texte
vient d'un agent IA et n'est pas fiable, et la page tient un pont ouvert vers Java. Le script
n'écrit que par `textContent` et par attributs, jamais par `innerHTML`.

**En haut, le catalogue.** Une barre de filtres par rôle, puis un tableau par groupe : un bouton
« Conjurer », le nom, vitesse, PV, CA, attaque, dégâts, CMB, CMD, Réf, Vig, Vol, Init. Sous chaque
créature, une ligne de détail porte ses rôles, capacités, sorts et notes, quand elle en a.

**En bas, les créatures en jeu.** Un en-tête avec « Round suivant », « Tout renvoyer » et la
durée donnée aux nouvelles créatures. Puis une carte par créature : nom et numéro, rounds
restants avec − et +, points de vie actuels sur maximum avec une barre, un montant et les boutons
« Blesser » et « Soigner », une ligne de stats reprise du catalogue, et « Renvoyer ».

Une nouvelle carte se clone depuis un `<template>` rendu pour chaque créature du catalogue, avec
la même macro que les cartes rendues côté serveur.

Le pont planté sur `window.pcgen` offre :

| Méthode | Effet |
|---|---|
| `conjure(nom)` | ajoute une créature en jeu, renvoie son numéro |
| `setDamage(nom, numéro, dégâts)` | dégâts totaux de cette créature |
| `setRoundsLeft(nom, numéro, rounds)` | rounds restants de cette créature |
| `nextRound()` | un round de moins pour chaque créature comptée |
| `dismiss(nom, numéro)` / `dismissAll()` | renvoie une créature, ou toutes |
| `setDuration(rounds)` | durée donnée aux prochaines créatures |

La même page ouverte dans un navigateur ne trouve pas de pont et garde son état pour elle.

## 7. L'onglet

`ConjurationsInfoTab`, placé après l'onglet Tactique, avec `Tab.CONJURATIONS` et ses libellés
dans `LanguageBundle.properties` (Summons) et `LanguageBundle_fr.properties` (Conjurations).
À gauche l'éditeur du texte source, relu après une courte pause de frappe, avec un bandeau qui
nomme la première ligne fautive. À droite la page.

L'onglet passe par `ConjurationSheetFacade` (`pcgen.facade.core`), implémentée par
`ConjurationSheetFacadeImpl` (`pcgen.gui2.facade`), de sorte que l'interface ne touche jamais
`PlayerCharacter`.

Les deux moitiés de l'onglet existaient déjà pour la fiche tactique ; elles servent désormais aux
deux onglets :

- le panneau qui affiche une page rendue dans un `WebView` et y plante un pont
  (`TacticalSheetPanel`) devient `RenderedSheetPanel`, paramétré par son gabarit et par la
  fabrique de son pont ;
- l'éditeur relu après une pause de frappe, avec son bandeau, sort de `TacticalInfoTab` et
  devient `SourceEditorPanel`.

## 8. Opérations

`ConjurationService`, et trois opérations servies par MCP et par HTTP :

| Opération | Effet |
|---|---|
| `set_conjuration_sheet` | écrit le texte source ; refuse un texte qui ne se lit pas, en nommant ses lignes |
| `get_conjuration_sheet` | renvoie le texte source, la durée et les créatures en jeu |
| `clear_conjuration_sheet` | retire le catalogue et l'état du combat |

L'aide de `set_conjuration_sheet` décrit la syntaxe, pour qu'un agent l'écrive sans deviner.

## 9. Tests

- **Lecture** : chaque champ, les groupes, les sauts de ligne, et chaque cas d'échec du §3 avec
  son numéro de ligne.
- **État du combat** : numérotation par plus petit numéro libre, dégâts, rounds, round suivant,
  renvoi, et ce que le constructeur refuse.
- **Aller-retour `.pcg`** : dans `BasicSaveRestoreTest`, y compris des caractères qui servent de
  séparateurs dans le fichier.
- **Rendu** : le gabarit passé par le vrai gestionnaire d'export sur un vrai personnage ; un
  `<script>` écrit dans le texte ressort échappé ; la copie faite par `cloneForExport` garde le
  catalogue et le combat, ce qui échoue si une facette manque dans `applicationContext.xml`.
- **Façade et service** : sur un vrai personnage, jamais une doublure.

Le `WebView` et le pont sont la frontière qu'on ne teste pas automatiquement ; ils restent aussi
minces que possible.

## 10. Hors périmètre

- Calculer les statistiques depuis les races du bestiaire, les gabarits céleste ou fiélon, ou un
  don comme Augmentation des convocations.
- Le nombre de créatures qu'une convocation amène, et les utilisations par jour.
- Rendre la page en PDF.

## 11. Ordre de construction

1. Modèle et lecture du texte.
2. État du combat.
3. Facettes, accès par le personnage, lignes `.pcg`.
4. Modèle de sortie et gabarit.
5. Service et opérations.
6. Panneau partagé, onglet, façade et pont.
7. Shalendra : le tableur transcrit dans son personnage, enregistré dans
   `characters/Shalendra-12.pcg`.
