# Magic Forge Stations UI Design

Date: 2026-07-01
Repo: `MOD_MINECRAFT_STAT`
Scope: `statmod` magic forge stations (`infusion_forge`, `enchantment_anvil`)

## Objective

Convert `statmod:infusion_forge` and `statmod:enchantment_anvil` from mostly decorative recipe anchors into real interactive crafting stations with clear roles, stable UI layouts, and a migration path from the current JSON-only crafting flow.

This design keeps Overgeared as the base forging layer and moves the personalized magical finishing work into dedicated `statmod` station menus.

## Current State

The project already contains the first layer of station scaffolding:

- Dedicated blocks:
  - `tong.statmod.block.InfusionForgeBlock`
  - `tong.statmod.block.EnchantmentAnvilBlock`
- Dedicated block entities:
  - `tong.statmod.block.entity.InfusionForgeBlockEntity`
  - `tong.statmod.block.entity.EnchantmentAnvilBlockEntity`
- Dedicated menus:
  - `tong.statmod.menu.InfusionForgeMenu`
  - `tong.statmod.menu.EnchantmentAnvilMenu`
- Dedicated client screens:
  - `tong.statmod.client.gui.InfusionForgeScreen`
  - `tong.statmod.client.gui.EnchantmentAnvilScreen`

The current limitation is that both menus only expose `3` slots. That is enough for a prototype but not enough for a coherent personalized forge flow with future extensibility.

The current content flow is:

1. Overgeared produces or gates forged base materials and `rough_*` intermediates.
2. `statmod` uses JSON crafting recipes for:
   - `infusion/*.json`
   - `essence/*.json`
3. `infusion_forge` and `enchantment_anvil` exist visually, but their gameplay identity is still weaker than the rest of the forging pipeline.

## Design Goals

- Keep Overgeared as the source of raw forging and `rough_*` production.
- Give each `statmod` station a clear and non-overlapping role.
- Expand menu layouts now so the UI shape does not need to change later.
- Preserve compatibility with the current generated item set:
  - `rough_*`
  - `rune_essence_*`
  - grips
  - shard-based essence recipes
- Leave a clean extension point for future `tongs` and `smithing hammers`.

## Station Roles

### Infusion Forge

`infusion_forge` is the simple, readable magical finishing station.

It is responsible for:

- turning `rough_*` intermediates into infused named weapons
- consuming `rune_essence_*`
- consuming one grip as the finishing component

This station replaces the current role played by the `data/statmod/recipe/infusion/*.json` shapeless recipes.

### Enchantment Anvil

`enchantment_anvil` is the richer, more advanced specialization station.

It is responsible for:

- applying shard-driven or catalyst-driven upgrades
- producing enhanced or pre-enchanted weapon variants
- supporting more complex late-game combinations than the infusion forge
- reserving one visible slot for future rare cost or tooling requirements

This station replaces the current role played by the `data/statmod/recipe/essence/*.json` shapeless recipes.

## Final Slot Layouts

### Infusion Forge Layout

`4` slots total:

1. Base slot
2. Essence slot
3. Finishing slot
4. Output slot

Meaning:

1. Base `rough_*` input
2. `statmod:rune_essence_*`
3. grip
4. crafted infused weapon

This keeps the station focused and easy to read.

### Enchantment Anvil Layout

`5` slots total:

1. Base slot
2. Primary catalyst slot
3. Secondary catalyst slot
4. Rare cost / tool slot
5. Output slot

Meaning:

1. weapon or `rough_*` base, depending on recipe
2. main shard / essence / catalyst
3. second shard / catalyst support
4. optional early on, reserved for future rare cost or forge tooling
5. crafted result

This gives the anvil a stronger identity and avoids a future UI reshuffle.

## Slot Acceptance Matrix

### Infusion Forge

Slot 1 accepts:

- `statmod:rough_*` only

Slot 2 accepts:

- `statmod:rune_essence_arcane`
- `statmod:rune_essence_pyrium`
- `statmod:rune_essence_mithril`

Slot 3 accepts:

- `statmod:wooden_grip`
- `statmod:leather_wrap`
- `statmod:wire_wrap`
- `statmod:runic_grip`

Slot 4:

- output only
- never accepts manual placement

This directly matches the current infusion recipe structure:

- `rough_*`
- one `rune_essence_*`
- one grip

### Enchantment Anvil

Slot 1 accepts:

- allowed `statmod:rough_*` bases for shard recipes
- later, direct weapon inputs for upgrade-style recipes

Slot 2 accepts:

- primary shard / essence / magical catalyst item

Slot 3 accepts:

- secondary shard / duplicate shard / support catalyst

Slot 4 accepts:

- optional rare cost item at first
- future `tongs` / `smithing hammers`
- future special forging tool or ritual catalyst

Slot 5:

- output only
- never accepts manual placement

This directly matches the current essence recipe shape:

- one base
- one or two shard-like catalysts
- one grip or future requirement

## Coherence With Existing Content

This design intentionally follows the item families already present in the repo.

It does not introduce a new parallel system. It reuses:

- `rough_*` intermediates from the Overgeared-backed forging pipeline
- `rune_essence_*` for infusion
- grips as already-implemented finishing components
- existing shard-style catalysts already referenced by essence recipes

The design also stays coherent with future Overgeared-inspired tooling:

- `infusion_forge` does not depend on tools yet because those items are not implemented in `statmod`
- `enchantment_anvil` exposes the future tooling slot now, but keeps it optional for early recipes

## Tooling Strategy

The future `tongs` and `smithing hammers` should imitate Overgeared's native progression style, but they are not part of the first implementation slice for these station menus.

Decision:

- do not block the first station implementation on forge tools
- reserve the `enchantment_anvil` rare cost / tool slot now
- keep `infusion_forge` slot 3 limited to grips for now

This preserves coherence while avoiding dead or misleading slots in the simpler station.

## Menu Behavior

Both stations should follow these rules:

- output slot updates only when the full recipe is valid
- invalid combinations produce no result
- removing any required ingredient clears the output immediately
- shift-click should respect slot rules and never bypass station validation
- the output slot is server-authoritative
- taking the output consumes the required inputs

Persistence rules:

- block entity inventory persists in NBT
- reopening the station restores non-consumed inputs

## Migration Plan

### Phase 1

Keep the current JSON recipes as a temporary fallback while menu behavior is implemented and tested.

### Phase 2

Move the real craft logic into:

- `InfusionForgeMenu` flow first
- `EnchantmentAnvilMenu` flow second

### Phase 3

After both menu flows are stable:

- remove or neutralize duplicated `infusion/*.json`
- remove or neutralize duplicated `essence/*.json`

This prevents long-term double entry points for the same craft.

## Required Code Changes

At a high level, implementation will need to do the following:

- expand `InfusionForgeMenu` from `3` to `4` slots
- expand `EnchantmentAnvilMenu` from `3` to `5` slots
- update both block entities to hold the new container sizes
- update screen layouts and slot positioning to match the new menu shapes
- implement slot validators per station
- implement result computation for valid combinations
- implement input consumption on output pickup
- decide how temporary JSON fallback is disabled once menu crafting is live

## Testing Requirements

The implementation plan must include tests for:

- both blocks open their menus correctly
- block entity inventory persists across save/load
- slot validators reject invalid item families
- valid combinations generate the expected output
- missing required ingredients generate no output
- taking output consumes the correct inputs
- shift-click does not bypass validation
- no regression on currently registered forge items and recipe assets

## Out of Scope

These are intentionally not part of this spec's first implementation:

- full `statmod` implementation of Overgeared-like `tongs`
- full `statmod` implementation of Overgeared-like `smithing hammers`
- redesigning Overgeared's own base forging pipeline
- adding extra station blocks beyond `infusion_forge` and `enchantment_anvil`

## Final Decisions

- `infusion_forge` becomes a `4`-slot finishing station
- `enchantment_anvil` becomes a `5`-slot advanced station
- `infusion_forge` slot 3 accepts grips only for now
- `enchantment_anvil` slot 4 is visible immediately and optional at first
- Overgeared remains responsible for producing the base forged materials
- `statmod` stations become the dedicated layer for personalized magical weapon crafting
