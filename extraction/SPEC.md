# Villain Codex extraction — per-group agent spec

You process ONE group of the Pathfinder *Villain Codex*. Produce two reference
files. Do NOT write or edit any `.lst` / `.pcc` file. Do NOT invent content — if a
field cannot be read from the source text, write `(unclear)`.

## Inputs
- Group raw text: `extraction/raw/NN_<group>.txt` (whole group: New Rules + NPCs + Encounters).
- New Rules only: `extraction/newrules/NN_<group>.txt`.
- Page numbers printed at the bottom of text blocks are BOOK pages — cite those.
- Read the pilot outputs to match format EXACTLY:
  - NPC format: `extraction/npcs/04_corrupt_guard.md`
  - The pilot crunch lived in the `villain_codex` LST files; your crunch output is a
    structured inventory (below), not LST.

## Output 1 — NPC reference → `extraction/npcs/NN_<group>.md`
Same structure as the pilot: a title + source line; a roster table
`| # | NPC | CR | Build | Alignment / Type | Page |`; then one `### N. Name — CR X`
entry per NPC with build line, alignment, role, and notable mechanics (naming any
new-rules elements it uses). Read each stat block carefully; never guess class,
level, or alignment — use `(unclear)` if not legible in the text.

## Output 2 — Crunch inventory → `extraction/crunch/NN_<group>.md`
List EVERY element defined in the group's *New Rules* section, with all mechanics
needed to later author PCGen LST. One section per element, grouped by type:

- **Feats**: name; type (Combat/Teamwork/General/Metamagic/etc.); prerequisites
  (verbatim); full benefit text; source page.
- **Spells**: name; school + descriptors; full class/level list (e.g. bard 2,
  wizard 3); casting time; components; range; target/area/effect; duration; saving
  throw; spell resistance; full description; source page.
- **Mundane equipment**: name; price; weight; full description; source page.
- **Magic items**: name; aura; CL; slot; price; weight; full effect text;
  Construction Requirements + cost; source page.
- **Archetypes**: name; base class; for each level, which class feature is
  replaced/altered; full text of each new/altered feature; source page.
- **Class options** (rogue talents, rage powers, discoveries, oracle mysteries &
  revelations, hexes, etc.): name; category (what it is / which class); prerequisites;
  full effect text; source page.
- **Occult rituals**: name; school + level; casting time; components; skill checks;
  range; target; duration; saving throw; spell resistance; backlash; failure;
  full effect; source page.

If the group has no elements of a type, omit that type. Elements marked `*` in the
book are new to this book — those are exactly what to capture. Superscripts like
`APG`, `UM`, `ACG` mean the element is from another book — do NOT capture those as
new (only note them as references where relevant to an NPC).

Write only the two markdown files. Report a one-line summary of counts per type.
