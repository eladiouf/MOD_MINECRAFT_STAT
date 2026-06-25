# Unified Puffish Perk Category Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the multiple Puffish perk-family tabs with one unified `statmod_perks` category while keeping `STAT Mod` authoritative for perk ownership, family-specific validation, and real perk point spending.

**Architecture:** The existing Puffish perk bridge stays in place, but all perk nodes resolve to one category id. `PuffishSyncService` mirrors one aggregate UI point pool equal to the sum of all available family points, while unlock validation still delegates to the existing `PerkManager` and `PlayerStatData` rules. Resource generation changes from one category per `StatFamily` to one hub-and-branches category that still groups perks visually by family inside a single canvas.

**Tech Stack:** NeoForge 1.21.1, Java 21, Puffish Skills compat layer, JUnit Jupiter 5.10, static JSON resources under `src/main/resources/data/statmod/puffish_skills`.

---

## File Structure

### Existing files to modify

- `src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java`  
  Single source for perk category id / skill id mapping and reverse lookup.

- `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java`  
  Mirrors canonical perk state into Puffish and must switch from per-family category initialization to one unified category initialization.

- `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilder.java`  
  Currently generates one category per family. It needs to generate one combined `statmod_perks` category with family regions.

- `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeExporter.java`  
  Must export the unified category folder instead of one folder per family.

- `src/main/resources/data/statmod/puffish_skills/config.json`  
  Active Puffish category list. Remove family tabs from config and add one unified perk category.

- `src/test/java/tong/statmod/integration/puffish/PuffishPerkIdsTest.java`  
  Update category expectations to `statmod:statmod_perks`.

- `src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java`  
  Update sync expectations to one category and aggregate total points.

- `src/test/java/tong/statmod/integration/puffish/PuffishFamilyResourceConfigTest.java`  
  Update config expectations from family tabs to one unified category.

### New files to create

- `src/test/java/tong/statmod/integration/puffish/PuffishUnifiedPerkCategoryResourceTest.java`  
  Confirms the checked-in unified category resources exist and contain key family sections.

- `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/category.json`
- `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/definitions.json`
- `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/skills.json`
- `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/connections.json`

### Files intentionally not changed

- `src/main/java/tong/statmod/perks/PerkManager.java`
- `src/main/java/tong/statmod/storage/PlayerStatData.java`
- magic Puffish categories under `src/main/resources/data/statmod/puffish_skills/categories/statmod_magic_*`

These remain authoritative or unrelated to the unified perk-category migration.

---

### Task 1: Lock the new category contract in tests

**Files:**
- Modify: `src/test/java/tong/statmod/integration/puffish/PuffishPerkIdsTest.java`
- Modify: `src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java`
- Modify: `src/test/java/tong/statmod/integration/puffish/PuffishFamilyResourceConfigTest.java`
- Create: `src/test/java/tong/statmod/integration/puffish/PuffishUnifiedPerkCategoryResourceTest.java`

- [ ] **Step 1: Write the failing mapping assertions**

Replace `PuffishPerkIdsTest` with:

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PuffishPerkIdsTest {
    @Test
    void mapsEveryPerkToTheUnifiedCategory() {
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.BLADE_CORE));
        assertEquals("blade_technique__blade_core", PuffishPerkIds.skillId(Perk.BLADE_CORE));
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.ENDUR_TRANSCENDENCE));
        assertEquals("statmod:statmod_perks", PuffishPerkIds.categoryId(Perk.WILL_TRANSCENDENCE));
    }

    @Test
    void resolvesPerkBackFromUnifiedCategoryAndSkill() {
        assertSame(Perk.BRUTE_CORE,
                PuffishPerkIds.resolve("statmod:statmod_perks", "brute_force__brute_core"));
        assertSame(Perk.WILL_TRANSCENDENCE,
                PuffishPerkIds.resolve("statmod:statmod_perks", "willpower__will_transcendence"));
    }
}
```

- [ ] **Step 2: Write the failing sync assertions**

Replace `PuffishSyncServiceTest` with:

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishSyncServiceTest {
    @Test
    void mirrorsUnlockedPerksIntoTheUnifiedCategory() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 3);
        data.addUnlockedPerk(Perk.BRUTE_CORE.id);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("category:statmod:statmod_perks"));
        assertTrue(gateway.operations.contains("unlock:statmod:statmod_perks:brute_force__brute_core"));
    }

    @Test
    void mirrorsTotalAvailablePerkPointsAcrossFamilies() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 3);
        data.setPerkPoints(Perk.BLADE_CORE.stat.index, 2);
        data.setPerkPoints(Perk.WILL_CORE.stat.index, 4);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("points:statmod:statmod_perks:9"));
    }

    @Test
    void stillLocksPerksThatAreNotCanonicallyUnlocked() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 1);

        FakeGateway gateway = new FakeGateway();
        PuffishSyncService.sync(data, gateway);

        assertTrue(gateway.operations.contains("lock:statmod:statmod_perks:blade_technique__blade_core"));
    }

    static final class FakeGateway implements PuffishMirrorGateway {
        final List<String> operations = new ArrayList<>();

        @Override
        public void ensureCategoryUnlocked(String categoryId) {
            operations.add("category:" + categoryId);
        }

        @Override
        public void setPoints(String categoryId, int points) {
            operations.add("points:" + categoryId + ":" + points);
        }

        @Override
        public void unlock(String categoryId, String skillId) {
            operations.add("unlock:" + categoryId + ":" + skillId);
        }

        @Override
        public void lock(String categoryId, String skillId) {
            operations.add("lock:" + categoryId + ":" + skillId);
        }
    }
}
```

- [ ] **Step 3: Write the failing resource/config assertions**

Replace `PuffishFamilyResourceConfigTest` with:

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishFamilyResourceConfigTest {
    @Test
    void checkedInConfigUsesOneUnifiedPerkCategory() throws Exception {
        try (InputStream stream = PuffishFamilyResourceConfigTest.class.getClassLoader()
                .getResourceAsStream("data/statmod/puffish_skills/config.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"statmod_perks\""));
        }
    }
}
```

Create `PuffishUnifiedPerkCategoryResourceTest.java`:

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishUnifiedPerkCategoryResourceTest {
    @Test
    void unifiedPerkCategoryFilesExist() throws Exception {
        ClassLoader loader = PuffishUnifiedPerkCategoryResourceTest.class.getClassLoader();
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/category.json");
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/definitions.json");
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/skills.json");
        assertHas(loader, "data/statmod/puffish_skills/categories/statmod_perks/connections.json");
    }

    @Test
    void unifiedDefinitionsStillContainMultipleFamilies() throws Exception {
        try (InputStream stream = PuffishUnifiedPerkCategoryResourceTest.class.getClassLoader()
                .getResourceAsStream("data/statmod/puffish_skills/categories/statmod_perks/definitions.json")) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"brute_force__brute_core\""));
            assertTrue(json.contains("\"precision__precision_core\""));
            assertTrue(json.contains("\"arcane_power__arcane_core\""));
            assertTrue(json.contains("\"forging__forge_core\""));
        }
    }

    private static void assertHas(ClassLoader loader, String path) throws Exception {
        try (InputStream stream = loader.getResourceAsStream(path)) {
            assertNotNull(stream, path);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishPerkIdsTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest --tests tong.statmod.integration.puffish.PuffishFamilyResourceConfigTest --tests tong.statmod.integration.puffish.PuffishUnifiedPerkCategoryResourceTest`

Expected:
- `PuffishPerkIdsTest` fails because category ids are still family-specific
- `PuffishSyncServiceTest` fails because points are still mirrored per family category
- resource tests fail because `statmod_perks` resources do not exist yet

- [ ] **Step 5: Commit**

```bash
git add src/test/java/tong/statmod/integration/puffish/PuffishPerkIdsTest.java src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java src/test/java/tong/statmod/integration/puffish/PuffishFamilyResourceConfigTest.java src/test/java/tong/statmod/integration/puffish/PuffishUnifiedPerkCategoryResourceTest.java
git commit -m "test: define unified Puffish perk category contract"
```

---

### Task 2: Switch mapping and sync to the unified category

**Files:**
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java`
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java`

- [ ] **Step 1: Implement unified category mapping**

Replace `PuffishPerkIds.java` with:

```java
package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;

import java.util.HashMap;
import java.util.Map;

public final class PuffishPerkIds {
    public static final String UNIFIED_CATEGORY = "statmod:statmod_perks";

    private static final Map<String, Perk> BY_PATH = new HashMap<>();

    static {
        for (Perk perk : Perk.values()) {
            BY_PATH.put(categoryId(perk) + "|" + skillId(perk), perk);
        }
    }

    private PuffishPerkIds() {}

    public static String categoryId(Perk perk) {
        return UNIFIED_CATEGORY;
    }

    public static String skillId(Perk perk) {
        return PuffishFamilyTreeBuilder.skillId(perk);
    }

    public static Perk resolve(String categoryId, String skillId) {
        if (categoryId == null || skillId == null) {
            return null;
        }
        return BY_PATH.get(categoryId + "|" + skillId);
    }
}
```

- [ ] **Step 2: Implement aggregate total-point sync**

Replace `PuffishSyncService.java` with:

```java
package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.stats.StatFamily;
import tong.statmod.storage.PlayerStatData;

public final class PuffishSyncService {
    private PuffishSyncService() {}

    public static void sync(PlayerStatData data, PuffishMirrorGateway gateway) {
        String categoryId = PuffishPerkIds.UNIFIED_CATEGORY;
        gateway.ensureCategoryUnlocked(categoryId);
        gateway.setPoints(categoryId, totalAvailablePerkPoints(data));

        for (Perk perk : Perk.values()) {
            if (data.isPerkUnlocked(perk.id)) {
                gateway.unlock(categoryId, PuffishPerkIds.skillId(perk));
            } else {
                gateway.lock(categoryId, PuffishPerkIds.skillId(perk));
            }
        }

        PuffishMagicSyncService.sync(data, gateway);
    }

    static int totalAvailablePerkPoints(PlayerStatData data) {
        int total = 0;
        for (StatFamily family : StatFamily.values()) {
            total += data.getPerkPointsForFamily(family);
        }
        return total;
    }
}
```

- [ ] **Step 3: Run the targeted tests**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishPerkIdsTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest`

Expected: mapping and sync tests pass, resource tests still fail.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishPerkIds.java src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java
git commit -m "feat: unify Puffish perk mapping and sync category"
```

---

### Task 3: Refactor the builder to emit one hub-and-branches category

**Files:**
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilder.java`
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeExporter.java`

- [ ] **Step 1: Replace the config/builder API with unified outputs**

Replace `PuffishFamilyTreeBuilder.java` with this structure:

```java
package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class PuffishFamilyTreeBuilder {
    public record GeneratedCategoryFiles(
            String categoryJson,
            String skillsJson,
            String definitionsJson,
            String connectionsJson
    ) {}

    private static final List<PerkTier> TIER_ORDER = List.of(
            PerkTier.CORE,
            PerkTier.ACTIVE,
            PerkTier.SYNERGY,
            PerkTier.SITUATIONAL,
            PerkTier.MASTERY,
            PerkTier.TRANSCENDENCE
    );

    private PuffishFamilyTreeBuilder() {}

    public static String configJson() {
        return "{\n" +
                "    \"version\": 3,\n" +
                "    \"categories\": [\n" +
                "        \"statmod_perks\",\n" +
                "        \"statmod_magic_common\",\n" +
                "        \"statmod_magic_fire\",\n" +
                "        \"statmod_magic_water\",\n" +
                "        \"statmod_magic_air\",\n" +
                "        \"statmod_magic_earth\",\n" +
                "        \"statmod_magic_holy\",\n" +
                "        \"statmod_magic_blood\",\n" +
                "        \"statmod_magic_ender\",\n" +
                "        \"statmod_magic_evocation\",\n" +
                "        \"statmod_magic_eldritch\",\n" +
                "        \"statmod_magic_locked\"\n" +
                "    ]\n" +
                "}\n";
    }

    public static String categoryId(Perk perk) {
        return PuffishPerkIds.UNIFIED_CATEGORY;
    }

    public static String skillId(Perk perk) {
        return perk.stat.name().toLowerCase(Locale.ROOT) + "__" + perk.name().toLowerCase(Locale.ROOT);
    }

    public static GeneratedCategoryFiles unifiedCategoryFiles() {
        return new GeneratedCategoryFiles(
                buildUnifiedCategoryJson(),
                buildUnifiedSkillsJson(),
                buildUnifiedDefinitionsJson(),
                buildUnifiedConnectionsJson()
        );
    }

    private static String buildUnifiedCategoryJson() {
        return "{\n" +
                "    \"unlocked_by_default\": true,\n" +
                "    \"title\": \"Perk Tree\",\n" +
                "    \"icon\": {\n" +
                "        \"type\": \"item\",\n" +
                "        \"data\": {\n" +
                "            \"item\": \"minecraft:nether_star\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"background\": \"textures/gui/advancements/backgrounds/adventure.png\"\n" +
                "}\n";
    }

    private static String buildUnifiedSkillsJson() {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<StatFamily, List<StatType>> entry : familyStats().entrySet()) {
            StatFamily family = entry.getKey();
            List<StatType> stats = entry.getValue();
            int baseX = familyBaseX(family);
            int baseY = familyBaseY(family);
            for (int column = 0; column < stats.size(); column++) {
                StatType stat = stats.get(column);
                for (int row = 0; row < TIER_ORDER.size(); row++) {
                    Perk perk = Perk.byStatAndTier(stat, TIER_ORDER.get(row));
                    if (perk == null) continue;
                    String id = skillId(perk);
                    StringBuilder node = new StringBuilder();
                    node.append("    \\\"").append(id).append("\\\": {\\n");
                    node.append("        \\\"x\\\": ").append(baseX + column * 150).append(",\\n");
                    node.append("        \\\"y\\\": ").append(baseY + row * 82).append(",\\n");
                    node.append("        \\\"definition\\\": \\\"").append(id).append("\\\"");
                    if (perk.tier == PerkTier.CORE) {
                        node.append(",\\n        \\\"root\\\": true");
                    }
                    node.append("\\n    }");
                    entries.add(node.toString().replace("\\\\", ""));
                }
            }
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildUnifiedDefinitionsJson() {
        List<String> entries = new ArrayList<>();
        for (Perk perk : Perk.values()) {
            String id = skillId(perk);
            entries.add("    \"" + id + "\": {\n" +
                    "        \"title\": \"" + escape(perk.name) + "\",\n" +
                    "        \"description\": \"" + escape(perk.description) + "\",\n" +
                    "        \"icon\": {\n" +
                    "            \"type\": \"item\",\n" +
                    "            \"data\": {\n" +
                    "                \"item\": \"" + iconForStat(perk.stat) + "\"\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }");
        }
        return "{\n" + String.join(",\n", entries) + "\n}\n";
    }

    private static String buildUnifiedConnectionsJson() {
        List<String> pairs = new ArrayList<>();
        for (StatType stat : StatType.values()) {
            Perk previous = null;
            for (PerkTier tier : TIER_ORDER) {
                Perk perk = Perk.byStatAndTier(stat, tier);
                if (perk == null) continue;
                if (previous != null) {
                    pairs.add("            [\n" +
                            "                \"" + skillId(previous) + "\",\n" +
                            "                \"" + skillId(perk) + "\"\n" +
                            "            ]");
                }
                previous = perk;
            }
        }
        return "{\n" +
                "    \"normal\": {\n" +
                "        \"bidirectional\": [\n" +
                String.join(",\n", pairs) + "\n" +
                "        ]\n" +
                "    }\n" +
                "}\n";
    }

    private static Map<StatFamily, List<StatType>> familyStats() {
        Map<StatFamily, List<StatType>> byFamily = new LinkedHashMap<>();
        for (StatFamily family : StatFamily.values()) {
            byFamily.put(family, Arrays.stream(StatType.values())
                    .filter(stat -> stat.family() == family)
                    .collect(Collectors.toList()));
        }
        return byFamily;
    }

    private static int familyBaseX(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> 40;
            case RANGED_HUNT_CONTROL -> 760;
            case MAGICAL_CORE -> 400;
            case ELEMENTAL_SPECIALIZATION -> 400;
            case MENTAL_PRESSURE_RESILIENCE -> 40;
            case CRAFTING_SUPPORT -> 760;
        };
    }

    private static int familyBaseY(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> 40;
            case RANGED_HUNT_CONTROL -> 40;
            case MAGICAL_CORE -> 520;
            case ELEMENTAL_SPECIALIZATION -> 1080;
            case MENTAL_PRESSURE_RESILIENCE -> 1080;
            case CRAFTING_SUPPORT -> 1080;
        };
    }

    private static String iconForStat(StatType stat) {
        return switch (stat) {
            case BRUTE_FORCE -> "minecraft:iron_axe";
            case BLADE_TECHNIQUE -> "minecraft:iron_sword";
            case RAPIDITE -> "minecraft:golden_sword";
            case AGILITY -> "minecraft:feather";
            case PHYSICAL_RESISTANCE -> "minecraft:shield";
            case PHYSICAL_ENDURANCE -> "minecraft:cooked_beef";
            case PRECISION -> "minecraft:bow";
            case TRACKING -> "minecraft:spyglass";
            case KEEN_SENSES -> "minecraft:ender_eye";
            case ARCANE_POWER -> "minecraft:blaze_rod";
            case WATER_AFFINITY -> "minecraft:water_bucket";
            case EARTH_AFFINITY -> "minecraft:stone";
            case FIRE_AFFINITY -> "minecraft:fire_charge";
            case AIR_AFFINITY -> "minecraft:feather";
            case MAGIC_RESISTANCE -> "minecraft:shield";
            case CASTING_SPEED -> "minecraft:clock";
            case MANA_POOL -> "minecraft:amethyst_shard";
            case ERUDITION -> "minecraft:book";
            case FORGING -> "minecraft:anvil";
            case COOKING -> "minecraft:cooked_beef";
            case ALCHEMY -> "minecraft:brewing_stand";
            case INTIMIDATION -> "minecraft:wither_skeleton_skull";
            case WILLPOWER -> "minecraft:totem_of_undying";
        };
    }

    private static String escape(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
```

- [ ] **Step 2: Update the exporter to emit one category folder**

Replace `PuffishFamilyTreeExporter.java` with:

```java
package tong.statmod.integration.puffish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PuffishFamilyTreeExporter {
    private PuffishFamilyTreeExporter() {}

    public static void main(String[] args) throws IOException {
        exportTo(Path.of("src/main/resources/data/statmod/puffish_skills"));
    }

    public static void exportTo(Path root) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("config.json"), PuffishFamilyTreeBuilder.configJson());

        Path categoriesRoot = root.resolve("categories");
        Files.createDirectories(categoriesRoot);

        PuffishFamilyTreeBuilder.GeneratedCategoryFiles files = PuffishFamilyTreeBuilder.unifiedCategoryFiles();
        Path perkRoot = categoriesRoot.resolve("statmod_perks");
        Files.createDirectories(perkRoot);
        Files.writeString(perkRoot.resolve("category.json"), files.categoryJson());
        Files.writeString(perkRoot.resolve("skills.json"), files.skillsJson());
        Files.writeString(perkRoot.resolve("definitions.json"), files.definitionsJson());
        Files.writeString(perkRoot.resolve("connections.json"), files.connectionsJson());
    }
}
```

- [ ] **Step 3: Run a compile check**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishPerkIdsTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest`

Expected: mapping/sync tests still pass. Resource tests still fail because checked-in files are not updated yet.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeBuilder.java src/main/java/tong/statmod/integration/puffish/PuffishFamilyTreeExporter.java
git commit -m "refactor: generate one unified Puffish perk category"
```

---

### Task 4: Check in unified resources and activate them in config

**Files:**
- Modify: `src/main/resources/data/statmod/puffish_skills/config.json`
- Create: `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/category.json`
- Create: `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/definitions.json`
- Create: `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/skills.json`
- Create: `src/main/resources/data/statmod/puffish_skills/categories/statmod_perks/connections.json`

- [ ] **Step 1: Generate the unified resource payload**

Run this temporary exporter entrypoint from the repo root:

```bash
@'
import tong.statmod.integration.puffish.PuffishFamilyTreeBuilder;
import tong.statmod.integration.puffish.PuffishFamilyTreeBuilder.GeneratedCategoryFiles;
print(PuffishFamilyTreeBuilder.configJson());
'@ | python -
```

Expected: inspect the generated category names and confirm `"statmod_perks"` is present before copying anything.

- [ ] **Step 2: Replace the active checked-in config**

Set `src/main/resources/data/statmod/puffish_skills/config.json` to:

```json
{
    "version": 3,
    "categories": [
        "statmod_perks",
        "statmod_magic_common",
        "statmod_magic_fire",
        "statmod_magic_water",
        "statmod_magic_air",
        "statmod_magic_earth",
        "statmod_magic_holy",
        "statmod_magic_blood",
        "statmod_magic_ender",
        "statmod_magic_evocation",
        "statmod_magic_eldritch",
        "statmod_magic_locked"
    ]
}
```

- [ ] **Step 3: Check in the unified category resources**

Create `category.json`:

```json
{
    "unlocked_by_default": true,
    "title": "Perk Tree",
    "icon": {
        "type": "item",
        "data": {
            "item": "minecraft:nether_star"
        }
    },
    "background": "textures/gui/advancements/backgrounds/adventure.png"
}
```

Create `definitions.json`, `skills.json`, and `connections.json` using the exact output of `PuffishFamilyTreeBuilder.unifiedCategoryFiles()`. Do not hand-invent partial content: the checked-in files must match the builder contract from Task 3.

- [ ] **Step 4: Run the full targeted Puffish tests**

Run: `.\gradlew.bat test --tests tong.statmod.integration.puffish.PuffishPerkIdsTest --tests tong.statmod.integration.puffish.PuffishSyncServiceTest --tests tong.statmod.integration.puffish.PuffishFamilyResourceConfigTest --tests tong.statmod.integration.puffish.PuffishUnifiedPerkCategoryResourceTest`

Expected: PASS.

- [ ] **Step 5: Run a build**

Run: `.\gradlew.bat build`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/data/statmod/puffish_skills/config.json src/main/resources/data/statmod/puffish_skills/categories/statmod_perks src/test/java/tong/statmod/integration/puffish/PuffishPerkIdsTest.java src/test/java/tong/statmod/integration/puffish/PuffishSyncServiceTest.java src/test/java/tong/statmod/integration/puffish/PuffishFamilyResourceConfigTest.java src/test/java/tong/statmod/integration/puffish/PuffishUnifiedPerkCategoryResourceTest.java
git commit -m "feat: ship unified Puffish perk category resources"
```

---

### Task 5: Manual Puffish runtime verification

**Files:** none

- [ ] **Step 1: Run the client**

Run: `.\gradlew.bat runClient`

Expected: client launches cleanly.

- [ ] **Step 2: Open the perk UI**

Use the existing perk keybind or the stats screen perk button.

Expected:
- one Puffish perk tab for stats/perks
- no family tab switching for perks
- separate magic tabs still visible

- [ ] **Step 3: Verify grouping**

Inspect the unified perk category.

Expected:
- visible family regions rather than one flat wall
- frontline/ranged/magical/elemental/mental/crafting all present

- [ ] **Step 4: Verify aggregate points do not bypass validation**

Prepare a player with points in one family and none in another, then click a locked perk from the wrong family.

Expected:
- Puffish shows the unified total pool
- canonical unlock still fails when the correct family budget is missing
- the node stays locked after sync

- [ ] **Step 5: Commit a smoke marker**

```bash
git commit --allow-empty -m "test: verify unified Puffish perk category in client"
```

---

## Self-Review Notes

- Spec §3.1 source of truth → covered in Tasks 2 and 5; `PlayerStatData` and `PerkManager` stay authoritative.
- Spec §3.2 single UI category → covered in Tasks 2, 3, and 4 through `statmod:statmod_perks`.
- Spec §3.3 family logic preserved → covered in Tasks 2 and 5; aggregate display is UI-only, validation remains canonical.
- Spec §4.2 hub-and-branches layout → covered in Task 3 through unified resource generation with family regions.
- Spec §5 total point display → covered in Task 2 via `totalAvailablePerkPoints`.
- Spec §6 unlock flow → covered in Task 5 manual verification because the server-side unlock pipeline should stay unchanged.
- Spec §8 config/resources → covered in Tasks 3 and 4.
- Spec §10 testing strategy → covered in Tasks 1, 4, and 5.

No independent subsystems remain unplanned. This is a single bounded Puffish perk-category migration.
