# Tactical Sheet — design

Date: 2026-09-13
Status: implemented

## Goal

Give every character a *tactical sheet*: what to do, round by round and
situation by situation. The content is written by an AI agent through the MCP
server, stored in the character, editable in the PCGen GUI, and rendered in the
exported character sheets for Pathfinder 1e.

## Decisions taken during brainstorming

| Question | Decision |
|---|---|
| Who calls the AI | Claude through the existing MCP server. PCGen itself never calls an LLM API: no HTTP client, no API key, no network dependency. |
| Content shape | Structured: ordered sections, each holding ordered entries of `trigger -> actions`. |
| GUI placement | A new top-level tab, next to Summary / Skills / Spells. Manual editing allowed. |
| Export scope | Every Pathfinder 1e sheet (`outputsheets/d20/fantasy`) that shows character-wide information — the standard and condensed HTML and PDF sheets. Spellbook and statblock sheets are left alone. |

Not in scope: recording the character level at generation time to warn about a
stale sheet. It can be added later without changing the storage format.

## Model

Package `pcgen.core.tactics`, immutable records:

- `TacticalEntry(String trigger, String actions, String note)` — `trigger` is
  the circumstance in plain words ("Round 1", "enemy at range", "HP below 12"),
  `actions` is what the character does, `note` is optional extra detail.
- `TacticalSection(String title, List<TacticalEntry> entries)` — a titled group
  of entries ("Opening", "Rounds 1-2", "Emergency").
- `TacticalSheet(List<TacticalSection> sections)` — the whole sheet.

The trigger is free text rather than a round number, so the same type covers
both "Round 1" and "if the fight goes badly".

Construction validates: no null or blank title, trigger or actions; a section
with no entries is rejected. Invalid input fails at construction rather than
propagating an empty sheet.

## Storage

`TacticalSheetFacet extends AbstractItemFacet<CharID, TacticalSheet>`, following
`NoteItemFacet`. Read access through `CharacterDisplay`, write access through
`PlayerCharacter`.

## Character file (.pcg)

One line per section and one per entry, the entry naming the index of its
section, so sections keep their order and their entries stay attached to them.
The note is written only when there is one. Five new tags in `IOConstants`:

```
TACTICALSECTION:<title>
TACTICALENTRY:<section index>|TACTICALTRIGGER:<trigger>|TACTICALACTIONS:<actions>|TACTICALNOTE:<note>
```

Values are encoded with `EntityEncoder`, as the notes lines already are.
`PCGVer2Creator` writes them, `PCGVer2Parser` reads them. `PCGVer2Parser`
dispatches on a cache keyed by tag name and only reads the tags it knows about,
so a file carrying these tags still loads in upstream PCGen, which drops the
tactical sheet. A round-trip test covers write then read.

## MCP tools

New `pcgen.mcp.tools.TacticalSheetTools`, registered in `McpServerBuilder`:

- `set_tactical_sheet` — replaces the whole sheet. Takes the sections and their
  entries as structured JSON. Rejects malformed input with an explicit error
  instead of storing a partial sheet.
- `get_tactical_sheet` — returns the current sheet.
- `clear_tactical_sheet` — removes it.

The agent already has `get_character_details` to learn the character's attacks,
spells and hit points before writing the sheet.

## GUI

`pcgen.gui2.tabs.TacticalInfoTab implements CharacterInfoTab`, registered in
`InfoTabbedPane` after `DescriptionInfoTab`, with a new `Tab.TACTICAL` entry and
its labels in `LanguageBundle.properties` and `LanguageBundle_fr.properties`.
Sections are listed on the left; the lines of the selected section are edited on
the right in a `TacticalSectionTableModel` (When / Do / Note). A character with
no tactical sheet sees a sentence saying how to get one instead of an empty
table.

The tab talks to a `TacticalSheetFacade` (`pcgen.facade.core`) implemented by
`pcgen.gui2.facade.TacticalSheetFacadeImpl`, so the GUI never touches
`PlayerCharacter` directly — the pattern `DescriptionFacade` already follows.
The facade addresses sections by position rather than by value: two sections may
legitimately hold the same title and the same lines, and editing one must not
silently edit the other.

## Export

`plugin.exporttokens.TacticToken` (token name `TACTIC`), modelled on
`NoteToken`:

- `TACTIC.COUNT` — sections in the sheet
- `TACTIC.<section>.TITLE` and `TACTIC.<section>.COUNT`
- `TACTIC.<section>.<entry>.TRIGGER` / `.ACTIONS` / `.NOTE`
- `TACTIC.FLAT.COUNT` and `TACTIC.FLAT.<entry>.SECTION` / `.TRIGGER` /
  `.ACTIONS` / `.NOTE` — the same sheet as one run of entries, for templates
  whose loop cannot nest one counter inside another

An index past the end of the sheet exports nothing, so a template that loops one
step too far stays silent. The token does not escape its text: `Token.isEncoded()`
is left at its default, so the export handler escapes for the output format.

Two variables let a template loop: `COUNT[TACTICS]` (sections) and
`COUNT[TACTICENTRIES]` (entries in all), through
`PCCountTacticalSectionsTermEvaluator` and `PCCountTacticalEntriesTermEvaluator`
registered in `TermEvaluatorBuilderPCVar`. A per-section entry count is not a
variable: `EvaluatorFactory` looks builders up by literal key, so a term
carrying a number cannot be registered. Templates read `TACTIC.<section>.COUNT`
instead.

Sheets that render it, all of them for Pathfinder 1e:

- `outputsheets/base.xml.ftl` gains a `<tactics>` block under `basics`, which
  every XSLT sheet reads.
- `outputsheets/d20/fantasy/pdf/common_sheet/block_tactics.xslt`, imported and
  applied by the four PDF masters: `fantasy_master_common_blocks.xslt` (which
  covers the nine `std` sheets and, through `fantasy_master_simple.xslt`, the
  nine `simple` ones), `fantasy_master_alt_largetext.xslt`,
  `fantasy_master_common_companion_box.xslt` and
  `fantasy_master_no_header.xslt`. That is 21 of the 22 PDF sheets;
  `csheet_fantasy_spell_list_only` prints spells only and is left alone, as are
  the spellbook sheets.
- `csheet_fantasy_std.htm.ftl` in FreeMarker, and `csheet_fantasy_compact.htm`
  through the older `|FOR...|` engine, which is why the flat view exists. Two
  details of that engine matter and are covered by a test: its loop bound is
  exclusive (`ExportHandler.replaceTokenForDfor` loops `while (iStart < cMax)`,
  so the sheet passes `COUNT[TACTICENTRIES]`, not `COUNT[TACTICENTRIES]-1`), and
  its last parameter must be `0`, or the loop stops at the first entry whose
  note is empty. The condition around the block is `|IIF(HASVAR:...)|`, since a
  bare `>0` comparison is read as a token name, not as arithmetic.

An empty sheet renders nothing at all — no empty heading on a character without
a tactical sheet.

## Testing

- `code/src/utest/pcgen/core/tactics/TacticalSheetTest` — model validation and
  immutability.
- `code/src/utest/pcgen/cdom/facet/TacticalSheetFacetTest` — facet behaviour,
  through the shared `AbstractItemFacetTest`.
- `code/src/utest/pcgen/mcp/tools/TacticalSheetPayloadTest` — the JSON an agent
  sends, in both directions, including what is rejected.
- `code/src/utest/pcgen/gui2/tabs/tactics/TacticalSectionTableModelTest` — what
  the table publishes when a cell is edited, and what it refuses.
- `code/src/itest/pcgen/io/BasicSaveRestoreTest` — `.pcg` round trip, including
  text carrying the separator characters of the file format.
- `code/src/test/plugin/exporttokens/TacticTokenTest` — token output against a
  character built in the test.
- `code/src/test/pcgen/core/term/TacticalCountTermTest` — the two COUNT
  variables.
- `code/src/test/pcgen/gui2/facade/TacticalSheetFacadeImplTest` — editing
  through the facade, including two sections holding the same content.
- `code/src/test/pcgen/io/TacticalSheetFreeMarkerExportTest` — the loops the
  output sheets carry, run through both export engines.
- `code/src/utest/pcgen/io/TacticalOutputSheetSyntaxTest` — the two FreeMarker
  sheets still parse.

## Delivery order

1. Model, facet, `.pcg` persistence, tests.
2. MCP tools.
3. Export tokens, `COUNT`, output sheets.
4. GUI tab and facade.

Each step stays under 500 changed lines and can be committed on its own.
