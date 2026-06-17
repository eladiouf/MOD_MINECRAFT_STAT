# STAT Mod - Puffish Skills Perk UI Replacement Design

**Date:** 2026-06-17  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Design approved for spec drafting, not yet implemented

## 1. Goal

Replace the custom `STAT Mod` perk screen with the native `Puffish Skills` tree UI while keeping `STAT Mod` as the only source of truth for:

- perk ownership
- perk point economy
- perk unlock validation
- perk side effects and integration rewards

The player should interact with perk progression through the `Puffish Skills` UI, but the gameplay state must still be owned by existing `STAT Mod` systems.

## 2. Scope Decisions

### In Scope

- replace the main perk UI entrypoints with `Puffish Skills`
- cover all existing `STAT Mod` perks
- map every `STAT Mod` perk 1:1 to a `Puffish Skills` skill node
- keep perk points authoritative in `STAT Mod`
- keep perk unlock checks authoritative in `STAT Mod`
- synchronize free-granted and integration-granted perks into `Puffish Skills`
- preserve existing perk rewards and integrations unchanged
- add targeted tests for mapping, unlock flow, and synchronization

### Out of Scope

- migrating perk storage into `Puffish Skills`
- replacing `PerkManager` or `PlayerStatData`
- changing perk costs or tier order
- redesigning perk balance
- changing Epic Fight, Tensura, Mahou, ParCool, or Overgeared perk effects
- adding a second custom UI on top of `Puffish Skills`

## 3. High-Level Architecture

### Source of truth

`STAT Mod` remains authoritative for:

- unlocked perks
- per-stat perk points
- free-granted perk tracking
- unlock validation
- post-unlock gameplay effects

`Puffish Skills` is a presentation and interaction layer only.

### Integration principle

There must not be a double perk economy.

`Puffish Skills` internal skill states and category points are treated as a mirrored UI state derived from `STAT Mod`. Whenever the two disagree, `STAT Mod` wins and `Puffish Skills` is resynchronized.

### Main bridge

Add a dedicated compat layer, tentatively `tong.statmod.integration.puffish.PuffishSkillsCompat`, responsible for:

- opening the native `Puffish Skills` screen
- mapping `Perk` ids to Puffish category/skill ids
- mirroring canonical unlock state into Puffish
- mirroring canonical point counts into Puffish
- handling Puffish unlock events and routing them through `PerkManager`

## 4. Tree Model

## 4.1 Category strategy

Use one `Puffish Skills` category per `STAT Mod` perk stat.

This keeps the UI model aligned with the existing point model because perk points already exist per stat inside `STAT Mod`.

Expected categories:

- `brute_force`
- `blade_technique`
- `rapidite`
- `agility`
- `physical_resistance`
- `physical_endurance`
- `precision`
- `tracking`
- `keen_senses`
- `forging`
- `cooking`
- `alchemy`
- `intimidation`
- `willpower`

Each category is unlocked by default.

## 4.2 Skill strategy

Each `STAT Mod` perk becomes one Puffish skill node.

Mapping is 1:1:

- Puffish skill id is deterministic and derived from the perk enum
- unlock state mirrors `PlayerStatData.isPerkUnlocked(...)`
- display name and description mirror the `Perk` enum metadata

Recommended id format:

- category id: `statmod:<stat_name>`
- skill id: `<perk_enum_name_lowercase>`

Example:

- `Perk.BLADE_CORE` -> category `statmod:blade_technique`, skill `blade_core`

## 4.3 Connection strategy

Inside each category, node progression follows the existing perk tier order:

- `CORE`
- `ACTIVE`
- `SYNERGY`
- `SITUATIONAL`
- `MASTERY`
- `TRANSCENDENCE`

The base layout is a linear progression from top to bottom or left to right depending on what reads best in Puffish.

Synergy perks remain placed in the main line for the first version. Their secondary-stat requirement is enforced by `STAT Mod` validation, not by a separate branch topology.

## 4.4 Cost strategy

Every perk node costs one point, matching the current `STAT Mod` perk flow.

Puffish points shown in a category are a mirrored view of:

- `PlayerStatData.getPerkPointsForStat(stat.index)`

Spent points are not trusted from Puffish. Canonical spending remains whatever `PerkManager` and `PlayerStatData` record.

## 5. Open Flow

The current entrypoints opening the custom perk screen are:

- the perk keybind in `ClientInputHandler`
- the perk button in `StatTabScreen`

Those entrypoints must stop instantiating `PerkScreen` directly.

Instead they should request the server to open the Puffish screen through its public API:

- `SkillsAPI.openScreen(ServerPlayer)` for the global tree, or category-specific opening if needed

Reason:

- the mod already owns its network protocol for UI state
- server-side opening keeps the displayed state aligned with synchronized Puffish data
- this avoids fragile direct client construction of Puffish screen internals

## 6. Unlock Flow

## 6.1 Player action

The player clicks a node in the Puffish tree.

`Puffish Skills` emits a skill unlock event through its API.

## 6.2 Server-side handling

The compat layer listens to Puffish unlock events and resolves:

- category id
- skill id
- mapped `Perk`

Then it routes the unlock attempt through the existing `STAT Mod` path:

1. look up the player `PlayerStatData`
2. create/use `PerkManager`
3. validate normal requirements:
   - perk already unlocked
   - enough perk points for the owning stat
   - required stat level
   - required synergy stat if present
   - existing mod-specific gates already enforced by current code
4. if valid, unlock through `PerkManager`
5. let existing reward logic run unchanged
6. resynchronize Puffish state from canonical `STAT Mod` data

## 6.3 Invalid unlocks

If the Puffish click represents an invalid unlock from the `STAT Mod` point of view:

- the perk must remain locked in `STAT Mod`
- the mirrored Puffish skill must be reverted to locked
- mirrored Puffish points must be reset back to canonical values

Optional feedback such as a chat/system message is acceptable, but the key requirement is that invalid Puffish state must not survive synchronization.

## 7. Synchronization Model

## 7.1 Canonical-to-mirror sync

Create a sync routine that projects current `STAT Mod` perk state into Puffish:

- for each managed category:
  - ensure category is unlocked
  - set mirrored points to current available perk points for that stat
  - unlock all skills whose perks are unlocked in `STAT Mod`
  - lock all skills whose perks are not unlocked in `STAT Mod`

This routine should be reusable after:

- player login
- player clone/respawn if needed
- perk unlock
- perk revoke or respec
- free perk grants from integrations
- race evolution changes affecting free perks
- explicit screen-open requests

## 7.2 Free-granted perks

Perks granted outside normal spending, such as intrinsic Tensura perks or other compat rewards, still originate in `STAT Mod`.

When such a perk becomes unlocked:

- `PlayerStatData` remains canonical
- Puffish mirrors the unlocked node
- no Puffish point deduction is considered authoritative

## 7.3 Respec behavior

Any flow that revokes perks in `STAT Mod` must also resync Puffish.

This includes:

- normal perk revocation paths
- race-driven auto-respec
- free-granted perk reconciliation

After respec, Puffish must show:

- only currently unlocked perks
- current available points for each stat

## 8. Data and Resources

## 8.1 Resource ownership

Add `Puffish Skills` resource data inside `STAT Mod` resources rather than depending on `Default Skill Trees` content.

`Default Skill Trees` is treated as a reference/example pack, not a runtime source of truth for `STAT Mod`.

## 8.2 Expected data shape

Provide dedicated Puffish data resources for the `STAT Mod` categories, including:

- root Puffish config listing all `STAT Mod` categories
- one category folder per perk stat
- category metadata
- skill definitions
- skill layout positions
- skill connections

The implementation may generate these statically as JSON resources or through datagen if the project already uses a fitting pattern. For the first pass, static resources are acceptable and lower risk.

## 8.3 Visual direction

Use Puffish's native skill tree presentation with minimal extra styling.

Do not reintroduce the current custom `PerkScreen` visual stack as an overlay. The point of this change is to use the other mod's native tree UX.

## 9. Backward Compatibility

Existing perk logic must continue to work without modification in these areas:

- `PerkManager`
- perk reward/effect handlers
- Tensura perk bridges
- Epic Fight perk gates
- Mahou bridges
- ParCool and Overgeared interactions

The UI replacement must therefore be additive around the existing systems, not a rewrite of their state model.

If `Puffish Skills` is absent at runtime, `STAT Mod` may keep a guarded fallback to the old `PerkScreen`, but when Puffish is present it becomes the primary path.

## 10. File Impact

Expected touched areas:

- client open flow:
  - `src/main/java/tong/statmod/client/ClientInputHandler.java`
  - `src/main/java/tong/statmod/client/StatTabScreen.java`
- new compat and mapping:
  - `src/main/java/tong/statmod/integration/puffish/...`
- possible network request for opening from client to server:
  - `src/main/java/tong/statmod/network/...`
- Puffish resource data:
  - `src/main/resources/data/puffish_skills/...`

Legacy custom screen files may remain in the codebase temporarily but should no longer be the primary player-facing route:

- `src/main/java/tong/statmod/client/gui/PerkScreen.java`
- `src/main/java/tong/statmod/client/gui/TalentTreePanel.java`
- `src/main/java/tong/statmod/client/gui/PerkNodeWidget.java`

## 11. Testing Strategy

Add targeted automated coverage for:

- full `Perk` to Puffish id mapping
- reverse Puffish id to `Perk` mapping
- valid unlock through Puffish event path
- invalid unlock with insufficient points
- invalid unlock with missing stat requirement
- invalid unlock with missing synergy requirement
- sync of free-granted perks
- resync after perk revoke/respec

Verification before completion must also include:

- `.\gradlew.bat test`
- targeted build
- `.\gradlew.bat runClient`
- manual confirmation that:
  - perk keybind opens Puffish UI
  - stats screen perk button opens Puffish UI
  - unlocked perks appear correctly
  - invalid clicks do not desync the tree

## 12. Risks and Mitigations

### Risk: transient Puffish unlock before STAT validation

Mitigation:

- always resync from canonical `STAT Mod` state after handling the event

### Risk: double point accounting

Mitigation:

- never trust Puffish spent points as gameplay authority
- always project available points from `PlayerStatData`

### Risk: incomplete coverage of free-granted perk flows

Mitigation:

- centralize canonical-to-mirror sync and call it from every existing perk mutation path already used by integrations

### Risk: category model drift from current perk model

Mitigation:

- keep one category per perk stat in V1
- avoid mixed-stat categories in the first implementation

## 13. Acceptance Criteria

This design is satisfied when all of the following are true:

- the player no longer uses the custom `STAT Mod` perk screen as the main path
- all existing `STAT Mod` perks are represented in Puffish trees
- perk points still come only from `STAT Mod`
- perk unlock validation still comes only from `STAT Mod`
- free-granted and integration-granted perks appear correctly in Puffish
- respec or revoke paths resync Puffish correctly
- existing perk-based integrations still behave the same
- tests pass, the build is green, and `runClient` verifies the new UI flow
