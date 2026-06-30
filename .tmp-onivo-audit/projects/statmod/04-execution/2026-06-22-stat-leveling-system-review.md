---
type: meeting
status: Draft
meeting_type: review
date: 2026-06-22
owners: [Build Cell]
attendees: [Build Cell, Verification Cell]
related_projects: [STAT MOD]
decisions: []
actions: []
tags: [stats, xp, leveling, progression, quality]
---

# STAT Mod — Revue du Système de Niveaux et Progression

## Goal

Auditer et aligner le système de stats, XP, leveling et perks pour garantir cohérence, maintenabilité et équilibre avant la release.

## Contexte

Le code a évolué via plusieurs itérations (port NeoForge, intégration Tensura, arbre magique Iron's Spellbooks). Le système de progression n'a jamais été revu dans son ensemble. Plusieurs inconsistances ont été identifiées entre le code, la config et la documentation.

## Résumé de l'Audit

### Stats (23, pas 22)

- **23 stats** (indices 0–22) réparties en **6 familles** — la CLAUDE.md dit 22, le code dit 23
- Chaque stat a 6 perks (total 138 perks) avec tiers débloqués à levels 10/25/40/55/75/95
- Effets de combat : formules linéaires en `level * 0.5%` pour la plupart, caps variables

### Formule de Progression

- **XP requise par level :** `(level + 1)² × 10` — courbe quadratique
- **Level cap :** 100 (hardcodé, pas lu depuis Config)
- **XP combat :** `max(1, round(target HP × 1.5))` — ex: zombie = 30 XP, wither = 450 XP
- **XP non-combat :** seulement 6/23 stats ont des sources non-combat (FORGING, COOKING, ALCHEMY surtout)
- **Scale final :** `baseXP × raceMultiplier × (1 + soulLevel/100) × (parallelExistence ? 2 : 1)`

### Global Level & Perk Points

- **Global level = sum(levels) / 23** (division entière)
- **1 perk point par famille** tous les 10 niveaux globaux
- À global level 100 : 10 points/famille = max ~6-8 perks sur 36 possibles
- **Progression très lente** : 230 levels totaux pour le premier milestone (global 10)

### Issues Identifiées

| # | Issue | Gravité |
|---|---|---|
| 1 | Config (`combatXpMultiplier`, `nonCombatXpMultiplier`, `maxStatLevel`, `basePerkPointsPerLevel`) — **définis mais jamais lus** | Haute |
| 2 | `ProgressionRouting.java` — wrapper mort, jamais appelé | Basse |
| 3 | Stats magiques (8-15) — **aucune source XP non-combat** | Haute |
| 4 | CLAUDE.md dit 22 stats, le code en a 23 | Basse |
| 5 | Détection de minerais hardcodée (19 blocs) — pas de support moddé | Moyenne |
| 6 | Pas d'event level-up publiable pour autres systèmes | Moyenne |
| 7 | Global level trop lent — ~50h+ pour premier point de perk | Haute |

## Agenda

1. **Validation des constats** — Parcourir les résultats de l'audit
2. **Décisions sur les issues #1–7** — Prioriser et décider des correctifs
3. **Rééquilibrage** — Ajuster la courbe XP, les sources non-combat, le global level
4. **Wiring Config** — Connecter les valeurs de Config.java au runtime
5. **Prochaines étapes** — Mission dédiée ou intégration dans M4/M5 ?

## Notes

*À remplir pendant la réunion.*

## Décisions

*À documenter après la réunion.*

## Actions

- [ ] Build Cell — Publier le rapport d'audit complet dans `docs/superpowers/specs/`
- [ ] TBD — Issue #1 : Wired Config dans CombatXPHandler et NonCombatXPHandler
- [ ] TBD — Issue #3 : Ajouter sources XP non-combat pour stats magiques
- [ ] TBD — Issue #7 : Réviser formule global level ou fréquence des perk points
- [ ] TBD — Issue #2 : Supprimer ProgressionRouting.java
- [ ] TBD — Issue #4 : Mettre à jour CLAUDE.md (23 stats)
