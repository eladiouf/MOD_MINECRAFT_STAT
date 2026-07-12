---
type: project
status: Executing
stage: Production
owner: Founder Desk
repository: https://github.com/eladiouf/MOD_MINECRAFT_STAT
genre: RPG progression and mod integration
platform: NeoForge 1.21.1
related_projects: []
tags:
  - studio-project
  - statmod
---

# STAT Mod

## Thesis

Build `STAT Mod` as the progression authority for a multi-mod RPG stack on NeoForge 1.21.1.

## Current Objective

**Public beta live (2026-07-12)** — v1.2.0-beta published on Modrinth (`statmod`, in moderation) and CurseForge (`stat-mod-rpg` #1583282, file approval pending). Current focus: dungeon balancing from playtests, store-page polish (galleries, CurseForge relations), then beta → release decision.

## Shipped Surface (as of 2026-07-12)

- 23 stats, 84 perks, XP combat/défense/non-combat, GUI complet (miroir Puffish)
- Arbre magique unifié Iron's Spellbooks : 249 nœuds, 9 écoles (Phase 1, tuning Fire)
- **Trial Dungeon** : dimension procédurale infinie — 100 thèmes/10 arcs, étages en chaîne de pièces, arènes de boss à ciel ouvert (roster 40+), Dungeon Rush (combos/jackpots/records), économie de points → monnaie, salles secrètes, sanctuaires reliques, co-op FTB Teams
- **Mages du donjon** : IA de caster calquée sur le WizardAttackGoal d'Iron's (kite, télégraphe, cohésion d'escouade, interruption de cast) — voir [[2026-07-03 - STAT Mod M6 Trial Dungeon Mission]]
- **Économie FDP** : pièces/billets physiques, banquier magique, pont boutique SDM avec catalogue détaillé

## Active Constraints

- mission `M5` (Overgeared Universal Forge) toujours en cours — voir [[2026-06-30 - STAT Mod M5 Overgeared Universal Forge Mission]]
- équilibrage donjon piloté par playtests (feedback loop actif avec l'opérateur)
- le repo embarque logs/runs (~500 Mo) — coût de clone/push, candidat à un nettoyage gouverné

## Interfaces

- Adapter package: `projects/statmod/`
- Local bridge: `.onivo-project/`
- Repo: `https://github.com/eladiouf/MOD_MINECRAFT_STAT`
- Distribution: [[2026-07-12 - STAT Mod v1.2.0 Beta Public Release]]

## Risks

- modérations Modrinth/CurseForge en attente (post-release checks ouverts)
- intégrations multi-mods = surface de régression large (10+ mods pontés)
- dette de test sur le cœur donjon lié à ServerLevel (pas d'infra GameTest)
