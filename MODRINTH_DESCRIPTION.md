# STAT Mod

**A complete RPG progression authority for NeoForge 1.21.1** — stats, XP, perks, a unified magic tree, and an endless procedural Trial Dungeon.

![STAT Mod skill tree](https://raw.githubusercontent.com/eladiouf/MOD_MINECRAFT_STAT/neoforge-1.21.1/docs/wiki/assets/screenshots/skill-tree.png)

## ⚔️ Core Features

### 23 Stats & 84 Perks
- **14 active stats** (Strength, Agility, Vitality, Forging…) each with **6 perk tiers** — 84 perks total
- **8 magic stats** powering a unified magic economy (`magicPoints` + stat gates)
- XP from combat, defense, and non-combat activities (mining, crafting, farming…)
- Full GUI: stat tabs, talent tree panel, cosmetic screens

### 🏰 Trial Dungeon
An **endless procedural dungeon dimension** with 100 rotating themes across 10 arcs:

![STAT Mod Trial Dungeon architecture map rendered from its Java sources](https://raw.githubusercontent.com/eladiouf/MOD_MINECRAFT_STAT/neoforge-1.21.1/docs/wiki/assets/screenshots/dungeon-architecture-map.png)

The 4K architectural overview above is generated from the real Floor 0 coordinates,
the 20-room floor route, objective cadence, vertical offsets, chapter definitions,
and floor spacing used by the mod.

![Trial Dungeon encounter](https://raw.githubusercontent.com/eladiouf/MOD_MINECRAFT_STAT/neoforge-1.21.1/docs/wiki/assets/screenshots/dungeon-encounter.png)
- Serpentine room-chain floors, giant open-sky boss arenas, treasure vaults
- **Predefined boss roster** (40+ bosses) summoned at altars
- **Dungeon Rush**: kill combos, jackpots, flawless-floor bonuses, personal records
- **Points economy**: kills and floor conquest award dungeon points, then the Floor 0 exchanger or `/dungeon convert [amount]` converts them into spendable physical `FDP_cfa` currency
- Secret rooms, ultra-secret sanctuaries with unique relic weapons, command-block traps
- Team-based co-op conquest (FTB Teams) with shared unlocks and assist points
- **Mage mobs** with real caster AI: kiting, spell telegraphs, squad cohesion, cast interruption counterplay

![Dungeon point conversion and Magic Banker](https://raw.githubusercontent.com/eladiouf/MOD_MINECRAFT_STAT/neoforge-1.21.1/docs/wiki/assets/screenshots/magic-banker.png)

### 🔮 Deep Mod Integrations (all optional)

![STAT Mod unified magic tree rendered from its real resources](https://raw.githubusercontent.com/eladiouf/MOD_MINECRAFT_STAT/neoforge-1.21.1/docs/wiki/assets/screenshots/magic-tree-render.png)

The unified magic tree contains **246 nodes across 9 schools**, with branch progression, spell unlocks, school points, stat requirements, and optional Tensura race affinities. The 4K overview above is rendered directly from the same node positions, connections, and spell icons used in game.

![STAT Mod spell codex](https://raw.githubusercontent.com/eladiouf/MOD_MINECRAFT_STAT/neoforge-1.21.1/docs/wiki/assets/screenshots/spell-codex.png)

| Mod | Integration |
|---|---|
| Iron's Spellbooks | Unified magic tree (246 nodes, 9 schools), mana bridge, dungeon mage mobs |
| Tensura Reincarnated | Race stat modifiers, soul-level sync, skill perk gates |
| Pufferfish's Skills | Mirror UI for the perk tree |
| Epic Fight | Skill gating & scaling (experimental) |
| Overgeared | Forging stat gates, universal forge recipes |
| Waystones / Lootr / L2 Hostility / PlayerRevive / FTB Teams | Dungeon checkpoints, per-player loot, difficulty scaling, bleed-out revive, team conquest |

**The mod works fully standalone** — integrations light up automatically when the companion mods are present.

![Trial Dungeon treasure floor](https://raw.githubusercontent.com/eladiouf/MOD_MINECRAFT_STAT/neoforge-1.21.1/docs/wiki/assets/screenshots/treasure-floor.png)

## 📋 Requirements
- Minecraft **1.21.1**
- NeoForge **21.1+**
- Java 21

## 🌍 Languages
English & Français

---
*Report issues on the [GitHub repository](https://github.com/eladiouf/MOD_MINECRAFT_STAT).*
