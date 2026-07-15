# STAT Mod — Forge 1.20.1 master remake design

**Date:** 2026-07-15  
**Branch:** `forge-1.20.1`  
**Platform:** Minecraft 1.20.1, Forge 47.4.10, Java 17  
**Status:** approved direction  

## 1. Product goal

Rebuild STAT Mod as the progression backbone of a large medieval RPG modpack.
The Forge 1.20.1 edition keeps the identity and useful systems of the 1.21.1
edition while replacing fragile cross-mod patches with explicit, testable
compatibility boundaries.

The result is one STAT Mod JAR with:

- a standalone core that loads without third-party gameplay mods;
- optional Iron's Spells and Epic Fight integrations;
- server-authoritative progression and casting decisions;
- data-driven balancing and addon classification;
- safe degradation when an optional mod or addon is absent;
- no Tensura code, metadata, content, migration, or runtime dependency.

“Feature parity” means preserving the intended player experience, not copying
the old implementation line for line.

## 2. Current accepted baseline

The current branch already provides:

- 23 stable statistics in six families;
- Forge player capabilities, defensive NBT persistence, lifecycle copying, and
  full client snapshots;
- operator and player inspection commands;
- automatic XP for 14 non-magical statistics;
- rolling anti-farm limits and public equipment classification tags;
- an Iron's Spells addon catalog and a stable client batch;
- a clean Forge build with all tests passing and 74 preserved addon JARs.

This behavior is the compatibility baseline. Later work may extend its data
format but must not silently rename stat identifiers or lose saved values.

## 3. Architecture decision

STAT Mod remains a single deployable mod but uses strict internal modules.

### 3.1 Core modules

- `stats`: roster, values, XP curve, persistence, snapshots, and mutations;
- `progression`: normalized gameplay actions, rewards, limits, and feedback;
- `effects`: stat-to-attribute scaling and passive milestone effects;
- `stamina`: independent resource, recovery, costs, and exhaustion rules;
- `perks`: prerequisites, points, unlock state, and runtime effects;
- `client`: read-only caches, HUD, screens, key mappings, and presentation;
- `config`: server-owned balancing values with validation and safe defaults.

Core modules may use Forge and Minecraft only. Domain calculations stay plain
Java whenever Minecraft objects are not required.

### 3.2 Compatibility modules

- `integration.ironspells`: magical XP, mana/stat scaling, learned spell
  access, Curios grimoire resolution, and casting;
- `integration.epicfight`: stamina bridge, combat events, weapon behavior, and
  optional skill gates;
- later integrations only when they have a documented modpack need.

Compatibility code is guarded by mod-presence checks and isolated class
loading. An absent optional mod must not resolve its API classes. Addons are
classified through tags/configuration first; direct addon API links require a
specific demonstrated need and a compatibility test.

### 3.3 Server authority

The server owns stats, XP, perks, stamina, learned-spell eligibility, selected
spell, mana spending, cooldown validation, target validation, and cast
execution. Clients send intentions and render synchronized results. No client
packet may directly assign progression state or bypass a prerequisite.

Network messages use explicit protocol versions, bounded payloads, stable
string/resource identifiers, main-thread handling, and rate limits for
player-triggered actions.

## 4. Delivery order

### Milestone 0 — protect the foundation

- retain green unit tests and clean-build verification;
- add a dedicated-server smoke launch and JAR content inspection;
- establish versioned configuration and save-data schema constants;
- document all supported runtime mod versions.

### Milestone 1 — stats that visibly matter

- implement attribute/passive effects for the 23 stats;
- make curves and caps server-configurable with validated defaults;
- make application idempotent using stable modifier UUIDs;
- recalculate on login, respawn, dimension change, stat mutation, and config
  reload without stacking modifiers;
- add localized level-up feedback and an unobtrusive stats HUD/screen.

No permanent potion effect is used when a Forge attribute modifier can express
the effect safely.

### Milestone 2 — unified stamina and Epic Fight boundary

- implement standalone stamina before connecting Epic Fight;
- expose a small internal stamina service instead of sharing mutable storage;
- bridge Epic Fight only when present and prevent double charging/recovery;
- validate common weapons plus addon weapons through public tags and an honest
  compatibility matrix;
- keep combat functional when an addon lacks a dedicated classification.

### Milestone 3 — Iron's Spells progression

- target the modpack's pinned Iron's Spells 3.16.2 API first;
- award magical and elemental XP from validated spell events;
- map schools to the four affinity stats through data, not hard-coded addon
  class names;
- scale spell power, casting speed, mana, resistance, and recovery with bounded
  formulas;
- synchronize one coherent mana/stat snapshot after each accepted mutation;
- test base Iron's Spells alone before enabling the validated addon batch.

### Milestone 4 — Curios grimoire and equipment-independent casting

The required interaction contract is exact:

1. Right-clicking while holding the grimoire lets its normal Curios behavior
   place it in the intended Curios slot.
2. Right-clicking with an empty hand does not cast, even if a grimoire is in
   that slot.
3. Pressing the configured current-spell key (default `V`) resolves the active
   spellbook from the Curios slot when no cast-capable book is held.
4. A non-target spell casts once when all normal Iron's Spells rules pass.
5. A target-required spell uses the target selected for that same key press
   and casts once; target acquisition must not consume or cancel the cast.
6. Invalid target, cooldown, mana, silence, missing learned spell, or missing
   grimoire rejects the action safely and returns localized feedback.
7. A held cast-capable item follows one deterministic precedence rule over the
   Curios grimoire; the same key press can never trigger both.

Implementation uses one server-side cast coordinator with the phases
`resolve source -> resolve selected spell -> validate -> acquire/validate
target -> commit mana/cooldown -> invoke cast -> synchronize`. Validation has
no side effects. Resource commitment happens only after all validation and is
rolled back or avoided if invocation cannot start.

Mixins are a last resort. Each necessary mixin must be narrowly targeted,
version-gated, documented with the exact missing API hook, and covered by a
startup compatibility assertion. Broad overwrites are forbidden.

### Milestone 5 — perks and magic tree

- introduce a standalone perk model with stable IDs and defensive persistence;
- make stat levels real prerequisites rather than duplicate progression;
- provide readable physical, utility, and magic branches;
- expose Iron's spell nodes only when their spell IDs exist at runtime;
- keep unlock validation server-side and respec transactions atomic;
- build the native STAT Mod screen first; Puffish Skills support, if retained,
  is an optional presentation/export bridge rather than the source of truth.

### Milestone 6 — medieval crafting and economy

- port forging stations, intermediate materials, recipes, and JEI visibility in
  small vertical slices;
- prefer Forge tags and recipes over recipe-manager or screen mixins;
- keep prices and material gates data-driven;
- make every menu server-validated and duplication-safe.

### Milestone 7 — dungeon and world systems

- port dungeon sessions only after player progression is stable;
- separate generation, session state, encounters, rewards, and recovery;
- use bounded generation work and persistent recovery checkpoints;
- prove create-world, dedicated-server, death, reconnect, and restart safety
  before adding decorative complexity.

## 5. Configuration and compatibility policy

Server configuration owns balance. Defaults form the supported baseline and
invalid values are clamped with one clear warning. Client configuration owns
presentation and key bindings only.

Every supported external mod has a record containing:

- exact tested Minecraft, Forge, mod, and addon versions;
- mandatory dependencies;
- client startup, world creation, dedicated-server startup, and gameplay smoke
  results;
- known partial or incompatible behaviors;
- the responsible STAT Mod integration boundary.

No addon is declared compatible only because Minecraft reaches the title
screen. Runtime validation must exercise its relevant combat, spell, Curios,
recipe, or world behavior.

## 6. Error handling and observability

- Optional integrations fail closed without corrupting core state.
- Missing registry IDs are skipped and reported once, not once per tick.
- Malformed config/NBT entries preserve unrelated valid entries.
- Network rejection messages identify the safe reason without exposing server
  internals.
- Debug logging can trace a cast through every coordinator phase and include
  source, spell ID, target result, and rejection reason without logging every
  normal tick.
- Compatibility failures identify the external mod version and the disabled
  feature rather than producing an unexplained crash later.

## 7. Verification gates for every milestone

Each milestone requires, in order:

1. failing focused tests that describe the new behavior;
2. pure/domain unit tests and malformed-input cases;
3. Forge wiring/contract tests;
4. `gradlew clean test build`;
5. clean-foundation and preserved-addon verification;
6. client startup and new-world creation;
7. dedicated-server startup for server-relevant changes;
8. an updated compatibility record and French/English localization;
9. no unexplained error or fatal line in the newest runtime log.

Iron's Spells and Epic Fight milestones additionally require a minimal test
profile containing only the base mod and dependencies, followed by the full
validated medieval profile. A failure in the full profile is bisected by addon
batch rather than patched blindly.

## 8. Explicit exclusions

- Tensura and all Tensura-derived races, skills, magicules, bridges, wrappers,
  recipes, configs, translations, and save migrations;
- copied NeoForge event/network code that does not match Forge 1.20.1;
- bundled fake API classes for external mods;
- unconditional references to optional-mod classes;
- broad compatibility claims for untested addons;
- restoring the entire old dungeon/economy stack before the core RPG loop is
  playable and stable.

## 9. Definition of the first playable release

The first playable Forge release is reached after Milestone 5 when:

- stats progress, persist, synchronize, and apply visible balanced effects;
- HUD/screen feedback is localized and usable;
- standalone and Epic Fight combat share one stamina model;
- magical stats progress through Iron's Spells;
- a Curios-equipped grimoire casts the current spell with `V`, including
  target-required spells, while empty-hand right-click remains inert;
- the perk/magic tree gates progression without client-side bypass;
- the game starts, creates a world, saves/reloads, respawns, changes dimension,
  and runs on a dedicated server with the validated medieval profile.

Crafting, economy, and dungeons then expand the stable RPG foundation without
blocking that first playable release.
