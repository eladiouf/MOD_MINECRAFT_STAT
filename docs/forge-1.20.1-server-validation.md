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

## Cataclysm-free dungeon AI baseline — 2026-07-18

- Validated source commit: `add0d4c`
- Forbidden-identifier scan: `rg -n -i "cataclysm:" src/main/java/tong/statmod/config src/main/java/tong/statmod/dungeon src/main/resources/data/statmod`
- Scan result: zero matches (ripgrep exit code 1).
- Contract tests: dungeon themes 1–100, boss selection through floor 1000, active-source exclusion, and deterministic adventurer role selection all passed.
- Full verification: `.\gradlew.bat clean test build --console=plain`
- Result: `BUILD SUCCESSFUL in 30s`; 14 actionable tasks executed.
- Artifact: `build/libs/statmod-0.1.0+1.20.1.jar`
- Artifact size: 892755 bytes
- Artifact SHA-256: `EAD7C8139B2D451DA4EE8A558CEED3E92FF66E587806B6B99DCB6E1C87A884F7`

Cataclysm remains available to the modpack for third-party dependency compatibility, but no active STAT Mod dungeon roster, theme, scripted encounter, boss tag, mob pool, or server default selects a `cataclysm:*` entity. Replacements use tactical `statmod:adventurer` squads, Iron's Spells casters, and compatible non-Cataclysm mobs.

## Tactical Iron's Spells AI build validation — 2026-07-18

- Validated source commit: `c252955`
- Iron's Spells API: `1.20.1-3.16.2`
- Full verification: `.\gradlew.bat clean test build --console=plain`
- Result: `BUILD SUCCESSFUL in 32s`; 14 actionable tasks executed.
- Tests: 256 executed, 0 failed, 0 errors.
- Forbidden-identifier scan: `rg -n -i "cataclysm:" src/main/java/tong/statmod/config src/main/java/tong/statmod/dungeon src/main/resources/data/statmod`
- Scan result: zero matches.
- Artifact: `build/libs/statmod-0.1.0+1.20.1.jar`
- Artifact size: 937335 bytes
- Artifact SHA-256: `87B3721D9AA55F5A86D1FBD57F71E3E293A533A347E80AD484AA0D793128DFA3`

Dungeon casters now select one of seven tactical intentions before resolving a real enabled spell from the Iron's Spells registry: direct damage, area damage, control, defense, mobility, ally support, or summon. Spell level is clamped to the spell's configured minimum and maximum and scales with floors 1–100. Missing or disabled registry entries are skipped without linking addon classes.

The new caster goal is limited to the actor's exact `statmod_ai_squad` identifier. Ordinary room squads keep the `floor:room:roomIndex` form, so healing, critical-ally checks, summon counts, telegraphs, and spell decisions never cross chamber boundaries. Area and summon actions use a 20-tick warning, intent categories have independent cooldowns, and living summons are capped at two per squad. Existing rival-party mage/healer goals are not duplicated; rival mages use the same tactical intent policy through the backwards-compatible Iron's Spells bridge.

This section records build validation only. Dedicated-server deployment and runtime smoke testing remain part of the final AI deployment phase.

## Living dungeon AI build validation — 2026-07-18

- Validated source commit: `771aba3`
- Full verification: `.\gradlew.bat clean test build --console=plain`
- Result: `BUILD SUCCESSFUL in 32s`; 14 actionable tasks executed.
- Tests: 269 executed, 0 failed, 0 errors.
- Active Cataclysm scan: zero matches.
- Artifact: `build/libs/statmod-0.1.0+1.20.1.jar`
- Artifact size: 959148 bytes
- Artifact SHA-256: `74F51CB0A4A123B9BFFEB426FB07FE04BF4DE535A7BF2C53F841AE0682E3F7C9`

Combat safehouses now contain a deterministic population of one to four non-combat inhabitants: wounded survivor, merchant, scavenger and prisoner. Their persistent non-combat tag excludes them from initial-wave checks, active-room completion and retry cleanup. `INHABITANTS` never receive player aggro from the encounter director. A prisoner is released only by explicit player interaction, stores that player's UUID and follows only that rescuer after chunk reload.

Deep combat rooms gain three bounded hostile living roles without increasing wave size. The rival explorer replaces slot 0 in room 4 from floor 31, the ritualist replaces slot 0 in room 12 from floor 51, and the engineer replaces slot 0 in room 15 from floor 71. Each uses the existing deferred room queue, authorization, scaling and exact `floor:room:roomIndex` squad. The slot-0 invariant prevents mage escort insertion from consuming the specialist slot.

Living goals are idempotently recovered by the occupied-floor director. Scavengers approach dropped items without deleting them; engineers repair only damaged `DUNGEON_CONSTRUCTS` actors in the exact same squad on a 100-tick budget. This remains build validation; dedicated-server deployment and runtime smoke testing follow in the final deployment phase.

## Final dungeon AI deployment — 2026-07-18

- Deployed source commit: `e92d98e` (the latest preceding code commit is `6d74d00`; `e92d98e` adds only this phase's deployment plan).
- Release gate: `.\gradlew.bat clean test build --console=plain` completed successfully in 32 seconds with 14 tasks executed.
- Tests: 269 executed, 0 failed, 0 errors.
- Active dungeon Cataclysm scan: zero matches.
- JAR contents: `META-INF/mods.toml`, `statmod.refmap.json`, tactical Iron caster classes and living-dungeon actor classes are present.
- Artifact size: 959148 bytes.
- Build/client/server SHA-256: `40F5C7BD9C77798AF0776FDBDBB4417702113842DC55126380FF8F998A8B91A9`.
- Exactly one `statmod-*.jar` is installed in each target.

The active launcher profile `ModpackDonjon` uses `C:\Users\El Hadji\AppData\Roaming\.minecraft`, so the client deployment target is its global `mods` directory. The dedicated-server target is `C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1\mods`. Epic Fight, Puffish Attributes, Iron's Spells, FTB Teams, Lootr, SDMShop and SDMEconomy were present on both sides before replacement.

Rollback copies are stored at:

- Client: `C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\dungeon-ai-20260718-134858`.
- Server: `C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1\statmod-backups\dungeon-ai-20260718-134858`.

The first runtime gate exposed a third-party dedicated-server packaging fault in `MobsPlus-Forge-EFM-20.14.11-1.1.5.jar`: `AbstractClientPlayerPatchMixin` was declared in its required common mixin list even though it casts `net.minecraft.client.player.AbstractClientPlayer`. The original JAR is preserved at `server-compat-backups/mobsplus-20260718-135249`. The server copy was minimally corrected by moving that entry to the mixin configuration's `client` list; the patched JAR SHA-256 is `C298A465F62FE74430ABFE426C6FF4E55928F1BD1DB535D1B0B5A9C28D24C6A4`. The client JAR was not changed.

The final Java 17 dedicated-server smoke test succeeded:

- Full launcher-to-readiness observation: 113.5 seconds; Minecraft reported `Done (28.435s)` after server construction.
- Stat Mod logged `Initializing STAT Mod for Forge 1.20.1`.
- Runtime catalog logged 151 spells, 959 scroll entries, 31 sale entries and 0 skipped entries.
- The redirected `stop` command produced `Stopping server`, `Saving players`, `Saving worlds` and `All dimensions are saved`.
- Process exit code: 0; no Forge server process remained.
- Fatal-marker count: 0 for missing mandatory dependencies, mod-loading exceptions, mixin apply failures, `NoClassDefFoundError`, server tick-loop exceptions and failed server startup.
- Captured stdout: `statmod-dungeon-ai-validation-20260718-135830.stdout.log`.
- Captured stderr: `statmod-dungeon-ai-validation-20260718-135830.stderr.log` (only the standard JVM class-data-sharing warning).

Cataclysm and its unrelated modpack configuration remain installed. EULA, authentication mode, operators, whitelist, shop data and unrelated configuration were not modified by this deployment.

## Hostile dungeon health reduction — 2026-07-19

- Validated code commit: `5da884f`.
- Final maximum-health multiplier: `0.5` through the stable `statmod:dungeon_enemy_health_balance` attribute modifier.
- Modifier persistence: permanent and idempotent across chunk reloads; reapplication preserves the current health percentage.
- Exclusions: players, every actor outside `statmod:trial_dungeon`, and every actor marked `statmod_living_non_combat`.
- Covered hostile paths: general room scaling, rival/debug adventurer parties, boss altars, trap ambushes, secret-room bosses, Ultra Vault bosses and occupied-floor recovery.
- Unchanged systems: damage, armor, toughness, spells, tactical AI, encounter size and neutral inhabitants.

The final release gate used `.\gradlew.bat clean test build --console=plain` and completed successfully in 33 seconds with all 14 tasks executed. The suite contains 274 tests with 0 failures and 0 errors. The release artifact is 961583 bytes with SHA-256 `E550ECF5957ACF57591E5DF3B02165B0532B43C1C5852E36ED8236CA07941897`.

Deployment evidence:

- Client target: `C:\Users\El Hadji\AppData\Roaming\.minecraft\mods\statmod-0.1.0-1.20.1.jar`.
- Server target: `C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1\mods\statmod-0.1.0+1.20.1.jar`.
- Build, client and server hashes are identical, and each target contains exactly one Stat Mod JAR.
- Pre-change rollback copies: client and server `statmod-backups/health-halving-20260719-032407`.
- Final-deployment rollback copies: client and server `statmod-backups/health-halving-final-20260719-033349`.

The final Java 17 dedicated-server smoke test reached readiness in 93.1 seconds; Minecraft reported `Done (9.078s)`. Stat Mod logged its Forge 1.20.1 initialization, no fatal dependency/mixin/class-loading/server-tick marker appeared, and redirected `stop` produced `Stopping server`, `Saving players`, `Saving worlds` and `All dimensions are saved`. The process exited with code 0 and no Forge server process remained. Captured logs are `statmod-health-halving-final-20260719-033408.stdout.log` and `statmod-health-halving-final-20260719-033408.stderr.log` in the server root.
