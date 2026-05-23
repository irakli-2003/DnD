# General Implementation Overview

## Purpose
This document provides a high-level overview of how the DnD homebrew system should be implemented. Detailed design notes live in sub-packages under `Documentation/implementation/`.

## Architecture Overview
- **Pattern**: Use the Page Object Model (POM) to represent each navigable page/state.
- **Modes**: Provide two entry points:
  - **CLI mode**: primary implementation focus now.
  - **UI mode**: reserved for later; keep a separate entry point.

## Page Object Model (POM)
- Each page is a class that:
  - Knows its available commands (page links).
  - Knows how to render its content and command list.
  - Can resolve a user command to the next page (or an action).
- A central controller (router) manages the current page and handles transitions.

## CLI Mode Behavior (Initial Target)
- The CLI entry point runs an infinite loop that:
  1. Displays the current page content and all available commands.
  2. Waits for user input.
  3. If input is invalid: prints `invalid command`, re-displays commands, and waits again.
  4. If input is valid: transitions to the target page or performs an action.
  5. Provides an **exit** command to terminate the loop and program.

## Campaign Flow
- Landing page asks whether the user is a Dungeon Master or a Player.
- DM path goes to **campaign selection**:
  - Choose an existing custom campaign, or
  - Create a new campaign.
- New campaign page provides:
  - **Create default campaign**: copies the default campaign into custom campaigns.
  - **Create blank campaign**: same structure/files as default, but empty arrays.

## Campaign Storage Layout
Project storage is organized into three packages (directories):
- `src/main/java/com/dnd` (project core package): implementation logic.
- `src/main/resources/data/default-campaign/` (default campaign package):
  - `world/` JSON files for classes, races, items, spells.
  - `players/` JSON files for player data.
  - This package is read-only from the CLI and used as a template.
- `src/main/resources/data/custom-campaigns/` (custom campaigns package):
  - One subfolder per custom campaign.
  - Same structure and JSON files as the default campaign.
  - Editable through the CLI.

## JSON Data Guidelines
- **Descriptive values** are strings (e.g., name, description).
- **Logic-driven values** are numeric or boolean (e.g., stats, bonuses, effects).
- **Collections** use arrays of nested objects (e.g., player items, spells).
- **Stats** use a structured stat block (strength, dexterity, constitution, intelligence, wisdom, charisma).
- **Abilities** are objects (id, name, description, effects).
- **Enums** such as habitat and challenge rating are stored as strings for readability.

## Data Model and Persistence
- Model classes live under `src/main/java/com/dnd/model/`.
- Core model groups:
  - **CoreStats**: Strength, Dexterity, Constitution, Intelligence, Wisdom, Charisma.
  - **Skills**: skill modifiers for Acrobatics through Survival.
  - **CombatStats**: armor class, initiative, speed, hit points, temporary HP tracking, inspiration, proficiency bonus, death saves.
  - **SavingThrows**: saving throw modifiers for each core stat.
  - **Equipment**: armor slots, hand slots, battle-ready items, stored items.
  - **Items**: `Item` base class with `Armor` and `Weapon` subclasses (and armor slot subclasses).
  - **Damage**: damage type logic and resolution against armor.
- JSON persistence uses repositories that support create, read, update, delete (CRUD):
  - `com.dnd.data.JsonRepository` handles file I/O and list management.
  - `com.dnd.data.CampaignRepositories` exposes per-entity repositories.

## Data Model Package Layout
- `com.dnd.model.character`: characters, classes, races.
- `com.dnd.model.character.stats`: core stats, skills, combat stats, saving throws.
- `com.dnd.model.character.equipment`: equipment slots and loadouts.
- `com.dnd.model.creature`: NPCs, monsters, beasts, creature enums.
- `com.dnd.model.item`: base items, armor, weapons, materials.
- `com.dnd.model.magic`: spells, abilities, effects.
- `com.dnd.model.combat`: damage and damage types.
- `com.dnd.model.world`: places and world objects.

## Spell Model Notes
- `Spell.school` uses `com.dnd.model.magic.School`.
- `Spell.castingMethod` uses `com.dnd.model.magic.CastingMethod`.
- `Spell.effects` is a list of `com.dnd.model.magic.Effect` objects.
- `Spell.damage` uses `com.dnd.model.combat.Damage`.
- `Spell.concentration` uses `com.dnd.model.magic.Concentration` with difficulty/required roll rules.

## UI Mode (Deferred)
- A separate entry point is reserved for the UI application.
- UI-specific details should not impact the CLI flow or domain model.

## Packaging and Distribution
- The project should build into a runnable artifact (e.g., a fat JAR).
- Running the artifact should:
  - Open a command-line window.
  - Display the landing page and its available commands.

## Implementation Documentation Structure
Sub-packages under `Documentation/implementation/` should include:
- **cli/**: CLI flow, input parsing, and loop behavior.
- **pages/**: Page Object Model definitions and page maps.
- **routing/**: Navigation rules and transitions.
- **domain/**: Rules, data, and game mechanics.
- **persistence/**: Any storage or save/load strategy.

## Open Questions
- Should invalid input be logged or just printed to console?
- What is the minimum viable landing page content?
