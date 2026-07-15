# STAT Mod — Forge 1.20.1 automatic combat perks

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Status:** awaiting written approval

## 1. Goal

Extend the automatic-perk foundation from 21 to 33 passive perks by adding
three combat milestones for Brute Force, Blade Technique, Precision, and
Physical Resistance. Perks continue to activate and deactivate directly from
server-authoritative stat levels, with no tree, points, purchase, selection,
respec, or persisted unlock state.

This delivery deliberately uses only combat paths that are already classified
and production-tested. Tracking and Keen Senses are deferred to a separate
pursuit/perception design because assigning them generic damage bonuses would
erase their identities.

## 2. Alternatives considered

### 2.1 Extend the existing classified combat pipeline — selected

The current `LivingHurtEvent` path already distinguishes heavy, blade, and
precision attacks and already limits physical defense to an approved damage
profile. Adding perk factors inside the pure combat calculations preserves
those boundaries and keeps one server-authoritative damage mutation.

### 2.2 Generic attack-damage and armor attributes — rejected

`minecraft:generic.attack_damage` would strengthen unclassified weapons and
other melee sources. Armor or toughness modifiers would affect damage profiles
outside STAT Mod's current physical-combat contract. These attributes cannot
express the required specialization safely.

### 2.3 A second combat event handler — rejected

A separate perk handler would make event priority and multiplication order
observable, risk applying damage twice, and duplicate weapon/source validation.

## 3. Catalog additions

The existing milestone levels remain exactly 25, 50, and 75. Each row produces
three stable IDs by appending `_25`, `_50`, and `_75`.

| Stat | ID prefix | Effect per active milestone |
|---|---|---|
| Brute Force | `statmod:brute_force` | +5% final damage for classified heavy attacks |
| Blade Technique | `statmod:blade_technique` | +5% final damage for classified blade attacks |
| Precision | `statmod:precision` | +5% final damage for classified precision attacks |
| Physical Resistance | `statmod:physical_resistance` | +2 percentage points of eligible physical reduction |

At level 75, all three milestones for that stat are active. The offensive perk
bonus is therefore 15%; the resistance perk bonus is 6 percentage points.
There is no level-100 perk in this batch.

The catalog order becomes:

1. the existing 21 perks, unchanged and in their current order;
2. `brute_force_25/50/75`;
3. `blade_technique_25/50/75`;
4. `precision_25/50/75`;
5. `physical_resistance_25/50/75`.

Existing IDs, amounts, localization keys, and activation requirements are not
renamed or reordered.

## 4. Offensive calculation

The current continuous multiplier remains unchanged:

```text
continuous = weaponDamageBase
           + weaponDamageScale * (level / 100) ^ weaponDamageExponent
```

The resolver supplies a perk bonus only for the stat selected by the existing
weapon classification. The final multiplier is:

```text
final = continuous * (1 + activeMilestones * configuredPerMilestone)
```

With the default `0.05`, the perk factor is `1.00`, `1.05`, `1.10`, or `1.15`.
Multiplication occurs once inside `CombatStatScaling`; `CombatEffectEvents`
still calls `event.setAmount` at most once after offense and defense are
calculated.

Eligibility remains unchanged:

- Heavy perks apply only to `WeaponClassification.HEAVY` direct attacks.
- Blade perks apply only to `WeaponClassification.BLADE` direct attacks.
- Precision perks apply only to `WeaponClassification.PRECISION` attacks,
  including approved projectile classification.
- Ambiguous and unclassified equipment receives no offensive perk factor.
- Indirect non-projectile damage, spells, commands, environmental damage,
  fake players, Creative players, Spectators, allies, and ineligible targets
  remain excluded by the existing checks.

## 5. Defensive calculation

The Physical Resistance milestone bonus augments only the resistance component:

```text
resistance = min(0.95,
    physicalResistanceCap * normalizedResistanceLevel
    + activeMilestones * configuredPerMilestone)

endurance = physicalEnduranceCap * normalizedEnduranceLevel
final = (1 - resistance) * (1 - endurance)
```

The default per-milestone value is `0.02`. At Physical Resistance 100 with all
three perks and the current `0.65` cap, the component is `0.71`. With Physical
Endurance 100 at `0.35`, the combined multiplier is `0.1885`, or 81.15% total
reduction.

The existing `CombatEligibility.physicalProfile` remains authoritative. Magic,
fall, fire, drowning, starvation, void, and other rejected profiles never use
the resistance perks.

## 6. Configuration

Add four server values under `automaticPerks`:

```text
bruteForceDamagePerMilestone = 0.05
bladeTechniqueDamagePerMilestone = 0.05
precisionDamagePerMilestone = 0.05
physicalResistancePerMilestone = 0.02
```

Every value is finite and bounded from `0.0` to `0.25`. Existing values are
unchanged. Reloading server configuration refreshes calculations naturally;
there are no combat modifiers to remove or reattach.

## 7. Domain and runtime changes

`AutomaticPerkEffect` gains four effect kinds matching the configuration keys.
`AutomaticPerkBonuses` continues to resolve the active catalog once from
`PlayerStats` and exposes summed, bounded amounts by effect.

`CombatStatScaling` receives the resolved perk amount as an explicit argument.
Pure overloads without a perk amount remain available for compatibility tests
and delegate with zero bonus. Minecraft event classes do not reproduce catalog
or threshold logic.

No NBT schema change is required because no perk state is stored.

## 8. Network and client presentation

The active-ID list can now contain 33 entries, so
`StatsSnapshotMessage.MAX_PERKS` changes from 21 to 33. The network protocol
changes from `"5"` to `"6"`; older clients therefore fail channel negotiation
instead of rejecting or truncating a valid new snapshot.

The existing active-perk list in the native `P` screen displays localized names
and descriptions for the 12 additions. Its three-line visual bound and `+N`
overflow behavior are unchanged. No button or interaction is added.

## 9. Failure handling

- Unknown IDs remain discarded during snapshot canonicalization.
- Declared perk counts below zero or above 33 are rejected before allocation.
- Non-finite damage, invalid rules, absent capabilities, missing classifications,
  and ineligible sources preserve the original damage path.
- Configuration values are clamped by `ForgeConfigSpec` and pure rule objects.
- Offense and defense calculations clamp invalid results and never produce
  negative damage, NaN, or infinity.
- Missing addon tags produce `UNCLASSIFIED`, not a guessed specialization.

## 10. Verification

Automated tests must cover:

- catalog size 33, stable order, uniqueness, and exact new IDs;
- activation boundaries 24/25, 49/50, and 74/75 for all four stats;
- offensive perk factors 1.00/1.05/1.10/1.15;
- strict separation of heavy, blade, precision, ambiguous, and unclassified
  attacks;
- resistance results below and above each milestone, including the 81.15%
  level-100 combined example;
- no perk effect on rejected physical profiles or non-player paths;
- protocol 6, maximum 33, oversized-payload rejection, and round-trip of all IDs;
- English and French name/description coverage for all 33 catalog entries;
- absence of tree, points, purchases, affinities, new modifier UUIDs, and cast
  hooks.

Delivery requires `clean test build`, a forced full test rerun, JAR inspection,
the required-provider Forge GameTest smoke, backup, and SHA-256-verified
deployment to `test-vrai`. A client check sets one covered stat to 24, 25, 50,
and 75, observes the `P` list, and verifies a correctly classified combat case.

## 11. Explicit exclusions

- Tracking and Keen Senses perks;
- Intimidation, Willpower, Erudition, Forging, Cooking, and Alchemy perks;
- level-100 capstones or combined-stat perks;
- critical-hit, armor-penetration, lifesteal, execute, tracking, highlighting,
  perception, loot, or crafting effects;
- generic attack-damage, armor, or toughness modifiers;
- changes to XP rewards, anti-farm rules, weapon tags, Iron's Spells casting,
  Epic Fight skills, or Puffish Attributes ownership;
- affinity restoration, perk trees, points, purchases, or saved unlock flags.

## 12. Completion criteria

The batch is complete when all 12 new perks derive solely from current levels,
affect only their exact classified/eligible combat path, synchronize safely
under protocol 6, display in the existing read-only list, pass all automated and
Forge smoke gates, and deploy as the only STAT Mod JAR without altering the
remaining `test-vrai` mod set.
