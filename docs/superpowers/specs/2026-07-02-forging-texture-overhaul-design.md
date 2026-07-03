# Forging Texture Overhaul Design

Date: 2026-07-02
Scope: replace the most visible placeholder item textures in the forge pipeline, starting with `rough_*`.

## Goal

Make the forge pipeline look coherent with the current gameplay loop:
- Overgeared-style forged intermediates stay readable as metal parts.
- StatMod magical stations still feel connected through subtle mystical accents.
- Inventory readability improves versus the current placeholder set.

## Priority Order

1. `rough_*`
2. `heated_*`
3. `grips`, `rune_essence_*`, `blueprints`

## Visual Direction

Chosen direction: realistic forged metal with a light mystical accent.

Why:
- keeps compatibility with the base Overgeared forging fantasy
- avoids overusing purple on every item
- still connects visually to the infusion and enchantment stations

## Rough Item Rules

Applies to:
- `rough_blade_*`
- `rough_axe_head_*`
- `rough_spear_tip_*`
- `rough_dagger_blade_*`
- `rough_bow_limb_*`
- `rough_staff_core_*`

Shared rules:
- strong silhouette first, material identity second
- brighter than the current placeholders for inventory readability
- visible hammering / unfinished forging marks
- slightly irregular edges to sell the “unfinished” state
- no heavy glow on normal metals
- magical materials may get a restrained secondary tint, never full neon

## Material Language

Baseline material intent:
- `gold`, `tin`, `bronze`: warm metals, more earthy and practical
- `diamond`, `mithril`: cooler, cleaner, more refined
- `pyrium`, `arcane`, `magisteel*`: more saturated accents and subtle mystical reflections
- `orichalcum`, `adamantite`, `hihiirokane`: premium heroic metals with stronger contrast

## Production Strategy

Phase 1:
- establish master silhouettes for `blade`, `axe_head`, `spear_tip`, `dagger_blade`
- validate style using 3 materials: one mundane, one arcane, one premium

Phase 2:
- derive the rest of `rough_*` from those masters
- keep family consistency tighter than perfect uniqueness

Phase 3:
- use the approved material palette for `heated_*`, `grips`, and rune items

## Success Criteria

- `rough_*` items are no longer identifiable as flat placeholder recolors
- materials are distinguishable at inventory scale
- the family reads as one coherent forge progression
- magical flavor is present but does not drown the metal identity
