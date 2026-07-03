# AI Forging Texture Prompt System

Date: 2026-07-03
Purpose: provide a stable prompt system for generating the `rough_*` forge intermediary item textures with ChatGPT Images.

## Use Case

Use case: stylized-concept
Asset type: Minecraft item texture source image for later downscale and cleanup

The prompt system is designed for:
- isolated single-item generations
- near-final source images that convert cleanly into Minecraft item textures
- strong family consistency across the whole forge pipeline

## Global Prompt Rules

Every prompt should preserve these constraints:
- single isolated item only
- centered composition
- no environment
- no hand, no character, no table, no forge scene
- no frame, no card, no UI, no text, no watermark
- transparent-looking neutral isolation or plain flat background only
- readable silhouette at tiny inventory scale
- low detail density, not painterly
- pixel-art aware rendering intended to downscale into Minecraft item texture quality
- forged unfinished state: rough edges, hammer marks, asymmetry, partially refined metal
- realistic metal first, restrained mystical accent second

## Shared Negative Constraints

Append this block to every prompt:

```text
No background scene, no person, no hand, no table, no weapon being held, no forge room, no sparks cloud, no smoke, no flames around the object, no dramatic glow, no bloom, no lens effects, no ornate border, no icon frame, no text, no watermark, no multiple items, no photorealistic photo setup, no soft painterly brushwork, no blurry edges, no heavy shadow beneath the item, no full purple recolor, no overly polished finished weapon look.
```

## Master Prompt Template

```text
Create a single isolated Minecraft-style pixel-art item texture source image.

Subject: an unfinished forged [FAMILY_NAME] made from [MATERIAL_NAME].
State: rough forge intermediary, not a finished weapon, visibly hammered and partially refined.
Silhouette: strong and readable at very small inventory size.
Rendering: clean sprite-oriented pixel-art aesthetic, crisp edges, controlled shading, limited clusters, readable highlights, designed to downscale cleanly into a Minecraft inventory texture.
Composition: one centered object only, front-facing inventory icon presentation, no perspective scene.
Surface language: forged metal with uneven hammer marks, subtle edge nicks, compact high-contrast shading, brighter than a dark placeholder texture.
Material profile: [MATERIAL_BLOCK]
Family profile: [FAMILY_BLOCK]
Magic treatment: restrained mystical accent only where the material calls for it, never dominant over the metal.
Background: plain isolated background suitable for easy extraction, with no environment or props.

No background scene, no person, no hand, no table, no weapon being held, no forge room, no sparks cloud, no smoke, no flames around the object, no dramatic glow, no bloom, no lens effects, no ornate border, no icon frame, no text, no watermark, no multiple items, no photorealistic photo setup, no soft painterly brushwork, no blurry edges, no heavy shadow beneath the item, no full purple recolor, no overly polished finished weapon look.
```

## Family Blocks

### `rough_blade_*`

```text
Broad unfinished sword blade blank without hilt, guard, or grip. The shape should read as a forged blade core: thick spine, incomplete edge refinement, slightly uneven tip, compact vertical silhouette, practical smithing proportions.
```

### `rough_axe_head_*`

```text
Single unfinished axe head without handle. The shape should read as a forged striking head: broad cutting face, thick poll, dense weight, rough socket area, asymmetrical hammer-forged contour, strong side-heavy silhouette.
```

### `rough_spear_tip_*`

```text
Single unfinished spear tip without shaft. The shape should read as a forged polearm head: elongated leaf or spike form, reinforced midrib, compact socket base impression, sharp directional silhouette, unfinished edge refinement.
```

### `rough_dagger_blade_*`

```text
Single unfinished dagger blade without hilt or grip. The shape should read as a compact stabbing blade blank: shorter and leaner than a sword blade, aggressive point, narrow body, rough forged bevel hints, clean small-scale readability.
```

### `rough_bow_limb_*`

```text
Single unfinished magical bow limb component, not a complete bow. The shape should read as a forged arc segment: curved reinforced armature, strong taper, rigid core, unfinished attachment ends, compact silhouette that still reads as bow hardware.
```

### `rough_staff_core_*`

```text
Single unfinished staff core component, not a complete staff. The shape should read as a forged magical rod blank: elongated central core, reinforced head section, rough mounting geometry, slight arcane craftsmanship hints, compact readable silhouette.
```

## Material Blocks

### `gold`

```text
Warm yellow gold metal, soft but bright reflective contrast, slightly worn forge coloration, rich warm highlights, no magical aura.
```

### `tin`

```text
Pale muted gray-silver metal with a faint warm cast, practical low-status material, simpler reflectivity, modest contrast, no magical aura.
```

### `bronze`

```text
Warm brown-orange alloy metal, earthy and practical, deeper midtones than gold, durable forged feel, no magical aura.
```

### `diamond`

```text
Cool bright cyan-steel interpretation, cleaner and sharper than common metals, crisp cool highlights, refined premium feel, only a tiny mineral shimmer.
```

### `pyrium`

```text
Heated ember-red alloy with orange-gold reflections, strong internal warmth, restrained volcanic intensity, slight arcane heat accent without surrounding flames.
```

### `arcane`

```text
Cool blue-violet enchanted metal, premium forged surface with subtle mystical reflections, restrained runic energy hints, never full neon, never dominant glow.
```

### `mithril`

```text
Bright cool silver-blue metal, elegant and refined, lighter overall value range, clean premium highlights, very subtle magical nobility.
```

### `low_magisteel`

```text
Dark steel with a faint magical blue-violet undertone, early-stage enchanted alloy, restrained accent color, practical more than mystical.
```

### `magisteel`

```text
Balanced enchanted steel with medium cool contrast, confident magical alloy identity, subtle blue-violet reflective accents, premium but controlled.
```

### `pure_magisteel`

```text
Refined luminous enchanted steel, clearer mystical purity, brighter cool highlights, restrained violet-blue accenting, premium magical alloy without neon excess.
```

### `high_magisteel`

```text
Dense elite enchanted steel with strong contrast, deeper body tones, sharper cool highlights, authoritative magical alloy identity, restrained arcane intensity.
```

### `orichalcum`

```text
Heroic radiant alloy between gold and copper, premium legendary warmth, strong clean highlights, regal forged presence, slight mythical prestige without visible aura.
```

### `adamantite`

```text
Extremely dense dark premium metal, hard-edged contrast, cold powerful reflectivity, intimidating forged mass, almost indestructible feel, no overt glow.
```

### `hihiirokane`

```text
Rare crimson-gold legendary alloy, noble warm metallic body with intense premium highlights, restrained mythic richness, no magical overglow.
```

## Resolution Guidance

When using ChatGPT Images, ask for:
- a single isolated item
- crisp sprite-like readability
- low clutter
- lighting that supports downscale

Do not ask for:
- huge decorative detail
- cinematic lighting
- backgrounds
- multiple variants in one image

## Recommended Production Pattern

1. Pick the family block.
2. Pick the material block.
3. Insert both into the master prompt.
4. Generate one item at a time.
5. Review at inventory scale after downscale, not only at full size.
