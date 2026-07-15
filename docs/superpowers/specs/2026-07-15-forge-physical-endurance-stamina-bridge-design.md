# Forge Physical Endurance stamina bridge design

**Date:** 2026-07-15  
**Target:** Minecraft 1.20.1, Forge 47.x, Java 17

## Goal

Make STAT Mod's Physical Endurance level improve the stamina systems already
owned by Epic Fight and ParCool. The bridge must remain safe when either mod is
missing and must never introduce a third stamina pool.

## Selected approach

STAT Mod resolves optional attributes by registry ID and applies transient
vanilla `AttributeModifier` instances to the server player. This avoids compile-
time imports from Epic Fight or ParCool while allowing both mods to retain
ownership of current stamina, consumption, regeneration ticks, and battle-mode
switching.

Rejected alternatives:

- A STAT Mod stamina capability would duplicate state and conflict with
  ParCool's existing Epic Fight adapter.
- Direct optional-mod API calls would couple the build to exact addon classes
  and make missing-mod behavior more fragile.

## Balance curve

Physical Endurance uses a linear, clamped level ratio:

`bonus = configuredBonusAt100 * clamp(level, 0, 100) / 100`

Level 0 is neutral. With defaults, level 100 grants:

- +100% base maximum stamina;
- +50% base stamina recovery/regeneration.

Both bonuses use `MULTIPLY_BASE`. The values are server-configurable under an
`endurance` section:

- `staminaCapacityBonusAt100`, default `1.0`, range `0.0..5.0`;
- `staminaRecoveryBonusAt100`, default `0.5`, range `0.0..5.0`.

Invalid non-finite inputs in the pure scaling function are treated as zero.

## Optional attribute targets

The bridge targets exactly these installed registry IDs:

| Provider | Capacity | Recovery |
|---|---|---|
| Epic Fight | `epicfight:staminar` | `epicfight:stamina_regen` |
| ParCool | `parcool:max_stamina` | `parcool:stamina_recovery` |

`epicfight:staminar` is intentionally misspelled because that is the audited
runtime ID. Each target has its own constant UUID. On every refresh STAT Mod
removes its modifier by UUID, then adds one transient modifier only when the
computed amount is positive. This makes refresh idempotent across relogs,
respawns, dimension changes, and repeated commands.

The same bonus is applied to both providers. This is not double stamina:
ParCool's built-in adapter chooses Epic Fight's pool in Epic Fight mode and its
own pool outside that mode. STAT Mod does not read, copy, refill, or synchronize
current stamina values.

## Components

### `PhysicalEnduranceScaling`

A Forge-independent pure function computes a safe modifier amount from the
stat level and configured level-100 bonus. Unit tests cover boundaries,
clamping, proportional values, and non-finite configuration inputs.

### `StaminaAttributeTarget`

A small immutable definition owns the registry ID, stable UUID, modifier name,
and whether it receives the capacity or recovery bonus. The target list is
fixed and package-visible for focused tests.

### `PlayerAttributeEffects`

The server-side adapter reads Physical Endurance from the player capability,
resolves each optional attribute through `ForgeRegistries.ATTRIBUTES`, obtains
the player's `AttributeInstance`, and replaces the stable transient modifier.
Missing registries, attributes, capability data, or attribute instances are
normal no-op cases.

No class from Epic Fight or ParCool is imported, and neither mod is declared as
a required dependency in `mods.toml`.

## Refresh lifecycle

Refresh runs before the normal stats snapshot on:

- player login;
- player respawn;
- dimension change;
- successful `/statmod stat set` and `/statmod stat addxp` mutations;
- automatic XP awards only when the Physical Endurance level changed.

The XP path compares the level before and after the existing coordinator call,
so ordinary awards for other stats do not churn attribute instances.

Config reload hot-application is outside this slice. New config values apply
on the next lifecycle or stat refresh; adding a dedicated reload listener can
be done later if operators need immediate live rebalancing.

## Failure and compatibility behavior

- Missing Epic Fight or ParCool: skip its targets without logging an error.
- Present mod but absent player attribute instance: skip that target.
- Level reset to zero: remove existing STAT Mod modifiers and add nothing.
- Repeated refresh: replace by UUID, never stack.
- Dedicated server: no client classes or side-only code are referenced.
- Optional mods are not added to the Gradle test/build classpath.

## Verification

The implementation is accepted when:

1. pure scaling tests pass;
2. target IDs and UUID uniqueness tests pass;
3. source contract tests confirm registry-only optional integration and all
   lifecycle refresh points;
4. the full JUnit suite and clean Forge build pass without Epic Fight/ParCool;
5. the artifact verifier accepts the built JAR;
6. the finite dedicated GameTest server smoke starts and stops without a fatal
   error.

