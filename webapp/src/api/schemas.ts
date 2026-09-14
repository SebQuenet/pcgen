import { z } from 'zod'

/** Something the loaded data holds, as callers name it and as a reader sees it. */
export const namedEntry = z.object({ key: z.string(), name: z.string() })

export const gameMode = z.object({ name: z.string(), display_name: z.string() })

export const sourcesLoaded = z.object({
  source_set_id: z.string(),
  auto_added_dependencies: z.array(z.string()),
  warnings: z.array(z.string()),
})

export const characterCreated = z.object({ character_id: z.string() })

export const classLevel = z.object({ name: z.string(), level: z.number() })

export const characterSummary = z.object({
  name: z.string(),
  race: z.string().nullable(),
  alignment: z.string().nullable(),
  deity: z.string().nullable(),
  ability_scores: z.record(z.string(), z.number()),
  ability_modifiers: z.record(z.string(), z.number()),
  classes: z.array(classLevel),
  hp: z.number(),
  xp: z.number(),
  xp_for_next_level: z.number(),
  carried_weight: z.string().nullable(),
  load: z.string().nullable(),
  todo_list: z.array(z.string()),
})

export const statDetail = z.object({
  base: z.number(),
  total: z.string(),
  modifier: z.number(),
  race_bonus: z.number(),
  other_bonus: z.number(),
})

export const characterDetails = z.object({
  hp: z.number(),
  carried_weight: z.string().nullable(),
  load: z.string().nullable(),
  weight_limit: z.string().nullable(),
  funds: z.number(),
  wealth: z.number(),
  stats: z.record(z.string(), statDetail),
})

export const levelSkillPoints = z.object({
  level: z.number(),
  character_class: z.string(),
  remaining_skill_points: z.number(),
  spent_skill_points: z.number(),
  gained_skill_points: z.number(),
})

export const trainedSkill = z.object({
  key: z.string(),
  name: z.string(),
  ranks: z.number(),
  modifier: z.number(),
  total: z.number(),
})

export const skillSummary = z.object({
  levels: z.array(levelSkillPoints),
  trained_skills: z.array(trainedSkill),
})

export const skillInvestment = z.object({
  skill: z.string(),
  points_invested: z.number(),
  total_ranks: z.number(),
  remaining_points: z.number(),
  choices_applied: z.array(z.string()),
  note: z.string().nullable(),
})

export const abilityCategory = z.object({
  key: z.string(),
  name: z.string(),
  remaining: z.number(),
  total: z.number(),
})

export const availableAbility = z.object({
  key: z.string(),
  name: z.string(),
  qualified: z.boolean(),
})

export const abilityAdded = z.object({
  status: z.string(),
  ability: z.string(),
  choice: z.array(z.string()),
  available_choices: z.array(z.string()),
  note: z.string().nullable(),
})

export const inventoryItem = z.object({
  key: z.string(),
  name: z.string(),
  quantity: z.number(),
  cost: z.number(),
  weight: z.number(),
})

export const inventory = z.object({
  items: z.array(inventoryItem),
  funds: z.number(),
  carried_weight: z.string().nullable(),
  load: z.string().nullable(),
})

export const equippedSlot = z.object({
  slot: z.string(),
  type: z.string(),
  equipment: z.string().nullable(),
  equipment_key: z.string().nullable(),
  quantity: z.number().nullable(),
})

export const spellEntry = z.object({
  key: z.string(),
  name: z.string(),
  level: z.string().nullable(),
  character_class: z.string().nullable(),
  school: z.string().nullable(),
  subschool: z.string().nullable(),
  components: z.string().nullable(),
  range: z.string().nullable(),
  duration: z.string().nullable(),
  cast_time: z.string().nullable(),
  count: z.number().nullable(),
  spell_list: z.string().nullable(),
})

export const biography = z.object({
  name: z.string().nullable(),
  players_name: z.string().nullable(),
  gender: z.string().nullable(),
  age: z.number().nullable(),
  age_category: z.string().nullable(),
  weight: z.number().nullable(),
  hair_color: z.string().nullable(),
  eye_color: z.string().nullable(),
  skin_color: z.string().nullable(),
  handed: z.string().nullable(),
  bio: z.string().nullable(),
  description: z.string().nullable(),
  companions: z.string().nullable(),
  assets: z.string().nullable(),
  magic: z.string().nullable(),
  gm_notes: z.string().nullable(),
})

export const todoList = z.object({ todo_list: z.array(z.string()), count: z.number() })

export const dirtyState = z.object({ dirty: z.boolean() })

export const pendingChoice = z.object({
  choice_id: z.string(),
  title: z.string(),
  available_options: z.array(z.string()),
  remaining_selections: z.number(),
  require_complete: z.boolean(),
})

export const renderedSheet = z.object({ format: z.string(), content: z.string() })

/** Operations whose answer the interface does not read beyond "it worked". */
export const acknowledged = z.unknown()

export type GameMode = z.infer<typeof gameMode>
export type NamedEntry = z.infer<typeof namedEntry>
export type CharacterSummary = z.infer<typeof characterSummary>
export type CharacterDetails = z.infer<typeof characterDetails>
export type SkillSummary = z.infer<typeof skillSummary>
export type AbilityCategory = z.infer<typeof abilityCategory>
export type AvailableAbility = z.infer<typeof availableAbility>
export type AbilityAdded = z.infer<typeof abilityAdded>
export type Inventory = z.infer<typeof inventory>
export type EquippedSlot = z.infer<typeof equippedSlot>
export type SpellEntry = z.infer<typeof spellEntry>
export type Biography = z.infer<typeof biography>
export type TodoList = z.infer<typeof todoList>
export type PendingChoice = z.infer<typeof pendingChoice>
export type RenderedSheet = z.infer<typeof renderedSheet>
export type SourcesLoaded = z.infer<typeof sourcesLoaded>
