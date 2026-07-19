# Adventurer Healer Full Nerf

## Goal

Make rival adventurer parties beatable by creating clear windows between healer actions while preserving the healer's support identity.

## Validated balance

### Normal heal

- Cooldown increases from 20 ticks to 200 ticks (10 seconds).
- Direct healing becomes `5.0 + healer maximum health * 0.05`.
- Nearby splash healing becomes 25% of the direct amount instead of 50%.
- The normal heal no longer grants Regeneration.

### Sanctuary

- Sanctuary activates when at least two party members are below 30% health instead of 40%.
- Sanctuary restores 20% of each party member's maximum health instead of 50%.
- Its cooldown increases from 400 ticks to 600 ticks (30 seconds).
- Sanctuary keeps Regeneration I for 40 ticks (2 seconds).
- Sanctuary keeps Resistance I for 120 ticks (6 seconds).

### Shield and cleanse

- The interval increases from 160 ticks to 300 ticks (15 seconds).
- Absorption becomes Absorption I for 100 ticks (5 seconds).
- The existing cleanse remains unchanged.

### War hymn

- The interval increases from 180 ticks to 300 ticks (15 seconds).
- Strength I and Speed I remain for 160 ticks (8 seconds).
- The hymn no longer grants Resistance.

### Patient selection

- The healer prioritizes itself only below 60% health instead of 85%.
- Other party members become eligible for a normal heal below 75% health instead of 90%.

## Scope

Only `HealPartyGoal` and its exact-value regression contract change. Tank, assassin, mage and archer logic remain unchanged. Friendly fire, party coordination, targeting, equipment, maximum health and the global hostile-dungeon health reduction remain unchanged.

## Verification

A focused automated contract must fail against the current healer values before production code changes. It must cover every value and the removal of both normal-heal regeneration and hymn resistance. The complete Forge 1.20.1 test suite and clean build must pass. The final JAR must be identical on the development build, client and dedicated server, followed by a Java 17 dedicated-server readiness and clean-stop smoke test.
