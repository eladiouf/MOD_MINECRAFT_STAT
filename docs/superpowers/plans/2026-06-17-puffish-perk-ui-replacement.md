# Puffish Perk UI Replacement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the custom `STAT Mod` perk screen with `Puffish Skills` while keeping `STAT Mod` as the only source of truth for perk unlocks, perk points, and perk rewards.

**Architecture:** Add a reflection-based Puffish compat layer because `puffish_skills` is not present in `libs` and must remain optional at compile time. Drive the tree from deterministic `Perk` mappings, mirror canonical perk state into Puffish on every existing sync path, and move perk screen opening onto a small client-to-server payload that asks the server to sync then open the native Puffish UI.

**Tech Stack:** NeoForge 1.21.1, Java 21, NeoForge payloads, optional-mod reflection, JUnit 5, static JSON resources for Puffish categories

---

## File Structure

### New units

- `src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java`
  - deterministic mapping between `Perk` and Puffish category/skill ids
- `src/main/java/tong/statmod/integration/puffish/PuffishMirrorGateway.java`
  - small interface for mirror operations, testable without Puffish runtime
- `src/main/java/tong/statmod/integration/puffish/PuffishReflectionGateway.java`
  - reflection adapter around `net.puffish.skillsmod.api.SkillsAPI`, `Category`, and `Skill`
- `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java`
  - project canonical `STAT Mod` perk state into Puffish mirror state
- `src/main/java/tong/statmod/integration/puffish/PuffishUnlockService.java`
  - handle Puffish unlock/lock callbacks through `PerkManager`
- `src/main/java/tong/statmod/integration/puffish/PuffishSkillsCompat.java`
  - optional-mod bootstrap, event registration, sync/open façade
- `src/main/java/tong/statmod/network/OpenPerkTreePayload.java`
  - client request asking the server to sync and open the Puffish tree
- `src/main/java/tong/statmod/client/PerkUiRouter.java`
  - central client routing for `OPEN_PERKS` keybind and stats-screen button

### New tests

- `src/test/java/tong/statmod/integration/puffish/PuffishPerkIdsTest.java`
- `src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java`
- `src/test/java/tong/statmod/integration/puffish/PuffishUnlockServiceTest.java`
- `src/test/java/tong/statmod/client/PerkUiRouterTest.java`

### Existing files to modify

- `src/main/java/tong/statmod/STATMod.java`
  - initialize Puffish compat
- `src/main/java/tong/statmod/network/NetworkHandler.java`
  - register `OpenPerkTreePayload`
- `src/main/java/tong/statmod/network/ServerPayloadHandler.java`
  - handle open-tree requests
- `src/main/java/tong/statmod/network/SyncHelper.java`
  - call Puffish mirror sync from canonical perk syncs
- `src/main/java/tong/statmod/client/ClientInputHandler.java`
  - route perk keybind through `PerkUiRouter`
- `src/main/java/tong/statmod/client/StatTabScreen.java`
  - route perk button through `PerkUiRouter`

### New resources

- `src/main/resources/data/puffish_skills/puffish_skills/config.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/<category>/category.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/<category>/definitions.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/<category>/skills.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/<category>/connections.json`

---

### Task 1: Add deterministic Puffish ids for every perk

**Files:**
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java`
- Test: `src/test/java/tong/statmod/integration/puffish/PuffishPerkIdsTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PuffishPerkIdsTest {
    @Test
    void mapsPerkToDeterministicCategoryAndSkillIds() {
        assertEquals("statmod:blade_technique", PuffishPerkIds.categoryId(Perk.BLADE_CORE));
        assertEquals("blade_core", PuffishPerkIds.skillId(Perk.BLADE_CORE));
        assertEquals("statmod:physical_endurance", PuffishPerkIds.categoryId(Perk.ENDUR_TRANSCENDENCE));
    }

    @Test
    void resolvesPerkBackFromCategoryAndSkill() {
        assertSame(Perk.BRUTE_CORE, PuffishPerkIds.resolve("statmod:brute_force", "brute_core"));
        assertSame(Perk.WILL_TRANSCENDENCE, PuffishPerkIds.resolve("statmod:willpower", "will_transcendence"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishPerkIdsTest`  
Expected: FAIL with missing `PuffishPerkIds`

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PuffishPerkIds {
    private static final Map<String, Perk> BY_PATH = new HashMap<>();

    static {
        for (Perk perk : Perk.values()) {
            BY_PATH.put(categoryId(perk) + "|" + skillId(perk), perk);
        }
    }

    private PuffishPerkIds() {}

    public static String categoryId(Perk perk) {
        return "statmod:" + perk.stat.name().toLowerCase(Locale.ROOT);
    }

    public static String skillId(Perk perk) {
        return perk.name().toLowerCase(Locale.ROOT);
    }

    public static Perk resolve(String categoryId, String skillId) {
        if (categoryId == null || skillId == null) {
            return null;
        }
        return BY_PATH.get(categoryId + "|" + skillId);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishPerkIdsTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java src/test/java/tong/statmod/integration/puffish/PuffishPerkIdsTest.java
git commit -m "Add Puffish perk id mapping"
```

### Task 2: Add a reflection-safe mirror sync service

**Files:**
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishMirrorGateway.java`
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishReflectionGateway.java`
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java`
- Modify: `src/main/java/tong/statmod/network/SyncHelper.java`
- Test: `src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishSyncServiceTest {
    @Test
    void mirrorsUnlockedPerksAndPerStatPoints() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 3);
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("unlock:statmod:brute_force:brute_core"));
        assertTrue(gateway.operations.contains("points:statmod:brute_force:3"));
    }

    static final class FakeGateway implements PuffishMirrorGateway {
        final List<String> operations = new ArrayList<>();
        public void ensureCategoryUnlocked(String categoryId) { operations.add("category:" + categoryId); }
        public void setPoints(String categoryId, int points) { operations.add("points:" + categoryId + ":" + points); }
        public void unlock(String categoryId, String skillId) { operations.add("unlock:" + categoryId + ":" + skillId); }
        public void lock(String categoryId, String skillId) { operations.add("lock:" + categoryId + ":" + skillId); }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishSyncServiceTest`  
Expected: FAIL with missing gateway and sync service

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.puffish;

public interface PuffishMirrorGateway {
    void ensureCategoryUnlocked(String categoryId);
    void setPoints(String categoryId, int points);
    void unlock(String categoryId, String skillId);
    void lock(String categoryId, String skillId);
}
```

```java
package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

public final class PuffishSyncService {
    private PuffishSyncService() {}

    public static void sync(PlayerStatData data, PuffishMirrorGateway gateway) {
        for (Perk perk : Perk.values()) {
            String categoryId = PuffishPerkIds.categoryId(perk);
            gateway.ensureCategoryUnlocked(categoryId);
            gateway.setPoints(categoryId, data.getPerkPointsForStat(perk.stat.index));
            if (data.isPerkUnlocked(perk.id)) {
                gateway.unlock(categoryId, PuffishPerkIds.skillId(perk));
            } else {
                gateway.lock(categoryId, PuffishPerkIds.skillId(perk));
            }
        }
    }
}
```

```java
public static void syncPerks(ServerPlayer player) {
    PlayerStatData data = player.getData(ModAttachments.STATS);
    PacketDistributor.sendToPlayer(player,
            new SyncPerksPayload(data.getUnlockedPerks(), data.getPerkPoints()));
    PuffishSkillsCompat.sync(player, data);
}
```

- [ ] **Step 4: Flesh out the reflection adapter**

```java
package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;

import java.lang.reflect.Method;

public final class PuffishReflectionGateway implements PuffishMirrorGateway {
    private final ServerPlayer player;
    // cache Methods for SkillsAPI.getCategory, Category.unlock, Category.setExtraPoints, Skill.unlock, Skill.lock

    public PuffishReflectionGateway(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void ensureCategoryUnlocked(String categoryId) { /* reflective category lookup + unlock */ }

    @Override
    public void setPoints(String categoryId, int points) { /* reflective setExtraPoints or setPoints */ }

    @Override
    public void unlock(String categoryId, String skillId) { /* category.getSkill(skillId) + unlock(player) */ }

    @Override
    public void lock(String categoryId, String skillId) { /* category.getSkill(skillId) + lock(player) */ }
}
```

- [ ] **Step 5: Run tests**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishSyncServiceTest`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishMirrorGateway.java src/main/java/tong/statmod/integration/puffish/PuffishReflectionGateway.java src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java src/main/java/tong/statmod/network/SyncHelper.java src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java
git commit -m "Add Puffish perk mirror sync"
```

### Task 3: Bridge Puffish unlock callbacks through PerkManager

**Files:**
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishUnlockService.java`
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishSkillsCompat.java`
- Modify: `src/main/java/tong/statmod/STATMod.java`
- Test: `src/test/java/tong/statmod/integration/puffish/PuffishUnlockServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishUnlockServiceTest {
    @Test
    void unlockConsumesCanonicalPointsWhenValid() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(Perk.BRUTE_CORE.stat.index, 10);
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 1);

        assertTrue(PuffishUnlockService.tryUnlock(data, Perk.BRUTE_CORE, null));
        assertTrue(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
        assertEquals(0, data.getPerkPointsForStat(Perk.BRUTE_CORE.stat.index));
    }

    @Test
    void unlockDoesNotMutateCanonicalStateWhenInvalid() {
        PlayerStatData data = new PlayerStatData();

        assertFalse(PuffishUnlockService.tryUnlock(data, Perk.BRUTE_CORE, null));
        assertFalse(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishUnlockServiceTest`  
Expected: FAIL with missing unlock service

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.integration.puffish;

import net.minecraft.world.entity.player.Player;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.storage.PlayerStatData;

public final class PuffishUnlockService {
    private PuffishUnlockService() {}

    public static boolean tryUnlock(PlayerStatData data, Perk perk, Player player) {
        return new PerkManager(data).unlock(perk, player);
    }
}
```

```java
package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.storage.PlayerStatData;

public final class PuffishSkillsCompat {
    private static boolean loaded;

    private PuffishSkillsCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("puffish_skills");
        if (!loaded) {
            STATMod.LOGGER.info("Puffish Skills not detected, skipping PuffishSkillsCompat");
            return;
        }
        // reflectively register unlock and lock callbacks
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void openScreen(ServerPlayer player) {
        if (!loaded) return;
        // reflective call to SkillsAPI.openScreen(player)
    }

    public static void sync(ServerPlayer player, PlayerStatData data) {
        if (!loaded) return;
        PuffishSyncService.sync(data, new PuffishReflectionGateway(player));
    }
}
```

- [ ] **Step 4: Register reflective event callbacks**

```java
// In PuffishSkillsCompat.init()
// SkillsAPI.registerSkillUnlockEvent(proxy)
// proxy method:
//   Perk perk = PuffishPerkIds.resolve(categoryId.toString(), skillId);
//   if (perk == null) return;
//   PlayerStatData data = player.getData(ModAttachments.STATS);
//   PuffishUnlockService.tryUnlock(data, perk, player);
//   SyncHelper.syncPerks(player);
//
// Register the lock event too, but only resync canonical state:
//   SyncHelper.syncPerks(player);
```

- [ ] **Step 5: Run tests**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishUnlockServiceTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest --tests tong.statmod.integration.puffish.PuffishPerkIdsTest`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishUnlockService.java src/main/java/tong/statmod/integration/puffish/PuffishSkillsCompat.java src/main/java/tong/statmod/STATMod.java src/test/java/tong/statmod/integration/puffish/PuffishUnlockServiceTest.java
git commit -m "Bridge Puffish unlock events into canonical perks"
```

### Task 4: Route perk opening through the server-owned Puffish UI flow

**Files:**
- Create: `src/main/java/tong/statmod/network/OpenPerkTreePayload.java`
- Create: `src/main/java/tong/statmod/client/PerkUiRouter.java`
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Modify: `src/main/java/tong/statmod/network/ServerPayloadHandler.java`
- Modify: `src/main/java/tong/statmod/client/ClientInputHandler.java`
- Modify: `src/main/java/tong/statmod/client/StatTabScreen.java`
- Test: `src/test/java/tong/statmod/client/PerkUiRouterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkUiRouterTest {
    @Test
    void usesPuffishWhenCompatReportsLoaded() {
        assertTrue(PerkUiRouter.shouldUsePuffish(true));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.client.PerkUiRouterTest`  
Expected: FAIL with missing `PerkUiRouter`

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.client;

import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.network.OpenPerkTreePayload;

public final class PerkUiRouter {
    private PerkUiRouter() {}

    public static boolean shouldUsePuffish(boolean puffishLoaded) {
        return puffishLoaded;
    }

    public static void openFromClient(boolean puffishLoaded) {
        if (shouldUsePuffish(puffishLoaded)) {
            PacketDistributor.sendToServer(new OpenPerkTreePayload());
            return;
        }
        net.minecraft.client.Minecraft.getInstance().setScreen(new tong.statmod.client.gui.PerkScreen());
    }
}
```

```java
public record OpenPerkTreePayload() implements CustomPacketPayload {
    public static final Type<OpenPerkTreePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_perk_tree"));
    public static final StreamCodec<ByteBuf, OpenPerkTreePayload> CODEC =
            StreamCodec.unit(new OpenPerkTreePayload());
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 4: Wire client and server handlers**

```java
// ClientInputHandler
while (ClientSetup.OPEN_PERKS.consumeClick()) {
    PerkUiRouter.openFromClient(PuffishSkillsCompat.isLoaded());
}

// StatTabScreen
PerkUiRouter.openFromClient(PuffishSkillsCompat.isLoaded());

// NetworkHandler
registrar.playToServer(OpenPerkTreePayload.TYPE, OpenPerkTreePayload.CODEC,
        ServerPayloadHandler::handleOpenPerkTree);

// ServerPayloadHandler
public static void handleOpenPerkTree(OpenPerkTreePayload payload, IPayloadContext context) {
    context.enqueueWork(() -> {
        if (!(context.player() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PuffishSkillsCompat.sync(player, data);
        PuffishSkillsCompat.openScreen(player);
    });
}
```

- [ ] **Step 5: Run tests**

Run: `.\gradlew.bat test --tests tong.statmod.client.PerkUiRouterTest --tests tong.statmod.integration.puffish.PuffishUnlockServiceTest`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/network/OpenPerkTreePayload.java src/main/java/tong/statmod/client/PerkUiRouter.java src/main/java/tong/statmod/network/NetworkHandler.java src/main/java/tong/statmod/network/ServerPayloadHandler.java src/main/java/tong/statmod/client/ClientInputHandler.java src/main/java/tong/statmod/client/StatTabScreen.java src/test/java/tong/statmod/client/PerkUiRouterTest.java
git commit -m "Route perk UI opening through Puffish"
```

### Task 5: Add Puffish tree resources for every STAT Mod perk category

**Files:**
- Create: `src/main/resources/data/puffish_skills/puffish_skills/config.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/brute_force/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/brute_force/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/brute_force/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/brute_force/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/blade_technique/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/blade_technique/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/blade_technique/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/blade_technique/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/rapidite/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/rapidite/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/rapidite/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/rapidite/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/agility/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/agility/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/agility/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/agility/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_resistance/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_resistance/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_resistance/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_resistance/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_endurance/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_endurance/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_endurance/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/physical_endurance/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/precision/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/precision/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/precision/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/precision/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/tracking/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/tracking/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/tracking/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/tracking/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/keen_senses/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/keen_senses/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/keen_senses/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/keen_senses/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/forging/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/forging/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/forging/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/forging/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/cooking/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/cooking/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/cooking/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/cooking/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/alchemy/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/alchemy/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/alchemy/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/alchemy/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/intimidation/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/intimidation/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/intimidation/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/intimidation/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/willpower/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/willpower/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/willpower/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/willpower/connections.json`

- [ ] **Step 1: Add the root Puffish config**

```json
{
  "version": 3,
  "categories": [
    "brute_force",
    "blade_technique",
    "rapidite",
    "agility",
    "physical_resistance",
    "physical_endurance",
    "precision",
    "tracking",
    "keen_senses",
    "forging",
    "cooking",
    "alchemy",
    "intimidation",
    "willpower"
  ]
}
```

- [ ] **Step 2: Add one canonical category template and then expand it to all stat categories**

```json
// category.json
{
  "unlocked_by_default": true,
  "title": "Brute Force",
  "icon": {
    "type": "item",
    "data": {
      "item": "minecraft:iron_axe"
    }
  },
  "background": "textures/gui/advancements/backgrounds/adventure.png"
}
```

```json
// definitions.json
{
  "brute_core": {
    "title": "Heavy Hitter",
    "description": "+5% damage with axes and clubs"
  },
  "brute_active": {
    "title": "Mighty Swing",
    "description": "Charged attack deals +10% damage"
  },
  "brute_synergy": {
    "title": "Crushing Force",
    "description": "Synergy: +15% damage when below 50% HP"
  },
  "brute_situational": {
    "title": "Berserker",
    "description": "+20% damage when below 30% HP"
  },
  "brute_mastery": {
    "title": "Colossus",
    "description": "Attacks stun targets below 50% HP"
  },
  "brute_transcendence": {
    "title": "Titan's Wrath",
    "description": "+50% damage, enemies explode on kill"
  }
}
```

```json
// skills.json
{
  "0_0": { "x": 0, "y": 0, "definition": "brute_core", "root": true },
  "0_1": { "x": 0, "y": 64, "definition": "brute_active" },
  "0_2": { "x": 0, "y": 128, "definition": "brute_synergy" },
  "0_3": { "x": 0, "y": 192, "definition": "brute_situational" },
  "0_4": { "x": 0, "y": 256, "definition": "brute_mastery" },
  "0_5": { "x": 0, "y": 320, "definition": "brute_transcendence" }
}
```

```json
// connections.json
{
  "normal": {
    "bidirectional": [
      ["0_0", "0_1"],
      ["0_1", "0_2"],
      ["0_2", "0_3"],
      ["0_3", "0_4"],
      ["0_4", "0_5"]
    ]
  }
}
```

- [ ] **Step 3: Expand the same pattern to every remaining stat category**

Use the exact same linear six-node shape for each category, substituting:

- the category folder name from `PuffishPerkIds.categoryId(perk)`
- the icon item per stat
- the six `Perk` names and descriptions belonging to that stat

Keep ids equal to the lowercase enum names so the runtime bridge and resource tree stay 1:1.

- [ ] **Step 4: Build to verify resources package cleanly**

Run: `.\gradlew.bat build`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/data/puffish_skills/puffish_skills
git commit -m "Add Puffish skill tree resources for stat perks"
```

### Task 6: Full verification and runtime proof

**Files:**
- Modify: `docs/superpowers/validation/2026-06-17-multi-mod-live-validation-checklist.md` if new Puffish-specific checks need to be recorded

- [ ] **Step 1: Run the focused Puffish test suite**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishPerkIdsTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest --tests tong.statmod.integration.puffish.PuffishUnlockServiceTest --tests tong.statmod.client.PerkUiRouterTest`  
Expected: PASS

- [ ] **Step 2: Run the full test suite**

Run: `.\gradlew.bat test`  
Expected: PASS

- [ ] **Step 3: Run the full build**

Run: `.\gradlew.bat build`  
Expected: PASS

- [ ] **Step 4: Run the client and verify the new flow**

Run: `.\gradlew.bat runClient`  
Manual checks:
- Press `P` and confirm the Puffish tree opens
- Open stats screen, click the perk button, confirm the Puffish tree opens
- Confirm an already unlocked perk appears unlocked in Puffish
- Click a valid node and confirm `STAT Mod` points decrease once
- Click an invalid node and confirm the node snaps back after sync
- Confirm free-granted race perks appear mirrored correctly

- [ ] **Step 5: Commit the verification result**

```bash
git add docs/superpowers/validation/2026-06-17-multi-mod-live-validation-checklist.md
git commit -m "Document Puffish perk ui verification"
```

---

## Self-Review

### Spec coverage

- UI replacement entrypoints: covered by Task 4
- canonical perk ownership and points: covered by Tasks 2 and 3
- 1:1 perk-to-skill mapping: covered by Tasks 1 and 5
- free-granted and respec sync: covered by Tasks 2 and 3 via canonical sync hooks
- Puffish resources for all perk stats: covered by Task 5
- tests, build, and runClient: covered by Task 6

### Placeholder scan

- No `TODO` or `TBD` markers remain
- File paths are explicit
- Commands are explicit
- Resource pattern is fully defined

### Type consistency

- `PuffishPerkIds` is the only mapping source used by resources and runtime bridge
- `PuffishMirrorGateway` is the test seam used by `PuffishSyncService`
- `OpenPerkTreePayload` is the only client request used to open Puffish from the server
