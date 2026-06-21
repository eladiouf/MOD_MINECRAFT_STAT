# Changelog

## [1.2.0] - 2026-06-21

### Added
- Unified magic tree — Iron's Spellbooks Phase 1 (common trunk + Fire school active)
- 8 magic schools structurally present (locked, pending Phase 2)
- Arcane and school point system with persistence
- IronSpellEventBridge for event-driven spell casting
- Puffish Skills mirror UI for magic tree
- `/magic` command for tree management
- Magic node unlock/gating with race affinity cost adjustment
- Client-side magic tree cache with sync on join

### Fixed
- Audit M1 — 10 critical/important/minor fixes from code review (065d57a)
- SwordSoaringClientModEventsMixin cleanup
- Magic state sync on player join/respawn

### Changed
- Port NeoForge 1.21.1 finalized (zero Forge residuals in src/)
- Perk gating integrates with Tensura skill level
- Build CI targets JDK 21, NeoForge 1.21.1

---

## [1.1.0] - 2026-06-04

### Added
- Full config externalization: XP rates, perk thresholds, skill levels, thirst rates, weapon XP, cooldown multiplier
- `README.md` with installation, commands, and progression documentation
- `CHANGELOG.md` (this file)

### Fixed
- **StatPassiveSkill UUID collision**: Each stat+tier now has a unique UUID, preventing passives from overwriting each other
- **LevelUpHandler**: Particle effects now trigger correctly per milestone level
- **FatigueHandler**: Config values are now read once at load instead of every tick
- **System.currentTimeMillis()**: Replaced with server game ticks in `StatActiveSkill` and mana blocking for deterministic cooldowns
- **ThirstHandler**: All thirst rates are now configurable
- **PerkTickHandler**: Added early return when player has no perks

### Removed
- Stale `META-INF/mods.toml` at project root (WeaponsOfMiracles artifact)

### Changed
- Perk tier thresholds now read from config (were hardcoded 20/50/80)
- Skill unlock levels now read from config (were hardcoded 20/50/80/100)
- XP formula multiplier configurable (was hardcoded 50)
- Weapon mastery max level configurable (was hardcoded 50)

---

## [1.0.0] - 2026-05-27

### Initial Release
- 23 character stats across 5 categories
- 60+ Epic Fight custom skills
- 42 perks (3 per stat)
- Fatigue system with 7 debuff thresholds
- Thirst system with dehydration effects
- Weapon mastery for 13 weapon types
- Mana pool system
- 48-minute day cycle
- Adrenaline potions and effect
- Full French and English localization
- `/statmod` admin command with list/get/set/xp/reset
- Character Screen (P) and Perk Screen (O)
- HUD: fatigue bar, global level overlay, combo counter
- ParCool integration
- AAA Particles integration
