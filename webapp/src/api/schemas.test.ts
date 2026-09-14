import { describe, expect, it } from 'vitest'
import { z } from 'zod'

import abilitiesFixture from './fixtures/abilities.json'
import abilityCategoriesFixture from './fixtures/ability_categories.json'
import biographyFixture from './fixtures/biography.json'
import characterFixture from './fixtures/character.json'
import characterCreatedFixture from './fixtures/character_created.json'
import characterDetailsFixture from './fixtures/character_details.json'
import dirtyFixture from './fixtures/dirty.json'
import equippedFixture from './fixtures/equipped.json'
import gameModesFixture from './fixtures/game_modes.json'
import inventoryFixture from './fixtures/inventory.json'
import knownSpellsFixture from './fixtures/known_spells.json'
import pendingChoicesFixture from './fixtures/pending_choices.json'
import skillSummaryFixture from './fixtures/skill_summary.json'
import sourcesFixture from './fixtures/sources.json'
import sourcesLoadedFixture from './fixtures/sources_loaded.json'
import todoFixture from './fixtures/todo.json'
import * as schemas from './schemas'

/**
 * Every fixture was captured from a running PCGen server, so a schema that no
 * longer matches means the server changed shape under the interface.
 */
const readsItsOwnServer: ReadonlyArray<readonly [string, z.ZodType, unknown]> = [
  ['list_game_modes', z.array(schemas.gameMode), gameModesFixture],
  ['list_sources', z.array(schemas.namedEntry), sourcesFixture],
  ['load_sources', schemas.sourcesLoaded, sourcesLoadedFixture],
  ['create_character', schemas.characterCreated, characterCreatedFixture],
  ['get_character', schemas.characterSummary, characterFixture],
  ['get_character_details', schemas.characterDetails, characterDetailsFixture],
  ['get_skill_summary', schemas.skillSummary, skillSummaryFixture],
  ['list_ability_categories', z.array(schemas.abilityCategory), abilityCategoriesFixture],
  ['list_abilities', z.array(schemas.availableAbility), abilitiesFixture],
  ['get_inventory', schemas.inventory, inventoryFixture],
  ['get_equipped_items', z.array(schemas.equippedSlot), equippedFixture],
  ['get_known_spells', z.array(schemas.spellEntry), knownSpellsFixture],
  ['get_biography', schemas.biography, biographyFixture],
  ['get_todo_list', schemas.todoList, todoFixture],
  ['get_pending_choices', z.array(schemas.pendingChoice), pendingChoicesFixture],
  ['is_dirty', schemas.dirtyState, dirtyFixture],
]

describe('the schemas the interface reads answers with', () => {
  it.each(readsItsOwnServer)('reads what %s really answered', (_name, schema, fixture) => {
    const read = schema.safeParse(fixture)

    expect(read.success ? null : read.error.issues).toBeNull()
  })
})
