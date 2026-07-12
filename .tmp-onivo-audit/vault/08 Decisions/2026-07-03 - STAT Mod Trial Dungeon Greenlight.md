---
type: decision
status: Accepted
decision_id: STAT-DEC-TRIAL-DUNGEON
date: 2026-07-03
area: product-scope
owners:
  - Executive Orchestrator
  - Product Cell
  - Strategy Council
related_projects:
  - STAT Mod
tags:
  - decision
  - statmod
  - dungeon
---

# 2026-07-03 — STAT Mod Trial Dungeon Greenlight

## Context

Le serveur RPG premium a besoin d'un contenu endgame rejouable qui monétise la progression STAT MOD (points → monnaie → boutique). Les structures vanilla et les donjons de mods tiers ne sont ni contrôlables ni infinis.

## Decision

Greenlight de la mission `M6` : dimension dédiée `statmod:trial_dungeon` — donjon procédural en grille XZ, étages infinis, autels de boss avec roster prédéfini (SLU + Iron's + vanilla), objectifs de conquête par étage, système de points, HUD dédié.

## Consequences

- Le donjon devient l'artère de contenu principale du projet (7 jalons livrés entre le 03 et le 12 juillet — voir [[2026-07-03 - STAT Mod M6 Trial Dungeon Mission]])
- Dépendances runtime nouvelles, toutes optionnelles : Waystones, PlayerRevive, FTB Teams, Lootr, L2 Hostility
- La difficulté est pilotée par playtests opérateur (boucle de feedback courte)

## Follow-up

- Équilibrage continu post-release beta
- Infra GameTest pour couvrir le cœur lié à ServerLevel (dette assumée)
