# Forge Hunter Perception Perks Design

**Date:** 2026-07-16
**Target:** Minecraft 1.20.1, Forge 47.4.10, Java 17
**Branch lineage:** `forge-1.20.1`
**Status:** Approved direction; written-spec review pending

## 1. Goal

Give `TRACKING` and `KEEN_SENSES` distinct, useful, deterministic gameplay
effects while preserving STAT Mod's automatic level-based perk model.

- `TRACKING` retains information about one prey the player has actually hit.
- `KEEN_SENSES` provides an intentional, short-range threat scan while the
  player is crouching.
- Both effects are personal to the observing client.
- Neither effect changes damage, dodge chance, loot, XP, targeting, or entity
  state.

This batch adds six automatic perks at levels 25, 50, and 75, taking the
catalog from 33 to 39 definitions.

## 2. Existing behavior retained

The current XP sources already match the two identities and remain unchanged:

- killing an eligible hostile target awards `TRACKING` XP;
- discovering a biome for the first time awards `KEEN_SENSES` XP.

The existing rolling XP limits, eligibility rules, notices, save schema, stat
screen, and administrative commands remain unchanged. The six perks are
derived from current levels and have no persisted unlock state, tree, perk
points, purchase, or respec behavior.

## 3. Alternatives considered

### 3.1 Personal deterministic perception — selected

A committed hit marks one prey for Tracking. Crouching scans nearby hostile
entities for Keen Senses. A client-only world-space contour displays the
results without changing the server entity's glowing flag.

This matches the canonical identities, is deterministic, remains private in
multiplayer, and does not overlap the combat scaling stats.

### 3.2 Damage on marked prey and random dodge — rejected

This would make Tracking overlap Precision and Keen Senses overlap Agility or
Physical Resistance. Randomly cancelling hits would also complicate Epic Fight
and Iron's Spells interactions and make combat results harder to reason about.

### 3.3 Loot bonuses and ore detection — rejected

Loot modification risks duplication and conflicts with the Trial Dungeon's
point and no-drop policies. Repeated ore searches are expensive and encourage
passive scanning rather than hunter gameplay.

## 4. Automatic perk catalog

The existing 33 IDs, order, requirements, effects, and localized text remain
unchanged. Append these definitions in this exact order:

| Order | ID | Requirement | Effect | Amount |
|---:|---|---|---|---:|
| 33 | `statmod:tracking_25` | Tracking 25 | `TRACKING_FOCUS` | 0.25 |
| 34 | `statmod:tracking_50` | Tracking 50 | `TRACKING_FOCUS` | 0.25 |
| 35 | `statmod:tracking_75` | Tracking 75 | `TRACKING_FOCUS` | 0.25 |
| 36 | `statmod:keen_senses_25` | Keen Senses 25 | `KEEN_SENSES_AWARENESS` | 0.25 |
| 37 | `statmod:keen_senses_50` | Keen Senses 50 | `KEEN_SENSES_AWARENESS` | 0.25 |
| 38 | `statmod:keen_senses_75` | Keen Senses 75 | `KEEN_SENSES_AWARENESS` | 0.25 |

`TRACKING_FOCUS` and `KEEN_SENSES_AWARENESS` are normalized milestone scores,
not attribute modifiers. Existing automatic-bonus aggregation produces
`0.25`, `0.50`, or `0.75`; the pure perception-rules layer converts that value
to zero through three active milestones with `round(score / 0.25)`. This fits
the existing `0.75` aggregate safety cap without changing it. These two effects
use a fixed `0.25` configured amount in the resolver switch. No new
server-config keys are added, avoiding client/server tuning divergence for
visual-only behavior.

## 5. Pure perception rules

Create a loader-neutral `HunterPerceptionRules` utility. It clamps levels to
0–100 and normalized scores to 0–0.75, then derives an integer milestone count
from 0 to 3 before applying these formulas.

### 5.1 Tracking

Tracking is disabled at level 0. At levels 1–100:

```text
markDurationTicks = 60 + level + 40 * activeMilestones
markRangeBlocks   = 12.0 + 0.12 * level + 4.0 * activeMilestones
```

Important anchors:

| Level | Milestones | Duration | Range |
|---:|---:|---:|---:|
| 0 | 0 | disabled | disabled |
| 1 | 0 | 61 ticks | 12.12 blocks |
| 25 | 1 | 125 ticks | 19 blocks |
| 50 | 2 | 190 ticks | 26 blocks |
| 75 | 3 | 255 ticks | 33 blocks |
| 100 | 3 | 280 ticks | 36 blocks |

Each perk therefore adds 40 ticks and 4 blocks. The rules never produce a
duration above 280 ticks or a range above 36 blocks.

### 5.2 Keen Senses

Keen Senses is disabled at level 0. At levels 1–100:

```text
scanRangeBlocks = 6.0 + 0.10 * level + 2.0 * activeMilestones
```

Important anchors:

| Level | Milestones | Range |
|---:|---:|---:|
| 0 | 0 | disabled |
| 1 | 0 | 6.1 blocks |
| 25 | 1 | 10.5 blocks |
| 50 | 2 | 15 blocks |
| 75 | 3 | 19.5 blocks |
| 100 | 3 | 22 blocks |

Each perk adds 2 blocks. Scanning requires the local player to be alive,
crouching, and not a spectator.

## 6. Tracking data flow

### 6.1 Server acquisition

Observe final positive `LivingDamageEvent` damage at lowest priority. A mark is
created only when:

- the responsible entity is an eligible `ServerPlayer`;
- the damaged entity implements vanilla `Enemy`;
- final damage is finite and strictly positive;
- the player's Tracking level is at least 1.

Any damage source correctly attributed to the player may mark prey, including a
committed Iron's Spells damage event. Cast attempts, shields, healing,
movement, summons, control effects, and zero-damage hits cannot mark anything.
The handler observes damage and never mutates or cancels the event.

Only the most recently damaged hostile entity is marked. Hitting another
eligible hostile replaces the previous mark. Hitting the same entity refreshes
its duration. Death, logout, dimension change, or an explicit clear invalidates
the mark.

### 6.2 Network message

Add a server-to-client `TrackedPreyMessage` containing:

- `entityId`: non-negative for a mark, `-1` for clear;
- `durationTicks`: 0–400, with 0 required for clear.

Decoding rejects entity IDs below `-1`, negative durations, durations above
400, and inconsistent clear payloads. The production formula stays below this
defensive wire bound.

The client stores only the current entity ID and a client-tick expiry. It does
not persist the mark. Missing, removed, dead, cross-dimension, expired, or
out-of-range entities are not rendered.

## 7. Keen Senses data flow

Keen Senses needs no new packet. The client already receives the authoritative
stat snapshot and canonical active-perk IDs.

Every five client ticks while the scan is active:

1. read the synced Keen Senses level and milestone count;
2. calculate the bounded scan radius;
3. query living `Enemy` entities in the player's local AABB;
4. keep only alive, non-removed entities within the exact spherical radius;
5. sort nearest first and keep at most 64 results.

The cache is cleared immediately when the player stops crouching, dies, becomes
a spectator, disconnects, changes level, or has level 0. Invisible hostile
entities may be outlined if they are already legitimately present in the
client's entity view; this system never forces the server to track or transmit
an otherwise unknown entity.

## 8. Personal client rendering

Render world-space wireframe contours after entities using a dedicated
client-only component:

- marked Tracking prey: amber;
- Keen Senses threats: red;
- if one entity qualifies for both, amber takes precedence;
- contours are visible through terrain;
- no text, sound, particle spam, action-bar message, toast, or XP notice is
  produced.

The renderer uses only client cache data. It never calls `setGlowingTag`, never
modifies synced entity data, and never sends vanilla metadata packets. Other
players therefore cannot see another player's marks or scan results.

The renderer accepts first- and third-person cameras, interpolates entity
positions using the partial tick, and restores every render state it changes.
Client-only classes must remain isolated behind `Dist.CLIENT` subscribers or
client packet handlers so a dedicated server cannot load rendering classes.

## 9. Network and presentation changes

- Change `StatModRuntime.NETWORK_PROTOCOL` from `"6"` to `"7"`.
- Change `StatsSnapshotMessage.MAX_PERKS` from 33 to 39.
- Register exactly one new bounded server-to-client message.
- Preserve canonical catalog-order filtering and oversized-list rejection.
- Reuse the current `P` screen; it already derives perk rows from the catalog.

Add English and French name/description keys for all six perks. Tracking text
states `+2 seconds and +4 blocks` per milestone. Keen Senses text states
`+2 blocks` of crouched threat-scan range per milestone. The underlying exact
Tracking duration is 40 ticks, which is two seconds at the normal 20 TPS.

## 10. Trial Dungeon and provider boundaries

This batch does not modify or import anything from `tong.statmod.dungeon`.
Dungeon mobs that implement `Enemy` participate naturally; dungeon points,
drops, objectives, spawn guards, boss tracking, and mob scaling remain
untouched.

Iron's Spells remains required, but no cast hook or target selection is changed.
Epic Fight and Pufferfish's Attributes remain required providers, but this
feature does not read or modify their attributes, skills, stamina, animations,
or events.

## 11. Failure handling and safety

- Null or missing player stats disable the effect.
- Malformed levels and milestone scores are clamped by pure rules.
- Malformed network payloads are rejected before cache mutation.
- A stale entity ID produces no rendering and is cleared at expiry.
- A missing client world or player clears both perception caches.
- Client scans are throttled to once per five ticks and capped at 64 entities.
- No gameplay authority is granted to the client; all client decisions are
  visual only.
- No second damage-mutating handler is introduced.

## 12. Verification

Automated verification must cover:

- all pure formula anchors and malformed-input clamps;
- exact catalog count, IDs, order, and 24/25/49/50/74/75 boundaries;
- preservation of the first 33 definitions;
- protocol 7 and maximum 39;
- full 39-perk snapshot round trip and oversized payload rejection;
- tracked-prey codec validation and clear semantics;
- committed-damage eligibility and explicit absence of damage mutation;
- client scan eligibility, nearest-first cap, and cache clearing contracts;
- personal renderer restrictions, color precedence, and no global glow calls;
- English and French localization completeness;
- the complete clean unit suite, JAR inspection, and required-provider Forge
  GameTest smoke.

Manual client validation in `test-vrai` must confirm:

- only the observing player sees the contour;
- marking refresh/replacement and expiry work;
- crouching starts and releasing crouch stops the threat scan;
- invisible loaded hostile mobs can be outlined;
- non-hostile entities and players are never outlined;
- the dungeon, Iron's Spells casting, and Epic Fight combat still behave
  normally.

## 13. Explicit exclusions

This batch does not add:

- damage bonuses against marked targets;
- dodge, hit cancellation, invulnerability, or projectile reduction;
- loot, Fortune, Looting, drop-table, ore, chest, or dungeon reward changes;
- automatic target selection, aim assist, homing, or spell targeting changes;
- global glowing state, team manipulation, or entity metadata spoofing;
- new key bindings, toggles, notifications, sounds, particles, or HUD widgets;
- XP source, rolling-limit, save-schema, affinity, tree, point, purchase, or
  respec changes.

## 14. Completion criteria

The batch is complete when:

1. the catalog contains exactly 39 canonical automatic perks;
2. the six new perks activate only at their exact level thresholds;
3. Tracking marks only one committed-damage hostile prey per eligible player;
4. Keen Senses scans only while crouching and respects its range and cap;
5. perception contours remain personal and never mutate server entity state;
6. protocol 7 and both codecs reject malformed bounds;
7. all automated tests, build, artifact inspection, and required-provider
   GameTest pass;
8. the verified JAR is backed up and deployed by replacing only STAT Mod in
   `test-vrai`;
9. the Forge 1.20.1 branch is pushed without touching the parallel dungeon
   worktree.
