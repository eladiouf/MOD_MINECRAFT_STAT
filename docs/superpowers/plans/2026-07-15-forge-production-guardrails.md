# Forge Production Guardrails Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Protect the Forge 1.20.1 foundation with explicit save/network versions, reproducible artifact checks, supported-version documentation, and a dedicated-server smoke gate.

**Architecture:** One dependency-free runtime contract owns stable protocol and schema constants. Persistence writes a schema marker while remaining backward-compatible with existing unversioned data. PowerShell and CI verify the built artifact and run Forge's finite GameTest dedicated-server profile.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, ForgeGradle 6, JUnit 5.10.2, Gradle 8.8, PowerShell 7.

## Global Constraints

- Work only on branch `forge-1.20.1` in `.worktrees/forge-1.20.1`.
- Keep mod ID `statmod`, Java group `tong.statmod`, and Forge `47.4.10`.
- The core must load with Forge and Minecraft only.
- Do not add Tensura, NeoForge, Iron's Spells, Epic Fight, or addon API imports.
- Preserve all 74 catalogued external JARs and their recorded SHA-256 values.
- Preserve unversioned player NBT by treating a missing schema marker as schema 0.
- Do not stage or alter the pre-existing dirty modpack-script changes.

---

### Task 1: Stable runtime contract and save schema

**Files:**
- Create: `src/main/java/tong/statmod/StatModRuntime.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/stats/PlayerStats.java`
- Create: `src/test/java/tong/statmod/StatModRuntimeTest.java`
- Modify: `src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java`

**Interfaces:**
- Produces: `StatModRuntime.NETWORK_PROTOCOL : String` equal to `"1"`.
- Produces: `StatModRuntime.PLAYER_STATS_SCHEMA : int` equal to `1`.
- Produces: `PlayerStats.serializedSchema(CompoundTag) : int`, package-private and returning `0` when the key is absent.
- Consumes: the existing `PlayerStats.serializeNbt()` and `deserializeNbt(CompoundTag)` API.

- [ ] **Step 1: Write failing runtime and persistence tests**

```java
@Test
void exposesStableProtocolAndFirstVersionedPlayerSchema() {
    assertEquals("1", StatModRuntime.NETWORK_PROTOCOL);
    assertEquals(1, StatModRuntime.PLAYER_STATS_SCHEMA);
}
```

Add these assertions to `PlayerStatsNbtTest`:

```java
CompoundTag saved = source.serializeNbt();
assertEquals(1, PlayerStats.serializedSchema(saved));

CompoundTag legacy = saved.copy();
legacy.remove("schema");
assertEquals(0, PlayerStats.serializedSchema(legacy));
PlayerStats restored = new PlayerStats();
restored.deserializeNbt(legacy);
assertEquals(source.snapshot(), restored.snapshot());
```

- [ ] **Step 2: Run the focused tests and confirm the missing contract fails**

Run: `./gradlew test --tests tong.statmod.StatModRuntimeTest --tests tong.statmod.stats.PlayerStatsNbtTest --console=plain`

Expected: compilation fails because `StatModRuntime` and `serializedSchema` do not exist.

- [ ] **Step 3: Add the contract and versioned backward-compatible persistence**

```java
package tong.statmod;

public final class StatModRuntime {
    public static final String NETWORK_PROTOCOL = "1";
    public static final int PLAYER_STATS_SCHEMA = 1;

    private StatModRuntime() {
    }
}
```

In `PlayerStats`, write `schema` before `stats` and expose the package-private reader:

```java
private static final String SCHEMA_KEY = "schema";

root.putInt(SCHEMA_KEY, StatModRuntime.PLAYER_STATS_SCHEMA);

static int serializedSchema(CompoundTag root) {
    return root.contains(SCHEMA_KEY, Tag.TAG_INT) ? root.getInt(SCHEMA_KEY) : 0;
}
```

Replace `StatNetwork.PROTOCOL` and its uses with
`StatModRuntime.NETWORK_PROTOCOL`. Do not reject schema 0 during deserialization;
the current keyed representation is already the schema-0 migration path.

- [ ] **Step 4: Run focused tests and the full suite**

Run: `./gradlew test --tests tong.statmod.StatModRuntimeTest --tests tong.statmod.stats.PlayerStatsNbtTest --console=plain`

Expected: focused tests pass.

Run: `./gradlew test --console=plain`

Expected: all tests pass.

- [ ] **Step 5: Commit the runtime contract**

```powershell
git add -- src/main/java/tong/statmod/StatModRuntime.java src/main/java/tong/statmod/network/StatNetwork.java src/main/java/tong/statmod/stats/PlayerStats.java src/test/java/tong/statmod/StatModRuntimeTest.java src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java
git commit -m "feat: version Forge runtime contracts"
```

### Task 2: Supported runtime record

**Files:**
- Create: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Create: `src/test/java/tong/statmod/SupportedRuntimeContractTest.java`

**Interfaces:**
- Produces: the authoritative tested-version record for later integration matrices.
- Consumes: exact versions from `gradle.properties` and the pinned Iron's Spells baseline from the master design.

- [ ] **Step 1: Write a failing documentation contract test**

```java
@Test
void recordsExactPlatformAndHonestOptionalIntegrationStatus() throws IOException {
    String record = Files.readString(Path.of(
            "docs/compatibility/forge-1.20.1-supported-runtime.md"));
    assertTrue(record.contains("Minecraft | 1.20.1 | verified"));
    assertTrue(record.contains("Forge | 47.4.10 | verified"));
    assertTrue(record.contains("Java | 17 | verified"));
    assertTrue(record.contains("Iron's Spells 'n Spellbooks | 3.16.2 | prepared"));
    assertTrue(record.contains("Epic Fight | unpinned | untested"));
    assertTrue(record.contains("Tensura | excluded | unsupported"));
}
```

- [ ] **Step 2: Run the test and confirm the missing document fails**

Run: `./gradlew test --tests tong.statmod.SupportedRuntimeContractTest --console=plain`

Expected: FAIL with `NoSuchFileException`.

- [ ] **Step 3: Write the runtime record**

Create a document with a table headed `Component | Version | Status | Notes`
and the exact rows asserted above. Define `verified` as build plus automated
tests, `prepared` as dependencies catalogued but integration not implemented,
`untested` as no compatibility claim, and `unsupported` as deliberately absent.
Record that client startup, world creation, and dedicated-server gameplay will
be promoted to verified only after their matching smoke gates pass.

- [ ] **Step 4: Run the focused test**

Run: `./gradlew test --tests tong.statmod.SupportedRuntimeContractTest --console=plain`

Expected: test passes.

- [ ] **Step 5: Commit the compatibility record**

```powershell
git add -- docs/compatibility/forge-1.20.1-supported-runtime.md src/test/java/tong/statmod/SupportedRuntimeContractTest.java
git commit -m "docs: record supported Forge runtime"
```

### Task 3: Artifact and forbidden-reference verification

**Files:**
- Modify: `scripts/verify-clean-foundation.ps1`
- Modify: `.github/workflows/build.yml`

**Interfaces:**
- Produces: `verify-clean-foundation.ps1 -Mode After`, the single post-build artifact gate used locally and in CI.
- Consumes: `build/libs/statmod-*.jar`, Git tracking state, and the preserved addon manifest.

- [ ] **Step 1: Extend the verifier with intentional failing probes**

Run the existing verifier before editing:

```powershell
./scripts/verify-clean-foundation.ps1 -Mode After
```

Expected: PASS, establishing the pre-change baseline.

Add checks that the archive contains
`tong/statmod/StatModRuntime.class`, contains no `.class` under known external
namespaces (`io/redspace/ironsspellbooks/`, `yesman/epicfight/`,
`net/neoforged/`), and contains no duplicate archive entry names. Keep the
existing required-entry list.

- [ ] **Step 2: Make CI execute the post-build gate**

Append after the build step:

```yaml
      - name: Verify artifact and preserved addon baseline
        shell: pwsh
        run: ./scripts/verify-clean-foundation.ps1 -Mode After
```

- [ ] **Step 3: Build and run the strengthened verifier**

Run: `./gradlew clean build --console=plain`

Expected: `BUILD SUCCESSFUL`.

Run: `./scripts/verify-clean-foundation.ps1 -Mode After`

Expected: `OK mode=After branch=forge-1.20.1 jars=74 manifest=74`.

- [ ] **Step 4: Commit the production artifact gate**

```powershell
git add -- scripts/verify-clean-foundation.ps1 .github/workflows/build.yml
git commit -m "ci: verify Forge production artifact"
```

### Task 4: Finite dedicated-server smoke profile

**Files:**
- Create: `scripts/smoke-gametest-server.ps1`
- Modify: `.github/workflows/build.yml`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`

**Interfaces:**
- Produces: a finite script that invokes `runGameTestServer`, preserves its log, rejects fatal/crash signatures, and returns the Gradle exit code.
- Consumes: ForgeGradle's existing `gameTestServer {}` run configuration and `run/logs/latest.log`.

- [ ] **Step 1: Create the smoke script with strict log validation**

The script must set `$ErrorActionPreference = 'Stop'`, remove only the verified
workspace-local `run/logs/latest.log`, run
`./gradlew runGameTestServer --console=plain`, require exit code 0, require a
new log, and reject these case-insensitive patterns:

```text
\[.*FATAL.*\]|Exception in server tick loop|Failed to start the minecraft server|ModLoadingException|Crash report saved to
```

On success print `OK dedicated Forge GameTest server smoke`.

- [ ] **Step 2: Run the smoke script locally**

Run: `./scripts/smoke-gametest-server.ps1`

Expected: exit code 0 and `OK dedicated Forge GameTest server smoke`.

- [ ] **Step 3: Add the smoke gate to CI and promote only its tested scope**

Append to `.github/workflows/build.yml`:

```yaml
      - name: Smoke dedicated Forge server
        shell: pwsh
        run: ./scripts/smoke-gametest-server.ps1
```

Update the runtime record to state that the standalone Forge GameTest dedicated
server profile is verified. Do not claim that Iron's Spells, Epic Fight, their
addons, or normal world gameplay are verified by this profile.

- [ ] **Step 4: Run the complete guardrail suite**

Run: `./gradlew clean test build --console=plain`

Expected: `BUILD SUCCESSFUL`.

Run: `./scripts/verify-clean-foundation.ps1 -Mode After`

Expected: the 74-JAR baseline and artifact checks pass.

Run: `./scripts/smoke-gametest-server.ps1`

Expected: the finite dedicated-server smoke passes with no forbidden log line.

- [ ] **Step 5: Commit the server gate**

```powershell
git add -- scripts/smoke-gametest-server.ps1 .github/workflows/build.yml docs/compatibility/forge-1.20.1-supported-runtime.md
git commit -m "test: smoke Forge dedicated server"
```

## Completion audit

- Run `rg -n "TBD|TODO|implement later|fill in|appropriate error handling|Similar to Task" docs/superpowers/plans/2026-07-15-forge-production-guardrails.md`; the only permitted match is this audit command itself.
- Run `git diff --check`.
- Confirm only the plan's files were staged in each commit.
- Confirm the pre-existing modpack-script modifications remain unstaged and unchanged.
- Record any GameTest limitation honestly instead of weakening or bypassing the smoke gate.
