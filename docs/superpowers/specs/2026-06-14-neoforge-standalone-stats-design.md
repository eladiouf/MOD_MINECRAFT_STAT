# Standalone Stats System — NeoForge 1.21.1

**Date:** 2026-06-14
**Branch:** `neoforge-1.21.1`
**Goal:** Migrate STAT_MOD from Forge 1.20.1 + Epic Fight to NeoForge 1.21.1 standalone (stats, XP, leveling, 84 perks). No Epic Fight dependency.

---

## 1. Project Setup

### Build System
- Plugin: `net.neoforged.gradle.userdev` (NeoGradle 7+)
- JDK 21 (NeoForge 1.21.1 requirement)
- Mappings: Official Mojang (or Parchment if released for 1.21.1)
- Minecraft: `1.21.1`, NeoForge version TBD
- Remove ForgeGradle + Parchment Librarian plugins
- Remove all Epic Fight + Epic Fight addon dependencies

### Dependencies Kept
- JUnit Jupiter (tests)
- CurseMaven / Modrinth Maven repos (for optional deps)
- NeoForge framework only — no Epic Fight, no L2Hostility, no ParCool, etc.

### Mod Metadata
- `mod_id`: `statmod`
- `mod_group`: `tong.statmod`
- Mod file: `mods.toml` → `neoforge.mods.toml` (NeoForge format, minimal change)
- `pack_format`: 18 (MC 1.21)

---

## 2. Data Storage — NeoForge Attachments

Replace Forge Capabilities with NeoForge Data Attachments.

```
src/main/java/tong/statmod/storage/
├── PlayerStatData.java      ← int[23] levels, int[23] xp, int[23] perkPoints
├── ModAttachments.java      ← AttachmentType<PlayerStatData> registration
└── StatSerializer.java      ← readNBT/writeNBT
```

**PlayerStatData** (records-like POJO):
```java
public class PlayerStatData {
    private final int[] levels = new int[23];
    private final int[] xp = new int[23];
    private final int[] perkPoints = new int[23];
    // getters, setters, addXP, addLevel, getPerkPointsForStat, addPerkPointsForStat, etc.
}
```

**Registration:**
```java
public static final AttachmentType<PlayerStatData> STATS =
    AttachmentType.builder(PlayerStatData::new).serialize(StatSerializer.INSTANCE).build();
```

Attachment attached to `EntityType.PLAYER` in `ModAttachments.register()`.

---

## 3. Stat System — Standalone

No Epic Fight `AttributeStat`. Stats are pure integer IDs stored in `PlayerStatData`.

### StatType Enum (22 entries)

Same as current 14 active + 8 inactive (MAGIC stats kept for future):
```
BRUTE_FORCE(0), BLADE_TECHNIQUE(1), RAPIDITE(2), AGILITY(3),
PHYSICAL_RESISTANCE(4), PHYSICAL_ENDURANCE(5), PRECISION(6),
ARCANE_POWER(7), WATER_AFFINITY(8), EARTH_AFFINITY(9), FIRE_AFFINITY(10),
AIR_AFFINITY(11), MAGIC_RESISTANCE(12), CASTING_SPEED(13),
MANA_POOL(14), ERUDITION(15),
TRACKING(16), KEEN_SENSES(17),
FORGING(18), COOKING(19), ALCHEMY(20),
INTIMIDATION(21), WILLPOWER(22)
```

### File structure:
```
stats/
├── StatType.java              ← enum (copied from current, unchanged)
├── StatRegistry.java          ← static init + names (no DeferredRegister needed — pure Java)
├── StatEffectApplier.java     ← @SubscribeEvent methods (vanilla events)
└── StatCommand.java           ← /stats, /level, /respec (ported from StatsCommands)
```

12 magic stats (ARCANE_POWER through ERUDITION) have no perks and no effects yet — reserved for future magic system.

### Effect Mapping (via vanilla events)

| Stat | Vanilla Event | Effect |
|---|---|---|
| BRUTE_FORCE | `LivingHurtEvent` (attacker) | +0.5% damage per level |
| BLADE_TECHNIQUE | `LivingHurtEvent` (attacker) | +0.5% damage per level (multiplicative with BF) |
| RAPIDITE | PlayerTickEvent | Sync `ATTACK_SPEED` attribute: +1% per level |
| AGILITY | PlayerTickEvent | Sync `MOVEMENT_SPEED` attribute: +0.5% per level |
| PHYSICAL_RESISTANCE | `LivingHurtEvent` (victim) | -0.5% damage taken per level |
| PHYSICAL_ENDURANCE | `LivingHurtEvent` | Bonus absorption scaling |
| PRECISION | ProjectileHitEvent | +1% projectile damage per level |
| TRACKING | PlayerTickEvent | Extended reach/glow |
| KEEN_SENSES | `LivingHurtEvent` | Dodge chance |
| FORGING | `AnvilRepairEvent` | Reduced repair cost |
| COOKING | `PlayerEvent.ItemCraftedEvent` | Bonus saturation |
| ALCHEMY | `BrewingRecipeRegisterEvent` | Longer potion duration |
| INTIMIDATION | `LivingHurtEvent` | Bonus damage to same targets |
| WILLPOWER | PlayerTickEvent | Faster status effect decay |

---

## 4. XP & Leveling System

### XP Curve (unchanged)
```java
int requiredXP(int level) {
    return (level + 1) * (level + 1) * 10;
}
```

### Global Player Level
```java
int globalLevel = Arrays.stream(levels).sum() / StatType.values().length;
```
Range: 0–100. Drives perk point grants and other milestone events.

### Perk Points (unchanged from current system)
- Every 10 global levels (10, 20, 30, ... 100): +1 perk point to **every** stat
- Total: 10 points per stat (enough to unlock all 6 perks in a tree)
- Points are per-stat: `perkPoints[stat.index]`

### Events to Wire (TBD — Focus on structure first)
The XP gain events are not part of this initial scope. The system stores data and applies effects, but no XP is earned yet. This will be wired after the core structure is complete.

---

## 5. Perk System — 84 Perks

Ported directly from the current codebase. Files:

```
perks/
├── PerkTier.java         ← CORE(10), ACTIVE(25), SYNERGY(40), SITUATIONAL(55), MASTERY(75), TRANSCENDENCE(95)
├── Perk.java             ← 84 entries, IDs 0-83, 6 per active stat
├── PerkManager.java      ← per-stat points, unlock logic, synergy checks
├── PerkState.java        ← runtime tracking (cooldowns, combos, kills, etc.)
└── PerkEffectHandler.java ← all 84 effects (tick, attack, defense, status, item)
```

### Adaptations for NeoForge / No Epic Fight
- `PerkEffectHandler`: Replace Epic Fight events with vanilla analogues
  - `ServerPlayerOnAttackEvent` → `LivingHurtEvent` (check attacker is Player)
  - `PlayerEvent.PlayerTickEvent` → still works (vanilla)
- `PerkManager`: `LevelUpHandler` now triggers milestone grants directly (no Epic Fight level event)
- `PerkState`: No changes needed (pure Java maps)

---

## 6. GUI

```
client/gui/
├── PerkScreen.java          ← Multitab screen (ported, adapted for NeoForge Screen API)
├── PerkNodeWidget.java      ← Widget with tier colors (ported)
├── TalentTreePanel.java     ← 6-node layout (ported)
└── StatsOverviewScreen.java ← NEW — simple list of stats + levels + XP
```

### NeoForge GUI changes
- `Screen` superclass stays (Mojang-mapped)
- Button/Widget rendering: `RenderSystem` → `GuiGraphics` (NeoForge 1.21 uses Mojang's GUI system)
- `PoseStack` → `GuiGraphics.Pose` (mostly compatible)
- Font rendering: `font.draw(graphics, text, x, y, color)`

---

## 7. Network — NeoForge Payloads

Replace Forge `SimpleChannel` with NeoForge `PayloadRegistrar` (play payloads).

```
network/
├── SyncPerksPayload.java   ← int[] perkIds, int[] perStatPoints (formerly SyncPerksPacket)
├── UnlockPerkPayload.java  ← int perkId (formerly UnlockPerkPacket)
└── BatchSyncPayload.java   ← full player data sync (formerly BatchSyncPacket)
```

### Key difference
NeoForge uses `CustomPacketPayload` with codecs instead of FHSSimpleChannel:
```java
public record SyncPerksPayload(int[] perkIds, int[] perStatPoints) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPerksPayload> TYPE = ...;
    public static final StreamCodec<ByteBuf, SyncPerksPayload> CODEC = ...;
}
```

---

## 8. Files to Port (from current codebase)

### Direct copy with minor adaptations:
| File | Adaptation needed |
|---|---|
| `Perk.java` | None (pure Enum) |
| `PerkTier.java` | None (pure Enum) |
| `PerkManager.java` | Use `PlayerStatData` instead of `ICombatLevels` capability |
| `PerkState.java` | None (pure Java static maps) |
| `StatType.java` | None (pure Enum) |
| `StatsCommands.java` | Use NeoForge `CommandSourceStack`, `PlayerStatData` |
| `ClientPerkCache.java` | Minimal (network payload change) |

### Rewritten files:
| File | Why |
|---|---|
| `STATMod.java` | New mod class for NeoForge (`@Mod` from `net.neoforged.fml`) |
| `ModAttachments.java` | New (NeoForge attachments system) |
| `PlayerStatData.java` | New (replaces `ICombatLevels` + `CombatLevels`) |
| `NetworkHandler.java` | Rewrite for NeoForge payloads |
| `PerkScreen.java` | Adapt for NeoForge 1.21 GUI APIs |
| `TalentTreePanel.java` | Adapt for NeoForge 1.21 GUI APIs |
| `PerkNodeWidget.java` | Adapt for NeoForge 1.21 GUI APIs |
| `LevelUpHandler.java` | Adapt for NeoForge events, remove Epic Fight |

### Files NOT ported (Epic Fight dependent, removed):
- `EpicFightCompat.java`, `EpicParcoolCompat.java`
- `SkillRegistry.java`, `SkillRequirementRegistry.java`, `SkillUnlockRegistry.java`
- `StatModSkillCategories.java`, `StatModSkillSlots.java`
- `WeaponMasteryManager.java`
- `MobSkillEngine.java`, `MobSkillState.java`
- `L2Hostility` integration
- `FTB` integrations
- All Epic Fight addon compats
- `PerkDamageHandler.java`, `PerkTickHandler.java`, `PerkStatusHandler.java`, `PerkItemHandler.java`

---

## 9. Implementation Order

1. Scaffold NeoForge project (build.gradle, gradle.properties, main class)
2. Storage layer (PlayerStatData, ModAttachments)
3. Stat system (StatType, StatRegistry, effect stubs)
4. XP/Leveling (LevelUpHandler, global level calc)
5. Perks (port PerkTier, Perk, PerkManager, PerkState)
6. Network (payloads, sync on login/join)
7. GUI (PerkScreen, TalentTreePanel, PerkNodeWidget, StatsOverviewScreen)
8. Perk effects (port PerkEffectHandler)
9. Test + build verification
10. XP gain events (later phase)

---

## 10. Open Questions / Future Work
- Tensura Reincarnated integration (post-MVP)
- XP gain events (wired after core is stable)
- 12 magic stats (ARCANE_POWER–ERUDITION) — no perks, no effects yet
- Weapon mastery (removed with Epic Fight, may be redesigned later)
- Mob skills engine (removed with Epic Fight/L2Hostility)
