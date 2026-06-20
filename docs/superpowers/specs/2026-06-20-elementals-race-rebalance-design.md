# Elementals Race Rebalance Design

Date: 2026-06-20
Project: `STAT Mod` on NeoForge 1.21.1
Scope: Rebalance the `Elementals` mage progression around the four supported Tensura races, preserve a strong racial identity in early game, and support a late-game universal mage path without flattening race differences.

## Goals

- Keep only four supported mage races for `Elementals` integration:
  - `tensura:elf`
  - `tensura:human`
  - `tensura:dwarf`
  - `tensura:beastfolk`
- Preserve the native `Elementals` UX, keybinds, and branch UI.
- Keep `STAT Mod` as the progression authority for unlock logic.
- Make race identity matter strongly in early and mid game.
- Keep late-game convergence possible, but expensive.
- Expand rare-branch design to include `metal` alongside `lightning` and `blood`.

## Design Direction

The balance model is `semi-rigid`.

- Early game should feel clearly racial and specialized.
- Mid game should allow controlled expansion into a third base branch.
- Late game should allow partial convergence into a broader mage identity.
- Humans should be the best late universal mage candidates.
- Elves and dwarves should be the strongest specialized mage candidates.
- Beastfolk should remain viable, but should not out-compete dedicated caster races on pure magical ceiling.

## Race Identities

### Elf

- Starter branches: `AIR + WATER`
- Role: pure caster and control mage
- Strengths:
  - easiest awakening path
  - strongest route into control, sustain, and flow
  - discounted mastery support thresholds for `AIR` and `WATER`
- Weaknesses:
  - harder progression into `EARTH`
  - medium progression into `FIRE`
  - difficult access to `METAL`

### Dwarf

- Starter branches: `FIRE + EARTH`
- Role: heavy mage, stable power mage
- Strengths:
  - easy awakening path
  - discounted mastery support thresholds for `FIRE` and `EARTH`
  - strongest natural route into `METAL`
- Weaknesses:
  - slower on finesse and control-focused branches
  - weaker natural route into `LIGHTNING`

### Human

- Starter branches: deterministic two-element pair
- Role: adaptable and convergent mage
- Strengths:
  - best route into third and fourth base branches
  - lowest total thresholds for broad multi-branch convergence
  - best late-game universal mage candidate
- Weaknesses:
  - lower specialization power than elf or dwarf in early/mid game

### Beastfolk

- Starter branches: `WATER + AIR`
- Role: adaptive and tempo-oriented mage
- Strengths:
  - functional mage start
  - identity around flexibility and survival rather than pure dominance
- Weaknesses:
  - highest awaken and multi-branch thresholds
  - weakest pure magical ceiling among the four supported races

## Progression States

### Awakened

This is the first real mage gate.

- Unlocks the race starter branches
- Enables branch progression
- Keeps `Elementals` runtime in sync with `STAT Mod`

### Mastered

This marks a base branch as fully stabilized.

- Requires branch-focused stat investment
- Requires a support stat
- Requires broader magical investment
- Requires the branch mastery perk from `STAT Mod`

### Third Base Branch

This is the first controlled broadening step.

- Humans should reach this first
- Elves and dwarves should reach this with meaningful but fair investment
- Beastfolk should reach this later than the others

### Fourth Base Branch

This is a late-game expansion gate.

- It should be clearly late-game for every race
- Humans remain the best candidates
- Beastfolk remain the hardest path

## Rare Branch Philosophy

`LIGHTNING`, `BLOOD`, and `METAL` are all rare secondary branches.

Shared rules:

- They are never part of normal four-branch base progression.
- They are always `grimoire-first`.
- Race and stats affect difficulty and payoff, not whether the branch is fundamentally allowed.
- Unlocks are permanent once earned.

### Lightning

- Identity: rare speed/casting branch
- Natural profile: `AIR + CASTING_SPEED + ARCANE_POWER`
- Best fit: elf
- Acceptable fit: human
- Hard fit: dwarf, beastfolk

### Blood

- Identity: rare will/arcane branch
- Natural profile: `WILLPOWER + ARCANE_POWER`
- Best fit: human and dwarf
- Harder fit: elf, beastfolk

### Metal

- Identity: rare earth-forged advanced branch
- Natural profile: `EARTH + FIRE + ARCANE`
- Best fit: dwarf
- Secondary fit: human
- Hard fit: elf, beastfolk

`METAL` is not a free dwarf-exclusive branch.

- It still requires a grimoire.
- Dwarves simply have the most coherent and least punishing access path.

## Starter Branch Rules

### Fixed Starters

- Elf: `AIR + WATER`
- Dwarf: `FIRE + EARTH`
- Beastfolk: `WATER + AIR`

### Human Starters

Humans keep a deterministic two-element pair based on player identity.

Constraints:

- The pair should remain stable per player.
- The pair should come from the four base branches only.
- The implementation should preserve replayable variety without introducing unstable randomness.

## Concrete Thresholds

### Awaken

- Elf:
  - 2 core magic stats at `12`
  - magical total `34`
- Dwarf:
  - 2 core magic stats at `12`
  - magical total `36`
- Human:
  - 2 core magic stats at `12`
  - magical total `38`
- Beastfolk:
  - 2 core magic stats at `13`
  - magical total `42`

Core awakening stats remain:

- `ARCANE_POWER`
- `CASTING_SPEED`
- `MANA_POOL`
- `ERUDITION`
- `MAGIC_RESISTANCE`
- `WILLPOWER`

### Mastered Base Branch

Base rule:

- primary branch stat `18`
- support stat `14`
- magical total `48`
- matching mastery perk required

Race modifiers:

- Elf:
  - `AIR` and `WATER` support stat reduced to `12`
- Dwarf:
  - `FIRE` and `EARTH` support stat reduced to `12`
- Human:
  - any base branch magical total reduced to `46`
- Beastfolk:
  - any base branch magical total increased to `52`

### Third Base Branch

- Human:
  - primary stat `20`
  - `ERUDITION 16`
  - `ARCANE_POWER 16`
  - magical total `54`
- Elf and Dwarf:
  - primary stat `22`
  - `ERUDITION 18`
  - `ARCANE_POWER 18`
  - magical total `60`
- Beastfolk:
  - primary stat `24`
  - `ERUDITION 18`
  - `ARCANE_POWER 18`
  - magical total `66`

### Fourth Base Branch

- Human:
  - primary stat `24`
  - `ERUDITION 20`
  - `ARCANE_POWER 20`
  - magical total `70`
- Elf and Dwarf:
  - primary stat `26`
  - `ERUDITION 22`
  - `ARCANE_POWER 22`
  - magical total `78`
- Beastfolk:
  - primary stat `28`
  - `ERUDITION 24`
  - `ARCANE_POWER 24`
  - magical total `86`

### Rare Branch Thresholds

#### Lightning

- Human:
  - `CASTING_SPEED 20`
  - `ARCANE_POWER 20`
  - magical total `60`
- Elf:
  - `CASTING_SPEED 18`
  - `ARCANE_POWER 20`
  - magical total `58`
- Dwarf:
  - `CASTING_SPEED 22`
  - `ARCANE_POWER 20`
  - magical total `64`
- Beastfolk:
  - `CASTING_SPEED 22`
  - `ARCANE_POWER 20`
  - magical total `64`

#### Blood

- Human:
  - `WILLPOWER 20`
  - `ARCANE_POWER 20`
  - magical total `60`
- Elf:
  - `WILLPOWER 22`
  - `ARCANE_POWER 20`
  - magical total `62`
- Dwarf:
  - `WILLPOWER 20`
  - `ARCANE_POWER 20`
  - magical total `60`
- Beastfolk:
  - `WILLPOWER 22`
  - `ARCANE_POWER 20`
  - magical total `64`

#### Metal

- Dwarf:
  - `EARTH 20`
  - `FIRE 18`
  - `ARCANE_POWER 18`
  - magical total `60`
- Human:
  - `EARTH 22`
  - `FIRE 20`
  - `ARCANE_POWER 20`
  - magical total `66`
- Elf:
  - `EARTH 24`
  - `FIRE 22`
  - `ARCANE_POWER 20`
  - magical total `72`
- Beastfolk:
  - `EARTH 24`
  - `FIRE 22`
  - `ARCANE_POWER 20`
  - magical total `72`

## Perk and Unlock Model

- Base branch progression continues to depend on `STAT Mod` perk gates.
- Mastery remains tied to branch mastery perks.
- Third and fourth base unlocks remain tied to dedicated perk requirements.
- Rare branches remain permanent once unlocked.
- `METAL` must receive a permanent reward binding the same way other rare branches do.

## Code Structure Changes

The implementation should stay within the current `Elementals` integration architecture.

Primary files expected to change:

- `src/main/java/tong/statmod/integration/elementals/ElementalBranch.java`
  - add `METAL`
- `src/main/java/tong/statmod/integration/elementals/ElementalsRaceAffinity.java`
  - preserve supported races
  - keep or refine deterministic human starter pairs
- `src/main/java/tong/statmod/integration/elementals/MageRaceProfile.java`
  - expose enough affinity information to avoid hardcoded race branching everywhere
- `src/main/java/tong/statmod/integration/elementals/ElementalsMageRules.java`
  - replace generic thresholds with race-aware threshold helpers
- `src/main/java/tong/statmod/integration/elementals/ElementalsPerkBindings.java`
  - add `METAL` rare reward mapping
- `src/main/java/tong/statmod/item/ElementalGrimoireItem.java`
  - support `METAL`
- `src/main/java/tong/statmod/item/ModItems.java`
  - register `metal_grimoire`
- item model and lang resources
  - add `metal_grimoire`

## Data Flow

- Race identity continues to resolve through `ElementalsRaceAffinity.resolve(...)`.
- Unlock thresholds continue to resolve through `ElementalsMageRules`.
- Runtime branch state continues to flow through `ElementalsCompat` and `ElementalsRuntimeBridge`.
- Rare branch acquisition remains item-driven through grimoires, then synchronized back into the `Elementals` runtime.

## Testing Strategy

Targeted tests should cover:

- supported race resolution and starter branch outcomes
- awaken thresholds by race
- mastery thresholds by race and favored branch
- third/fourth base unlock thresholds by race
- `LIGHTNING`, `BLOOD`, and `METAL` rare grimoire gates by race
- permanent rare unlock persistence
- no regression in runtime sync or penalty application

## Non-Goals

- No replacement of native `Elementals` UI
- No replacement of `Elementals` casting flow
- No inclusion of Mahou in this rebalance
- No inclusion of Tensura spell design in this rebalance
- No expansion beyond the four supported races
