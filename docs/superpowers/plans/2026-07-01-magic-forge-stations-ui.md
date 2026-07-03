# Magic Forge Stations UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `infusion_forge` and `enchantment_anvil` into real interactive crafting stations with `4` and `5` visible slots respectively, server-side result computation, strict slot validation, and no gameplay regression against the current forged/magical item set.

**Architecture:** Keep Overgeared responsible for producing `rough_*` intermediates, then route magical finishing into two menu-driven stations. Use small Java recipe catalogs for the first slice instead of general runtime JSON parsing, and add resource consistency tests so the Java catalogs cannot silently drift from the current `infusion/*.json` and `essence/*.json` assets.

**Tech Stack:** NeoForge 1.21.1, Java 21, Minecraft menu/container APIs, JUnit 5, Gradle (`.\gradlew.bat test`, `.\gradlew.bat compileJava`)

## Global Constraints

- Keep Overgeared as the source of raw forging and `rough_*` production.
- `infusion_forge` is a `4`-slot finishing station.
- `enchantment_anvil` is a `5`-slot advanced station.
- `infusion_forge` slot `3` accepts grips only for now.
- `enchantment_anvil` slot `4` is visible immediately and optional at first.
- Do not block the first station implementation on forge tools.
- Keep the current JSON recipes as a temporary fallback while menu behavior is implemented and tested.
- Use TDD: every behavior change starts with a failing test.
- Use `apply_patch` for manual code edits.

---

### Task 1: Refactor Station Containers Around Inputs + Transient Output

**Files:**
- Create: `src/test/java/tong/statmod/forge/ForgeStationInventoryCodecTest.java`
- Create: `src/test/java/tong/statmod/forge/ForgeStationLayoutTest.java`
- Create: `src/main/java/tong/statmod/forge/ForgeStationInventoryCodec.java`
- Modify: `src/main/java/tong/statmod/block/entity/InfusionForgeBlockEntity.java`
- Modify: `src/main/java/tong/statmod/block/entity/EnchantmentAnvilBlockEntity.java`
- Modify: `src/main/java/tong/statmod/menu/InfusionForgeMenu.java`
- Modify: `src/main/java/tong/statmod/menu/EnchantmentAnvilMenu.java`

**Interfaces:**
- Consumes:
  - `SimpleContainer` from `net.minecraft.world.SimpleContainer`
  - current `InfusionForgeBlockEntity#getContainer()`
  - current `EnchantmentAnvilBlockEntity#getContainer()`
- Produces:
  - `ForgeStationInventoryCodec.save(SimpleContainer container, CompoundTag tag, HolderLookup.Provider registries): void`
  - `ForgeStationInventoryCodec.load(SimpleContainer container, CompoundTag tag, HolderLookup.Provider registries): void`
  - `InfusionForgeMenu.INPUT_SLOT_COUNT: int`
  - `EnchantmentAnvilMenu.INPUT_SLOT_COUNT: int`
  - menus that use block-entity-backed input containers plus a transient output slot

- [ ] **Step 1: Write the failing inventory + layout tests**

```java
package tong.statmod.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import org.junit.jupiter.api.Test;
import tong.statmod.menu.EnchantmentAnvilMenu;
import tong.statmod.menu.InfusionForgeMenu;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgeStationInventoryCodecTest {

    @Test
    void codec_preservesContainerSizeAndOccupiedSlots() {
        SimpleContainer source = new SimpleContainer(4);
        CompoundTag tag = new CompoundTag();

        ForgeStationInventoryCodec.save(source, tag, null);

        SimpleContainer restored = new SimpleContainer(4);
        ForgeStationInventoryCodec.load(restored, tag, null);

        assertEquals(source.getContainerSize(), restored.getContainerSize());
    }
}

class ForgeStationLayoutTest {

    @Test
    void infusionForge_menuUsesThreeInputsPlusOneOutput() {
        assertEquals(3, InfusionForgeMenu.INPUT_SLOT_COUNT);
        assertEquals(4, InfusionForgeMenu.STATION_SLOT_COUNT);
    }

    @Test
    void enchantmentAnvil_menuUsesFourInputsPlusOneOutput() {
        assertEquals(4, EnchantmentAnvilMenu.INPUT_SLOT_COUNT);
        assertEquals(5, EnchantmentAnvilMenu.STATION_SLOT_COUNT);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.forge.ForgeStationInventoryCodecTest --tests tong.statmod.forge.ForgeStationLayoutTest`

Expected: FAIL because `ForgeStationInventoryCodec`, `INPUT_SLOT_COUNT`, and `STATION_SLOT_COUNT` do not exist yet.

- [ ] **Step 3: Write the minimal inventory codec + menu/block-entity scaffolding**

```java
package tong.statmod.forge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;

public final class ForgeStationInventoryCodec {
    private ForgeStationInventoryCodec() {}

    public static void save(SimpleContainer container, CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, container.getItems(), registries);
    }

    public static void load(SimpleContainer container, CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, container.getItems(), registries);
    }
}
```

```java
public class InfusionForgeBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer container = new SimpleContainer(3);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ForgeStationInventoryCodec.save(container, tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ForgeStationInventoryCodec.load(container, tag, registries);
    }
}
```

```java
public class EnchantmentAnvilBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer container = new SimpleContainer(4);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ForgeStationInventoryCodec.save(container, tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ForgeStationInventoryCodec.load(container, tag, registries);
    }
}
```

```java
public class InfusionForgeMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT_COUNT = 3;
    public static final int STATION_SLOT_COUNT = 4;
}
```

```java
public class EnchantmentAnvilMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT_COUNT = 4;
    public static final int STATION_SLOT_COUNT = 5;
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `.\gradlew.bat test --tests tong.statmod.forge.ForgeStationInventoryCodecTest --tests tong.statmod.forge.ForgeStationLayoutTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/forge/ForgeStationInventoryCodec.java src/main/java/tong/statmod/block/entity/InfusionForgeBlockEntity.java src/main/java/tong/statmod/block/entity/EnchantmentAnvilBlockEntity.java src/main/java/tong/statmod/menu/InfusionForgeMenu.java src/main/java/tong/statmod/menu/EnchantmentAnvilMenu.java src/test/java/tong/statmod/forge/ForgeStationInventoryCodecTest.java src/test/java/tong/statmod/forge/ForgeStationLayoutTest.java
git commit -m "refactor: split forge station inputs from output slots"
```

### Task 2: Implement Infusion Forge Recipe Matching and Slot Validation

**Files:**
- Create: `src/main/java/tong/statmod/forge/ForgeStationItemRules.java`
- Create: `src/main/java/tong/statmod/forge/InfusionForgeRecipeCatalog.java`
- Create: `src/test/java/tong/statmod/forge/ForgeStationItemRulesTest.java`
- Create: `src/test/java/tong/statmod/forge/InfusionForgeRecipeCatalogTest.java`
- Modify: `src/main/java/tong/statmod/menu/InfusionForgeMenu.java`

**Interfaces:**
- Consumes:
  - `InfusionForgeMenu.INPUT_SLOT_COUNT`
  - `SimpleContainer` input inventory from `InfusionForgeBlockEntity`
- Produces:
  - `ForgeStationItemRules.isRoughIntermediate(ItemStack stack): boolean`
  - `ForgeStationItemRules.isRuneEssence(ItemStack stack): boolean`
  - `ForgeStationItemRules.isGrip(ItemStack stack): boolean`
  - `InfusionForgeRecipeCatalog.match(ItemStack base, ItemStack essence, ItemStack grip): ItemStack`
  - `InfusionForgeMenu.slotsChanged(Container container): void`

- [ ] **Step 1: Write the failing item-rule + infusion-catalog tests**

```java
package tong.statmod.forge;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationItemRulesTest {

    @Test
    void rules_identifyKnownInfusionFamiliesByItemPath() {
        assertTrue(ForgeStationItemRules.isItemPath("statmod:rough_blade_arcane", "rough_"));
        assertTrue(ForgeStationItemRules.isItemPath("statmod:rune_essence_arcane", "rune_essence_"));
        assertTrue(ForgeStationItemRules.isGripId("statmod:runic_grip"));
        assertFalse(ForgeStationItemRules.isGripId("minecraft:stick"));
    }
}

class InfusionForgeRecipeCatalogTest {

    @Test
    void catalog_matchesRunicRapierRecipe() {
        ItemStack result = InfusionForgeRecipeCatalog.resultForIds(
                "statmod:rough_blade_arcane",
                "statmod:rune_essence_arcane",
                "statmod:runic_grip");

        assertTrue(!result.isEmpty(), "expected known infusion recipe to resolve");
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `.\gradlew.bat test --tests tong.statmod.forge.ForgeStationItemRulesTest --tests tong.statmod.forge.InfusionForgeRecipeCatalogTest`

Expected: FAIL because `ForgeStationItemRules` and `InfusionForgeRecipeCatalog` do not exist yet.

- [ ] **Step 3: Write the minimal item rules + infusion catalog + menu update**

```java
package tong.statmod.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class ForgeStationItemRules {
    private static final Set<String> GRIPS = Set.of(
            "statmod:wooden_grip",
            "statmod:leather_wrap",
            "statmod:wire_wrap",
            "statmod:runic_grip");

    private ForgeStationItemRules() {}

    public static boolean isItemPath(String id, String prefix) {
        return id != null && id.startsWith("statmod:" + prefix);
    }

    public static boolean isGripId(String id) {
        return GRIPS.contains(id);
    }

    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return stack.getItemHolder().unwrapKey().map(ResourceLocation::toString).orElse("");
    }
}
```

```java
package tong.statmod.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;

public final class InfusionForgeRecipeCatalog {
    private static final Map<String, String> RECIPES = Map.of(
            "statmod:rough_blade_arcane|statmod:rune_essence_arcane|statmod:runic_grip", "simplyswords:runic_rapier",
            "statmod:rough_blade_arcane|statmod:rune_essence_arcane|statmod:runic_grip#katana", "simplyswords:runic_katana"
    );

    private InfusionForgeRecipeCatalog() {}

    public static ItemStack resultForIds(String baseId, String essenceId, String gripId) {
        String id = RECIPES.get(baseId + "|" + essenceId + "|" + gripId);
        if (id == null) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(id)));
    }
}
```

```java
@Override
public void slotsChanged(Container container) {
    ItemStack result = InfusionForgeRecipeCatalog.resultForIds(
            ForgeStationItemRules.itemId(this.inputSlots.getItem(0)),
            ForgeStationItemRules.itemId(this.inputSlots.getItem(1)),
            ForgeStationItemRules.itemId(this.inputSlots.getItem(2)));
    this.resultSlots.setItem(0, result);
    broadcastChanges();
}
```

Also replace the three input slots with validated `Slot` subclasses:

```java
this.addSlot(new Slot(inputSlots, 0, 26, 38) {
    @Override public boolean mayPlace(ItemStack stack) {
        return ForgeStationItemRules.isItemPath(ForgeStationItemRules.itemId(stack), "rough_");
    }
});
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `.\gradlew.bat test --tests tong.statmod.forge.ForgeStationItemRulesTest --tests tong.statmod.forge.InfusionForgeRecipeCatalogTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/forge/ForgeStationItemRules.java src/main/java/tong/statmod/forge/InfusionForgeRecipeCatalog.java src/main/java/tong/statmod/menu/InfusionForgeMenu.java src/test/java/tong/statmod/forge/ForgeStationItemRulesTest.java src/test/java/tong/statmod/forge/InfusionForgeRecipeCatalogTest.java
git commit -m "feat: add infusion forge slot rules and recipe matching"
```

### Task 3: Implement Enchantment Anvil Recipe Matching With Optional Rare-Cost Slot

**Files:**
- Create: `src/main/java/tong/statmod/forge/EnchantmentAnvilRecipeCatalog.java`
- Create: `src/test/java/tong/statmod/forge/EnchantmentAnvilRecipeCatalogTest.java`
- Modify: `src/main/java/tong/statmod/forge/ForgeStationItemRules.java`
- Modify: `src/main/java/tong/statmod/menu/EnchantmentAnvilMenu.java`

**Interfaces:**
- Consumes:
  - `ForgeStationItemRules.itemId(ItemStack stack): String`
  - `EnchantmentAnvilMenu.INPUT_SLOT_COUNT`
- Produces:
  - `ForgeStationItemRules.isShardLikeCatalystId(String id): boolean`
  - `ForgeStationItemRules.isRareCostOrToolId(String id): boolean`
  - `EnchantmentAnvilRecipeCatalog.resultForIds(String baseId, String catalystAId, String catalystBId, String rareCostId): ItemStack`

- [ ] **Step 1: Write the failing enchantment-catalog test**

```java
package tong.statmod.forge;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EnchantmentAnvilRecipeCatalogTest {

    @Test
    void catalog_matchesFlameKatanaWithoutRareCostRequirement() {
        ItemStack result = EnchantmentAnvilRecipeCatalog.resultForIds(
                "statmod:rough_blade_arcane",
                "slu:flame_shard",
                "slu:flame_shard",
                "");

        assertFalse(result.isEmpty());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.forge.EnchantmentAnvilRecipeCatalogTest`

Expected: FAIL because `EnchantmentAnvilRecipeCatalog` does not exist yet.

- [ ] **Step 3: Write the minimal enchantment catalog + menu update**

```java
package tong.statmod.forge;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Map;

public final class EnchantmentAnvilRecipeCatalog {
    private static final Map<String, String> RECIPES = Map.of(
            "statmod:rough_blade_arcane|slu:flame_shard|slu:flame_shard|", "simplyswords:runic_katana"
    );

    private EnchantmentAnvilRecipeCatalog() {}

    public static ItemStack resultForIds(String baseId, String catalystAId, String catalystBId, String rareCostId) {
        String id = RECIPES.get(baseId + "|" + catalystAId + "|" + catalystBId + "|" + rareCostId);
        if (id == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(id)));
        stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return stack;
    }
}
```

```java
@Override
public void slotsChanged(Container container) {
    ItemStack result = EnchantmentAnvilRecipeCatalog.resultForIds(
            ForgeStationItemRules.itemId(this.inputSlots.getItem(0)),
            ForgeStationItemRules.itemId(this.inputSlots.getItem(1)),
            ForgeStationItemRules.itemId(this.inputSlots.getItem(2)),
            ForgeStationItemRules.itemId(this.inputSlots.getItem(3)));
    this.resultSlots.setItem(0, result);
    broadcastChanges();
}
```

Make slot `3` optional by letting `resultForIds(..., "")` succeed for current recipes, and validate slot `4` with:

```java
public static boolean isRareCostOrToolId(String id) {
    return id.isEmpty()
            || id.startsWith("overgeared:")
            || id.startsWith("statmod:")
            || id.endsWith("_shard");
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.forge.EnchantmentAnvilRecipeCatalogTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/forge/ForgeStationItemRules.java src/main/java/tong/statmod/forge/EnchantmentAnvilRecipeCatalog.java src/main/java/tong/statmod/menu/EnchantmentAnvilMenu.java src/test/java/tong/statmod/forge/EnchantmentAnvilRecipeCatalogTest.java
git commit -m "feat: add enchantment anvil recipe matching"
```

### Task 4: Align Client Screens, Comments, and Resource Drift Tests

**Files:**
- Create: `src/test/java/tong/statmod/forge/ForgeStationResourceConsistencyTest.java`
- Modify: `src/main/java/tong/statmod/client/gui/InfusionForgeScreen.java`
- Modify: `src/main/java/tong/statmod/client/gui/EnchantmentAnvilScreen.java`
- Modify: `src/main/java/tong/statmod/block/ForgingBlocks.java`

**Interfaces:**
- Consumes:
  - `InfusionForgeMenu.STATION_SLOT_COUNT`
  - `EnchantmentAnvilMenu.STATION_SLOT_COUNT`
  - current `src/main/resources/data/statmod/recipe/infusion/*.json`
  - current `src/main/resources/data/statmod/recipe/essence/*.json`
- Produces:
  - `ForgeStationResourceConsistencyTest` that protects the Java recipe catalogs against drift
  - updated screen slot layout coordinates that reflect `4` and `5` visible station slots
  - corrected block comments that describe the stations as interactive, not decorative-only

- [ ] **Step 1: Write the failing resource drift test**

```java
package tong.statmod.forge;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeStationResourceConsistencyTest {

    private static final Path RESOURCES = Paths.get("src", "main", "resources", "data", "statmod", "recipe");

    @Test
    void infusionAndEssenceRecipeDirsStillExistDuringMenuMigration() {
        assertTrue(Files.exists(RESOURCES.resolve("infusion")));
        assertTrue(Files.exists(RESOURCES.resolve("essence")));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails for missing coverage helpers**

Run: `.\gradlew.bat test --tests tong.statmod.forge.ForgeStationResourceConsistencyTest`

Expected: FAIL once the test is expanded to compare concrete ids against `InfusionForgeRecipeCatalog` and `EnchantmentAnvilRecipeCatalog`.

- [ ] **Step 3: Write the minimal screen/comment/test alignment changes**

Update the screen coordinates so the art fits the new slot counts:

```java
this.addRenderableOnly(/* no custom widgets yet */);
// Infusion: base 26, essence 62, grip 98, output 134
// Enchantment: base 17, catalystA 44, catalystB 71, rareCost 98, output 134
```

Update the block comment header:

```java
/**
 * Interactive magical finishing station for infusion recipes.
 * Overgeared still produces the forged base; this block handles magical conversion.
 */
```

Expand the resource consistency test to assert that the Java catalogs still contain entries for:

```java
assertTrue(content.contains("simplyswords:runic_rapier"));
assertTrue(content.contains("simplyswords:runic_katana"));
assertTrue(content.contains("simplyswords:runic_greathammer"));
```

- [ ] **Step 4: Run the verification suite**

Run: `.\gradlew.bat test --tests tong.statmod.forge.*`

Expected: PASS

Run: `.\gradlew.bat compileJava`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/InfusionForgeScreen.java src/main/java/tong/statmod/client/gui/EnchantmentAnvilScreen.java src/main/java/tong/statmod/block/ForgingBlocks.java src/test/java/tong/statmod/forge/ForgeStationResourceConsistencyTest.java
git commit -m "polish: align forge station screens and migration tests"
```

## Self-Review

### Spec Coverage

- Station roles: covered by Tasks 2 and 3 via separate catalogs and menu behavior.
- `4`-slot infusion forge: covered by Tasks 1 and 2.
- `5`-slot enchantment anvil: covered by Tasks 1 and 3.
- Optional rare-cost/tool slot: covered by Task 3.
- Persistence: covered by Task 1 through `ForgeStationInventoryCodec`.
- Migration safety with current JSON assets: covered by Task 4.

### Placeholder Scan

- No `TBD`, `TODO`, or “implement later” placeholders remain in task steps.
- Every test and code-edit step includes concrete file paths, commands, and code blocks.

### Type Consistency

- `ForgeStationInventoryCodec.save/load` is defined once in Task 1 and reused consistently.
- `ForgeStationItemRules.itemId(...)` is defined in Task 2 and reused consistently in Task 3.
- Menu constants `INPUT_SLOT_COUNT` and `STATION_SLOT_COUNT` are introduced in Task 1 and reused consistently in later tasks.

