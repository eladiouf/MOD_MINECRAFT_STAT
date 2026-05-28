# GUI Redesign — Style Parchemin

**Date:** 2026-05-27
**Status:** Draft
**Scope:** Full visual overhaul of CharacterScreen, PerkScreen, and HUD overlays in a medieval parchment RPG style

---

## 1. Overview

Replace the current minimalist/plain GUI with a warm parchment-themed RPG interface. The style evokes medieval grimoires and illuminated manuscripts: beige/tan backgrounds, sepia-brown borders, serif/script typography, ornamental dividers, and gold-accented highlights.

**Texture assets (AI-generated):**
- Parchment background tile (for screen backdrops)
- Ornate border frames (for panels and tab containers)
- 23 stat icons (sword, shield, arrow, flame, etc.)
- Tab divider / ribbon element
- XP bar texture (thin gold-inlaid groove)

---

## 2. CharacterScreen (Touche P)

### 2.1 Layout

```
┌──────────────────────────────────────────────────┐
│  ════ ❧ STATISTIQUES ❧ ════                     │
│                                                    │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐   │
│  │Combat│ │Magie │ │Survie│ │Art.  │ │Mental│   │  ← Onglets parchemin
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘   │
│  ────────────────────────────────────────────────  │  ← Séparateur
│                                                    │
│  ⚔ Brute Force                   Niv. 34          │
│  ┌──────────────────────────────────────┐          │
│  │████████████████████░░░░░░░░░░░░░░░░░░│          │  ← Barre XP
│  └──────────────────────────────────────┘          │
│                            340 / 520 XP            │
│                                                    │
│  🗡 Technique de Lame            Niv. 28          │
│  ┌──────────────────────────────────────┐          │
│  │████████████████░░░░░░░░░░░░░░░░░░░░░░│          │
│  └──────────────────────────────────────┘          │
│                            220 / 400 XP            │
│                                                    │
│  (suite des stats de la catégorie...)              │
└──────────────────────────────────────────────────┘
```

### 2.2 Components

**Background:** Full-screen parchment texture (tiled or scaled). Darkened slightly (multiply blend) at ~60% opacity so text remains readable.

**Title header:** "❧ STATISTIQUES ❧" centered, flanked by ornamental `═══` lines in sepia/brown. Font: Minecraft default but rendered in dark brown (#3a1a00).

**Tabs:** 5 category tabs (Combat, Magie, Survie, Artisanat, Mental). Each tab is a rounded rectangle with a papyrus/ribbon texture background. Active tab: brighter beige (#d4c494) with gold border (#c49a3c). Inactive tabs: muted tan (#c4a86a) with brown border (#8b4513). Hover: slight glow effect.

**Tab separator:** A thin horizontal line (#8b4513, 1px) with small ornamental dot in the center.

**Stat entry / StatWidget:** Each stat displayed as a horizontal card:
- **Card background:** Warm beige (#d4c494) with 1px sepia border (#a0724a), 4px border-radius
- **Padding:** 6px 8px
- **Left:** 16x16 icon (AI-generated), then stat display name in bold #3a1a00
- **Right:** Level number in bold #8b4513
- **XP bar:** 4px tall, background #b8965a (tan), fill = horizontal gradient #8b4513 → #d2691e (dark brown → light brown)
- **XP text:** right-aligned below bar, #6b4c1e, small font ("340 / 520 XP" or "MAX" at level 100)
- **Gap between cards:** 8px

**Scrolling:** If stats exceed screen height, content area scrolls (mouse wheel). No scrollbar — use Minecraft's native clipping.

### 2.3 Texture Requirements

| Texture | Size | Usage |
|---------|------|-------|
| `bg_parchment.png` | 256×256 | Tiled screen background |
| `tab_active.png` | 9×24 (9-patch) | Active tab background |
| `tab_inactive.png` | 9×24 (9-patch) | Inactive tab background |
| `stat_icon_*.png` | 16×16 each | 23 icons, one per stat |
| `xp_bar_fill.png` | 4×4 tiled | Fill texture for XP bars |

---

## 3. PerkScreen / Arbre de Talents (Touche O)

### 3.1 Layout

```
┌──────────────────────────────────────────────────────┐
│  ════ ❧ ARBRE DE TALENTS ❧ ════                     │
│                                                        │
│  Points disponibles: 2                                 │
│                                                        │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐       │
│  │Combat│ │Magie │ │Survie│ │Art.  │ │Mental│       │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘       │
│  ────────────────────────────────────────────────────  │
│                                                        │
│  ╔══════════════════════════════════════════════╗      │
│  ║   Catégorie: Combat                          ║      │
│  ║                                              ║      │
│  ║   ⚔ Brute Force    🗡 Tech.Lame    🏹 Préc. ║      │
│  ║   ┌───┐            ┌───┐            ┌───┐   ║      │
│  ║   │ ★ │────┌───┐   │ ★ │────┌───┐   │ ★ │   ║      │
│  ║   │   │    │   │   │   │    │   │   │   │   ║      │
│  ║   └───┘    └───┘   └───┘    └───┘   └───┘   ║      │
│  ║    Niv.20   Niv.50  Niv.20   Niv.50  Niv.20  ║      │
│  ║                                              ║      │
│  ║   🛡 Résistance    ⚡ Rapidité    🏃 Agilité║      │
│  ║   ┌───┐            ┌───┐            ┌───┐   ║      │
│  ║   │   │────┌───┐   │   │────┌───┐   │   │   ║      │
│  ║   │   │    │   │   │   │    │   │   │   │   ║      │
│  ║   └───┘    └───┘   └───┘    └───┘   └───┘   ║      │
│  ║    Niv.20   Niv.50  Niv.20   Niv.50  Niv.20  ║      │
│  ║                                              ║      │
│  ║   (scrollable si nécessaire)                 ║      │
│  ╚══════════════════════════════════════════════╝      │
└──────────────────────────────────────────────────────┘
```

### 3.2 Components

**Title:** Same style as CharacterScreen — "❧ ARBRE DE TALENTS ❧"

**Points display:** "Points disponibles: N" in gold (#c49a3c), right-aligned below title.

**Tabs:** Same tab component as CharacterScreen.

**Content panel:** A framed box with ornate parchment border (uses border texture). Inside, stats are arranged in a grid layout — one row per stat in the selected category.

**Perk nodes (circles):**
- Each stat has 3 perk nodes arranged horizontally, connected by thin lines
- Each node is a circle, 24px diameter
- **Unlocked perk (owned):** Gold border (#c49a3c), semi-transparent gold glow, icon visible, checkmark overlay
- **Available to purchase (meets level req, has points):** Gold border with pulse animation (subtle alpha oscillation), icon visible
- **Locked (level req not met):** Gray border (#888), icon at 30% opacity, lock overlay
- **Connector lines:** Horizontal lines between nodes. If both perks at each end are unlocked → gold (#c49a3c). If either is locked → gray (#666)

**Node tooltip:** Hovering over a node shows a small tooltip box (parchment style):
- Perk name (bold, #3a1a00)
- Description (#5a3a10)
- Requirement ("Niveau XX requis" or "Cliquez pour débloquer")
- Lock/unlock status

**Click behavior:**
- Click on an available node → spend 1 point → unlock → send packet to server
- Click on locked node → no action (or show "Niveau XX requis" message)
- Click on already unlocked node → show tooltip with perk description

**Scrolling:** If the tree exceeds panel height, scroll within the content panel.

### 3.3 Texture Requirements

| Texture | Size | Usage |
|---------|------|-------|
| `panel_border.png` | 16×16 (9-patch) | Ornate frame for content panel |
| `perk_node_bg.png` | 24×24 | Perk node background |
| `perk_node_unlocked.png` | 24×24 | Unlocked perk node overlay |
| `perk_connector.png` | 8×1 | Horizontal connector line |

---

## 4. HUD (Haut-gauche)

### 4.1 Layout

```
┌──────────────────────┐
│ ╔══════════════════╗ │
│ ║ ✦ NIVEAU 34     ║ │  ← Global level, gold glow
│ ║ ▓▓▓▓▓▓▓▓▓▓░░░░░ ║ │  ← XP bar (purple-brown gradient)
│ ╚══════════════════╝ │
│                      │
│ ♥ ▓▓▓▓▓▓▓▓▓░░░░░░░ │  ← Health bar (dark red → red)
│ ☕ ▓▓▓▓▓▓▓░░░░░░░░░ │  ← Food bar (gold → orange)
│ ⚡ ▓▓▓▓░░░░░░░░░░░░ │  ← Fatigue (green/yellow/orange/red)
│ 💧 ▓▓▓▓▓▓▓▓▓▓▓░░░░ │  ← Thirst (blue)
└──────────────────────┘
```

### 4.2 Style Adaptation

The existing HUD redesign spec (2026-05-27-hud-redesign-design.md) already defines the bar components and architecture. This spec overrides the **visual style** to match Parchemin:

| Element | Old (Minimalist-Modern) | New (Parchemin) |
|---------|------------------------|-----------------|
| Background | Semi-transparent black (#000000 50%) | Semi-transparent warm beige (#d4c494 40%) with thin brown border (#8b4513, 1px) |
| Level text | White, gold glow | Dark brown (#3a1a00), gold glow (#c49a3c) |
| XP bar | Purple gradient (#4A00E0 → #8E2DE2) | Gold-brown gradient (#8b4513 → #d2691e) |
| Health bar | #8B0000 → #FF4444 | Same colors (red stays red for readability) |
| Food bar | #8B5E00 → #FFA500 | Same colors |
| Fatigue | Green → Yellow → Orange → Red | Same color logic |
| Thirst | #3399FF | Same (light blue) |
| Bar background | #000000 50% | #b8965a (matching XP bar background) |
| Icons | PNG sprites | Same icon assets, but style-adjusted to match parchment theme |

**Bar rendering:** `HudBar` already uses `graphics.fill()` with colored rectangles. With Parchemin style, we add:
- Bar background rendered as filled tan rectangle (#b8965a) instead of dark
- Bar fill: same gradient approach (N vertical slices) but with Parchemin color ranges
- Optional: 1px sepia border around the bar (#8b4513)

### 4.3 Thirst Bar Addition

The existing HUD spec (v1) did not include a thirst bar. **Add thirst bar** to the survival overlay, below fatigue, with a blue gradient (#1a5276 → #3399FF) and a water droplet icon.

### 4.4 Icons

The AI-generated 16×16 icons for the HUD:
- `hud_heart.png` — stylized parchment-style heart
- `hud_food.png` — stylized drumstick
- `hud_fatigue.png` — lightning bolt
- `hud_thirst.png` — water droplet

---

## 5. Implementation Details

### 5.1 New Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `TextureCache` | `tong.statmod.client.texture` | Loads and caches all GUI textures (parchement bg, borders, icons). Provides `drawNinePatch()` helper. |
| `NinePatchRenderer` | `tong.statmod.client.gui` | Utility to render 9-patch textures for resizable frames/panels |
| `PerkNodeWidget` | `tong.statmod.client.gui.perks` | Individual perk node circle widget (clickable, state-aware) |
| `TalentTreePanel` | `tong.statmod.client.gui.perks` | Panel that lays out stats + perks in a row-based tree |
| `ParchmentButton` | `tong.statmod.client.gui.components` | Reusable styled button (for tabs, etc.) |
| `ParchmentPanel` | `tong.statmod.client.gui.components` | Reusable styled panel with border texture |

### 5.2 Modified Classes

| Class | Changes |
|-------|---------|
| `CharacterScreen` | Replace plain rendering with parchment background, styled tabs, new StatWidget layout, ornamental headers |
| `StatWidget` | Add icon rendering, parchment-style card background, gradient XP bar, new color scheme |
| `PerkScreen` | Replace text list with TalentTreePanel, add node rendering, tooltips, point display |
| `SurvivalOverlay` | Add thirst bar, change bar colors to parchment palette, add 1px border |
| `GlobalLevelOverlay` | Add parchment background panel, change XP bar to gold-brown gradient |
| `HUDManager` | Register thirst bar (no change needed — already uses SurvivalOverlay) |
| `ClientSetup` | No changes needed |

### 5.3 Packet/Data Dependencies

- `ClientStatsCache` — unchanged. Provides level, XP, fatigue, thirst for both screens and HUD.
- `ClientPerkCache` — unchanged. Provides unlocked perks + available points.
- `SyncStatsPacket` / `SyncPerksPacket` — unchanged.

### 5.4 Texture Loading

```java
// TextureCache.java — simplified approach
public class TextureCache {
    private static final Map<String, ResourceLocation> textures = new HashMap<>();

    public static ResourceLocation get(String path) {
        return textures.computeIfAbsent(path,
            p -> new ResourceLocation(STATMod.MODID, "textures/gui/" + p));
    }

    // Draw a 9-patch region
    public static void drawNinePatch(GuiGraphics graphics, ResourceLocation tex,
                                      int x, int y, int w, int h, int border) {
        // ... 9-slice rendering using blit()
    }
}
```

Textures stored in `src/main/resources/assets/statmod/textures/gui/`:
- `bg_parchment.png`
- `tab_active.png`, `tab_inactive.png`
- `panel_border.png`
- `perk_node_bg.png`, `perk_node_unlocked.png`, `perk_connector.png`
- `xp_bar_fill.png`
- `stat_icon_0.png` through `stat_icon_22.png` (index = StatType.index)
- `hud_heart.png`, `hud_food.png`, `hud_fatigue.png`, `hud_thirst.png`

---

## 6. Performance Considerations

- Texture loading: Once at startup via `TextureCache` — negligible
- Nine-patch rendering: 9 `blit()` calls per panel — negligible (< 0.01ms)
- Perk tree: Max 7 stats × 3 nodes = 21 widgets per category — no measurable cost
- Scrolling: Vanilla `Screen` handles clipping natively
- HUD: Same cost as current implementation (fill rectangles) + 1 border line per bar

---

## 7. Decisions

- **Magic tab:** Visible but grayed-out (inactive style). When hovered, tooltip shows "En sommeil — sera actif dans une future mise à jour".
- **Perk tooltip:** Immediate on hover (no delay). Uses vanilla `Screen.renderTooltip()` with parchment-colored background.
- **Tab order:** Combat, Magie, Survie, Artisanat, Mental — kept as-is.

---

## 8. Dependencies

- Existing: `ClientStatsCache`, `ClientPerkCache`, all existing screen/keybinding classes
- Existing: `LerpedValue`, `HudBar` from HUD system
- New: `TextureCache`, `NinePatchRenderer`, `PerkNodeWidget`, `TalentTreePanel`, `ParchmentButton`, `ParchmentPanel`
- External: 32 AI-generated texture files (23 stat icons + 4 HUD icons + 5 UI textures)
- Forge: `GuiGraphics`, `Screen`, `AbstractWidget`, `ResourceLocation`

---

## 9. Implementation Order

1. `TextureCache` + `NinePatchRenderer` — texture system foundation
2. `ParchmentButton` + `ParchmentPanel` — reusable styled components
3. CharacterScreen overhaul — background, tabs, stat cards
4. HUD adaptation — thirst bar, parchment colors, borders
5. `PerkNodeWidget` + `TalentTreePanel` — perk tree components
6. PerkScreen overhaul — tabs + talent tree rendering
7. Tooltip system for perk nodes
8. Texture assets integration (AI-generated PNGs)

---

## 10. AI Texture Generation Spec

For the AI image generator, provide these prompts / specs:

### Stat Icons (16×16, transparent PNG, simple medieval icon style, sepia/brown palette)

| Index | Stat | Prompt keywords |
|-------|------|-----------------|
| 0 | Brute Force | medieval iron fist, gauntlet |
| 1 | Blade Technique | crossed swords, blade |
| 2 | Rapidité | wing, feather, wind |
| 3 | Agility | leaping figure, acrobat |
| 4 | Physical Resistance | shield, iron wall |
| 5 | Physical Endurance | heart, stamina rune |
| 6 | Precision | arrow hitting target |
| 7 | Arcane Power | magic orb, arcane rune |
| 8 | Water Affinity | water drop, wave |
| 9 | Earth Affinity | mountain, crystal |
| 10 | Fire Affinity | flame, torch |
| 11 | Air Affinity | wind swirl, cloud |
| 12 | Magic Resistance | magic shield, barrier |
| 13 | Casting Speed | lightning bolt, speed |
| 14 | Mana Pool | mana crystal, flask |
| 15 | Erudition | book, scroll, knowledge |
| 16 | Tracking | paw print, compass |
| 17 | Keen Senses | eye, ear, awareness |
| 18 | Forging | anvil, hammer |
| 19 | Cooking | cooking pot, stew |
| 20 | Alchemy | potion bottle, herbs |
| 21 | Intimidation | skull, roaring face |
| 22 | Willpower | crown, diamond, spirit |

### HUD Icons (16×16, transparent PNG)

- `hud_heart.png` — medieval stylized heart
- `hud_food.png` — medieval drumstick / roasted meat
- `hud_fatigue.png` — lightning bolt / exhaustion rune
- `hud_thirst.png` — water droplet / goblet

### UI Textures

- `bg_parchment.png` — 256×256 seamless tile (aged parchment, subtle stains, warm beige/tan)
- `tab_active.png` — 48×24 (parchment ribbon with gold border, slightly raised look)
- `tab_inactive.png` — 48×24 (parchment ribbon, flat, muted colors)
- `panel_border.png` — 32×32 (ornate medieval frame, 8px border width for 9-patch)
- `perk_node_bg.png` — 32×32 (circular parchment medallion)
- `perk_node_unlocked.png` — 32×32 (golden wreath overlay for unlocked perk)
- `perk_connector.png` — 16×4 (thin golden line segment for tree connections)
- `xp_bar_fill.png` — 8×8 (gold/amber gradient tile)

---

*Design approved 2026-05-27. Ready for implementation planning.*
