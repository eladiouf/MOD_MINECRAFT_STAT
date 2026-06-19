# STAT Mod - Elementals Mage Class Design

**Date:** 2026-06-19  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Design approved for spec drafting, not yet implemented

## 1. Goal

Define how `STAT Mod` should drive `Elementals` as the primary mage-class progression layer without replacing the mod's native casting identity.

This design establishes:

- how a player becomes a mage
- how race determines starting elemental affinities
- how additional elements are unlocked
- how rare branches like `lightning` and `blood` are earned
- how `STAT Mod` gates and scales `Elementals`
- how this path stays compatible with a later universal magic architecture that also includes `Tensura` and `Mahou Tsukai`

This spec does not classify `Tensura` or `Mahou Tsukai` spells yet. That is a later planning slice.

## 2. Core Design Commitments

### 2.1 Source of Truth

`STAT Mod` is the gameplay source of truth for mage identity and elemental progression.

That means it owns:

- mage awakening
- element unlock conditions
- mastery thresholds
- rare-branch requirements
- stat-based cast penalties and bonuses

`Elementals` remains the native execution layer for:

- casting flow
- keybinds
- native HUD
- native upgrade tree feel
- native spell handling

### 2.2 Integration Philosophy

The design is a **mage identity layer**, not a total rewrite of `Elementals`.

Desired outcome:

- the player still feels like they are using `Elementals`
- progression logic obeys `STAT Mod`
- magical identity stays coherent with the rest of the project

### 2.3 Class Fantasy

The target fantasy is:

- strong racial starting identity
- limited early access
- difficult but rewarding broadening over time
- a late-game `universal mage` path that must be earned

The system should not give every mage all elements early.

### 2.4 Allowed Race Set

This design assumes the race layer is restricted to these four player races:

- `elf`
- `human`
- `dwarf`
- `beastfolk`

The elemental identity system is tuned specifically for those four.

## 3. Element Structure

### 3.1 Element Categories

`Elementals` is divided into:

- base elements: `air`, `water`, `earth`, `fire`
- rare elements: `lightning`, `blood`

`metal` exists in the mod, but it is not part of this mage-class design slice.

### 3.2 Progression States

Each supported element uses one of three states:

- `locked`
- `awakened`
- `mastered`

Interpretation:

- `locked`: the player cannot meaningfully use that element
- `awakened`: the player can use it, but with real penalties
- `mastered`: the player uses it at full intended strength

### 3.3 Casting Rule Model

The casting rule model is **mixed**:

- fully locked elements are blocked
- awakened elements are usable but intentionally weaker and less efficient
- mastered elements behave normally

This preserves progression friction without making the system feel inert.

## 4. Race Affinity Model

### 4.1 Starting Elements by Race

Starting affinities are assigned automatically from race:

- `elf` -> `air + water`
- `dwarf` -> `fire + earth`
- `beastfolk` -> `water + air`
- `human` -> `2` random base elements chosen from `air`, `water`, `earth`, `fire`

Humans are the versatile start. They are not stronger at the beginning, but they are more flexible in route shape.

### 4.2 Human Identity

Humans are designed as:

- versatile
- broad-potential
- slightly favored for later multi-element progression

They still begin with only `2` starting elements.

### 4.3 Beastfolk Identity

Beastfolk are intentionally weaker in magic than the other three races.

That weakness is expressed through both:

- stricter progression thresholds
- slower magical progression

They still receive `2` starting elements so they remain viable, but they are not intended to dominate magical builds.

## 5. Mage Awakening

### 5.1 Class Unlock Model

The mage class unlocks automatically once the player's magical build is sufficiently developed.

There is no manual class selection for this layer.

### 5.2 Mage-Awakened Threshold

Normal threshold:

- at least `2` magical stats at `12+`
- total magical stat sum of `36+`

Beastfolk threshold:

- at least `2` magical stats at `13+`
- total magical stat sum of `40+`

Relevant magical stats for this design are:

- `ARCANE_POWER`
- `CASTING_SPEED`
- `MANA_POOL`
- `ERUDITION`
- `MAGIC_RESISTANCE`
- `WILLPOWER`

### 5.3 Awakening Outcome

When mage awakening happens, the player gains:

- access to the mage-class path
- access to their race-defined starting elements
- eligibility to progress those elements toward mastery

Awakening should feel like entering a real specialization, not just unlocking a UI screen.

## 6. Element Mastery Progression

### 6.1 Starting Element Mastery

A starting element becomes `mastered` when all of the following are true:

- the element's primary supporting stat is `18+`
- the required secondary core stat is `14+`
- total magical stat sum is `48+`
- the matching family `MASTERY` perk is owned

Secondary core stat by element:

- `air` -> `CASTING_SPEED`
- `fire` -> `CASTING_SPEED`
- `water` -> `MANA_POOL`
- `earth` -> `MANA_POOL`

Primary supporting stat by element:

- `air` -> `AIR_AFFINITY`
- `water` -> `WATER_AFFINITY`
- `earth` -> `EARTH_AFFINITY`
- `fire` -> `FIRE_AFFINITY`

### 6.2 Third Base Element Unlock

The third base element is intentionally very difficult to unlock.

Requirements:

- `mage-awakened`
- at least `1` starting element already `mastered`
- target element primary supporting stat at `22+`
- `ERUDITION 18+`
- `ARCANE_POWER 18+`
- total magical stat sum of `60+`
- required perk ownership

Race modifiers:

- `human`: total magical stat sum reduced to `56+`
- `beastfolk`: total magical stat sum increased to `66+`

### 6.3 Fourth Base Element Unlock

The fourth base element represents advanced late-game universalization.

Requirements:

- at least `2` elements already `mastered`
- target element primary supporting stat at `26+`
- `ERUDITION 22+`
- `ARCANE_POWER 22+`
- total magical stat sum of `76+`

Race modifiers:

- `human`: total magical stat sum reduced to `72+`
- `beastfolk`: total magical stat sum increased to `82+`

This stage should be exceptional, not normal progression.

## 7. Rare Branches: Lightning and Blood

### 7.1 Unlock Philosophy

`lightning` and `blood` are not racial starting paths and not standard progression branches.

They are unlocked through extremely rare custom grimoires.

### 7.2 Acquisition Model

Each rare branch requires:

- finding its dedicated grimoire
- meeting the stat gate to use it

On successful use, the unlock is:

- permanent
- tied to the character
- accompanied by a perk and a skill grant

### 7.3 Lightning Requirements

`lightning` grimoire use requires:

- `CASTING_SPEED 20+`
- `ARCANE_POWER 20+`
- total magical stat sum of `58+`

### 7.4 Blood Requirements

`blood` grimoire use requires:

- `WILLPOWER 20+`
- `ARCANE_POWER 20+`
- total magical stat sum of `58+`

### 7.5 Stacking Rule

`lightning` and `blood` are cumulative. A character may earn both.

This is allowed because the system compensates with harsh cost and progression penalties.

## 8. Runtime Power Model

### 8.1 Awakened-State Penalties

An `awakened` element uses the following default penalties:

- power `-20%`
- resource cost `+25%`

This keeps partial access meaningful while making mastery materially better.

### 8.2 Mastered-State Behavior

A `mastered` element behaves at full intended strength with no awakened-state penalties.

### 8.3 Locked-State Behavior

A `locked` element is blocked from normal use by the integration layer.

## 9. Penalty Model for Rare and Broad Builds

### 9.1 Rare Branch Cost

`blood` and `lightning` must be stronger in identity than common branches, but they must also be more punishing to sustain.

They therefore carry:

- higher cast costs
- slower progression on other elements

### 9.2 Universal Mage Cost

The system should permit a late universal mage build, but it must never be the cheapest route.

As the player broadens across more elements, progression pressure should rise through:

- higher stat thresholds
- perk requirements
- opportunity cost

The design should reward commitment before breadth.

## 10. Perk and Tree Expectations

### 10.1 Perk Ownership

Elemental progression must interact with the project's perk ecosystem.

This includes:

- mastery perks for base elements
- unlock perks for broader progression stages
- rare-branch perks attached to grimoire acquisition

### 10.2 Tree Organization

Perk presentation should stay organized by family rather than by isolated stat.

For the mage path, the important split is:

- magical core progression
- elemental specialization progression

This spec does not define the exact final perk-tree layout, but it does require `Elementals` progression to fit that family-first organization.

## 11. Architectural Slice

The expected implementation should be split into dedicated integration components:

- `ElementalsCompat`
- `ElementalsRaceAffinity`
- `ElementalsMageProgression`
- `ElementalsElementState`
- `ElementalsGrimoireRegistry`
- `ElementalsStatScaling`
- `ElementalsGate`
- `ElementalsPenaltyModel`

Responsibilities:

- `ElementalsCompat`: entrypoint and lifecycle wiring
- `ElementalsRaceAffinity`: race -> starting element assignment
- `ElementalsMageProgression`: awakening, 3rd element, 4th element rules
- `ElementalsElementState`: locked/awakened/mastered storage and checks
- `ElementalsGrimoireRegistry`: rare-branch definitions and unlock hooks
- `ElementalsStatScaling`: stat-derived bonuses and penalties
- `ElementalsGate`: cast access and rule enforcement
- `ElementalsPenaltyModel`: universal and rare-branch pressure tuning

## 12. Scope Boundaries

This spec intentionally does not do the following:

- replace `Mahou Tsukai`
- replace `Tensura`
- classify all spells across every magic mod
- define exact loot-table placement for grimoires
- define every numeric cast-scaling formula
- define `metal` progression

Those are follow-up slices.

## 13. Non-Negotiable Outcomes

The implementation derived from this spec must preserve these outcomes:

- `STAT Mod` stays the authority for progression and gating
- `Elementals` stays the native-feeling casting frontend
- race identity matters at the start
- humans remain the best broadening route
- beastfolk remain viable but clearly weaker for magic
- third and fourth base elements stay hard-earned
- `blood` and `lightning` stay rare, permanent, and punishing
- late universal mage progression exists, but only as a difficult build path

## 14. Next Planning Slice

The next implementation plan should turn this design into concrete work items for:

1. race-state storage and fallback rules
2. mage awakening detection
3. element-state persistence
4. native `Elementals` hook points for gating and scaling
5. grimoire item and unlock flow
6. perk requirements and reward hooks
7. tests for thresholds, penalties, and persistence

After that, a separate design slice should classify spells in:

1. `Tensura`
2. `Mahou Tsukai`

That later audit will define how the broader universal magic identity maps beyond `Elementals`.
