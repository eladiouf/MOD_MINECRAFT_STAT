# STAT Mod v2 — Design Specification

**Date:** 2026-05-27
**Branch:** experiment
**Package:** tong.statmod
**Minecraft:** 1.20.1 | **Forge:** 47.4.20 | **Epic Fight:** 20.9.5

---

## 1. Overview

STAT Mod v2 is a complete rewrite experimenting with a new approach: deeper Epic Fight integration through **unlockable Epic Fight skills** tied to stat thresholds, a **23-stat system** across 5 gameplay categories, and a streamlined architecture with only 3 core systems (Stats + Fatigue + Weapon Mastery).

### 1.1 Compared to v1 (master branch)

| Aspect | v1 (master) | v2 (experiment) |
|--------|-------------|-----------------|
| Stats | 7 combat-only | 23 across 5 categories |
| Systems | 7 overlapping (fatigue, posture, flow, combo, rank, weapon, unlocks) | 3 focused (stats, fatigue, weapon mastery) |
| Skill unlocks | 28 passive modifiers | Real Epic Fight skills (animations, hitboxes, cooldowns) |
| HUD | 7 overlays | 2 overlays + 5-tab screen |
| Network | 6 packet types | 3 packet types + dirty flag |
| Language | French + English | French + English |

---

## 2. Architecture

### 2.1 Three-Layer Design

```
┌──────────────────────────────────────────────┐
│ LAYER 1: Stats Definition                    │
│ StatType enum (23 values) + StatRegistry     │
│ Defines: name, category, min/max (0-100)     │
├──────────────────────────────────────────────┤
│ LAYER 2: Progression                         │
│ PlayerStats capability (XP + levels per stat) │
│ FatigueManager capability (fatigue 0-100)    │
│ WeaponMasteryManager (weapon XP 1-50)        │
├──────────────────────────────────────────────┤
│ LAYER 3: Effects                             │
│ StatEffectApplier → LivingHurtEvent modifiers │
│ SkillUnlockHandler → EpicFight skill unlocks  │
│ FatigueEffects → Debuff thresholds            │
└──────────────────────────────────────────────┘
```

### 2.2 Data Flow

```
Player Action (hit mob, craft item, dodge)
  → ActionType detection
    → XP awarded to associated stat(s)
      → Level up check (threshold reached?)
        Yes → SkillUnlockHandler grants Epic Fight skill
              → StatEffectApplier updates damage modifiers
        No  → Dirty flag set
              → Next tick: StatUpdatePacket sent to client
```

### 2.3 Package Structure

```
tong.statmod/
├── StatMod.java                    # @Mod entry point
├── Config.java                     # Server config
├── ConfigClient.java               # Client config
│
├── stats/
│   ├── StatType.java              # Enum: 23 stats
│   ├── StatRegistry.java          # Stat registration
│   ├── StatCalculator.java        # XP curves, effect scaling
│   └── StatEffectApplier.java     # LivingHurtEvent handlers
│
├── capability/
│   ├── PlayerStats.java           # XP + levels (INBTSerializable)
│   └── PlayerStatsProvider.java   # Capability provider
│
├── progression/
│   ├── CombatXPHandler.java       # XP from Epic Fight combat
│   ├── NonCombatXPHandler.java    # XP from crafting, brewing, exploring
│   └── ActionType.java            # Action → stat XP mapping
│
├── fatigue/
│   ├── FatigueManager.java        # Fatigue logic
│   ├── FatigueEffects.java        # Threshold debuffs
│   └── FatigueHandler.java        # Event costs, sleep reset
│
├── weapon/
│   ├── WeaponMasteryManager.java  # Weapon XP/levels
│   ├── WeaponType.java            # 11 weapon types
│   └── WeaponXPHandler.java       # Combat → weapon XP
│
├── skills/
│   ├── SkillUnlockRegistry.java   # Stat×tier → EF skill map
│   ├── SkillUnlockHandler.java    # Level up → unlock trigger
│   └── skills/                    # Custom EF skill classes
│       ├── EarthSplitterSkill.java
│       ├── IaijutsuSkill.java
│       ├── ShadowStepSkill.java
│       ├── IronWallSkill.java
│       └── ...
│
├── network/
│   ├── NetworkHandler.java        # Channel registration
│   ├── SyncAllStatsPacket.java    # Full sync
│   ├── StatUpdatePacket.java      # Single stat update
│   └── FatiguePacket.java         # Fatigue sync
│
├── client/
│   ├── ClientSetup.java           # Keybinds, client events
│   ├── ClientStatsCache.java      # Client-side stat cache
│   ├── gui/
│   │   ├── CharacterScreen.java   # Main GUI (5 tabs)
│   │   └── StatWidget.java        # Level/XP bar widget
│   ├── hud/
│   │   ├── HUDManager.java        # Overlay manager
│   │   ├── ComboOverlay.java      # Hit counter
│   │   └── FatigueOverlay.java    # Fatigue bar
│   └── notification/
│       └── LevelUpToast.java      # Level-up notification
│
├── integration/
│   └── EpicFightCompat.java       # EF soft dependency hooks
│
└── command/
    └── StatsCommands.java         # /stats, /fatigue
```

---

## 3. Stat System

### 3.1 The 23 Stats

| Index | Category | Name | XP Source | Primary Effect |
|-------|----------|------|-----------|---------------|
| 0 | Combat | Brute Force | Heavy weapon damage | +1% heavy damage/point |
| 1 | Combat | Blade Technique | Sword/katana/dagger damage | +1% slash damage/point |
| 2 | Combat | Rapidité | Hits in combo window | +0.5% attack speed/point |
| 3 | Combat | Agility | Successful EF dodges | +0.3% move speed, iframe bonus |
| 4 | Combat | Physical Resistance | Damage taken | -0.5% damage received/point |
| 5 | Combat | Physical Endurance | Successful EF guards | +0.2 hearts/point |
| 6 | Combat | Precision | Bow/crossbow hits | +0.5% crit chance/point |
| 7 | Magic | Arcane Power | Magic damage dealt | +1% magic damage/point |
| 8 | Magic | Water Affinity | Fighting underwater/rain | Water breathing + swim speed |
| 9 | Magic | Earth Affinity | Mining blocks | +0.5% mining speed, knockback resist |
| 10 | Magic | Fire Affinity | Fire damage (dealt & taken) | Fire resist + fire damage bonus |
| 11 | Magic | Air Affinity | Jumping, falling, elytra | Fall damage reduction, air speed |
| 12 | Magic | Magic Resistance | Suffer potion/poison effects | -1% magic damage taken/point |
| 13 | Magic | Casting Speed | Using potions, enchanted items | Item use speed |
| 14 | Magic | Mana Pool | Passive (scales with magic XP) | Mana reserve for EF skills |
| 15 | Magic | Erudition | Discover enchantments, read books | Anvil XP cost reduction |
| 16 | Survival | Tracking | Kill hostile mobs | Detection range + loot bonus |
| 17 | Survival | Keen Senses | Explore, discover biomes | Night vision + ore detection |
| 18 | Crafting | Forging | Craft tools/weapons | Durability + repair efficiency |
| 19 | Crafting | Cooking | Cook food | Saturation bonus + effect duration |
| 20 | Crafting | Alchemy | Brew potions | Potion duration + effect potency |
| 21 | Mental | Intimidation | Kill powerful mobs (bosses) | Weak mobs flee, +dmg vs bosses |
| 22 | Mental | Willpower | Survive at < 30% HP | Resistance at low HP, reduces fatigue gain |

### 3.2 XP Progression

- **Curve:** `XP_required(level) = (level + 1)² × xp_base` (default xp_base = 10)
- **Max level:** 100 per stat
- **Base XP per action:** 5 (scaled by damage dealt / mob health)
- **Death penalty:** 10% XP loss in current stat (configurable)

**XP required table (xp_base = 10):**

| Level | XP needed | Cumulative |
|-------|-----------|-----------|
| 0 → 1 | 10 | 10 |
| 9 → 10 | 1,000 | 3,850 |
| 49 → 50 | 25,000 | 429,250 |
| 99 → 100 | 100,000 | 3,383,500 |

### 3.3 XP Sources Mapping

| Action | Awarded to |
|--------|-----------|
| Hit with axe/greatsword/mace | Brute Force |
| Hit with sword/katana/dagger | Blade Technique |
| Rapid successive hits (combo ≥5) | Rapidité |
| Successful EF dodge | Agility |
| Take damage (any source) | Physical Resistance |
| Successful EF guard/parry | Physical Endurance |
| Hit with bow/crossbow | Precision |
| Deal magic damage | Arcane Power |
| Fight in water/rain | Water Affinity |
| Mine blocks with pickaxe | Earth Affinity |
| Deal or take fire damage | Fire Affinity |
| Jump, take fall damage, use elytra | Air Affinity |
| Get hit by potion/poison/wither | Magic Resistance |
| Drink potions, use enchanted items | Casting Speed |
| Passive (1% of all magic XP gained) | Mana Pool |
| Find enchanted books, disenchant | Erudition |
| Kill hostile mobs | Tracking |
| Discover new biomes, break spawners | Keen Senses |
| Craft tools, weapons, armor | Forging |
| Cook food in furnace/smoker/campfire | Cooking |
| Brew potions | Alchemy |
| Kill bosses (Wither, Dragon, Elder Guardian) | Intimidation |
| Survive hits at < 30% HP | Willpower |

---

## 4. Fatigue System

### 4.1 Mechanics

- **Resource:** 0 (fresh) to 100 (exhausted)
- **Costs:**

| Action | Fatigue Cost |
|--------|-------------|
| Successful hit | +3 |
| Sprint (per second) | +1 |
| Jump | +2 |
| Damage taken | +5 |
| Mining a block | +1 |
| Epic Fight skill use | +10-20 (skill-dependent) |

- **Recovery:** standing still (-2/sec), sneaking (-3/sec), sleep (reset to 0)
- **Willpower synergy:** Each Willpower level reduces fatigue gain by 0.5% (max 50% reduction at level 100)

### 4.2 Threshold Debuffs

| Fatigue % | Effect |
|-----------|--------|
| ≥ 25% | -5% attack speed |
| ≥ 50% | -10% damage, -10% move speed |
| ≥ 75% | -20% damage, -20% move speed, screen vignette |
| ≥ 90% | -30% all stats, screen shake, EF skill cooldown ×1.5 |
| 100% | Cannot run/jump, -50% damage, forced walk |

---

## 5. Weapon Mastery

### 5.1 Weapon Types

11 types mapped to Epic Fight's weapon categories:

| Index | Weapon | EF Category |
|-------|--------|-------------|
| 0 | Sword | SWORD |
| 1 | Greatsword | GREATSWORD |
| 2 | Katana | KATANA |
| 3 | Spear | SPEAR |
| 4 | Dagger | DAGGER |
| 5 | Axe | AXE |
| 6 | Fist | FIST |
| 7 | Bow | BOW |
| 8 | Crossbow | CROSSBOW |
| 9 | Trident | TRIDENT |
| 10 | Shield | SHIELD |

### 5.2 Progression

- **Levels:** 1-50 per weapon type
- **XP:** Gained from dealing damage with that weapon type
- **Curve:** Same quadratic formula: `(level + 1)² × 10`
- **Synergy:** 50% of weapon XP also awarded to the associated combat stat

### 5.3 Weapon ↔ Primary Stat Mapping

| Weapon | Primary Stat |
|--------|-------------|
| Axe, Greatsword | Brute Force |
| Sword, Katana, Dagger | Blade Technique |
| Fist | Rapidité |
| Bow, Crossbow | Precision |
| Spear, Trident | Agility |
| Shield | Physical Endurance |

### 5.4 Weapon Skill Unlocks

| Weapon | Level 25 | Level 50 |
|--------|----------|----------|
| Sword | Sharp Edge (passive, +slash dmg) | Blade Waltz (common, 3-hit combo) |
| Greatsword | Colossus Slayer (passive, +dmg vs large) | Giant Slayer (common, charged slam) |
| Katana | Quick Draw (passive, first hit bonus) | Moon Slash (common, wide arc) |
| Dagger | Toxic Edge (passive, poison chance) | Assassinate (common, backstab) |
| Axe | Deep Wounds (passive, bleed) | Berserker (common, rage mode) |
| Fist | Iron Fist (passive, unarmed bonus) | Flurry (common, rapid punches) |
| Bow | Steady Aim (passive, draw speed) | Arrow Rain (common, multi-shot) |
| Crossbow | Reload Master (passive, reload speed) | Piercing Bolt (common, armor pierce) |
| Spear | Long Reach (passive, range bonus) | Whirlwind (common, AOE spin) |
| Trident | Tidal Force (passive, water bonus) | Storm Call (common, lightning) |
| Shield | Sturdy Guard (passive, block bonus) | Shield Bash (common, stun attack) |

---

## 6. Epic Fight Skill Unlocks

### 6.1 Unlock Tiers

| Tier | Threshold | Skill Category Used | Type |
|------|-----------|-------------------|------|
| Tier 1 | Stat level 10 | `passive` or `weapon_passive` | Passive always-on bonus |
| Tier 2 | Stat level 25 | `dodge` or `guard` | Defensive active skill |
| Tier 3 | Stat level 50 | `common` or `mover` | Offensive active skill |
| Tier 4 | Stat level 75 | `module` | Advanced skill with special conditions |

### 6.2 Skill Matrix (Combat Stats)

| Stat | Tier 1 (10) | Tier 2 (25) | Tier 3 (50) | Tier 4 (75) |
|------|------------|------------|------------|------------|
| Brute Force | Heavy Impact (passive) | Shatter Guard (guard) | Earth Splitter (common) | Titan's Wrath (module) |
| Blade Technique | Sharp Edge (wpn_passive) | Iaijutsu (common) | Blade Storm (common) | Deathblow (module) |
| Rapidité | Fast Hands (passive) | Adrenaline Rush (mover) | Tempest (common) | Overdrive (module) |
| Agility | Quickstep (dodge) | Shadow Step (dodge) | Wind Walker (mover) | Phantom (module) |
| Physical Resistance | Thick Hide (passive) | Iron Shell (guard) | Unbreakable (common) | Diamond Skin (module) |
| Physical Endurance | Iron Skin (passive) | Iron Wall (guard) | Last Stand (common) | Immortal (module) |
| Precision | Hawk Eye (passive) | Piercing Shot (common) | Eagle Eye (common) | Marksman (module) |

### 6.3 Skill Implementation

Each unlocked skill extends `yesman.epicfight.skill.Skill` and is registered into the appropriate `SkillCategory`:

- **Skill class structure:** Extend Skill, override `onUse()`, `onCancel()`, `isExecutable()`
- **Animations:** Reuse EF animations via `SkillDataManager`
- **Hitboxes:** Defined via EF's `Collider` system
- **Cooldowns:** EF's built-in cooldown system (`SkillContainer` has CD tracking)
- **Resource costs:** Fatigue (our custom resource) consumed on skill use
- **Localization:** `assets/statmod/lang/fr_fr.json` and `en_us.json`

### 6.4 Trigger Flow

```
PlayerStats.addXP(statIndex, amount)
  → Level up detected
    → Compare new level with unlock tiers (10, 25, 50, 75)
    → If new tier crossed:
      → Look up skill in SkillUnlockRegistry
      → Call EpicFightCompat.grantSkill(player, skill)
        → EpifFight's SkillSlots.register() or player's SkillContainer.giveSkill()
      → Send LevelUpPacket (visual feedback)
```

---

## 7. Networking

### 7.1 Packet Types

| Packet | Direction | Content | When |
|--------|-----------|---------|------|
| SyncAllStatsPacket | Server → Client | All 23 stat levels + XP + fatigue + all weapon masteries | Login, respawn, dimension change |
| StatUpdatePacket | Server → Client | Array of (statIndex, level, xp) tuples | Any XP gain or level up |
| FatiguePacket | Server → Client | Fatigue value (0-100) | Fatigue change > 5% |

### 7.2 Anti-Spam

Dirty flag per player: each tick, collect all dirty updates and batch into a single `StatUpdatePacket`. Multiple stat changes in one tick are sent as a single packet with an array of tuples. Maximum 1 packet per player per tick.

### 7.3 Sync Flow

```
Login/Respawn:
  PlayerLoggedInEvent → SyncAllStatsPacket → ClientStatsCache.fill()

During Gameplay:
  Tick start → iterate dirty stats → build StatUpdatePacket → send
  Fatigue > 5% change → FatiguePacket

Level Up:
  addXP() → mark dirty → (next tick) StatUpdatePacket
  skill unlocked → no network packet needed (EF handles its own skill sync)
```

---

## 8. Client

### 8.1 HUD Elements

**Combo Overlay** (top-center, above EF health bar):
- Shows current hit combo count
- Fades out after 3 seconds without landing a hit
- Color/scale changes at combo milestones (10, 25, 50)
- White → Yellow → Orange → Red gradient

**Fatigue Bar** (below combo, horizontal bar):
- Visible only when fatigue > 0%
- Width: 100px, height: 6px
- Color shifts green (0-25%) → yellow (25-50%) → orange (50-75%) → red (75-100%)
- Numeric percentage shown next to bar

### 8.2 Character Screen (Key: P)

Five tabs, one per stat category:

```
┌─────────────────────────────────────────────┐
│  ⚔ Combat  │  ✦ Magic  │  🌿 Survie  │ ... │
├─────────────────────────────────────────────┤
│                                              │
│  Brute Force     Lv.45 ████████████░░  45%  │
│  +45% Heavy Damage                          │
│  ✓ Heavy Impact  ✓ Shatter Guard  ✓ Earth   │
│     Splitter     ██ Earth Splitter           │
│  ─────────────────────────────────────────   │
│  Blade Technique  Lv.23 ██████░░░░░░  23%   │
│  +23% Slash Damage                          │
│  ● Heavy Impact  ██ [Lv10] → Attendez Lv25  │
│                                              │
│  ... (toutes les 7 stats combat)             │
└─────────────────────────────────────────────┘
```

### 8.3 Controls

| Key | Action |
|-----|--------|
| P | Open Character Screen |
| K | Open Epic Fight skill screen (vanilla EF, not recreated) |

### 8.4 Notifications

- **Level Up:** Brief toast bottom-right with stat name + new level + sound effect
- **Skill Unlocked:** Distinct toast with skill name + tier + icon + different sound
- **Fatigue Warning:** Screen vignette (red edges) appears at ≥75%, screen shake at ≥90%

---

## 9. Configuration

### 9.1 Server Config (statmod-server.toml)

```toml
[progression]
xp_base = 10
xp_per_hit = 5
xp_per_kill_multiplier = 2.0
death_xp_loss = 0.1

[fatigue]
sprint_cost = 1
jump_cost = 2
attack_cost = 3
damage_taken_cost = 5
mining_cost = 1
skill_cost_base = 15
recovery_rate = 2
sneak_recovery_rate = 3

[effects]
force_damage_per_point = 0.01
technique_damage_per_point = 0.01
speed_per_point = 0.005
agility_speed_per_point = 0.003
resistance_per_point = 0.005
endurance_hearts_per_point = 0.2
precision_crit_per_point = 0.005
willpower_fatigue_reduction = 0.005
```

### 9.2 Client Config (statmod-client.toml)

```toml
[hud]
combo_enabled = true
combo_position_x = 0.5
combo_position_y = 0.15
fatigue_enabled = true
fatigue_position_x = 0.5
fatigue_position_y = 0.2

[notifications]
level_up_sound = true
skill_unlock_sound = true
fatigue_vignette = true
fatigue_screenshake = true

[keys]
character_screen = 47
```

---

## 10. Implementation Order

### Phase 1: Foundation
1. StatType enum + StatRegistry
2. PlayerStats capability + PlayerStatsProvider
3. Capability registration + attach to player
4. NetworkHandler + SyncAllStatsPacket

### Phase 2: Core Progression
5. CombatXPHandler (Epic Fight events)
6. NonCombatXPHandler (crafting, brewing, exploring)
7. StatCalculator (XP curves, death penalty)
8. StatEffectApplier (LivingHurtEvent modifiers)

### Phase 3: Fatigue
9. FatigueManager
10. FatigueEffects (threshold debuffs)
11. FatigueHandler (event costs, sleep reset)
12. FatiguePacket

### Phase 4: Weapon Mastery
13. WeaponType enum
14. WeaponMasteryManager
15. WeaponXPHandler
16. Integration with CombatXPHandler

### Phase 5: Skill Unlocks + EF Integration
17. EpicFightCompat (soft dependency detection)
18. SkillUnlockRegistry (stat×tier → skill mapping)
19. Custom EF Skill classes (minimum 10 skills)
20. SkillUnlockHandler (trigger on level up)
21. Skill registration in EF systems

### Phase 6: Client
22. ClientSetup (keybinds, event registration)
23. CharacterScreen (5 tabs, StatWidget)
24. ComboOverlay + FatigueOverlay
25. LevelUpToast notifications
26. ClientStatsCache

### Phase 7: Config & Commands
27. Config.java (server toml)
28. ConfigClient.java (client toml)
29. StatsCommands (/stats, /fatigue)

### Phase 8: Polish
30. fr_fr.json + en_us.json localization files
31. Advancements (first level up, max stat, max fatigue)
32. Balance tuning pass (adjust XP rates, effect scaling)

---

## 11. Risk & Mitigation

| Risk | Mitigation |
|------|-----------|
| Epic Fight API changes between versions | Pin to 20.9.5, check API surface before implementing skills |
| 23 stats too many to balance | Start with Combat + Fatigue, add remaining categories incrementally |
| Mana Pool stat requires new resource | Defer: use fatigue as universal resource initially; Mana Pool is cosmetic + passive bonus |
| EF Skill class hierarchy is complex | Start with 2-3 skills (one per category type), validate pattern, then scale to full matrix |
| Client/server desync on skill unlocks | EF handles its own skill sync; we only fire unlock trigger server-side |
| Performance with 23 stats per player | Dirty flag pattern ensures max 1 network packet/tick; stat effects calculated on-demand |
