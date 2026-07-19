# Forge 1.20.1 Iron's Spells Cast XP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Award server-authoritative Arcane Power, Casting Speed, and Mana Pool XP from successful Iron's Spells spellbook casts.

**Architecture:** Add Iron's Spells 3.16.2 as a pinned, deobfuscated compile-only dependency, translate its committed `SpellOnCastEvent` into one provider-neutral `XpAction`, and reuse the existing pure reward policy, rolling limiter, capability mutation, snapshot, notice, and attribute-refresh pipeline. Keep the Iron event adapter isolated under `integration.ironspells`; no school or affinity data enters progression.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, ForgeGradle 6, Iron's Spells `1.20.1-3.16.2`, JUnit Jupiter 5.10.2, PowerShell verification scripts.

## Global Constraints

- Work only in `.worktrees/forge-1.20.1` on branch `forge-1.20.1`.
- Minecraft stays `1.20.1`, Forge stays `47.4.10`, and Java stays `17`.
- Iron's Spells stays mandatory with `mods.toml` range `[1.20.1-3.16.2,)`, ordering `AFTER`, and side `BOTH`.
- Compile against exact artifact `maven.modrinth:irons-spells-n-spellbooks:1.20.1-3.16.2` from `https://api.modrinth.com/maven`.
- The Iron artifact is `compileOnly` and ForgeGradle-deobfuscated; never shade or embed `io/redspace/ironsspellbooks` classes.
- Accept only server-side `CastSource.SPELLBOOK` events with a non-blank spell ID and positive original spell level.
- Use `getOriginalSpellLevel()` and `getOriginalManaCost()`, never their mutable counterparts.
- Award no elemental affinity or school-specific XP.
- Leave Erudition learning/inscription XP and Magic Resistance damage-received XP out of this slice.
- Reuse the existing 200 XP per stat over 1,200 ticks rolling limit; add no second cast cooldown.
- Preserve unrelated user files and launcher-owned client JARs.

---

## File map

- `gradle.properties` — owns the pinned Iron's Spells compile version.
- `build.gradle` — owns the filtered Modrinth repository and deobfuscated compile-only dependency.
- `src/main/java/tong/statmod/progression/xp/XpActionKind.java` — declares the provider-neutral `SPELL_CAST` action kind.
- `src/main/java/tong/statmod/progression/xp/XpAction.java` — constructs normalized spell-cast actions from original level and mana cost.
- `src/main/java/tong/statmod/progression/xp/XpRewardPolicy.java` — calculates the three bounded cast rewards without Forge or Iron objects.
- `src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java` — filters the typed Iron event and delegates one action to `XpAwardService`.
- `src/test/java/tong/statmod/IronDependencyContractTest.java` — locks the repository, dependency scope, provider version, and mandatory metadata.
- `src/test/java/tong/statmod/progression/xp/XpRewardPolicyTest.java` — locks cast formulas and invalid-input behavior.
- `src/test/java/tong/statmod/AutomaticXpContractTest.java` — locks the 17 active XP stats and leaves only Erudition/Magic Resistance deferred.
- `src/test/java/tong/statmod/integration/ironspells/IronSpellXpEventsContractTest.java` — locks event filtering and original-value usage.
- `src/test/java/tong/statmod/CleanFoundationVerifierContractTest.java` — requires the adapter in the built-JAR verifier.
- `scripts/verify-clean-foundation.ps1` — verifies the integration class exists and external provider classes remain absent.
- `docs/compatibility/forge-1.20.1-supported-runtime.md` — records the activated Iron cast progression.
- `docs/compatibility/stat-attribute-provider-matrix.md` — records the intentional typed-event exception to the registry-only attribute bridge.
- `docs/superpowers/specs/2026-07-15-statmod-forge-1.20.1-master-remake-design.md` — updates the accepted baseline from 14 to 17 progressing stats.

---

### Task 1: Pin the Iron's Spells compile API

**Files:**
- Create: `src/test/java/tong/statmod/IronDependencyContractTest.java`
- Modify: `gradle.properties:11-14`
- Modify: `build.gradle:46-53`

**Interfaces:**
- Consumes: ForgeGradle's `fg.deobf(Object dependency)` and the existing mandatory `mods.toml` entry.
- Produces: compile-only mapped classes for `SpellOnCastEvent` and `CastSource`; property `irons_spellbooks_version=1.20.1-3.16.2`.

- [ ] **Step 1: Write the failing dependency contract test**

```java
package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronDependencyContractTest {
    @Test
    void pinsDeobfuscatedCompileOnlyIronApiWithoutEmbeddingIt() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"));
        String build = Files.readString(Path.of("build.gradle"));
        String metadata = Files.readString(
                Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(properties.contains(
                "irons_spellbooks_version=1.20.1-3.16.2"));
        assertTrue(build.contains("https://api.modrinth.com/maven"));
        assertTrue(build.contains("includeGroup 'maven.modrinth'"));
        assertTrue(build.contains(
                "compileOnly fg.deobf(\"maven.modrinth:irons-spells-n-spellbooks:${irons_spellbooks_version}\")"));
        assertTrue(metadata.contains("modId=\"irons_spellbooks\""));
        assertTrue(metadata.contains("mandatory=true"));
        assertTrue(metadata.contains("versionRange=\"[1.20.1-3.16.2,)\""));
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.IronDependencyContractTest --rerun-tasks --console=plain
```

Expected: FAIL because `irons_spellbooks_version`, the Modrinth repository, and the `compileOnly` coordinate do not exist yet.

- [ ] **Step 3: Add the pinned version and filtered repository**

Add to `gradle.properties` immediately after `mapping_version`:

```properties
irons_spellbooks_version=1.20.1-3.16.2
```

Replace the repository block in `build.gradle` with:

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'Modrinth'
        url = 'https://api.modrinth.com/maven'
        content {
            includeGroup 'maven.modrinth'
        }
    }
}
```

- [ ] **Step 4: Add the deobfuscated compile-only dependency**

Make the dependency block exactly:

```groovy
dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    compileOnly fg.deobf("maven.modrinth:irons-spells-n-spellbooks:${irons_spellbooks_version}")
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}
```

- [ ] **Step 5: Resolve dependencies and verify GREEN**

Run:

```powershell
.\gradlew.bat dependencies --configuration compileClasspath --console=plain
.\gradlew.bat test --tests tong.statmod.IronDependencyContractTest --rerun-tasks --console=plain
```

Expected: dependency output contains `maven.modrinth:irons-spells-n-spellbooks:1.20.1-3.16.2`; focused test PASS.

- [ ] **Step 6: Commit the compile contract**

```powershell
git add -- gradle.properties build.gradle src/test/java/tong/statmod/IronDependencyContractTest.java
git commit -m "build: compile against Iron spell API"
```

---

### Task 2: Add the pure spell-cast reward policy

**Files:**
- Modify: `src/main/java/tong/statmod/progression/xp/XpActionKind.java:3-17`
- Modify: `src/main/java/tong/statmod/progression/xp/XpAction.java:41-48`
- Modify: `src/main/java/tong/statmod/progression/xp/XpRewardPolicy.java:15-83`
- Modify: `src/test/java/tong/statmod/progression/xp/XpRewardPolicyTest.java`
- Modify: `src/test/java/tong/statmod/AutomaticXpContractTest.java:35-93`

**Interfaces:**
- Consumes: `XpAction`'s existing `magnitude` and `quantity` fields and `StatXpAward(StatType, int, String)`.
- Produces: `XpAction.spellCast(int originalSpellLevel, int originalManaCost)` and `XpActionKind.SPELL_CAST`; `XpRewardPolicy.awards` emits Arcane Power, Casting Speed, and optionally Mana Pool.

- [ ] **Step 1: Write failing reward formula tests**

Add these methods to `XpRewardPolicyTest` and add the `assertFalse` static import:

```java
@Test
void spellCastRewardsCoreMagicStatsFromOriginalEffort() {
    List<StatXpAward> minimum = XpRewardPolicy.awards(XpAction.spellCast(1, 0));
    assertEquals(2, minimum.size());
    assertEquals(3, amountFor(minimum, StatType.ARCANE_POWER));
    assertEquals(2, amountFor(minimum, StatType.CASTING_SPEED));

    List<StatXpAward> ordinary = XpRewardPolicy.awards(XpAction.spellCast(4, 21));
    assertEquals(3, ordinary.size());
    assertEquals(6, amountFor(ordinary, StatType.ARCANE_POWER));
    assertEquals(3, amountFor(ordinary, StatType.CASTING_SPEED));
    assertEquals(3, amountFor(ordinary, StatType.MANA_POOL));

    List<StatXpAward> capped = XpRewardPolicy.awards(
            XpAction.spellCast(Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertEquals(12, amountFor(capped, StatType.ARCANE_POWER));
    assertEquals(6, amountFor(capped, StatType.CASTING_SPEED));
    assertEquals(15, amountFor(capped, StatType.MANA_POOL));
}

@Test
void spellCastRejectsInvalidLevelAndNeverRewardsDeferredMagicStats() {
    assertTrue(XpRewardPolicy.awards(XpAction.spellCast(0, 20)).isEmpty());
    assertTrue(XpRewardPolicy.awards(XpAction.spellCast(-1, 20)).isEmpty());

    List<StatXpAward> negativeCost = XpRewardPolicy.awards(
            XpAction.spellCast(3, -10));
    assertEquals(2, negativeCost.size());
    assertFalse(negativeCost.stream().map(StatXpAward::stat)
            .anyMatch(stat -> stat == StatType.MANA_POOL
                    || stat == StatType.ERUDITION
                    || stat == StatType.MAGIC_RESISTANCE));
}
```

Replace `neverEmitsDeferredMagicalStats` with:

```java
@Test
void neverEmitsEruditionOrMagicResistanceInCurrentPolicy() {
    Set<StatType> deferred = EnumSet.of(
            StatType.ERUDITION, StatType.MAGIC_RESISTANCE);
    for (XpActionKind kind : XpActionKind.values()) {
        XpAction action = kind == XpActionKind.SPELL_CAST
                ? XpAction.spellCast(4, 20)
                : new XpAction(kind, 120, 4, 2, true, null);
        assertTrue(XpRewardPolicy.awards(action).stream()
                .map(StatXpAward::stat).noneMatch(deferred::contains));
    }
}
```

- [ ] **Step 2: Update the global automatic-XP contract to expect 17 active stats**

Rename `mainCodeHasNoOptionalModImports` to
`mainCodeHasNoUnsupportedProviderImports`. Rename `hasOptionalModImport` to
`hasUnsupportedProviderImport` and make it:

```java
private static boolean hasUnsupportedProviderImport(String source) {
    return source.lines()
            .map(String::strip)
            .filter(line -> line.startsWith("import "))
            .anyMatch(line -> line.contains("epicfight")
                    || line.contains("tensura")
                    || line.contains("parcool"));
}
```

Replace `rewardPolicyActivatesExactlyFourteenNonMagicalStats` with:

```java
@Test
void rewardPolicyActivatesFourteenNonMagicalAndThreeCastStats() {
    List<XpAction> actions = List.of(
            XpAction.damage(XpActionKind.MELEE_HEAVY, 5),
            XpAction.damage(XpActionKind.MELEE_BLADE, 5),
            XpAction.damage(XpActionKind.PROJECTILE, 5),
            XpAction.damage(XpActionKind.PHYSICAL_DAMAGE_RECEIVED, 5),
            XpAction.damage(XpActionKind.SHIELD_BLOCKED, 5),
            XpAction.damage(XpActionKind.WILLPOWER_SURVIVAL, 5),
            XpAction.combo(3), XpAction.landing(8), XpAction.biome(),
            XpAction.kill(120, true), XpAction.forging(100, 1),
            XpAction.cooking(1), XpAction.alchemy(1, 0),
            XpAction.spellCast(4, 20));
    Set<StatType> emitted = actions.stream()
            .flatMap(action -> XpRewardPolicy.awards(action).stream())
            .map(StatXpAward::stat)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(StatType.class)));

    Set<StatType> deferred = EnumSet.of(
            StatType.ERUDITION, StatType.MAGIC_RESISTANCE);
    assertEquals(17, emitted.size());
    assertTrue(emitted.stream().noneMatch(deferred::contains));
}
```

Update the renamed test body to call `hasUnsupportedProviderImport(source)`.

- [ ] **Step 3: Run the focused tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.progression.xp.XpRewardPolicyTest --tests tong.statmod.AutomaticXpContractTest --rerun-tasks --console=plain
```

Expected: compilation FAIL because `SPELL_CAST` and `spellCast` do not exist.

- [ ] **Step 4: Add the normalized spell-cast action**

Append `SPELL_CAST` after `POTION_BREWED` in `XpActionKind`:

```java
POTION_BREWED,
SPELL_CAST
```

Add to `XpAction` before `withOpponent`:

```java
public static XpAction spellCast(int originalSpellLevel, int originalManaCost) {
    return new XpAction(
            XpActionKind.SPELL_CAST,
            originalManaCost,
            originalSpellLevel,
            0,
            false,
            null);
}
```

- [ ] **Step 5: Implement bounded spell rewards without integer overflow**

Add this switch branch in `XpRewardPolicy.awards`:

```java
case SPELL_CAST -> spellCastAwards(action);
```

Add this method before `single`:

```java
private static List<StatXpAward> spellCastAwards(XpAction action) {
    int level = action.quantity();
    double manaCost = action.magnitude();
    if (level <= 0 || !Double.isFinite(manaCost)) {
        return List.of();
    }

    List<StatXpAward> awards = new ArrayList<>(3);
    int arcanePower = (int) Math.min(12L, 2L + level);
    int castingSpeed = (int) Math.min(6L, 1L + ((long) level + 1L) / 2L);
    awards.add(new StatXpAward(
            StatType.ARCANE_POWER, arcanePower, action.kind().name()));
    awards.add(new StatXpAward(
            StatType.CASTING_SPEED, castingSpeed, action.kind().name()));

    if (manaCost > 0) {
        long manaUnits = (long) Math.ceil(manaCost / 10.0);
        int manaPool = (int) Math.min(15L, Math.max(1L, manaUnits));
        awards.add(new StatXpAward(
                StatType.MANA_POOL, manaPool, action.kind().name()));
    }
    return List.copyOf(awards);
}
```

- [ ] **Step 6: Run focused and coordinator tests and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.progression.xp.XpRewardPolicyTest --tests tong.statmod.progression.xp.XpAwardCoordinatorTest --tests tong.statmod.AutomaticXpContractTest --rerun-tasks --console=plain
```

Expected: all selected tests PASS; the coordinator still applies the existing rolling limits and batching unchanged.

- [ ] **Step 7: Commit the pure progression policy**

```powershell
git add -- src/main/java/tong/statmod/progression/xp/XpActionKind.java src/main/java/tong/statmod/progression/xp/XpAction.java src/main/java/tong/statmod/progression/xp/XpRewardPolicy.java src/test/java/tong/statmod/progression/xp/XpRewardPolicyTest.java src/test/java/tong/statmod/AutomaticXpContractTest.java
git commit -m "feat: reward Iron spellbook casts"
```

---

### Task 3: Bridge committed Iron spellbook casts to the XP service

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java`
- Create: `src/test/java/tong/statmod/integration/ironspells/IronSpellXpEventsContractTest.java`

**Interfaces:**
- Consumes: Iron's `SpellOnCastEvent`, `CastSource.SPELLBOOK`, `XpAction.spellCast(int, int)`, and `XpAwardService.award(ServerPlayer, List<XpAction>, long)`.
- Produces: `IronSpellXpEvents.onSpellCast(SpellOnCastEvent)` registered on the Forge event bus.

- [ ] **Step 1: Write the failing adapter contract test**

```java
package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronSpellXpEventsContractTest {
    @Test
    void acceptsOnlyCommittedServerSpellbookCastsAndUsesOriginalValues()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java"));

        assertTrue(source.contains("@Mod.EventBusSubscriber"));
        assertTrue(source.contains("bus = Mod.EventBusSubscriber.Bus.FORGE"));
        assertTrue(source.contains("@SubscribeEvent"));
        assertTrue(source.contains("SpellOnCastEvent"));
        assertTrue(source.contains("instanceof ServerPlayer player"));
        assertTrue(source.contains("XpAwardService.isEligible(player)"));
        assertTrue(source.contains(
                "event.getCastSource() != CastSource.SPELLBOOK"));
        assertTrue(source.contains("event.getSpellId().isBlank()"));
        assertTrue(source.contains("event.getOriginalSpellLevel()"));
        assertTrue(source.contains("event.getOriginalManaCost()"));
        assertTrue(source.contains("XpAction.spellCast("));
        assertEquals(1, occurrences(source, "XpAwardService.award("));
        assertFalse(source.contains("event.getSpellLevel()"));
        assertFalse(source.contains("event.getManaCost()"));
        assertFalse(source.contains("getSchoolType"));
        assertFalse(source.contains("StatType."));
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length())
                / token.length();
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.ironspells.IronSpellXpEventsContractTest --rerun-tasks --console=plain
```

Expected: FAIL with `NoSuchFileException` because the adapter does not exist.

- [ ] **Step 3: Implement the isolated typed event adapter**

Create `IronSpellXpEvents.java` with:

```java
package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.progression.xp.XpAction;
import tong.statmod.progression.xp.XpAwardService;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IronSpellXpEvents {
    private IronSpellXpEvents() {
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !XpAwardService.isEligible(player)
                || event.getCastSource() != CastSource.SPELLBOOK
                || event.getSpellId() == null
                || event.getSpellId().isBlank()
                || event.getOriginalSpellLevel() <= 0) {
            return;
        }

        XpAwardService.award(
                player,
                List.of(XpAction.spellCast(
                        event.getOriginalSpellLevel(),
                        event.getOriginalManaCost())),
                player.serverLevel().getGameTime());
    }
}
```

- [ ] **Step 4: Compile and run adapter/service contracts**

Run:

```powershell
.\gradlew.bat compileJava test --tests tong.statmod.integration.ironspells.IronSpellXpEventsContractTest --tests tong.statmod.progression.xp.XpAwardServiceContractTest --rerun-tasks --console=plain
```

Expected: compilation succeeds against the pinned deobfuscated Iron API; both selected tests PASS.

- [ ] **Step 5: Confirm the integration stays isolated**

Run:

```powershell
rg -n "io\.redspace\.ironsspellbooks" src/main/java
```

Expected: exactly the two imports in `integration/ironspells/IronSpellXpEvents.java`; no Iron import appears in `stats`, `progression`, `effects`, `network`, or `client`.

- [ ] **Step 6: Commit the server event bridge**

```powershell
git add -- src/main/java/tong/statmod/integration/ironspells/IronSpellXpEvents.java src/test/java/tong/statmod/integration/ironspells/IronSpellXpEventsContractTest.java
git commit -m "feat: bridge Iron casts to stat XP"
```

---

### Task 4: Lock packaging and update the supported-runtime record

**Files:**
- Modify: `src/test/java/tong/statmod/CleanFoundationVerifierContractTest.java`
- Modify: `scripts/verify-clean-foundation.ps1:67-92`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md:8-12,79-94`
- Modify: `docs/compatibility/stat-attribute-provider-matrix.md:15-29,72-93,141-171`
- Modify: `docs/superpowers/specs/2026-07-15-statmod-forge-1.20.1-master-remake-design.md:27-39,115-123`

**Interfaces:**
- Consumes: built class path `tong/statmod/integration/ironspells/IronSpellXpEvents.class` and the existing forbidden prefix `io/redspace/ironsspellbooks/`.
- Produces: executable packaging guard and truthful documentation that exactly 17 stats now have automatic XP sources.

- [ ] **Step 1: Make the verifier contract fail on a missing event adapter requirement**

Rename the verifier test to `requiresIronMagicIntegrationClassesInBuiltJar` and add:

```java
assertTrue(source.contains(
        "tong/statmod/integration/ironspells/IronSpellXpEvents.class"));
assertTrue(source.contains("io/redspace/ironsspellbooks/"));
```

- [ ] **Step 2: Run the verifier contract and verify RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.CleanFoundationVerifierContractTest --rerun-tasks --console=plain
```

Expected: FAIL because the new integration class is not in `$requiredEntries`.

- [ ] **Step 3: Require the adapter in the built-JAR verifier**

Add this entry immediately after `XpAwardService.class` in
`scripts/verify-clean-foundation.ps1`:

```powershell
'tong/statmod/integration/ironspells/IronSpellXpEvents.class'
```

Keep the existing forbidden prefix unchanged:

```powershell
'io/redspace/ironsspellbooks/'
```

- [ ] **Step 4: Update runtime and provider documentation**

In `forge-1.20.1-supported-runtime.md`, replace the Iron row with:

```markdown
| Iron's Spells 'n Spellbooks | 3.16.2 | required | Mandatory provider for spell power, casting, mana, and magic-resistance attributes; successful spellbook casts now award Arcane Power, Casting Speed, and Mana Pool XP. |
```

After the stats-screen paragraph, add:

```markdown
Successful server-side Iron spellbook casts use the provider's original spell
level and mana cost to progress Arcane Power, Casting Speed, and Mana Pool.
Scrolls, spellblades, commands, mobs, schools, and retired affinities do not
produce cast XP. Erudition and Magic Resistance await their dedicated gameplay
sources.
```

In `stat-attribute-provider-matrix.md`, add to the integration policy:

```markdown
8. Iron's public typed `SpellOnCastEvent` is used only inside
   `integration.ironspells` because registry attributes cannot report a
   committed cast. Original level and mana values feed the provider-neutral XP
   policy; school data is discarded.
```

In the Iron section add:

```markdown
STAT Mod also compiles against the pinned public `SpellOnCastEvent` and
`CastSource` API. Only committed `SPELLBOOK` events reach automatic XP; the
provider JAR remains compile-only and is never embedded.
```

In the master design accepted baseline, replace:

```markdown
- automatic XP for 14 non-magical statistics;
```

with:

```markdown
- automatic XP for 14 non-magical statistics plus Arcane Power, Casting Speed,
  and Mana Pool from committed Iron's Spells spellbook casts;
```

Under Milestone 3, add this status sentence after the heading:

```markdown
The core attribute bridge and spellbook-cast XP slice are implemented.
Erudition learning XP and Magic Resistance damage-received XP remain.
```

- [ ] **Step 5: Run documentation/verifier tests and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.CleanFoundationVerifierContractTest --tests tong.statmod.IronDependencyContractTest --tests tong.statmod.AutomaticXpContractTest --rerun-tasks --console=plain
git diff --check
```

Expected: all selected tests PASS and `git diff --check` reports no errors.

- [ ] **Step 6: Commit packaging and documentation**

```powershell
git add -- scripts/verify-clean-foundation.ps1 src/test/java/tong/statmod/CleanFoundationVerifierContractTest.java docs/compatibility/forge-1.20.1-supported-runtime.md docs/compatibility/stat-attribute-provider-matrix.md docs/superpowers/specs/2026-07-15-statmod-forge-1.20.1-master-remake-design.md
git commit -m "docs: record Iron cast progression"
```

---

### Task 5: Full verification and clean `test-vrai` deployment

**Files:**
- Verify: `build/libs/statmod-0.1.0+1.20.1.jar`
- Deploy: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods\statmod-0.1.0+1.20.1.jar`
- Backup pattern: `C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\yyyyMMdd-HHmmss-before-irons-cast-xp\statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes: completed Tasks 1-4, exact required-provider JARs already present in `test-vrai`, and `scripts/smoke-gametest-server.ps1`.
- Produces: verified STAT Mod JAR deployed without changing any other client mod.

- [ ] **Step 1: Run the complete suite from a clean output directory**

Run:

```powershell
.\gradlew.bat clean test build --console=plain
.\gradlew.bat test --rerun-tasks --console=plain
```

Expected: `BUILD SUCCESSFUL` twice and zero failed JUnit tests.

- [ ] **Step 2: Inspect the built JAR for the adapter and provider isolation**

Run:

```powershell
$jar = Resolve-Path 'build/libs/statmod-0.1.0+1.20.1.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $entries = @($zip.Entries | ForEach-Object FullName)
    if ($entries -notcontains 'tong/statmod/integration/ironspells/IronSpellXpEvents.class') {
        throw 'Missing IronSpellXpEvents.class'
    }
    $duplicates = @($zip.Entries | Group-Object FullName | Where-Object Count -GT 1)
    if ($duplicates.Count -ne 0) {
        throw "Duplicate entries: $($duplicates.Name -join ', ')"
    }
    $embedded = @($entries | Where-Object { $_ -like 'io/redspace/ironsspellbooks/*' })
    if ($embedded.Count -ne 0) {
        throw "Embedded Iron classes: $($embedded -join ', ')"
    }
    "OK entries=$($entries.Count) adapter=present embeddedIron=0 duplicates=0"
}
finally {
    $zip.Dispose()
}
```

Expected: `OK ... adapter=present embeddedIron=0 duplicates=0`.

- [ ] **Step 3: Run the required-provider dedicated-server smoke**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke`, successful save of vanilla dimensions and `irons_spellbooks:pocket_dimension`, and no fatal/crash signature.

- [ ] **Step 4: Back up and replace only the STAT Mod JAR**

Close Minecraft first, then run:

```powershell
$source = (Resolve-Path 'build/libs/statmod-0.1.0+1.20.1.jar').Path
$mods = 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
$target = Join-Path $mods 'statmod-0.1.0+1.20.1.jar'
$existing = @(Get-ChildItem -LiteralPath $mods -File -Filter 'statmod-*.jar')
if ($existing.Count -ne 1 -or $existing[0].FullName -ne $target) {
    throw "Expected one canonical STAT Mod JAR, found: $($existing.FullName -join ', ')"
}
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backup = "C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\$stamp-before-irons-cast-xp"
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item -LiteralPath $target -Destination (Join-Path $backup (Split-Path $target -Leaf))
Copy-Item -LiteralPath $source -Destination $target -Force
$sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
$targetHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
if ($sourceHash -ne $targetHash) {
    throw 'Deployed JAR hash mismatch.'
}
"OK deployed=$target sha256=$targetHash backup=$backup"
```

Expected: matching SHA-256 hashes and a timestamped backup path. No other JAR is copied, moved, or removed.

- [ ] **Step 5: Record the manual client acceptance checklist**

Launch `test-vrai`, create or load a world, then verify:

1. cast a valid spell from a held Iron spellbook and observe Arcane Power and Casting Speed XP;
2. cast a positive-mana spell and observe Mana Pool XP;
3. attempt a cast during cooldown or without enough mana and observe no XP;
4. cast from a scroll or spellblade and observe no cast XP;
5. press `P` and confirm the three updated values persist after save/reload;
6. confirm spell power, cast-time/cooldown, maximum mana, and regeneration still reflect their stat levels;
7. confirm no affinity statistic appears.

- [ ] **Step 6: Confirm final repository state**

Run:

```powershell
git status --short --branch
git log -6 --oneline
```

Expected: branch `forge-1.20.1`, no uncommitted implementation changes, and the four scoped implementation commits following the design/plan commits.
