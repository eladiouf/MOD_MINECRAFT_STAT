# HUD Redesign — Design Spec

**Date:** 2026-05-27
**Status:** Approved
**Scope:** Custom survival HUD replacing vanilla health/food bars + global level display

---

## 1. Overview

Replace the vanilla Minecraft HUD bars (health, food) with a custom minimalist-modern styled HUD that also displays:
- **Global level** (average of all 23 stats, rounded to integer)
- **XP progress** toward next global level
- **Fatigue** bar (already exists, redesigned)
- **Health** bar (replaces vanilla)
- **Food** bar (replaces vanilla)

No combo counter. The HUD is always visible during gameplay.

## 2. Architecture

```
client/hud/
├── HUDManager.java              — overlay registration
├── overlays/
│   ├── SurvivalOverlay.java     — health + food + fatigue (single overlay)
│   └── GlobalLevelOverlay.java  — global level + XP bar
├── components/
│   ├── AnimatedBar.java         — reusable bar with lerp + gradient
│   └── GlowText.java            — text with glow/pulse effect
├── animation/
│   └── LerpedValue.java         — smooth interpolation (exponential chase)
└── textures/statmod/
    └── icons.png                — sprite sheet for icons (heart, food, lightning)
```

### Responsibilities

| Component | Purpose |
|-----------|---------|
| `HUDManager` | Registers overlays via `RegisterGuiOverlaysEvent`, hides vanilla bars |
| `SurvivalOverlay` | Renders health, food, fatigue bars in bottom-left |
| `GlobalLevelOverlay` | Renders global level + XP bar above survival bars |
| `AnimatedBar` | Reusable bar component: gradient fill, rounded corners, lerp animation |
| `GlowText` | Text with shadow + glow effect, pulse on critical events |
| `LerpedValue` | Exponential interpolation for smooth animations |

## 3. Screen Layout

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                                                         │
│                                                         │
│                                                         │
│                                                         │
│                                                         │
│                                                         │
│  ┌──────────────────┐                                   │
│  │ ★ Niveau 47      │                                   │
│  │ ████████████░░░░  │  ← XP bar (gradient bleu-violet) │
│  │                   │                                   │
│  │ ❤ ██████████░░░░  │  ← Health (gradient rouge)       │
│  │ 🍖 ████████░░░░░  │  ← Food (gradient orange)        │
│  │ ⚡ ████████████░░  │  ← Fatigue (vert → rouge)        │
│  └──────────────────┘                                   │
│                                                         │
│              [hotbar vanilla]                           │
└─────────────────────────────────────────────────────────┘
```

- **Position:** Bottom-left corner, above hotbar
- **Spacing:** 2px gap between bars, 6px gap between level section and bars
- **Bar dimensions:** 80px wide × 6px tall (compact, modern)
- **Level text:** Left-aligned, Archivo-style (vanilla font), 10px

## 4. Components

### 4.1 LerpedValue

Inspired by Create's `LerpedFloat`:

```java
public class LerpedValue {
    private float current;
    private float target;
    private float speed; // exponential chase rate (0.0–1.0)

    public void chase(float target);  // set new target
    public void tick();               // advance animation each frame
    public float getValue(float partialTicks); // interpolated value for rendering
}
```

- Default speed: `0.15f` (smooth, ~0.5s to converge)
- Critical speed: `0.3f` (faster for damage/shake)
- Uses exponential interpolation: `current += (target - current) * speed`

### 4.2 AnimatedBar

Reusable bar component:

```java
public class AnimatedBar {
    private LerpedValue fillAmount;
    private int colorStart;   // gradient start (ARGB)
    private int colorEnd;     // gradient end (ARGB)
    private boolean pulsing;  // pulse when critical

    public void render(GuiGraphics graphics, int x, int y, int width, int height);
    public void setFill(float amount); // 0.0–1.0
    public void setPulsing(boolean pulse);
}
```

Rendering:
1. Draw background: `fill()` with semi-transparent black (0x80000000)
2. Draw filled portion: gradient from `colorStart` to `colorEnd` (horizontal)
3. If pulsing: modulate alpha with sine wave (period 0.8s)
4. Rounded corners via 4 `fill()` calls trimming corners (1px)

Gradient rendering approach:
- Draw N vertical slices (8–10) across the filled width
- Each slice interpolates color between `colorStart` and `colorEnd`
- Creates a smooth horizontal gradient without shaders

### 4.3 GlowText

```java
public class GlowText {
    public static void render(GuiGraphics graphics, Font font, String text,
                              int x, int y, int color, int glowColor, float glowAlpha);
}
```

- Draw text shadow (offset +1,+1, dark color)
- Draw glow layer (same text, glow color, alpha 0.3, offset -1,-1)
- Draw glow layer (same text, glow color, alpha 0.15, offset -2,-2)
- Draw main text (full color)

### 4.4 SurvivalOverlay

Single overlay rendering health, food, and fatigue stacked vertically.

- **Health:** Lerped value from `player.getHealth() / player.getMaxHealth()`
  - Gradient: #8B0000 → #FF4444
  - Pulse when < 20% (alpha oscillation)
  - Shake on damage (2px horizontal, 0.2s)

- **Food:** Lerped value from `player.getFoodData().getFoodLevel() / 20.0`
  - Gradient: #8B5E00 → #FFA500
  - Pulse when < 3 (food level)

- **Fatigue:** Lerped value from `ClientStatsCache.getFatigue() / 100.0`
  - Color based on value:
    - 0–25%: Green (#006400 → #00FF00)
    - 25–50%: Yellow (#FFD700)
    - 50–75%: Orange (#FF8C00)
    - 75–100%: Red (#FF0000)

Icons: Small PNG icons rendered to the left of each bar (heart, drumstick, lightning bolt).

### 4.5 GlobalLevelOverlay

Displays the player's global level above the survival bars.

- **Level:** Average of all 23 stat levels from `ClientStatsCache`
- **XP:** Average XP progress across all stats
- **Text:** "★ Niveau X" with glow effect (gold glow)
- **XP Bar:** Same AnimatedBar component, gradient #4A00E0 → #8E2DE2

Level up animation:
1. Flash white overlay (full screen, alpha 0.6 → 0 in 0.5s)
2. Text "Niveau X !" scales from 1.5x → 1.0x over 0.3s
3. Glow intensifies (alpha 0.8 → 0.2 over 1s)
4. Particle burst (simple: 8–12 small rectangles flying outward)

## 5. Animations & Effects

| Trigger | Effect | Duration |
|---------|--------|----------|
| Bar value changes | Lerp to new value | 0.5s |
| Health < 20% | Pulse alpha (0.6–1.0) | Continuous |
| Health < 10% | Pulse + red glow | Continuous |
| Take damage | Shake horizontal (2px) | 0.2s |
| Food < 3 | Pulse alpha | Continuous |
| Fatigue > 75% | Pulse red | Continuous |
| Global level up | Flash + scale + glow + particles | 1.5s |
| Stat level up (any) | Subtle glow flash on level text | 0.5s |

## 6. Colors

| Element | Primary | Gradient End | Notes |
|---------|---------|-------------|-------|
| Health bar | #8B0000 | #FF4444 | Dark → bright red |
| Food bar | #8B5E00 | #FFA500 | Dark → bright orange |
| Fatigue (low) | #006400 | #00FF00 | Green |
| Fatigue (mid) | #B8860B | #FFD700 | Gold |
| Fatigue (high) | #8B0000 | #FF0000 | Red |
| XP bar | #4A00E0 | #8E2DE2 | Purple gradient |
| Level text | #FFFFFF | — | White with gold glow |
| Bar background | #000000 | — | 50% alpha |
| Panel background | #1A1A1A | — | 40% alpha |

## 7. Vanilla Bar Handling

The mod must hide the vanilla health and food bars to avoid duplication:

```java
// In HUDManager or SurvivalOverlay
@SubscribeEvent
public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
    if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) ||
        event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
        event.setCanceled(true);
    }
}
```

## 8. Performance

- `LerpedValue.tick()` called once per client tick (20 TPS) — negligible cost
- Gradient rendering: 8–10 `fill()` calls per bar — negligible
- GlowText: 3 extra `drawString()` calls — negligible
- No per-frame allocations — all objects reused
- Total: < 0.1ms per frame, no measurable impact

## 9. Dependencies

- Existing: `ClientStatsCache` (provides stat levels, XP, fatigue)
- Existing: `HUDManager` (overlay registration)
- New: `LerpedValue`, `AnimatedBar`, `GlowText` (utility classes)
- New: `icons.png` texture (16x16 icons for heart, food, lightning)
- Forge: `RegisterGuiOverlaysEvent`, `RenderGuiOverlayEvent`, `GuiGraphics`

## 10. Open Questions

- **Hide vanilla XP bar?** The vanilla XP bar shows Minecraft XP. The mod's global level XP is different. Keep vanilla XP bar visible.
- **Configurable position?** Future enhancement — not in v1.
- **Keybind to toggle HUD?** Not needed for v1 — HUD is always visible.

---

*Design approved 2026-05-27. Ready for implementation planning.*
