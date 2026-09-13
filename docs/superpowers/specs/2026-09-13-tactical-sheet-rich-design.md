# Rich tactical sheets

Date: 2026-09-13
Supersedes the data model of `2026-09-13-tactical-sheet-design.md`; keeps its goal.

## Why

The first tactical sheet stores three strings per line — trigger, actions, note — grouped
into titled sections. That shape cannot hold what a player actually wants in front of them
during a fight. The reference the feature is measured against is a hand-built HTML sheet for
Miros holding: a vitals band, an attack table that switches between target kinds, tickable
per-day resource counters, a filterable spell repertoire, stat blocks for summoned
creatures, item cards, non-stacking rules, and a round-by-round rotation.

Two goals, settled in one pass rather than one after the other:

- richer data, so the sheet can hold all of the above;
- a rendering that matches the reference, both inside PCGen and in the exported file.

## Decisions

1. **One HTML renderer, two consumers.** The sheet is rendered as HTML and shown inside
   PCGen in a `WebView`, the way the character sheet tab already works
   (`CharacterSheetPanel`). The same renderer feeds the export. No second presentation
   built out of Swing widgets.
2. **Reference or override, per field.** A block may point at something the character owns
   and inherit its computed values, or carry written values. References resolve at render
   time, so the sheet follows a level-up without being rewritten.
3. **Session state is live and persisted** in the `.pcg`: damage taken and resources spent.
   PCGen tracks no play state today — `PlayerCharacter` has neither current hit points nor
   damage — so this is the first. Ticking a box marks the character dirty.
4. **The source text is the source of truth.** The plan is authored, stored and edited as
   text. The object model is a parse result and is never serialised back.
5. **The sheet is an output sheet whose template is given the object model.** Output sheets
   otherwise reach PCGen only through flat token strings (`base.xml.ftl:246-253` reads
   `pcstring('TACTIC.${section}.${entry}.TRIGGER')`), and six block types through that
   keyhole would mean about thirty token paths and index arithmetic in the template. So
   `FreeMarkerExportHandler` puts one more entry in the data model — `tactics`, built by
   `TacticalOutputModel` — beside the `pcstring` and `pcvar` helpers it already provides.
   The sheet is then an ordinary template under `outputsheets/tactics/`, rendered by the
   existing handler: the plan comes from `tactics`, and everything the character already
   knows comes from `pcstring`. No new export plumbing, and no departure from convention.

## Data model

`pcgen.core.tactics`, all records, all immutable, validating in their compact constructors.

```java
record TacticalSheet(List<TacticalSection> sections)
record TacticalSection(String title, List<TacticalBlock> blocks)

sealed interface TacticalBlock
    permits TacticalStep, TacticalAttack, TacticalResource,
            TacticalCreature, TacticalSpellList, TacticalNote

record TacticalStep(String trigger, String actions, String note)
record TacticalAttack(TacticalSubject subject, Optional<String> toHit,
                      Optional<String> damage, Optional<String> critical,
                      List<TacticalVariant> variants, String note)
record TacticalVariant(String label, String effect)
record TacticalResource(String label, TacticalSubject maximum, String action)
record TacticalCreature(String name, String source, String duration, List<TacticalRow> rows)
record TacticalRow(String label, String content)
record TacticalSpellList(SpellSource source, List<TacticalTag> tags)
record TacticalTag(String spellName, List<String> tags)
record TacticalNote(String title, String prose)
```

What a block points at is itself a two-case union, so an unresolvable literal cannot be
confused with a reference:

```java
sealed interface TacticalSubject permits TacticalReference, TacticalLiteral
record TacticalReference(ReferenceKind kind, String key) implements TacticalSubject
record TacticalLiteral(String text) implements TacticalSubject

enum ReferenceKind { WEAPON, VAR }
enum SpellSource { PREPARED, KNOWN }
```

`ReferenceKind` lists only what PCGen can actually answer. `ABILITY`, `EQUIPMENT` and
`SPELL` were dropped once it became clear that an ability's uses per day is not a first
class field in PCGen but a variable — so `@var(name)` covers it, and a kind pointing at
something PCGen has no concept of would have resolved to nothing. A spell repertoire is
reached through `SpellSource` rather than a reference, so spells lose nothing.

`TacticalCreature.rows` is deliberately a list of labelled rows rather than a fixed set of
fields. A stat block's rows differ per creature; a free label and free content render as the
reference does — a label column and a content column — and the model will not need changing
the day a creature wants a row nobody anticipated.

Session state is a separate structure, because a plan is not a counter:

```java
record TacticalSessionState(int damageTaken, Map<String, Integer> resourcesSpent)
```

`resourcesSpent` is keyed by the resource's label. A duplicate label is a validation error
in the parser, so the key is unambiguous.

## Source syntax

Line-oriented. `key: value`, `|` between fields, two spaces of indentation for sub-lines,
`##` for a section heading, `@` for something the character owns. Blank lines are ignored
outside indented prose. A sheet must open with a section heading.

```
sheet     := section+
section   := "## " title NL (block | blank)*
block     := note | resource | attack | step | creature | spells
```

| Line | Shape |
|---|---|
| section | `## <title>` |
| note | `note: <title>` then one or more `  <prose>` lines |
| resource | `resource: <label> \| <maximum> \| <action>` |
| attack, referenced | `attack: @weapon(<name>)` |
| attack, written | `attack: <name> \| <to hit> \| <damage> \| <crit>` |
| step | `step: <trigger>` then `  do: <actions>` |
| creature | `creature: <name> \| <source> \| <duration>` then `  row:` lines |
| spells | `spells: @prepared` or `spells: @known` then `  tag:` lines |

Sub-lines, each indented two spaces and belonging to the block above:

| Sub-line | Belongs to | Shape |
|---|---|---|
| `target:` | attack | `  target: <label> \| <effect>` |
| `row:` | creature | `  row: <label> \| <content>` |
| `tag:` | spells | `  tag: <spell> \| <tag>, <tag>` |
| `do:` | step | `  do: <actions>` |
| `note:` | attack, step | `  note: <text>` |

References, usable wherever the table above shows `@`: `@weapon(<name>)` and
`@var(<name>)`, plus the two bare forms `@prepared` and `@known` for a spell list.

A `note:` at column zero opens a note block and takes indented prose; a `note:` indented two
spaces annotates the block above it. Indentation is what tells them apart.

Optional fields, all of which hold the empty string when the writer leaves them out: a
resource's action, a creature's source and duration, an attack's or a step's `note:`. A
resource written `resource: Pouvoir mythique | 11` is therefore valid. Required fields are
the ones the failure list below names.

Worked example, from the Miros sheet:

```
## Avant le combat

note: Contexte
  État de référence sans buff. Seuls l'Infusion d'armes du Parangon
  et Arme sacrée sont appliqués, tous deux au Sceptre de Timéon.

resource: Pouvoir mythique | 11 | immédiate
resource: Canalisation | @var(ChannelUses) | simple

attack: @weapon(Sceptre de Timéon)
  target: créature Mauvaise | +2d6 (sainte)
  target: extérieur Mauvais | +2 altération, +2d6
  note: ses pouvoirs ne fonctionnent qu'entre des mains Bonnes

## Pièges à éviter

step: Adversaire qui vise la CA de contact
  do: CA de contact 14 — les attaques de contact et les rayons passent
  note: CA totale 25, pris au dépourvu 23

## Invocations

creature: Archonte molosse | Convocation de monstre IV | 7 rounds
  row: Déf | CA 19 (contact 10, DFD 19) · PV 51
  row: RD/RM | RD 10/épique · RM 15 · immunité électricité et pétrification
  row: Att | Épée à 2 mains de maître +11/+6 (2d6+9) et morsure +5 (1d8+4)

## Sorts

spells: @prepared
  tag: Châtiment sacré | dégâts, registre du Bien
  tag: Bénédiction de ferveur | buff, mythique
```

### Parsing contract

Errors are typed values, not exceptions:

```java
sealed interface TacticalParseResult permits TacticalParseSuccess, TacticalParseFailure
record TacticalParseSuccess(TacticalSheet sheet) implements TacticalParseResult
record TacticalParseFailure(List<TacticalParseError> errors) implements TacticalParseResult
record TacticalParseError(int line, String message)
```

`line` is one-based, so the editor can point at it. Every error names a line. Parsing
reports every error it finds rather than stopping at the first.

Conditions that must fail: text not opening with a section heading; an unknown key at column
zero; a sub-line whose key does not belong to the block above; a sub-line with no block
above; a block missing a required field (`step` without `do:`, `resource` without a maximum,
`creature` without a row, `attack` written form with fewer than four fields); a duplicate
resource label; a malformed reference.

## Storage

The plan goes in on one line, the text encoded with `EntityEncoder.encode`, which already
escapes `\ \n \r \f : | [ ] &` — precisely the characters the syntax uses
(`EntityEncoder.java:34`).

In a single line field the two characters `\n` stand for a line break, since a field cannot
hold a real one. The writer escapes on the way out and the parser unescapes on the way in,
so a note holding several lines survives a save.

| Tag | Content |
|---|---|
| `TACTICALPLAN` | the whole source text, entity-encoded |
| `TACTICALDAMAGE` | damage taken, an integer |
| `TACTICALSPENT` | `<resource label>\|<count>`, one line per entamed resource |

Two facets hold it, each with one job, both declared in `applicationContext.xml` —
a facet absent from that file does not survive `cloneForExport`:

- `TacticalPlanFacet` keeps the source text (renamed from `TacticalSheetFacet`, which kept
  a `TacticalSheet`: a facet named for the sheet but holding a string would surprise its
  next reader);
- `TacticalSessionFacet` keeps the `TacticalSessionState`.

Parsing happens at the point of use. A few hundred lines of text is cheap enough that
caching a parse result is not worth the invalidation it would demand.

### Migration

`TacticalPlanWriter` writes a sheet back out as source text. Nothing writes the model back
during ordinary editing — the text is the source of truth — but the migration needs one
direction, and the writer also gives the parser a round trip to be tested against.

`PCGVer2Parser` keeps reading the current tags — `TACTICALSECTION`, `TACTICALENTRY`,
`TACTICALTRIGGER`, `TACTICALACTIONS`, `TACTICALNOTE` — and turns them into source text:
one `## ` heading per section, one `step:` block per entry. `PCGVer2Creator` writes only the
new tags. A character saved by this version therefore will not load in an earlier PCGen;
the feature exists nowhere but this fork, so nothing outside it is affected.

`characters/Miros.pcg` carries the old tags and is the migration's first test subject.

## Reference resolution

A `TacticalResolver` takes a `PlayerCharacter` and answers a `TacticalReference` with a
resolved value, going through the export token layer that already computes these values
rather than recomputing them:

| Kind | Resolves to |
|---|---|
| `WEAPON` | to-hit, damage and critical of the equipped weapon of that name, read through `WeaponToken` |
| `VAR` | the value of a variable the character defines, or nothing when it defines no such variable |

A spell repertoire block resolves separately, through `TacticalResolver.spells(SpellSource)`,
off `CharacterDisplay.getCharacterSpells` rather than the export tokens: level, name, book
and how many times each spell is held. Save DCs are not resolved per spell — the vitals
band carries them per level, which is where a player reads them.

A resolution that fails does not drop the block. The renderer shows the block with a
warning naming what could not be found, so a weapon sold or an ability lost is visible
rather than silent.

Written values win over resolved ones: an `Optional` field that is present is used as is.

## Rendering

`TacticalSheetRenderer` is a pure function — plan, character, session state in, HTML out. It
runs a FreeMarker template with the object model as its data model. FreeMarker is already on
the classpath.

The template, its stylesheet and its script live in `outputsheets/tactics/`. Style and
script are inlined into the produced HTML: `WebView.loadContent` has no base URL, and the
exported file has to open anywhere.

The renderer produces a **fragment** — body plus inlined style and script — and a page shell
wraps it for the two standalone cases. Consumers:

- the Tactical tab, through `WebView`, by the same `character.export` call the character
  sheet tab makes;
- the export, by naming `outputsheets/tactics/tactical.htm.ftl` as the output sheet —
  including from the command line, `pcgen -c <character> -E <that template> -o <file>`.

No `TACTIC.HTML` token is needed after all: the sheet is a template, so a template is what
names it. The existing `TACTIC.n.m.TRIGGER` paths stay. The PDF and compact sheets keep their flat
trigger/actions/note table over `step` blocks, since a PDF cannot render the rich sheet.
Only the HTML renderings gain it.

### Visual language

Taken from the reference sheet: dark ground, a semantic colour per kind of information
(attack, damage, defence, mythic, holy, buff, warning), `Cinzel` for headings and
`Source Sans 3` for text with a real fallback stack, collapsible sections, a vitals band at
the top. Blocks render as the reference renders them: attacks as a table with a target-kind
switch, resources as a row of tickable pips, creatures as a labelled-row stat card, spells
as a filterable list, notes as prose cards.

The vitals band is not a block and is never authored. The renderer computes it from the
character — hit points, armour class with touch and flat-footed, initiative, speed, saves,
base attack and manoeuvre numbers, spell save DCs — so it is present on every sheet and
correct without anyone maintaining it.

### Session state and the bridge

In the `WebView`, `executeScript("window")` then `setMember("pcgen", bridge)` exposes an
object with two methods, `setDamage(int)` and `spend(String label, int count)`, which write
through the facade and mark the character dirty.

The same page opened in an ordinary browser finds no `window.pcgen` and keeps its state in
the page only. One page, two behaviours, by feature detection — not two builds.

### Escaping

The sheet's text is written by an AI agent and is untrusted. Unescaped, a `<script>` slipped
into a line would run with a bridge open to Java. The template opens with
`<#ftl output_format="HTML">`, so FreeMarker escapes every interpolation and none can be
forgotten; a render test asserts that an authored `<script>` comes out escaped.

The sheet's own wording — column headings, "normal target", the empty and error messages —
comes from `LanguageBundle` through the model's `labels` map, so the page speaks the
user's language. The plan's text is the writer's and is shown as written.

## The Tactical tab

A `JSplitPane`: the source editor on the left, a `JFXPanel` holding a `WebView` on the
right, following `CharacterSheetPanel.java:53` and `:171`.

Editing the plan and playing the session are two different acts. The left pane changes the
plan: typing re-parses, the right pane keeps its last valid render, and a banner names the
offending line while the text is invalid. The right pane plays the session: clicking a pip
or the damage control writes through the bridge.

The tab replaces the current section list and entry table.

## MCP tools

Payloads change; the count grows by one.

| Tool | Change |
|---|---|
| `get_tactical_sheet` | returns the source text |
| `set_tactical_sheet` | takes the source text, refuses naming the offending line |
| `clear_tactical_sheet` | unchanged |
| `list_tactical_references` | new: equipped weapons, abilities with uses per day, prepared spells |

`list_tactical_references` is not a convenience. Without it the agent guesses the names
behind `@weapon(...)` and the reference breaks at render time.

No render tool: the agent has no use for the HTML.

## Testing

The parser and the renderer are pure functions, and carry the coverage.

- **Parser**: text to model, one test per block type; every failure condition listed above,
  each asserting the reported line number; the Miros sheet as a whole.
- **Migration**: a `.pcg` holding the old tags parses into the equivalent source text.
- **Resolver**: against a real `PlayerCharacter` from the existing test harness, never a
  double — a weapon resolves, a sold weapon reports a failure.
- **Renderer**: model plus character plus state in, asserted HTML out, including that an
  authored `<script>` comes out escaped.
- **Round trip**: text parses, and the stored-then-reloaded text is identical.

The bridge and the `WebView` are the untestable edge and stay as thin as possible for that
reason.

## Out of scope

- Rendering the rich sheet in PDF.
- Writing session state back from the exported file — a local file has no server for it.
- A tool letting the agent set session state; the human ticks the boxes.
- Tracking current hit points anywhere else in PCGen.

## Build order

1. Model and parser.
2. Storage, session state, migration of the old tags.
3. Reference resolution.
4. The template, its data model, its stylesheet and its script.
5. ~~Export token~~ — dropped: the sheet is a template, so nothing needs a token.
6. The split tab and the JavaScript bridge.
7. MCP tools.
