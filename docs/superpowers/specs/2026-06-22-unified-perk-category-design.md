# STAT Mod - Unified Puffish Perk Category Design

**Date:** 2026-06-22  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Design approved for spec drafting, not yet implemented

## 1. Goal

Replace the current `one Puffish category per perk family` structure with `one single Puffish category` for all `STAT Mod` perks, while keeping:

- `STAT Mod` as the source of truth for perk ownership
- `STAT Mod` as the source of truth for perk unlock validation
- `STAT Mod` as the source of truth for family-specific perk points
- `Puffish Skills` as the single player-facing perk tree UI

The player should see one unified perk tab instead of hopping between multiple family tabs.

## 2. Scope Decisions

### In Scope

- replace multiple perk family categories with one unified Puffish perk category
- keep all existing perk nodes
- keep perk ownership authoritative in `STAT Mod`
- keep perk validation authoritative in `STAT Mod`
- show all stat-family branches inside the same Puffish category
- expose total available perk points as the Puffish category point count
- keep magic categories separate from the perk category
- update tests and generated/static Puffish resources accordingly

### Out of Scope

- changing perk effects
- changing perk costs
- changing perk prerequisites
- changing the internal `PlayerStatData` perk point storage model
- merging magic trees into the same Puffish category
- redesigning the full perk balance

## 3. Core Design Commitments

### 3.1 Source of Truth

`STAT Mod` remains authoritative for:

- unlocked perks
- per-family perk point counts
- perk eligibility checks
- perk side effects
- free-granted or integration-granted perks

`Puffish Skills` remains a mirrored UI and interaction layer only.

### 3.2 Single UI Category

All `STAT Mod` perks must live under one Puffish category:

- `statmod:statmod_perks`

There should no longer be separate Puffish perk categories such as:

- `frontline_physical_combat`
- `ranged_hunt_control`
- `magical_core`
- `elemental_specialization`
- `mental_pressure_resilience`
- `crafting_support`

Those family identities should remain visible through layout and grouping inside the unified category, not through separate tabs.

### 3.3 Family Logic Preserved

The UI category becomes unified, but the gameplay economy does not become naive or global by accident.

The existing family-based point logic in `STAT Mod` remains valid and authoritative unless a later design explicitly changes it.

That means:

- a perk still belongs to one `StatFamily`
- unlock validation still checks that family's real point budget
- Puffish does not gain authority to spend points by itself

## 4. Unified Tree Model

## 4.1 Category Strategy

Use one Puffish category for all perks:

- category id: `statmod:statmod_perks`

This category is unlocked by default.

## 4.2 Layout Strategy

The unified category uses a `hub-and-branches` structure rather than a flat list or one merged spaghetti graph.

Layout rule:

- a small central hub introduces the perk tree
- six major family branches radiate from the hub
- each family branch occupies its own territory in the same canvas

Recommended family placement:

- `frontline` in the upper-left region
- `ranged` in the upper-right region
- `magical` in the upper-center region
- `elemental` in the lower-center region
- `mental` in the lower-left region
- `crafting` in the lower-right region

This keeps the "one tab" requirement while preserving strong visual grouping.

## 4.3 Family Silhouette Rule

Each family should keep its own internal silhouette.

The unified category must not regress into:

- one long line
- one plain grid
- one indistinguishable mass of nodes

Instead, each family region should still feel intentional and recognizable inside the larger unified tree.

## 4.4 Node Mapping

Every existing `Perk` still maps 1:1 to one Puffish skill node.

What changes is only the category ownership:

- before: a perk node resolved to a family category id
- after: every perk node resolves to `statmod:statmod_perks`

The skill ids remain deterministic and reversible.

## 5. Point Display Policy

The unified Puffish perk category should display `total available perk points`.

Definition:

- displayed points = sum of all family perk points currently available in `STAT Mod`

This is a UI-level aggregate only.

It does **not** mean the player may spend any point on any perk regardless of family.

Real unlock validation still checks:

- the perk's family
- the perk's real cost
- the player's real family point budget

## 6. Unlock Flow

## 6.1 Player Action

The player clicks a perk node inside `statmod:statmod_perks`.

`Puffish Skills` emits the unlock interaction through the existing integration path.

## 6.2 Server-Side Resolution

The integration layer resolves:

- category id
- skill id
- mapped `Perk`

Then it routes the unlock attempt through the existing `STAT Mod` path:

1. load `PlayerStatData`
2. resolve the target `Perk`
3. validate unlock through existing `STAT Mod` logic
4. if valid, unlock canonically in `STAT Mod`
5. resynchronize Puffish from canonical state

## 6.3 Invalid Unlocks

If the player has enough `total displayed points` but lacks the correct family budget, the unlock must still fail.

Required behavior:

- the perk remains locked in canonical state
- the Puffish node is reverted if necessary
- the unified category point display is resynced from canonical totals

This is an intentional tradeoff of the unified UI:

- the displayed total point pool is a navigational convenience
- the authoritative spending rules remain family-specific

## 7. Sync Model

## 7.1 Category Initialization

`PuffishSyncService` should initialize the unified perk category once:

- ensure `statmod:statmod_perks` is unlocked
- set category points to the total available perk points across all families

## 7.2 Node Mirroring

For each `Perk`:

- if canonically unlocked in `STAT Mod`, unlock the Puffish node
- otherwise lock the Puffish node

The category id used for all perk sync operations becomes the unified category id.

## 7.3 Aggregate Point Helper

Add or centralize one helper for:

- summing available perk points across all `StatFamily` values

This avoids scattered ad hoc point summing across the Puffish integration.

## 8. Resource Model

## 8.1 Config

The Puffish root config should list:

- one unified perk category
- the separate magic categories that already exist

The old perk-family categories should be removed from the active config.

## 8.2 Category Resources

Perk resources should move from `many family category folders` to `one unified category folder`.

Expected unified resource root:

- `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/`

That folder owns:

- `category.json`
- `definitions.json`
- `skills.json`
- `connections.json`

## 8.3 Builder Strategy

The current family-oriented builder/export path should be refactored so it can generate one large category rather than one category per family.

The new builder should be responsible for:

- one category id
- one combined definitions map
- one combined skills layout map
- one combined connections map
- explicit placement of each family region inside the same canvas

## 9. File Impact

Expected touched areas:

- mapping:
  - `src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java`
- sync:
  - `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java`
- tree generation:
  - `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilder.java`
  - `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeExporter.java`
- Puffish config/resources:
  - `src/main/resources/data/statmod/puffish_skills/config.json`
  - `src/main/resources/data/statmod/puffish_skills/categories/...`
- tests:
  - Puffish mapping tests
  - Puffish sync tests
  - Puffish resource config tests

## 10. Testing Strategy

Add or update targeted coverage for:

- every perk resolving to the unified category id
- reverse mapping from unified category + skill id back to `Perk`
- sync initializing the unified category once
- sync displaying total perk points as the Puffish category points
- valid unlock in the unified category
- invalid unlock when total displayed points are sufficient but the correct family budget is not
- config/resources containing only the unified perk category for perks

Verification before completion must also include:

- targeted `.\gradlew.bat test`
- `.\gradlew.bat build`
- manual in-client confirmation that all stat perks open under one Puffish tab

## 11. Risks and Mitigations

### Risk: UI suggests fully global perk spending

Mitigation:

- keep authoritative unlock checks in `STAT Mod`
- preserve family-specific validation
- rely on layout grouping to communicate family structure

### Risk: unified tree becomes visually unreadable

Mitigation:

- use the hub-and-branches layout
- reserve a distinct canvas territory per family
- keep family silhouettes inside the unified tree

### Risk: category migration breaks existing Puffish mapping assumptions

Mitigation:

- centralize category id computation in one place
- update tests for category id, sync, and resources together

## 12. Acceptance Criteria

This design is satisfied when all of the following are true:

- all stat perks appear under one Puffish category
- the player no longer changes Puffish tabs to browse perk families
- family identity remains readable inside the unified tree
- displayed Puffish perk points equal the total currently available perk points
- actual unlock validation still respects the perk's true family rules
- sync remains canonical to `STAT Mod`
- tests pass and build stays green
