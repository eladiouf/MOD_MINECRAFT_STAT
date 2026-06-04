# STAT Mod — Architecture

## Overview

A Minecraft Forge 1.20.1 RPG mod adding 23 stats, 60+ Epic Fight skills, perks, fatigue, thirst, and weapon mastery.

## Dependency Graph

```
STATMod (entry point)
├── Config (Forge config spec, all tunables)
├── CapabilityHandler (attaches 5 capabilities)
│   ├── PlayerStats (23 stats, mana)
│   ├── FatigueManager + FatigueHandler
│   ├── ThirstManager + ThirstHandler
│   ├── PerkManager + PerkTickHandler + PerkDamageHandler
│   └── WeaponMasteryManager + WeaponXPHandler
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
