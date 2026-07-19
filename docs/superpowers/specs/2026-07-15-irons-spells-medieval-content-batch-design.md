# Iron's Spells Medieval Content Batch — Design

**Date:** 2026-07-15  
**Target client:** `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-1.20.1`  
**Loader:** Forge 47.4.10  
**Minecraft:** 1.20.1

## Goal

Add one coherent medieval-magic content batch to the currently launchable client without changing STAT Mod, Epic Fight, Project Babylon, or the existing Iron's Spells core. The batch adds spells, magical equipment, familiars, constructs, geomancy, ritual visuals, Cataclysm integration, and Deeper and Darker integration.

## Approved batch

Install these seven addons from the prepared Forge 1.20.1 inventory:

1. `alshanex_familiars-1.20.1_v1.1.2_HotFix.jar`
2. `arcanists_equipage-1.0.2-20.1.jar`
3. `constructs_casting-2.2.5.jar`
4. `gtbcs_geomancy_plus-2.0.0-1.20.1.jar`
5. `magiccircles-1.2.2-1.20.1.jar`
6. `cataclysm_spellbooks-1.2.9-1.20.1-all.jar`
7. `darkermagic-1.3.1-1.20.1-ver.b.jar`

The first five have no missing mandatory dependency in the current client. Cataclysm Spellbooks additionally requires AzureLib `3.0+`; Darker Magic additionally requires Deeper and Darker `1.3.3+`.

## Dependency policy

- Download only official Forge 1.20.1 releases of AzureLib and Deeper and Darker.
- Verify downloaded hashes when the distribution API supplies them.
- Read each downloaded JAR's `META-INF/mods.toml` and require the expected mod ID, Minecraft version, loader, and minimum version before installation.
- Recompute mandatory dependency closure against the complete active client plus the proposed batch.
- Do not install the batch if any dependency is absent, targets another loader, or has an unresolved mod-ID conflict.
- Do not add unrelated optional dependencies or other Iron's Spells addons during this batch.

## Installation transaction

The seven prepared addons and two verified dependencies form one transaction. Before installation:

1. confirm no Java process is running the `test-1.20.1` client;
2. confirm every destination filename is absent;
3. hash every source JAR;
4. write the planned file list and hashes to a timestamped manifest under `disabled-mods`.

Copy all nine JARs into `mods`, verify destination hashes, then mark the manifest complete. If any copy or verification fails, remove only the files introduced by this transaction and mark the manifest rolled back. Existing active mods must remain untouched.

## Validation

Static acceptance requires:

- all nine expected JARs are active;
- no mandatory dependency is missing;
- exactly one active provider exists for each newly introduced primary mod ID;
- STAT Mod, Epic Fight, Iron's Spells, and Project Babylon remain present.

Runtime acceptance proceeds in order:

1. reach the main menu without a new crash report or fatal mod-loading error;
2. open or create a world without registry, datapack, recipe, or mixin failure;
3. verify an existing Iron's Spells cast still works;
4. exercise at least one feature from Cataclysm Spellbooks, Darker Magic, Geomancy Plus, and one of the remaining content addons.

If startup or world loading fails, use the newest crash/log evidence to identify the failing addon. Remove the whole batch only if the failure cannot be isolated safely; otherwise quarantine the single incompatible addon and any addon that requires it.

## Deferred addons

All other prepared Iron's Spells addons remain unchanged in `active` or `needs-testing`. In particular, no Arknights, PMMO, Create, origins, voice-casting, EMC, leveling, camera, food, or quick-cast integration enters this content batch.
