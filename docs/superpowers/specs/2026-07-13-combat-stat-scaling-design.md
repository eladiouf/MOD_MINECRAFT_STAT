# Combat Stat Scaling Design

**Date:** 2026-07-13  
**Status:** Approved design  
**Scope:** Offensive and defensive combat-stat curves, plus matching Trial Dungeon health anchors

## Goal

Make investment in combat stats substantially more valuable while keeping the Trial Dungeon
playable at low levels and challenging in deep floors. A primary offensive stat at level 100 must
produce a total weapon damage multiplier of `x10`. This replaces neither weapon-specific perks nor
material bonuses and does not restore the removed dungeon-only melee boost.

## Offensive Scaling

The existing accelerating curve remains:

```text
multiplier = base + scale * weight * (level / 100)^1.5
```

Default configuration values change to:

```text
base  = 1.5
scale = 8.5
```

Primary weapon stats keep weight `1.0`:

- `BRUTE_FORCE`
- `BLADE_TECHNIQUE`
- `PRECISION`

Their default progression is approximately:

| Level | Total multiplier |
|---:|---:|
| 0 | x1.50 |
| 25 | x2.56 |
| 50 | x4.51 |
| 75 | x7.02 |
| 100 | x10.00 |

`RAPIDITE` and `ARCANE_POWER` keep weight `0.6`, because their weapons or effects gain value from
attack speed or magic synergies in addition to raw damage. They reach `x6.6` at level 100. This
preserves distinct combat roles instead of making every offensive stat mechanically identical.

The character/stat UI must use the same `base` and `scale` defaults so displayed bonuses match the
server calculation.

## Defensive Scaling

Defensive stats remain role-specific. They do not reduce environmental damage.

| Stat | Level 100 effect | Hard cap |
|---|---:|---:|
| Physical Resistance | 65% physical reduction | 65% |
| Physical Endurance | 35% additional physical reduction | 35% |
| Magic Resistance | 65% magic reduction | 65% |
| Willpower | 45% status-damage reduction | 45% |

Physical Resistance and Physical Endurance remain multiplicative. At level 100 in both stats:

```text
damage taken = 0.35 * 0.65 = 0.2275
total reduction = 77.25%
```

Willpower also reduces negative-effect duration by up to 45%. The Iron Will perk adds 10 percentage
points, producing 55% at level 100. The final duration reduction remains capped at 65% to leave room
for future temporary bonuses without allowing immunity through duration rounding.

## Trial Dungeon Mob Health

The dungeon-only global melee multiplier remains deleted. Difficulty is balanced through the
existing deterministic floor health curve instead.

Normal-mob health anchors become:

| Floor | Health multiplier |
|---:|---:|
| 1 | x3.0 |
| 10 | x4.5 |
| 25 | x7.5 |
| 50 | x12.0 |
| 75 | x17.0 |
| 100 | x22.0 |

Interpolation remains linear between anchors. Role multipliers remain unchanged:

- Normal: `x1.0`
- Elite: `x1.25`
- Boss: `x1.5`

Abyss floors start from `x22` at floor 100, add `0.10` per floor, and cap at `x32` for normal mobs.
This raises deep-floor durability without fully cancelling the player's new stat power. Attack,
armor, and toughness scaling are unchanged.

When L2 Hostility owns mob scaling, its existing behavior remains authoritative. This change only
updates STAT Mod's deterministic health curve and does not stack a second hidden health multiplier.

## Configuration And Compatibility

- `weaponDamageBase` remains configurable and defaults to `1.5`.
- `weaponDamageScale` remains configurable and defaults to `8.5`.
- Existing user configuration files keep their explicit values until regenerated or edited.
- No `trial_dungeon.meleeDamageMultiplier` setting or event subscriber is reintroduced.
- Perks, racial modifiers, Overgeared material bonuses, Epic Fight stamina modifiers, and spell
  scaling continue to apply through their existing systems.

## Testing

Automated tests must verify:

1. Primary weapon stats reach `x10` at level 100 with default configuration.
2. The intermediate level 25, 50, and 75 curve points match the approved progression.
3. Secondary offensive stats reach `x6.6` at level 100.
4. Defensive stats reach the new level-100 values and never exceed their caps.
5. Combined physical reduction is exactly 77.25% at level 100 in both physical defenses.
6. Negative-effect duration follows the new Willpower curve and remains bounded.
7. Dungeon health anchors, role multipliers, interpolation, and Abyss cap match this specification.
8. The standalone dungeon melee boost and its configuration key remain absent.

## Non-Goals

- No change to spell-damage formulas.
- No change to dungeon mob attack damage, armor, or toughness.
- No global multiplier based only on being inside the Trial Dungeon.
- No rebalance of individual perks, races, weapons, or armor materials in this change.
