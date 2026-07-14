# Forge 1.20.1 Player Statistics Foundation

**Date:** 2026-07-14  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Status:** approved design

## 1. Goal

Build the first functional slice of the Forge 1.20.1 rewrite: a stable,
server-authoritative player statistics model that persists across sessions and
player lifecycle changes, synchronizes to the client, and can be exercised by
administrative commands.

This slice deliberately excludes gameplay XP sources, stat effects, graphical
interfaces, and third-party mod integrations. It establishes the data contract
that those later systems will consume.

## 2. Stat roster

The system contains exactly 23 stats grouped into six families.

### Front-line physical combat

- `brute_force`
- `blade_technique`
- `rapidite`
- `agility`
- `physical_resistance`
- `physical_endurance`

### Ranged and hunt control

- `precision`
- `tracking`
- `keen_senses`

### Magical core

- `arcane_power`
- `casting_speed`
- `mana_pool`
- `erudition`
- `magic_resistance`

### Elemental specialization

- `fire_affinity`
- `water_affinity`
- `earth_affinity`
- `air_affinity`

### Mental pressure and resilience

- `intimidation`
- `willpower`

### Crafting, provisioning, and technical support

- `forging`
- `cooking`
- `alchemy`

Each stat has a stable string identifier and a family. Persistence and network
payloads use the string identifier, never an enum ordinal or array index. This
keeps saved data compatible if the Java declaration order changes later.

## 3. Progression model

Every stat stores:

- a level from 0 through 100;
- the XP accumulated toward the next level.

The XP required to advance from the current level is:

```text
requiredXp(level) = 10 * (level + 1)^2
```

Adding XP may advance multiple levels in one operation. Each advancement
subtracts that level's requirement and evaluates the next requirement. At
level 100, XP is always normalized to zero and further XP additions have no
effect.

Setting a level through an administrative command resets that stat's XP to
zero. The first slice exposes no automatic XP source and applies no gameplay
modifier.

## 4. Architecture

### 4.1 Domain layer

The progression rules live in plain Java types independent from Forge events
and networking. The domain layer owns the roster, stable identifiers, level
bounds, XP curve, mutation rules, validation, and copy semantics.

The API must not expose mutable internal collections. Callers receive immutable
views or snapshots so client code and integrations cannot bypass validation.

### 4.2 Forge player capability

A `PlayerStats` capability is attached only to player entities. It owns one
progress record for every known stat and is the server-side source of truth.

Forge lifecycle handlers:

- attach and register the capability for each player;
- serialize and deserialize it through NBT;
- copy it during `PlayerEvent.Clone`, including death respawns;
- send a full client snapshot after login, respawn, dimension change, and an
  accepted administrative mutation;
- invalidate providers when their player lifecycle ends.

### 4.3 Network layer

STAT Mod uses one versioned Forge `SimpleChannel` and one server-to-client
snapshot message for this slice. The snapshot contains every stat identifier,
level, and current XP value.

The server is authoritative. No client-to-server stat mutation message exists.
The client stores the latest snapshot in a read-only cache for future HUD and
screen work. The cache is cleared when the client leaves a world.

A full snapshot is intentional: 23 small records are inexpensive and avoid
partial-state ordering problems. Delta synchronization may be introduced only
when automatic gameplay XP creates a measured need.

## 5. Commands

The command root is `/statmod`.

```text
/statmod stats
/statmod stats <player>
/statmod stat get <player> <stat>
/statmod stat set <player> <stat> <level>
/statmod stat addxp <player> <stat> <amount>
```

Command behavior:

- `/statmod stats` shows the caller's full roster and requires no operator
  permission when executed by a player.
- Reading another player and all mutation commands require operator permission
  level 2.
- `<stat>` offers suggestions from the 23 stable identifiers and rejects any
  other identifier.
- `set` accepts levels from 0 through 100 and resets current XP to zero.
- `addxp` accepts positive integer amounts only and uses the domain progression
  rules, including multiple level-ups and the level-100 cap.
- A successful mutation immediately synchronizes the target player's complete
  snapshot when that player has a connected client.
- Console execution is valid for commands with an explicit target. The
  targetless `/statmod stats` command reports that a player source is required.

User-facing command messages are translatable in English and French.

## 6. Persistence and recovery rules

NBT stores a keyed record per stable stat identifier. Deserialization is
defensive:

- a missing known stat is created at level 0 with zero XP;
- an unknown identifier is ignored;
- a negative level or XP is clamped to zero;
- a level above 100 is clamped to 100 and its XP is reset;
- XP at a level below 100 is normalized through the normal progression rules,
  so oversized values can legitimately cause level-ups;
- a malformed individual entry does not prevent other valid entries from
  loading.

No migration from the deleted NeoForge/Tensura implementation is required. The
stable string format becomes the compatibility baseline for future Forge
versions.

## 7. Testing and verification

Automated tests must cover:

- exactly 23 unique stable identifiers assigned to exactly six families;
- level bounds and the XP requirement formula;
- single and multiple level-ups;
- level-100 normalization;
- independent deep-copy behavior;
- lossless NBT round trips;
- missing, unknown, negative, oversized, and individually malformed NBT data;
- snapshot encode/decode round trips;
- the permission and input-validation policy used by command registration;
- the expected Forge capability, lifecycle, command, and network registrations.

The implementation is complete only when the focused tests and a fresh
`gradlew clean build` succeed and the produced JAR still contains the Forge mod
entry point and metadata.

## 8. Out of scope

This slice does not include:

- automatic XP from combat, magic, movement, crafting, or exploration;
- passive bonuses, attributes, thresholds, perks, fatigue, or weapon mastery;
- a HUD, character screen, or key binding;
- Iron's Spells, Epic Fight, Curios, Puffish Skills, or addon APIs;
- spell learning, spell selection, grimoire slots, or casting behavior;
- offline-player administration or a global UUID database;
- publishing or modpack activation changes.

The later Iron's Spells integration must consume this API without becoming an
ownership layer for stat data.

## 9. Acceptance criteria

The slice is accepted when all of the following are true:

1. A new player has all 23 stats at level 0 and zero XP.
2. Operator commands can inspect, set, and add XP to an online player's stats.
3. Multiple level-ups and the level-100 cap behave according to the specified
   formula.
4. Values survive save/reload, death respawn, and dimension changes.
5. The connected client receives the same complete state after every required
   lifecycle event and accepted mutation.
6. Invalid commands and malformed saved entries fail safely without corrupting
   unrelated stats.
7. The system loads and builds without Iron's Spells, Epic Fight, Tensura, or
   any addon installed.
