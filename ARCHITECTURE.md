# STAT Mod — Architecture

## Overview

A Minecraft Forge 1.20.1 RPG mod adding 23 stats, 60+ Epic Fight skills, perks, fatigue, thirst, and weapon mastery.

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
│   └── MobStats (23 stat levels per Mob entity)
├── Mob Stats System (Phase 1)
│   ├── MobStats / MobStatsProvider (Forge capability)
│   ├── MobStatCalculator (level → bonus formulas)
│   ├── MobStatEffectApplier (writes bonuses to mob attributes)
│   ├── MobStatInitializer (applies JSON defaults to a mob)
│   ├── MobStatReloadListener (loads data/statmod/mob_stats/*.json)
│   └── data/statmod/mob_stats/minecraft/*.json (13 vanilla profiles)
├── Reload Listeners
│   ├── BossRewardReloadListener (data/statmod/boss_rewards/*.json)
│   └── MobStatReloadListener (data/statmod/mob_stats/*.json)
├── integration/ (optional — only registered when target mod is loaded)
│   ├── L2HostilityMobSync — L2H MobTraitCap.lv + traits → MobStats (PLANNED)
│   └── L2HPlayerResistance — player stat-based reduction of L2H trait damage (PLANNED)
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

## Phase 1 Status (2026-06-08)

- ✅ T1–T6 implemented: `MobStats` capability is attached to every mob, initialized from JSON when L2Hostility is absent.
- ⏳ T7 (`L2HostilityMobSync`) and T8 (`L2HPlayerResistance`) are deferred. L2Hostility's Gradle build requires JVM 25 (we run JVM 21), so the L2H JAR is not available as a `compileOnly` dependency in this environment. Two implementation paths remain open:
  - Build L2H on a JVM 25 host once and check the resulting JAR into `libs/` for `compileOnly` use, then implement T7/T8 with direct imports.
  - Build a reflection-based `L2HCompat` shim and implement T7/T8 against it so STAT Mod stays decoupled from the L2H JAR at compile time.

## L2H Trait Mapping (planned for T7)

20 L2H trait → STAT Mod stat bonuses are specified in
`docs/superpowers/plans/2026-06-07-phase1-mob-stats-l2h-bridge.md` (Task 7, `applyTraitBonus`).
Examples: `fiery` → +30 `FIRE_AFFINITY`, `regen` → +25 `PHYSICAL_ENDURANCE`, `aura` → +25 `INTIMIDATION`.
