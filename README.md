# STAT Mod

[![Build](https://github.com/eladiouf/MOD_MINECRAFT_STAT/actions/workflows/build.yml/badge.svg)](https://github.com/eladiouf/MOD_MINECRAFT_STAT/actions)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.228-orange)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-red)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![CurseForge](https://img.shields.io/badge/CurseForge-Available-orange?logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/stat-mod-rpg)
[![Modrinth](https://img.shields.io/badge/Modrinth-Available-green?logo=modrinth)](https://modrinth.com/mod/statmod)
[![Wiki](https://img.shields.io/badge/Wiki-GitHub%20Pages-blue)](https://eladiouf.github.io/MOD_MINECRAFT_STAT/)

A deep RPG progression mod for Minecraft **NeoForge 1.21.1**. `STAT Mod` acts as a progression authority for stats, perks, stamina, survival pressure, and optional multi-mod combat or magic integrations.

---

## Features

- **23 Stats** across 5 families, with XP, thresholds, and stat-driven effects
- **84 Perks** mirrored into the current Puffish-driven perk tree flow
- **Unified stamina, fatigue, and thirst** systems for day-to-day pacing and combat pressure
- **Weapon mastery** and stat-driven progression rewards
- **48-minute Overworld day cycle** with sleep and recovery systems
- **Epic Fight integration** for native combat-facing skill UX where available
- **Puffish Skills integration** for perk and magic tree presentation
- **Magic groundwork** for unified progression across external spell systems
- **Configurable tuning** through the common config surface

---

## Requirements

| Mod | Version | Required |
|-----|---------|----------|
| **Minecraft** | 1.21.1 | Yes |
| **NeoForge** | 21.1.228 or above | Yes |
| **Java** | 21 | Yes |
| Epic Fight and other integrations | Matching the active modpack/runtime line | Optional / feature-dependent |

---

## Installation

1. Install **Minecraft 1.21.1**
2. Install **NeoForge 21.1.228 or above**
3. Copy the `STAT Mod` jar into your `mods` folder
4. Add any optional integration mods that your pack expects
5. Launch Minecraft

If you run combat, perk-tree, or magic integrations, make sure those mods match the active runtime line of your pack.

---

## Commands

`/statmod` — Admin command for stat management
- `/statmod list [player]` — List all stats and levels
- `/statmod get <stat> [player]` — Get a specific stat level
- `/statmod set <stat> <level> [player]` — Set stat level (0-100)
- `/statmod xp add <stat> <amount> [player]` — Add XP to a stat
- `/statmod reset [player]` — Reset all stats

---

## Keybindings

| Key | Action |
|-----|--------|
| `P` | Open the main character / progression screen |
| `O` | Open the perk tree flow |
| Custom | Integration-specific combat or class actions |

---

## Progression System

### Stat Leveling (0-100)
- XP curve: `(level + 1) * 50` XP per level (configurable)
- Combat: awarded on hits, combos, overkill, blocking
- Non-combat: mining, crafting, smelting, brewing, swimming, jumping
- XP tiers: COMMON (2-3), INTERMEDIATE (5-8), RARE (12-20)

### Skill Tiers
- Level 20 → Tier 1 Passive
- Level 50 → Tier 2 Passive
- Level 80 → Tier 3 Passive
- Level 100 → Active Skill

### Perk Points
- Level 20, 40, 50, 60, 80 → +1 point each
- Level 100 → +3 points

---

## Configuration

Core tuning lives in `.minecraft/config/statmod-common.toml`. Key sections include:

- **XP Progression**: XP per level multiplier, XP tier ranges
- **Perks & Skills**: Unlock thresholds, perk or skill behavior
- **Thirst**: Decay rates, sprint/jump/armor costs
- **Fatigue**: Accumulation rates, costs, recovery, capacity
- **Weapon Mastery**: XP awards, max level
- **Stamina / Time**: day-cycle pacing, recovery, and related balance knobs

---

## Development

```bash
# Clone the repo
git clone -b neoforge-1.21.1 https://github.com/eladiouf/MOD_MINECRAFT_STAT.git
cd MOD_MINECRAFT_STAT

# Build
./gradlew build

# Run client
./gradlew runClient
```

---

## Credits

- **Developer**: ela_juff
- **Built with**: NeoForge, Java 21, optional multi-mod integrations
- **Inspiration**: RPG leveling systems from Skyrim, Dark Souls, and AuraSkills

---

## License

MIT License — see [LICENSE](LICENSE) for details.
