# AI Forging Texture Generation Design

Date: 2026-07-03
Scope: generate the `rough_*` forge intermediary textures with ChatGPT Images, then convert them into Minecraft-ready item textures.

## Goal

Replace the current placeholder forge intermediary textures with a coherent AI-assisted set that:
- keeps strong Minecraft inventory readability
- respects the Overgeared forging identity
- lightly connects to the magical stations without turning every item purple
- scales to the full `rough_*` family without drifting stylistically

## Approved Direction

- Output style: true Minecraft-style pixel-art item textures
- Visual direction: realistic forged metal with a restrained mystical accent
- Generation method: material-specific generation from the start
- Initial scope: the full `rough_*` family
- AI role: generate near-final source images that are designed to downscale cleanly into game textures

## Asset Families

The first wave covers:
- `rough_blade_*`
- `rough_axe_head_*`
- `rough_spear_tip_*`
- `rough_dagger_blade_*`
- `rough_bow_limb_*`
- `rough_staff_core_*`

Each family keeps a stable silhouette language so materials vary more than shape identity.

## Visual Rules

Shared rules for every generated item:
- centered single item on transparent or plain isolated background for clean extraction
- no scene, no hand, no table, no environment
- no dramatic glow, no bloom, no smoke
- readable silhouette at very small size
- unfinished forged look: hammered surfaces, rough edges, partially refined geometry
- brighter value range than the current placeholders so the items stay visible in inventory

Material rules:
- mundane metals stay mostly practical and metallic
- magical metals can carry a subtle secondary hue or reflective accent
- premium metals can use stronger contrast, but still stay readable as forged parts first

## Prompt Strategy

Use one prompt system with two stable layers:

1. Family layer
- defines shape language and forging state for each family

2. Material layer
- defines metal color, contrast, reflectivity, and any restrained mystical accent

This avoids writing unrelated prompts from scratch for every item while still generating each texture directly as its own asset.

The concrete prompt template and modifiers live in:
- `docs/superpowers/specs/2026-07-03-ai-forging-texture-prompt-system.md`
- `docs/superpowers/specs/2026-07-03-ai-forging-texture-prompt-pack.md`

## Generation Workflow

1. Write one master prompt template.
2. Write family modifiers for the six `rough_*` families.
3. Write material modifiers for each supported material.
4. Generate each item as an isolated asset, one texture per prompt.
5. Downscale and clean the output into Minecraft-ready texture resolution.
6. Review the batch in inventory scale, not only at full size.
7. Adjust prompts only when readability or family coherence breaks.

## Resolution Strategy

The AI should not be asked to produce tiny native `16x16` sprites directly.

Instead:
- generate a larger clean isolated item image
- preserve silhouette clarity and limited detail density
- reduce it into Minecraft texture scale during post-processing

This keeps the forms readable and avoids muddy low-resolution direct generations.

## Success Criteria

The first wave is considered successful when:
- `rough_*` items no longer read as flat recolor placeholders
- family silhouettes are consistent across materials
- materials are distinguishable at inventory scale
- magical materials feel slightly special without overwhelming the forge identity
- the generation method is reusable for the next texture families

## Non-Goals

This phase does not cover:
- GUI backgrounds
- block textures for the stations
- spell icons
- final weapons or armor textures outside the forge intermediary pipeline
