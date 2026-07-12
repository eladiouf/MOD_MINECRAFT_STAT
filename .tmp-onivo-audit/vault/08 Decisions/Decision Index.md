# Decision Index

This page indexes important studio and project decisions.

## Catalogue (manual fallback, also available via Dataview)

| Id | Date | Area | Subject |
|---|---|---|---|
| `STUDIO-DEC-001` | 2026-06-21 | governance | Studio autonomy expansion (Founder Amendment) |
| `STAT-DEC-001` | 2026-06-21 | platform-identity | NeoForge 1.21.1 runtime truth |
| `STAT-DEC-002` | 2026-06-21 | integration-policy | Epic Fight reclassified as experimental optional |
| `STAT-DEC-003` | 2026-06-21 | intelligence-process | Mahou + Elementals removal postmortem opened |
| `STAT-DEC-004` | 2026-06-21 | studio-operations | STAT Mod onboarding into Onivo Studio complete |
| `STAT-DEC-005` | 2026-06-21 | documentation-taxonomy | `docs/superpowers/plans/` canonical, `.opencode/plans/` deprecated |
| `STAT-DEC-006` | 2026-06-21 | delivery-sequencing | Mission prioritization sequence locked |
| `STAT-DEC-007` | 2026-06-22 | quality | Test vs fix priority for XP handlers |
| `STAT-DEC-008` | 2026-06-22 | config | Wire 4 dead config values to runtime |
| `STAT-DEC-009` | 2026-06-22 | perks | Fix True Strike + 5 tooltip mismatches |
| `STAT-DEC-010` | 2026-06-22 | progression | Global level excludes locked magic stats |
| `STAT-DEC-011` | 2026-06-22 | progression | Non-combat XP for 9 magic stats |
| `STAT-DEC-012` | 2026-06-22 | progression | Fix kodachi misclassification |
| `STAT-DEC-013` | 2026-06-22 | cleanup | Delete dead code (ProgressionRouting, ActionType, Config.register) |
| `STAT-DEC-014` | 2026-06-22 | performance | Fix memory leaks in static maps |
| `STAT-DEC-M4-REVERSE` | 2026-06-21 | magic-bridge | Reverse bridge Tensura → Iron's Spellbooks (`decisions/`) |
| `STAT-DEC-M4-B` | 2026-06-21 | magic-ux | Virtual Inscription : keybind plutôt qu'item craftable (`decisions/`) |
| `STAT-DEC-M4-CONFORMANCE` | 2026-06-22 | magic-bridge | Conformance Tensura du reverse bridge (`decisions/`) |
| `STAT-DEC-MAGICULE-MANA-RATIO` | 2026-06-22 | magic-economy | Conversion magicule Tensura → mana Iron's (`decisions/`) |
| `STAT-DEC-MASTERY-MAPPING` | 2026-06-22 | magic-economy | Mastery Tensura → spell level Iron's (`decisions/`) |
| `STAT-DEC-CATALOG-AUDIT` | 2026-06-23 | magic-catalog | Audit Mission O des classifications SpellRole (`decisions/`) |
| `STAT-DEC-MAGE-CODEX` | 2026-06-23 | magic-ux | Écran Codex du Mage (`decisions/`) |
| `STAT-DEC-START-BRANCH-CHOOSER` | 2026-06-23 | magic-ux | UI de chooser de branche de départ (`decisions/`) |
| `STAT-DEC-XP-AUDIT` | 2026-06-23 | progression | Audit complet des sources XP par stat (`decisions/`) |
| `STAT-DEC-MAGIC-UNIFIED-ECONOMY` | 2026-06 | magic-economy | Monnaie unique `magicPoints` + 3 gates de stats par nœud (`decisions/`) |
| `STAT-DEC-PHASE-GATING` | 2026-06 | magic-scope | 9 écoles structurellement actives, tuning Fire d'abord (`decisions/`) |
| `STAT-DEC-COMMONNETWORK-SHIM-REMOVAL` | 2026-06-30 | runtime-hotfix | Suppression des shims commonnetwork (`decisions/`) |
| `STAT-DEC-OVERGEARED-EXPANSION` | 2026-06-30 | integration-overgeared | Mission M5 Universal Forge greenlight (`decisions/`) |
| `STAT-DEC-TRIAL-DUNGEON` | 2026-07-03 | product-scope | Mission M6 Trial Dungeon greenlight — [[2026-07-03 - STAT Mod Trial Dungeon Greenlight]] |
| `STAT-DEC-PUBLIC-DISTRIBUTION` | 2026-07-12 | publishing | Release publique bêta Modrinth + CurseForge — [[2026-07-12 - STAT Mod Public Distribution]] |

> Les entrées marquées `decisions/` pointent vers les records canoniques du dossier `decisions/` à la racine du repo studio ; les entrées avec wikilink ont une note vault complète.

## Dataview (auto)

```dataview
TABLE date, area, status, related_projects
FROM "vault/08 Decisions"
WHERE type = "decision" AND file.name != "Decision Index"
SORT date DESC
```

## Meetings

| Id | Date | Type |
|---|---|---|
| `STAT-MTG-001` | 2026-06-21 | Plenary — Milestone Review + Exception Council |
| `STAT-MTG-002` | 2026-06-22 | Technical Audit Plenary — Stat Leveling System |
