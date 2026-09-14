import { z } from 'zod'

import { callOperation, type Result } from './client'
import * as schemas from './schemas'

const list = <T>(of: z.ZodType<T>) => z.array(of)

// Sources
export const listGameModes = (): Promise<Result<schemas.GameMode[]>> =>
  callOperation('list_game_modes', {}, list(schemas.gameMode))

export const listSources = (gameMode: string): Promise<Result<schemas.NamedEntry[]>> =>
  callOperation('list_sources', { game_mode: gameMode }, list(schemas.namedEntry))

export const loadSources = (gameMode: string, campaigns: string[]): Promise<Result<schemas.SourcesLoaded>> =>
  callOperation('load_sources', { game_mode: gameMode, campaigns }, schemas.sourcesLoaded)

// Character lifecycle
export const createCharacter = (name: string): Promise<Result<{ character_id: string }>> =>
  callOperation('create_character', { name }, schemas.characterCreated)

export const getCharacter = (characterId: string): Promise<Result<schemas.CharacterSummary>> =>
  callOperation('get_character', { character_id: characterId }, schemas.characterSummary)

export const saveCharacter = (characterId: string): Promise<Result<unknown>> =>
  callOperation('save_character', { character_id: characterId }, schemas.acknowledged)

export const closeCharacter = (characterId: string): Promise<Result<unknown>> =>
  callOperation('close_character', { character_id: characterId }, schemas.acknowledged)

export const openCharacter = (filePath: string): Promise<Result<{ character_id: string }>> =>
  callOperation('open_character', { file_path: filePath }, schemas.characterCreated)

// Identity
export const setName = (characterId: string, name: string): Promise<Result<unknown>> =>
  callOperation('set_name', { character_id: characterId, name }, schemas.acknowledged)

export const setRace = (characterId: string, raceKey: string): Promise<Result<unknown>> =>
  callOperation('set_race', { character_id: characterId, race_key: raceKey }, schemas.acknowledged)

export const setAlignment = (characterId: string, alignmentKey: string): Promise<Result<unknown>> =>
  callOperation('set_alignment', { character_id: characterId, alignment_key: alignmentKey }, schemas.acknowledged)

export const setDeity = (characterId: string, deityKey: string): Promise<Result<unknown>> =>
  callOperation('set_deity', { character_id: characterId, deity_key: deityKey }, schemas.acknowledged)

export const getBiography = (characterId: string): Promise<Result<schemas.Biography>> =>
  callOperation('get_biography', { character_id: characterId }, schemas.biography)

export const setBiography = (characterId: string, fields: Record<string, string | number>): Promise<Result<unknown>> =>
  callOperation('set_biography', { character_id: characterId, ...fields }, schemas.acknowledged)

// Levels and stats
export const addClassLevel = (characterId: string, classKey: string, levels: number): Promise<Result<unknown>> =>
  callOperation('add_class_level', { character_id: characterId, class_key: classKey, levels }, schemas.acknowledged)

export const setAllAbilityScores = (
  characterId: string,
  scores: Record<string, number>,
): Promise<Result<unknown>> =>
  callOperation('set_all_ability_scores', { character_id: characterId, scores }, schemas.acknowledged)

export const rollStats = (characterId: string): Promise<Result<unknown>> =>
  callOperation('roll_stats', { character_id: characterId }, schemas.acknowledged)

export const getCharacterDetails = (characterId: string): Promise<Result<schemas.CharacterDetails>> =>
  callOperation('get_character_details', { character_id: characterId }, schemas.characterDetails)

// Skills
export const getSkillSummary = (characterId: string): Promise<Result<schemas.SkillSummary>> =>
  callOperation('get_skill_summary', { character_id: characterId }, schemas.skillSummary)

export const investSkillPoints = (
  characterId: string,
  skillKey: string,
  points: number,
  levelIndex: number,
): Promise<Result<unknown>> =>
  callOperation(
    'invest_skill_points',
    { character_id: characterId, skill_key: skillKey, points, level_index: levelIndex },
    schemas.acknowledged,
  )

// Abilities
export const listAbilityCategories = (characterId: string): Promise<Result<schemas.AbilityCategory[]>> =>
  callOperation('list_ability_categories', { character_id: characterId }, list(schemas.abilityCategory))

export const listAbilities = (characterId: string, categoryKey: string): Promise<Result<schemas.AvailableAbility[]>> =>
  callOperation(
    'list_abilities',
    { character_id: characterId, category_key: categoryKey },
    list(schemas.availableAbility),
  )

export const addAbility = (
  characterId: string,
  categoryKey: string,
  abilityKey: string,
  choice: string[],
): Promise<Result<schemas.AbilityAdded>> =>
  callOperation(
    'add_ability',
    { character_id: characterId, category_key: categoryKey, ability_key: abilityKey, choice },
    schemas.abilityAdded,
  )

export const removeAbility = (
  characterId: string,
  categoryKey: string,
  abilityKey: string,
): Promise<Result<unknown>> =>
  callOperation(
    'remove_ability',
    { character_id: characterId, category_key: categoryKey, ability_key: abilityKey },
    schemas.acknowledged,
  )

// Equipment
export const getInventory = (characterId: string): Promise<Result<schemas.Inventory>> =>
  callOperation('get_inventory', { character_id: characterId }, schemas.inventory)

export const buyEquipment = (characterId: string, equipmentKey: string, quantity: number): Promise<Result<unknown>> =>
  callOperation(
    'buy_equipment',
    { character_id: characterId, equipment_key: equipmentKey, quantity },
    schemas.acknowledged,
  )

export const sellEquipment = (characterId: string, equipmentKey: string, quantity: number): Promise<Result<unknown>> =>
  callOperation(
    'sell_equipment',
    { character_id: characterId, equipment_key: equipmentKey, quantity },
    schemas.acknowledged,
  )

export const setFunds = (characterId: string, amount: number): Promise<Result<unknown>> =>
  callOperation('set_funds', { character_id: characterId, amount }, schemas.acknowledged)

export const getEquippedItems = (characterId: string): Promise<Result<schemas.EquippedSlot[]>> =>
  callOperation('get_equipped_items', { character_id: characterId }, list(schemas.equippedSlot))

export const equipItem = (characterId: string, equipmentKey: string, slot: string): Promise<Result<unknown>> =>
  callOperation(
    'equip_item',
    { character_id: characterId, equipment_key: equipmentKey, slot },
    schemas.acknowledged,
  )

export const unequipItem = (characterId: string, slot: string): Promise<Result<unknown>> =>
  callOperation('unequip_item', { character_id: characterId, slot }, schemas.acknowledged)

// Spells
export const getAvailableSpells = (characterId: string): Promise<Result<schemas.SpellEntry[]>> =>
  callOperation('get_available_spells', { character_id: characterId }, list(schemas.spellEntry))

export const getKnownSpells = (characterId: string): Promise<Result<schemas.SpellEntry[]>> =>
  callOperation('get_known_spells', { character_id: characterId }, list(schemas.spellEntry))

export const addKnownSpell = (characterId: string, spellName: string): Promise<Result<unknown>> =>
  callOperation('add_known_spell', { character_id: characterId, spell_name: spellName }, schemas.acknowledged)

export const getPreparedSpells = (characterId: string): Promise<Result<schemas.SpellEntry[]>> =>
  callOperation('get_prepared_spells', { character_id: characterId }, list(schemas.spellEntry))

export const addPreparedSpell = (characterId: string, spellName: string): Promise<Result<unknown>> =>
  callOperation('add_prepared_spell', { character_id: characterId, spell_name: spellName }, schemas.acknowledged)

// Sheet and housekeeping
export const getCharacterSheet = (characterId: string, format: string): Promise<Result<schemas.RenderedSheet>> =>
  callOperation('get_character_sheet', { character_id: characterId, format }, schemas.renderedSheet)

export const getTodoList = (characterId: string): Promise<Result<schemas.TodoList>> =>
  callOperation('get_todo_list', { character_id: characterId }, schemas.todoList)

export const isDirty = (characterId: string): Promise<Result<{ dirty: boolean }>> =>
  callOperation('is_dirty', { character_id: characterId }, schemas.dirtyState)

// Choices
export const getPendingChoices = (): Promise<Result<schemas.PendingChoice[]>> =>
  callOperation('get_pending_choices', {}, list(schemas.pendingChoice))

export const resolveChoice = (choiceId: string, selections: string[]): Promise<Result<unknown>> =>
  callOperation('resolve_choice', { choice_id: choiceId, selections }, schemas.acknowledged)
