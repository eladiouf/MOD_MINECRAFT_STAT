# SDM Shop Progression Pricing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand and rebalance the SDM Shop so item prices follow dungeon progression, keep solo farming from making the player rich too early, and grow the catalog to 1000 items while preserving the category layout.

**Architecture:** `tools/generate_sdm_shop_entries.ps1` is the source of truth for catalog selection and pricing. It will be refactored into a tier-driven pricing model keyed by dungeon progression, rarity, and item role; the generated Java catalog remains the runtime input consumed by `SDMShopCatalog`. Tests will enforce category bands, tier ordering, and a few anchor items so the balancing cannot drift back into flat or random pricing.

**Tech Stack:** PowerShell 5+, Java 21, JUnit 5, NeoForge 1.21.1 mod source tree.

## Global Constraints

- Keep the current SDM integration path: `STATMod` still registers `SDMShopNPCBridge` and `SDMShopDatabaseInitializer` only when `sdmshop` is loaded.
- Preserve the current shop structure: 26 categories, the existing category names, and a 1000-item catalog target.
- Keep the current currency bridge: `FDP_cfa`.
- Keep item selection progression-safe: no spawn eggs, command blocks, debug stick, or other forbidden IDs in the shop.
- Use a progression-serrated economy: early items must stay affordable, while high-impact combat items and rare control items must land in late/boss-tier price bands.
- Do not introduce new runtime dependencies; balance work stays inside the catalog generator, generated catalog, and tests.

---

### Task 1: Lock the balance rules in tests before changing prices

**Files:**
- Modify: `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`
- Create: `src/test/java/tong/statmod/integration/sdm/SDMShopPricingBalanceTest.java`

**Interfaces:**
- Consumes: `SDMShopCatalog.tabs()`, `SDMShopCatalog.items()`
- Consumes: `SDMShopCatalog.ShopItem.tab()`, `itemId()`, `price()`, `count()`, `potionId()`
- Produces: category band assertions, anchor-item ordering assertions, and a strict “no price regression” test suite

- [ ] **Step 1: Write the failing tests**

Add a focused pricing test class with three checks:

```java
@Test
void earlyCategoriesStayCheapAndHighTierTabsStayExpensive() {
    assertPriceBand("Minerais bruts", 100, 800);
    assertPriceBand("Lingots et gemmes", 300, 2500);
    assertPriceBand("Potions et soins", 200, 3000);
    assertPriceBand("Armures magiques", 4000, 14000);
    assertPriceBand("Armes uniques et légendaires", 8000, 30000);
    assertPriceBand("Objets rares contrôlés", 10000, 30000);
}

@Test
void keyProgressionAnchorsIncreaseMonotonically() {
    assertPriceLessThan("minecraft:raw_iron", "minecraft:diamond");
    assertPriceLessThan("minecraft:diamond", "minecraft:netherite_ingot");
    assertPriceLessThan("epicfight:diamond_dagger", "simplyswords:netherite_katana");
    assertPriceLessThan("simplyswords:diamond_katana", "simplyswords:runic_katana");
    assertPriceLessThan("minecraft:diamond_chestplate", "minecraft:netherite_chestplate");
}

@Test
void noCategoryContainsOutOfBandPricesOrForbiddenIds() {
    for (SDMShopCatalog.ShopItem item : SDMShopCatalog.items()) {
        assertFalse(SDMShopCatalog.isForbiddenItemId(item.itemId()), item.itemId());
        assertTrue(item.price() > 0, item.itemId());
        assertTrue(item.count() > 0, item.itemId());
    }
}
```

Expected result: the new price-band assertions fail against the current generated catalog, because the current prices are still tied to older ad hoc thresholds.

- [ ] **Step 2: Run the targeted test to verify the failure**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain
```

Expected: fail on price-band or anchor assertions.

- [ ] **Step 3: Add the helper methods needed by the assertions**

Implement the helpers in the new test class so later tasks can use them without duplicating lookups:

```java
private void assertPriceBand(String tab, int minInclusive, int maxInclusive) { ... }
private void assertPriceLessThan(String cheaperItemId, String pricierItemId) { ... }
private SDMShopCatalog.ShopItem find(String itemId) { ... }
```

These helpers must work only against `SDMShopCatalog.items()` and must not hardcode the output file format.

- [ ] **Step 4: Re-run the targeted test and confirm it still fails for the right reason**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain
```

Expected: the helper methods compile, but the current catalog still violates the intended progression bands.

---

### Task 2: Refactor the generator into a tier-driven pricing model

**Files:**
- Modify: `tools/generate_sdm_shop_entries.ps1`

**Interfaces:**
- Consumes: `docs/generated/sdm-shop-item-candidates.tsv`
- Produces: `src/main/java/tong/statmod/integration/sdm/SDMShopCatalogEntries.java`
- Produces: deterministic price/quantity selection based on category tier, rarity keywords, and item role

- [ ] **Step 1: Write the pricing model inside the generator**

Replace the current small set of ad hoc `BasePrice * 2` / `* 4` adjustments with explicit tier bands. The script should expose these helpers:

```powershell
function Get-CategoryProfile {
    param([string]$Tab)
    # returns: Tier, BaseMin, BaseMax, Count, CombatBias
}

function Get-RarityWeight {
    param([string]$ItemId)
    # returns: Common, Uncommon, Rare, Epic, Legendary, Boss
}

function Get-PriceForItem {
    param(
        [string]$Tab,
        [string]$ItemId,
        [int]$SeedPrice
    )
    # clamps the final price into the category band and applies rarity bias
}
```

The price model should follow this progression:

```text
Tier 1  ->  100 to 800
Tier 2  ->  800 to 3,000
Tier 3  ->  3,000 to 12,000
Tier 4  ->  12,000 to 30,000
```

Category intent:

- tier 1: Minerais bruts, Fleurs/plantes/bois, Nourriture, Potions et soins, Construction, Mécanismes et Redstone
- tier 2: Lingots et gemmes, Forge et amélioration, Utilitaires, Mobilité et transport, Trophées et décoration
- tier 3: Matériaux avancés, Runes et composants magiques, Magie et parchemins, Armures classiques, Armes à distance, Lances et armes d’hast
- tier 4: Armes légères, Armes lourdes, Armes de Tensura, Armes uniques et légendaires, Armures fantastiques, Armures historiques, Armures magiques, Composants de monstres, Objets rares contrôlés

The important rule is that the highest-impact combat items and rare-controlled items end up in tier 4 even if their raw material origin is lower.

- [ ] **Step 2: Rework the per-category selection so the script still emits the same catalog shape**

Keep the same category names and the same general item selection, but make the price logic deterministic and band-based:

```powershell
$selected.Add([pscustomobject]@{
    Tab=$Tab
    Id=$id
    Price=$price
    Count=$Count
    Potion=$null
})
```

Do not change the emitted `SDMShopCatalog.ShopItem` shape; only the prices and, where needed, stack sizes for the progression economy.

- [ ] **Step 3: Preserve special potion entries and the two potion variants**

Keep the explicit `strong_healing` and `strong_strength` entries and tune them into the potion tier band instead of leaving them as outliers:

```powershell
$selected.Add([pscustomobject]@{ Tab='Potions et soins'; Id='minecraft:potion'; Price=1500; Count=1; Potion='strong_healing' })
$selected.Add([pscustomobject]@{ Tab='Potions et soins'; Id='minecraft:potion'; Price=2000; Count=1; Potion='strong_strength' })
```

Those values may shift a little if the final band model needs it, but they must stay in the consumable tier and not drift into rare-item pricing.

- [ ] **Step 4: Run the generator and inspect the output ordering**

Run:

```powershell
.\tools\generate_sdm_shop_entries.ps1
```

Expected: `src/main/java/tong/statmod/integration/sdm/SDMShopCatalogEntries.java` is regenerated with the same category set and a progression-shaped price curve.

---

### Task 3: Regenerate the catalog artifacts and verify the economy shape

**Files:**
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMShopCatalogEntries.java`
- Modify: `docs/generated/sdm-shop-catalog-by-category.md`
- Modify: `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`

**Interfaces:**
- Consumes: the new tier-driven generator output
- Produces: a stable generated catalog, a human-readable price reference, and passing balance tests

- [ ] **Step 1: Regenerate the source catalog and the human-readable catalog dump**

Run the generator once the pricing model is in place, then refresh the category report so the documented item list matches the generated Java source.

```powershell
.\tools\generate_sdm_shop_entries.ps1
```

If the markdown catalog dump is produced by a separate helper, regenerate it from the same source data and keep it in sync with the Java output.

- [ ] **Step 2: Tighten the existing catalog test around the new bands**

Update `SDMShopCatalogTest` so it verifies:

```java
assertTrue(SDMShopCatalog.items().size() == 699);
assertTrue(find("minecraft:coal").price() <= 200);
assertTrue(find("minecraft:netherite_ingot").price() >= 12000);
assertTrue(find("simplyswords:runic_katana").price() >= 12000);
assertTrue(find("apotheosis:gems/core/dragonfire_spessartite").price() >= 20000);
```

That keeps the highest-value items in the late/boss tier and protects the “solo cannot get rich too early” rule.

- [ ] **Step 3: Run the targeted and full test suites**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopCatalogTest --console=plain
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain
```

Expected: both targeted suites pass after the generator and generated catalog are updated.

- [ ] **Step 4: Run the full mod test suite**

Run:

```powershell
.\gradlew.bat test
```

Expected: no regressions in the rest of the mod, especially the SDM bridge and dungeon integration tests.

- [ ] **Step 5: Commit the balanced catalog**

```powershell
git add tools/generate_sdm_shop_entries.ps1 src/main/java/tong/statmod/integration/sdm/SDMShopCatalogEntries.java src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java src/test/java/tong/statmod/integration/sdm/SDMShopPricingBalanceTest.java docs/generated/sdm-shop-catalog-by-category.md
git commit -m "feat: rebalance SDM shop progression pricing"
```
