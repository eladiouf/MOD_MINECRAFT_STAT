# Task 1 Report: Lock the balance rules in tests before changing prices

## Summary

Added a new focused pricing regression test suite at `src/test/java/tong/statmod/integration/sdm/SDMShopPricingBalanceTest.java`.

Did not keep any changes in `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`, because that file was already dirty before this task and the new pricing suite was sufficient on its own.

## What changed

- Added `SDMShopPricingBalanceTest` with the three required checks:
  - `earlyCategoriesStayCheapAndHighTierTabsStayExpensive()`
  - `keyProgressionAnchorsIncreaseMonotonically()`
  - `noCategoryContainsOutOfBandPricesOrForbiddenIds()`
- Added the required helper methods:
  - `assertPriceBand(String tab, int minInclusive, int maxInclusive)`
  - `assertPriceLessThan(String cheaperItemId, String pricierItemId)`
  - `find(String itemId)`
- Kept the helper logic fully self-contained against `SDMShopCatalog.items()`.

## Test runs

### 1) Required red check

Command:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain
```

Result: failed as expected.

Observed failure reason:

- `earlyCategoriesStayCheapAndHighTierTabsStayExpensive()`
- `minecraft:netherite_ingot above band for Lingots et gemmes: 12000`

This is the intended failure mode for Task 1: the test compiles and runs, and the catalog fails on pricing balance rather than on missing items or broken helper logic.

### 2) Existing SDM catalog suite sanity check

Command:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopCatalogTest --console=plain
```

Result: passed.

Purpose: confirm the new pricing test addition did not break the existing SDM catalog suite.

### 3) Required re-run after helper completion

Command:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain
```

Result: failed again as expected with the same balance assertion path.

## Files changed

- `src/test/java/tong/statmod/integration/sdm/SDMShopPricingBalanceTest.java`
- `.superpowers/sdd/task-1-report.md`

## Self-review

- Scope stayed test-only.
- No generator or runtime catalog files were touched.
- The new tests use only `SDMShopCatalog.items()` lookups for helper behavior, as required.
- The red state is meaningful: current pricing violates the locked band rules, specifically in `Lingots et gemmes`.
- I intentionally avoided committing `SDMShopCatalogTest.java` because it already contained unrelated uncommitted changes before this task.

## Concerns

- No blocking concerns.
- One transient Gradle file-lock collision occurred when two test commands were run in parallel against the same `build/test-results` directory; re-running sequentially resolved it. This did not affect the final verification results.

---

## Task 1 review-fix addendum: lock the 1000-item catalog target

## Summary

Updated `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java` to replace the loose catalog-size checks with an explicit 1000-item assertion while preserving the exact category-name assertion.

## What changed

- Added `EXPECTED_ITEM_COUNT = 1000`.
- Replaced the former size-range assertion in `exposesDetailedCategoriesAndControlledVolume()` with `assertEquals(EXPECTED_ITEM_COUNT, SDMShopCatalog.items().size())`.
- Replaced the later lower-bound-only catalog-size assertion in `allCatalogEntriesHaveValidValuesAndKnownTabs()` with the same exact 1000-item assertion.
- Left the existing `EXPECTED_TABS` exact category-name assertion intact.

## Focused test runs

### 1) Catalog target lock

Command:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopCatalogTest --console=plain
```

Result: failed.

Observed failure reason:

- `exposesDetailedCategoriesAndControlledVolume()`: `expected: <1000> but was: <699>`
- `allCatalogEntriesHaveValidValuesAndKnownTabs()`: `expected: <1000> but was: <699>`

### 2) Pricing balance regression suite

Command:

```powershell
.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain
```

Result: failed.

Observed failure reason:

- `earlyCategoriesStayCheapAndHighTierTabsStayExpensive()`
- `minecraft:netherite_ingot above band for Lingots et gemmes: 12000`

## Notes

- A prior attempt to launch both focused Gradle commands concurrently produced an unrelated `build/test-results/test/binary/output.bin` file-lock error. The final verification above was re-run sequentially and is the authoritative result.

## Files changed by this review fix

- `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`
- `.superpowers/sdd/task-1-report.md`
