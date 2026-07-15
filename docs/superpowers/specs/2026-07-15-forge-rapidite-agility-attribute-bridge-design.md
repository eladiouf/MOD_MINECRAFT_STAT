# Forge Rapidité and Agility attribute bridge design

**Date:** 2026-07-15  
**Target:** Minecraft 1.20.1, Forge 47.x, Java 17

## Goal

Give Rapidité and Agility stable passive identities using vanilla attributes as
the guaranteed baseline and audited Epic Fight/Puffish attributes as optional
specialized outputs.

## Selected approach

All targets, including vanilla targets, are resolved through the Forge
attribute registry. STAT Mod applies transient `MULTIPLY_BASE` modifiers with
stable per-target UUIDs. This extends the existing Physical Endurance adapter
without importing any optional-mod class.

Rejected alternatives:

- Vanilla-only effects would ignore useful installed integration points.
- Addon-only effects would make core stats inert when an addon is absent.
- Tick-based potion effects would cause churn, visual noise, and poor
  interaction with other attribute sources.

## Stat identities and default balance

All curves are linear and clamped from level 0 through 100. Level 0 removes the
STAT Mod modifier and is neutral.

### Rapidité

Rapidité represents offensive cadence, not world movement.

- `minecraft:generic.attack_speed`: +30% base at level 100;
- `epicfight:offhand_attack_speed`: +30% base at level 100 when present.

Both targets receive the same configured amount so main-hand and Epic Fight
off-hand cadence remain coherent. The default matches the previously approved
+0.3% attack speed per level identity.

### Agility

Agility represents movement, repositioning, and body control.

- `minecraft:generic.movement_speed`: +20% base at level 100;
- `puffish_attributes:sprinting_speed`: +10% base at level 100 when present.

Puffish applies its sprinting attribute only while the player is sprinting, so
it is a specialized burst layered on the guaranteed general movement bonus.
The Puffish target uses `MULTIPLY_BASE`: its audited dynamic attribute evaluates
that operation relative to the movement value being modified.

Jump and fall-reduction outputs are deliberately excluded. Jump strength also
belongs to Air Affinity, while fall reduction needs its own capped perk policy.

## Configuration

Add a `mobility` server-config section:

- `rapiditeAttackSpeedBonusAt100`, default `0.30`, range `0.0..2.0`;
- `agilityMovementSpeedBonusAt100`, default `0.20`, range `0.0..2.0`;
- `agilitySprintingSpeedBonusAt100`, default `0.10`, range `0.0..2.0`.

The existing safe linear scaling function is generalized from the endurance-
specific name to `LinearStatScaling`. It treats negative or non-finite
configured values as zero and clamps levels to `0..100`.

## Components

### `LinearStatScaling`

Rename `PhysicalEnduranceScaling` to a stat-neutral utility because Rapidité,
Agility, and Endurance share exactly the same bounded linear formula. Existing
Endurance tests move to the generalized name so no duplicate formula exists.

### `MobilityAttributeTarget`

An enum defines four registry IDs, stable UUIDs, source stat, and config bonus
kind. It computes the modifier amount from a `PlayerStats` snapshot without
knowing about player entities or registries.

### `PlayerAttributeEffects`

The current adapter keeps the Endurance targets and also applies mobility
targets through one shared replace helper:

1. resolve the target with `ForgeRegistries.ATTRIBUTES`;
2. get the player's attribute instance;
3. remove the modifier UUID;
4. add one transient `MULTIPLY_BASE` modifier if the amount is positive.

Missing optional targets remain silent no-ops. Vanilla targets are expected to
exist on players but use the same safe path.

### Relevant-level snapshot

Automatic XP currently refreshes only after an Endurance level change. Replace
that single comparison with an immutable snapshot of Rapidité, Agility, and
Physical Endurance levels. A changed snapshot triggers one refresh before the
network snapshot. Awards that change unrelated stats do not churn attributes.

## Lifecycle

The existing refresh points remain authoritative:

- login;
- respawn;
- dimension change;
- successful stat commands;
- automatic XP when a relevant level changes.

No periodic player tick handler is added.

## Compatibility and failure behavior

- Epic Fight absent: vanilla main-hand attack speed still scales.
- Puffish Attributes absent: vanilla movement speed still scales.
- Optional attribute or player instance absent: only that output is skipped.
- Repeated refresh: stable UUID removal prevents stacking.
- Stat reset to zero: the previous modifier is removed.
- Dedicated server: no client or optional-mod classes are referenced.
- Existing Physical Endurance behavior and UUIDs remain unchanged.

## Verification

Acceptance requires:

1. generalized scaling tests cover zero, midpoint, bounds, and unsafe input;
2. target tests prove exact registry IDs, UUID uniqueness, stat ownership, and
   default/config mapping;
3. adapter contract tests prove registry-only transient replacement;
4. lifecycle tests prove any of the three relevant levels triggers refresh;
5. the complete JUnit suite and clean Forge build pass without optional mods;
6. the built JAR contains the new bridge classes and no external classes;
7. the finite dedicated GameTest server smoke exits without a fatal signature.

