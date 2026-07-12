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
