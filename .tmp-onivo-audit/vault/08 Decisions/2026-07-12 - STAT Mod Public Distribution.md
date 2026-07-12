---
type: decision
status: Accepted
decision_id: STAT-DEC-PUBLIC-DISTRIBUTION
date: 2026-07-12
area: publishing
owners:
  - Executive Orchestrator
  - Publishing Cell
  - Founder Desk
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
  - release
  - modrinth
  - curseforge
---

# 2026-07-12 — STAT Mod Distribution Publique (Modrinth + CurseForge)

## Context

Roadmap point 8 (« Release publique gouvernée ») était ⚪. Le scope v1.2.0 est clos (donjon M6, économie FDP, boutique SDM, IA des mages validée en jeu), la suite de tests est verte, et l'audit de dépendances confirme que le mod est **standalone** (toutes intégrations soft-gated).

## Decision

Publication **v1.2.0-beta** sur les deux surfaces standard :

| Surface | Identifiants | Méthode |
|---|---|---|
| Modrinth | `statmod` / `s6kZZpsn` | Projet créé via API v2, version via Minotaur (`gradlew modrinth`) |
| CurseForge | `stat-mod-rpg` / #1583282 | Projet créé manuellement (pas d'API), fichier via CurseForgeGradle (`gradlew publishCurseForge`) |

Canal **beta** tant que l'équilibrage donjon est actif. Conformité au `playbook-release-flow` consignée dans le decision record du repo : `docs/superpowers/specs/2026-07-12-public-distribution-release-record.md`.

## Consequences

- Pipeline de release reproductible en une commande ; tokens hors repo (profil utilisateur)
- Incident consigné : un premier token Modrinth exposé en clair → révoqué immédiatement (politique « secret exposé = brûlé »)
- Dépendances Modrinth déclarées optionnelles (8 slugs vérifiés) ; Tensura/FTB Teams/Overgeared/SLU absents de Modrinth → documentés dans la description
- Post-release checks ouverts : modération Modrinth + approbation fichier CurseForge

## Follow-up

- Galeries des deux pages (captures opérateur)
- Relations CurseForge via le site
- Décision future : passage beta → release

Voir [[2026-07-12 - STAT Mod v1.2.0 Beta Public Release]]
