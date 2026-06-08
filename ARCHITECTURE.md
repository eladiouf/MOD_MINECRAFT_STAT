# STAT Mod — Architecture

## Overview

A Minecraft Forge 1.20.1 RPG mod adding 23 stats, 60+ Epic Fight skills, perks, fatigue, thirst, weapon mastery, and stat-driven mob skills.

## Dependency Graph

```
STATMod (entry point)
├── Config (Forge config spec, all tunables)
├── CapabilityHandler (attaches 5 player + 1 mob capability)
│   ├── PlayerStats (23 stats, mana)
│   ├── FatigueManager + FatigueHandler
│   ├── ThirstManager + ThirstHandler
│   ├── PerkManager + PerkTickHandler + PerkDamageHandler
│   ├── WeaponMasteryManager + WeaponXPHandler
│   ├── MobStats (23 stat levels per Mob entity)
│   └── MobSkillState (per-mob cooldowns + mana, NBT persisted)
├── Mob Stats System (Phase 1)
│   ├── MobStats / MobStatsProvider (Forge capability)
│   ├── MobStatCalculator (level → bonus formulas)
│   ├── MobStatEffectApplier (writes bonuses to mob attributes)
│   ├── MobStatInitializer (applies JSON defaults to a mob)
│   ├── MobStatReloadListener (loads data/statmod/mob_stats/*.json)
│   └── data/statmod/mob_stats/minecraft/*.json (13 vanilla profiles)
├── Mob Skills Engine (Phase 2)
│   ├── MobSkill (interface) + MobSkillRegistry (static, 8 hardcoded)
│   ├── MobSkillState / MobSkillStateProvider (capability — cooldowns + mana NBT)
│   ├── MobSkillLoadout (record) + MobSkillReloadListener (data/statmod/mob_skills/)
│   ├── MobSkillTickHandler (server tick driver, configurable interval)
│   ├── ReflectSkillHandler (reactive Reflect via LivingHurtEvent)
│   └── 8 skills: Charge, Whirlwind, Lunge, Reflect, Fireball, Curse, MagicMissile, BattleCry
├── Reload Listeners
│   ├── BossRewardReloadListener (data/statmod/boss_rewards/*.json)
│   ├── MobStatReloadListener (data/statmod/mob_stats/*.json)
│   └── MobSkillReloadListener (data/statmod/mob_skills/*.json)
├── integration/ (optional — only registered when target mod is loaded)
│   ├── L2HostilityMobSync — L2H MobTraitCap.lv + traits → MobStats
│   ├── L2HPlayerResistance — player stat-based reduction of L2H trait damage
│   ├── L2HostilityScaling — L2H level → XP gain multiplier
│   └── L2HostilityMobSkillSync — L2H traits unlock extra mob skills (Phase 2)
├── Progression
│   ├── ActionXpHelper (XP award + level-up trigger)
│   ├── CombatXPHandler (combat XP sources)
│   ├── NonCombatXPHandler (non-combat XP sources)
│   └── LevelUpHandler (milestones, particles, perk points)
├── Skills (Epic Fight integration)
│   ├── SkillRegistry (builds 60+ skills via SkillBuildEvent)
│   ├── SkillUnlockRegistry (maps stat+tier → skill)
│   ├── SkillRequirementRegistry (maps skill → stat requirements)
│   ├── StatActiveSkill / StatPassiveSkill / StatGuardSkill
│   ├── WeaponPassiveSkill / IdentitySkill / MoverSkill / NonCombatSkill
│   └── StatModSkillCategories / StatModSkillSlots (CLASS_ARTS)
├── Stats System
│   ├── StatType (23-stat enum + categories)
│   ├── StatCalculator (all scaling formulas)
│   └── StatEffectApplier (applies bonuses to attributes)
├── Network (SimpleChannel, 6 packet types)
├── Client (GUI: CharacterScreen, PerkScreen, HUD, LevelUpToast)
└── API (IStatModPlugin — addon interface)
```

## Data Flow

```
Player Action → Event Handler → CapabilityHelper.withStats()
    → stats.addXp() → LevelUpHandler.onLevelUp()
    → SkillUnlock + PerkPointGrant + StatEffectApplier.applyAllBonuses()
    → NetworkHandler.sendToPlayer() → ClientStatsCache.updateStat()
```

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Server-authoritative XP | Prevents client-side cheating |
| Capabilities for storage | Forge standard, persists across death, serializable |
| Event-driven XP | Clean separation from game logic |
| UUID per stat+tier for passives | Prevents passive overwrites between stats |
| Game ticks for cooldowns (not System.currentTimeMillis) | Deterministic, survives server lag |
| Configurable everything | Server admins need fine control |

## Extension Points

1. **Add a stat**: StatType enum + StatCalculator formula + EffectApplier + XP source
2. **Add a perk**: Perk enum + PerkTickHandler or PerkDamageHandler
3. **Add a skill**: SkillRegistry builder + SkillUnlockRegistry mapping + SkillRequirementRegistry
4. **Addon mods**: Implement `IStatModPlugin` (auto-discovered via ServiceLoader)
5. **Add mob stat profile** (Phase 1): drop a JSON in `data/statmod/mob_stats/<ns>/<mob>.json` with `entity_type` and `base_levels` keyed by `StatType` enum names. Reload via `/reload`.
6. **Add boss reward** (Phase 0): drop a JSON in `data/statmod/boss_rewards/<boss>.json` with `entity_type`, `items`, `xp_per_stat`, `message`.
7. **Add a mob skill** (Phase 2): implement `MobSkill` interface, register via `MobSkillRegistry.register(...)` in `STATMod.commonSetup()` or an addon plugin's setup.
8. **Add a mob skill loadout** (Phase 2): drop a JSON in `data/statmod/mob_skills/<ns>/<mob>.json` with `entity_type`, `global_cooldown_ticks`, and `skills` array (`{ "id", "weight" }`).

## Phase Status (2026-06-08)

- ✅ Phase 0 — Critical fixes (StatPassiveSkill, ServerValidator, MobScalingHandler cap, BossLoot JSON datapack, DiscordPresence removed)
- ✅ Phase 1 — MobStats capability + 13 vanilla mob JSON profiles + L2Hostility bridges (`L2HostilityMobSync`, `L2HPlayerResistance`, `L2HostilityScaling`)
- ✅ Phase 2 — Mob Skills Engine (8 skills, JSON loadouts for 8 mobs, `L2HostilityMobSkillSync` bridge)

## L2H Trait Mappings

**MobStats (Phase 1)** — 20 trait → stat bonuses in `L2HostilityMobSync.applyTraitBonus`. Examples: `fiery` → +30 `FIRE_AFFINITY`, `regen` → +25 `PHYSICAL_ENDURANCE`, `aura` → +25 `INTIMIDATION`.

**MobSkills (Phase 2)** — 6 trait → skill unlocks in `L2HostilityMobSkillSync.augment`: `aura`→BattleCry, `killer_aura`→MagicMissile, `fiery`→Fireball, `gravity`→Charge, `dispell`→Curse, `master`→[BattleCry+MagicMissile+Fireball].
