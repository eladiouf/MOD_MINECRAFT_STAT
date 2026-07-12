# Magic Security Hardening Design

## Goal

Close the confirmed magic-system exploit paths while preserving the existing gameplay: Iron's Spellbooks mana, spells learned through the STAT Mod magic tree, Tensura wrapper spells, and access to additional schools after a valid starting branch.

## Scope

### Mana authority

Remove the global mixin that turns any non-cooldown/non-learning cast failure into success. Iron's Spellbooks remains responsible for its native cast validation; STAT Mod keeps its server-side learned-spell guard as defense in depth.

### Tensura wrapper authority

The Tensura-to-Iron's wrapper may invoke only a Tensura skill that is already present in the caster's Tensura `SkillStorage`. It must never call `learnSkill` while casting. A wrapper that cannot resolve its underlying skill performs no effect and logs a diagnostic.

### Cast progression

Progression is evaluated server-side after a successful Iron's cast.

- A cast that costs less than 5% of maximum mana receives no mastery or Magic Points.
- A qualifying cast with no verifiable hostile impact receives a small mastery reward only; it receives no Magic Points and cannot trigger a mastery milestone bonus.
- A qualifying cast with a verifiable hostile impact receives the existing impactful mastery reward and one Magic Point.
- Existing continuous mastery conversion and milestone rewards apply only to impact-backed mastery. Empty-cast mastery is persisted but is not convertible to Magic Points.

The impact signal is collected from the server-side spell damage event for the current cast and consumed when the cast event completes. It is keyed by player and cleared on logout, clone, and respawn.

### Combat Magic Point farming

The generic kill reward remains available for hostile combat, but it must reject passive mobs and repeated farming of the same entity type within a short server-side window. The protection is keyed by player and entity type, uses game ticks rather than wall-clock time, and is cleared on logout/clone/respawn.

### Virtual inscription request

The virtual inscription menu remains available anywhere by design. The server accepts at most one open request per player during a short tick-based cooldown; excess requests are ignored.

### Race branches

Race affinities remain a restriction only for selecting the initial branch. After a legal starting branch is selected, any school may be unlocked if its normal node, stat, and Magic Point conditions are met.

## Testing

Add focused JUnit regression tests for pure policy/state helpers:

- casts without a qualifying impact receive only the small non-convertible mastery reward;
- impact-backed casts retain Magic Point rewards;
- repeated or passive kill candidates are rejected by the anti-farm policy;
- virtual-menu cooldown rejects repeated requests;
- wrapper authorization requires the underlying Tensura skill to already exist.

Run the focused tests, then `gradlew.bat test` and `gradlew.bat build` from the isolated worktree.

## Non-goals

- Do not restrict post-start multi-school access by race.
- Do not redesign Iron's Spellbooks or Tensura native spell balance.
- Do not alter unrelated user changes in the primary checkout.
