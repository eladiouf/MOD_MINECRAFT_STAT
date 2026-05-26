# Combat Framework Mod — Complete Technical & Design Documentation

## Project Vision

Create a deep combat progression framework for Minecraft Forge 1.20.1 built around Epic Fight.

The mod is NOT intended to be:
- a generic RPG mod
- a stat inflation simulator
- a menu-heavy MMORPG system

The mod IS intended to become:
- a combat mastery framework
- an immersive progression system
- a skill-expression combat experience
- a scalable RPG foundation for future expansions

Core philosophy:

> Practice creates mastery.

Players evolve through actions, combat experience, adaptation, and technique.

---

# Core Design Pillars

## 1. Organic Progression

Players improve stats naturally by performing actions.

Examples:
- Heavy attacks improve Force
- Perfect dodges improve Agility
- Parries improve Reflex
- Fire spells improve Fire Affinity
- Forging improves Smithing

No traditional:
- “+5 stat points per level”
- artificial class systems
- forced builds

---

## 2. Skill Expression

Combat rewards:
- timing
- positioning
- adaptation
- precision
- combo mastery

NOT:
- damage spam
- stat stacking
- passive gameplay

---

## 3. Emergent Builds

No classes.

The player creates their identity through actions.

Examples:
- Aggressive greatsword fighter
- Agile duelist
- Defensive counter specialist
- Elemental battlemage
- Alchemical assassin

---

## 4. Mechanical Progression

Stats should unlock mechanics.

BAD progression:
- +1 damage
- +2 stamina
- +3 crit

GOOD progression:
- dodge cancels
- combo extensions
- posture counters
- aerial finishers
- stance switching
- perfect guard mechanics

---

# Technical Environment

## Minecraft Version

- Forge 1.20.1

## Java Version

- Java 17

## IDE

Recommended:
- IntelliJ IDEA

---

# Required Libraries & Dependencies

## Core Dependencies

### Epic Fight
Purpose:
- Base combat framework
- Animation combat system
- Advanced melee mechanics

### GeckoLib
Purpose:
- Custom animations
- Combat techniques
- Boss animations
- Spell animations

### Mixin
Purpose:
- Modify internal combat logic
- Hook into Epic Fight systems
- Advanced behavior injection

### Curios API
Purpose:
- Accessories
- Talismans
- Combat relics
- Magic artifacts

### Cloth Config
Purpose:
- Mod configuration UI
- Balancing options
- Debug tools

---

# Recommended Project Structure

```text
src/main/java/com/project/combatframework/
│
├── CombatFrameworkMod.java
│
├── capability/
│   ├── player/
│   ├── mana/
│   └── stamina/
│
├── combat/
│   ├── posture/
│   ├── stamina/
│   ├── combos/
│   ├── parry/
│   ├── dodge/
│   └── flow/
│
├── progression/
│   ├── stats/
│   ├── xp/
│   ├── mastery/
│   ├── unlocks/
│   └── scaling/
│
├── skills/
│   ├── active/
│   ├── passive/
│   └── weapon/
│
├── magic/
│   ├── mana/
│   ├── affinities/
│   ├── spells/
│   └── casting/
│
├── network/
│   ├── packets/
│   └── sync/
│
├── client/
│   ├── hud/
│   ├── gui/
│   ├── rendering/
│   └── animation/
│
├── registry/
├── util/
├── config/
└── data/
```

---

# Core Gameplay Systems

# 1. Stat System

## Philosophy

Stats represent:
- combat experience
- physical adaptation
- magical specialization
- mental resilience

Stats must:
- visibly impact gameplay
- unlock mechanics
- scale naturally
- remain understandable

---

# Combat Stats

| Stat | Description |
|---|---|
| Force | Heavy impact and posture damage |
| Technique | Combo mastery and advanced combat skills |
| Agility | Movement speed and dodge effectiveness |
| Endurance | Stamina pool and recovery |
| Precision | Critical accuracy and weak-point strikes |
| Reflex | Perfect dodge/parry timing |
| Resistance | Physical damage mitigation |

---

# Arcane Stats

| Stat | Description |
|---|---|
| Arcane Power | Overall spell strength |
| Mana Control | Casting efficiency and mana management |
| Magic Resistance | Resistance against magical effects |
| Fire Affinity | Fire specialization |
| Water Affinity | Water specialization |
| Earth Affinity | Earth specialization |
| Air Affinity | Wind specialization |
| Knowledge | Unlock advanced magical techniques |

---

# Survival Stats

| Stat | Description |
|---|---|
| Instinct | Detection and tracking |
| Adaptation | Environmental resilience |

---

# Crafting Stats

| Stat | Description |
|---|---|
| Smithing | Weapon and armor crafting quality |
| Alchemy | Potions, oils, toxins |
| Cooking | Food buffs and efficiency |

---

# Mental Stats

| Stat | Description |
|---|---|
| Willpower | Resistance against fear and stagger |
| Presence | Aura and intimidation effects |

---

# 2. Organic Progression System

## Core Principle

Actions increase related stats.

No traditional class leveling.

---

# Combat Growth Examples

| Action | Stat Increased |
|---|---|
| Heavy attacks | Force |
| Combo variety | Technique |
| Perfect dodges | Agility |
| Long combat sessions | Endurance |
| Headshots/weak points | Precision |
| Perfect parries | Reflex |
| Tanking damage | Resistance |

---

# Magic Growth Examples

| Action | Stat Increased |
|---|---|
| Spell usage | Arcane Power |
| Continuous casting | Mana Control |
| Receiving magic damage | Magic Resistance |
| Fire spells | Fire Affinity |
| Water spells | Water Affinity |
| Earth spells | Earth Affinity |
| Wind spells | Air Affinity |
| Discovering ancient knowledge | Knowledge |

---

# Crafting Growth Examples

| Action | Stat Increased |
|---|---|
| Forging equipment | Smithing |
| Brewing potions | Alchemy |
| Cooking meals | Cooking |

---

# 3. XP Scaling Formula

## Base Formula

XP Required:

XP = 100 × Level^1.5

Purpose:
- Fast early progression
- Slower high-level mastery
- Long-term scaling

---

# Diminishing Returns System

Prevent repetitive grinding.

Formula:

Effective XP = Base XP / (1 + Repetition Factor)

Examples:
- Repeating same action reduces gains
- Fighting weak enemies gives reduced XP
- Variety gives bonuses

---

# 4. Posture System

## Core Combat Mechanic

Every entity has posture.

Posture represents:
- combat balance
- defensive stability
- pressure tolerance

---

# Posture Mechanics

Actions causing posture damage:
- heavy attacks
- perfect counters
- charged attacks
- combo finishers

When posture reaches zero:
- enemy staggered
- execution vulnerability
- combo opportunity

---

# Posture Formula

Posture Break Threshold = Force + Weapon Impact - Enemy Stability

---

# 5. Stamina System

## Purpose

Prevent infinite spam.

Creates:
- combat pacing
- tactical choices
- tension
- risk management

---

# Stamina Consumption

| Action | Cost |
|---|---|
| Dodge | Medium |
| Sprint attack | Medium |
| Heavy attack | High |
| Block | Low continuous |
| Spell casting | Variable |

---

# Stamina Recovery

Recovery reduced while:
- attacking
- blocking
- sprinting
- taking damage

Recovery enhanced by:
- Endurance
- resting
- successful defensive timing

---

# 6. Weapon Mastery System

## Philosophy

Weapons evolve through use.

Each weapon category has:
- mastery XP
- unlockable techniques
- advanced combos
- passive bonuses

---

# Planned Weapon Categories

| Weapon Type | Style |
|---|---|
| Greatsword | Heavy posture breaking |
| Katana | Precision and counters |
| Spear | Reach and spacing |
| Dual Blades | Aggressive combo chains |
| Fists | Mobility and counters |
| Sword & Shield | Defensive control |

---

# Example Unlocks

## Katana

### Mastery 10
- Dash Slash

### Mastery 20
- Recovery Cancel

### Mastery 35
- Perfect Counter

### Mastery 50
- Iaijutsu Stance

---

# 7. Flow State System

## Purpose

Reward skilled combat.

Flow State represents:
- combat rhythm
- confidence
- momentum

---

# Combat Rank System

Ranks:
- D
- C
- B
- A
- S
- SS

Based on:
- combo variety
- damage avoidance
- aggression
- timing precision
- posture breaks

---

# Flow State Benefits

At high combat rank:
- faster recovery
- smoother combos
- enhanced movement
- cinematic effects
- advanced finishers

---

# 8. Skill Unlock System

## Philosophy

Progression unlocks mechanics.

NOT passive number inflation.

---

# Unlock Examples

| Requirement | Unlock |
|---|---|
| Reflex 20 | Perfect Dodge |
| Technique 30 | Combo Cancel |
| Force 40 | Guard Break |
| Fire Affinity 25 | Burning Weapon |
| Endurance 35 | Stamina Surge |

---

# Hidden Unlocks

Secret techniques unlocked through behavior.

Example:
- 500 perfect parries
- unlock Counter Specialist

Purpose:
- discovery
- replayability
- mastery depth

---

# 9. Magic System

## Design Philosophy

Magic should feel:
- dangerous
- specialized
- learned
- powerful

NOT:
- spammy MMO casting

---

# Core Systems

## Mana

Used for:
- spell casting
- magical techniques
- enchantments

---

## Affinities

Using elemental magic increases affinity.

Higher affinity:
- stronger effects
- advanced spells
- lower mana costs
- elemental passives

---

# Future Elements

Possible future additions:
- Light
- Shadow
- Void
- Blood
- Lightning
- Frost

---

# 10. Enemy AI Philosophy

Enemies should feel:
- reactive
- dangerous
- intelligent

NOT:
- health sponges

---

# Planned AI Behaviors

Enemies can:
- dodge attacks
- punish spam
- block intelligently
- react to posture pressure
- exploit openings
- interrupt spellcasting

---

# Boss Design Philosophy

Bosses should test:
- timing
- positioning
- stamina management
- adaptability

NOT:
- excessive health
- unavoidable attacks

---

# 11. Data-Driven Design

## CRITICAL SYSTEM

Most progression should be configurable via JSON.

Purpose:
- easier balancing
- future expansions
- addon compatibility
- datapack support

---

# Example Skill JSON

```json
{
  "skill": "perfect_counter",
  "required_stat": "reflex",
  "required_level": 30,
  "weapon_type": "katana"
}
```

---

# Example Weapon Mastery JSON

```json
{
  "weapon": "greatsword",
  "xp_multiplier": 1.2,
  "unlocks": [
    "guard_break",
    "earthsplitter"
  ]
}
```

---

# 12. Networking System

## Purpose

Minecraft uses:
- server authority
- client rendering

All progression must sync correctly.

---

# Required Packets

| Packet | Purpose |
|---|---|
| SyncStatsPacket | Sync player stats |
| LevelUpPacket | Trigger level-up events |
| FlowStatePacket | Update combat rank |
| SkillUnlockPacket | Unlock abilities |
| StaminaPacket | Update stamina HUD |
| PosturePacket | Update posture HUD |

---

# 13. HUD & UI Design

## Design Philosophy

UI should be:
- clean
- immersive
- readable
- minimalist

Avoid MMO-style clutter.

---

# Planned HUD Elements

| Element | Purpose |
|---|---|
| Stamina Bar | Combat resource |
| Posture Bar | Stability meter |
| Flow Meter | Combat momentum |
| Rank Display | Style rank |
| Skill Notification | Technique unlocks |

---

# Planned Menus

| Menu | Purpose |
|---|---|
| Character Screen | Stats overview |
| Weapon Mastery Screen | Weapon progression |
| Skill Archive | Learned techniques |
| Magic Codex | Spells and affinities |

---

# 14. Performance Guidelines

## VERY IMPORTANT

Combat mods can destroy performance.

---

# Avoid

- scanning all entities every tick
- excessive particles
- unnecessary packet spam
- heavy loops
- expensive calculations every frame

---

# Recommended Optimization

- cache calculations
- event-driven systems
- cooldown-based checks
- lightweight HUD rendering
- async-safe data handling

---

# 15. Development Roadmap

# PHASE 1 — Core Foundation

Goals:
- player stat capability
- XP system
- networking
- save/load
- basic HUD

---

# PHASE 2 — Organic Progression

Goals:
- action tracking
- stat gains
- anti-farm system
- soft caps

---

# PHASE 3 — Epic Fight Integration

Goals:
- combat hooks
- posture system
- stamina system
- dodge/parry tracking

---

# PHASE 4 — Weapon Mastery

Goals:
- weapon XP
- unlockable techniques
- advanced combo mechanics

---

# PHASE 5 — Flow State

Goals:
- combat ranking
- stylish combat bonuses
- momentum system

---

# PHASE 6 — Magic System

Goals:
- mana
- affinities
- casting
- elemental specialization

---

# PHASE 7 — Advanced AI

Goals:
- adaptive enemies
- combat intelligence
- advanced bosses

---

# PHASE 8 — World Expansion

Goals:
- factions
- arenas
- dungeons
- trainers
- legendary weapons

---

# 16. Naming Philosophy

The mod name should feel:
- memorable
- thematic
- serious
- combat-oriented

Avoid:
- generic RPG names
- anime clichés
- overly long names

---

# Example Name Directions

| Name | Style |
|---|---|
| Blade Doctrine | Martial philosophy |
| Ashen Path | Dark fantasy |
| Flowstate | Stylish combat |
| Echelon | Elite progression |
| Path of Steel | Warrior mastery |
| Ascension | Growth and evolution |

---

# 17. Long-Term Vision

The final goal is NOT simply:
- adding combat stats

The final goal is:

## creating the definitive combat progression framework for Epic Fight.

A system where:
- every battle matters
- every action teaches the player
- mastery feels earned
- combat feels personal
- builds emerge naturally
- progression feels immersive

---

# Final Development Advice

## Start Small

First milestone should ONLY include:
- stats
- XP gains
- stamina
- posture
- one weapon mastery

Complete and polish that FIRST.

---

## Prioritize Feel Over Content

A small polished combat system is better than:
- 100 unfinished features
- bloated systems
- shallow progression

---

## Design For Scalability

Everything should support:
- future addons
- balancing
- datapacks
- multiplayer
- expansion mods

---

# Ultimate Design Goal

The player should feel:

> “I became stronger because I mastered combat.”

NOT:

> “I became stronger because numbers increased.”

