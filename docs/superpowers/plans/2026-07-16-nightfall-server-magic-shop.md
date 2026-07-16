# Nightfall Server Magic Shop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and deploy a deterministic SDMShop `default` catalog containing every enabled Iron/addon spell level, a demanding resource-sale economy, and a one-time 1,000-coin starting balance.

**Architecture:** Keep all balance and pricing rules in dependency-free Java classes, then place the Minecraft/SDM adapters in focused integration classes. The catalog is generated after Forge registries and SDMShop have initialized, with stable UUIDs, a pre-write backup, and a JSON report; an operator command regenerates the same catalog safely.

**Tech Stack:** Java 17, Forge 47.4.4 for Minecraft 1.20.1, Iron's Spells 3.16.2, SDMShop 7.2.2, SDMEconomy 2.2.0, JUnit 5.10.2, Gradle 8/ForgeGradle 6.

## Global Constraints

- Shop ID is exactly `default`; it is permanent, searchable, and grouped by registered magic school.
- Include every enabled Iron/addon spell and every valid level immediately; scrolls are buy-only.
- Prices are rarity base × level multiplier: Common 500, Uncommon 1,200, Rare 3,000, Epic 7,500, Legendary 20,000; multipliers 1–10 are 1, 2, 4, 7, 11, 16, 22, 29, 37, 46.
- Levels above 10 use `max(46, round(0.5 × level²))`; unknown rarities use Common.
- New players receive exactly 1,000 `sdm_coin` once; existing balances and dungeon currencies are never reset.
- Resource entries are sell-only and reject damaged, renamed, or custom-NBT stacks; emeralds, gear, scrolls, curios, containers, potions, and finished products are excluded.
- Regeneration is deterministic, backs up the previous catalog, emits a report, and skips malformed spell data without aborting the remaining catalog.
- Dedicated-server validation must reach Forge ready state with no shop registry/deserialization errors.

---

## File structure

- `MagicShopPricing.java`: pure rarity and level price policy.
- `MagicShopSaleCatalog.java`: immutable curated resource bundles and values.
- `MagicShopIds.java`: deterministic tab/entry UUID generation.
- `StrictItemEntryType.java`: SDM item matcher configured to require exact clean stacks.
- `MagicShopGenerator.java`: registry scan, tabs, entries, backup, report, and persistence.
- `MagicShopEvents.java`: server lifecycle generation and first-login balance grant.
- `MagicShopCommands.java`: operator-only `/statmod shop regenerate` entry point.
- `SDMEconomyBridge.java`: version-2.2.0 reflection bridge for balance, save, and sync.

### Task 1: Pure pricing and sale policy

**Files:**
- Create: `src/main/java/tong/statmod/integration/sdmshop/MagicShopPricing.java`
- Create: `src/main/java/tong/statmod/integration/sdmshop/MagicShopSaleCatalog.java`
- Test: `src/test/java/tong/statmod/integration/sdmshop/MagicShopPricingTest.java`
- Test: `src/test/java/tong/statmod/integration/sdmshop/MagicShopSaleCatalogTest.java`

**Interfaces:**
- Produces: `long MagicShopPricing.scrollPrice(String rarity, int level)` and `List<SaleOffer> MagicShopSaleCatalog.offers()`.

- [ ] **Step 1: Write failing price tests** covering every rarity base, levels 1–10, unknown rarity fallback, and levels 11–20 using `max(46, Math.round(0.5D * level * level))`.
- [ ] **Step 2: Run** `./gradlew test --tests '*MagicShopPricingTest'` and expect failures because the class does not exist.
- [ ] **Step 3: Implement the exact price policy:**

```java
public static long scrollPrice(String rarity, int level) {
    if (level < 1) throw new IllegalArgumentException("level must be positive");
    long base = switch (rarity.toLowerCase(Locale.ROOT)) {
        case "uncommon" -> 1_200L;
        case "rare" -> 3_000L;
        case "epic" -> 7_500L;
        case "legendary" -> 20_000L;
        default -> 500L;
    };
    long multiplier = level <= 10 ? LEVEL_MULTIPLIERS[level - 1]
            : Math.max(46L, Math.round(0.5D * level * level));
    return Math.multiplyExact(base, multiplier);
}
```

- [ ] **Step 4: Write and pass sale-catalog tests** asserting exact bundles, positive values, unique IDs, no emeralds, and no buy offers. Include 64 cobblestone→8, 16 oak logs→20, 32 wheat→25, 16 coal→40, 8 raw copper→60, 8 raw iron→80, 4 raw gold→100, 16 rotten flesh→20, 16 bones→45, 16 blaze rods→70, and 1 diamond→180.
- [ ] **Step 5: Commit** with `git commit -m "feat: define demanding magic shop economy"`.

### Task 2: Deterministic identifiers and strict item matching

**Files:**
- Create: `src/main/java/tong/statmod/integration/sdmshop/MagicShopIds.java`
- Create: `src/main/java/tong/statmod/integration/sdmshop/StrictItemEntryType.java`
- Test: `src/test/java/tong/statmod/integration/sdmshop/MagicShopIdsTest.java`
- Test: `src/test/java/tong/statmod/integration/sdmshop/StrictItemEntryTypeContractTest.java`

**Interfaces:**
- Produces: `UUID tab(String schoolId)`, `UUID scroll(String spellId, int level)`, `UUID sale(String offerId)` and an `ItemEntryType` with `strictNbt=true`, `ignoreDamage=false`.

- [ ] **Step 1: Write failing deterministic-ID tests** asserting same input→same UUID, different level→different UUID, and namespace separation among tab/scroll/sale IDs.
- [ ] **Step 2: Run** `./gradlew test --tests '*MagicShopIdsTest'` and expect missing-class failure.
- [ ] **Step 3: Implement UUID generation:**

```java
private static UUID stable(String kind, String value) {
    return UUID.nameUUIDFromBytes(("statmod:magic_shop:" + kind + ":" + value)
            .getBytes(StandardCharsets.UTF_8));
}
```

- [ ] **Step 4: Implement strict matching as an SDM subclass:**

```java
public final class StrictItemEntryType extends ItemEntryType {
    public StrictItemEntryType(ShopEntry entry, ItemStack stack) {
        super(entry, stack);
        this.strictNbt = true;
        this.ignoreDamage = false;
    }
}
```

- [ ] **Step 5: Commit** with `git commit -m "feat: add deterministic strict shop entries"`.

### Task 3: Current SDMEconomy 2.2.0 bridge and starting grant

**Files:**
- Modify: `src/main/java/tong/statmod/integration/sdm/SDMEconomyBridge.java`
- Modify: `src/main/java/tong/statmod/stats/PlayerStats.java`
- Test: `src/test/java/tong/statmod/integration/sdm/SDMEconomyBridgeContractTest.java`
- Test: `src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java`

**Interfaces:**
- Produces: `boolean ensureStartingBalance(ServerPlayer player, long amount)`, `boolean hasReceivedStartingBalance()`, and `void markStartingBalanceReceived()`.

- [ ] **Step 1: Add failing NBT round-trip tests** for `shopStartingBalanceReceived`, including clone/copy preservation.
- [ ] **Step 2: Run** `./gradlew test --tests '*PlayerStatsNbtTest'` and expect the new assertions to fail.
- [ ] **Step 3: Add the boolean to `PlayerStats`, serialize as `shopStartingBalanceReceived`, deserialize with false default, and copy it in `copyFrom`.**
- [ ] **Step 4: Replace obsolete reflection targets** (`economyData.*`, `FDP_cfa`) with SDMEconomy 2.2.0 targets:

```java
Class<?> api = Class.forName("net.sixik.sdmeconomy.api.EconomyAPI");
Object data = api.getMethod("getPlayerCurrencyServerData").invoke(null);
Method add = data.getClass().getMethod("addCurrencyValue", Player.class, String.class, double.class);
Object result = add.invoke(data, player, "sdm_coin", (double) amount);
```

  Resolve and call the public SDMEconomy sync/save methods after mutation. Return false without setting the player marker if any API call reports failure.
- [ ] **Step 5: Implement `ensureStartingBalance`** so it grants 1,000 only when the persistent player marker is false, then marks and synchronizes stats only after the currency mutation succeeds.
- [ ] **Step 6: Run** `./gradlew test --tests '*SDMEconomyBridgeContractTest' --tests '*PlayerStatsNbtTest'` and expect all tests to pass.
- [ ] **Step 7: Commit** with `git commit -m "fix: integrate SDM Economy 2.2.0 balances"`.

### Task 4: SDMShop and SDMEconomy build/runtime requirements

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Modify: `src/main/resources/META-INF/mods.toml`
- Test: `src/test/java/tong/statmod/RequiredShopDependenciesContractTest.java`

**Interfaces:**
- Consumes: SDMShop 7.2.2 and SDMEconomy 2.2.0.
- Produces: compile-visible SDM APIs and mandatory Forge metadata.

- [ ] **Step 1: Add failing metadata tests** requiring `sdmshop` `[1.20.1-7.2.2,)` and `sdmeconomy` `[2.2.0,)`, both `mandatory=true`, `side="BOTH"`.
- [ ] **Step 2: Add the reproducible compile dependency** `compileOnly fg.deobf("maven.modrinth:sdm-shop:${sdmshop_version}")`; keep `sdmshop_version=1.20.1-7.2.2` and `sdmeconomy_version=2.2.0` in `gradle.properties`. SDMEconomy stays reflection-only because it does not publish a required compile artifact through SDMShop's Modrinth metadata.
- [ ] **Step 3: Add the two mandatory dependency blocks to `mods.toml` and run** `./gradlew compileJava processResources` expecting `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit** with `git commit -m "build: require SDM shop economy stack"`.

### Task 5: Spell registry catalog generation

**Files:**
- Create: `src/main/java/tong/statmod/integration/sdmshop/MagicShopGenerator.java`
- Create: `src/main/java/tong/statmod/integration/sdmshop/MagicShopReport.java`
- Test: `src/test/java/tong/statmod/integration/sdmshop/MagicShopGeneratorContractTest.java`

**Interfaces:**
- Consumes: `SpellRegistry.getEnabledSpells()`, `ISpellContainer.createScrollContainer`, `SDMShopServer.Instance()`, pricing, sale catalog, IDs.
- Produces: `GenerationResult regenerate(MinecraftServer server)` containing spell, scroll-entry, sale-entry, skipped, and backup counts.

- [ ] **Step 1: Write failing contract tests** asserting enabled-registry scan, valid inclusive min/max levels, `Items.SCROLL`, exact NBT container creation, school grouping, stable sorting, `default` shop clearing, Buy scrolls, Sell resources, backup, save, and report.
- [ ] **Step 2: Create scroll stacks exactly as Iron expects:**

```java
ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
ISpellContainer.createScrollContainer(spell, level, scroll);
```

- [ ] **Step 3: Sort spells by school registry ID, spell registry ID, then level.** For each school create `ShopTab(shop, MagicShopIds.tab(schoolId))`; for each spell level create `ShopEntry(shop, stableEntryId, tabId, new MoneySellerType(price))`, attach `StrictItemEntryType`, set count 1, price, and type `Buy`.
- [ ] **Step 4: Append curated sale tabs and entries** with type `Sell`, exact bundle count/value, `MoneySellerType`, and strict item entry types.
- [ ] **Step 5: Before mutation, copy existing shop files** to `config/statmod/shop-backups/<UTC timestamp>/`; never touch SDMEconomy player data. Clear/create `default`, populate it, call `saveShopToFile`, then write `config/statmod/shop-generation-report.json` through a temporary sibling followed by atomic move.
- [ ] **Step 6: Catch malformed spell exceptions per spell** and add ID/error to the report; only fatal I/O errors fail the generation as a whole.
- [ ] **Step 7: Run** `./gradlew test --tests '*MagicShopGeneratorContractTest'` and expect pass.
- [ ] **Step 8: Commit** with `git commit -m "feat: generate complete Iron spell shop"`.

### Task 6: Lifecycle, command, and notifications

**Files:**
- Create: `src/main/java/tong/statmod/integration/sdmshop/MagicShopEvents.java`
- Create: `src/main/java/tong/statmod/integration/sdmshop/MagicShopCommands.java`
- Modify: `src/main/java/tong/statmod/event/PlayerStatsEvents.java`
- Test: `src/test/java/tong/statmod/integration/sdmshop/MagicShopEventsContractTest.java`
- Test: `src/test/java/tong/statmod/integration/sdmshop/MagicShopCommandsContractTest.java`

**Interfaces:**
- Produces: automatic generation on `ServerStartedEvent`, one-time grant on `PlayerLoggedInEvent`, and permission-level-4 `/statmod shop regenerate`.

- [ ] **Step 1: Write failing wiring tests** for server-start generation, login grant of exactly 1,000, level-4 command requirement, success counts, and failure logging.
- [ ] **Step 2: Implement lifecycle handlers** guarded by `ModList` checks for `sdmshop`, `sdmeconomy`, and `irons_spellbooks`; schedule login grant on the server thread after SDMEconomy player initialization.
- [ ] **Step 3: Register `MagicShopCommands` from the existing `RegisterCommandsEvent`.** The command calls the same generator and reports counts; it never edits balances.
- [ ] **Step 4: Set `config/SDMShop/sdmshop-common.snbt` to default shop `default`, keybind enabled, notifications enabled, and admin debug disabled after validation.**
- [ ] **Step 5: Run** `./gradlew test --tests '*MagicShop*' --tests '*PlayerStats*'` and expect pass.
- [ ] **Step 6: Commit** with `git commit -m "feat: wire magic shop lifecycle and command"`.

### Task 7: Full verification and clean deployment

**Files:**
- Modify: `docs/forge-1.20.1-server-validation.md`
- Deploy: `C:/Users/El Hadji/AppData/Roaming/.minecraft/versions/NightfallCraft - The Casket of Reveries (2)/mods/statmod-0.1.0+1.20.1.jar`
- Deploy: `C:/Users/El Hadji/Downloads/serveur/The Casket of Reveries Server 2.2.9.1/mods/statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Produces: verified client/server artifact and reproducible validation evidence.

- [ ] **Step 1: Run unit and contract verification:** `./gradlew clean test` and require zero failures.
- [ ] **Step 2: Run production build:** `./gradlew build` and require `BUILD SUCCESSFUL`; record SHA-256 of the JAR.
- [ ] **Step 3: Back up each previous StatMod JAR, remove only older StatMod duplicates, and copy the exact built JAR** to the Nightfall client and server mod folders. Do not re-enable the dedicated-server-incompatible MobsPlus JAR.
- [ ] **Step 4: Start the dedicated server with its normal Forge launcher** and wait for `Done`; verify logs contain SDMShop/SDMEconomy initialization, catalog counts, and no registry/deserialization errors.
- [ ] **Step 5: Stop gracefully, inspect** `config/SDMShop/shops`, `config/statmod/shop-generation-report.json`, and the backup folder; confirm the default shop has tabs and entries.
- [ ] **Step 6: Re-run generation and compare report/catalog hashes** to prove determinism while confirming the SDMEconomy player-data hash is unchanged.
- [ ] **Step 7: Document exact test count, catalog counts, server ready time, deployed SHA-256, MobsPlus disabled status, and rollback paths.**
- [ ] **Step 8: Commit** with `git commit -m "docs: verify Nightfall magic shop deployment"`.

### Task 8: Final branch verification

**Files:**
- Verify only: all changed files.

- [ ] **Step 1: Run** `git diff --check`, `./gradlew clean test build`, and require all commands to exit 0.
- [ ] **Step 2: Run** `git status --short` and confirm only intentional deployment-external paths remain outside Git.
- [ ] **Step 3: Review the generated report against the installed server addon list** and confirm every enabled spell namespace is represented or explicitly listed as skipped with a reason.
- [ ] **Step 4: Commit any final evidence correction** with `git commit -m "chore: finalize magic shop validation"`; do not create an empty commit.
