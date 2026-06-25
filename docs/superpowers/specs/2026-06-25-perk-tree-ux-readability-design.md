# STAT Mod - Perk Tree UX Readability Design

**Date:** 2026-06-25  
**Project:** `STAT Mod` on NeoForge 1.21.1  
**Status:** Drafted for user review before implementation plan

## 1. Goal

Improve the user experience of the custom perk tree so a player can understand, at a glance:

- which perks are already unlocked
- which perks are unlockable right now
- which perks are blocked
- why a perk is blocked
- what a perk gives before clicking it

The target is not a new perk system. The target is a much clearer presentation of the existing one.

## 2. Scope

### In Scope

- improve readability of the custom perk tree UI
- make node states visually distinct
- make perk costs and prerequisite failures easier to read
- improve tooltip structure and wording
- improve family/branch readability inside the tree
- add targeted tests for the new UI-state and tooltip behavior where feasible

### Out of Scope

- changing perk balance
- changing perk costs
- changing perk prerequisites
- replacing the custom perk UI with Puffish for this slice
- changing magic tree gameplay rules
- adding zoom/pan rework unless required by the readability changes

## 3. Problem Statement

The current custom perk tree works, but the main UX weakness is not interaction speed. It is comprehension.

Today, the player must inspect too many nodes manually to answer simple questions:

- "Can I unlock this now?"
- "If not, what exactly is missing?"
- "How expensive is this node?"
- "What family or progression track am I really looking at?"

That creates friction even when the underlying rules are correct.

## 4. Design Choice

Three candidate directions were considered:

1. Tooltip-first
2. State-first
3. Navigation-first

This design chooses **state-first**.

Reason:

- better navigation does not help enough if node meaning is still unclear
- better tooltips help, but still require too much hover-driven scanning
- strong node-state readability improves both glanceability and tooltip usefulness

## 5. UX Outcome

After this change, the player should be able to open the tree and immediately distinguish:

- `Unlocked`
- `Unlockable now`
- `Locked by stat requirement`
- `Locked by point requirement`
- `Locked by prerequisite perk`
- `Hovered / selected`

The player should not need to guess whether a node is merely unavailable or nearly available.

## 6. Visual State Model

### 6.1 Node states

Each node must resolve to one primary visual state:

- `UNLOCKED`
- `AVAILABLE`
- `LOCKED_STAT`
- `LOCKED_POINTS`
- `LOCKED_PREREQ`
- `LOCKED_MIXED`
- `HOVERED`
- `SELECTED`

`HOVERED` and `SELECTED` are overlay states on top of the base unlock state.

### 6.2 Visual rules

- `UNLOCKED` uses the strongest stable confirmation styling
- `AVAILABLE` uses the strongest call-to-action styling
- `LOCKED_*` stays readable but desaturated enough to avoid false affordance
- `SELECTED` must be more distinct than simple hover
- `HOVERED` must help focus without overpowering availability/unlock states

### 6.3 Family readability

The tree should visually reinforce perk family identity:

- node accents follow family color language already present in the UI
- branch/link readability should make local progression easier to trace
- the active family context should feel grouped rather than visually noisy

## 7. Tooltip Model

Tooltips should always render in a stable order:

1. perk name
2. short effect summary
3. tier / category context
4. cost
5. requirement status lines
6. final action or block reason

### 7.1 Requirement lines

Requirement lines must be explicit and never vague.

Examples:

- `Stat requirement: Brute Force 8 / 12`
- `Perk points: 1 / 3`
- `Requires: Heavy Swing`

### 7.2 Color semantics

- passed checks are green
- failed checks are red
- informational labels are neutral

### 7.3 Final status line

Exactly one final status line should summarize the current action state:

- `Click to unlock`
- `Blocked: not enough perk points`
- `Blocked: stat level too low`
- `Blocked: prerequisite perk missing`

If multiple failures exist, the summary line should show the dominant blocker while the detailed lines still list all failures.

## 8. Interaction Rules

### 8.1 Hover

Hover should:

- highlight the node clearly
- reinforce connected links if cheap to do with current rendering structure
- open the tooltip in a stable readable position

### 8.2 Click

Click on an available node should feel decisive and readable:

- visual confirmation before/after unlock should be obvious
- failure feedback should come from existing validation, but the UI should already make the reason predictable

### 8.3 Selected focus

If the current screen architecture already tracks a selected node, the selected state should persist visually long enough to help orientation after hover changes.

If not, the implementation may add a lightweight selected-node presentation state as long as it stays UI-only.

## 9. Rendering Boundaries

Primary implementation targets:

- `src/main/java/tong/statmod/client/gui/PerkScreen.java`
- `src/main/java/tong/statmod/client/gui/TalentTreePanel.java`
- `src/main/java/tong/statmod/client/gui/PerkNodeWidget.java`

Supporting code may introduce a small pure helper for:

- node visual state resolution
- tooltip line building
- blocker prioritization

The rendering layer should not become the new source of unlock logic. Gameplay authority remains in existing perk systems.

## 10. Error Handling and Fail-Safe Rules

- if a perk state cannot be classified cleanly, fall back to the most conservative locked rendering
- if tooltip details are incomplete, still show a minimal valid tooltip instead of crashing or showing raw null data
- if a family color or icon is missing, use the current default visuals rather than inventing a second style system

## 11. Testing Strategy

Implementation planning must include targeted verification for:

- visual-state classification from perk/player conditions
- blocker prioritization
- tooltip line generation
- no regression on click/unlock flow
- no regression on scrolling / hover behavior already covered by current panel tests

Likely test targets:

- `TalentTreePanelTest`
- `PerkNodeWidget`-adjacent tests if practical
- a new pure helper test if state/tooltip logic is extracted

## 12. Acceptance Criteria

The slice is successful when:

- a player can distinguish unlocked vs available vs blocked nodes without reading every tooltip
- blocked nodes communicate exact missing requirements
- the tooltip order is stable and easy to scan
- the tree feels easier to read without changing perk mechanics
- existing perk unlock logic remains authoritative

## 13. Implementation Shape

The implementation plan that follows this spec should be a single bounded UI slice:

1. extract or formalize node-state resolution
2. refactor tooltip content into a stable model
3. restyle node rendering for the new states
4. tighten targeted tests

Navigation upgrades such as deeper camera behavior, advanced focus, or zoom tuning should be considered later only if readability remains insufficient after this slice.

## 14. Self-Review

- No placeholders or TBD sections remain
- Scope is limited to one custom perk-tree UX slice
- No perk-economy or gameplay-rule changes are implied
- The architecture keeps rendering separate from perk authority

*Drafted on 2026-06-25. Ready for user review before implementation planning.*
