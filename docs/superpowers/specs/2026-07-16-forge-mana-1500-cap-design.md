# Forge 1.20.1 — Mana cap at 1,500

## Goal

Correct the supported Forge 1.20.1 balance so a player has 500 maximum mana at
Mana Pool level 0 and exactly 1,500 maximum mana at level 100 with all three
automatic Mana Pool milestones active. No valid level or milestone input may
produce more than 1,500 mana.

## Scope

- Keep Iron's Spells as the sole mana resource owner.
- Keep the existing level range 0–100 and milestones at levels 25, 50, and 75.
- Keep each milestone's `+3%` maximum-mana contribution.
- Keep mana regeneration unchanged: 1 mana/s at level 0, `+0.145/s` per level,
  `+0.5/s` per milestone, capped at exactly 17 mana/s.
- Do not change health, attack damage, spell costs, XP, dungeon logic, network
  protocol, persistence, or current-mana filling behavior.

## Capacity formula

Let:

- `L = clamp(level, 0, 100)`;
- `M = clamp(milestones, 0, 3)`.

The continuous level contribution is reduced from `+2.00%` to `+1.91%` of the
500-mana base per level. Milestones retain `+3%` each:

```text
raw = 500 × (1 + 0.0191 × L + 0.03 × M)
maxMana = min(1500, raw)
```

Required anchors:

| Level | Milestones | Maximum mana |
|---:|---:|---:|
| 0 | 0 | 500.0 |
| 25 | 1 | 753.75 |
| 50 | 2 | 1,007.5 |
| 75 | 3 | 1,261.25 |
| 100 | 3 | 1,500.0 |

Out-of-range inputs are clamped, so `(500, 50)` also produces exactly 1,500.

## Attribute integration

`PlayerBaseBalanceRules.maxMana` owns the pure formula and the hard cap.
`MagicAttributeTarget.MAX_MANA` derives its multiplier from that final target:

```text
multiplier = maxMana(L, M) / 500 - 1
```

The stable `+400` additive modifier first raises Iron's 100-mana base to 500;
the derived multiplier then produces the exact target. The former configurable
`+200% at level 100` capacity path is removed so it cannot bypass the cap or
double-count the three milestones. Other magic attribute targets remain
unchanged, and refreshing attributes must not fill current mana.

## Presentation and documentation

English and French perk text continues to state `+3%` maximum mana and
`+0.5 mana/s` regeneration per milestone. README and the supported-runtime
record must state that 1,500 is the final level-100 total including all three
milestones, not a pre-milestone subtotal.

## Verification

- Update the pure-rule tests first and observe failure against the old 1,545
  result.
- Test every anchor and malicious/out-of-range inputs.
- Add integration/config contracts proving that `MAX_MANA` derives its value
  from the capped rule and that the obsolete configurable capacity path is
  absent.
- Run the complete test suite twice, build the reobfuscated JAR, inspect it,
  and run the required-provider Forge GameTest smoke.
- Merge any newer `forge-1.20.1` commits non-destructively before push and
  deployment to `test-vrai`.
