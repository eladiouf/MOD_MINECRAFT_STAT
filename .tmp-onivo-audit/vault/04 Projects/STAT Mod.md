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

Stabilize the active NeoForge branch and continue integrating combat and magic runtimes under one STAT Mod identity.

## Active Constraints

- public README and release-facing identity are behind the current runtime branch
- the working tree is actively evolving with many local changes
- multi-mod integration work increases regression risk

## Interfaces

- Adapter package: `projects/statmod/`
- Local bridge: `.onivo-project/`
- Repo: `https://github.com/eladiouf/MOD_MINECRAFT_STAT`

## Risks

- runtime/documentation drift
- integration complexity across Epic Fight, Puffish, Tensura, and Iron's Spellbooks
