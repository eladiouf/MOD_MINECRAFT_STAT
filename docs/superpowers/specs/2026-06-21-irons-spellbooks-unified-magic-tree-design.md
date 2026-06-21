# STAT Mod - Iron's Spellbooks Unified Magic Tree Design

**Date:** 2026-06-21  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Design approved for spec drafting, not yet implemented

## 1. Goal

Define the target architecture for integrating `Iron's Spellbooks` into `STAT Mod` as the primary spellcasting system while keeping:

- `STAT Mod` as the progression and identity authority
- `Iron's Spellbooks` as the runtime casting, mana, spellbook, and spell UX layer
- `Tensura` as part of the same magical identity instead of a disconnected second magic track

This design does not implement the whole magical endgame at once. It defines the final structure and the first vertical slice to build safely.

## 2. Scope Shape

This project is intentionally split into:

- a **long-term target architecture** for a unified magic tree
- a **phase 1 vertical slice** that proves the model with one complete branch

The phase 1 slice is:

- common magical trunk
- one fully playable `Fire` branch
- all other major branches visible but locked

`Iron's` addons are explicitly out of the first implementation slice. The architecture must allow them later, but phase 1 targets base `Iron's Spellbooks` only.

## 3. Core Design Commitments

### 3.1 Source of Truth

`STAT Mod` remains the authority for:

- magical identity
- stat thresholds
- perk gating
- school progression rules
- race-based affinity rules
- cross-mod unlock logic

`Iron's Spellbooks` remains the authority for:

- native mana runtime behavior
- spell casting
- spellbooks and spell equipment UX
- spell level and cast events exposed by its API

This project must avoid a second independent progression system for `Iron's`.

### 3.2 Shared Magical Identity

`Tensura` and `Iron's Spellbooks` must describe the same mage from the point of view of `STAT Mod`.

Shared identity anchors:

- `ARCANE_POWER`
- `MANA_POOL`
- `CASTING_SPEED`
- `ERUDITION`
- elemental affinities

That means a player does not build one magical identity for `Tensura` and another for `Iron's`. They build one magical character whose content is expressed through multiple mods.

### 3.3 Mana Policy

`Iron's` native mana remains the cast resource.

`STAT Mod` influences it through scaling and eligibility rather than by replacing it with a second mana implementation.

Reason:

- better addon compatibility
- lower bug risk
- preserves native spellbook and spell behavior
- still allows `STAT Mod` to act as the progression brain

### 3.4 Learning Policy

Spells are not merely weakened when the player is not ready. They are treated as **not yet learned**.

Implications:

- a locked spell should not be part of normal usable progression
- the tree represents magical learning, not only numeric bonus scaling
- higher spell levels require explicit progression instead of passive free access

## 4. Target Tree Topology

### 4.1 Common Trunk

The top of the magic system is a shared trunk that represents universal magical growth.

It should hold:

- broad magical prerequisites
- universal mana and cast support
- multi-school enablers
- advanced magical discipline gates
- universal late-game branch prerequisites

The common trunk is where `Arcane Points` are primarily spent.

### 4.2 Four Elemental Trunks

The main playable structure below the common trunk is not a raw copy of `Iron's` internal school list.

Instead, the tree is organized around four `STAT Mod`-aligned elemental trunks:

1. `Fire`
2. `Water`
3. `Air`
4. `Earth`

These trunks match the existing stat and race identity model better than a flat `Iron's` taxonomy.

Each trunk then hosts `Iron's` content inside it:

- `Fire` contains fire-focused offensive magic
- `Water` contains water and ice-oriented sustain and control
- `Air` contains air, lightning, mobility, and tempo casting
- `Earth` contains earth, nature, defense, and anchored control

This lets the project preserve race logic and existing affinity stats while still integrating the real spell content of `Iron's`.

### 4.3 Late-Game Branches

The following branches are part of the final architecture and should be visible from the beginning:

- `Holy`
- `Blood`
- `Ender`
- `Evocation`
- `Eldritch`

They should appear as distant, aspirational, clearly locked branches.

Their design roles are:

- `Holy`: difficult vocation branch unlocked later through church/sanctuary quest content
- `Blood`: dangerous late-game branch unlocked later through dedicated boss content
- `Ender`: rare technical branch
- `Evocation`: advanced high-control branch
- `Eldritch`: deepest abnormal late-game branch

Phase 1 does not make these branches playable. It only fixes their place in the information architecture.

### 4.4 Tensura Placement

`Tensura` should not live in a separate magic tree.

Instead:

- `Iron's` provides the dense spell-by-spell backbone inside branches
- `Tensura` appears through major branch nodes
- those `Tensura` nodes are placed both mid-branch and as capstones

The rule is:

- `Iron's` fills the branch
- `Tensura` punctuates the branch

This keeps the tree readable while still making `Tensura` materially present.

## 5. Race and Starting Structure

The allowed race foundation for the integrated magic tree is:

- `Human`: flexible multi-school profile with two weak universal affinities instead of one strong natural pairing
- `Elf`: `Air + Water`
- `Dwarf`: `Earth + Fire`
- `Beast`: `Water + Air`, weaker magical purity than `Elf`

At the start:

- the player sees both natural racial affinities
- the player chooses which natural branch to launch first
- the second natural branch should open very cheaply afterward
- out-of-affinity branches remain possible later, but at higher cost and with slower progression

`Human` does not start as the strongest caster. It starts as the most flexible long-term multi-school candidate.

## 6. Spell and School Unlock Model

### 6.1 Tree Structure Rules

The tree should not model every spell rank as a separate top-level node.

The correct structure is:

- common trunk nodes
- school opening nodes
- school tier nodes
- selected signature spell nodes
- internal progression for higher spell ranks gated by stats and tier access

This preserves full spell coverage without turning the UI into an unreadable mass.

### 6.2 Spell Availability

A spell is usable only if it has been learned through the tree's rules.

That learning can come from:

- school access
- tier access
- explicit signature nodes where needed

The project should not use a model where every spell can be cast badly before proper progression.

### 6.3 Higher Spell Levels

Higher levels of a spell must require:

- higher relevant stats
- access to the corresponding branch tier nodes

They should not require a separate pure-usage grind to rank up.

### 6.4 Late-Game Access Rules

Late-game branches should not open through stats alone.

They require both:

- serious stat and tree maturity
- external world or progression keys such as quests, bosses, rare books, or other content gates

This is especially important for:

- `Holy`
- `Blood`
- eventually `Ender`, `Evocation`, and `Eldritch`

## 7. Progression Resource Model

The tree should use a mixed progression currency model.

### 7.1 Arcane Points

`Arcane Points` are the broad magical currency.

They are used mainly for:

- common trunk progression
- universal magical foundations
- major cross-school gates
- some late-game prerequisite nodes

### 7.2 School Points

`School Points` are branch-oriented progression points.

They are used mainly for:

- advancing deeper into a specific branch
- buying branch-specific progression nodes
- expressing specialization without consuming the entire global character budget

The system should use:

- a small common magical economy
- plus branch progression economies

This is more scalable than one giant shared pool and less fragmented than fully isolated per-school systems.

### 7.3 How School Points Are Earned

`School Points` should come from both:

- relevant stat growth
- real practice in the school

The recommended model is:

- background school mastery grows through valid magical play
- crossing mastery thresholds awards explicit points
- the player spends those points manually in the tree

This preserves both practice fantasy and player agency.

## 8. Cast-Driven Progression Model

`Iron's` casts should feed `STAT Mod` progression directly, but under anti-abuse rules.

### 8.1 Baseline Rule

Every valid cast can contribute some progress.

### 8.2 Better Rewards

Meaningful real-use casts should reward more:

- offensive spells that hit
- defensive spells used under pressure
- healing that matters
- control that lands in real situations
- stronger or more difficult spells

### 8.3 Anti-Abuse Rule

Empty or trivial spam must be weak progression.

The system should reward:

- valid use
- combat impact
- difficult context

It should not heavily reward idle macro-like repetition.

## 9. Equipment Policy

The tree should strongly control schools and spells, but not hard-lock all magical equipment.

Recommended policy:

- basic spellbooks remain broadly usable
- specialized books and equipment become more effective when the player has the matching branch and stats
- later signature equipment may require explicit branch progression

This keeps `Iron's` loot and crafting satisfying without allowing equipment to bypass magical progression.

## 10. Player Onboarding

The starting magical experience should be simple and consistent.

Phase 1 onboarding target:

- all players receive a neutral starter spellbook
- the player chooses which natural racial branch to begin with
- the first starter spell is fixed and pedagogical
- stronger identity choices arrive immediately after the first safe introduction

This is better than full free choice at minute zero because it reduces early failure states and gives a predictable learning ramp.

## 11. Phase 1 Vertical Slice

Phase 1 should prove the architecture with one fully playable branch:

- common magical trunk
- complete `Fire` branch
- all other branches visible but locked

### 11.1 Why Fire First

`Fire` is the best pilot branch because it is:

- mechanically legible
- offensively easy to verify
- strongly aligned with `ARCANE_POWER`, `CASTING_SPEED`, and `MANA_POOL`
- suitable for later mixed `Iron's + Tensura` branch design

### 11.2 Phase 1 Active Content

Phase 1 should include:

- shared magical trunk foundations
- racial opening logic as tree visibility and eligibility rules
- `Fire` branch school opening
- `Fire` tier progression
- `Fire` spell learning and gating
- `Fire` cast-driven progression hooks
- visible but locked future branches

Because only `Fire` is fully implemented in phase 1, the slice must not soft-lock non-fire races out of the magic system.

The phase 1 rule is therefore:

- `Fire` is the universal pilot branch available to all races for implementation purposes
- racial affinity discounts and future branch identity are still modeled in the tree structure
- strict first-affinity onboarding becomes fully enforced only once at least a second elemental branch is implemented

### 11.3 Phase 1 Explicit Non-Goals

Phase 1 should not attempt to complete:

- all four elemental branches
- addon spell integration
- `Holy` questline content
- `Blood` boss content
- full late-game branch mechanics
- full mixed `Tensura` branch coverage in every school

It is acceptable for phase 1 to place only the structural anchors needed for those future systems.

## 12. Suggested Implementation Boundaries

The implementation should prefer a small number of clear units instead of one giant compatibility file.

Recommended responsibilities:

- `IronSpellsCompat` or equivalent bootstrap layer
- `MagicTreeProgressionService` for unified unlock logic
- `SchoolProgressTracker` for school mastery and school point awards
- `IronSpellEventBridge` for cast, mana, level, and school event hookups
- `MagicEligibilityResolver` for race, stat, and tier checks
- `MagicTreeViewModel` or equivalent data builder for locked/visible branch state

Names can change, but the boundaries should stay focused and testable.

## 13. Error Handling and Compatibility Rules

The design should fail safe.

Required behavior:

- if a player lacks eligibility, spells stay locked rather than half-functional
- if a branch is not yet implemented, it remains visible but explicitly locked
- if addon content is present, phase 1 should ignore it instead of partially integrating it
- if `Iron's` exposes runtime events with unexpected data, progression should refuse the reward rather than corrupt player state

The tree must avoid double-unlock or split-source states between runtime spell data and `STAT Mod` progression data.

## 14. Test Strategy

The implementation plan must include tests for:

- race affinity visibility and opening rules
- second natural affinity cheaper than out-of-affinity
- locked spell rejection behavior
- higher spell levels requiring both stats and tier access
- school mastery to school point conversion
- cast progression awarding valid low rewards for generic valid casts
- stronger rewards for meaningful real-use casts
- anti-abuse protection against empty spam loops
- phase 1 branch visibility rules for locked future schools

Where practical, branch logic and progression math should be testable without launching the full client.

## 15. Next Planning Slice

The implementation plan that follows this spec should focus on a single buildable slice:

1. establish the `Iron's` bridge and compatibility boundaries
2. define the phase 1 tree data model
3. implement common trunk progression
4. implement `Fire` branch progression and learning
5. wire cast-driven school mastery gains
6. expose visible-but-locked future branches
7. add regression-safe tests

Only after that slice is stable should the project plan:

- the second elemental branch
- addon integration
- deeper `Tensura` magical nodes
- `Holy` quest content
- `Blood` boss content
