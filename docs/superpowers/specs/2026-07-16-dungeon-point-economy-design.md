# Dungeon Point Economy Design

**Date:** 2026-07-16
**Target:** Forge 1.20.1 Trial Dungeon

## Goal

Create a predictable point economy whose rewards and risks increase with dungeon depth without making early floors punitive or allowing modded mobs to break the economy.

## Rules

- Mob points use real scaled difficulty and a gentle depth multiplier: `1 + min(floor, 150) × 0.005`.
- Mob reward is `round(difficulty × 0.18 × depthMultiplier)`, with a floor-sensitive minimum `3 + floor / 25` and cap `min(600, 120 + 3 × floor)`.
- Normal/trasure floor completion awards `100 + 18 × floor + 60 × floorTier`, where `floorTier = floor / 10`.
- Boss completion awards `750 + 80 × floor + 100 × floorTier`. The normal mob-kill reward still applies to the killing blow.
- Flawless completion keeps the existing ×2 multiplier on the completion or boss bonus only.
- FTB teammate assists remain 40% of the unmodified base mob reward; combo and jackpot remain personal.
- Death loses `max(50 + 12 × floor, ceil(currentPoints × deathFraction))`, capped at the current balance. `deathFraction = 10% + 0.1% × floor`, capped at 25% on floor 150.
- Reaching zero points retains the existing ejection rule.
- Ascending does not charge an entry tax. Point conversion remains 1:1 and voluntary.

## Reference values

| Floor | Clear bonus | Boss bonus | Death fraction | Minimum death loss |
|---:|---:|---:|---:|---:|
| 1 | 118 | 830 | 10.1% | 62 |
| 10 | 340 | 1,650 | 11% | 170 |
| 25 | 670 | 2,950 | 12.5% | 350 |
| 50 | 1,300 | 5,250 | 15% | 650 |
| 100 | 2,500 | 9,750 | 20% | 1,250 |
| 150 | 3,700 | 14,250 | 25% | 1,850 |

## Architecture

`DungeonPointBalance` is a pure Java policy containing every formula. `DungeonPoints` becomes the server adapter responsible only for reading entities/player state, applying the pure results, syncing, and displaying messages. This keeps balancing deterministic and independently testable.

## Verification

- Exact boundary tests at floors 1, 10, 25, 50, 100, 150, and above the cap.
- Monotonicity tests ensure rewards and risk never decrease as depth increases.
- Clamp tests prevent negative rewards, overflow-like caps, or losses above the balance.
- Contract tests verify server event wiring passes the actual floor into clear, boss, and death calculations.
- Full Gradle test and clean build remain required.

