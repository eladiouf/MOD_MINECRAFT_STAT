# Adventurer Healer Regeneration Balance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce rival adventurer healer regeneration to Regeneration I for 2 seconds on normal heals and 4 seconds on Sanctuary.

**Architecture:** Keep the existing `HealPartyGoal` behavior and change only the two regeneration effect declarations. Protect the exact durations and amplifier with a focused source contract test, matching the repository's existing Forge wiring-contract test style.

**Tech Stack:** Java 17, Minecraft Forge 1.20.1, JUnit 5, Gradle 8.8

## Global Constraints

- A normal direct heal grants Regeneration I for 2 seconds (40 ticks).
- Sanctuary grants Regeneration I for 4 seconds (80 ticks).
- Both effects use amplifier `0`.
- Direct healing, cooldowns, thresholds and unrelated buffs remain unchanged.
- The built JAR must be identical on the development build, client and dedicated server.

---

### Task 1: Lock and implement healer regeneration values

**Files:**
- Create: `src/test/java/tong/statmod/dungeon/party/goal/HealPartyGoalRegenerationContractTest.java`
- Modify: `src/main/java/tong/statmod/dungeon/party/goal/HealPartyGoal.java:131,229`

**Interfaces:**
- Consumes: the two `MobEffectInstance` constructions for `MobEffects.REGENERATION` in `HealPartyGoal`.
- Produces: exact source-level contracts for normal-heal and Sanctuary regeneration values.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.dungeon.party.goal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealPartyGoalRegenerationContractTest {
    @Test
    void normalHealAndSanctuaryUseReducedRegenerationOneDurations() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/party/goal/HealPartyGoal.java"));

        assertTrue(source.contains(
                "MobEffects.REGENERATION, 40, 0, false, true"));
        assertTrue(source.contains(
                "MobEffects.REGENERATION, 80, 0, false, true"));
        assertEquals(2, source.split("MobEffects.REGENERATION", -1).length - 1);
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.dungeon.party.goal.HealPartyGoalRegenerationContractTest --console=plain
```

Expected: FAIL because the source still contains durations/amplifiers `80, 1` and `120, 1`.

- [ ] **Step 3: Apply the minimal production change**

In the normal direct heal:

```java
target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true));
```

In Sanctuary:

```java
ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));
```

- [ ] **Step 4: Run focused and complete verification**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.dungeon.party.goal.HealPartyGoalRegenerationContractTest --console=plain
.\gradlew.bat clean test build --console=plain
```

Expected: focused test PASS, complete build SUCCESS, zero test failures.

- [ ] **Step 5: Commit the tested behavior**

```powershell
git add src/test/java/tong/statmod/dungeon/party/goal/HealPartyGoalRegenerationContractTest.java src/main/java/tong/statmod/dungeon/party/goal/HealPartyGoal.java docs/superpowers/plans/2026-07-19-adventurer-healer-regeneration-balance.md
git commit -m "balance: reduce adventurer healer regeneration"
```

### Task 2: Deploy and publish the validated artifact

**Files:**
- Modify: `docs/forge-1.20.1-server-validation.md`
- Deploy: `C:\Users\El Hadji\AppData\Roaming\.minecraft\mods\statmod-0.1.0-1.20.1.jar`
- Deploy: `C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1\mods\statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes: `build/libs/statmod-0.1.0+1.20.1.jar` produced by Task 1.
- Produces: matching client/server deployments, validation record and pushed `forge-1.20.1` branch.

- [ ] **Step 1: Back up and copy the artifact**

```powershell
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$build = 'build\libs\statmod-0.1.0+1.20.1.jar'
$client = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\mods\statmod-0.1.0-1.20.1.jar'
$server = 'C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1\mods\statmod-0.1.0+1.20.1.jar'
$clientBackup = "C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\healer-regen-$stamp"
$serverBackup = "C:\Users\El Hadji\Downloads\serveur\The Casket of Reveries Server 2.2.9.1\statmod-backups\healer-regen-$stamp"
New-Item -ItemType Directory -Force $clientBackup, $serverBackup | Out-Null
Copy-Item -LiteralPath $client -Destination $clientBackup
Copy-Item -LiteralPath $server -Destination $serverBackup
Copy-Item -LiteralPath $build -Destination $client -Force
Copy-Item -LiteralPath $build -Destination $server -Force
```

- [ ] **Step 2: Verify artifact identity**

```powershell
$hashes = Get-FileHash -Algorithm SHA256 -LiteralPath $build, $client, $server
$hashes
if (($hashes.Hash | Select-Object -Unique).Count -ne 1) { throw 'Deployed JAR hashes differ' }
```

Expected: three identical SHA-256 values.

- [ ] **Step 3: Record validation**

Append a dated `Adventurer healer regeneration balance — 2026-07-19` section to `docs/forge-1.20.1-server-validation.md`. Record Regeneration I for 40 ticks on normal heals, Regeneration I for 80 ticks on Sanctuary, unchanged direct healing and unrelated support behavior, the exact aggregated XML test totals, the exact `Get-Item.Length` artifact size, the identical `Get-FileHash` SHA-256 value, and both deployment paths.

- [ ] **Step 4: Commit and push**

```powershell
git add docs/forge-1.20.1-server-validation.md docs/superpowers/plans/2026-07-19-adventurer-healer-regeneration-balance.md
git commit -m "docs: validate healer regeneration balance"
git push origin forge-1.20.1
```

- [ ] **Step 5: Verify publication**

Fetch `origin/forge-1.20.1`, assert local `HEAD` equals the remote ref, and confirm no tracked changes remain.
