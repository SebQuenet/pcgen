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
   une copie du personnage en prend la moitié). Les statistiques du catalogue sont calculées au
   chargement de la page, une créature après l'autre, puis gardées en mémoire par la façade. La
   modification de la CA cible recalcule les dégâts attendus sans reconstruire les créatures.
   Une créature en jeu est aussi gardée en mémoire pour appliquer les bonus. Les valeurs calculées
   arrivent sur la page par le pont : une page exportée et ouverte dans un navigateur ne les montre pas.

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
| race source | `  race: <clé de race PCGen>`, facultatif pour conserver un nom affiché différent dans `summon:` |
| niveau | `  level: IV`, facultatif si le niveau ne figure pas à la fin du titre du groupe |
| rôles | `  role: <rôle>, <rôle>`, facultatif, une fois au plus |
| métadonnées | `  speed:`, `hp:`, `ac:`, `attack:`, `damage:`, `special:`, `cmb:`, `cmd:`, `ref:`, `fort:`, `will:`, `init:`, `spells:` ou `note:`, chacune facultative, une fois au plus |

- Le nom d'une créature se déduit : la race, suivie de ses gabarits entre parenthèses
  (« Bear (Grizzly) (Celestial Creature) »), sauf quand `race:` fournit une clé source distincte :
  le nom écrit dans `summon:` est alors conservé. Deux créatures ne portent pas le même nom, sans tenir
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
Ajouter `race:` à une entrée historique la fait passer aux statistiques calculées par PCGen ;
ses anciennes valeurs chiffrées restent dans le texte enregistré mais ne sont plus utilisées.
Le nom `summon:` reste affiché et sert à retrouver les créatures déjà en jeu.

## 4. Modèle

Paquet `pcgen.core.conjurations`, des records immuables qui se valident à la construction.

```java
record SummonCatalog(List<SummonGroup> groups)
record SummonGroup(String title, List<SummonTemplate> summons)
record SummonTemplate(String raceKey, List<String> templateKeys, int line)   // name() déduit

record BonusSourceKey(String kind, String category, String key)   // "SPELL=Prayer", "ABILITY=FEAT=Power Attack"
record AppliedBonus(BonusSourceKey source, OptionalInt chosenValue)

record ConjuredCreature(String creature, int number, int damage, OptionalInt roundsLeft, List<AppliedBonus> bonuses, String form)
record ConjurationSession(int duration, List<ConjuredCreature> conjured, List<AppliedBonus> groupBonuses, boolean skeletonUsed)
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
| `CONJURATIONSKELETONUSED` | `1` si la variante quotidienne de Skeleton Summoner a été utilisée |
| `CONJURED` | `<nom encodé>\|<numéro>\|<dégâts>\|<rounds restants, vide si non comptés>[\|<forme encodée>]`, dans l'ordre d'arrivée |
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

**En haut, le catalogue** : une seule liste des options accessibles par les sorts de convocation
connus du personnage, y compris les sorts automatiquement connus par les lanceurs préparés.
Un sort de niveau supérieur donne aussi accès aux listes de niveaux
inférieurs de la même famille ; les dons et anneaux ajoutent leurs options seulement si le sort
correspondant est connu. Le pouvoir magique de convocation du convocateur et du convocateur
unchained compte également. Un pouvoir magique inné dont le nom précise sa source entre parenthèses
(« Summon Monster VI (Invocateur Naturel) ») compte pour son niveau. Quand le personnage a Héraut
Divin (Invocateur de Saint Thero), seules restent les options dont l'alignement est celui de son
dieu ou à un cran de lui : l'alignement vient du kit `<race> Default`, et un gabarit céleste,
fiélon, résolu ou entropique remplace l'axe qu'il désigne. Les choix fixes d'Expanded Summon Monster et les gabarits Versatile
non choisis ne sont pas proposés (les deux gabarits de Versatile Summon Monster se choisissent dans
l'onglet des dons, à chaque prise du don ; une ligne `# versatile-sm:` du catalogue reste aussi lue) comme options de conjuration. Aucun sélecteur de groupe ne cache
des lignes. Le tableau est triable par
niveau, nom, note RPGBOT, dés de vie, dégâts attendus par round, CA totale, de contact et pris au
dépourvu, BMO et DMD. Un champ « CA cible » au-dessus du
tableau règle la probabilité de toucher pour les dégâts attendus ; les attaques itératives sont
comptées, avec un échec sur 1 et une réussite sur 20. Ce calcul ne comprend ni critique ni
réduction des dégâts. Les rôles restent visibles ; les notes sont réservées au panneau de détails.

La colonne Niveau n'affiche que le chiffre romain du sort (I à IX) ; le nom du groupe passe en
infobulle et dans le panneau de détails, et ne s'ajoute plus au nom de la créature. Le premier clic
sur Niveau trie du I au IX, le second inverse ; à niveau égal, les créatures suivent l'ordre
alphabétique de leur nom affiché, de A à Z, dans les deux sens. Le filtre des rôles et la liste des
bonus de groupe sont aussi dans l'ordre alphabétique, de A à Z. À côté de « Conjurer », un champ
numérique (1 par défaut, de 1 à 15) dit combien de créatures conjurer d'un clic ; les squelettes
de Skeleton Summoner, limités à un par jour, n'en ont pas. Le panneau de détails ajoute deux liens
qui ouvrent le premier résultat d'une recherche DuckDuckGo, comme les fiches d'information de
PCGen : d20pfsrd par le nom anglais de la race, pathfinder-fr par son nom français. L'en-tête des dégâts attendus est abrégé, l'intitulé
complet passe en infobulle.

Au-dessus du tableau, trois filtres se combinent : un rôle (tous par défaut), un niveau minimal et un
niveau maximal (I et IX par défaut). Une ligne cachée par un filtre reste triée avec les autres.

**Rôles.** Une créature du catalogue publié a d'un à trois rôles, pris dans ce vocabulaire fermé,
jugés pour le niveau du sort qui l'appelle :

| Clé | Rôle | Critère |
|-----|------|---------|
| `bruiser` | Cogneur | dégâts au contact élevés : attaque à outrance, bond, éventration |
| `ranged` | Tireur | attaque à distance ou souffle utilisable sans attendre plusieurs rounds |
| `area` | Zone | dégâts sur plusieurs cibles : souffle, boule de feu, tourbillon |
| `maneuvers` | Manœuvres | croc-en-jambe, étreinte, constriction ; BMO au-dessus de la moyenne |
| `control` | Contrôle | pouvoirs qui entravent : toile, peur, charme, paralysie, poison affaiblissant, mur |
| `support` | Soutien | soins, bénédictions ou protections données aux alliés |
| `blocker` | Rempart | CA ou PV élevés, allonge, tient un couloir |
| `scout` | Éclaireur | vol rapide, creusement, invisibilité, vision dans le noir, perception supérieure |
| `utility` | Utilitaire | hors combat : transport, compétences, lumière, travail, langue |

Les rôles et leur mode d'emploi vivent dans les guides (`pathfinder1-guides-fr.json` et `-en.json`),
sous `roles` : une clé du vocabulaire par rôle, associée à une ou deux phrases qui disent quand et
comment jouer la créature dans ce rôle. Les deux langues donnent les mêmes clés dans le même ordre.
Les libellés viennent de `LanguageBundle` (`in_conjurations_role_<clé>`). Une ligne écrite à la main
garde ses propres rôles, en texte libre, sans mode d'emploi.

Une loupe à côté du nom, dans le catalogue comme sur la carte d'une créature en jeu, ouvre un
panneau à droite. Il réunit le niveau et les rôles, les conseils tactiques propres à la race,
le mode d'emploi de chaque rôle,
les capacités et notes rédigées par le joueur, puis les statistiques calculées par PCGen. Les
conseils français pour les 16 races du catalogue de Miros sont dans
`code/src/resources/pcgen/conjurations/pathfinder1-guides-fr.json`. Ils sont séparés des notes du
personnage et ne remplacent pas les règles ou la fiche calculée. Une race absente du guide conserve
ses notes, capacités et statistiques dans le même panneau. Le panneau lie aussi chaque entrée à
sa fiche complète du Bestiaire ; dans PCGen, ces liens s'ouvrent dans le navigateur système.

Le panneau s'ouvre sur une illustration de la race : un JPEG de 250×250 par race du catalogue publié,
dans `code/src/resources/pcgen/conjurations/portraits/`, nommé d'après la clé de race en minuscules,
chaque suite de caractères non alphanumériques remplacée par `_`. `PathfinderSummonPortraits` le lit et le pont le
renvoie à la page en URI `data:` par `portrait(race)`, car la page est chargée sans adresse de base.
Une page exportée n'a donc pas d'illustration. `scripts/generate_summon_portraits.py` fait dessiner
les races manquantes par la CLI Codex, en style d'affiche de film d'heroic fantasy sur fond blanc.

Sur sa carte, une créature en jeu de race `Dog`, `Wolf` ou `Wolf (Dire)` porte l'une des six
illustrations canines de `portraits/canine/` (`1.jpg` à `6.jpg`), choisie selon son numéro :
la créature n° 1 prend la première, la n° 7 reprend la première. Le pont la renvoie par
`cardPortrait(race, numéro)`. Les autres créatures n'ont pas d'illustration sur leur carte.

Ces conseils synthétisent le [guide de RPGBOT](https://rpgbot.net/pathfinder/characters/summon_monster/),
les [règles de Convocation de monstres](https://legacy.aonprd.com/coreRulebook/spells/summonMonster.html)
et les entrées du Bestiaire de Pathfinder 1. Ils sont paraphrasés et les deux premières sources
sont liées depuis le panneau. Les pouvoirs interdits aux créatures convoquées, notamment la
téléportation et les sorts à composante matérielle coûteuse, sont signalés lorsque pertinents.

**En bas, les créatures en jeu.** Un en-tête avec « Round suivant », « Tout renvoyer » et la durée ;
un bandeau « Tout le groupe » avec un jeton par bonus de groupe (× le retire) et un champ
« + bonus ». Puis une carte par créature : nom et numéro, rounds restants, points de vie, « Blesser »
et « Soigner », une ligne « Ses bonus » avec ses jetons et son champ « + bonus », ses statistiques
(vitesse, CA, attaques, BMO, DMD, jets de sauvegarde, initiative), et « Renvoyer ». Une valeur
qu'un bonus a changée montre l'ancienne barrée.

Un clic sur le nom d'une carte permet de le réécrire. Entrée valide, Échap rétablit l'ancien nom,
un nom vide est refusé. Le nouveau nom n'existe que dans la page : il n'est ni envoyé au pont ni
enregistré, et disparaît quand la page est redessinée (texte du catalogue modifié ou changement
de personnage).

Les créatures dotées de formes calculées ont un sélecteur sur leur carte. Les formes canines de
l'archon molosse recalculent taille, vitesse, CA, BMO, DMD, initiative, Réflexes et morsure ; la
forme de vent du bralani modifie son déplacement et ses attaques. La fusion de neuf archons
lumineux est décrite dans le panneau de détails comme capacité collective et ne figure pas parmi
les formes d'un archon individuel.

Le menu « + bonus » liste les bonus que le pont renvoie pour le groupe ou la créature ; un bonus qui
demande une valeur (Inspiration vaillante) fait apparaître un champ numérique borné et un bouton
« Appliquer ». Au chargement et après chaque conjuration, le script demande au pont les valeurs
de chaque carte.

**Variantes repliées.** Les créatures de même race et de même niveau forment un groupe, quelle que soit
leur liste d'origine (convocation de monstres, Summon Good Monster, ligne écrite à la main). Seule la
ligne qui a le moins de gabarits reste affichée, la première du catalogue en cas d'égalité ; un
bouton « ▸ +N » devant son nom déplie les N autres juste en dessous, et « ▾ » les replie. Le tri et
les filtres portent sur la ligne de base ; ses variantes la suivent dans l'ordre du catalogue.

**Carte d'une variante.** La carte d'une créature en jeu nomme ses gabarits sous son nom. Quand ses
valeurs arrivent, elle liste aussi ce que ces gabarits changent par rapport à la même créature
construite sans gabarit (même kit, même Augment Summoning, même forme) : chaque statistique qui
diffère, « avant → après », puis les capacités ajoutées. La créature sans gabarit est construite une
fois par race et gardée en mémoire. `view(nom, numéro)` renvoie ces écarts dans `templateDifferences`
(`stat`, `withoutTemplates`, `withTemplates`).

**Variantes mythiques.** Avec la capacité mythique Mighty Summons, un conjurateur qui convoque une
seule créature peut dépenser une utilisation de pouvoir mythique pour lui donner le gabarit mythique
simple agile ou sauvage. Le catalogue publié ajoute donc, pour chaque option des deux plus hauts
niveaux de convocation du personnage, une variante agile et une variante sauvage (gabarits
`Mythic Simple Template ~ Agile` et `~ Savage`). Une variante n'amène jamais qu'une créature.
Les deux gabarits suivent les règles de *Mythic Adventures* (rang mythique 1, ou 2 à partir de
11 DV pour sauvage ; PV mythiques de 6, 8 ou 10 par rang selon le dé de vie) :

- agile : +20 en initiative, +2 d'esquive à la CA, +30 pieds à chaque déplacement sans dépasser
  le double de la vitesse de base au sol, évasion, double initiative ;
- sauvage : +2 d'armure naturelle, RD et résistance à toutes les énergies selon les DV (1-4 : 5 ;
  5-10 : 10 et RD 5/épique ; 11+ : 15 et RD 10/épique), saignement 1, sauvagerie bestiale.

L'attaque supplémentaire de la sauvagerie bestiale et le saignement figurent parmi les capacités ;
ils n'entrent pas dans les attaques calculées.

**Tableau à télécharger.** Un bouton au-dessus du catalogue enregistre le catalogue en classeur
`.xlsx`, construit comme le tableur de Shalendra : un en-tête gras sur fond gris, une ligne par
créature, triées par niveau puis par nom ; les variantes d'une créature suivent sa ligne de base,
repliées sous elle dans le plan du classeur (bouton « + » dans la marge), comme dans la page. Une
créature qui a des formes calculées (archon molosse : chien, loup, loup sanguinaire de Forme animale
II ; bralani : forme de vent) a aussi une ligne repliée par forme, « ↳ <créature> — <forme> », avec
les valeurs de cette forme et le même nombre maximal ; ses variantes ont aussi les leurs. Mise en
forme (`ConjurationSheetStyle`) : en-tête blanc sur fond sombre, figé avec les quatre premières
colonnes (niveau, rôles, créature, nb max) et répété à l'impression ; un fond par niveau, alterné
d'un niveau à l'autre, et une bordure plus sombre au début de chaque niveau ; une variante en
italique, décalée et précédée de « ↳ », sur un fond plus pâle ; les nombres centrés, niveau et nb max
en gras ; la note RPGBOT, en colonne après nb max, dans sa couleur (bleu, vert, orange, rouge) ;
Calibri 10, fines bordures grises, largeurs fixées par colonne ; impression en paysage, ajustée à la
largeur de la page. Les colonnes sont niveau (chiffre romain), rôles, créature, nombre
maximal, vitesse, points de vie, CA (totale / contact / pris au dépourvu), attaques, dégâts,
capacités spéciales, BMO, DMD, Réflexes, Vigueur, Volonté, initiative et sorts. Les valeurs sont
celles que calcule PCGen pour le catalogue, Augment Summoning compris ; une créature écrite à la
main avec ses propres valeurs les garde. Les capacités spéciales sont celles de la créature
construite, avec sa RD et sa RM ; une ligne `special:` les remplace, et la colonne des sorts reprend
la ligne `spells:`.

Le nombre maximal suppose les dés au maximum. Avec un sort de convocation de niveau N, une
créature de niveau N vient seule, une de niveau N-1 vient à 1d3 (3), une de niveau inférieur à
1d4+1 (5). Superior Summoning et Mighty Summons ajoutent chacun une créature quand le sort en amène
plusieurs. N est le plus haut niveau connu de la famille de la créature (convocation de monstres
ou convocation d'alliés naturels). Une variante mythique vaut 1.

La construction des créatures qui manquent encore se fait hors du fil de l'interface, après le
choix du fichier ; le pont n'a pas de valeur de retour pour cette opération.

Le pont planté sur `window.pcgen` offre, en plus de `conjure`, `setDamage`, `setRoundsLeft`,
`nextRound`, `dismiss`, `dismissAll` et `setDuration` :

| Méthode | Renvoie (JSON) |
|---|---|
| `view(nom, numéro)` | la créature : `stats`, `baseStats`, `bonuses` |
| `groupBonuses()` | les bonus de groupe : `key`, `label` |
| `groupBonusChoices()` / `bonusChoices(nom, numéro)` | `key`, `label`, `range` |
| `addGroupBonus(clé, valeur)` / `removeGroupBonus(clé)` | toutes les créatures, mises à jour |
| `addBonus(nom, numéro, clé, valeur)` / `removeBonus(nom, numéro, clé)` | cette créature, mise à jour |
| `portrait(race)` | l'illustration en URI `data:`, ou une chaîne vide (texte brut, pas du JSON) |
| `cardPortrait(race, numéro)` | l'illustration de la carte en URI `data:`, ou une chaîne vide |
| `saveSpreadsheet()` | rien : demande où enregistrer le classeur, puis l'écrit |

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
- Le nombre de créatures qu'une convocation amène en jeu (seul le tableau donne le maximum), et
  les utilisations par jour.
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
