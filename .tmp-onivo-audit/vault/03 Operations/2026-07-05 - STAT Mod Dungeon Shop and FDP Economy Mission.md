---
type: mission
status: Executing
mission_id: ECON-1
date: 2026-07-05
area: economy
owners:
  - Executive Orchestrator
  - Product Cell
related_projects:
  - STAT Mod
tags:
  - mission
  - statmod
  - economy
  - sdm-shop
---

# 2026-07-05 — STAT Mod Économie FDP & Boutique SDM (mission consolidée)

## Objectif

Fermer la boucle économique du serveur : points de donjon → monnaie `FDP_cfa` → boutique.

## Livré (au 2026-07-12)

- **Monnaie physique FDP** : pièces (50/100/200/500) et billets (1000/2000/5000/10000), items + textures + `FdpCashMath`/`FdpDenomination` testés
- **Banquier magique** : `MagicBankService`, `MagicBanker` (villageois), `VillageBankerSpawner`, écran `MagicBankScreen`, payloads réseau dédiés
- **Pont SDM Shop** : `SDMEconomyBridge` (monnaie enregistrée), `SDMShopNPCBridge`, `SDMShopDatabaseInitializer`, mixins `ShopPage`/`ShopPageModern`, forceur d'onglets client
- **Catalogue détaillé** : migration + population du catalogue SDM (branche `feature/sdm-shop-expansion`, développée en worktree isolé, fusionnée fast-forward le 2026-07-12 après cycle de tests — invariants `SDMShopCatalogTest`)
- **Conversion** : `/dungeon convert [amount]`, échangeur du donjon, garde anti-éjection (1 point conservé)

## Notes de gouvernance

- Le contenu du shop (prix serveur) reste une config serveur — hors scope code
- Décisions repo associées : « physical FDP currency and magic banker » (c21c394), « Bountiful FDP bank » (78a969b)

## Liens

- [[STAT Mod]] · [[2026-07-03 - STAT Mod M6 Trial Dungeon Mission]]
