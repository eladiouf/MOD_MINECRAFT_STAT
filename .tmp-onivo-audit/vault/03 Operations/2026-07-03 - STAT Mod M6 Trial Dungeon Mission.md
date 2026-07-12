---
type: mission
status: Executing
mission_id: M6
date: 2026-07-03
area: dungeon
owners:
  - Executive Orchestrator
  - Product Cell
related_projects:
  - STAT Mod
tags:
  - mission
  - statmod
  - dungeon
---

# 2026-07-03 — STAT Mod M6 Trial Dungeon (mission consolidée)

## Objectif

Livrer le Trial Dungeon : dimension `statmod:trial_dungeon`, donjon procédural infini, cœur de la boucle de jeu premium du serveur RPG.

## Timeline des jalons (2026-07-03 → 2026-07-12)

| Date | Jalon |
|---|---|
| 07-03 | Création : grille XZ, portail, autels de boss, roster SLU, HUD, loot shards. Island Redesign (silhouettes organiques déterministes) |
| 07-04 | Refonte de fiabilité : ModdedMobPool corrigé (modId `slu` réel), L2 leveling unifié via chokepoint spawn guard, pipeline de génération simplifié. « Vraie aventure » : conquête obligatoire par étage (CLEAR_WAVE / LOOT_VAULT / SLAY_BOSS), fix invasion de mobs (liste blanche stricte) |
| 07-05 | Layout serpentin : chaîne de pièces boustrophedon, thèmes ×100 (10 arcs), palettes par thème |
| 07-09 | Audit softlock boss (fix critique : mort du joueur ne détruit plus le combat de boss). « Dungeon Rush » : combos, jackpots, sans-faute, records. Îles agrandies, co-op réparé + équipes FTB. Feedback playtest : portes Macaw's, pièges command block, mobs disciplinés, salles secrètes, chambres-fortes ultra-secrètes |
| 07-11/12 | **Mages du donjon** : fix œufs custom (NBT NeoForgeData + autorisation spawn guard), fix perte d'IA au rechargement (goalSelector jamais sérialisé → garde WeakHashMap), IDs de sorts corrigés contre le jar, **IA de caster réécrite calquée sur le WizardAttackGoal d'Iron's** (seeTime, riposte, cadence par distance, kite backpedal, télégraphe, cohésion d'escouade, interruption de cast = contre-jeu solo), spawns naturels réparés (étage 3+, escouade tank+archer, 30 % pièces Archives) |

## Verification

- Suites JUnit donjon vertes à chaque jalon (IslandTerrainShaper, DungeonObjective, DungeonBossTracker, DungeonRush, DungeonRecords, DungeonCoop, DungeonUltraVault…)
- Validation en jeu par l'opérateur + logs de session (casts des mages observés le 2026-07-12)

## Dette restante

- Pas de tests directs sur DungeonBossHandler/SpawnGuard/Progress/MobSpawner/RespawnHandler (logique liée ServerLevel, pas d'infra GameTest)
- Backlog approuvé non réalisé : arènes de boss à phases, ambiance sonore par arc, prégénération d'étage

## Liens

- [[STAT Mod]] · [[2026-07-12 - STAT Mod v1.2.0 Beta Public Release]]
- Decision record repo : `STAT-DEC-TRIAL-DUNGEON` (CLAUDE.md §Trial Dungeon)
