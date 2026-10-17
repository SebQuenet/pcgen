# Conception : bonus des créatures conjurées

**Cible** : le joueur d'un conjurateur (Shalendra), en partie, sur la page Conjurations.
**Remplace** les décisions 2 et 5 de `specs/conjurations.md` et son §10 (« calculer les statistiques
depuis les races du bestiaire » sort du hors-périmètre).

## 1. Intention

Le joueur veut que les bonus soient comptés correctement sur ses créatures : Prière, Inspiration
vaillante, Force du taureau, Rapidité… Tout modificateur qui vaut pour un personnage doit valoir
pour une créature, avec les règles de cumul de PCGen (deux bonus de chance ne s'additionnent pas,
Force +4 donne +3 aux dégâts d'une arme à deux mains).

Des valeurs écrites à la main (`attack: +11/+6 épée`) ne le permettent pas : on ne sait pas d'où
viennent les totaux. Chaque créature devient donc une vraie créature PCGen, calculée depuis sa race.

## 2. Objectifs

1. Chaque entrée du catalogue désigne une race PCGen et, au besoin, des gabarits ; PCGen calcule
   toutes ses valeurs.
2. **Bonus de groupe** : posés une fois, ils s'appliquent à toutes les créatures en jeu **et à celles
   conjurées ensuite**, jusqu'à ce que le joueur les retire, ce qui les retire de toutes.
3. **Bonus propres** : une créature peut en porter en plus de ceux du groupe (Force du taureau sur
   l'archonte 2 seulement).
4. Une créature peut recevoir un bonus que le personnage n'a pas : ce qu'elle se donne elle-même
   (le sort *aide* de l'archonte molosse, la rage d'une bête).
5. Si le personnage a le don Augmentation des convocations, chaque créature reçoit le gabarit
   `Augment Summoning` (`cr_templates.lst:30`) à son arrivée, sans clic.

## 3. Non-objectifs

- Les bonus du personnage ne passent jamais aux créatures, et inversement.
- Pas de décompte de rounds par bonus : un bonus reste posé jusqu'à ce qu'on le retire.
- Pas d'exception par créature à un bonus de groupe (retirer Prière d'une seule créature).
- Pas de rôles ni de notes dans le catalogue, donc plus de filtre par rôle (décision du joueur :
  « le lien vers sa race et son gabarit »).
- Pas d'opération MCP/HTTP qui pose ou retire un bonus : comme le reste du combat, c'est le joueur
  qui clique (décision 4 de `specs/conjurations.md`).

## 4. Anti-objectifs

- Un bonus compté deux fois, ou cumulé contre les règles de type : la fonction n'a plus de raison
  d'être si les nombres sont faux.
- Une page qui se redessine à chaque clic et perd le défilement : la règle de la décision 3 tient.

## 5. Catalogue

Le texte source reste (éditeur à gauche, écrit à la main ou par un agent). Nouvelle forme :

```
## Convocation de monstres IV

summon: Archon (Hound)
summon: Agathion (Vulpinal)
summon: Eagle
  template: Celestial Creature
```

- `summon:` porte la **clé de race** PCGen.
- `template:` facultatif, des clés de gabarit séparées par des virgules.
- Le nom de l'entrée se déduit : nom de la race, suivi des gabarits entre parenthèses
  (« Eagle (Celestial Creature) », « Eagle (Fiendish Creature) »). C'est ce nom que les créatures
  en jeu désignent, comme aujourd'hui (« Eagle (Celestial Creature) 2 »).
- Les anciennes sous-clés (`hp`, `attack`, `role`…) deviennent des erreurs de lecture.
- Erreurs ajoutées : race inconnue dans les sources chargées, gabarit inconnu, deux entrées au
  même nom déduit.
- Le catalogue de Shalendra (45 entrées) est réécrit une fois avec les clés PCGen. Les trois
  noms qui diffèrent le plus : `Vulnudaemon` (B3), `Protean (Naunet)` (B2), `Rhinoceros (Woolly)` (B1).

## 6. La créature

Une créature (du catalogue ou en jeu) est un `PlayerCharacter` construit sur les sources du
personnage : race, puis le kit `<race> Default` du bestiaire quand il existe (dons,
caractéristiques, arme : sans lui l'archonte molosse n'a pas d'épée), puis les gabarits, puis
`Augment Summoning` si le personnage a le don. Elle n'est jamais enregistrée comme personnage :
elle se reconstruit depuis le `.pcg` du conjurateur.

Valeurs affichées : vitesse, PV max, CA (normale / contact / pris au dépourvu), attaques avec
leurs dégâts, BMO, DMD, Réf, Vig, Vol, Init. PV actuels = PV max − dégâts, donc un bonus de
Constitution change aussi les PV actuels.

## 7. Bonus

- **Liste d'une créature** : celle que PCGen construit pour un personnage
  (`CharacterFacadeImpl.java:1043-1104`) — bonus de ses propres capacités, sorts et objets, plus
  tous les bonus `TEMPBONUS:ANYPC` des sources chargées.
- **Liste du groupe** : seulement les bonus `ANYPC` des sources chargées.
- Application comme `TempBonusHelper` : `addTempBonus(bonus, source, créature)`, formule évaluée
  sur la créature.
- Un bonus à valeur choisie (`TEMPVALUE`, ex. Inspiration vaillante +1 à +3) demande sa valeur
  dans la page au moment de le poser ; la valeur est enregistrée avec le bonus.

## 8. La page (maquette A, choisie par le joueur)

En haut, le catalogue groupé par niveau de convocation ; chaque ligne montre les valeurs d'arrivée
(gabarits et Augmentation compris, aucun bonus).

En bas, les créatures en jeu :

- un bandeau « Tout le groupe » : un jeton par bonus de groupe, avec × pour le retirer, et
  « + bonus » ;
- sur chaque carte, une ligne « Ses bonus » avec les mêmes jetons et son propre « + bonus » ;
- les statistiques recalculées ; une valeur modifiée par un bonus montre l'ancienne barrée ;
- PV, blessures, rounds, renvoi : inchangés.

« + bonus » ouvre une liste filtrée à la frappe. Poser ou retirer un bonus passe par le pont, qui
renvoie les nouvelles valeurs des créatures touchées ; le script met à jour les cartes sans
redessiner la page. Toujours `textContent`, jamais `innerHTML`.

Ajouts au pont : `addGroupBonus(clé, valeur?)`, `removeGroupBonus(clé)`,
`addBonus(nom, numéro, clé, valeur?)`, `removeBonus(nom, numéro, clé)`, `bonusChoices(nom?, numéro?)`.

## 9. Stockage `.pcg`

| Ligne | Contenu |
|---|---|
| `CONJUREDGROUPBONUS` | `<clé encodée>\|<valeur choisie, vide sinon>`, une ligne par bonus de groupe |
| `CONJUREDBONUS` | `<nom encodé>\|<numéro>\|<clé encodée>\|<valeur>`, une ligne par bonus propre |

Un bonus dont la clé n'existe plus dans les sources est écarté avec un avertissement. L'état reste
dans `ConjurationSessionFacet` (déclarée dans `applicationContext.xml`).

## 10. Contraintes

- Construire un personnage PCGen coûte cher. Le catalogue en compte 45 : ses valeurs se calculent
  une fois par changement de texte ou de sources, pas à chaque rendu. Le constructeur mesure le
  temps sur le catalogue de Shalendra ; au-delà de quelques secondes à l'ouverture, il le signale.
- Construire une créature ne doit ouvrir aucune boîte de dialogue : choix de kit ou de bonus
  passent par un `HeadlessUIDelegate`, comme `LanguageCompanionService.addCompanion`.
- Tests : sur de vraies créatures PCGen (Archon (Hound) avec Prière + Force du taureau, cumul de
  deux bonus de chance), jamais une doublure ; aller-retour `.pcg` dans `BasicSaveRestoreTest`.
- `specs/conjurations.md` est mis à jour pour refléter ce document une fois construit.

## 11. Laissé au constructeur

- La forme exacte du modèle (records) et du JSON renvoyé par le pont.
- Le libellé des jetons (nom du sort, de la capacité).
- Comment repérer l'« ancienne valeur » à barrer (comparer avec la créature sans bonus).

## 12. Choix faits par Claude, à vérifier

1. Le texte source du catalogue reste ; les anciennes sous-clés deviennent des erreurs, et Shalendra
   est réécrite une fois.
2. Le kit `<race> Default` est appliqué à chaque créature.
3. Le catalogue affiche les valeurs d'arrivée, Augmentation comprise.
4. Pas d'exception par créature à un bonus de groupe.
5. Aucune opération MCP/HTTP n'écrit les bonus ; `get_conjuration_sheet` les renvoie en lecture.
6. Un bonus à valeur choisie demande sa valeur dans la page.
7. Le nom d'une entrée se déduit de sa race et de ses gabarits.
