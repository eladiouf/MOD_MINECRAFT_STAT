# Forge Combat Stat Effects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply weapon-specific offensive scaling and bounded multiplicative physical defense from player stat levels on Forge 1.20.1.

**Architecture:** Plain Java value/policy types own all formulas and numerical safety. One Forge `LivingHurtEvent` adapter resolves attack classification and player capabilities, then updates the event once. A Forge server config produces an immutable rules snapshot for each event.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, ForgeGradle 6, JUnit 5.10.2, PowerShell 7.

## Global Constraints

- Work only on branch `forge-1.20.1` in `.worktrees/forge-1.20.1`.
- Keep the core loadable with Minecraft and Forge only.
- Do not import Iron's Spells, Epic Fight, Curios, addons, NeoForge, or Tensura.
- Add no mixin, persistent modifier, potion loop, or player-level cache.
- Preserve the 23 stable stat identifiers and current NBT/network formats.
- Preserve all 74 external addon JARs and their SHA-256 values.
- Do not stage or modify the pre-existing dirty modpack-script files.
- Follow the approved design in `docs/superpowers/specs/2026-07-15-forge-combat-stat-effects-design.md`.

---

### Task 1: Pure combat scaling policy

**Files:**
- Create: `src/main/java/tong/statmod/effects/CombatScalingRules.java`
- Create: `src/main/java/tong/statmod/effects/CombatStatScaling.java`
- Create: `src/test/java/tong/statmod/effects/CombatScalingRulesTest.java`
- Create: `src/test/java/tong/statmod/effects/CombatStatScalingTest.java`

**Interfaces:**
- Produces: `CombatScalingRules.defaults() : CombatScalingRules`.
- Produces: record accessors `weaponDamageBase()`, `weaponDamageScale()`, `weaponDamageExponent()`, `physicalResistanceCap()`, and `physicalEnduranceCap()`.
- Produces: `CombatStatScaling.offensiveStat(WeaponClassification) : Optional<StatType>`.
- Produces: `CombatStatScaling.offensiveMultiplier(int, CombatScalingRules) : double`.
- Produces: `CombatStatScaling.defensiveMultiplier(int, int, CombatScalingRules) : double`.
- Produces: `CombatStatScaling.applyMultiplier(float, double) : float`.

- [ ] **Step 1: Write failing tests for validated rules and exact curves**

Create `CombatScalingRulesTest`:

```java
package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CombatScalingRulesTest {
    @Test
    void exposesApprovedDefaults() {
        CombatScalingRules rules = CombatScalingRules.defaults();
        assertEquals(1.0, rules.weaponDamageBase());
        assertEquals(9.0, rules.weaponDamageScale());
        assertEquals(1.5, rules.weaponDamageExponent());
        assertEquals(0.65, rules.physicalResistanceCap());
        assertEquals(0.35, rules.physicalEnduranceCap());
    }

    @Test
    void replacesNonFiniteValuesAndClampsEveryRange() {
        CombatScalingRules rules = new CombatScalingRules(
                Double.NaN, 500.0, 0.0, 2.0, -1.0);
        assertEquals(1.0, rules.weaponDamageBase());
        assertEquals(99.0, rules.weaponDamageScale());
        assertEquals(0.1, rules.weaponDamageExponent());
        assertEquals(0.95, rules.physicalResistanceCap());
        assertEquals(0.0, rules.physicalEnduranceCap());
    }
}
```

Create `CombatStatScalingTest` with these tests:

```java
package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tong.statmod.progression.xp.WeaponClassification;
import tong.statmod.stats.StatType;

class CombatStatScalingTest {
    private static final double EPSILON = 0.000_001;

    @Test
    void followsExactDefaultOffensiveAnchors() {
        CombatScalingRules rules = CombatScalingRules.defaults();
        assertEquals(1.0, CombatStatScaling.offensiveMultiplier(0, rules), EPSILON);
        assertEquals(2.125, CombatStatScaling.offensiveMultiplier(25, rules), EPSILON);
        assertEquals(4.181980515, CombatStatScaling.offensiveMultiplier(50, rules), EPSILON);
        assertEquals(6.845671476, CombatStatScaling.offensiveMultiplier(75, rules), EPSILON);
        assertEquals(10.0, CombatStatScaling.offensiveMultiplier(100, rules), EPSILON);
    }

    @Test
    void clampsLevelsAndMapsOnlySupportedClassifications() {
        CombatScalingRules rules = CombatScalingRules.defaults();
        assertEquals(1.0, CombatStatScaling.offensiveMultiplier(-20, rules), EPSILON);
        assertEquals(10.0, CombatStatScaling.offensiveMultiplier(200, rules), EPSILON);
        assertEquals(StatType.BRUTE_FORCE,
                CombatStatScaling.offensiveStat(WeaponClassification.HEAVY).orElseThrow());
        assertEquals(StatType.BLADE_TECHNIQUE,
                CombatStatScaling.offensiveStat(WeaponClassification.BLADE).orElseThrow());
        assertEquals(StatType.PRECISION,
                CombatStatScaling.offensiveStat(WeaponClassification.PRECISION).orElseThrow());
        assertTrue(CombatStatScaling.offensiveStat(WeaponClassification.AMBIGUOUS).isEmpty());
        assertTrue(CombatStatScaling.offensiveStat(WeaponClassification.UNCLASSIFIED).isEmpty());
    }

    @Test
    void appliesBoundedMultiplicativePhysicalDefense() {
        CombatScalingRules rules = CombatScalingRules.defaults();
        assertEquals(1.0, CombatStatScaling.defensiveMultiplier(0, 0, rules), EPSILON);
        assertEquals(0.2275, CombatStatScaling.defensiveMultiplier(100, 100, rules), EPSILON);
        assertEquals(22.75F, CombatStatScaling.applyMultiplier(100F,
                CombatStatScaling.defensiveMultiplier(100, 100, rules)), 0.0001F);
    }

    @Test
    void keepsInvalidDamageNeutralAndClampsOverflow() {
        assertTrue(Float.isNaN(CombatStatScaling.applyMultiplier(Float.NaN, 2.0)));
        assertEquals(-4F, CombatStatScaling.applyMultiplier(-4F, 2.0));
        assertEquals(5F, CombatStatScaling.applyMultiplier(5F, Double.NaN));
        assertEquals(Float.MAX_VALUE,
                CombatStatScaling.applyMultiplier(Float.MAX_VALUE, 10.0));
    }
}
```

- [ ] **Step 2: Run focused tests and verify the missing types fail compilation**

Run: `./gradlew test --tests 'tong.statmod.effects.*' --console=plain`

Expected: compilation fails because `CombatScalingRules` and
`CombatStatScaling` do not exist.

- [ ] **Step 3: Implement the immutable validated rules**

```java
package tong.statmod.effects;

public record CombatScalingRules(
        double weaponDamageBase,
        double weaponDamageScale,
        double weaponDamageExponent,
        double physicalResistanceCap,
        double physicalEnduranceCap) {

    public CombatScalingRules {
        weaponDamageBase = finiteClamp(weaponDamageBase, 1.0, 1.0, 10.0);
        weaponDamageScale = finiteClamp(weaponDamageScale, 9.0, 0.0, 99.0);
        weaponDamageExponent = finiteClamp(weaponDamageExponent, 1.5, 0.1, 5.0);
        physicalResistanceCap = finiteClamp(physicalResistanceCap, 0.65, 0.0, 0.95);
        physicalEnduranceCap = finiteClamp(physicalEnduranceCap, 0.35, 0.0, 0.95);
    }

    public static CombatScalingRules defaults() {
        return new CombatScalingRules(1.0, 9.0, 1.5, 0.65, 0.35);
    }

    private static double finiteClamp(double value, double fallback, double minimum, double maximum) {
        double finite = Double.isFinite(value) ? value : fallback;
        return Math.max(minimum, Math.min(maximum, finite));
    }
}
```

- [ ] **Step 4: Implement the pure formula policy**

```java
package tong.statmod.effects;

import java.util.Optional;
import tong.statmod.progression.xp.WeaponClassification;
import tong.statmod.stats.StatProgress;
import tong.statmod.stats.StatType;

public final class CombatStatScaling {
    private CombatStatScaling() {
    }

    public static Optional<StatType> offensiveStat(WeaponClassification classification) {
        if (classification == null) {
            return Optional.empty();
        }
        return switch (classification) {
            case HEAVY -> Optional.of(StatType.BRUTE_FORCE);
            case BLADE -> Optional.of(StatType.BLADE_TECHNIQUE);
            case PRECISION -> Optional.of(StatType.PRECISION);
            case AMBIGUOUS, UNCLASSIFIED -> Optional.empty();
        };
    }

    public static double offensiveMultiplier(int level, CombatScalingRules rules) {
        int bounded = Math.max(0, Math.min(StatProgress.MAX_LEVEL, level));
        double progress = bounded / (double) StatProgress.MAX_LEVEL;
        double multiplier = rules.weaponDamageBase()
                + rules.weaponDamageScale() * Math.pow(progress, rules.weaponDamageExponent());
        return Double.isFinite(multiplier) && multiplier >= 0.0 ? multiplier : 1.0;
    }

    public static double defensiveMultiplier(
            int resistanceLevel, int enduranceLevel, CombatScalingRules rules) {
        double resistance = rules.physicalResistanceCap() * normalized(resistanceLevel);
        double endurance = rules.physicalEnduranceCap() * normalized(enduranceLevel);
        double multiplier = (1.0 - resistance) * (1.0 - endurance);
        return Double.isFinite(multiplier) ? Math.max(0.0, multiplier) : 1.0;
    }

    public static float applyMultiplier(float amount, double multiplier) {
        if (!Float.isFinite(amount) || amount <= 0F
                || !Double.isFinite(multiplier) || multiplier < 0.0) {
            return amount;
        }
        double scaled = amount * multiplier;
        if (!Double.isFinite(scaled) || scaled >= Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        return (float) Math.max(0.0, scaled);
    }

    private static double normalized(int level) {
        int bounded = Math.max(0, Math.min(StatProgress.MAX_LEVEL, level));
        return bounded / (double) StatProgress.MAX_LEVEL;
    }
}
```

- [ ] **Step 5: Run focused and complete tests**

Run: `./gradlew test --tests 'tong.statmod.effects.*' --console=plain`

Expected: all focused tests pass.

Run: `./gradlew test --console=plain`

Expected: the complete suite passes.

- [ ] **Step 6: Commit the pure policy**

```powershell
git add -- src/main/java/tong/statmod/effects/CombatScalingRules.java src/main/java/tong/statmod/effects/CombatStatScaling.java src/test/java/tong/statmod/effects/CombatScalingRulesTest.java src/test/java/tong/statmod/effects/CombatStatScalingTest.java
git commit -m "feat: define combat stat scaling policy"
```

### Task 2: Forge server configuration

**Files:**
- Create: `src/main/java/tong/statmod/config/StatModServerConfig.java`
- Modify: `src/main/java/tong/statmod/StatMod.java`
- Create: `src/test/java/tong/statmod/config/StatModServerConfigContractTest.java`

**Interfaces:**
- Consumes: `CombatScalingRules` from Task 1.
- Produces: `StatModServerConfig.SPEC : ForgeConfigSpec`.
- Produces: `StatModServerConfig.snapshot() : CombatScalingRules`.

- [ ] **Step 1: Write the failing config wiring contract**

```java
package tong.statmod.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StatModServerConfigContractTest {
    @Test
    void registersServerConfigAndDefinesEveryApprovedKey() throws Exception {
        String entrypoint = Files.readString(Path.of("src/main/java/tong/statmod/StatMod.java"));
        String config = Files.readString(Path.of(
                "src/main/java/tong/statmod/config/StatModServerConfig.java"));
        assertTrue(entrypoint.contains("ModConfig.Type.SERVER"));
        assertTrue(entrypoint.contains("StatModServerConfig.SPEC"));
        assertTrue(config.contains("weaponDamageBase"));
        assertTrue(config.contains("weaponDamageScale"));
        assertTrue(config.contains("weaponDamageExponent"));
        assertTrue(config.contains("physicalResistanceCap"));
        assertTrue(config.contains("physicalEnduranceCap"));
        assertTrue(config.contains("new CombatScalingRules("));
    }
}
```

- [ ] **Step 2: Run the contract and verify the missing config fails**

Run: `./gradlew test --tests tong.statmod.config.StatModServerConfigContractTest --console=plain`

Expected: FAIL with `NoSuchFileException` for `StatModServerConfig.java`.

- [ ] **Step 3: Implement the server config and immutable snapshot**

```java
package tong.statmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import tong.statmod.effects.CombatScalingRules;

public final class StatModServerConfig {
    private static final ForgeConfigSpec.DoubleValue WEAPON_DAMAGE_BASE;
    private static final ForgeConfigSpec.DoubleValue WEAPON_DAMAGE_SCALE;
    private static final ForgeConfigSpec.DoubleValue WEAPON_DAMAGE_EXPONENT;
    private static final ForgeConfigSpec.DoubleValue PHYSICAL_RESISTANCE_CAP;
    private static final ForgeConfigSpec.DoubleValue PHYSICAL_ENDURANCE_CAP;

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("combat");
        WEAPON_DAMAGE_BASE = builder.defineInRange("weaponDamageBase", 1.0, 1.0, 10.0);
        WEAPON_DAMAGE_SCALE = builder.defineInRange("weaponDamageScale", 9.0, 0.0, 99.0);
        WEAPON_DAMAGE_EXPONENT = builder.defineInRange("weaponDamageExponent", 1.5, 0.1, 5.0);
        PHYSICAL_RESISTANCE_CAP = builder.defineInRange(
                "physicalResistanceCap", 0.65, 0.0, 0.95);
        PHYSICAL_ENDURANCE_CAP = builder.defineInRange(
                "physicalEnduranceCap", 0.35, 0.0, 0.95);
        builder.pop();
        SPEC = builder.build();
    }

    private StatModServerConfig() {
    }

    public static CombatScalingRules snapshot() {
        return new CombatScalingRules(
                WEAPON_DAMAGE_BASE.get(),
                WEAPON_DAMAGE_SCALE.get(),
                WEAPON_DAMAGE_EXPONENT.get(),
                PHYSICAL_RESISTANCE_CAP.get(),
                PHYSICAL_ENDURANCE_CAP.get());
    }
}
```

Register it in `StatMod()` before network registration:

```java
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import tong.statmod.config.StatModServerConfig;

// Inside the existing constructor, before StatNetwork.register():
ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, StatModServerConfig.SPEC);
StatNetwork.register();
```

- [ ] **Step 4: Run the focused contract and build**

Run: `./gradlew test --tests tong.statmod.config.StatModServerConfigContractTest --console=plain`

Expected: PASS.

Run: `./gradlew compileJava --console=plain`

Expected: compilation succeeds against Forge 47.4.10.

- [ ] **Step 5: Commit the server configuration**

```powershell
git add -- src/main/java/tong/statmod/config/StatModServerConfig.java src/main/java/tong/statmod/StatMod.java src/test/java/tong/statmod/config/StatModServerConfigContractTest.java
git commit -m "feat: configure combat stat scaling"
```

### Task 3: Server-authoritative Forge damage adapter

**Files:**
- Create: `src/main/java/tong/statmod/event/CombatEffectEvents.java`
- Create: `src/test/java/tong/statmod/event/CombatEffectEventsContractTest.java`

**Interfaces:**
- Consumes: `StatModServerConfig.snapshot()`, `CombatStatScaling`,
  `WeaponClassifier`, `CombatEligibility`, and `StatCapabilities.PLAYER_STATS`.
- Produces: one Forge `LivingHurtEvent` subscriber that performs offense then defense and calls `event.setAmount` at most once.

- [ ] **Step 1: Write the failing event wiring contract**

```java
package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CombatEffectEventsContractTest {
    @Test
    void usesOneForgeHurtAdapterAndExistingClassificationBoundaries() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/event/CombatEffectEvents.java"));
        assertTrue(source.contains("LivingHurtEvent"));
        assertTrue(source.contains("StatModServerConfig.snapshot()"));
        assertTrue(source.contains("WeaponClassifier.classify"));
        assertTrue(source.contains("CombatEligibility.eligibleTarget"));
        assertTrue(source.contains("CombatEligibility.physicalProfile"));
        assertTrue(source.contains("StatCapabilities.PLAYER_STATS"));
        assertEquals(1, occurrences(source, "event.setAmount("));
        assertFalse(source.contains("irons_spellbooks"));
        assertFalse(source.contains("epicfight"));
        assertFalse(source.toLowerCase().contains("tensura"));
    }

    private static int occurrences(String source, String token) {
        return source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
```

- [ ] **Step 2: Run the contract and verify the missing adapter fails**

Run: `./gradlew test --tests tong.statmod.event.CombatEffectEventsContractTest --console=plain`

Expected: FAIL with `NoSuchFileException`.

- [ ] **Step 3: Implement offense resolution**

```java
package tong.statmod.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.effects.CombatScalingRules;
import tong.statmod.effects.CombatStatScaling;
import tong.statmod.progression.xp.CombatEligibility;
import tong.statmod.progression.xp.WeaponClassification;
import tong.statmod.progression.xp.WeaponClassifier;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class CombatEffectEvents {
    private CombatEffectEvents() {
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        float original = event.getAmount();
        if (!Float.isFinite(original) || original <= 0F) {
            return;
        }

        CombatScalingRules rules = StatModServerConfig.snapshot();
        float scaled = applyOffense(event.getEntity(), event.getSource(), original, rules);
        scaled = applyDefense(event.getEntity(), event.getSource(), scaled, rules);
        if (Float.compare(original, scaled) != 0) {
            event.setAmount(scaled);
        }
    }

    private static float applyOffense(
            LivingEntity target, DamageSource source, float amount, CombatScalingRules rules) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || !eligiblePlayer(attacker)
                || !CombatEligibility.eligibleTarget(attacker, target)) {
            return amount;
        }

        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
        boolean direct = source.getDirectEntity() == attacker;
        if (!projectile && !direct) {
            return amount;
        }

        WeaponClassification classification = WeaponClassifier.classify(
                attacker.getMainHandItem(), projectile);
        StatType stat = CombatStatScaling.offensiveStat(classification).orElse(null);
        if (stat == null) {
            return amount;
        }

        PlayerStats stats = attacker.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve().orElse(null);
        if (stats == null) {
            return amount;
        }
        double multiplier = CombatStatScaling.offensiveMultiplier(
                stats.get(stat).level(), rules);
        return CombatStatScaling.applyMultiplier(amount, multiplier);
    }

    private static float applyDefense(
            LivingEntity target, DamageSource source, float amount, CombatScalingRules rules) {
        if (!(target instanceof ServerPlayer victim)
                || !eligiblePlayer(victim)
                || !CombatEligibility.physicalProfile(source, victim)) {
            return amount;
        }

        PlayerStats stats = victim.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve().orElse(null);
        if (stats == null) {
            return amount;
        }
        double multiplier = CombatStatScaling.defensiveMultiplier(
                stats.get(StatType.PHYSICAL_RESISTANCE).level(),
                stats.get(StatType.PHYSICAL_ENDURANCE).level(),
                rules);
        return CombatStatScaling.applyMultiplier(amount, multiplier);
    }

    private static boolean eligiblePlayer(ServerPlayer player) {
        return !(player instanceof FakePlayer)
                && !player.isCreative()
                && !player.isSpectator();
    }
}
```

- [ ] **Step 4: Implement physical defense and the single event mutation**

Confirm the completed file from Step 3 contains exactly one final mutation:

```java
if (Float.compare(original, scaled) != 0) {
    event.setAmount(scaled);
}
```

Confirm with `rg -n "setAmount|setCanceled|XpAward|StatNetwork|addPermanentModifier|addEffect" src/main/java/tong/statmod/event/CombatEffectEvents.java`.
Expected: only the single `setAmount` line matches.

- [ ] **Step 5: Run focused and complete tests**

Run: `./gradlew test --tests tong.statmod.event.CombatEffectEventsContractTest --tests 'tong.statmod.effects.*' --console=plain`

Expected: focused tests pass.

Run: `./gradlew test --console=plain`

Expected: all tests pass.

- [ ] **Step 6: Commit the Forge adapter**

```powershell
git add -- src/main/java/tong/statmod/event/CombatEffectEvents.java src/test/java/tong/statmod/event/CombatEffectEventsContractTest.java
git commit -m "feat: apply combat stat effects"
```

### Task 4: Production verification and artifact contract

**Files:**
- Modify: `scripts/verify-clean-foundation.ps1`
- Modify: `README.md`

**Interfaces:**
- Consumes: all Task 1-3 classes.
- Produces: a production JAR gate that requires combat rules, policy, config,
  and adapter classes.

- [ ] **Step 1: Add exact JAR requirements**

Add these entries to `$requiredEntries` in the existing verifier:

```text
tong/statmod/effects/CombatScalingRules.class
tong/statmod/effects/CombatStatScaling.class
tong/statmod/config/StatModServerConfig.class
tong/statmod/event/CombatEffectEvents.class
```

- [ ] **Step 2: Document the active effects honestly**

Append this section to `README.md`:

```markdown
## Effets de combat des statistiques

Brute Force, Blade Technique et Precision multiplient uniquement les attaques
classées pour leur spécialisation. La courbe reste neutre au niveau 0 (`x1`)
et atteint `x10` au niveau 100. Les tags publics d'équipement déterminent la
classification, y compris pour les armes d'addons.

Physical Resistance et Physical Endurance réduisent uniquement les dégâts de
combat physiques éligibles. Leurs réductions sont multiplicatives et atteignent
ensemble `77,25 %` lorsque les deux statistiques sont au niveau 100.

La magie, la stamina, les perks, la qualité d'artisanat et l'intégration directe
d'Epic Fight seront activés dans leurs tranches fonctionnelles dédiées.
```

- [ ] **Step 3: Run the complete production gates**

Run: `./gradlew clean test build --console=plain`

Expected: `BUILD SUCCESSFUL`.

Run: `./scripts/verify-clean-foundation.ps1 -Mode After`

Expected: `OK mode=After branch=forge-1.20.1 jars=74 manifest=74`.

Run: `./scripts/smoke-gametest-server.ps1`

Expected: `OK dedicated Forge GameTest server smoke` and no fatal signature.

- [ ] **Step 4: Inspect repository state and commit verification updates**

Run: `git diff --check`.

Expected: exit code 0; line-ending notices are acceptable, whitespace errors
are not.

```powershell
git add -- scripts/verify-clean-foundation.ps1 README.md
git commit -m "docs: validate combat stat effects"
```

## Completion audit

- Run `rg -n "TBD|TODO|implement later|fill in|appropriate error handling|Similar to Task" docs/superpowers/plans/2026-07-15-forge-combat-stat-effects.md`; only this audit command may match.
- Confirm every acceptance criterion in the design maps to Tasks 1-4.
- Confirm every later method name exactly matches the Task 1 interface block.
- Confirm `git status --short` lists only the pre-existing modpack-script changes after all feature commits.
