# Detailed SDM Shop Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a progression-safe SDM Shop with 18 detailed categories and 350–450 verified articles from the installed modpack.

**Architecture:** `SDMShopCatalog` remains a server-independent declarative catalog. `SDMShopDatabaseInitializer` resolves namespaced item IDs at runtime, skips absent optional-mod entries, creates special potion stacks, and regenerates legacy NBT files through a catalog version marker.

**Tech Stack:** Java 21, NeoForge 21.1.232 for Minecraft 1.21.1, JUnit 5, Gradle, local mod JARs in `libs/`.

## Global Constraints

- Use exactly 18 functional categories from the approved design.
- Keep the final catalog between 350 and 450 unique articles.
- Cover Vanilla, STAT Mod, Tensura, Iron's Spells, Ice and Fire, Apotheosis, Epic Fight, Simply Swords, Epic Knights, and Overgeared.
- Exclude spawn eggs, boss summons, creative/admin items, narrative keys, and progression-breaking final rewards.
- Resolve every item through `BuiltInRegistries.ITEM`; missing optional items must be logged and skipped.
- Use the five approved price bands: 1–25, 30–100, 120–350, 400–900, and 1,000–2,500 FDP.

---

### Task 1: Encode catalog invariants

**Files:**
- Modify: `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMShopCatalog.java`

**Interfaces:**
- Consumes: `SDMShopCatalog.tabs()`, `SDMShopCatalog.items()`
- Produces: `SDMShopCatalog.isForbiddenItemId(String): boolean`

- [ ] **Step 1: Write failing tests for detailed categories, volume, uniqueness, and exclusions**

```java
private static final Set<String> EXPECTED_TABS = Set.of(
    "Minerais bruts", "Lingots et gemmes", "Matériaux avancés",
    "Forge et amélioration", "Armes légères", "Armes lourdes",
    "Lances et armes d'hast", "Armes à distance", "Armures classiques",
    "Armures fantastiques", "Magie et parchemins",
    "Runes et composants magiques", "Potions et soins",
    "Composants de monstres", "Nourriture", "Construction",
    "Utilitaires", "Objets rares contrôlés");

@Test
void exposesDetailedCategoriesAndControlledVolume() {
    assertEquals(EXPECTED_TABS, SDMShopCatalog.tabs().stream()
        .map(SDMShopCatalog.ShopTab::name).collect(Collectors.toSet()));
    assertTrue(SDMShopCatalog.items().size() >= 350);
    assertTrue(SDMShopCatalog.items().size() <= 450);
}

@Test
void containsNoDuplicatesOrForbiddenItems() {
    Set<String> ids = new HashSet<>();
    for (var item : SDMShopCatalog.items()) {
        assertTrue(ids.add(item.itemId()), item.itemId());
        assertFalse(SDMShopCatalog.isForbiddenItemId(item.itemId()), item.itemId());
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopCatalogTest --console=plain`

Expected: FAIL because the catalog has 8 categories, fewer than 350 entries, and `isForbiddenItemId` does not exist.

- [ ] **Step 3: Add the minimal forbidden-ID policy**

```java
private static final List<String> FORBIDDEN_ID_PARTS = List.of(
    "spawn_egg", "boss_summoner", "creative", "command_block",
    "structure_block", "debug_stick");

public static boolean isForbiddenItemId(String itemId) {
    return FORBIDDEN_ID_PARTS.stream().anyMatch(itemId::contains);
}
```

- [ ] **Step 4: Keep the test red only for category count and catalog volume**

Run the same targeted Gradle command.

Expected: FAIL on category/volume assertions, with the forbidden-ID API compiling.

- [ ] **Step 5: Commit the invariant test and policy**

```powershell
git add src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java src/main/java/tong/statmod/integration/sdm/SDMShopCatalog.java
git commit -m "test: define detailed SDM shop invariants"
```

### Task 2: Produce a verified candidate inventory

**Files:**
- Create: `tools/audit_sdm_shop_items.ps1`
- Create: `docs/generated/sdm-shop-item-candidates.tsv`
- Test: `src/test/java/tong/statmod/integration/sdm/SDMShopCandidateInventoryTest.java`

**Interfaces:**
- Consumes: item model paths from `libs/*.jar`
- Produces: TSV columns `mod_id`, `item_id`, `source_jar`

- [ ] **Step 1: Write a failing resource test**

```java
@Test
void candidateInventoryHasRequiredModsAndNoForbiddenIds() throws IOException {
    Path path = Path.of("docs/generated/sdm-shop-item-candidates.tsv");
    assertTrue(Files.exists(path));
    String tsv = Files.readString(path);
    for (String mod : List.of("minecraft", "statmod", "tensura",
            "irons_spellbooks", "iceandfire", "apotheosis", "epicfight",
            "simplyswords", "magistuarmory", "overgeared")) {
        assertTrue(tsv.contains(mod + "\t"), mod);
    }
    assertFalse(tsv.contains("spawn_egg"));
}
```

- [ ] **Step 2: Run the resource test and verify RED**

Run: `.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopCandidateInventoryTest --console=plain`

Expected: FAIL because the TSV does not exist.

- [ ] **Step 3: Implement the JAR audit script**

```powershell
$rows = foreach ($jar in Get-ChildItem -LiteralPath libs -Filter *.jar) {
    & jar tf $jar.FullName |
        Where-Object { $_ -match '^assets/([^/]+)/models/item/([^/]+)\.json$' } |
        ForEach-Object {
            if ($_ -match '^assets/([^/]+)/models/item/([^/]+)\.json$') {
                $id = "$($Matches[1]):$($Matches[2])"
                if ($id -notmatch 'spawn_egg|boss_summoner|creative|command_block|debug_stick') {
                    "$($Matches[1])`t$id`t$($jar.Name)"
                }
            }
        }
}
$rows | Sort-Object -Unique | Set-Content -Encoding utf8 docs/generated/sdm-shop-item-candidates.tsv
```

- [ ] **Step 4: Run the audit and make the resource test pass**

Run: `powershell -ExecutionPolicy Bypass -File tools/audit_sdm_shop_items.ps1`

Then run the targeted resource test. Expected: PASS.

- [ ] **Step 5: Commit the reproducible inventory**

```powershell
git add tools/audit_sdm_shop_items.ps1 docs/generated/sdm-shop-item-candidates.tsv src/test/java/tong/statmod/integration/sdm/SDMShopCandidateInventoryTest.java
git commit -m "build: audit SDM shop item candidates"
```

### Task 3: Populate the 18-category controlled catalog

**Files:**
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMShopCatalog.java`
- Modify: `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`

**Interfaces:**
- Consumes: verified namespaced IDs in `docs/generated/sdm-shop-item-candidates.tsv`
- Produces: 350–450 unique `ShopItem` records grouped under the 18 approved tabs

- [ ] **Step 1: Add family-coverage assertions**

```java
@Test
void coversEveryRequiredContentFamily() {
    Set<String> namespaces = SDMShopCatalog.items().stream()
        .map(i -> i.itemId().substring(0, i.itemId().indexOf(':')))
        .collect(Collectors.toSet());
    assertTrue(namespaces.containsAll(Set.of(
        "minecraft", "statmod", "tensura", "irons_spellbooks",
        "iceandfire", "apotheosis", "epicfight", "simplyswords",
        "magistuarmory", "overgeared")));
}
```

- [ ] **Step 2: Run tests and verify RED**

Expected: FAIL for missing namespaces and insufficient volume.

- [ ] **Step 3: Replace the eight legacy tabs with the approved eighteen tabs**

Use stable Vanilla or verified modded icons, for example:

```java
tab("Minerais bruts", "minecraft:raw_iron"),
tab("Lingots et gemmes", "minecraft:iron_ingot"),
tab("Matériaux avancés", "minecraft:netherite_ingot"),
tab("Forge et amélioration", "minecraft:smithing_table"),
tab("Armes légères", "epicfight:dagger"),
tab("Armes lourdes", "epicfight:iron_greatsword")
```

Continue with all 18 names exactly as asserted in Task 1.

- [ ] **Step 4: Populate 350–450 unique entries**

Add only IDs present in the generated TSV. Assign each ID to one functional
category and one price band. Use counts of 16–64 for common construction
blocks and ammunition, 4–16 for food and common raw resources, and 1 for
equipment, rare materials, magic components, and monster drops.

- [ ] **Step 5: Run targeted tests and verify GREEN**

Run: `.\gradlew.bat test --tests 'tong.statmod.integration.sdm.*' --console=plain`

Expected: all SDM integration tests PASS.

- [ ] **Step 6: Commit the complete catalog**

```powershell
git add src/main/java/tong/statmod/integration/sdm/SDMShopCatalog.java src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java
git commit -m "feat: populate detailed SDM shop catalog"
```

### Task 4: Migrate NBT data and verify the build

**Files:**
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMShopCatalog.java`
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMShopDatabaseInitializer.java`
- Test: `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`

**Interfaces:**
- Consumes: `SDMShopCatalog.VERSION`, `ShopItem.potionId()`
- Produces: regenerated `SDMTovarTab.sdm` and `SDMTovarList.sdm` with safe optional-item handling

- [ ] **Step 1: Write a failing migration-version assertion**

```java
@Test
void detailedCatalogUsesANewMigrationVersion() {
    assertTrue(SDMShopCatalog.VERSION >= 3);
    assertTrue(SDMShopCatalog.isCurrentVersion(SDMShopCatalog.VERSION));
    assertFalse(SDMShopCatalog.isCurrentVersion(2));
}
```

- [ ] **Step 2: Run the test and verify RED**

Expected: FAIL because the current version is 2.

- [ ] **Step 3: Raise the version and retain safe runtime resolution**

```java
public static final int VERSION = 3;
```

Keep `resolveItem(String)` based on `BuiltInRegistries.ITEM.getOptional`.
Missing icons skip only their tab; missing articles skip only their entry.

- [ ] **Step 4: Run scoped and full verification**

```powershell
.\gradlew.bat test --tests 'tong.statmod.integration.sdm.*' --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat build -x test --console=plain
git diff --check
```

Expected: scoped tests PASS and build succeeds. If the known unrelated
`PuffishUnifiedPerkCategoryResourceTest` still fails, record it separately
with the exact total and do not modify Puffish files as part of this feature.

- [ ] **Step 5: Commit the migration**

```powershell
git add src/main/java/tong/statmod/integration/sdm/SDMShopCatalog.java src/main/java/tong/statmod/integration/sdm/SDMShopDatabaseInitializer.java src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java
git commit -m "feat: migrate SDM shop to detailed catalog"
```
