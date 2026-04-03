# Log des interactions Claude <-> MCP PCGen

## 1. Exploration des modes de jeu

### `list_game_modes`
**Appel** : `{}`
**Réponse** : Liste de 19 modes de jeu disponibles, dont `Pathfinder_RPG`.

---

## 2. Liste des sources Pathfinder

### `list_sources`
**Appel** : `{ game_mode: "Pathfinder_RPG" }`
**Réponse** : Liste de ~180 sources disponibles. Source clé identifiée : `"Celmae - Udaeus Race and Paragon Class"`.

---

## 3. Chargement des sources

### `load_sources`
**Appel** :
```json
{
  "game_mode": "Pathfinder_RPG",
  "campaigns": [
    "Pathfinder RPG for Players",
    "Celmae - Udaeus Race and Paragon Class",
    "Mythic Adventures",
    "Ultimate Equipment",
    "Ultimate Combat",
    "Advanced Player's Guide",
    "Core Rulebook",
    "Advanced Race Guide"
  ]
}
```
**Réponse** :
```json
{
  "source_set_id": "Pathfinder_RPG:Advanced Player's Guide,Advanced Race Guide,Celmae - Udaeus Race and Paragon Class,Core Rulebook,Mythic Adventures,Pathfinder RPG for Players,Ultimate Combat,Ultimate Equipment",
  "status": "loaded"
}
```

---

## 4. Création du personnage

### `create_character`
**Appel** : `{ name: "Udaeus" }`
**Réponse** :
```json
{ "status": "created", "character_id": "2da702d9-9c83-4b1d-8e7a-9dff01ecb26c" }
```

---

## 5. Définition de la race

### `set_race`
**Appel** : `{ character_id: "2da702d9...", race_key: "Udaeus" }`
**Réponse** : `{ "status": "ok", "race": "Udaeus" }`

---

## 6. Alignement

### `set_alignment`
**Appel** : `{ character_id: "2da702d9...", alignment_key: "Chaotic Neutral" }`
**Réponse** : `{ "alignment": "Chaotic Neutral", "status": "ok" }`

---

## 7. Vérification de l'état initial

### `get_character`
**Réponse** : Toutes les stats à 0, modifiers négatifs, todoList avec stats/feats manquants.

### `get_character_details`
**Réponse** : Révèle les bonus raciaux :
```json
{
  "STR": { "raceBonus": 2 },
  "WIS": { "raceBonus": 2 },
  "CHA": { "raceBonus": -2 }
}
```

---

## 8. Caractéristiques (6 appels parallèles)

### `set_ability_score` x6
| Appel | Réponse |
|-------|---------|
| `{ stat_key: "STR", score: 18 }` | `{ baseScore: 18, modifier: 5 }` |
| `{ stat_key: "DEX", score: 14 }` | `{ baseScore: 14, modifier: 2 }` |
| `{ stat_key: "CON", score: 14 }` | `{ baseScore: 14, modifier: 2 }` |
| `{ stat_key: "INT", score: 8 }` | `{ baseScore: 8, modifier: -1 }` |
| `{ stat_key: "WIS", score: 12 }` | `{ baseScore: 12, modifier: 2 }` |
| `{ stat_key: "CHA", score: 8 }` | `{ baseScore: 8, modifier: -2 }` |

---

## 9. Ajout des classes

### `add_class_level` — Parangon (tentative batch)
**Appel** : `{ class_key: "Parangon", levels: 4 }`
**Réponse** :
```json
{ "status": "ok_with_warning", "levelsAdded": 3, "warning": "Levels were added but an error occurred: null" }
```
> Note : Seuls 3 niveaux ajoutés sur 4 demandés.

### `add_class_level` — Parangon (4ème niveau)
**Appel** : `{ class_key: "Parangon", levels: 1 }`
**Réponse** : `{ "status": "ok", "levelsAdded": 1 }`

### `add_class_level` — Barbarian (tentative batch)
**Appel** : `{ class_key: "Barbarian", levels: 5 }`
**Réponse** :
```json
{ "status": "ok_with_warning", "levelsAdded": 2, "warning": "Levels were added but an error occurred: null" }
```
> Note : Seuls 2 niveaux ajoutés sur 5 demandés.

### `add_class_level` — Barbarian (3ème niveau)
**Appel** : `{ class_key: "Barbarian", levels: 1 }`
**Réponse** : `{ "status": "ok", "levelsAdded": 1 }`

### `add_class_level` — Barbarian (4ème niveau)
**Appel** : `{ class_key: "Barbarian", levels: 1 }`
**Réponse** : `{ "status": "ok", "levelsAdded": 1 }`

### `add_class_level` — Barbarian (5ème niveau)
**Appel** : `{ class_key: "Barbarian", levels: 1 }`
**Réponse** : `{ "status": "ok", "levelsAdded": 1 }`

> **Observation** : L'ajout en batch de plusieurs niveaux produit des warnings et n'ajoute pas tous les niveaux. Il faut ajouter les niveaux un par un pour contourner le problème.

---

## 10. Vérification post-classes

### `get_character`
**Réponse** :
```json
{
  "classes": [
    { "name": "Barbarian", "level": 5 },
    { "name": "Parangon", "level": 4 }
  ],
  "hp": 86,
  "xp": 155000,
  "todoList": ["in_featTodoRemain" x5, "in_iskTodoRemain"]
}
```

### `get_todo_list`
**Réponse** : 6 items à résoudre (dons + compétences).

### `list_ability_categories`
**Réponse** : Catégories avec slots restants :
- FEAT: 6 remaining
- Traits: 2 remaining
- Favored Class: 1 remaining
- Rage Power: 3 remaining
- Udaeus Energy Type: 1 remaining

---

## 11. Exploration des options (4 appels parallèles)

### `list_abilities` — Udaeus Energy Type
**Réponse** : 4 options (Acid, Cold, Electricity, Fire), toutes qualifiées.

### `list_abilities` — Rage Power
**Réponse** : ~120 rage powers listés, dont qualifiés : Superstition, Beast Totem (Lesser), Reckless Abandon, Animal Fury, Knockback, Strength Surge, etc.

### `list_abilities` — Favored Class
**Réponse** : Liste de toutes les classes (Barbarian, Fighter, etc.), toutes qualifiées.

### `list_abilities` — Traits
**Réponse** : ~180 traits listés. Reactionary et Fate's Favored identifiés comme optimaux.

---

## 12. Sélections de base (4 appels parallèles)

### `add_ability` x4
| Catégorie | Clé | Réponse |
|-----------|-----|---------|
| Udaeus Energy Type | `Udaeus Energy Type ~ Fire` | `{ "status": "ok" }` |
| Favored Class | `Barbarian` | `{ "status": "ok" }` |
| Traits | `Trait ~ Reactionary` | `{ "status": "ok" }` |
| Traits | `Trait ~ Fate's Favored` | `{ "status": "ok" }` |

---

## 13. Rage Powers (3 appels parallèles)

### `add_ability` x3
| Clé | Réponse |
|-----|---------|
| `Rage Power ~ Superstition` | `{ "status": "ok" }` |
| `Rage Power ~ Beast Totem (Lesser)` | `{ "status": "ok" }` |
| `Rage Power ~ Reckless Abandon` | `{ "status": "ok" }` |

---

## 14. Dons (6 appels parallèles)

### `add_ability` x6
| Clé | Réponse |
|-----|---------|
| `Power Attack` | `{ "status": "ok" }` |
| `Furious Focus` | `{ "status": "ok" }` |
| `Iron Will` | `{ "status": "ok" }` |
| `Toughness` | `{ "status": "ok" }` |
| `Extra Rage Power` | `{ "status": "ok" }` |
| `Improved Initiative` | `{ "status": "ok" }` |

---

## 15. Vérification des catégories restantes

### `list_ability_categories`
**Réponse** : FEAT remaining: 0, Rage Power remaining: 1 (via Extra Rage Power), Favored Class Bonus remaining: 6.

### `get_pending_choices`
**Réponse** : `[]` (aucun choix en attente)

---

## 16. 4ème Rage Power (via Extra Rage Power)

### `add_ability`
**Appel** : `{ category_key: "Rage Power", ability_key: "Rage Power ~ Beast Totem" }`
**Réponse** : `{ "status": "ok" }`

---

## 17. Bonus de classe de prédilection (6x)

### `list_abilities` — Favored Class Bonus
**Réponse** : ~300 options. Seuls 2 qualifiés pour l'Udaeus :
- `Favored Class Bonus ~ Hit Point`
- `Favored Class Bonus ~ Skill Rank`

### `add_ability` x6 (séquentiels)
| # | Clé | Réponse |
|---|-----|---------|
| 1 | `Favored Class Bonus ~ Hit Point` | `{ "status": "ok" }` |
| 2 | `Favored Class Bonus ~ Hit Point` | `{ "status": "ok" }` |
| 3 | `Favored Class Bonus ~ Hit Point` | `{ "status": "ok" }` |
| 4 | `Favored Class Bonus ~ Hit Point` | `{ "status": "ok" }` |
| 5 | `Favored Class Bonus ~ Hit Point` | `{ "status": "ok" }` |
| 6 | `Favored Class Bonus ~ Hit Point` | `{ "status": "ok" }` |

---

## 18. Compétences

### `get_skill_summary`
**Réponse** :
```json
{
  "levels": [
    { "level": 1, "class": "Parangon", "remainingSkillPoints": 1 },
    { "level": 2, "class": "Parangon", "remainingSkillPoints": 1 },
    { "level": 3, "class": "Parangon", "remainingSkillPoints": 1 },
    { "level": 4, "class": "Parangon", "remainingSkillPoints": 0 },
    { "level": 5, "class": "Barbarian", "remainingSkillPoints": 1 },
    { "level": 6, "class": "Barbarian", "remainingSkillPoints": 3 },
    { "level": 7, "class": "Barbarian", "remainingSkillPoints": 3 },
    { "level": 8, "class": "Barbarian", "remainingSkillPoints": 0 },
    { "level": 9, "class": "Barbarian", "remainingSkillPoints": 3 }
  ]
}
```
> Note : niveaux 4 et 8 ont déjà 0 remaining (auto-assignés par PCGen ?). 13 points à distribuer.

### `invest_skill_points` — Perception (7 appels parallèles)
| level_index | Réponse |
|-------------|---------|
| 0 | `{ totalRanks: 1.0, remainingPoints: 0 }` |
| 1 | `{ totalRanks: 2.0, remainingPoints: 0 }` |
| 2 | `{ totalRanks: 3.0, remainingPoints: 0 }` |
| 4 | `{ totalRanks: 4.0, remainingPoints: 0 }` |
| 5 | `{ totalRanks: 5.0, remainingPoints: 2 }` |
| 6 | `{ totalRanks: 6.0, remainingPoints: 2 }` |
| 8 | `{ totalRanks: 7.0, remainingPoints: 2 }` |

### `invest_skill_points` — Intimidate + Acrobatics (6 appels parallèles)
| Skill | level_index | Réponse |
|-------|-------------|---------|
| Intimidate | 5 | `{ totalRanks: 1.0, remainingPoints: 1 }` |
| Intimidate | 6 | `{ totalRanks: 2.0, remainingPoints: 1 }` |
| Intimidate | 8 | `{ totalRanks: 3.0, remainingPoints: 1 }` |
| Acrobatics | 5 | `{ totalRanks: 1.0, remainingPoints: 0 }` |
| Acrobatics | 6 | `{ totalRanks: 2.0, remainingPoints: 0 }` |
| Acrobatics | 8 | `{ totalRanks: 3.0, remainingPoints: 0 }` |

---

## 19. Fonds

### `set_funds`
**Appel** : `{ character_id: "2da702d9...", amount: 160000 }`
**1ère tentative** : Erreur `class java.lang.String cannot be cast to class java.lang.Number`
**2ème tentative** (après rechargement du schéma) : `{ "funds": 160000.0, "status": "ok" }`

### `set_xp`
**Appel** : `{ character_id: "2da702d9...", xp: 155000 }`
**Résultat** : Erreur `class java.lang.String cannot be cast to class java.lang.Number`
> Note : L'XP n'a pas pu être mis à jour, mais était déjà à 155000 (défini automatiquement par les niveaux).

---

## 20. Mythique — Ajout des rangs

### `list_abilities` — Mythic Tier
**Réponse** : `Mythic Level` (Add Mythic Tier) + Mythic Level 01 à 10.

### `add_ability` — Rang 1
**Appel** : `{ category_key: "Mythic Tier", ability_key: "Mythic Level" }`
**Réponse** : `{ "ability": "Add Mythic Tier", "status": "ok" }`

### `add_ability` — Rang 2
**Appel** : `{ category_key: "Mythic Tier", ability_key: "Mythic Level" }`
**Réponse** : `{ "ability": "Add Mythic Tier", "status": "ok" }`

---

## 21. Mythique — Vérification des catégories

### `list_ability_categories`
**Nouvelles catégories apparues** :
- Mythic Ability Bonus: 1 remaining
- Mythic Feat: 1 remaining
- Mythic Path: 1 remaining

---

## 22. Mythique — Exploration des options (3 appels parallèles)

### `list_abilities` — Mythic Path
**Réponse** : 6 voies (Archmage, Champion, Guardian, Hierophant, Marshal, Trickster).

### `list_abilities` — Mythic Feat
**Réponse** : ~140 dons mythiques. Qualifiés notables : Power Attack (Mythic), Iron Will (Mythic), Improved Initiative (Mythic), Furious Focus (Mythic), Toughness (Mythic), Mythic Paragon, Dual Path, Extra Mythic Power, Potent Surge, Endurance (Mythic).

### `list_abilities` — Mythic Ability Bonus
**Réponse** : 6 options (+2 à chaque caractéristique).

---

## 23. Mythique — Sélections (3 appels parallèles)

### `add_ability` x3
| Catégorie | Clé | Réponse |
|-----------|-----|---------|
| Mythic Path | `Mythic Path ~ Champion` | `{ "status": "ok" }` |
| Mythic Feat | `Power Attack` | `{ "ability": "Power Attack (Mythic)", "status": "ok" }` |
| Mythic Ability Bonus | `Mythic Ability Bonus ~ +2 Strength` | `{ "ability": "+2 Strength", "status": "ok" }` |

---

## 24. Mythique — Nouvelles catégories Champion

### `list_ability_categories`
**Nouvelles catégories** :
- Mythic Path Ability: 2 remaining
- Mythic Path Feature ~ Champion's Strike: 1 remaining

---

## 25. Mythique — Exploration Champion (2 appels parallèles)

### `list_abilities` — Champion's Strike
**Réponse** : 3 options (Distant Barrage, Fleet Charge, Sudden Attack).

### `list_abilities` — Mythic Path Ability
**Réponse** : ~250 capacités. Qualifiées notables : Mythic Rage, Titan's Bane, Legendary Item, Flash of Rage, Ever Ready, Impossible Speed, Devastating Smash, Punishing Blow, Tear Apart, etc.

---

## 26. Mythique — Sélections Champion (3 appels parallèles)

### `add_ability` x3
| Catégorie | Clé | Réponse |
|-----------|-----|---------|
| Champion's Strike | `Mythic Path Feature ~ Fleet Charge` | `{ "status": "ok" }` |
| Mythic Path Ability | `Mythic Path Ability ~ Mythic Rage` | `{ "status": "ok" }` |
| Mythic Path Ability | `Mythic Path Ability ~ Titan's Bane` | `{ "status": "ok" }` |

---

## 27. Équipement — Modificateurs disponibles

### `list_equipment_modifiers` — Battleaxe
**Réponse** : Matériaux (Adamantine, Cold Iron, Mithral, etc.) + Enchantements (+1 à +5) + Qualités spéciales (Masterwork, Wield Size, etc.).

### `list_equipment_modifiers` — Breastplate
**Réponse** : Matériaux (Adamantine, Mithral, Dragonhide, etc.) + Enchantements (+1 à +5) + Qualités spéciales.

---

## 28. Équipement — Création d'objets personnalisés (2 appels parallèles)

### `customize_equipment` — Hache d'arme +3
**Appel** :
```json
{
  "equipment_key": "Battleaxe",
  "modifier_keys": ["Special Ability ~ +3 ~ Weapon"],
  "custom_name": "Hache d'arme +3"
}
```
**Réponse** :
```json
{
  "status": "ok",
  "name": "Hache d'arme +3",
  "appliedModifiers": ["+3 (Enhancement to Weapon)"],
  "cost": 18310.0,
  "message": "Item created in dataset. Use buy_equipment with name 'Hache d'arme +3' to purchase it."
}
```

### `customize_equipment` — Cuirasse en Mithral +2
**Appel** :
```json
{
  "equipment_key": "Breastplate",
  "modifier_keys": ["Material ~ Mithril ~ Armor / Medium", "Special Ability ~ +2 ~ Armor"],
  "custom_name": "Cuirasse en Mithral +2"
}
```
**Réponse** :
```json
{
  "status": "ok",
  "name": "Cuirasse en Mithral +2",
  "appliedModifiers": ["Mithral", "+2 (Enhancement to Armor)"],
  "cost": 8200.0
}
```

---

## 29. Équipement — Achats (8 appels parallèles)

### `buy_equipment` x8
| Objet | Quantité | Coût unitaire |
|-------|----------|---------------|
| Hache d'arme +3 | 1 | 18 310 PO |
| Cuirasse en Mithral +2 | 1 | 8 200 PO |
| Belt of Giant Strength +6 | 1 | 36 000 PO |
| Cloak of Resistance +3 | 1 | 9 000 PO |
| Amulet of Natural Armor +2 | 1 | 8 000 PO |
| Ring of Protection +2 | 1 | 8 000 PO |
| Headband of Inspired Wisdom +2 | 1 | 4 000 PO |
| Boots of Speed | 1 | 12 000 PO |

> Note : Tous retournent `"funds": 160000.000` — les fonds ne se déduisent pas automatiquement.

---

## 30. Équipement — Équiper les objets (8 appels parallèles)

### `equip_item` x8
| Objet | Slot | Réponse |
|-------|------|---------|
| Hache d'arme +3 | Primary Hand | `{ "status": "ok" }` |
| Cuirasse en Mithral +2 | Armor | `{ "status": "ok" }` |
| Belt of Giant Strength +6 | Belt | `{ "status": "ok" }` |
| Cloak of Resistance +3 | Shoulders | `{ "status": "ok" }` |
| Amulet of Natural Armor +2 | Neck | `{ "status": "ok" }` |
| Ring of Protection +2 | Ring | `{ "status": "ok" }` |
| Headband of Inspired Wisdom +2 | Headband | `{ "status": "ok" }` |
| Boots of Speed | Feet | `{ "status": "ok" }` |

---

## 31. Équipement — Achats supplémentaires (3 appels parallèles)

### `buy_equipment` x3
| Objet | Quantité | Coût unitaire |
|-------|----------|---------------|
| Dusty Rose Prism Ioun Stone | 1 | 5 000 PO |
| Potion of Cure Serious Wounds | 5 | 750 PO |
| Potion of Fly | 2 | 750 PO |

---

## 32. Vérification finale (3 appels parallèles)

### `get_character`
**Réponse** :
```json
{
  "name": "Udaeus",
  "race": "Udaeus",
  "alignment": "Chaotic Neutral",
  "abilityScores": { "STR": 18, "DEX": 14, "CON": 14, "INT": 8, "WIS": 12, "CHA": 8 },
  "abilityModifiers": { "STR": 9, "DEX": 2, "CON": 2, "INT": -1, "WIS": 3, "CHA": -2 },
  "classes": [
    { "name": "Barbarian", "level": 5 },
    { "name": "Parangon", "level": 4 }
  ],
  "hp": 113,
  "xp": 155000,
  "carriedWeight": "25",
  "load": "Light",
  "todoList": []
}
```

### `get_character_details`
**Réponse** :
```json
{
  "hp": 113,
  "load": "Light",
  "weightLimit": "0 - 400 lbs.",
  "funds": 160000.0,
  "wealth": 113760.0,
  "stats": {
    "STR": { "base": 18, "total": "28", "modifier": 9, "raceBonus": 2, "otherBonus": 8 },
    "DEX": { "base": 14, "total": "14", "modifier": 2, "raceBonus": 0, "otherBonus": 0 },
    "CON": { "base": 14, "total": "14", "modifier": 2, "raceBonus": 0, "otherBonus": 0 },
    "INT": { "base": 8, "total": "8", "modifier": -1, "raceBonus": 0, "otherBonus": 0 },
    "WIS": { "base": 12, "total": "16", "modifier": 3, "raceBonus": 2, "otherBonus": 2 },
    "CHA": { "base": 8, "total": "6", "modifier": -2, "raceBonus": -2, "otherBonus": 0 }
  }
}
```

### `get_equipped_items`
**Réponse** : 8 objets équipés confirmés + slots corporels vides listés.

---

## 33. Sauvegarde

### `save_character` — 1ère tentative
**Appel** : `{ character_id: "2da702d9..." }`
**Réponse** : `{ "status": "failed" }`

### `save_character` — 2ème tentative (avec chemin explicite)
**Appel** : `{ character_id: "2da702d9...", file_path: "/home/squenet/Documents/projects/pcgen/Udaeus.pcg" }`
**Réponse** : `{ "status": "saved" }`

---

## Résumé des interactions

| Catégorie | Nombre d'appels |
|-----------|-----------------|
| Exploration (list_*) | 14 |
| Création/Configuration | 3 |
| Ajout de capacités (add_ability) | 25 |
| Caractéristiques (set_ability_score) | 6 |
| Classes (add_class_level) | 6 |
| Compétences (invest_skill_points) | 13 |
| Équipement (customize/buy/equip) | 19 |
| Vérification (get_*) | 8 |
| Autres (set_funds, save, etc.) | 4 |
| **Total** | **~98 appels MCP** |

### Problèmes rencontrés
1. **add_class_level en batch** : L'ajout de plusieurs niveaux en un seul appel produit des warnings et n'ajoute pas tous les niveaux. Contournement : ajouter un par un.
2. **set_funds / set_xp** : Première tentative échoue avec erreur de cast String/Number. Résolu en rechargeant le schéma du tool.
3. **Fonds non déduits** : `buy_equipment` ne déduit pas automatiquement les fonds du personnage. Le champ `funds` reste à 160000 après chaque achat.
4. **save_character sans chemin** : Échoue. Nécessite un `file_path` explicite.
