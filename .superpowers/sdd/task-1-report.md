# Task 1 Report: lock the balance rules in tests before changing prices

## Summary

Updated `src/test/java/tong/statmod/integration/sdm/SDMShopPricingBalanceTest.java` so the pricing-policy checks are test-owned and the third required test now enforces the remaining category price bands instead of only checking positive values. Also updated `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java` to lock the exact 26-category set and the 1000-item target.

## Final implementation

- Kept the three required test methods:
  - `earlyCategoriesStayCheapAndHighTierTabsStayExpensive()`
  - `keyProgressionAnchorsIncreaseMonotonically()`
  - `noCategoryContainsOutOfBandPricesOrForbiddenIds()`
- Kept the three required helper methods:
  - `assertPriceBand(String tab, int minInclusive, int maxInclusive)`
  - `assertPriceLessThan(String cheaperItemId, String pricierItemId)`
  - `find(String itemId)`
- Added a local `FORBIDDEN_ID_PARTS` list and a private local `isForbiddenItemId(String itemId)` helper so forbidden-ID assertions no longer call `SDMShopCatalog.isForbiddenItemId(...)`.
- Added a local `REMAINING_CATEGORY_PRICE_BANDS` map so `noCategoryContainsOutOfBandPricesOrForbiddenIds()` now checks the progression-policy bands for the remaining categories:
  - tier 1 style bands: `Fleurs, plantes et bois`, `Nourriture`, `Construction`, `Mécanismes et Redstone`
  - tier 2 style bands: `Forge et amélioration`, `Utilitaires`, `Mobilité et transport`, `Trophées et décoration`
  - tier 3 style bands: `Matériaux avancés`, `Runes et composants magiques`, `Magie et parchemins`, `Armures classiques`, `Armes à distance`, `Lances et armes d'hast`
  - tier 4 style bands: `Armes légères`, `Armes lourdes`, `Armes de Tensura`, `Armures fantastiques`, `Armures historiques`, `Composants de monstres`
- Left generator/runtime code untouched.

## Files changed

- `src/test/java/tong/statmod/integration/sdm/SDMShopPricingBalanceTest.java`
- `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`
- `.superpowers/sdd/task-1-report.md`

## Focused verification

### Pricing balance suite

Command:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain
```

Result: failed, as expected for the current un-rebalanced catalog.

Observed failures:

- `earlyCategoriesStayCheapAndHighTierTabsStayExpensive()`
  - `minecraft:netherite_ingot above band for Lingots et gemmes: 12000`
- `noCategoryContainsOutOfBandPricesOrForbiddenIds()`
  - `iceandfire:armor_red_helmet below band for Armures fantastiques: 4000`

Observed pass:

- `keyProgressionAnchorsIncreaseMonotonically()`

### Existing SDM catalog suite

Command:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopCatalogTest --console=plain
```

Result: failed.

Observed failures:

- `exposesDetailedCategoriesAndControlledVolume()`
  - `expected: <1000> but was: <699>`
- `allCatalogEntriesHaveValidValuesAndKnownTabs()`
  - `expected: <1000> but was: <699>`

## Notes

- An initial attempt to run both focused Gradle commands in parallel reproduced the existing `build/test-results/test/binary/output.bin` file-lock issue. The sequential reruns above are the authoritative verification results.

## Concerns

- `SDMShopCatalogTest` still fails on the exact 1000-item expectation from the prior Task 1 review fix because the current catalog remains at 699 items.
- The updated pricing test is intentionally red against the current catalog until the generator/catalog rebalance work is done.
