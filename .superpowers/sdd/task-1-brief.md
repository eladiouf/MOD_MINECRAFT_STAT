Task 1: Lock the balance rules in tests before changing prices

Files:
- Modify: `src/test/java/tong/statmod/integration/sdm/SDMShopCatalogTest.java`
- Create: `src/test/java/tong/statmod/integration/sdm/SDMShopPricingBalanceTest.java`

Interfaces:
- Consumes: `SDMShopCatalog.tabs()`, `SDMShopCatalog.items()`
- Consumes: `SDMShopCatalog.ShopItem.tab()`, `itemId()`, `price()`, `count()`, `potionId()`
- Produces: category band assertions, anchor-item ordering assertions, and a strict “no price regression” test suite

Requirements:
- Add a focused pricing test class with three checks:
  1. earlyCategoriesStayCheapAndHighTierTabsStayExpensive()
  2. keyProgressionAnchorsIncreaseMonotonically()
  3. noCategoryContainsOutOfBandPricesOrForbiddenIds()
- The assertions must initially fail against the current generated catalog if prices have not been reworked yet.
- Add helper methods:

```java
private void assertPriceBand(String tab, int minInclusive, int maxInclusive) { ... }
private void assertPriceLessThan(String cheaperItemId, String pricierItemId) { ... }
private SDMShopCatalog.ShopItem find(String itemId) { ... }
```

- Keep the tests self-contained and based only on `SDMShopCatalog.items()`.

Test command:
`.\gradlew.bat test --tests tong.statmod.integration.sdm.SDMShopPricingBalanceTest --console=plain`

