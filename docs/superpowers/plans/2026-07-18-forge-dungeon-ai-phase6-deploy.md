# Dungeon AI Phase 6 — Client and Dedicated Server Deployment Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the verified Forge 1.20.1 AI build to the active ModpackDonjon client and Casket server, prove byte identity, and complete a real dedicated-server start/stop smoke test.

**Architecture:** Replace only Stat Mod through timestamped rollback directories. The Minecraft launcher profile proves the client game directory is the global `.minecraft` root. A redirected Java process uses the server's Forge argument file, watches combined logs for readiness or crash markers, sends `stop` through standard input, and requires clean process exit.

**Tech Stack:** PowerShell, SHA-256, Forge 47.4.4 dedicated server, Java process redirection.

## Global Constraints

- Client target: `C:\Users\El Hadji\AppData\Roaming\.minecraft\mods`.
- Server target: `C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1\mods`.
- Replace only `statmod-*.jar`; preserve every unrelated mod and configuration.
- Keep exactly one Stat Mod JAR in each target.
- Back up both prior JARs before replacement.
- Require Epic Fight 20.14.17+, Puffish Attributes 0.8.2+, Iron's Spells 3.16.2+, FTB Teams 2001.3.2+, Lootr, SDMShop and SDMEconomy on both sides.
- Do not remove Cataclysm; its non-STAT pack configuration remains intact.
- Do not change EULA, offline mode, operators, whitelist, worlds or shop data.

---

### Task 1: Fresh release gate

- [x] Record and version this deployment plan before executing it.
- [x] Run `.\gradlew.bat clean test build --console=plain` and require exit code 0.
- [x] Inspect the built JAR for `mods.toml`, mixin refmap and living/Iron AI classes.
- [x] Scan active STAT dungeon source/resources for `cataclysm:` and require zero matches.
- [x] Record artifact size and SHA-256.

### Task 2: Transactional deployment

- [x] Confirm no Minecraft client or Forge dedicated-server Java process is running.
- [x] Create matching timestamped client/server directories under `statmod-backups/dungeon-ai-<timestamp>`.
- [x] Copy every existing `statmod-*.jar` from each mods directory to its rollback directory.
- [x] Replace the single existing Stat Mod target with `build/libs/statmod-0.1.0+1.20.1.jar` in each mods directory.
- [x] Require exactly one target JAR and SHA-256 equality among build, client and server.

### Task 3: Dedicated-server smoke

- [x] Start Java 17 with the Forge argument file in the server directory and redirected standard streams.
- [x] Wait up to 240 seconds for the server `Done` marker while failing immediately on mod-loading exceptions or crash-report creation.
- [x] Send `stop` through standard input, wait up to 60 seconds and require process exit.
- [x] Verify logs contain Stat Mod initialization and no missing mandatory dependency, mixin apply failure, `NoClassDefFoundError`, or fatal exception attributable to Stat Mod.

### Task 4: Final audit and publication

- [x] Append exact hashes, rollback paths, readiness time and smoke evidence to `docs/forge-1.20.1-server-validation.md`.
- [x] Run a final Git status/diff audit and preserve the user's unrelated untracked files.
- [x] Commit `docs: verify dungeon AI deployment`, push `forge-1.20.1`, and verify local/remote commit equality.
