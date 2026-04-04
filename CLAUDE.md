# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

PCGen — Java desktop application for creating/managing RPG player characters (D&D 3.5/4.0/5.0, Pathfinder 1e, Starfinder). This is the SebQuenet fork of PCGen/pcgen, which adds an MCP server for AI-driven character creation and homebrew data (Celmae/Udaeus).

- **Java**: 25 (Temurin), auto-resolved via foojay-resolver-convention
- **Build**: Gradle wrapper (`./gradlew`), always use the wrapper
- **UI**: JavaFX (gui2 = Swing-era, gui3 = FXML-based)
- **Version**: defined in `gradle.properties`

## Essential Commands

```bash
./gradlew build              # Full build
./gradlew compileJava        # Compile only
./gradlew run                # Launch GUI
./gradlew runMcp             # Launch MCP server (headless, stdio)

# Testing
./gradlew test               # Fast unit tests (code/src/utest/)
./gradlew itest              # Integration tests (code/src/itest/)
./gradlew slowtest           # Slow tests (code/src/test/)
./gradlew datatest           # Data loading tests
./gradlew pfinttest          # Pathfinder integration tests
./gradlew inttest            # Character integration tests

# Code quality
./gradlew allReports         # Checkstyle + PMD + SpotBugs
./gradlew testCoverage       # Jacoco coverage report

# Distribution
./gradlew qbuild             # Quick dev build to output/
./gradlew buildDist          # Distribution zips
./gradlew jpackage           # Native installer via jpackage
```

Run a single test class: `./gradlew test --tests "pcgen.some.TestClass"`

## Code Style Rules (Checkstyle-enforced)

- **Never use `System.exit()`** — use `pcgen.util.GracefulExit.exit()` instead
- **Left curly brace on new line** (Allman style, not K&R)
- Max line length: 201 characters
- No star imports, no unused imports
- Newline required at end of file
- Braces always required

Config: `code/standards/checkstyle.xml`, `code/standards/ruleset.xml`, `code/standards/spotbugs_ignore.xml`

## Architecture

### Entry Points
- **GUI**: `pcgen.system.Main` — launches JavaFX application, validates runtime environment
- **MCP Server**: `pcgen.mcp.McpMain` — headless mode, loads data and exposes tools over stdio

### Source Layout
Production code: `code/src/java/pcgen/`

| Package | Role |
|---------|------|
| `cdom` | Core Data Object Model — domain model, facets, references, formulas |
| `core` | Game entities: PlayerCharacter, Ability, Equipment, Spell, Race, PCClass, etc. |
| `facade` | Interfaces decoupling UI from core (CharacterFacade, DataSetFacade) |
| `gui2` | JavaFX GUI (main frame, tabs, dialogs) |
| `gui3` | Newer FXML-based GUI components |
| `io` | Character save/load (PCGVer2Creator/Parser), export (FreeMarker templates) |
| `mcp` | MCP server — tools, resources, session management for AI agents |
| `persistence` | LST file loading, campaign/game mode loaders |
| `system` | Application bootstrap, configuration, facade factory |
| `output` | Export actors and channels |

### Test Source Sets (nonstandard directories)
- `code/src/utest/` — fast unit tests (`./gradlew test`)
- `code/src/itest/` — integration tests (`./gradlew itest`)
- `code/src/test/` — slow tests (`./gradlew slowtest`)
- `code/src/testcommon/` — shared test fixtures (available to all test sets)
- `code/src/testResources/` — test data files

### MCP Server (`pcgen.mcp`)
Exposes PCGen as an MCP server for AI-driven character building. Key classes:
- `McpMain` — entry point, headless initialization
- `McpServerBuilder` — registers all tools and resources
- `McpSessionManager` — manages loaded sources, open characters, pending choices
- `tools/` — 16 tool classes (source loading, character lifecycle, abilities, skills, spells, equipment, etc.)

MCP config for Claude Code: `.mcp.json`
Known issues: `mcp-to-fix.md`

### Runtime File Layout
These directories are validated at startup (`Main.validateEnvironment()`) — renames/deletions break the app:
- `data/` — game data files (35e, 3e, 5e, pathfinder, starfinder, homebrew)
- `outputsheets/` — character sheet templates
- `system/` — game system configuration
- `preview/` — character preview templates

### Companion Libraries
- `PCGen-base/` — separate Gradle build for base utilities
- `PCGen-Formula/` — separate Gradle build for formula system

## Gotchas

- Java 25 and JavaFX are tightly coupled across build.gradle, run/test/compile tasks, and CI — changing one requires adjusting all of them.
- Plugins are built as separate JARs from compiled classes (`code/gradle/plugins.gradle`); main jar depends on `jarAllPlugins`.
- Some maven repos use `allowInsecureProtocol = true` — do not change without coordination.
- Build logic is split across `code/gradle/*.gradle` files (autobuild, distribution, reporting, release, plugins).

## Importing Creature Data from PCFinder

The `scripts/` directory contains a two-step pipeline to import Pathfinder bestiary creatures into PCGen LST format, using pcfinder-csr as the primary data source and d20pfsrd.com for supplemental fields.

### Prerequisites

- Python 3 with `requests` and `beautifulsoup4` (`pip install requests beautifulsoup4`)
- The pcfinder-csr repo cloned alongside this one (expects `../pcfinder/apps/pcfinder-csr/static-data/creatures.json`)

### Step 1 — Scrape d20pfsrd.com (optional enrichment)

Scrapes feats, skills, languages, and spell-like abilities that pcfinder-csr doesn't provide:

```bash
python3 scripts/scrape_d20pfsrd.py \
    --source ../pcfinder/apps/pcfinder-csr/static-data/creatures.json \
    --bestiary 5 6 \
    --output scripts/d20pfsrd_cache.json
```

Options: `--resume` (continue interrupted scrape), `--test "Creature Name"` (scrape one creature), `--limit N` (scrape first N only). The URL index is cached in `scripts/d20pfsrd_index.json` to avoid re-crawling type listing pages. Respects a 1.5s delay between requests.

### Step 2 — Generate LST files

Generates PCGen race, kit, and ability LST files from pcfinder-csr data, optionally enriched with the d20pfsrd cache:

```bash
python3 scripts/generate_bestiary.py \
    --source ../pcfinder/apps/pcfinder-csr/static-data/creatures.json \
    --bestiary 5 6 \
    --cache scripts/d20pfsrd_cache.json
```

Output goes to `data/pathfinder/paizo/roleplaying_game/bestiary_{N}/` (races, kits, abilities). The PCC files referencing these LST files must already exist.

### What each script produces

| Script | Input | Output |
|--------|-------|--------|
| `scrape_d20pfsrd.py` | creatures.json + d20pfsrd.com | `d20pfsrd_cache.json` (feats, skills, languages, SLAs) |
| `generate_bestiary.py` | creatures.json + cache | `b{N}_races.lst`, `b{N}_kits_race.lst`, `b{N}_abilities_race.lst` |

### After generation

Run `./gradlew test` to verify the generated data loads without errors.

## Issue Tracker

Jira: https://pcgenorg.atlassian.net (projects: CODE, DATA, etc.)
