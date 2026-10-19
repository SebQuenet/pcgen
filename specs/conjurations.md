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

1. **Une syntaxe dédiée, pas une section de la fiche tactique.** Une conjuration désigne une race
   de PCGen et ses gabarits, ce que le bloc `creature:` de la fiche tactique ne sait pas dire.
2. **Le texte source fait foi**, comme pour la fiche tactique. Le modèle objet est le résultat
   d'une lecture et n'est jamais réécrit en texte.
3. **La partie droite est coupée en deux, haut et bas** : le catalogue en haut, les créatures en
   jeu en bas, séparés par une barre que l'on déplace. C'est une seule page HTML, donc un seul
   gabarit, un seul pont vers Java et une seule exportation. Conjurer une créature ou la blesser
   ne redessine pas la page : le script met la page à jour lui-même et enregistre par le pont,
   si bien que le défilement du catalogue reste en place.
4. **L'état du combat est enregistré dans le `.pcg`**, parce qu'un combat survit à une pause, et
   chaque clic marque le personnage modifié. Aucune opération ne l'écrit : c'est le joueur qui
   clique, comme pour la fiche tactique. `get_conjuration_sheet` le renvoie en lecture.
5. **Les valeurs sont calculées par PCGen** (conception : `docs/superpowers/specs/2026-10-17-conjuration-buffs-design.md`).
   Chaque créature est construite comme un personnage : sa race, le kit `<race> Default` du
   bestiaire, ses gabarits, puis `Augment Summoning` quand le conjurateur a le don. Les bonus
   temporaires (Prière, Rapidité…) lui sont appliqués par les règles de cumul de PCGen.
6. **Construire une créature prend plusieurs secondes** (environ 4 s : la vérification du kit sur
   une copie du personnage en prend la moitié). Le catalogue n'affiche donc que les noms ; une
   créature en jeu est construite une fois puis gardée en mémoire par la façade, et un bonus posé
   ou retiré la modifie sans la reconstruire. Ses valeurs n'arrivent sur la page que par le pont :
   une page exportée et ouverte dans un navigateur ne les montre pas.

## 3. Syntaxe

Orientée ligne, comme la fiche tactique : `## ` ouvre un groupe, `summon:` en colonne zéro nomme
la race d'une créature, une ligne `template:` indentée de deux espaces liste ses gabarits.
Les lignes `role:` et les métadonnées facultatives restent disponibles, notamment pour relire
les catalogues enregistrés avant le calcul des statistiques par PCGen.

```
## Convocation de monstres IV

summon: Archon (Hound)
summon: Bear (Grizzly)
  template: Celestial Creature
```

| Ligne | Forme |
|---|---|
| groupe | `## <titre>`, en général un niveau de convocation |
| créature | `summon: <clé de race>` |
| gabarits | `  template: <clé>, <clé>`, facultatif, une fois au plus |
| rôles | `  role: <rôle>, <rôle>`, facultatif, une fois au plus |
| métadonnées | `  speed:`, `hp:`, `ac:`, `attack:`, `damage:`, `special:`, `cmb:`, `cmd:`, `ref:`, `fort:`, `will:`, `init:`, `spells:` ou `note:`, chacune facultative, une fois au plus |

- Le nom d'une créature se déduit : la race, suivie de ses gabarits entre parenthèses
  (« Bear (Grizzly) (Celestial Creature) »). Deux créatures ne portent pas le même nom, sans tenir
  compte de la casse : c'est par lui que les créatures en jeu désignent leur modèle.
- Les lignes vides sont ignorées ; les deux caractères `\n` valent un saut de ligne.

Une lecture qui échoue renvoie toutes ses erreurs, chacune avec son numéro de ligne (base 1),
sous forme de valeurs typées. Cas d'échec : un texte qui ne s'ouvre pas sur un groupe, une clé
inconnue en colonne zéro, une sous-clé inconnue, une ligne indentée sans créature
au-dessus, `template:` écrit deux fois, une race vide, un nom déjà pris, un groupe sans créature,
un texte sans groupe.

`CatalogResolution.unresolvedIn` vérifie ensuite le catalogue contre les sources chargées : une
race ou un gabarit qu'elles n'ont pas est une erreur sur la ligne de sa créature.
Une entrée historique avec `hp:` est traitée comme une fiche rédigée à la main : elle conserve
son nom et ses statistiques dans la page et n'est pas résolue comme une race PCGen. Les entrées
sans `hp:` continuent d'utiliser leurs race et gabarits pour le calcul des statistiques.

## 4. Modèle

Paquet `pcgen.core.conjurations`, des records immuables qui se valident à la construction.

```java
record SummonCatalog(List<SummonGroup> groups)
record SummonGroup(String title, List<SummonTemplate> summons)
record SummonTemplate(String raceKey, List<String> templateKeys, int line)   // name() déduit

record BonusSourceKey(String kind, String category, String key)   // "SPELL=Prayer", "ABILITY=FEAT=Power Attack"
record AppliedBonus(BonusSourceKey source, OptionalInt chosenValue)

record ConjuredCreature(String creature, int number, int damage, OptionalInt roundsLeft, List<AppliedBonus> bonuses)
record ConjurationSession(int duration, List<ConjuredCreature> conjured, List<AppliedBonus> groupBonuses)
```

- Une créature en jeu se désigne par le nom de son modèle et son numéro. Conjurer donne le plus
  petit numéro libre pour ce nom.
- `groupBonuses` s'appliquent à toutes les créatures en jeu et à celles conjurées ensuite ; ils
  survivent à « Tout renvoyer ». Les `bonuses` d'une créature s'ajoutent aux siens ; quand les deux
  viennent de la même source, la valeur propre à la créature l'emporte.
- `SummonedCreatureBuilder` construit une créature (PV moyens des dés de vie, comme le bestiaire)
  et lit ses valeurs (`CreatureStats`) par les jetons des fiches de sortie. `ConjuredCreatures`
  garde les créatures construites d'un conjurateur.
- `pcgen.core.bonus.TemporaryBonusSources` liste les sources de bonus : `castableOnOthers()` pour
  le groupe (bonus `TEMPBONUS:ANYPC`), `availableTo(créature)` pour une créature, ses propres
  capacités comprises. `TemporaryBonusApplier` pose et retire un bonus sans boîte de dialogue.

## 5. Stockage

| Ligne `.pcg` | Contenu |
|---|---|
| `CONJURATIONS` | tout le texte source, encodé par `EntityEncoder` |
| `CONJURATIONDURATION` | la durée donnée aux nouvelles créatures, quand elle n'est pas 0 |
| `CONJURED` | `<nom encodé>\|<numéro>\|<dégâts>\|<rounds restants, vide si non comptés>`, dans l'ordre d'arrivée |
| `CONJUREDGROUPBONUS` | `<clé encodée>\|<valeur choisie, vide sinon>`, un bonus de groupe par ligne |
| `CONJUREDBONUS` | `<nom encodé>\|<numéro>\|<clé encodée>\|<valeur>`, un bonus propre par ligne |

Deux facettes, toutes deux déclarées dans `applicationContext.xml` : `ConjurationCatalogFacet` garde
le texte source, `ConjurationSessionFacet` la `ConjurationSession`. Une ligne illisible, un bonus
dont la source n'est plus chargée ou qui vise une créature absente sont écartés avec un
avertissement.

## 6. La page

Le gabarit `outputsheets/conjurations/conjurations.htm.ftl` reçoit le modèle `conjurations`,
construit par `pcgen.io.conjurations.ConjurationOutputModel`. Il ouvre sur
`<#ftl output_format="HTML">`, et le script n'écrit que par `textContent` et par attributs.

**En haut, le catalogue** : un tableau par groupe, avec un bouton « Conjurer » et le nom.

**En bas, les créatures en jeu.** Un en-tête avec « Round suivant », « Tout renvoyer » et la durée ;
un bandeau « Tout le groupe » avec un jeton par bonus de groupe (× le retire) et un champ
« + bonus ». Puis une carte par créature : nom et numéro, rounds restants, points de vie, « Blesser »
et « Soigner », une ligne « Ses bonus » avec ses jetons et son champ « + bonus », ses statistiques
(vitesse, CA, attaques, BMO, DMD, jets de sauvegarde, initiative), et « Renvoyer ». Une valeur
qu'un bonus a changée montre l'ancienne barrée.

Le champ « + bonus » propose, à la première saisie, les bonus que le pont renvoie ; un bonus qui
demande une valeur (Inspiration vaillante) fait apparaître un champ numérique borné. Au chargement
et après chaque conjuration, le script demande au pont les valeurs de chaque carte.

Le pont planté sur `window.pcgen` offre, en plus de `conjure`, `setDamage`, `setRoundsLeft`,
`nextRound`, `dismiss`, `dismissAll` et `setDuration` :

| Méthode | Renvoie (JSON) |
|---|---|
| `view(nom, numéro)` | la créature : `stats`, `baseStats`, `bonuses` |
| `groupBonuses()` | les bonus de groupe : `key`, `label` |
| `groupBonusChoices()` / `bonusChoices(nom, numéro)` | `key`, `label`, `range` |
| `addGroupBonus(clé, valeur)` / `removeGroupBonus(clé)` | toutes les créatures, mises à jour |
| `addBonus(nom, numéro, clé, valeur)` / `removeBonus(nom, numéro, clé)` | cette créature, mise à jour |

`valeur` vaut `-1` pour un bonus qui n'en demande pas.

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
| `set_conjuration_sheet` | écrit le texte source ; refuse un texte qui ne se lit pas ou qui nomme une race ou un gabarit absents des sources, en nommant ses lignes |
| `get_conjuration_sheet` | renvoie le texte source, la durée, les créatures en jeu et leurs bonus, et les bonus de groupe |
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
- **Créatures et bonus** : sur les vraies données du Livre de base et du Bestiaire
  (`BestiarySources`) — l'archonte molosse a les valeurs du bestiaire, Augmentation des
  convocations, Prière, Force du taureau, Rapidité, Inspiration vaillante, deux bonus de chance qui
  ne se cumulent pas.

Le `WebView` et le pont sont la frontière qu'on ne teste pas automatiquement ; ils restent aussi
minces que possible.

## 10. Hors périmètre

- Les valeurs d'arrivée dans le catalogue (voir décision 6).
- Le nombre de créatures qu'une convocation amène, et les utilisations par jour.
- Une exception par créature à un bonus de groupe.
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
