# STAT Mod

[![Build](https://github.com/eladiouf/MOD_MINECRAFT_STAT/actions/workflows/build.yml/badge.svg)](https://github.com/eladiouf/MOD_MINECRAFT_STAT/actions)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.20-orange)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17-red)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![CurseForge](https://img.shields.io/badge/CurseForge-Available-orange?logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/statmod)
[![Modrinth](https://img.shields.io/badge/Modrinth-Available-green?logo=modrinth)](https://modrinth.com/mod/statmod)
[![Wiki](https://img.shields.io/badge/Wiki-GitHub%20Pages-blue)](https://eladiouf.github.io/MOD_MINECRAFT_STAT/)

A deep RPG progression mod for Minecraft Forge 1.20.1, built as an **Epic Fight** addon. Adds 23 character stats, 60+ custom skills, perks, fatigue, thirst, weapon mastery, and a full leveling system.

---

## Features

- **23 Stats** across 5 categories (Combat, Magic, Survival, Crafting, Mental) — level 0-100
- **60+ Epic Fight Skills** — Weapon Passives (12), Stat Passives (21), Active Skills (7), Identity Ultimates (3), Guard Skills (3), Mover Skills (3), Non-Combat Skills (17)
- **42 Perks** — 3 per stat, unlockable at milestones
- **Fatigue System** — Accumulates from activity, recovers with rest. 7 debuff thresholds
- **Thirst System** — Decays from exertion/heat/armor. Dehydration causes debuffs and damage
- **Weapon Mastery** — 13 weapon types with individual XP and levels
- **Mana Pool** — Spellcasting resource, scales with Mana Pool stat
- **48-Minute Day Cycle** — Slower days for better pacing
- **Adrenaline Potions** — Brewable fatigue-reducing potions
- **Full Config** — Almost every value configurable via Forge config

---

## Requirements

| Mod | Version | Required |
|-----|---------|----------|
| **Minecraft Forge** | 47.4.20 | Yes |
| **Epic Fight** | 20.14.17 | Yes |
| ParCool / Epic Parcool | 20.12.0.1 | Optional |
| AAA Particles | 2.2.1 | Optional |
| Weapons of Miracles | 2.0.15 | Optional |

---

## Installation

1. Install **Minecraft Forge 1.20.1** (version 47.4.20+)
2. Install **Epic Fight** (version 20.14.17) in your `mods` folder
3. Copy `statmod-1.0.0.jar` to your `mods` folder
4. Launch Minecraft

Optional mods (ParCool, AAA Particles, Weapons of Miracles) go in `mods` too.

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
| `P` | Open Character Screen (stats overview) |
| `O` | Open Perk Screen (perk tree) |
| Custom | Class Arts (configurable) |

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

All values are in `.minecraft/config/statmod-common.toml`. Key sections:

- **XP Progression**: XP per level multiplier, XP tier ranges
- **Perks & Skills**: Unlock thresholds, cooldown multiplier
- **Thirst**: Decay rates, sprint/jump/armor costs
- **Fatigue**: Accumulation rates, costs, recovery, capacity
- **Weapon Mastery**: XP awards, max level

---

## Development

```bash
# Clone the repo
git clone -b experiment https://github.com/eladiouf/MOD_MINECRAFT_STAT.git
cd MOD_MINECRAFT_STAT

# Build
./gradlew build

# Run client
./gradlew runClient
```

---

## Credits

- **Developer**: ela_juff
- **Built with**: Minecraft Forge, Epic Fight, Parchment mappings
- **Inspiration**: RPG leveling systems from Skyrim, Dark Souls, and AuraSkills

---

## License

MIT License — see [LICENSE](LICENSE) for details.
