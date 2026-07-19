# STAT Mod — Forge 1.20.1 automatic perks foundation

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Status:** approved direction

## 1. Goal

Add passive perks that activate automatically when their stat-level requirements
are met. There is no perk tree, currency, purchase, selection, respec, or manual
unlock action. The first delivery establishes the complete automatic-perk
architecture and a deliberately bounded catalog for the seven stats whose
attribute bridges are already production-tested.

The other twelve stats remain eligible for later event-driven perk batches.
They are not assigned placeholder effects merely to make every stat appear in
the first catalog.

## 2. Source of truth

Perk activation is derived from the current server-owned `PlayerStats` snapshot.
STAT Mod does not save a second unlocked-perk list. A perk is active exactly
when every requirement in its definition is satisfied.

Consequences:

- reaching the required level activates the perk immediately;
- an operator lowering a required stat deactivates it immediately;
- death, dimension changes, reconnects, and config reloads cannot leave stale
  unlock state;
- legacy saves require no perk migration;
- Puffish Attributes supplies compatible attributes only and never owns perk
  progression.

## 3. Domain model

`AutomaticPerkDefinition` is immutable and contains:

- a stable namespaced identifier under `statmod`;
- a localization key and presentation order;
- one or more `(StatType, minimum level)` requirements;
- one effect kind;
- a finite, validated effect amount.

`AutomaticPerkCatalog` owns the ordered definitions and rejects duplicate IDs,
unknown/retired stats, levels outside 1–100, empty requirements, non-finite
amounts, and effects without a supported runtime target.

`AutomaticPerkResolver` is pure Java. Given a stat-level lookup, it returns the
ordered active perk IDs. It performs no mutation and has no Minecraft, Forge,
Iron's Spells, Epic Fight, or Puffish imports.

## 4. Initial catalog

The first catalog uses three milestones for each mature attribute-backed stat:
levels 25, 50, and 75. Each milestone is a separate perk with a stable ID. The
bonuses are cumulative and are added to the existing continuous stat curve.

| Stat | IDs | Effect per milestone |
|---|---|---|
| Rapidité | `rapidite_25`, `rapidite_50`, `rapidite_75` | +2% main-hand and Epic Fight off-hand attack speed |
| Agility | `agility_25`, `agility_50`, `agility_75` | +2% movement and Puffish sprinting speed |
| Physical Endurance | `physical_endurance_25`, `physical_endurance_50`, `physical_endurance_75` | +4% Epic Fight and ParCool stamina capacity and recovery |
| Arcane Power | `arcane_power_25`, `arcane_power_50`, `arcane_power_75` | +3% Iron's Spells spell power |
| Casting Speed | `casting_speed_25`, `casting_speed_50`, `casting_speed_75` | +2% Iron's Spells cast-time and cooldown reduction |
| Mana Pool | `mana_pool_25`, `mana_pool_50`, `mana_pool_75` | +3% Iron's Spells maximum mana and mana regeneration |
| Magic Resistance | `magic_resistance_25`, `magic_resistance_50`, `magic_resistance_75` | +2% Iron's Spells spell resistance |

At level 75, all three milestones for that stat are active. There is no level-100
perk in this first batch; level 100 remains valuable through the existing
continuous scaling and leaves room for later capstones with distinct behavior.

The catalog contains no elemental affinities and cannot resolve the retired
`fire_affinity`, `water_affinity`, `earth_affinity`, or `air_affinity` IDs.

## 5. Runtime application

`AutomaticPerkBonuses` converts the active catalog entries into additive bonus
amounts per supported effect target. `PlayerAttributeEffects.refresh` combines
the existing continuous amount with the automatic-perk amount before replacing
the target's stable transient modifier.

The existing UUID for each attribute target remains the only modifier UUID for
that target. Refresh therefore remains idempotent and cannot stack duplicate
modifiers. Missing optional attributes are skipped exactly as they are today.

Refresh continues on stat mutation, login, respawn, dimension change, and
server-config reload. No per-tick scan is added. Mana is never filled as a side
effect of increasing maximum mana.

## 6. Client presentation

The server includes the ordered active-perk IDs in the existing full stats
snapshot. The payload is bounded by the catalog size; the client never declares
a perk active by itself.

The native `P` screen gains a compact `Perks actifs` / `Active perks` section,
not a tree. It lists active localized perk names and their bonuses. Locked perks
are not shown in the first delivery, and there is no interaction control.

No separate activation notification is sent. The existing XP and level-up
feedback remains unchanged, avoiding duplicate messages when a stat crosses a
milestone.

## 7. Configuration

The milestone levels are stable catalog identity and remain 25/50/75. The seven
per-milestone effect amounts are server-configurable with the table values as
defaults, finite bounds from 0 to 0.25, and safe clamping.

Changing a configured amount refreshes online players. Configuration cannot add
arbitrary perk IDs or change requirements in this first delivery; data-driven
catalog loading is deferred until malformed datapack recovery and synchronization
semantics can be designed separately.

## 8. Failure handling

- Invalid built-in definitions fail focused tests and startup validation rather
  than being silently accepted.
- Unknown perk IDs received by a client are ignored and logged once.
- A missing Epic Fight, ParCool, Puffish, or Iron's Spells attribute produces no
  modifier and no crash, even though the supported modpack requires the audited
  providers.
- A stale or mismatched network protocol is rejected through Forge's existing
  channel negotiation.
- Perk resolution never changes XP, levels, mana, cooldowns, or inventory.

## 9. Verification

Automated coverage must prove:

- boundary behavior at levels 24/25, 49/50, and 74/75;
- cumulative bonuses and multi-requirement support in the resolver;
- rejection of malformed definitions and retired affinity IDs;
- idempotent attribute refresh with one modifier UUID per target;
- exact French and English localization coverage;
- bounded server-to-client perk synchronization;
- no perk points, purchase packets, tree resources, or Puffish Skills dependency;
- unchanged behavior for players below every milestone.

Delivery gates are `clean test build`, JAR inspection, the required-provider
Forge GameTest smoke, backup, and hash-verified deployment to `test-vrai`.
Client verification confirms the `P` list and attribute values immediately
below and above one milestone.

## 10. Explicit exclusions

- perk trees, tabs, nodes, edges, point currencies, purchases, respecs, and
  manual unlocks;
- Puffish Skills as a dependency or source of truth;
- elemental affinities;
- permanent stored unlock flags;
- level-100 capstones;
- event-driven perks for the other twelve stats in this first batch;
- modifications to Iron's Spells' native Curios casting.

## 11. Completion criteria

The milestone is complete when the 21 catalog perks activate and deactivate
solely from authoritative levels, their attribute bonuses apply without
stacking, the active list is visible but non-interactive in `P`, all automated
and Forge smoke gates pass, and `test-vrai` contains exactly one hash-matched
STAT Mod JAR with the rest of its mod set unchanged.
