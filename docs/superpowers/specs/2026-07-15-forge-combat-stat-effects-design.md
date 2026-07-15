# Forge 1.20.1 combat stat effects design

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Status:** approved direction

## 1. Goal

Make the first five combat statistics materially affect gameplay without
introducing optional-mod dependencies or persistent modifier stacking:

- `brute_force` scales valid heavy-weapon damage;
- `blade_technique` scales valid blade damage;
- `precision` scales projectile and explicitly classified precision damage;
- `physical_resistance` reduces eligible physical combat damage;
- `physical_endurance` applies a second, multiplicative physical reduction.

This is a narrow vertical slice. Stamina, attack cadence, movement, hunting,
magic, mental effects, crafting quality, perks, HUD, dungeons, Iron's Spells,
and Epic Fight bridges remain separate sub-projects.

## 2. Chosen architecture

Three approaches were considered:

1. Port the NeoForge handlers. Rejected because they mixed permanent attribute
   caches, periodic potion effects, races, Apotheosis, and optional APIs.
2. Express every effect through vanilla attributes. Rejected because an
   attribute cannot distinguish heavy, blade, precision, projectile, and
   environmental damage per hit.
3. Use a pure scaling policy plus one Forge damage adapter. Chosen because the
   formulas are independently testable, the server remains authoritative, and
   effects are applied exactly once to an event rather than stored on players.

The implementation contains:

- `CombatScalingRules`, an immutable validated rules value;
- `CombatStatScaling`, pure level clamping, formulas, classification mapping,
  and finite damage arithmetic;
- `CombatEffectEvents`, the Forge-only adapter for `LivingHurtEvent`;
- `StatModServerConfig`, the Forge server configuration and rules snapshot.

No player-level cache or permanent attribute modifier is required, so login,
death, dimension change, config reload, and stat mutation cannot stack effects.

## 3. Offensive rules

### 3.1 Curve

The total outgoing multiplier is:

```text
x = clamp(level, 0, 100) / 100
multiplier = base + scale * x^exponent
```

Default values:

```text
base = 1.0
scale = 9.0
exponent = 1.5
```

Default anchors, rounded for presentation only:

| Level | Multiplier |
|---:|---:|
| 0 | x1.000 |
| 25 | x2.125 |
| 50 | x4.182 |
| 75 | x6.846 |
| 100 | x10.000 |

This deliberately corrects the old 1.21.1 default base of `1.5`: an
uninvested level-0 player must retain vanilla damage rather than receiving a
free 50 percent increase. The level-100 target remains x10 for the intended
high-power RPG progression.

### 3.2 Classification and causality

The existing public item tags and `WeaponClassifier` remain authoritative.

- Projectile damage owned by a server player uses `precision`, independent of
  the item held at impact time.
- Direct damage classified as `precision` uses `precision`; this supports
  addon ranged attacks represented as direct Forge damage.
- Direct melee damage from a heavy item uses `brute_force`.
- Direct melee damage from a blade uses `blade_technique`.
- Heavy/blade ambiguous and unclassified direct damage receive no multiplier.
- Indirect non-projectile damage receives no weapon multiplier.
- A hit resolves to at most one offensive stat and is scaled at most once.

The target must satisfy the existing `CombatEligibility.eligibleTarget`
contract. Client events, fake players, creative players, spectators, allied
targets, owned tameables, armor stands, invalid targets, canceled events, and
non-positive/non-finite amounts are ignored.

## 4. Defensive rules

Defense uses the existing `CombatEligibility.physicalProfile` predicate. It
accepts combat damage with a responsible entity, including owned projectiles
and explosions, and excludes fire, armor-bypassing magic-like damage,
drowning, falling, freezing, lightning, self-damage, and ownerless hazards.

Reduction curves are linear and bounded:

```text
resistanceReduction = physicalResistanceCap * level / 100
enduranceReduction  = physicalEnduranceCap * level / 100
damageAfterDefense   = damage
                     * (1 - resistanceReduction)
                     * (1 - enduranceReduction)
```

Default caps:

- Physical Resistance: `0.65`.
- Physical Endurance: `0.35`.

At level 100 in both stats, damage received is `22.75%` and combined reduction
is exactly `77.25%`. The reductions remain multiplicative and each configured
cap is constrained to `[0.0, 0.95]`.

Defense applies after STAT Mod's offensive multiplier during player-versus-
player damage. The Forge event amount is changed once after both calculations.
Armor, enchantments, Epic Fight, and other Forge handlers continue through
their normal event pipeline; STAT Mod does not cancel the event.

## 5. Server configuration

Register a Forge server configuration containing:

- `combat.weaponDamageBase`, default `1.0`, range `[1.0, 10.0]`;
- `combat.weaponDamageScale`, default `9.0`, range `[0.0, 99.0]`;
- `combat.weaponDamageExponent`, default `1.5`, range `[0.1, 5.0]`;
- `combat.physicalResistanceCap`, default `0.65`, range `[0.0, 0.95]`;
- `combat.physicalEnduranceCap`, default `0.35`, range `[0.0, 0.95]`.

The event adapter takes one immutable rules snapshot per event. Formula code
does not read Forge config objects directly. Existing explicit server values
are preserved by Forge; invalid edited values are rejected or corrected by the
configuration layer.

## 6. Data flow

For one uncanceled server-side `LivingHurtEvent`:

1. Reject invalid damage amounts.
2. Read the immutable server rules snapshot.
3. If the responsible entity is an eligible server player, resolve exactly one
   offensive stat and scale from that stat's current capability level.
4. If the victim is an eligible server player and the damage profile is
   physical, apply Physical Resistance then Physical Endurance from the
   victim's capability.
5. If and only if the finite positive result differs, update the event amount.

Missing capabilities, invalid inputs, or absent classifications leave the
original event unchanged. The adapter does not award XP; the existing
`LivingDamageEvent` handler continues to observe final post-mitigation damage
for progression rewards.

## 7. Numerical safety

- Levels are clamped to `[0, 100]` even when called outside normal capability
  paths.
- Rule construction rejects non-finite values and clamps every value to its
  documented range.
- Non-finite or non-positive input damage is never transformed.
- Multiplication is performed as `double`; a result above `Float.MAX_VALUE` is
  clamped to `Float.MAX_VALUE` before returning to Forge.
- A non-finite calculated multiplier or reduction falls back to the neutral
  result rather than corrupting the event.
- Reduction never produces negative damage.

## 8. Compatibility boundaries

- No Iron's Spells, Epic Fight, Curios, addon, NeoForge, or Tensura imports.
- Standard Forge damage events and public item tags are the only addon
  boundary in this slice.
- No mixin is introduced.
- Addon weapons extend classification with datapacks as already documented.
- A future Epic Fight bridge may supply a more precise classification hook but
  must call the same pure policy and prevent double scaling.
- Magical damage and spell scaling remain entirely deferred to the dedicated
  Iron's Spells integration.

## 9. Testing and verification

Automated tests cover:

- exact default offensive anchors at levels 0, 25, 50, 75, and 100;
- level clamping and custom validated rules;
- heavy, blade, precision, projectile, ambiguous, and unclassified mapping;
- exact defense caps and the `77.25%` combined reduction;
- excluded/invalid damage remaining neutral;
- finite overflow handling;
- Forge adapter use of `LivingHurtEvent`, player capabilities,
  `CombatEligibility`, public classification, and one final `setAmount` path;
- absence of optional-mod and mixin dependencies.

Completion requires:

1. focused red/green tests;
2. the complete JUnit suite;
3. `gradlew clean test build`;
4. `verify-clean-foundation.ps1 -Mode After` with 74/74 addon hashes;
5. `smoke-gametest-server.ps1` with no fatal signature;
6. JAR inspection confirming the new policy, configuration, and event adapter.

## 10. Acceptance criteria

1. Level 0 causes no offensive damage change.
2. A valid level-100 heavy, blade, or precision hit reaches x10 before normal
   downstream mitigation.
3. Only the stat matching the resolved attack classification is read.
4. Ambiguous, unclassified, invalid, allied, and ineligible attacks are not
   scaled.
5. Eligible physical damage is reduced multiplicatively by the two defensive
   stats and environmental damage is unchanged.
6. One event cannot receive the same STAT Mod effect twice.
7. Changing a stat or server config affects the next event without stale
   caches or relogging.
8. STAT Mod still builds and starts on a dedicated server with no optional mod
   installed.
