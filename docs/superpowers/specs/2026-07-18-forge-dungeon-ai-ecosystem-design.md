# Forge 1.20.1 Dungeon AI Ecosystem Design

Date: 2026-07-18

## Objective

Turn the Trial Dungeon into a living tactical ecosystem on Forge 1.20.1. Encounters must contain readable, coordinated and varied behavior instead of relying mainly on large third-party monsters.

The implementation must:

- remove every `cataclysm:*` entity from active dungeon rosters, themes, pools, secret rooms, vaults, tags and server defaults;
- keep Cataclysm installed when another mod still needs it, but never select a Cataclysm mob inside the Trial Dungeon;
- reuse the existing `AdventurerEntity`, party roles and coordination code;
- add many reusable AI roles without creating one monolithic global brain;
- make Iron's Spells 3.16.2 a first-class combat system for dungeon casters;
- remain fair, telegraphed and performant with multiple FTB Teams on the same floor.

Historical design documents are not rewritten. The no-Cataclysm rule applies to active source, resources and generated server configuration.

## Chosen Architecture

The system uses a hybrid modular architecture.

1. Existing modded mobs provide visual and mechanical variety when they are compatible.
2. `statmod:adventurer` provides humanoid tactical actors requiring precise control.
3. Small role modules provide individual behavior.
4. One `DungeonEncounterDirector` coordinates each occupied floor.
5. A data-driven encounter catalog decides which factions and roles appear in each room.

Rejected alternatives:

- A separate entity class for every role would duplicate navigation, targeting and scaling logic.
- One global director for the entire dimension would scan too many entities and couple unrelated floors.
- Replacing goals on every third-party entity globally would change mobs outside the dungeon and create compatibility risk.

## Core Runtime Model

Every managed dungeon actor has persistent metadata describing:

- faction;
- tactical role;
- floor and encounter identifier;
- alert state;
- squad identifier;
- optional Iron's spell profile;
- short-term tactical memory timestamps.

The alert states are `IDLE`, `SUSPICIOUS`, `ALERTED`, `COMBAT`, `RETREATING` and `REGROUPING`. Actors may only react to information acquired through sight, damage, nearby sound events or squad communication. They do not receive a player's exact position through walls.

The floor director runs only when at least one non-spectator player occupies that floor. It updates at a coarse interval and delegates movement and attacks to vanilla `Goal` instances. It owns:

- target assignment and focus hysteresis;
- squad alerts and last-known positions;
- formations and backline anchors;
- reinforcement budgets;
- retreat and regroup decisions;
- faction hostility;
- encounter cleanup and reload recovery.

Two FTB Teams may occupy the same floor. The director treats every player independently and never shares progression, aggro or rewards between unrelated teams. An encounter may attack both teams, but completion remains governed by the existing FTB Teams dungeon rules.

## Factions

The initial faction set is:

- `ADVENTURER_RIVALS`: balanced humanoid parties;
- `CULT_OF_CINDERS`: fire, blood and necromancy casters;
- `FROZEN_COVEN`: frost control and defensive formations;
- `ARCANE_ORDER`: teleportation, wards and spell interruption;
- `RESTLESS_DEAD`: pressure, resurrection and attrition;
- `DUNGEON_CONSTRUCTS`: sentinels, statues and objective guards;
- `BEAST_PACKS`: hunting, flanking and scent pursuit;
- `INHABITANTS`: merchants, prisoners and survivors.

Faction relations are explicit. Hostile factions can fight one another when their encounter types permit it. Neutral inhabitants avoid combat. Rival adventurers remain hostile dungeon opponents rather than player companions.

## Tactical Role Catalog

The five existing roles remain: tank, assassin, mage, healer and archer.

The first expansion adds reusable roles in four groups.

### Frontline and Control

- `SHIELD_CAPTAIN`: protects allies, blocks corridors and calls focus targets.
- `SPEAR_KEEPER`: maintains distance and punishes direct approaches.
- `BERSERKER`: gains aggression when nearby allies fall.
- `WARDEN`: stays bound to an objective, door, altar or prisoner.
- `SPELLBREAKER`: prioritizes actively casting players and uses silence or displacement.

### Ranged and Magic

- `ELEMENTAL_CASTER`: chooses fire, frost, storm, holy, blood, nature or eldritch profiles.
- `BATTLE_CLERIC`: alternates healing, cleansing, wards and offensive holy spells.
- `NECROMANCER`: pressures from range and performs limited, budgeted summons.
- `HEXER`: applies control or debuffs, then retreats behind the frontline.
- `ARCANE_ARTILLERY`: slow, strongly telegraphed area attacks.
- `SAPPER`: places hazards and denies routes instead of dealing immediate burst damage.

### Hunting and Ambush

- `SCOUT`: observes, retreats and alerts its squad.
- `HUNTER`: follows last-known positions and punishes fleeing targets.
- `AMBUSHER`: begins hidden or dormant and activates from a fair trigger.
- `CEILING_STALKER`: attacks from vertical cover with an audible warning.
- `MIMIC_GUARD`: binds itself to treasure interaction.
- `JAILER`: protects keys or prisoners and attempts to separate a target.

### Living Dungeon Roles

- `RIVAL_EXPLORER`: moves between rooms, fights other factions and searches containers without deleting protected rewards.
- `WOUNDED_SURVIVOR`: flees danger and may lead players toward an encounter.
- `PRISONER`: follows an escort target after release.
- `WANDERING_MERCHANT`: follows a safe route and escapes combat.
- `SCAVENGER`: approaches cleared battlefields and retreats when threatened.
- `RITUALIST`: channels an interruptible room event.
- `ENGINEER`: repairs encounter doors, constructs or traps within a strict action budget.

## Iron's Spells Integration

Iron's Spells is required and its real spell registry is the source of magical actions. The existing `IronsCasterSpells` bridge is expanded rather than duplicating visual fake spells.

Each spell is classified by intent:

- `DIRECT_DAMAGE`;
- `AREA_DAMAGE`;
- `CONTROL`;
- `DEFENSE`;
- `MOBILITY`;
- `ALLY_SUPPORT`;
- `SUMMON`.

The classification is necessary because shields, wards, teleportation and summons do not require a hostile hit target. A caster chooses an intent from combat state first, then selects an enabled spell from the matching profile. This prevents support spells from being treated as failed attacks.

Rules:

- never cast a disabled or missing registry spell;
- clamp spell level to the spell's configured range and the dungeon tier;
- require line of sight only for spells that need it;
- keep defensive spells self-targeted;
- use ally targeting for healing and support;
- cap summons per squad and remove orphaned summons during cleanup;
- announce high-impact casts with sound, particles or a visible wind-up;
- keep friendly-fire protection for party-owned projectiles, summons and spell damage;
- apply independent cooldown categories so one spell cannot suppress every other action.

Initial profiles:

- Fire artillery: firebolt, fireball, magma bomb and flaming barrage.
- Frost controller: icicle, frostwave, ice spikes and frostbite.
- Storm hunter: lightning bolt, ball lightning, chain lightning and lightning lance.
- Arcane duelist: magic missile, magic arrow, guiding bolt and mobility/ward actions.
- Necromantic pressure: wither skull, blood slash, acid orb and limited summons.
- Holy support: healing, cleansing, ward and radiant offense selected from the enabled registry.

Addon spells may join profiles through registry IDs after runtime validation. No addon class is imported directly.

## Cataclysm Removal and Replacement

No active dungeon file may contain a `cataclysm:` entity identifier after migration. Removal covers:

- `StatModServerConfig` default mob and boss rosters;
- `DungeonBossRoster`;
- `DungeonThemes`;
- `ModdedMobPool`;
- scripted candidates in `DungeonMobSpawner`;
- `DungeonSecretRoom` and `DungeonUltraVault`;
- `DungeonSpawnGuard` namespace allowances when no longer needed;
- `data/statmod/tags/entity_type/dungeon_boss.json`;
- deployed server configs generated from old defaults.

Replacement follows encounter function, not visual similarity:

- Cataclysm soldiers become coordinated Stat Mod humanoid squads or compatible SLU/Born in Chaos units.
- Deepling groups become drowned, pirate and frost/storm caster formations.
- Ignited units become fire caster, berserker and construct squads.
- Cataclysm caster elites become Iron's necromancer, archevoker, cryomancer or custom spell-role adventurers.
- Cataclysm bosses become multi-wave elite encounters, compatible non-Cataclysm bosses, or a Stat Mod commander squad with boss-grade objectives.
- Secret-room Cataclysm bosses become named tactical champions with adds and a room mechanic.

If no appropriate third-party entity exists, the replacement is `statmod:adventurer` with a specialized role. Missing optional entities never fall back to Cataclysm.

## Encounter Distribution

Behavior complexity increases by depth:

- Floors 1–10: one-role patrols, guards and clear telegraphs.
- Floors 11–30: two-role combinations, scouts and basic ambushes.
- Floors 31–50: healing, spell interruption, prisoners and rival parties.
- Floors 51–70: faction conflict, rituals, reinforcements and room control.
- Floors 71–90: layered spell profiles, retreats and adaptive formations.
- Floors 91–100: complete squads, champions and multi-stage tactical encounters.

Not every room receives a special AI. Each combat floor gets a deterministic encounter budget based on floor seed, room role and active player count. Treasure and safe rooms use inhabitants or dormant guardians instead of continuous combat spawns.

## Fairness and Readability

- Powerful attacks have a wind-up and cooldown.
- Reinforcements have a visible or audible arrival cue.
- Ambush triggers never spawn unavoidable damage directly on a player.
- Alert propagation has a finite range and delay.
- Enemies investigate the last-known position instead of tracking through walls.
- Retreating enemies cannot indefinitely block floor completion.
- Crowd control uses immunity windows to prevent permanent lockout.
- Adaptive behavior only counters repeated tactics and resets between encounters.

## Performance Budget

- Only occupied floors have active directors.
- Coordination uses cached squad membership and bounded spatial queries.
- High-level coordination runs every 10–20 ticks; individual vanilla goals handle movement between updates.
- Each encounter caps active actors, summons, hazards and reinforcements.
- Dormant and neutral actors use reduced update frequency.
- All floor caches are cleared on unload, server stop and encounter completion.
- Runtime diagnostics report per-floor managed actors and director time through an operator command.

## Persistence and Recovery

Role and squad metadata persists in entity NBT. Goals are reattached idempotently after chunk reload, matching the existing party system. Directors reconstruct their transient state from tagged actors and active encounters. Invalid roles, missing entities or missing spells degrade to a safe basic combat role and log one bounded warning.

## Configuration

Server configuration exposes:

- master enable switch;
- maximum managed actors per occupied floor;
- reinforcement and summon caps;
- coordination period;
- faction and encounter weights;
- Iron's spell profile overrides by registry ID;
- debug logging and visualization controls.

Defaults are balanced for the current heavy Nightfall server. Configuration cannot re-enable Cataclysm dungeon entities.

## Testing and Validation

Pure unit tests cover targeting, alert propagation, faction relations, role selection, spell intent, cooldowns, encounter budgets and deterministic replacement.

Contract tests verify:

- every role is registered and recoverable after reload;
- every configured Iron's spell resolves and has a valid intent;
- support and defensive spells do not require hostile hit targets;
- no active dungeon source, resource or default config contains `cataclysm:`;
- Cataclysm is never used as a fallback;
- directors ignore unoccupied floors;
- independent FTB Teams do not share encounter ownership or rewards.

Dedicated-server validation covers representative floors at each complexity band, two teams on one floor, chunk unload/reload, server restart, spell-heavy combat and maximum encounter budgets. Success requires no dedicated-server client-class crash, no uncontrolled summon growth, no permanent objective blockage and acceptable tick time under the pack's normal load.

## Delivery Slices

1. Cataclysm purge and deterministic roster replacements.
2. Shared actor metadata, factions, alert states and floor director.
3. Frontline, ranged and hunting role modules.
4. Iron's spell intent registry and tactical caster profiles.
5. Living-dungeon actors and faction encounters.
6. Elite/champion encounters, configuration, diagnostics and dedicated-server balancing.

Each slice must build and pass its tests before the next slice begins.
