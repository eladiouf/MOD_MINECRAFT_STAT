# Unified Stamina and Epic Fight MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a unified `STAT Mod` stamina system, bridge it into native `Epic Fight` combat skill flow, and add Overworld-only 32/16 time control with semi-realistic sleep recovery.

**Architecture:** Keep `Epic Fight` as the visible combat UX and keep `STAT Mod` as the source of truth for stamina, fatigue, unlock gating, and world-time rules. Implement stamina as its own attachment and package, then bridge it into Epic Fight through a focused integration layer instead of inflating `EpicFightCompat.java`.

**Tech Stack:** NeoForge 1.21.1, Java 21, NeoForge attachments, NeoForge payloads, Epic Fight event hooks, JUnit 5, Gradle

---

## File Structure

### New units

- `src/main/java/tong/statmod/stamina/StaminaData.java`
  - attachment-backed persistent stamina state
- `src/main/java/tong/statmod/stamina/StaminaRules.java`
  - derived max stamina, thresholds, costs, passive drain/recovery math
- `src/main/java/tong/statmod/stamina/StaminaThreshold.java`
  - enum for `NORMAL`, `LOW`, `CRITICAL`
- `src/main/java/tong/statmod/stamina/StaminaManager.java`
  - common API for consuming, restoring, ticking, threshold evaluation
- `src/main/java/tong/statmod/stamina/MeditationManager.java`
  - seated meditation state and recovery loop
- `src/main/java/tong/statmod/stamina/StaminaEvents.java`
  - player tick, food, sleep, sprint, jump, movement hooks
- `src/main/java/tong/statmod/integration/epicfight/EpicFightStaminaBridge.java`
  - Epic Fight stamina display bridge, skill cost gate, selected-skill gating
- `src/main/java/tong/statmod/integration/epicfight/EpicFightSkillRequirementResolver.java`
  - maps 5 MVP Epic Fight skills to `STAT Mod` stat requirements
- `src/main/java/tong/statmod/time/OverworldTimeController.java`
  - Overworld-only 32/16 day-night pacing
- `src/main/java/tong/statmod/time/SleepRecoveryHandler.java`
  - semi-realistic sleep recovery and wake refill
- `src/main/java/tong/statmod/network/StaminaSyncPayload.java`
  - server-to-client stamina sync packet
- `src/main/java/tong/statmod/client/ClientStaminaCache.java`
  - local stamina cache for client HUD decisions and future use

### Existing files to modify

- `src/main/java/tong/statmod/storage/ModAttachments.java`
  - register new stamina attachment
- `src/main/java/tong/statmod/STATMod.java`
  - register stamina, bridge, meditation, time controller
- `src/main/java/tong/statmod/network/NetworkHandler.java`
  - register `StaminaSyncPayload`
- `src/main/java/tong/statmod/network/ClientPayloadHandler.java`
  - handle `StaminaSyncPayload`
- `src/main/java/tong/statmod/network/SyncHelper.java`
  - add stamina sync helper
- `src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java`
  - delegate stamina-related skill gating and low-stamina penalties
- `src/main/java/tong/statmod/storage/PlayerStatData.java`
  - do not add stamina storage here; only use stats as derived max stamina inputs

### Tests

- `src/test/java/tong/statmod/stamina/StaminaRulesTest.java`
- `src/test/java/tong/statmod/stamina/StaminaManagerTest.java`
- `src/test/java/tong/statmod/integration/epicfight/EpicFightStaminaBridgeTest.java`
- `src/test/java/tong/statmod/integration/epicfight/EpicFightSkillRequirementResolverTest.java`
- `src/test/java/tong/statmod/time/OverworldTimeControllerTest.java`
- `src/test/java/tong/statmod/time/SleepRecoveryHandlerTest.java`

---

### Task 1: Add Stamina Attachment and Pure Rules

**Files:**
- Create: `src/main/java/tong/statmod/stamina/StaminaData.java`
- Create: `src/main/java/tong/statmod/stamina/StaminaThreshold.java`
- Create: `src/main/java/tong/statmod/stamina/StaminaRules.java`
- Modify: `src/main/java/tong/statmod/storage/ModAttachments.java`
- Test: `src/test/java/tong/statmod/stamina/StaminaRulesTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.stamina;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaminaRulesTest {
    @Test
    void maxStaminaUsesBasePlusEnduranceScaling() {
        assertEquals(100.0f, StaminaRules.maxStamina(0));
        assertEquals(150.0f, StaminaRules.maxStamina(50));
    }

    @Test
    void thresholdUsesRatioBands() {
        assertEquals(StaminaThreshold.NORMAL, StaminaRules.threshold(90.0f, 100.0f));
        assertEquals(StaminaThreshold.LOW, StaminaRules.threshold(30.0f, 100.0f));
        assertEquals(StaminaThreshold.CRITICAL, StaminaRules.threshold(10.0f, 100.0f));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.stamina.StaminaRulesTest`  
Expected: FAIL with missing `StaminaRules` and `StaminaThreshold`

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.stamina;

public enum StaminaThreshold {
    NORMAL,
    LOW,
    CRITICAL
}
```

```java
package tong.statmod.stamina;

public final class StaminaRules {
    public static final float BASE_MAX_STAMINA = 100.0f;
    public static final float ENDURANCE_BONUS_PER_LEVEL = 1.0f;
    public static final float LOW_THRESHOLD_RATIO = 0.35f;
    public static final float CRITICAL_THRESHOLD_RATIO = 0.15f;

    private StaminaRules() {}

    public static float maxStamina(int enduranceLevel) {
        return BASE_MAX_STAMINA + Math.max(0, enduranceLevel) * ENDURANCE_BONUS_PER_LEVEL;
    }

    public static StaminaThreshold threshold(float current, float max) {
        if (max <= 0.0f) {
            return StaminaThreshold.CRITICAL;
        }
        float ratio = Math.max(0.0f, current) / max;
        if (ratio <= CRITICAL_THRESHOLD_RATIO) return StaminaThreshold.CRITICAL;
        if (ratio <= LOW_THRESHOLD_RATIO) return StaminaThreshold.LOW;
        return StaminaThreshold.NORMAL;
    }
}
```

```java
package tong.statmod.stamina;

public class StaminaData {
    private float currentStamina = StaminaRules.BASE_MAX_STAMINA;
    private float fatigueDebt;
    private boolean meditating;

    public float currentStamina() { return currentStamina; }
    public void setCurrentStamina(float value) { currentStamina = Math.max(0.0f, value); }
    public float fatigueDebt() { return fatigueDebt; }
    public void setFatigueDebt(float value) { fatigueDebt = Math.max(0.0f, value); }
    public boolean meditating() { return meditating; }
    public void setMeditating(boolean value) { meditating = value; }
}
```

```java
public static final DeferredHolder<AttachmentType<?>, AttachmentType<StaminaData>> STAMINA =
        ATTACHMENTS.register("stamina", () ->
                AttachmentType.builder(StaminaData::new)
                        .serialize(StaminaSerializer.INSTANCE)
                        .build());
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.stamina.StaminaRulesTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/stamina/StaminaData.java src/main/java/tong/statmod/stamina/StaminaThreshold.java src/main/java/tong/statmod/stamina/StaminaRules.java src/main/java/tong/statmod/storage/ModAttachments.java src/test/java/tong/statmod/stamina/StaminaRulesTest.java
git commit -m "Add stamina attachment and core stamina rules"
```

### Task 2: Add Stamina Manager and Network Sync

**Files:**
- Create: `src/main/java/tong/statmod/stamina/StaminaManager.java`
- Create: `src/main/java/tong/statmod/network/StaminaSyncPayload.java`
- Create: `src/main/java/tong/statmod/client/ClientStaminaCache.java`
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Modify: `src/main/java/tong/statmod/network/ClientPayloadHandler.java`
- Modify: `src/main/java/tong/statmod/network/SyncHelper.java`
- Test: `src/test/java/tong/statmod/stamina/StaminaManagerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.stamina;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaminaManagerTest {
    @Test
    void consumeReturnsFalseWhenAmountExceedsCurrentPool() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(20.0f);

        assertFalse(StaminaManager.consume(data, 30.0f));
        assertEquals(20.0f, data.currentStamina(), 0.0001f);
    }

    @Test
    void restoreClampsToDerivedMax() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(90.0f);

        StaminaManager.restore(data, 30.0f, 10);
        assertEquals(110.0f, data.currentStamina(), 0.0001f);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.stamina.StaminaManagerTest`  
Expected: FAIL with missing `StaminaManager`

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.stamina;

public final class StaminaManager {
    private StaminaManager() {}

    public static boolean consume(StaminaData data, float amount) {
        if (data == null || amount <= 0.0f) return true;
        if (data.currentStamina() < amount) return false;
        data.setCurrentStamina(data.currentStamina() - amount);
        return true;
    }

    public static void restore(StaminaData data, float amount, int enduranceLevel) {
        if (data == null || amount <= 0.0f) return;
        float max = StaminaRules.maxStamina(enduranceLevel);
        data.setCurrentStamina(Math.min(max, data.currentStamina() + amount));
    }
}
```

```java
public record StaminaSyncPayload(float currentStamina, float fatigueDebt, boolean meditating)
        implements CustomPacketPayload {
    public static final Type<StaminaSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "stamina_sync"));
}
```

```java
registrar.playToClient(StaminaSyncPayload.TYPE, StaminaSyncPayload.CODEC,
        ClientPayloadHandler::handleStaminaSync);
```

```java
public static void syncStamina(ServerPlayer player) {
    StaminaData data = player.getData(ModAttachments.STAMINA);
    PacketDistributor.sendToPlayer(player,
            new StaminaSyncPayload(data.currentStamina(), data.fatigueDebt(), data.meditating()));
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.stamina.StaminaManagerTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/stamina/StaminaManager.java src/main/java/tong/statmod/network/StaminaSyncPayload.java src/main/java/tong/statmod/client/ClientStaminaCache.java src/main/java/tong/statmod/network/NetworkHandler.java src/main/java/tong/statmod/network/ClientPayloadHandler.java src/main/java/tong/statmod/network/SyncHelper.java src/test/java/tong/statmod/stamina/StaminaManagerTest.java
git commit -m "Add stamina manager and client stamina sync"
```

### Task 3: Add Passive Drain, Recovery, and Meditation State

**Files:**
- Create: `src/main/java/tong/statmod/stamina/MeditationManager.java`
- Create: `src/main/java/tong/statmod/stamina/StaminaEvents.java`
- Modify: `src/main/java/tong/statmod/STATMod.java`
- Test: `src/test/java/tong/statmod/stamina/StaminaManagerTest.java`

- [ ] **Step 1: Extend the failing test for passive recovery math and meditation toggles**

```java
@Test
void passiveTickRegeneratesWhenBelowMax() {
    StaminaData data = new StaminaData();
    data.setCurrentStamina(50.0f);

    StaminaManager.tickPassive(data, 10, false, false);
    assertTrue(data.currentStamina() > 50.0f);
}

@Test
void meditationRecoveryBeatsPassiveRecovery() {
    float passive = StaminaRules.passiveRecoveryPerTick(false);
    float meditation = StaminaRules.passiveRecoveryPerTick(true);
    assertTrue(meditation > passive);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.stamina.StaminaManagerTest`  
Expected: FAIL with missing passive recovery methods

- [ ] **Step 3: Write minimal implementation**

```java
public static float passiveRecoveryPerTick(boolean meditating) {
    return meditating ? 0.18f : 0.02f;
}

public static float passiveDrainPerTick(boolean sprinting, boolean airborne) {
    float drain = 0.0025f;
    if (sprinting) drain += 0.08f;
    if (airborne) drain += 0.02f;
    return drain;
}
```

```java
public static void tickPassive(StaminaData data, int enduranceLevel, boolean meditating, boolean sleeping) {
    if (data == null) return;
    if (sleeping) {
        restore(data, 0.35f, enduranceLevel);
        return;
    }
    restore(data, StaminaRules.passiveRecoveryPerTick(meditating), enduranceLevel);
    consumeUnchecked(data, StaminaRules.passiveDrainPerTick(false, false));
}
```

```java
package tong.statmod.stamina;

public final class MeditationManager {
    private MeditationManager() {}

    public static void start(StaminaData data) {
        if (data != null) data.setMeditating(true);
    }

    public static void stop(StaminaData data) {
        if (data != null) data.setMeditating(false);
    }
}
```

```java
NeoForge.EVENT_BUS.register(StaminaEvents.class);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.stamina.StaminaManagerTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/stamina/MeditationManager.java src/main/java/tong/statmod/stamina/StaminaEvents.java src/main/java/tong/statmod/STATMod.java src/test/java/tong/statmod/stamina/StaminaManagerTest.java
git commit -m "Add stamina ticking and meditation state management"
```

### Task 4: Bridge Unified Stamina into Epic Fight

**Files:**
- Create: `src/main/java/tong/statmod/integration/epicfight/EpicFightStaminaBridge.java`
- Modify: `src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java`
- Test: `src/test/java/tong/statmod/integration/epicfight/EpicFightStaminaBridgeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;
import tong.statmod.stamina.StaminaThreshold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightStaminaBridgeTest {
    @Test
    void criticalThresholdBlocksSkillUse() {
        assertFalse(EpicFightStaminaBridge.canUseSkill(StaminaThreshold.CRITICAL, 20.0f, 15.0f));
        assertTrue(EpicFightStaminaBridge.canUseSkill(StaminaThreshold.NORMAL, 20.0f, 15.0f));
    }

    @Test
    void lowThresholdAppliesDamagePenalty() {
        assertEquals(0.9f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.LOW), 0.0001f);
        assertEquals(1.0f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.NORMAL), 0.0001f);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.epicfight.EpicFightStaminaBridgeTest`  
Expected: FAIL with missing `EpicFightStaminaBridge`

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.epicfight;

import tong.statmod.stamina.StaminaThreshold;

public final class EpicFightStaminaBridge {
    private EpicFightStaminaBridge() {}

    public static boolean canUseSkill(StaminaThreshold threshold, float current, float cost) {
        if (threshold == StaminaThreshold.CRITICAL) return false;
        return current >= cost;
    }

    public static float damageMultiplier(StaminaThreshold threshold) {
        if (threshold == StaminaThreshold.LOW) return 0.9f;
        if (threshold == StaminaThreshold.CRITICAL) return 0.8f;
        return 1.0f;
    }
}
```

```java
// inside EpicFightCompat.onConsumeSkill
float cost = event.getAmount();
var stamina = player.getData(ModAttachments.STAMINA);
float max = StaminaRules.maxStamina(RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index));
var threshold = StaminaRules.threshold(stamina.currentStamina(), max);
if (!EpicFightStaminaBridge.canUseSkill(threshold, stamina.currentStamina(), cost)) {
    event.setCanceled(true);
    return;
}
StaminaManager.consume(stamina, cost);
SyncHelper.syncStamina((ServerPlayer) player);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.epicfight.EpicFightStaminaBridgeTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/epicfight/EpicFightStaminaBridge.java src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java src/test/java/tong/statmod/integration/epicfight/EpicFightStaminaBridgeTest.java
git commit -m "Bridge unified stamina into Epic Fight skill usage"
```

### Task 5: Add Epic Fight Skill Requirement Resolver for 5 MVP Skills

**Files:**
- Create: `src/main/java/tong/statmod/integration/epicfight/EpicFightSkillRequirementResolver.java`
- Modify: `src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java`
- Test: `src/test/java/tong/statmod/integration/epicfight/EpicFightSkillRequirementResolverTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpicFightSkillRequirementResolverTest {
    @Test
    void definesExactlyFiveMvpSkills() {
        assertEquals(5, EpicFightSkillRequirementResolver.mvpRequirements().size());
    }

    @Test
    void rushingTempoUsesAgilityAndRapidite() {
        Map<Integer, Integer> requirements = EpicFightSkillRequirementResolver.requirementsFor("rushing_tempo");
        assertEquals(35, requirements.get(StatType.AGILITY.index));
        assertEquals(30, requirements.get(StatType.RAPIDITE.index));
    }

    @Test
    void liechtenauerUsesBladeTechniqueAndPrecision() {
        Map<Integer, Integer> requirements = EpicFightSkillRequirementResolver.requirementsFor("liechtenauer");
        assertEquals(40, requirements.get(StatType.BLADE_TECHNIQUE.index));
        assertEquals(25, requirements.get(StatType.PRECISION.index));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.epicfight.EpicFightSkillRequirementResolverTest`  
Expected: FAIL with missing resolver

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.epicfight;

import tong.statmod.stats.StatType;

import java.util.Map;

public final class EpicFightSkillRequirementResolver {
    private static final Map<String, Map<Integer, Integer>> MVP_REQUIREMENTS = Map.of(
            "berserker", Map.of(StatType.BRUTE_FORCE.index, 40, StatType.PHYSICAL_ENDURANCE.index, 25),
            "liechtenauer", Map.of(StatType.BLADE_TECHNIQUE.index, 40, StatType.PRECISION.index, 25),
            "rushing_tempo", Map.of(StatType.AGILITY.index, 35, StatType.RAPIDITE.index, 30),
            "roll", Map.of(StatType.AGILITY.index, 25, StatType.PHYSICAL_ENDURANCE.index, 15),
            "heartpiercer", Map.of(StatType.PRECISION.index, 40, StatType.AGILITY.index, 20)
    );

    private EpicFightSkillRequirementResolver() {}

    public static Map<String, Map<Integer, Integer>> mvpRequirements() {
        return MVP_REQUIREMENTS;
    }

    public static Map<Integer, Integer> requirementsFor(String skillId) {
        return MVP_REQUIREMENTS.getOrDefault(skillId, Map.of());
    }
}
```

```java
// in EpicFightCompat, add helper usage before unlock grant paths or skill availability hooks
Map<Integer, Integer> requirements = EpicFightSkillRequirementResolver.requirementsFor(skillId);
boolean unlocked = requirements.entrySet().stream()
        .allMatch(entry -> RaceEffectApplier.getEffectiveLevel(player, entry.getKey()) >= entry.getValue());
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.epicfight.EpicFightSkillRequirementResolverTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/epicfight/EpicFightSkillRequirementResolver.java src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java src/test/java/tong/statmod/integration/epicfight/EpicFightSkillRequirementResolverTest.java
git commit -m "Add Epic Fight MVP skill requirement resolver"
```

### Task 6: Add Overworld Time Controller and Sleep Recovery

**Files:**
- Create: `src/main/java/tong/statmod/time/OverworldTimeController.java`
- Create: `src/main/java/tong/statmod/time/SleepRecoveryHandler.java`
- Modify: `src/main/java/tong/statmod/STATMod.java`
- Test: `src/test/java/tong/statmod/time/OverworldTimeControllerTest.java`
- Test: `src/test/java/tong/statmod/time/SleepRecoveryHandlerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package tong.statmod.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverworldTimeControllerTest {
    @Test
    void convertsDayMinutesToVanillaTicksPerRealTick() {
        assertEquals(12.5d, OverworldTimeController.dayTicksPerSecond(), 0.0001d);
        assertEquals(25.0d, OverworldTimeController.nightTicksPerSecond(), 0.0001d);
    }
}
```

```java
package tong.statmod.time;

import org.junit.jupiter.api.Test;
import tong.statmod.stamina.StaminaData;
import tong.statmod.stamina.StaminaRules;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepRecoveryHandlerTest {
    @Test
    void wakeBonusRestoresMoreThanPassiveSleepTick() {
        StaminaData data = new StaminaData();
        data.setCurrentStamina(10.0f);
        SleepRecoveryHandler.applyWakeBonus(data, 50);
        assertTrue(data.currentStamina() > 30.0f);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.time.OverworldTimeControllerTest --tests tong.statmod.time.SleepRecoveryHandlerTest`  
Expected: FAIL with missing time classes

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.time;

public final class OverworldTimeController {
    public static final double DAY_SECONDS = 32.0 * 60.0;
    public static final double NIGHT_SECONDS = 16.0 * 60.0;

    private OverworldTimeController() {}

    public static double dayTicksPerSecond() {
        return 24000.0 / DAY_SECONDS;
    }

    public static double nightTicksPerSecond() {
        return 12000.0 / NIGHT_SECONDS;
    }
}
```

```java
package tong.statmod.time;

import tong.statmod.stamina.StaminaData;
import tong.statmod.stamina.StaminaManager;

public final class SleepRecoveryHandler {
    private SleepRecoveryHandler() {}

    public static void applyWakeBonus(StaminaData data, int enduranceLevel) {
        StaminaManager.restore(data, 35.0f, enduranceLevel);
    }
}
```

```java
NeoForge.EVENT_BUS.register(OverworldTimeController.class);
NeoForge.EVENT_BUS.register(SleepRecoveryHandler.class);
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat test --tests tong.statmod.time.OverworldTimeControllerTest --tests tong.statmod.time.SleepRecoveryHandlerTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/time/OverworldTimeController.java src/main/java/tong/statmod/time/SleepRecoveryHandler.java src/main/java/tong/statmod/STATMod.java src/test/java/tong/statmod/time/OverworldTimeControllerTest.java src/test/java/tong/statmod/time/SleepRecoveryHandlerTest.java
git commit -m "Add Overworld time controller and sleep recovery"
```

### Task 7: Final Integration Pass and Runtime Verification

**Files:**
- Modify: `src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java`
- Modify: `src/main/java/tong/statmod/stamina/StaminaEvents.java`
- Modify: `src/main/java/tong/statmod/time/OverworldTimeController.java`
- Test: `src/test/java/tong/statmod/integration/epicfight/EpicFightHandlersTest.java`

- [ ] **Step 1: Add failing assertions to the existing Epic Fight handler test**

```java
@Test
void lowAndCriticalThresholdsChangeCombatMultipliers() {
    assertEquals(0.9f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.LOW), 0.0001f);
    assertEquals(0.8f, EpicFightStaminaBridge.damageMultiplier(StaminaThreshold.CRITICAL), 0.0001f);
}
```

- [ ] **Step 2: Run the targeted tests**

Run: `.\gradlew.bat test --tests tong.statmod.integration.epicfight.EpicFightHandlersTest --tests tong.statmod.integration.epicfight.EpicFightStaminaBridgeTest --tests tong.statmod.time.OverworldTimeControllerTest`  
Expected: PASS after integration code is complete

- [ ] **Step 3: Run full build**

Run: `.\gradlew.bat build`  
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run runtime verification**

Run: `.\gradlew.bat runClient`  
Expected:
- client launches with Epic Fight loaded
- no `statmod` mixin crash
- no stamina sync crash
- Epic Fight UI still appears
- skill use is blocked at critical stamina
- stamina drains during sprint, dodge, and skill use

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/epicfight/EpicFightCompat.java src/main/java/tong/statmod/stamina/StaminaEvents.java src/main/java/tong/statmod/time/OverworldTimeController.java src/test/java/tong/statmod/integration/epicfight/EpicFightHandlersTest.java
git commit -m "Finish unified stamina Epic Fight MVP integration"
```

---

## Self-Review

### Spec coverage

- unified stamina ownership: Task 1, Task 2
- fatigue and derived thresholds: Task 1, Task 2, Task 3
- meditation seated recovery: Task 3
- Epic Fight stamina bridge: Task 4
- 5-skill Epic Fight MVP with stat unlock rules: Task 5
- Overworld 32/16 cycle: Task 6
- semi-realistic sleep recovery: Task 6
- build and runtime verification: Task 7

### Placeholder scan

- no `TODO`
- no `TBD`
- no cross-task “similar to previous task” placeholders
- every test step contains explicit code and commands

### Type consistency

- stamina source of truth stays in `StaminaData`
- threshold enum is always `StaminaThreshold`
- max stamina is always derived through `StaminaRules.maxStamina(...)`
- Epic Fight bridge always reads through `EpicFightStaminaBridge`
