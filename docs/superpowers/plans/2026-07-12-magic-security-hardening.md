# Magic Security Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close the audited magic exploit paths while retaining legal multi-school progression and a small non-convertible practice reward for casts in the void.

**Architecture:** The server remains authoritative. A separate persisted practice-mastery channel prevents void casts from ever producing Magic Points. Small pure stateful policies track cast impact, combat reward throttling, and virtual-menu request throttling.

**Tech Stack:** Java 21, NeoForge 1.21.1, Iron's Spellbooks, Tensura, JUnit 5, Gradle.

## Global Constraints

- Work only in the fix/magic-security-hardening worktree.
- Preserve access to all schools after a legal race-compatible starting branch.
- Void-cast practice must not contribute to conversion or milestones.
- Every behavior change starts with a failing focused test.
- End with gradlew.bat test and gradlew.bat build.

---

### Task 1: Restore native mana authority and strict wrapper authorization

**Files:**

- Modify: src/main/resources/statmod.mixins.json
- Delete: src/main/java/tong/statmod/mixin/IronSpellManaOverrideMixin.java
- Modify: src/main/java/tong/statmod/integration/ironspells/bridge/TensuraDelegatingSpell.java
- Create: src/test/java/tong/statmod/integration/ironspells/MagicCastSecuritySourceTest.java

**Produces:** Iron's native mana validation is untouched and a Tensura wrapper executes only a skill already present in SkillStorage.

- [ ] **Step 1: Write the failing source-regression tests**

    @Test
    void mixinConfigurationDoesNotRegisterManaBypass() throws IOException {
        String config = Files.readString(Path.of("src/main/resources/statmod.mixins.json"));
        assertFalse(config.contains("IronSpellManaOverrideMixin"));
    }

    @Test
    void tensuraWrapperNeverLearnsDuringCast() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/integration/ironspells/bridge/TensuraDelegatingSpell.java"));
        assertFalse(source.contains("storage.learnSkill(canonical)"));
        assertTrue(source.contains("storage.getSkill(canonical)"));
    }

- [ ] **Step 2: Verify RED**

Run: .\\gradlew.bat test --tests tong.statmod.integration.ironspells.MagicCastSecuritySourceTest

Expected: FAIL because the mana bypass is registered and the wrapper learns missing skills.

- [ ] **Step 3: Implement the minimal fix**

Remove IronSpellManaOverrideMixin from statmod.mixins.json and delete its source file. Replace the wrapper's missing-skill behavior with:

    Optional<ManasSkillInstance> resolved = storage.getSkill(canonical);
    if (resolved.isPresent()) {
        action.accept(storage, resolved.get());
    } else {
        LOGGER.warn("TensuraDelegatingSpell {} unavailable for {}: skill was not granted",
                tensuraSkillId, caster.getName().getString());
    }

- [ ] **Step 4: Verify GREEN**

Run: .\\gradlew.bat test --tests tong.statmod.integration.ironspells.MagicCastSecuritySourceTest

Expected: PASS.

- [ ] **Step 5: Commit**

    git add src/main/resources/statmod.mixins.json src/main/java/tong/statmod/mixin/IronSpellManaOverrideMixin.java src/main/java/tong/statmod/integration/ironspells/bridge/TensuraDelegatingSpell.java src/test/java/tong/statmod/integration/ironspells/MagicCastSecuritySourceTest.java
    git commit -m "fix: restore magic cast authorization"

### Task 2: Isolate void-cast practice mastery

**Files:**

- Modify: src/main/java/tong/statmod/magic/CastRewardPolicy.java
- Modify: src/main/java/tong/statmod/magic/SchoolProgressTracker.java
- Modify: src/main/java/tong/statmod/storage/PlayerStatData.java
- Modify: src/main/java/tong/statmod/storage/MagicStateSerializer.java
- Modify: src/main/java/tong/statmod/network/SyncMagicPayload.java
- Modify: src/main/java/tong/statmod/network/MagicStateSyncService.java
- Modify: src/main/java/tong/statmod/network/ClientPayloadActions.java
- Modify: src/main/java/tong/statmod/client/ClientMagicCache.java
- Modify: src/main/java/tong/statmod/client/codex/MageCodexScreen.java
- Modify: src/test/java/tong/statmod/magic/CastRewardPolicyTest.java
- Modify: src/test/java/tong/statmod/magic/SchoolProgressTrackerTest.java
- Modify: src/test/java/tong/statmod/storage/PlayerStatDataMagicTest.java
- Modify: src/test/java/tong/statmod/storage/ModAttachmentsMagicSerializationTest.java
- Modify: src/test/java/tong/statmod/network/MagicStateSyncServiceTest.java

**Interfaces:**

- Reward becomes Reward(int practiceMasteryDelta, int progressionMasteryDelta, int magicPointsDelta).
- applyPracticeMastery(PlayerStatData, MagicBranch, int) persists practice and always returns zero.
- SyncMagicPayload gains int[] practiceMasteryProgress.

- [ ] **Step 1: Write failing policy, storage, serializer, and sync tests**

    @Test
    void qualifyingVoidCastYieldsPracticeOnly() {
        var reward = CastRewardPolicy.evaluate(new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE, 0.25, false, false, 1));
        assertEquals(1, reward.practiceMasteryDelta());
        assertEquals(0, reward.progressionMasteryDelta());
        assertEquals(0, reward.magicPointsDelta());
    }

    @Test
    void practiceMasteryNeverConvertsToMagicPoints() {
        PlayerStatData data = new PlayerStatData();
        assertEquals(0, SchoolProgressTracker.applyPracticeMastery(data, MagicBranch.FIRE, 500));
        assertEquals(0, data.getMagicPoints());
        assertEquals(500, data.getSchoolPracticeMasteryProgress(MagicBranch.FIRE));
    }

- [ ] **Step 2: Verify RED**

Run: .\\gradlew.bat test --tests tong.statmod.magic.CastRewardPolicyTest --tests tong.statmod.magic.SchoolProgressTrackerTest --tests tong.statmod.storage.PlayerStatDataMagicTest --tests tong.statmod.network.MagicStateSyncServiceTest

Expected: FAIL because practice APIs and the payload field do not exist.

- [ ] **Step 3: Implement a persisted practice channel**

Add an independent schoolPracticeMasteryProgress array to PlayerStatData; copy and serialize it as schoolPracticeMastery. Add a practice array to SyncMagicPayload and its codec. Update the client cache and Codex display to show total mastery, but calculate the next conversion milestone from bankable mastery only.

Implement the reward paths:

    // qualifying void cast
    return new Reward(1, 0, 0);

    // qualifying impact cast
    return new Reward(0, IMPACT_MASTERY + Math.max(0, ctx.spellLevel() - 1), 1);

    public static int applyPracticeMastery(PlayerStatData data, MagicBranch branch, int amount) {
        if (data == null || branch == null || amount <= 0) return 0;
        data.addSchoolPracticeMasteryProgress(branch, amount);
        return 0;
    }

- [ ] **Step 4: Verify GREEN**

Run: .\\gradlew.bat test --tests tong.statmod.magic.CastRewardPolicyTest --tests tong.statmod.magic.SchoolProgressTrackerTest --tests tong.statmod.storage.PlayerStatDataMagicTest --tests tong.statmod.storage.ModAttachmentsMagicSerializationTest --tests tong.statmod.network.MagicStateSyncServiceTest

Expected: PASS.

- [ ] **Step 5: Commit**

    git add src/main/java/tong/statmod/magic src/main/java/tong/statmod/storage src/main/java/tong/statmod/network src/main/java/tong/statmod/client src/test/java/tong/statmod/magic src/test/java/tong/statmod/storage src/test/java/tong/statmod/network
    git commit -m "fix: isolate void-cast practice mastery"

### Task 3: Track verified spell impact

**Files:**

- Create: src/main/java/tong/statmod/integration/ironspells/CastImpactTracker.java
- Modify: src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java
- Create: src/test/java/tong/statmod/integration/ironspells/CastImpactTrackerTest.java
- Modify: src/test/java/tong/statmod/integration/ironspells/IronSpellEventBridgeLogicTest.java

**Interfaces:** begin(UUID, String, long), markImpact(UUID, String), consume(UUID, String, long), and clear(UUID).

- [ ] **Step 1: Write failing tracker tests**

    @Test
    void markedCastConsumesAsImpactful() {
        UUID player = UUID.randomUUID();
        tracker.begin(player, "irons_spellbooks:fireball", 100L);
        tracker.markImpact(player, "irons_spellbooks:fireball");
        assertTrue(tracker.consume(player, "irons_spellbooks:fireball", 101L));
    }

    @Test
    void untouchedCastConsumesAsVoidPractice() {
        UUID player = UUID.randomUUID();
        tracker.begin(player, "irons_spellbooks:fireball", 100L);
        assertFalse(tracker.consume(player, "irons_spellbooks:fireball", 101L));
    }

- [ ] **Step 2: Verify RED**

Run: .\\gradlew.bat test --tests tong.statmod.integration.ironspells.CastImpactTrackerTest

Expected: FAIL because CastImpactTracker does not exist.

- [ ] **Step 3: Implement and wire the tracker**

Begin tracking only after server pre-cast accepts the learned spell and current mana. Mark a matching cast on positive SpellDamageEvent damage. Schedule post-cast reward evaluation on the server executor, consume the tracker result, apply practice and bankable mastery separately, and sync when either changes. Clear tracker state on logout, clone, and respawn.

- [ ] **Step 4: Verify GREEN**

Run: .\\gradlew.bat test --tests tong.statmod.integration.ironspells.CastImpactTrackerTest --tests tong.statmod.integration.ironspells.IronSpellEventBridgeLogicTest --tests tong.statmod.magic.CastRewardPolicyTest

Expected: PASS.

- [ ] **Step 5: Commit**

    git add src/main/java/tong/statmod/integration/ironspells/CastImpactTracker.java src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java src/test/java/tong/statmod/integration/ironspells/CastImpactTrackerTest.java src/test/java/tong/statmod/integration/ironspells/IronSpellEventBridgeLogicTest.java
    git commit -m "fix: require spell impact for magic point rewards"

### Task 4: Throttle Magic Point rewards from combat kills

**Files:**

- Create: src/main/java/tong/statmod/progression/MagicKillRewardThrottle.java
- Modify: src/main/java/tong/statmod/progression/CombatXPHandler.java
- Create: src/test/java/tong/statmod/progression/MagicKillRewardThrottleTest.java
- Modify: src/test/java/tong/statmod/progression/CombatXPHandlerTest.java

**Interfaces:** tryAcquire(UUID, ResourceLocation, long) permits one reward per player and entity type every 200 ticks; clear(UUID) removes lifecycle state.

- [ ] **Step 1: Write a failing throttle test**

    @Test
    void repeatedSameEntityTypeIsRejectedInsideWindow() {
        UUID player = UUID.randomUUID();
        assertTrue(throttle.tryAcquire(player, ResourceLocation.withDefaultNamespace("zombie"), 100L));
        assertFalse(throttle.tryAcquire(player, ResourceLocation.withDefaultNamespace("zombie"), 299L));
        assertTrue(throttle.tryAcquire(player, ResourceLocation.withDefaultNamespace("zombie"), 300L));
    }

- [ ] **Step 2: Verify RED**

Run: .\\gradlew.bat test --tests tong.statmod.progression.MagicKillRewardThrottleTest

Expected: FAIL because the throttle does not exist.

- [ ] **Step 3: Implement hostile-only throttling**

Use a Map<UUID, Map<ResourceLocation, Long>>. Grant the combat Magic Point only when target is an Enemy, was threatening the player, and the throttle accepts BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()) at current game time. Clear state on logout, clone, and respawn.

- [ ] **Step 4: Verify GREEN**

Run: .\\gradlew.bat test --tests tong.statmod.progression.MagicKillRewardThrottleTest --tests tong.statmod.progression.CombatXPHandlerTest

Expected: PASS.

- [ ] **Step 5: Commit**

    git add src/main/java/tong/statmod/progression/MagicKillRewardThrottle.java src/main/java/tong/statmod/progression/CombatXPHandler.java src/test/java/tong/statmod/progression/MagicKillRewardThrottleTest.java src/test/java/tong/statmod/progression/CombatXPHandlerTest.java
    git commit -m "fix: throttle combat magic point rewards"

### Task 5: Rate-limit virtual inscription and fix the T4 cap

**Files:**

- Create: src/main/java/tong/statmod/network/VirtualInscriptionRequestThrottle.java
- Modify: src/main/java/tong/statmod/network/ServerPayloadHandler.java
- Modify: src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java
- Create: src/test/java/tong/statmod/network/VirtualInscriptionRequestThrottleTest.java
- Modify: src/test/java/tong/statmod/integration/ironspells/IronSpellEventBridgeLogicTest.java

**Interfaces:** tryAcquire(UUID, long) permits one request every 10 ticks; maxSpellLevelForTier(int) maps 1 to 2, 2 to 4, 3 to 6, 4 to 8, and any other value to 1.

- [ ] **Step 1: Write failing tests**

    @Test
    void virtualInscriptionAcceptsOnlyOneRequestPerTenTicks() {
        UUID player = UUID.randomUUID();
        assertTrue(throttle.tryAcquire(player, 100L));
        assertFalse(throttle.tryAcquire(player, 109L));
        assertTrue(throttle.tryAcquire(player, 110L));
    }

    @Test
    void tierFourDoesNotReduceSpellLevelCap() {
        assertEquals(8, IronSpellEventBridge.maxSpellLevelForTier(4));
    }

- [ ] **Step 2: Verify RED**

Run: .\\gradlew.bat test --tests tong.statmod.network.VirtualInscriptionRequestThrottleTest --tests tong.statmod.integration.ironspells.IronSpellEventBridgeLogicTest

Expected: FAIL because the throttle and cap helper are absent.

- [ ] **Step 3: Implement the guards**

Gate IronInscriptionOpenerService.openVirtual(player) behind a 10-tick request throttle in handleOpenVirtualInscription. Make clampSpellLevel delegate to a package-visible maxSpellLevelForTier switch and include the T4-to-8 case. Clear request state in existing player lifecycle handlers.

- [ ] **Step 4: Verify GREEN**

Run: .\\gradlew.bat test --tests tong.statmod.network.VirtualInscriptionRequestThrottleTest --tests tong.statmod.integration.ironspells.IronSpellEventBridgeLogicTest

Expected: PASS.

- [ ] **Step 5: Commit**

    git add src/main/java/tong/statmod/network/VirtualInscriptionRequestThrottle.java src/main/java/tong/statmod/network/ServerPayloadHandler.java src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java src/test/java/tong/statmod/network/VirtualInscriptionRequestThrottleTest.java src/test/java/tong/statmod/integration/ironspells/IronSpellEventBridgeLogicTest.java
    git commit -m "fix: throttle virtual inscription requests"

### Task 6: Verify the integrated result

- [ ] **Step 1: Run the full suite**

Run: .\\gradlew.bat test

Expected: BUILD SUCCESSFUL with no test failures.

- [ ] **Step 2: Build the distributable JAR**

Run: .\\gradlew.bat build

Expected: BUILD SUCCESSFUL and a JAR under build/libs.

- [ ] **Step 3: Inspect the final branch state**

Run: git status --short; git log --oneline -6

Expected: only intended magic-security changes.

