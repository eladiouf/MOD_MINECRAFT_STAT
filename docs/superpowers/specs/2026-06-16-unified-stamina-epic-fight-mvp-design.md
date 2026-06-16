# STAT Mod - Unified Stamina and Epic Fight MVP Design

**Date:** 2026-06-16  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Design approved for spec drafting, not yet implemented

## 1. Goal

Define a new V1 gameplay foundation for combat progression built on:

- a unified stamina system owned by `STAT Mod`
- native `Epic Fight` combat skill UX for the first MVP
- `STAT Mod` stat requirements as the unlock gate
- an Overworld day/night cycle tuned for longer in-world days

This design intentionally does **not** implement `Mahou Tsukai` integration or `Pufferfish's Skills` yet. Those remain future phases.

## 2. Scope Decisions

### In Scope for V1

- unified stamina resource inside `STAT Mod`
- stamina bridge to `Epic Fight` UI and skill usage
- `Epic Fight`-native unlock/equip/use flow
- 5 combat skills for the first MVP
- unlock requirements driven by `STAT Mod` levels
- fatigue thresholds with malus and hard blocks
- meditation-as-seated recovery mode
- Overworld-only custom time controller
- 48-minute full day/night cycle
- semi-realistic sleep recovery

### Out of Scope for V1

- `Mahou Tsukai` spell UX and stamina bridge
- `Pufferfish's Skills` integration
- `Pufferfish's Attributes` integration beyond design inspiration
- a brand-new custom combat HUD
- replacing Epic Fight slots with a custom slot bar
- non-Overworld custom time control

## 3. High-Level Architecture

The MVP uses `Epic Fight` as the player-facing combat layer and `STAT Mod` as the systems layer.

### Epic Fight responsibilities

- existing combat skill UI
- existing combat skill slots
- existing combat keybinds
- runtime activation flow for the selected combat skills

### STAT Mod responsibilities

- source of truth for stamina and fatigue
- source of truth for unlock prerequisites
- stamina costs, regeneration, and activity drain
- low/critical stamina penalties
- meditation and sleep recovery logic
- custom Overworld day/night pacing

### Integration principle

There must be only one authoritative stamina value. That value belongs to `STAT Mod`.  
`Epic Fight` reads from it and displays it through a bridge.

## 4. Unified Stamina System

### 4.1 Ownership

Unified stamina is stored in `STAT Mod`, not in `Epic Fight`.

This avoids making a combat mod the source of truth for systems that later need to serve:

- Epic Fight combat
- Mahou spellcasting
- movement and parkour
- daily fatigue gameplay

### 4.2 Stored Data

V1 stores a lightweight but extensible stamina state:

- `currentStamina`
- `fatigueDebt`
- `meditationState`

The following values are derived at runtime:

- `maxStamina`
- low-stamina state
- critical-stamina state
- temporary consumption modifiers

### Rationale

`currentStamina` alone is too weak for a realistic day-based system.  
`fatigueDebt` allows the design to reflect accumulated exhaustion without permanently storing every temporary activity signal.

### 4.3 Maximum Stamina

Maximum stamina uses a mixed model:

- a fixed base shared by all players
- bonuses derived primarily from `STAT Mod` stats

V1 expectation:

- `PHYSICAL_ENDURANCE` is the main contributor
- secondary stat scaling is optional and should stay conservative in V1

### 4.4 Drain Model

Stamina decreases:

- very slowly at rest
- significantly faster from activity

V1 activity drain applies to all of the following groups:

- combat actions
- movement actions
- special movement actions
- combat skill usage

Examples:

- attacking
- guarding
- sprinting
- dodging
- parkour actions
- activating Epic Fight skills

### 4.5 Recovery Model

V1 recovery uses four channels:

- passive natural regeneration
- food
- meditation
- sleep

Recovery ranking:

1. sleep
2. meditation
3. food
4. passive natural regeneration

### Passive natural regeneration

- always available
- intentionally slow
- never strong enough to replace the dedicated recovery actions

### Food

- medium recovery
- useful during normal play
- should help stabilize the player but not trivialize fatigue

### Meditation

- implemented as a seated mode
- stronger than food
- weaker than full sleep
- intended for controlled recovery outside immediate danger

### Sleep

- semi-realistic model
- restores some stamina during the sleep process
- gives a larger refill on wake-up

### 4.6 Threshold Rules

The stamina system has three gameplay states:

- normal
- low
- critical

### Normal state

- no stamina-related penalties

### Low state

Apply progressive penalties to both:

- combat performance
- movement performance

Examples of acceptable V1 malus targets:

- attack efficiency
- guard efficiency
- dodge responsiveness
- sprint performance
- jump or mobility effectiveness

### Critical state

Hard-block the most intensive actions while still leaving the player functional.

Blocked in V1:

- skills
- sprint
- dodge
- parkour or special movement

Still allowed in V1:

- walking
- basic attacks

Basic attacks remain available but should still suffer low-stamina penalties.

## 5. Epic Fight MVP

### 5.1 UX Direction

The first combat MVP is `Epic Fight`-first.

That means:

- unlock and equip flow stays in native `Epic Fight`
- no custom combat tree screen in `STAT Mod`
- no new combat slot bar
- no `Pufferfish's Skills` in V1

This is the fastest and least risky route to a stable replacement-style combat MVP.

### 5.2 Skill Count

V1 targets **5 Epic Fight combat skills**.

The set should be a mix of:

- offense
- mobility
- defense or counterplay

The exact five skills are not frozen in this document, but the selection must cover more than one combat role so the full unlock-cost-use loop is proven.

### 5.3 Unlock Logic

Epic Fight remains the visible unlock/equip layer, but `STAT Mod` controls whether a skill is eligible to unlock.

### Rules

- unlock is manual, not automatic
- unlock is free in the UI
- the cost is paid at runtime through stamina usage
- each skill uses a coherent set of required `STAT Mod` levels
- some skills may need one stat, others may need multiple stats

### Design principle

The requirement model should prioritize coherence over strict uniformity.

Examples:

- a blade-oriented skill may rely mainly on `BLADE_TECHNIQUE`
- a mobility attack may rely on `AGILITY` plus `RAPIDITE`
- a heavy defensive skill may rely on `PHYSICAL_ENDURANCE` plus `WILLPOWER`

### 5.4 Runtime Cost Model

Every MVP combat skill consumes unified stamina from `STAT Mod`.

If the player lacks enough stamina:

- the skill is blocked in critical cases
- or penalized according to threshold rules if that produces clearer gameplay

The precise rule may vary by skill category, but the general expectation for V1 is that high-cost combat skills should not fire at critical exhaustion.

### 5.5 UI and Inputs

V1 uses the native Epic Fight systems:

- existing Epic Fight slots
- existing Epic Fight keybinds
- existing Epic Fight combat screen

`STAT Mod` must not add a competing combat input bar for this MVP.

## 6. Meditation

Meditation is a dedicated seated recovery mode.

### V1 expectations

- player enters a seated or meditative state
- movement and combat behavior should be restricted while meditating
- recovery is materially better than passive regen and food
- meditation should break when the player is forced back into activity

The primary purpose is to give players a deliberate non-sleep recovery loop that fits the stamina fantasy.

## 7. Time and Sleep

### 7.1 Day/Night Cycle

V1 changes the Overworld day/night pace only.

Target full cycle:

- **48 real minutes total**
- **32 real minutes day**
- **16 real minutes night**

### 7.2 Implementation Direction

Do not change server TPS.

Instead:

- keep normal game tick speed
- add a `STAT Mod` server-side time controller
- change the rate at which Overworld `dayTime` advances depending on whether it is day or night

### Rationale

This is safer for compatibility than changing the global simulation rate.

### 7.3 Dimension Scope

V1 scope is:

- Overworld only

No custom pacing is required yet for:

- Nether
- End
- modded dimensions

### 7.4 Sleep Model

Sleep is semi-realistic.

### Single-player or local gameplay expectation

- sleeping provides recovery during the process
- waking gives an additional strong refill
- time advances rapidly toward morning

### Multiplayer expectation

- recovery can still be individual
- rapid night skip should only happen when the group conditions are met

This preserves recognizable Minecraft sleep expectations while still supporting the stamina fantasy.

## 8. Failure Handling and Edge Cases

V1 must define consistent fallback behavior for the following cases:

- Epic Fight loaded but stamina bridge temporarily unavailable
- player attempts to use a skill while stamina is critical
- player enters meditation while in an invalid activity state
- player sleeps in multiplayer without enough sleepers to advance the night
- player logs out or changes dimension while meditating

Expected direction:

- fail safe
- do not duplicate stamina sources
- do not silently grant free skill usage
- prefer blocking or canceling over desynchronizing

## 9. Testing Strategy

V1 should be verified at three levels.

### Unit-level

- stamina state transitions
- derived max stamina calculation
- threshold classification
- per-skill prerequisite resolution
- time conversion logic for the 32/16 cycle

### Integration-level

- Epic Fight unlock gating from `STAT Mod` stat levels
- Epic Fight stamina consumption bridged from `STAT Mod`
- meditation entry and exit behavior
- sleep recovery behavior
- Overworld day/night pacing

### Runtime validation

- client launch with Epic Fight loaded
- skill unlock and equip through native Epic Fight UI
- stamina drain in combat, sprint, dodge, and mobility
- critical threshold action blocking
- sleep and meditation recovery checks

## 10. Deferred Work

The following items are intentionally deferred beyond V1:

- `Mahou Tsukai` spell integration on unified stamina
- `Pufferfish's Skills` trees
- deeper `Pufferfish's Attributes` adoption
- custom `STAT Mod` skill tree screen
- non-combat stamina-consuming systems beyond the first bridge layer

## 11. Recommended Next Planning Slice

Implementation planning should break the work into at least these slices:

1. stamina data model, sync, and thresholds
2. Epic Fight bridge for display and consumption
3. unlock requirement resolver for 5 MVP skills
4. meditation mode
5. Overworld time controller and sleep recovery
6. verification pass with combat runtime checks
