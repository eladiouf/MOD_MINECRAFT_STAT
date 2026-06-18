# STAT Mod - Stat Identity and Family Design

**Date:** 2026-06-18  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Design approved for spec drafting, not yet implemented

## 1. Goal

Define the target identity of all `STAT Mod` stats so they can be implemented as a coherent long-term progression system across:

- `Epic Fight`
- `ParCool`
- `Tensura`
- `Mahou Tsukai`
- `Overgeared`
- future stamina, survival, and crafting loops

This spec is not a raw balance sheet. It defines what each stat is for, what gameplay loop it should create, what other stats it should naturally combine with, and what mod systems it should influence.

## 2. Core Design Commitments

### 2.1 Design Philosophy

The stat system is **hybrid**:

- grounded enough to feel internally believable
- stylized enough to produce strong builds and recognizable playstyles

Stats are not just numeric modifiers. Each stat must support a gameplay fantasy.

### 2.2 Identity Model

Each stat is designed as a **unique gameplay pillar**, not as a reused template with different numbers.

That means:

- different stats may expose different kinds of bonuses
- not every stat needs the same ratio of passives, thresholds, perks, or integrations
- each stat should be recognizable from gameplay, not only from a tooltip

### 2.3 Progression Shape

All stats use a **continuous progression plus breakpoints** model:

- every level should matter at least a little
- some thresholds should materially change the build
- the most important differentiation should happen in the `25-75` range

### 2.4 Scaling Model

All stats should use **soft caps**, not hard linear dominance.

Expected intent:

- early investment feels rewarding
- mid investment defines the build
- high investment remains valuable but not infinitely efficient

### 2.5 Build Philosophy

The system should reward **hybrid specialization**, not only hard mono-stat stacking.

Desired outcome:

- highly focused builds remain strong
- 2-stat and 3-stat cores are fully viable
- broad all-rounder spreading should be weaker than a deliberate build

### 2.6 Gameplay Target

Primary tuning target:

- `PvE + duel player`

Interpretation:

- PvE remains the main content target
- stats must still avoid degenerate PvP behavior
- anti-fun hard counters should be minimized

### 2.7 Utility Stat Policy

Non-combat stats should have **strong indirect combat value**, not large direct DPS value.

They should matter through:

- preparation
- sustain
- quality
- support
- economy
- progression efficiency

## 3. System-Level Rules

### 3.1 Source of Truth

`STAT Mod` remains the gameplay source of truth for:

- stat levels
- stat-derived modifiers
- stamina ownership
- unlock gating
- cross-mod progression logic

Integrated mods should read from it or be bridged by it.

### 3.2 Magic Ownership

Magical stats must be the canonical control layer for both:

- `Tensura`
- `Mahou Tsukai`

The project must avoid a split where one mod obeys one logic and the other obeys a second one.

### 3.3 Breakpoint Bands

Stats should generally expose meaningful threshold bands around:

- `10` for early definition
- `25` for first real specialization
- `50` for build identity
- `75` for capstone expression

These are design anchors, not a requirement that every stat use the exact same mechanical reward.

### 3.4 Synergy Rule

Synergies should be **structuring**, not decorative.

The intended build language is:

- one clear primary stat
- one strong complement
- optional third stat for refinement

## 4. Family Overview

The stat roster should be read as six families:

1. front-line physical combat
2. ranged and hunt control
3. magical core
4. elemental specialization
5. mental pressure and resilience
6. crafting, provisioning, and technical support

## 5. Front-Line Physical Combat

### 5.1 `BRUTE_FORCE`

**Role:** impact, rupture, heavy commitment offense

`BRUTE_FORCE` is the stat of force conversion:

- heavy weapon damage
- impact and stagger pressure
- guard and posture breaking
- punishing open windows with high burst

It should not grant finesse, accuracy, or tempo quality. Its identity is committing to power and converting that commitment into real threat.

Primary synergies:

- `PHYSICAL_ENDURANCE`
- `INTIMIDATION`
- situationally `AGILITY`

Main mod touchpoints:

- `Epic Fight` impact scaling
- heavy-weapon XP routing
- some intimidation and execution-style perks

### 5.2 `BLADE_TECHNIQUE`

**Role:** duel efficiency, clean melee execution, technical conversion

`BLADE_TECHNIQUE` is the stat of disciplined melee skill:

- blade-specific combat quality
- stronger counters, punishes, and precision strings
- better conversion of technical openings into damage
- superior interaction with blade-oriented `Epic Fight` skills

It should not become a generic crit stat for all physical combat. It belongs to blades, duelists, and technical melee builds.

Primary synergies:

- `RAPIDITE`
- `PRECISION`
- `AGILITY`

Main mod touchpoints:

- sword-family Epic Fight skills
- technical melee perk gates
- blade-focused combat XP routing

### 5.3 `RAPIDITE`

**Role:** offensive tempo, cadence, fluidity

`RAPIDITE` is the stat of attacking faster in a meaningful combat sense:

- attack flow
- recovery between actions
- offensive chain quality
- tempo-based skill usage

It is not world mobility. It is the quality of staying fast inside combat.

Primary synergies:

- `BLADE_TECHNIQUE`
- `AGILITY`
- edge-case hybrid with `CASTING_SPEED`

Main mod touchpoints:

- attack-speed oriented Epic Fight behavior
- combo chaining
- tempo-sensitive combat perks

### 5.4 `AGILITY`

**Role:** movement utility, evasion, placement, body control

`AGILITY` governs:

- dodge quality
- repositioning
- mobility in combat
- air control
- parkour-informed movement expression

It should improve access to better positions and cleaner exits, not simply become a second damage stat.

Primary synergies:

- `RAPIDITE`
- `PRECISION`
- `PHYSICAL_ENDURANCE`

Main mod touchpoints:

- `ParCool`
- dodge and aerial behavior in `Epic Fight`
- mobility-oriented perks

### 5.5 `PHYSICAL_RESISTANCE`

**Role:** physical mitigation, exchange reliability, defensive stability

`PHYSICAL_RESISTANCE` is the stat for reducing the punishment of mistakes:

- incoming physical damage reduction
- better ability to hold a line
- better guard and trade reliability
- stronger durability in repeated exchanges

It should not own stamina, sustain pacing, or mental resilience. It is the armor-side answer to physical danger.

Primary synergies:

- `PHYSICAL_ENDURANCE`
- `WILLPOWER`
- `BRUTE_FORCE`

Main mod touchpoints:

- defense-side Epic Fight modifiers
- tank-oriented passive effects
- front-line perk branches

### 5.6 `PHYSICAL_ENDURANCE`

**Role:** stamina anchor plus long-fight durability

`PHYSICAL_ENDURANCE` is a hybrid stat:

- source stat for unified stamina scaling
- resilience over long encounters
- fatigue tolerance
- recovery quality
- general front-line staying power

It must support both bruisers and tanks without becoming mandatory for every build.

Primary synergies:

- `BRUTE_FORCE`
- `PHYSICAL_RESISTANCE`
- `AGILITY`
- indirect sustain with `COOKING`

Main mod touchpoints:

- unified stamina capacity and recovery
- sleep, food, and meditation loops
- `ParCool` sustained movement support

## 6. Ranged and Hunt Control

### 6.1 `PRECISION`

**Role:** hit quality, ranged conversion, exactness

`PRECISION` governs the ability to convert a line of fire or a technical opening into reliable payoff:

- ranged damage efficiency
- weak-point or critical conversion
- cleaner hit confirmation
- more exact offensive payoff on technical builds

It should stay separate from `TRACKING` and `KEEN_SENSES`. `PRECISION` is about execution, not detection or awareness.

Primary synergies:

- `TRACKING`
- `KEEN_SENSES`
- `BLADE_TECHNIQUE`

Main mod touchpoints:

- bows, spears, tridents, and ranged Epic Fight families
- ranged perks
- hunter-style combat branches

### 6.2 `TRACKING`

**Role:** target acquisition, pursuit, maintained information

`TRACKING` should help the player:

- find targets
- keep advantage on marked prey
- maintain pursuit value
- exploit a target already identified

This is not accuracy. It is information retention and pursuit dominance.

Primary synergies:

- `PRECISION`
- `KEEN_SENSES`
- `INTIMIDATION`

Main mod touchpoints:

- mark systems
- pursuit and scouting perks
- visibility and prey-oriented utility

### 6.3 `KEEN_SENSES`

**Role:** awareness, reaction, anticipatory defense

`KEEN_SENSES` is the perception stat:

- immediate danger reading
- reaction quality
- dodge-adjacent awareness
- tactical clarity under pressure

It should support evasion and alertness, not simply copy `AGILITY`.

Primary synergies:

- `AGILITY`
- `TRACKING`
- `WILLPOWER`
- `PRECISION`

Main mod touchpoints:

- reaction and evasion perks
- perception utility
- scout and assassin archetypes

## 7. Magical Core

### 7.1 `ARCANE_POWER`

**Role:** universal offensive magic baseline

`ARCANE_POWER` is the base offensive stat shared by all casters:

- spell damage
- offensive pressure
- baseline spell threat
- partial scaling of hostile magical effects

It should not define the element or discipline. It defines how dangerous the caster is in general.

Primary synergies:

- all affinities
- `CASTING_SPEED`
- `ERUDITION`

Main mod touchpoints:

- `Tensura`
- `Mahou Tsukai`
- cross-school magical perks

### 7.2 `CASTING_SPEED`

**Role:** magical tempo and responsiveness

`CASTING_SPEED` governs:

- execution speed
- spell cadence
- responsiveness under pressure
- combo-like spell fluidity

It is the tempo axis of magic, not the endurance axis.

Primary synergies:

- `ARCANE_POWER`
- `AIR_AFFINITY`
- `FIRE_AFFINITY`
- edge-case melee-mage hybrids

Main mod touchpoints:

- cast pacing in `Tensura`
- cast responsiveness in `Mahou Tsukai`
- spell-chain expression

### 7.3 `MANA_POOL`

**Role:** magical endurance and sustained output

`MANA_POOL` supports:

- longer casting windows
- higher total magical workload
- more stable sustained combat
- better tolerance for expensive magical loops

It should not be a hidden damage stat. It is a durability stat for casters.

Primary synergies:

- `WATER_AFFINITY`
- `EARTH_AFFINITY`
- `ERUDITION`
- sometimes `PHYSICAL_ENDURANCE`

Main mod touchpoints:

- mana or mana-like reserve systems
- long-loop magical combat
- sustain casting builds

### 7.4 `ERUDITION`

**Role:** magical access, mastery quality, breadth

`ERUDITION` is a knowledge and control stat:

- unlock access
- better use of learned systems
- more variety and depth
- reduced practical friction in magical play

It is not a raw power stat. It is the stat that turns a caster from narrow to sophisticated.

Primary synergies:

- all magical paths
- especially `MANA_POOL`
- `CASTING_SPEED`
- `WILLPOWER`

Main mod touchpoints:

- advanced spell gates
- magical perk requirements
- multi-school and mastery systems

### 7.5 `MAGIC_RESISTANCE`

**Role:** technical anti-magic defense

`MAGIC_RESISTANCE` should protect against more than damage:

- magical damage reduction
- better resistance to hostile magical effects
- improved stability against magical control
- defensive value against technical casters

It should be strong without becoming a hard invalidation stat.

Primary synergies:

- `WILLPOWER`
- `EARTH_AFFINITY`
- `WATER_AFFINITY`
- `PHYSICAL_RESISTANCE`

Main mod touchpoints:

- `Tensura`
- `Mahou Tsukai`
- anti-caster preparation

## 8. Elemental Specialization

Elemental affinities serve two roles:

- amplify their element strongly
- gate access to advanced spells and perks of that element

### 8.1 `FIRE_AFFINITY`

**Role:** offensive fire specialization

`FIRE_AFFINITY` should reinforce:

- destructive fire magic
- pressure over time
- aggressive spell lines
- high-risk offensive identity

Primary synergies:

- `ARCANE_POWER`
- `CASTING_SPEED`
- secondary `WILLPOWER`

### 8.2 `WATER_AFFINITY`

**Role:** sustain, support, adaptive control

`WATER_AFFINITY` should reinforce:

- healing or restorative lines
- flexible control
- defensive support
- stabilizing magical play

Primary synergies:

- `MANA_POOL`
- `ERUDITION`
- `WILLPOWER`
- `MAGIC_RESISTANCE`

### 8.3 `EARTH_AFFINITY`

**Role:** structure, defense, zone control

`EARTH_AFFINITY` should reinforce:

- barriers
- anchored magic
- gravity, wall, and control-space tools
- stable long-form spellcasting

Primary synergies:

- `MANA_POOL`
- `MAGIC_RESISTANCE`
- `PHYSICAL_ENDURANCE`
- `WILLPOWER`

### 8.4 `AIR_AFFINITY`

**Role:** speed, reach, mobility magic

`AIR_AFFINITY` should reinforce:

- fast or evasive magical play
- reach and pressure projection
- mobility spells
- lightning, wind, and dynamic repositioning styles

Primary synergies:

- `CASTING_SPEED`
- `AGILITY`
- `ARCANE_POWER`
- light `RAPIDITE` hybrids

## 9. Mental Pressure and Resilience

### 9.1 `INTIMIDATION`

**Role:** psychological pressure and dominance conversion

`INTIMIDATION` should make advantage feel heavier:

- mark pressure
- enemy hesitation or collapse
- stronger conversion on pressured targets
- morale-style control in PvE

It should remain useful in duels without turning into unfair hard disable spam.

Primary synergies:

- `BRUTE_FORCE`
- `TRACKING`
- `WILLPOWER`
- sometimes `PHYSICAL_ENDURANCE`

Main mod touchpoints:

- fear, dominance, and prey pressure perks
- execution and control-side front-line builds

### 9.2 `WILLPOWER`

**Role:** inner stability, anti-control, pressure tolerance

`WILLPOWER` is the stat of staying functional:

- resisting disruption
- holding performance under pressure
- reducing control vulnerability
- supporting magical discipline

It must stay distinct from `MAGIC_RESISTANCE`: `WILLPOWER` is about self-coherence, not just magical defense.

Primary synergies:

- `PHYSICAL_RESISTANCE`
- `PHYSICAL_ENDURANCE`
- `MAGIC_RESISTANCE`
- `ERUDITION`

Main mod touchpoints:

- anti-stun and anti-control layers
- concentration-like magical behavior
- shield, guard, and mental resilience builds

## 10. Crafting, Provisioning, and Technical Support

### 10.1 `FORGING`

**Role:** equipment quality, workshop progression, material optimization

`FORGING` should improve:

- repair quality
- crafted gear quality
- workshop access
- technical progression through equipment work

Its combat value should remain indirect but meaningful through better tools, better gear, and better long-term reliability.

Primary synergies:

- `ERUDITION`
- `ALCHEMY`
- `PHYSICAL_ENDURANCE`

Main mod touchpoints:

- `Overgeared`
- vanilla/anvil-like item preparation
- technical crafting perks

### 10.2 `COOKING`

**Role:** nutrition strategy, stamina support, team sustain

`COOKING` should improve:

- food efficiency
- quality of nourishment
- daily sustain loops
- support and provisioning before long outings

It should have major indirect value for the unified stamina fantasy without replacing sleep or meditation.

Primary synergies:

- `PHYSICAL_ENDURANCE`
- `WATER_AFFINITY`
- `ALCHEMY`
- `WILLPOWER`

Main mod touchpoints:

- food effects
- stamina restoration loops
- support-oriented preparation

### 10.3 `ALCHEMY`

**Role:** prepared effects, technical utility, consumable optimization

`ALCHEMY` should improve:

- duration and quality of prepared effects
- technical flexibility
- buff or resistance planning
- situational solutions through consumables

It should feel more calculated and technical than `COOKING`.

Primary synergies:

- `ERUDITION`
- `COOKING`
- `MAGIC_RESISTANCE`
- `FORGING`
- some `MANA_POOL`

Main mod touchpoints:

- potion-like systems
- support preparation
- utility and anti-threat consumables

## 11. Intended Archetype Language

The stat system should naturally support at least these core build languages:

- bruiser: `BRUTE_FORCE + PHYSICAL_ENDURANCE`
- duelist: `BLADE_TECHNIQUE + RAPIDITE`
- skirmisher: `AGILITY + RAPIDITE + KEEN_SENSES`
- tank: `PHYSICAL_RESISTANCE + PHYSICAL_ENDURANCE + WILLPOWER`
- hunter: `PRECISION + TRACKING + KEEN_SENSES`
- battle mage: `ARCANE_POWER + CASTING_SPEED + chosen affinity`
- control mage: `ERUDITION + MANA_POOL + WATER/EARTH`
- mobile mage: `AIR_AFFINITY + CASTING_SPEED + AGILITY`
- anti-mage: `MAGIC_RESISTANCE + WILLPOWER + chosen front-line stat`
- artisan-support: `FORGING/COOKING/ALCHEMY + one combat or magic anchor`

## 12. Implementation Guidance

This spec intentionally does not freeze exact formulas yet, but it does freeze the intended direction:

- each stat needs a clear passive scaling axis
- each stat should expose meaningful thresholds in the `25-75` band
- stats should not collapse into duplicates
- integrated mods should be mapped by fantasy, not by convenience
- magical stats must remain unified across `Tensura` and `Mahou Tsukai`
- utility stats must remain indirect in combat value

## 13. Scope Notes

This design applies to all `23` current stats in `StatType`.

It does not require every stat to receive the same number of:

- perks
- formulas
- thresholds
- integration hooks

It does require every stat to have:

- a clear role
- a recognized build identity
- deliberate synergy partners
- at least one real gameplay loop it owns

## 14. Next Planning Slice

The next implementation plan should detail, for each family:

1. exact passive formulas
2. soft-cap points
3. threshold rewards
4. perk gating changes
5. mod integration touchpoints
6. tests needed for regression-safe rollout
