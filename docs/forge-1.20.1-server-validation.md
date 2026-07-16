# Forge 1.20.1 Nightfall server validation

Validated on 2026-07-16 against the dedicated server at:

`C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1`

## Build

- Command: `.\gradlew.bat clean test build`
- Result: `BUILD SUCCESSFUL`
- Tests: 213 executed, 0 failed
- Artifact: `statmod-0.1.0+1.20.1.jar`
- Deployed SHA-256: `E667FB83062CEBEE4DF09DEB0D1ED23861B5D0890E74FD387AEBB5BFA0C3A607`
- The client and dedicated-server copies matched the build artifact byte-for-byte.

## Dedicated-server smoke test

- Forge: `1.20.1-47.4.4`
- Server reached `Done (4.171s)`; ModernFix reported approximately 63 seconds for the complete heavy-modpack load.
- SDMShop and SDMEconomy initialized without a shop-related registry, deserialization, or lifecycle crash.
- MobsPlus remains disabled on the dedicated server at `disabled-mods/MobsPlus-Forge-EFM-20.14.11-1.1.5.jar` because its mandatory client-class mixin is not server-safe.

## Generated catalog

- Shop ID: `default`
- Enabled spells: 151
- Scroll purchase entries: 959
- Resource sale entries: 31
- Skipped malformed spells: 0
- Levels represented: 1 through 10
- Schools represented: 11
- Rarities represented: Common, Uncommon, Rare, Epic, Legendary
- Addon namespaces represented: `darkdoppelganger`, `darkermagic`, `gtbcs_geomancy_plus`, `irons_spellbooks`, `spells_gone_wrong`, `wind_spellbooks`, and `wizardshelp`
- Shop data SHA-256: `9B07262C09D4FDB89068734E0E82790D9B9C6CBBB276ABD4206F7435026D1643`
- Generation report SHA-256: `81265517D981FAEF5F988F28A962A259E1A7629FDA07191571BD144028CB2937`

Two consecutive starts with the same JAR and mod set produced the same shop and report hashes. The second start also created a new rollback snapshot under `config/statmod/shop-backups/20260716-201548`.

## SDMShop settings

- `default_shop_id: "default"`
- `disable_key_bind: false`
- `send_notify: true`
- `show_admin_messages: false`

## Rollback

- Pre-deployment client backup: `statmod-backups/shop-20260716-195548`
- Pre-deployment server backup: `statmod-backups/shop-20260716-195548`
- Catalog snapshots: `config/statmod/shop-backups/<UTC timestamp>`
- Report: `config/statmod/shop-generation-report.json`

The server EULA, offline-mode setting, OP list, worlds, and unrelated pack configuration were not changed by the shop implementation.
