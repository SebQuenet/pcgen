# Conception : les images du scénario et l'envoi vers Tabletop Simulator

**Cible** : le MJ qui prépare une séance dans la table de jeu, dans l'interface ou par MCP.
**Périmètre** : ranger dans le scénario les images d'une séance, les voir, les enregistrer sur le
disque, et les envoyer vers Tabletop Simulator par l'outil TTS-cloud-manager de l'utilisateur.

Les passages marqués *(choix du rédacteur)* sont des décisions prises sans l'utilisateur.

## 1. Pourquoi

Le MJ rassemble aujourd'hui à la main, d'un dossier à l'autre, les illustrations et les cartes
d'une séance, puis les fait entrer dans Tabletop Simulator une à une. Les images doivent vivre avec
le scénario, se transmettre avec lui à un autre MJ, et partir vers la table en un geste.

## 2. Ce qui est rangé où

| Niveau | Catégories, dans l'ordre d'affichage |
|---|---|
| Scénario | illustration du scénario, carte générale |
| Rencontre | illustration du lieu, battlemap, autre |

Les images de monstres sont hors périmètre.

Une image a un nom (celui du fichier par défaut), une catégorie, un format et son contenu. Les
formats acceptés sont ceux que Java sait afficher : PNG, JPEG, GIF, BMP *(choix du rédacteur : WebP
refusé, faute de lecteur dans le JDK)*. Le format se lit dans les premiers octets du fichier, pas
dans son extension. Une image dépasse rarement 50 Mo ; au-delà, elle est refusée *(choix du
rédacteur)*.

## 3. Stockage

Les images sont **dans le fichier `.pcgtable`**, encodées en base 64 : le scénario reste un seul
fichier à transmettre. Celles d'une rencontre sont rangées dans la rencontre, et la suivent quand
elle est renommée. Un scénario écrit avant cette fonction s'ouvre sans image.

## 4. Gestes

Dans l'interface, onglet **Rencontres** :

- en haut de la colonne de gauche, les images du scénario ;
- sous les notes de la rencontre choisie, ses images.

Chaque liste propose : **Ajouter…** (choix du fichier puis de la catégorie), **Voir** (fenêtre à
défilement, image à sa taille), **Enregistrer sous…**, **Envoyer vers le cloud TTS**, **Poser sur
la table TTS**, **Supprimer** (avec confirmation).

Par MCP et HTTP : `add_image` (d'un chemin de fichier local ou d'un contenu en base 64),
`get_image` (contenu en base 64), `remove_image`, `send_image_to_tabletop` (cloud seul, ou cloud
puis table). L'état de la table de jeu liste les images sans leur contenu. Par HTTP,
`GET /images/{id}` sert l'image elle-même, pour l'ouvrir dans un navigateur.

## 5. Envoi vers Tabletop Simulator

Le serveur local de TTS-cloud-manager (`http://127.0.0.1:8788` ; variable d'environnement
`PCGEN_TABLETOP_URL` pour un autre) sait déjà tout faire, il n'est pas modifié :

- **vers le cloud** : `POST /files` (multipart : `file`, `folder`) dépose l'image dans le dossier
  `PCGen` du cloud Steam *(choix du rédacteur)* ; si l'outil répond `file-already-exists`, l'image
  y est déjà et sa clé sert ;
- **sur la table** : après le dépôt, `POST /spawn` avec la clé fait naître une tuile
  (`Custom_Tile`) dans la partie chargée.

Un outil injoignable ou un refus est rapporté au MJ avec la raison, sans exception.

## 6. Tests

Modèle et fichier (aller-retour, ancien fichier, format refusé, catégorie hors niveau), session
(ajout, suppression), client TTS contre un faux serveur HTTP local (dépôt, déjà présent, pose,
injoignable), opérations servies.
