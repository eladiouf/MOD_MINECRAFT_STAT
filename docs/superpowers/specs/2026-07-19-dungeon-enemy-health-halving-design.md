# Dungeon Enemy Health Halving Design

## Goal

Reduce the final maximum health of every hostile entity managed by STAT Mod in the Forge 1.20.1 Trial Dungeon by exactly 50 percent. Preserve combat behavior, damage, armor, spell selection and encounter composition.

## Scope

The reduction applies to:

- ordinary room mobs;
- elites and bosses;
- boss-altar spawns;
- rival adventurer parties and `/statparty` test parties;
- tactical casters;
- rival explorers, ritualists and engineers.

It does not apply to players, neutral safehouse inhabitants, released prisoners, wandering merchants, wounded survivors, scavengers, or unrelated mobs outside the Trial Dungeon.

## Architecture

Introduce one dungeon health-balance component with a fixed `0.5` multiplier and a stable attribute-modifier identifier. Applying the component removes its previous modifier and installs the same multiplier again, making repeated calls safe.

The component preserves the entity's current health percentage when it changes maximum health. A full-health newly spawned enemy remains at full health relative to its reduced maximum. A previously damaged or reloaded enemy keeps the same health ratio and cannot be healed by reapplication.

Attach the component at all hostile STAT spawn paths:

1. the general floor-scaling path for ordinary, elite and deferred chamber mobs;
2. adventurer role configuration for rival parties, debug parties and adventurer bosses;
3. boss-altar setup for non-adventurer bosses;
4. occupied-floor AI recovery so persistent enemies created by an older build receive the reduction after reload.

Neutral inhabitants remain excluded by checking the living-actor non-combat marker before applying the balance modifier.

## Testing

Use test-driven development:

- prove the multiplier is exactly `0.5`;
- prove repeated application is mathematically idempotent;
- prove health-ratio preservation;
- add source contracts covering the general scaling, adventurer, boss-altar and occupied-floor recovery paths;
- run the complete Gradle test and build suite;
- deploy the resulting JAR to the active client and dedicated server only after successful verification.

## Acceptance Criteria

- Every hostile STAT dungeon mob has half the maximum health it would have had before this change.
- No hostile category or spawn path is omitted.
- Neutral inhabitants and players retain their existing maximum health.
- Reapplying the balance does not repeatedly halve an entity.
- Damage, armor, tactical AI, Iron's Spells behavior and room population are unchanged.
