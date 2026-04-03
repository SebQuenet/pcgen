# Problemes MCP restant a resoudre

## 1. Erreur de serialisation sur `add_class_level`

**Outil** : `add_class_level`
**Contexte** : Ajout d'un niveau de Parangon (surtout niveau 1 et niveau 4)
**Erreur** : `invalid_union` — le serveur MCP retourne un contenu non-texte (ni `text`, ni `image`, ni `audio`, ni `resource_link`) que le protocole MCP ne sait pas serialiser.
**Comportement** : L'action s'execute cote serveur (le niveau est bien ajoute) mais le client recoit une erreur. Resultat imprevisible : parfois le niveau est ajoute, parfois non. Il faut systematiquement verifier avec `get_character` apres chaque appel.
**Cause probable** : La reponse de `add_class_level` inclut un type de contenu MCP non standard (possiblement un objet complexe ou `undefined` au lieu d'un `text`). Cela arrive quand le niveau ajoute declenche des capacites speciales (Infuse Arms, Mythic Tier).
**Impact** : Lors du premier essai, 4 niveaux fantomes ont ete ajoutes silencieusement, obligeant a fermer et recreer le personnage.

## 2. `add_class_level` avec `levels > 1` instable

**Outil** : `add_class_level` avec parametre `levels: 4`
**Contexte** : Tentative d'ajouter 4 niveaux de Parangon en un seul appel
**Erreur** : Meme erreur de serialisation que ci-dessus, mais avec un resultat encore plus imprevisible (parfois 0 niveaux ajoutes, parfois tous).
**Recommandation** : Toujours ajouter les niveaux un par un (`levels: 1`) et verifier l'etat apres chaque ajout.

## 3. Classe de predilection (Favored Class) ne reconnait pas les classes homebrew

**Outil** : `add_ability` avec `category_key: "Favored Class"`
**Contexte** : Tentative de selectionner "Parangon" comme classe de predilection
**Erreur** : `Ability not found: Parangon`
**Cause** : La liste des classes de predilection est codee en dur avec les classes officielles Pathfinder uniquement. Les classes homebrew n'y apparaissent pas.
**Contournement** : Choisir une autre classe (Barbarian dans notre cas).

## 4. Les fonds ne sont pas debites lors des achats

**Outil** : `buy_equipment`, `customize_equipment`
**Contexte** : Achat de tout l'equipement (56 125 po au total)
**Comportement** : Le champ `funds` reste a 60 000 po apres chaque achat. Les objets sont bien ajoutes a l'inventaire mais l'or n'est jamais deduit.
**Impact** : Impossible de suivre le budget restant via l'API. Le calcul doit etre fait manuellement.

## 5. Equipement d'armes a 2 mains avec bouclier impossible

**Outil** : `equip_item`
**Contexte** : Equipement d'une Longue Lance +1 (arme a 2 mains) dans "Primary Hand" alors qu'un bouclier est en "Secondary Hand"
**Erreur** : `Cannot equip +1 Longspear in slot Primary Hand`
**Cause** : Le don "Lance et Bouclier" (homebrew) reduit la categorie de maniement de la longue lance via `BONUS:WIELDCATEGORY|Longspear,Spear|-1`, mais `equip_item` ne prend pas en compte ce changement de categorie pour determiner les slots valides.
**Contournement** : Placer la lance en "Carried". Les bonus mecaniques du don s'appliquent quand meme.

## 6. Noms d'equipement magique non devinables

**Outil** : `buy_equipment`
**Contexte** : Tentative d'achat de "Longspear (Masterwork)", "Full Plate +1", "Shield, Heavy Steel +1"
**Erreur** : `Equipment not found`
**Cause** : Les objets magiques/masterwork ne sont pas des entrees directes dans la base. Il faut acheter l'objet de base puis utiliser `customize_equipment` avec les bons `modifier_keys`.
**Recommandation** : Documenter ce workflow dans l'aide de `buy_equipment`, ou accepter des noms courants comme alias.

## 7. Slot "Shield" vs "Secondary Hand" inconsistant

**Outil** : `equip_item`
**Contexte** : `get_equipped_items` liste un slot "Shield" (BODY_SLOT), mais equiper un bouclier dans "Shield" echoue
**Erreur** : `Cannot equip +1 Shield, Heavy Steel in slot Shield`
**Contournement** : Utiliser "Secondary Hand" comme slot pour les boucliers.
**Recommandation** : Soit supprimer "Shield" de la liste des slots, soit le faire fonctionner.
