# Dungeon Monster Health Scaling

## Objective

Increase the health of every authorized dungeon monster so high-damage spells do not trivialize combat. Damage output, movement, AI, and non-dungeon entities remain unchanged.

## Scope

- Apply only to entities spawned through the dungeon authorized-spawn pipeline.
- Cover normal monsters, elites, mini-bosses, and floor bosses.
- Preserve compatibility with L2 Hostility and multiplayer.
- Do not modify floor architecture, encounter counts, player stats, or spell damage.

## Health Curve

Use a deterministic piecewise-linear multiplier:

| Floor | Health multiplier |
| --- | ---: |
| 1 | 3.0x |
| 10 | 4.0x |
| 25 | 6.0x |
| 50 | 9.0x |
| 75 | 12.0x |
| 100 | 15.0x |

Interpolate linearly between anchors. Above floor 100, add `0.10x` per floor and cap the base multiplier at `25.0x`.

Role multipliers apply after the floor curve:

- Normal monster: `1.0x`
- Elite or mini-boss: `1.25x`
- Floor boss: `1.5x`

The final maximum-health modifier is derived from the entity's post-L2 base health and uses a stable modifier ID, preventing repeated applications from stacking.

## Runtime Order

1. Spawn the authorized dungeon entity.
2. Let L2 Hostility initialize its level and attributes.
3. Resolve the dungeon floor and monster role.
4. Replace the existing STAT Mod health modifier with the newly calculated modifier.
5. Fill health once after the final maximum-health value is established.

Reapplying scaling must preserve the entity's current health ratio rather than healing it. Initial spawn application may fill it to maximum health.

## Role Detection

- Floor bosses use the existing `DungeonBossTracker`/boss spawn path or an explicit persistent-data role tag.
- Mini-bosses and elites receive an explicit persistent-data role tag when queued or spawned.
- Untagged authorized dungeon entities are normal monsters.

Role tags are preferable to entity-type heuristics because modded entity pools can reuse the same type in normal and elite encounters.

## Safety

- Clamp invalid floors to floor 1 for health calculation.
- Clamp the final multiplier to a positive finite value.
- Do nothing for null entities or entities without `MAX_HEALTH`.
- Never apply the multiplier to players, merchants, training dummies, or entities without the dungeon authorization tag.
- Do not alter `ATTACK_DAMAGE`; the existing damage scaling remains unchanged by this task.

## Testing

Unit tests cover:

- Every anchor value.
- Linear interpolation between anchors.
- Abyss growth and the `25.0x` cap.
- Elite and boss role multipliers.
- Floor-zero/negative input clamping.
- Stable modifier replacement without mathematical stacking through a pure multiplier calculation test.

The full Gradle test suite and build must pass before deployment. The built JAR must be copied to the `test` client and verified by SHA-256.
