# Forge 1.20.1 Automatic XP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Award proportional, rate-limited automatic XP to the 14 non-magical player stats from Forge gameplay events while keeping all magic and Epic Fight dependencies out of the runtime.

**Architecture:** Thin Forge event adapters create normalized immutable actions. A pure reward policy computes proposed awards, a separate player XP-state capability enforces persistent discovery and transient anti-farm rules, and one award service batches mutations and network synchronization. Public item tags and a compatibility guide let Epic Fight addons integrate without Java dependencies.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10 events/capabilities/tags, JUnit Jupiter 5.10.2, Gradle 8.8.

## Global Constraints

- Activate exactly 14 non-magical stats; never automatically award the nine magical stats listed in the design.
- Use the exact formulas, 1,200-tick windows, 200-XP global cap, 25-XP PvP opponent cap, 400-tick Willpower cooldown, and 100-tick Agility cooldown.
- Only real server players in survival or adventure are eligible.
- Send at most one complete stat snapshot per normalized Forge event.
- Persist discovered biome identifiers; keep combos, falls, cooldowns, and rate windows transient.
- Keep Epic Fight, its addons, Iron's Spells, Curios, and all other mods absent from Java imports and mod metadata.
- Use item tags and standard Forge events as the complete addon compatibility contract.
- Preserve all 74 external Iron's Spells addon JARs and their hashes unchanged.

## File map

- `progression/xp/XpActionKind.java`: normalized action vocabulary.
- `progression/xp/XpAction.java`: immutable action values and factories.
- `progression/xp/StatXpAward.java`: proposed stat/amount/reason value.
- `progression/xp/XpRewardPolicy.java`: pure formulas only.
- `progression/xp/RollingXpLimiter.java`: per-stat tick-window accounting.
- `progression/xp/PlayerXpState.java`: discovery, combos, falls, cooldowns, and limits.
- `capability/PlayerXpStateProvider.java`: NBT capability provider.
- `progression/xp/WeaponClassification.java`: classification result.
- `progression/xp/StatItemTags.java`: four public `TagKey<Item>` constants.
- `progression/xp/WeaponClassifier.java`: tag lookup, precedence, and conflict logging.
- `progression/xp/XpAwardResult.java`: accepted awards and changed flag.
- `progression/xp/XpAwardCoordinator.java`: pure batching against domain state.
- `progression/xp/XpAwardService.java`: ServerPlayer capability/network boundary.
- `event/CombatXpEvents.java`: damage, shield, combo, defense, kill adapters.
- `event/ExplorationXpEvents.java`: landing and biome adapters.
- `event/CraftingXpEvents.java`: craft, smelt, and brew adapters.
- `docs/compatibility/epic-fight-addon-xp.md`: addon integration contract.
- `docs/compatibility/epic-fight-addon-matrix.csv`: honest compatibility template.

---

### Task 1: Implement normalized actions and all reward formulas

**Files:**
- Create: `src/main/java/tong/statmod/progression/xp/XpActionKind.java`
- Create: `src/main/java/tong/statmod/progression/xp/XpAction.java`
- Create: `src/main/java/tong/statmod/progression/xp/StatXpAward.java`
- Create: `src/main/java/tong/statmod/progression/xp/XpRewardPolicy.java`
- Test: `src/test/java/tong/statmod/progression/xp/XpRewardPolicyTest.java`

**Interfaces:**
- Produces: `XpAction` factories, `StatXpAward(StatType,int,String)`, and `XpRewardPolicy.awards(XpAction)`.
- Consumers: coordinator and all three Forge event adapters.

- [ ] **Step 1: Write failing formula tests**

Create tests that assert these exact results:

```java
assertEquals(20, only(XpRewardPolicy.awards(XpAction.damage(
        XpActionKind.MELEE_HEAVY, 20.0))).amount());
assertEquals(11, only(XpRewardPolicy.awards(XpAction.damage(
        XpActionKind.MELEE_BLADE, 5.25))).amount());
assertEquals(2, only(XpRewardPolicy.awards(XpAction.combo(3))).amount());
assertEquals(6, only(XpRewardPolicy.awards(XpAction.combo(20))).amount());
assertEquals(7, only(XpRewardPolicy.awards(XpAction.landing(10.8))).amount());
assertEquals(10, only(XpRewardPolicy.awards(XpAction.biome())).amount());
assertEquals(16, only(XpRewardPolicy.awards(XpAction.forging(1561, 1))).amount());
assertEquals(8, only(XpRewardPolicy.awards(XpAction.cooking(4))).amount());
assertEquals(10, only(XpRewardPolicy.awards(XpAction.alchemy(2, 1))).amount());
assertTrue(XpRewardPolicy.awards(XpAction.damage(
        XpActionKind.MELEE_HEAVY, Double.NaN)).isEmpty());
assertTrue(XpRewardPolicy.awards(XpAction.damage(
        XpActionKind.MELEE_HEAVY, 0)).isEmpty());
```

Also assert `XpAction.kill(120, true)` returns Tracking 20 and Intimidation 30,
and that the union of all policy output stat types contains none of:

```java
ARCANE_POWER, CASTING_SPEED, MANA_POOL, ERUDITION, MAGIC_RESISTANCE,
FIRE_AFFINITY, WATER_AFFINITY, EARTH_AFFINITY, AIR_AFFINITY
```

- [ ] **Step 2: Run the test and verify RED**

Run: `.\gradlew.bat test --tests tong.statmod.progression.xp.XpRewardPolicyTest --console=plain`  
Expected: compilation fails because the XP action and policy types do not exist.

- [ ] **Step 3: Implement the normalized model**

Use this exact enum:

```java
public enum XpActionKind {
    MELEE_HEAVY, MELEE_BLADE, PROJECTILE, COMBO, CONTROLLED_LANDING,
    PHYSICAL_DAMAGE_RECEIVED, SHIELD_BLOCKED, HOSTILE_KILL,
    BIOME_DISCOVERY, WILLPOWER_SURVIVAL, EQUIPMENT_CRAFTED,
    FOOD_COOKED, POTION_BREWED
}
```

Implement `XpAction` as:

```java
public record XpAction(
        XpActionKind kind, double magnitude, int quantity, int secondary,
        boolean dangerousTarget, UUID opponentId) {
    public static XpAction damage(XpActionKind kind, double damage) {
        return new XpAction(kind, damage, 0, 0, false, null);
    }
    public static XpAction combo(int length) {
        return new XpAction(XpActionKind.COMBO, 0, length, 0, false, null);
    }
    public static XpAction landing(double distance) {
        return new XpAction(XpActionKind.CONTROLLED_LANDING, distance, 0, 0, false, null);
    }
    public static XpAction kill(double maxHealth, boolean dangerous) {
        return new XpAction(XpActionKind.HOSTILE_KILL, maxHealth, 0, 0, dangerous, null);
    }
    public static XpAction biome() {
        return new XpAction(XpActionKind.BIOME_DISCOVERY, 0, 0, 0, false, null);
    }
    public static XpAction forging(int maxDamage, int count) {
        return new XpAction(XpActionKind.EQUIPMENT_CRAFTED, maxDamage, count, 0, false, null);
    }
    public static XpAction cooking(int count) {
        return new XpAction(XpActionKind.FOOD_COOKED, 0, count, 0, false, null);
    }
    public static XpAction alchemy(int effectCount, int amplifierSum) {
        return new XpAction(XpActionKind.POTION_BREWED, 0, effectCount, amplifierSum, false, null);
    }
    public XpAction withOpponent(UUID id) {
        return new XpAction(kind, magnitude, quantity, secondary, dangerousTarget, id);
    }
}
```

`StatXpAward` is an immutable record. `XpRewardPolicy.awards` uses a switch and
these helpers:

```java
private static int damageXp(double value) { return clamp((int) Math.ceil(value * 2), 1, 20); }
private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
private static boolean positiveFinite(double value) { return Double.isFinite(value) && value > 0; }
```

Map action kinds exactly as specified: heavy→Brute Force, blade→Blade
Technique, projectile→Precision, combo→Rapidité, landing→Agility, received
damage→Physical Resistance, blocked→Physical Endurance, biome→Keen Senses,
survival→Willpower, crafting→Forging, cooking→Cooking, potion→Alchemy. A hostile
kill always proposes Tracking and additionally proposes Intimidation only when
`dangerousTarget` is true. Reject invalid magnitude/quantity/effect inputs
before clamping.

- [ ] **Step 4: Run formula tests and verify GREEN**

Run: `.\gradlew.bat test --tests tong.statmod.progression.xp.XpRewardPolicyTest --console=plain`  
Expected: all formula and deferred-magic assertions pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp src/test/java/tong/statmod/progression/xp/XpRewardPolicyTest.java
git commit -m "feat: define automatic XP reward policy"
```

### Task 2: Add persistent discovery and transient anti-farm state

**Files:**
- Create: `src/main/java/tong/statmod/progression/xp/RollingXpLimiter.java`
- Create: `src/main/java/tong/statmod/progression/xp/PlayerXpState.java`
- Create: `src/main/java/tong/statmod/capability/PlayerXpStateProvider.java`
- Modify: `src/main/java/tong/statmod/capability/StatCapabilities.java`
- Modify: `src/main/java/tong/statmod/event/PlayerStatsEvents.java`
- Test: `src/test/java/tong/statmod/progression/xp/PlayerXpStateTest.java`

**Interfaces:**
- Produces: `acceptXp`, `recordMeleeHit`, cooldown methods, fall methods,
  biome discovery methods, NBT serialization, and `PLAYER_XP_STATE` capability.
- Consumers: coordinator, combat events, exploration events.

- [ ] **Step 1: Write failing state tests**

Test all of the following with explicit assertions:

```java
assertEquals(200, state.acceptXp(BRUTE_FORCE, 250, 100, null));
assertEquals(0, state.acceptXp(BRUTE_FORCE, 1, 101, null));
assertEquals(1, state.acceptXp(BRUTE_FORCE, 1, 1300, null));

UUID opponent = UUID.randomUUID();
assertEquals(25, fresh.acceptXp(BLADE_TECHNIQUE, 40, 0, opponent));
assertEquals(0, fresh.acceptXp(BLADE_TECHNIQUE, 1, 1, opponent));

PlayerXpState atomic = new PlayerXpState();
assertEquals(200, atomic.acceptXp(PRECISION, 200, 0, null));
assertEquals(0, atomic.acceptXp(PRECISION, 10, 1, opponent));
assertEquals(10, atomic.acceptXp(PRECISION, 10, 1200, opponent));

assertEquals(1, state.recordMeleeHit(10));
assertEquals(2, state.recordMeleeHit(50));
assertEquals(1, state.recordMeleeHit(91));

assertTrue(state.tryWillpower(400));
assertFalse(state.tryWillpower(799));
assertTrue(state.tryWillpower(800));
assertTrue(state.tryAgility(100));
assertFalse(state.tryAgility(199));
assertTrue(state.tryAgility(200));
```

Record a biome, serialize, deserialize into a second state, and assert that the
biome remains discovered while combo/cooldown/window state is reset. Assert
`copyPersistentFrom` copies only biome identifiers.

- [ ] **Step 2: Run state tests and verify RED**

Run: `.\gradlew.bat test --tests tong.statmod.progression.xp.PlayerXpStateTest --console=plain`  
Expected: compilation fails because state and limiter types do not exist.

- [ ] **Step 3: Implement rolling limits and state**

`RollingXpLimiter` stores `EnumMap<StatType,ArrayDeque<Entry>>`, where
`Entry(long tick,int amount)` is private. `remaining(stat,tick,cap)` removes
entries satisfying `tick - entry.tick() >= 1200`, sums the remaining amounts,
and returns `max(0,cap-used)`. `record(stat,amount,tick)` records only positive
accepted amounts.

`PlayerXpState` contains one global limiter and a
`Map<OpponentStatKey,ArrayDeque<WindowEntry>>` for the 25-XP PvP limit. Its
`acceptXp` computes the remaining global and opponent budgets without recording,
accepts the minimum of the requested amount and both remaining budgets, then
records that same accepted amount in both windows atomically. A rejected global
award therefore cannot consume an opponent budget. Use records for both private
map keys/entries.

Use `lastMeleeHitTick = Long.MIN_VALUE`; continue a combo when
`tick - lastMeleeHitTick <= 40`, otherwise reset to one. Cooldown methods accept
when `tick >= nextAllowedTick` and then set `nextAllowedTick` to tick plus 400 or
100 respectively.

Fall state methods are:

```java
void observeAirborne(float fallDistance)
void markFallDamage()
OptionalDouble finishLanding()
void clearFall()
```

`finishLanding` returns the recorded peak only when it is at least five and no
fall damage was marked, then always clears fall state.

Persist biome strings under a `ListTag` named `discovered_biomes`. Parse with
`ResourceLocation.tryParse`, ignore invalid entries, and write identifiers in
sorted order for deterministic NBT.

- [ ] **Step 4: Add and wire the second capability**

Add `Capability<PlayerXpState> PLAYER_XP_STATE` to `StatCapabilities` and
register `PlayerXpState.class`. Implement `PlayerXpStateProvider` with the same
`ICapabilitySerializable<CompoundTag>` pattern as `PlayerStatsProvider`.

In `PlayerStatsEvents.attach`, attach both providers under distinct resource
locations `player_stats` and `player_xp_state`, registering both invalidation
listeners. In `clone`, copy `PlayerStats` fully and call
`targetXp.copyPersistentFrom(sourceXp)` for XP state. Do not copy transient
fields.

- [ ] **Step 5: Run state tests and compile Forge wiring**

Run: `.\gradlew.bat test --tests tong.statmod.progression.xp.PlayerXpStateTest --console=plain`  
Expected: all limiter, cooldown, combo, fall, NBT, and copy assertions pass.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: both Forge capability types compile.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp/PlayerXpState.java src/main/java/tong/statmod/progression/xp/RollingXpLimiter.java src/main/java/tong/statmod/capability src/main/java/tong/statmod/event/PlayerStatsEvents.java src/test/java/tong/statmod/progression/xp/PlayerXpStateTest.java
git commit -m "feat: track automatic XP limits and discoveries"
```

### Task 3: Publish item tags and deterministic classification

**Files:**
- Create: `src/main/java/tong/statmod/progression/xp/WeaponClassification.java`
- Create: `src/main/java/tong/statmod/progression/xp/StatItemTags.java`
- Create: `src/main/java/tong/statmod/progression/xp/WeaponClassifier.java`
- Create: `src/main/resources/data/statmod/tags/items/heavy_weapons.json`
- Create: `src/main/resources/data/statmod/tags/items/blade_weapons.json`
- Create: `src/main/resources/data/statmod/tags/items/precision_weapons.json`
- Create: `src/main/resources/data/statmod/tags/items/forgeable_equipment.json`
- Test: `src/test/java/tong/statmod/progression/xp/WeaponClassifierTest.java`
- Test: `src/test/java/tong/statmod/progression/xp/StatItemTagResourcesTest.java`

**Interfaces:**
- Produces: `WeaponClassifier.classify(ItemStack,boolean)` and four public tags.
- Consumers: combat and crafting event adapters.

- [ ] **Step 1: Write failing classification/resource tests**

Assert pure precedence through `WeaponClassifier.resolve(heavy,blade,precision,projectile)`:

```java
assertEquals(PRECISION, resolve(true, false, false, true));
assertEquals(AMBIGUOUS, resolve(true, true, false, false));
assertEquals(HEAVY, resolve(true, false, false, false));
assertEquals(BLADE, resolve(false, true, false, false));
assertEquals(PRECISION, resolve(false, false, true, false));
assertEquals(UNCLASSIFIED, resolve(false, false, false, false));
```

Load all four JSON resources from the test classloader, parse them with Gson,
assert `replace` is false, and assert these values exist:

```text
heavy: #minecraft:axes
blade: #minecraft:swords
precision: minecraft:bow, minecraft:crossbow, minecraft:trident
forgeable: #minecraft:axes, #minecraft:swords, #minecraft:pickaxes,
           #minecraft:shovels, #minecraft:hoes, #minecraft:trimmable_armor,
           minecraft:bow, minecraft:crossbow, minecraft:trident, minecraft:shield
```

- [ ] **Step 2: Run tests and verify RED**

Run: `.\gradlew.bat test --tests '*WeaponClassifierTest' --tests '*StatItemTagResourcesTest' --console=plain`  
Expected: compilation/resource failures because classifier and tag files do not exist.

- [ ] **Step 3: Implement tags, classifier, and JSON**

Create tags with `ItemTags.create(ResourceLocation.fromNamespaceAndPath(
StatMod.MOD_ID, path))`. `classify` reads `stack.is(tag)` and delegates to the
pure `resolve` method. Projectile is always Precision. Heavy+blade is Ambiguous
unless projectile took precedence. Use a concurrent set of item registry IDs
to log each ambiguous item at most once; return Ambiguous after logging.

Each JSON is exactly:

```json
{ "replace": false, "values": ["#minecraft:axes"] }
```

with the appropriate complete value arrays listed in Step 1.

- [ ] **Step 4: Run classifier/resource tests and verify GREEN**

Run: `.\gradlew.bat test --tests '*WeaponClassifierTest' --tests '*StatItemTagResourcesTest' --console=plain`  
Expected: precedence and all shipped resource assertions pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp src/main/resources/data/statmod/tags/items src/test/java/tong/statmod/progression/xp
git commit -m "feat: classify addon equipment with public tags"
```

### Task 4: Batch awards and synchronize once

**Files:**
- Create: `src/main/java/tong/statmod/progression/xp/XpAwardResult.java`
- Create: `src/main/java/tong/statmod/progression/xp/XpAwardCoordinator.java`
- Create: `src/main/java/tong/statmod/progression/xp/XpAwardService.java`
- Test: `src/test/java/tong/statmod/progression/xp/XpAwardCoordinatorTest.java`
- Test: `src/test/java/tong/statmod/progression/xp/XpAwardServiceContractTest.java`

**Interfaces:**
- Produces: `XpAwardCoordinator.apply(PlayerStats,PlayerXpState,List<XpAction>,long)` and `XpAwardService.award(ServerPlayer,List<XpAction>,long)`.
- Consumers: all Forge adapters.

- [ ] **Step 1: Write failing coordinator tests**

Apply one dangerous kill action and assert Tracking and Intimidation both change,
the accepted-award map contains both entries, and `changed()` is true. Apply an
invalid action and assert no changes. Exhaust a limiter, assert partial
acceptance. Set a target stat to level 100 and assert no mutation is reported.

The source contract test reads `XpAwardService.java` and asserts exactly one
textual occurrence of `StatNetwork.sendSnapshot(player)` inside the successful
batch path and no Epic Fight/Tensura imports.

- [ ] **Step 2: Run tests and verify RED**

Run: `.\gradlew.bat test --tests '*XpAwardCoordinatorTest' --tests '*XpAwardServiceContractTest' --console=plain`  
Expected: compilation fails because coordinator/service types do not exist.

- [ ] **Step 3: Implement coordinator and service**

`XpAwardResult` is `record XpAwardResult(boolean changed,
Map<StatType,Integer> accepted)`, defensively copying into an unmodifiable
`EnumMap`.

The coordinator flattens policy awards for every action and groups proposed
amounts by `(stat, opponentId)` so PvP attribution is never lost. It skips
level-100 stats, calls `state.acceptXp` per group, merges accepted amounts back
by stat for the result, mutates positive accepted amounts, and reports only
values whose before and after `StatValue` differ.

`XpAwardService.award` returns false for fake, creative, spectator, or
non-server players. It resolves both capabilities, calls the coordinator once,
and calls `StatNetwork.sendSnapshot(player)` exactly once when changed. Missing
either capability returns false without mutation.

- [ ] **Step 4: Run coordinator/service tests and verify GREEN**

Run: `.\gradlew.bat test --tests '*XpAwardCoordinatorTest' --tests '*XpAwardServiceContractTest' --console=plain`  
Expected: batching, partial acceptance, max-level behavior, and source contract pass.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp src/test/java/tong/statmod/progression/xp
git commit -m "feat: batch and synchronize automatic XP awards"
```

### Task 5: Adapt combat events

**Files:**
- Create: `src/main/java/tong/statmod/event/CombatXpEvents.java`
- Create: `src/main/java/tong/statmod/progression/xp/CombatEligibility.java`
- Test: `src/test/java/tong/statmod/progression/xp/CombatEligibilityTest.java`
- Test: `src/test/java/tong/statmod/event/CombatXpEventsContractTest.java`

**Interfaces:**
- Consumes: classifier, XP state, action factories, and award service.
- Produces: offense, combo, defense, block, Willpower, Tracking, and Intimidation event adapters.

- [ ] **Step 1: Write failing combat policy/contracts**

Test the pure damage-source flags via a small `CombatDamageProfile` value so
fire, magic-like bypass, drowning, fall, self, and ownerless damage are rejected
while melee, projectile-owner, opponent, and owned explosion profiles pass.

The source contract asserts `CombatXpEvents` subscribes to
`LivingDamageEvent`, `ShieldBlockEvent`, and `LivingDeathEvent`, uses
`EventPriority.LOWEST`, calls `XpAwardService.award`, and contains no formula
constants such as `* 2` or the stat enum names.

- [ ] **Step 2: Run tests and verify RED**

Run: `.\gradlew.bat test --tests '*CombatEligibilityTest' --tests '*CombatXpEventsContractTest' --console=plain`  
Expected: missing combat eligibility/event types.

- [ ] **Step 3: Implement combat eligibility**

`eligibleTarget(player,target)` rejects dead/invulnerable entities, armor stands,
the acting player, allied entities, and tamables owned by the player.
`physicalProfile(DamageSource,player)` uses `DamageTypeTags` to exclude fire,
fall, drowning, freezing, lightning, and bypass-armor magic-like damage; it
requires a non-self `source.getEntity()` or an owned explosion/projectile.

- [ ] **Step 4: Implement combat adapters**

At LOWEST priority, `LivingDamageEvent`:

- if `source.getEntity()` is an eligible `ServerPlayer` attacker, create one
  projectile action when `source.is(IS_PROJECTILE)`; otherwise classify the
  main-hand item and create heavy or blade action;
- for a qualifying direct melee hit, obtain `PlayerXpState`, call
  `recordMeleeHit(gameTime)`, and append a combo action only at length 3+;
- call the award service once with the offense action list, attaching the victim
  UUID when the victim is a player;
- if the victim is an eligible `ServerPlayer`, mark fall damage for fall sources;
  for a valid physical profile create Physical Resistance and, when
  `health - finalDamage > 0` and `(health-finalDamage)/maxHealth <= 0.30`, append
  Willpower only when `tryWillpower(gameTime)` accepts; award the defensive list once.

`ShieldBlockEvent` awards positive `getBlockedDamage()` through one shield action.

`LivingDeathEvent` awards only when the dead entity implements `Enemy` and kill
credit resolves to an eligible `ServerPlayer`. Set dangerous when
`entity.getType().is(Tags.EntityTypes.BOSSES)` or max health is at least 100.

- [ ] **Step 5: Run combat tests and compile Forge APIs**

Run: `.\gradlew.bat test --tests '*CombatEligibilityTest' --tests '*CombatXpEventsContractTest' --console=plain`  
Expected: all policy and wiring assertions pass.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: Forge 47.4.10 damage/shield/death APIs compile.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/tong/statmod/event/CombatXpEvents.java src/main/java/tong/statmod/progression/xp/CombatEligibility.java src/test/java/tong/statmod
git commit -m "feat: award combat and survival stat XP"
```

### Task 6: Adapt exploration events

**Files:**
- Create: `src/main/java/tong/statmod/event/ExplorationXpEvents.java`
- Test: `src/test/java/tong/statmod/event/ExplorationXpEventsContractTest.java`

**Interfaces:**
- Consumes: fall/biome state and award service.
- Produces: controlled-landing Agility and first-biome Keen Senses XP.

- [ ] **Step 1: Write failing source contract**

Assert the source uses `TickEvent.PlayerTickEvent`, requires `Phase.END`, samples
biomes only when `gameTime % 20 == 0`, uses `finishLanding`, `tryAgility`,
`hasDiscoveredBiome`, and calls `markBiomeDiscovered` only after an accepted
award. Assert it clears fall state for creative, spectator, elytra, and dimension
change cases.

- [ ] **Step 2: Run test and verify RED**

Run: `.\gradlew.bat test --tests '*ExplorationXpEventsContractTest' --console=plain`  
Expected: exploration event class is absent.

- [ ] **Step 3: Implement exploration adapter**

On server END player ticks, resolve XP state. When airborne and eligible, call
`observeAirborne(player.fallDistance)`. On the first grounded tick, call
`finishLanding`; if a distance exists and `tryAgility(gameTime)` succeeds, award
one landing action. Clear without reward during creative/spectator flight,
elytra flight, or when eligibility fails.

Every 20 ticks resolve the current biome key with
`level.getBiome(player.blockPosition()).unwrapKey()`. If its location is not yet
discovered, award one biome action; mark the ID only when `award` returns true.

Subscribe to `PlayerChangedDimensionEvent` and clear the new player's fall state.

- [ ] **Step 4: Run contract and compile**

Run: `.\gradlew.bat test --tests '*ExplorationXpEventsContractTest' --console=plain`  
Expected: source contract passes.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: tick, biome, and dimension APIs compile.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/event/ExplorationXpEvents.java src/test/java/tong/statmod/event/ExplorationXpEventsContractTest.java
git commit -m "feat: award exploration and agility XP"
```

### Task 7: Adapt crafting, cooking, and brewing events

**Files:**
- Create: `src/main/java/tong/statmod/event/CraftingXpEvents.java`
- Test: `src/test/java/tong/statmod/event/CraftingXpEventsContractTest.java`

**Interfaces:**
- Consumes: forgeable tag, action factories, PotionUtils, and award service.
- Produces: Forging, Cooking, and Alchemy event adapters.

- [ ] **Step 1: Write failing source contract**

Assert the class subscribes to `PlayerEvent.ItemCraftedEvent`,
`PlayerEvent.ItemSmeltedEvent`, and the exact Forge 47.4.10 type
`net.minecraftforge.event.brewing.PlayerBrewedPotionEvent`. Assert crafting
checks `StatItemTags.FORGEABLE_EQUIPMENT`, smelting checks `isEdible`, and
brewing reads `PotionUtils.getMobEffects`.

- [ ] **Step 2: Run test and verify RED**

Run: `.\gradlew.bat test --tests '*CraftingXpEventsContractTest' --console=plain`  
Expected: crafting event class is absent.

- [ ] **Step 3: Implement crafting adapter**

For a real server player:

- crafted stack in forgeable tag → `XpAction.forging(stack.getMaxDamage(), stack.getCount())`;
- edible smelted stack → `XpAction.cooking(stack.getCount())`;
- brewed potion with non-empty `PotionUtils.getMobEffects(stack)` → action with
  effect count and `sum(MobEffectInstance.getAmplifier())`.

Call the award service once per Forge event and use the server level game time.
Do not duplicate eligibility checks or formula constants in this adapter.

- [ ] **Step 4: Run contract and compile exact APIs**

Run: `.\gradlew.bat test --tests '*CraftingXpEventsContractTest' --console=plain`  
Expected: source contract passes.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: all three Forge 47.4.10 event signatures compile.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/event/CraftingXpEvents.java src/test/java/tong/statmod/event/CraftingXpEventsContractTest.java
git commit -m "feat: award crafting and provisioning XP"
```

### Task 8: Document Epic Fight addons and verify the complete slice

**Files:**
- Create: `docs/compatibility/epic-fight-addon-xp.md`
- Create: `docs/compatibility/epic-fight-addon-matrix.csv`
- Modify: `README.md`
- Modify: `scripts/verify-clean-foundation.ps1`
- Create: `src/test/java/tong/statmod/AutomaticXpContractTest.java`

**Interfaces:**
- Consumes: all preceding tasks.
- Produces: public addon contract, matrix template, JAR guard, and final regression checks.

- [ ] **Step 1: Write failing documentation/resource contract**

Assert the guide contains every tag ID, `replace: false`, all four allowed matrix
statuses, client and dedicated-server procedures, and the statement that an
untested addon is not claimed compatible. Parse the CSV header and assert it is
exactly:

```text
addon,version,weapon_tags,melee_xp,combo_xp,projectile_xp,crafting_xp,client_test,server_test,status,notes
```

Scan main Java sources and assert no `epicfight`, `irons_spellbooks`, or
`tensura` dependency text. Assert the policy can emit exactly the 14 active stat
types and none of the nine deferred types.

- [ ] **Step 2: Run contract and verify RED**

Run: `.\gradlew.bat test --tests tong.statmod.AutomaticXpContractTest --console=plain`  
Expected: documentation and matrix assertions fail because files do not exist.

- [ ] **Step 3: Write the compatibility guide and matrix**

The guide must include complete datapack JSON examples for all four tags, folder
paths, precedence/conflict rules, reload instructions, expected XP commands for
inspection, client and dedicated-server checklist, compatibility report fields,
and definitions of `untested`, `compatible`, `partial`, and `incompatible`.

The CSV contains the exact header plus one non-addon example row clearly labeled
`EXAMPLE_DO_NOT_SHIP_AS_RESULT` with status `untested`; do not mark any real addon
compatible without a performed test.

Update README with the 14 active/9 deferred split and links to both documents.

- [ ] **Step 4: Extend JAR inspection**

Add these required entries to `verify-clean-foundation.ps1`:

```text
tong/statmod/progression/xp/XpRewardPolicy.class
tong/statmod/progression/xp/PlayerXpState.class
tong/statmod/progression/xp/XpAwardService.class
tong/statmod/event/CombatXpEvents.class
tong/statmod/event/ExplorationXpEvents.class
tong/statmod/event/CraftingXpEvents.class
data/statmod/tags/items/heavy_weapons.json
data/statmod/tags/items/blade_weapons.json
data/statmod/tags/items/precision_weapons.json
data/statmod/tags/items/forgeable_equipment.json
```

- [ ] **Step 5: Run complete fresh verification**

Run:

```powershell
.\gradlew.bat clean build --console=plain
& '.\scripts\verify-clean-foundation.ps1' -Mode After
git diff --check
git status --short
```

Expected: build succeeds with zero failed tests; guard reports 74/74 JARs;
diff check is empty; status lists only Task 8 changes.

- [ ] **Step 6: Commit documentation and guards**

```powershell
git add docs/compatibility README.md scripts/verify-clean-foundation.ps1 src/test/java/tong/statmod/AutomaticXpContractTest.java
git commit -m "docs: publish Epic Fight XP compatibility contract"
```

- [ ] **Step 7: Verify from committed tree**

Run:

```powershell
.\gradlew.bat clean build --console=plain
& '.\scripts\verify-clean-foundation.ps1' -Mode After
git status --short
```

Expected: build and guard succeed; worktree status is empty.

## Self-review

- Spec coverage: Task 1 covers exact formulas and deferred magic; Task 2 covers
  persistence, combos, falls, cooldowns, and both rolling limits; Task 3 covers
  public classification; Task 4 covers batching and one sync; Tasks 5-7 cover
  every Forge source; Task 8 covers Epic Fight documentation, the honest matrix,
  JAR contents, all tests, and 74-addon preservation.
- API verification: Forge 47.4.10 contains `LivingDamageEvent`,
  `ShieldBlockEvent.getBlockedDamage`, `LivingDeathEvent`, nested crafted/smelted
  player events, `TickEvent.PlayerTickEvent`, and
  `net.minecraftforge.event.brewing.PlayerBrewedPotionEvent.getStack`.
- Placeholder scan: no unfinished implementation marker or undefined task is
  used. Every produced type is named before a later task consumes it.
- Type consistency: all event adapters emit the Task 1 `XpAction`; all awards go
  through the Task 4 service; both capability tokens live in `StatCapabilities`;
  all item resources use the Task 3 tag IDs.
