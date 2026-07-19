# Forge 1.20.1 Iron Creative and Utility Cast XP Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every committed Iron spellbook cast, including non-damaging utility spells, award cast XP to real Creative players without enabling other Creative automatic XP.

**Architecture:** Add a narrow `awardSpellCast` entry point to the existing XP service. It accepts only `SPELL_CAST`, allows Creative but rejects Spectator/FakePlayer, then delegates to the unchanged shared mutation pipeline; the Iron adapter remains driven solely by `SpellOnCastEvent` and never waits for a hit.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, Iron's Spells `1.20.1-3.16.2`, JUnit Jupiter 5.10.2, Gradle, PowerShell.

## Global Constraints

- Keep the general `XpAwardService.isEligible` behavior unchanged: Creative, Spectator, FakePlayer, and null remain rejected.
- Creative permission applies only to an exact `XpActionKind.SPELL_CAST` action.
- Spectator, FakePlayer, null, and non-spell actions remain rejected by the spell-specific entry point.
- Use only committed `SpellOnCastEvent`; no hit, target, damage, heal, summon, teleport, or school event may gate cast XP.
- Keep `CastSource.SPELLBOOK`, original level/mana formulas, level ceiling, rolling caps, snapshots, attribute refresh, and notices unchanged.
- Preserve Minecraft 1.20.1, Forge 47.4.10, Java 17, and Iron `1.20.1-3.16.2`.
- Replace only the STAT Mod JAR in `test-vrai`; preserve every other mod and create a timestamped backup.

---

### Task 1: Add spell-only Creative eligibility

**Files:**
- Modify: `src/test/java/tong/statmod/progression/xp/XpAwardServiceContractTest.java`
- Modify: `src/main/java/tong/statmod/progression/xp/XpAwardService.java`

**Interfaces:**
- Consumes: `XpAction.kind()`, `XpActionKind.SPELL_CAST`, and the existing shared award pipeline.
- Produces: `public static boolean awardSpellCast(ServerPlayer player, XpAction action, long tick)`.

- [ ] **Step 1: Write the failing service contract**

Add `assertTrue` and this test/helper to `XpAwardServiceContractTest`:

```java
@Test
void creativePermissionIsRestrictedToTheSpellCastEntryPoint() throws IOException {
    String source = Files.readString(Path.of(
            "src/main/java/tong/statmod/progression/xp/XpAwardService.java"));

    String general = method(source, "public static boolean isEligible(",
            "public static boolean award(");
    assertTrue(general.contains("!player.isCreative()"));
    assertTrue(general.contains("!player.isSpectator()"));
    assertTrue(general.contains("!(player instanceof FakePlayer)"));

    String spell = method(source, "public static boolean awardSpellCast(",
            "private static boolean awardEligible(");
    assertTrue(spell.contains("action.kind() != XpActionKind.SPELL_CAST"));
    assertTrue(spell.contains("player instanceof FakePlayer"));
    assertTrue(spell.contains("player.isSpectator()"));
    assertFalse(spell.contains("player.isCreative()"));
    assertTrue(spell.contains("awardEligible(player, List.of(action), tick)"));
}

private static String method(String source, String start, String end) {
    int from = source.indexOf(start);
    int to = source.indexOf(end, from + start.length());
    assertTrue(from >= 0 && to > from);
    return source.substring(from, to);
}
```

- [ ] **Step 2: Run RED**

```powershell
.\gradlew.bat test --tests tong.statmod.progression.xp.XpAwardServiceContractTest --rerun-tasks --console=plain
```

Expected: FAIL because `awardSpellCast` and `awardEligible` do not exist.

- [ ] **Step 3: Split eligibility from mutation and add the spell entry point**

Keep `isEligible` unchanged. Replace the start of `award` and add the new
method so the service reads:

```java
public static boolean award(ServerPlayer player, List<XpAction> actions, long tick) {
    if (!isEligible(player)) {
        return false;
    }
    return awardEligible(player, actions, tick);
}

public static boolean awardSpellCast(ServerPlayer player, XpAction action, long tick) {
    if (player == null
            || player instanceof FakePlayer
            || player.isSpectator()
            || action == null
            || action.kind() != XpActionKind.SPELL_CAST) {
        return false;
    }
    return awardEligible(player, List.of(action), tick);
}

private static boolean awardEligible(
        ServerPlayer player, List<XpAction> actions, long tick) {
    PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
            .resolve().orElse(null);
```

Move the rest of the existing `award` body unchanged under `awardEligible`.

- [ ] **Step 4: Run GREEN and regression tests**

```powershell
.\gradlew.bat test --tests tong.statmod.progression.xp.XpAwardServiceContractTest --tests tong.statmod.progression.xp.XpAwardCoordinatorTest --tests tong.statmod.AutomaticXpContractTest --rerun-tasks --console=plain
```

Expected: all selected tests PASS and there remains exactly one snapshot send in the service.

- [ ] **Step 5: Commit**

```powershell
git add -- src/main/java/tong/statmod/progression/xp/XpAwardService.java src/test/java/tong/statmod/progression/xp/XpAwardServiceContractTest.java
git commit -m "fix: allow creative Iron cast XP"
```

---

### Task 2: Route all committed utility casts through spell eligibility

**Files:**
- Modify: `src/test/java/tong/statmod/integration/ironspells/IronSpellXpEventsContractTest.java`
- Modify: `src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java`

**Interfaces:**
- Consumes: `XpAwardService.awardSpellCast(ServerPlayer, XpAction, long)`.
- Produces: one spell-specific award attempt per eligible committed spellbook cast, independent of targets and effects.

- [ ] **Step 1: Change the adapter contract first**

Replace the service assertions in `IronSpellXpEventsContractTest` with:

```java
assertFalse(source.contains("XpAwardService.isEligible(player)"));
assertEquals(1, occurrences(source, "XpAwardService.awardSpellCast("));
assertFalse(source.contains("XpAwardService.award("));
```

Add these utility-independence assertions:

```java
for (String forbidden : new String[] {
        "SpellDamageEvent", "SpellHealEvent", "SpellSummonEvent",
        "SpellTeleportEvent", "getTarget", "HitResult", "getDamage",
        "getHealAmount", "getSchoolType"
}) {
    assertFalse(source.contains(forbidden), forbidden);
}
```

- [ ] **Step 2: Run RED**

```powershell
.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronSpellXpEventsContractTest --rerun-tasks --console=plain
```

Expected: FAIL because the adapter still pre-filters with `isEligible` and calls generic `award`.

- [ ] **Step 3: Delegate directly to the spell-specific service**

Remove this condition from the adapter:

```java
|| !XpAwardService.isEligible(player)
```

Replace the generic call with:

```java
XpAwardService.awardSpellCast(
        player,
        XpAction.spellCast(
                event.getOriginalSpellLevel(),
                event.getOriginalManaCost()),
        player.serverLevel().getGameTime());
```

Remove the unused `java.util.List` import.

- [ ] **Step 4: Run GREEN and formula regressions**

```powershell
.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronSpellXpEventsContractTest --tests tong.statmod.progression.xp.XpRewardPolicyTest --tests tong.statmod.progression.xp.XpAwardServiceContractTest --rerun-tasks --console=plain
```

Expected: all selected tests PASS; no target/effect API appears in the adapter.

- [ ] **Step 5: Commit**

```powershell
git add -- src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java src/test/java/tong/statmod/integration/ironspells/IronSpellXpEventsContractTest.java
git commit -m "fix: count utility spellbook casts"
```

---

### Task 3: Document, verify, smoke, and deploy

**Files:**
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Verify: `build/libs/statmod-0.1.0+1.20.1.jar`
- Deploy: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods\statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes: completed Tasks 1-2 and the existing required-provider smoke script.
- Produces: verified/deployed fix plus a truthful manual acceptance checklist.

- [ ] **Step 1: Update the runtime behavior record**

Append to the Iron cast-XP paragraph:

```markdown
Creative players are accepted only by the dedicated spell-cast award path so
the development client can validate progression; Spectator and fake players
remain excluded. Cast XP is committed-event based: shields, healing, movement,
summons, control, and other non-damaging spells count without a target or hit.
All other automatic XP remains disabled in Creative.
```

- [ ] **Step 2: Commit documentation**

```powershell
git add -- docs/compatibility/forge-1.20.1-supported-runtime.md
git commit -m "docs: record creative utility cast XP"
```

- [ ] **Step 3: Run complete verification**

```powershell
.\gradlew.bat clean test build --console=plain
.\gradlew.bat test --rerun-tasks --console=plain
```

Expected: both commands report `BUILD SUCCESSFUL`; all tests have zero failures/errors.

- [ ] **Step 4: Inspect packaging isolation**

```powershell
$jar = (Resolve-Path 'build/libs/statmod-0.1.0+1.20.1.jar').Path
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $entries = @($zip.Entries | ForEach-Object FullName)
    if ($entries -notcontains 'tong/statmod/integration/ironspells/IronSpellXpEvents.class') {
        throw 'Missing IronSpellXpEvents.class'
    }
    $duplicates = @($zip.Entries | Group-Object FullName | Where-Object Count -GT 1)
    if ($duplicates.Count -ne 0) { throw 'Duplicate JAR entries detected.' }
    $embedded = @($entries | Where-Object { $_ -like 'io/redspace/ironsspellbooks/*' })
    if ($embedded.Count -ne 0) { throw 'Iron provider classes are embedded.' }
    "OK entries=$($entries.Count) adapter=present embeddedIron=0 duplicates=0"
}
finally { $zip.Dispose() }
```

Expected: `adapter=present embeddedIron=0 duplicates=0`.

- [ ] **Step 5: Run required-provider smoke**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke`.

- [ ] **Step 6: Back up and deploy only STAT Mod**

```powershell
$games = @(Get-CimInstance Win32_Process | Where-Object {
    ($_.Name -in @('java.exe','javaw.exe')) -and
    ($_.CommandLine -match 'cpw\.mods\.modlauncher\.Launcher|net\.minecraft\.client\.main\.Main|--launchTarget')
})
if ($games.Count -gt 0) { throw 'Minecraft is still running.' }
$source = (Resolve-Path 'build/libs/statmod-0.1.0+1.20.1.jar').Path
$mods = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
$target = Join-Path $mods 'statmod-0.1.0+1.20.1.jar'
$existing = @(Get-ChildItem -LiteralPath $mods -File -Filter 'statmod-*.jar')
if ($existing.Count -ne 1 -or $existing[0].FullName -ne $target) {
    throw "Unexpected STAT Mod JAR set: $($existing.FullName -join ', ')"
}
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backup = "C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\$stamp-before-creative-utility-cast-xp"
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item -LiteralPath $target -Destination (Join-Path $backup (Split-Path $target -Leaf))
Copy-Item -LiteralPath $source -Destination $target -Force
$sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
$targetHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
if ($sourceHash -ne $targetHash) { throw 'Deployed JAR hash mismatch.' }
"OK deployed=$target sha256=$targetHash backup=$backup"
```

Expected: matching hashes and a timestamped backup; no other mod JAR changes.

- [ ] **Step 7: Manual acceptance handoff**

In Creative mode, verify one damaging spell that misses and one shield spell
both show Arcane Power/Casting Speed notices plus Mana Pool when cost is
positive. Verify a cooldown failure gives none, Spectator gives none, and
non-spell Creative actions give no automatic XP.
