# Forge 1.20.1 Iron's Spells cast XP design

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Iron's Spells target:** `1.20.1-3.16.2`  
**Status:** approved design

## 1. Goal

Make successful Iron's Spells spellbook casts progress STAT Mod's core magical
statistics. Progression must remain server-authoritative, reuse the existing
automatic-XP pipeline and anti-farm limiter, and never recreate elemental
affinities or school-specific progression.

This slice activates cast-based XP for:

- `arcane_power`;
- `casting_speed`;
- `mana_pool`.

`erudition` remains reserved for learning and inscription. `magic_resistance`
remains reserved for surviving incoming spell damage. Those sources are
separate future slices.

## 2. Chosen integration

STAT Mod subscribes directly to Iron's Spells 3.16.2
`SpellOnCastEvent`. Iron's Spells is already a mandatory runtime dependency, so
a typed event adapter is preferable to reflection or inference from mana
changes.

The build uses the exact Iron's Spells `1.20.1-3.16.2` artifact as a
ForgeGradle-deobfuscated compile dependency. The dependency is obtained from a
reproducible repository coordinate pinned to that version. It is not shaded,
repacked, or embedded in the STAT Mod JAR.

The existing `mods.toml` dependency remains mandatory with version range
`[1.20.1-3.16.2,)` and ordering `AFTER`.

## 3. Eligibility contract

The adapter awards XP only when all of these conditions hold:

1. the event player is a `ServerPlayer`;
2. the player passes `XpAwardService.isEligible`;
3. the event source is exactly `CastSource.SPELLBOOK`;
4. the spell identifier is non-null and non-blank;
5. the original spell level is strictly positive.

The adapter rejects casts from `SCROLL`, `SWORD`, `MOB`, `COMMAND`, and `NONE`.
It also ignores client-side observations, invalid events, and rejected casts.

`SpellOnCastEvent` is emitted by Iron's Spells during the committed
server-side `castSpell` path, after pre-cast validation and before the spell's
effect invocation. STAT Mod does not listen to `SpellPreCastEvent`, does not
change the spell level or mana cost, and does not participate in cast
validation.

This slice deliberately rewards spellbook practice only. It prevents command
or mob casts from creating progression and avoids making weapon or consumable
magic the primary training route.

## 4. Normalized action and data flow

Add `SPELL_CAST` to `XpActionKind` and add an `XpAction.spellCast` factory. The
normalized action carries only:

- the original spell level;
- the original mana cost.

School, spell identifier, player, Forge event, and Iron's Spells objects do not
enter `XpRewardPolicy`. This keeps reward calculation pure and guarantees that
no school can produce a hidden affinity reward.

The data flow is:

```text
Iron SpellOnCastEvent
  -> IronSpellXpEvents eligibility filter
  -> XpAction.spellCast(original level, original mana cost)
  -> XpRewardPolicy
  -> XpAwardService
  -> PlayerXpState rolling limits
  -> PlayerStats mutation
  -> existing stat snapshot and XP notices
  -> existing attribute refresh
```

The adapter calls `XpAwardService.award` once per accepted cast with one
normalized action. The existing service batches the three possible stat
awards into one mutation/synchronization cycle.

## 5. Reward formulas

Rewards use `getOriginalSpellLevel()` and `getOriginalManaCost()`, not their
mutable event values. STAT Mod bonuses and third-party event modifiers
therefore cannot inflate progression.

### 5.1 Arcane Power

Every eligible spellbook cast awards:

```text
clamp(2 + originalSpellLevel, 3, 12)
```

### 5.2 Casting Speed

Every eligible spellbook cast awards:

```text
clamp(1 + ceil(originalSpellLevel / 2), 2, 6)
```

Integer implementation must preserve mathematical ceiling, for example
`1 + (level + 1) / 2` for positive levels.

### 5.3 Mana Pool

When the original mana cost is strictly positive, award:

```text
clamp(ceil(originalManaCost / 10), 1, 15)
```

A zero or negative original mana cost gives no Mana Pool XP but does not block
the Arcane Power or Casting Speed rewards.

## 6. Limits and abuse resistance

All rewards pass through the existing `PlayerXpState.acceptXp` path. Each
magical statistic therefore retains the global rolling budget of 200 accepted
XP over 1,200 server ticks.

No new persistent per-spell history or cooldown is introduced. Iron's Spells
already validates spellbook mana and cooldown rules, while the rolling budget
bounds rapid legitimate or automated casts. Adding a second cast cooldown
would incorrectly penalize low-cooldown builds and duplicate provider logic.

The existing level-100 ceiling, creative/spectator eligibility rules,
defensive numeric checks, single snapshot, progression notices, and attribute
refresh behavior remain unchanged.

## 7. Components

### `integration.ironspells.IronSpellXpEvents`

- owns the typed Forge event subscription;
- performs server/player/source/identifier/level eligibility checks;
- constructs one normalized `SPELL_CAST` action;
- delegates the mutation to `XpAwardService` using server game time;
- contains no reward constants.

### `progression.xp.XpActionKind` and `XpAction`

- add the provider-neutral spell-cast action;
- validate nothing beyond representing the supplied normalized values;
- retain the existing immutable action model.

### `progression.xp.XpRewardPolicy`

- maps `SPELL_CAST` to up to three `StatXpAward` records;
- validates positive level and finite, non-negative cost inputs;
- applies only the formulas and caps in this specification;
- never inspects school or spell ID.

### Build configuration

- adds the official/pinned Iron's Spells artifact repository and dependency;
- uses ForgeGradle deobfuscation so mapped Minecraft/Forge types compile;
- keeps the provider out of the produced STAT Mod artifact;
- keeps tests that inspect dependency metadata and JAR isolation.

## 8. Error handling and compatibility

- An invalid player, source, spell identifier, level, or cost is ignored safely.
- A malformed cost must never produce negative or overflowed XP.
- Missing Iron classes are a loader error because Iron's Spells is mandatory,
  not a silently disabled integration.
- Addon spells receive identical treatment when they use Iron's standard
  `SpellOnCastEvent`; no addon-specific import or school table is required.
- An addon that bypasses the public Iron cast event earns no XP until a separate
  compatibility contract demonstrates the need.

## 9. Verification

Pure unit tests cover:

- minimum, ordinary, and high spell levels;
- casting-speed ceiling arithmetic;
- zero, ordinary, and high mana costs;
- no Mana Pool award for non-positive cost;
- exactly the three intended stats and no affinities;
- invalid action inputs returning no rewards.

Contract tests cover:

- subscription to `SpellOnCastEvent`;
- `ServerPlayer` and `CastSource.SPELLBOOK` filtering;
- use of original rather than mutable level and mana values;
- one `XpAwardService.award` call per eligible event;
- the pinned, non-embedded Iron's Spells dependency.

Final verification runs:

1. forced JUnit tests;
2. `clean test build`;
3. JAR inspection for required integration classes, duplicate entries, and
   absence of embedded `io/redspace/ironsspellbooks` classes;
4. the required-provider Forge GameTest smoke with Iron's Spells and its
   dependency closure;
5. deployment to `test-vrai` only after every automated check passes.

Manual acceptance in `test-vrai` verifies that a successful spellbook cast
produces magical XP notices, rejected casts produce none, and the three stat
effects continue to alter Iron's public attributes.

## 10. Out of scope

- elemental affinity or school-specific XP;
- Erudition learning/inscription XP;
- Magic Resistance damage-received XP;
- scroll or spellblade progression;
- changing Iron's mana, cooldown, targeting, or cast execution;
- Curios-slot casting and the `V` target-cast repair;
- Puffish tree nodes or perks;
- addon-specific event bridges.
