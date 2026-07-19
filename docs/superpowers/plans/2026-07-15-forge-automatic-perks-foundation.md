# Forge Automatic Perks Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 21 server-authoritative passive perks that activate automatically at levels 25, 50, and 75, augment the seven mature attribute bridges, and appear as a non-interactive list in the native stats screen.

**Architecture:** A dependency-free catalog and resolver derive active perk IDs directly from `PlayerStats`; no unlock state is persisted. One bonus aggregator feeds the existing idempotent attribute refresh, while the existing full snapshot carries a bounded active-ID list to the client.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, JUnit 5.10.2, Iron's Spells 3.16.2, Epic Fight 20.14.17, Puffish Attributes 0.8.2, Gradle 8.8.

## Global Constraints

- Work only on branch `forge-1.20.1` in `.worktrees/forge-1.20.1`.
- No tree, nodes, points, purchase, respec, manual unlock, or stored perk flags.
- No elemental affinities or retired affinity identifiers.
- Perks are derived exclusively from authoritative server stat levels.
- Preserve one existing stable modifier UUID per attribute target; never stack perk modifiers.
- Preserve Iron's Spells' native Curios casting unchanged.
- Puffish Attributes is an attribute provider, not a perk owner.
- Deploy only after full tests and required-provider GameTest smoke pass.

---

### Task 1: Pure automatic-perk catalog and resolver

**Files:**
- Create: `src/main/java/tong/statmod/perks/AutomaticPerkEffect.java`
- Create: `src/main/java/tong/statmod/perks/AutomaticPerkRequirement.java`
- Create: `src/main/java/tong/statmod/perks/AutomaticPerkDefinition.java`
- Create: `src/main/java/tong/statmod/perks/AutomaticPerkCatalog.java`
- Create: `src/main/java/tong/statmod/perks/AutomaticPerkResolver.java`
- Create: `src/test/java/tong/statmod/perks/AutomaticPerkCatalogTest.java`
- Create: `src/test/java/tong/statmod/perks/AutomaticPerkResolverTest.java`

**Interfaces:**
- Produces: `AutomaticPerkCatalog.definitions() : List<AutomaticPerkDefinition>`.
- Produces: `AutomaticPerkCatalog.byId(String) : Optional<AutomaticPerkDefinition>`.
- Produces: `AutomaticPerkResolver.active(Map<StatType, Integer>) : List<AutomaticPerkDefinition>`.
- Consumes: existing `StatType` and `StatProgress.MAX_LEVEL` only.

- [ ] **Step 1: Write failing catalog and resolver tests**

Create tests asserting:

```java
assertEquals(21, AutomaticPerkCatalog.definitions().size());
assertEquals(21, AutomaticPerkCatalog.definitions().stream()
        .map(AutomaticPerkDefinition::id).distinct().count());
assertEquals(List.of(), ids(levels(StatType.RAPIDITE, 24)));
assertEquals(List.of("statmod:rapidite_25"),
        ids(levels(StatType.RAPIDITE, 25)));
assertEquals(List.of("statmod:rapidite_25", "statmod:rapidite_50"),
        ids(levels(StatType.RAPIDITE, 50)));
assertEquals(List.of("statmod:rapidite_25", "statmod:rapidite_50",
        "statmod:rapidite_75"), ids(levels(StatType.RAPIDITE, 75)));
assertTrue(AutomaticPerkCatalog.byId("statmod:fire_affinity_25").isEmpty());
assertThrows(IllegalArgumentException.class, () -> new AutomaticPerkRequirement(
        StatType.RAPIDITE, 0));
```

Also construct a test-only definition requiring both Rapidité 25 and Agility 15,
then assert `AutomaticPerkResolver.active(List.of(definition), levels)` rejects
24/15 and 25/14 but accepts 25/15.

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.perks.AutomaticPerkCatalogTest" --tests "tong.statmod.perks.AutomaticPerkResolverTest" --console=plain
```

Expected: test compilation fails because `tong.statmod.perks` does not exist.

- [ ] **Step 3: Implement immutable validated domain types**

Use these exact public shapes:

```java
public enum AutomaticPerkEffect {
    RAPIDITE_ATTACK_SPEED,
    AGILITY_MOVEMENT,
    ENDURANCE_STAMINA,
    ARCANE_SPELL_POWER,
    CASTING_SPEED_REDUCTIONS,
    MANA_CAPACITY_REGEN,
    MAGIC_RESISTANCE
}

public record AutomaticPerkRequirement(StatType stat, int minimumLevel) {
    public AutomaticPerkRequirement {
        Objects.requireNonNull(stat, "stat");
        if (minimumLevel < 1 || minimumLevel > StatProgress.MAX_LEVEL) {
            throw new IllegalArgumentException("minimumLevel must be between 1 and 100");
        }
    }
}

public record AutomaticPerkDefinition(String id, int order,
        List<AutomaticPerkRequirement> requirements,
        AutomaticPerkEffect effect, double defaultAmount) {
    public AutomaticPerkDefinition {
        if (id == null || !id.matches("statmod:[a-z0-9_]+")) {
            throw new IllegalArgumentException("invalid perk id");
        }
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        if (requirements.isEmpty() || requirements.stream().map(AutomaticPerkRequirement::stat)
                .distinct().count() != requirements.size()) {
            throw new IllegalArgumentException("requirements must be non-empty and unique");
        }
        Objects.requireNonNull(effect, "effect");
        if (!Double.isFinite(defaultAmount) || defaultAmount < 0.0 || defaultAmount > 0.25) {
            throw new IllegalArgumentException("invalid default amount");
        }
    }
}
```

Implement `AutomaticPerkResolver.active(List<AutomaticPerkDefinition>, Map<StatType,Integer>)`
by filtering definitions whose every requirement is met and sorting by `order`, then
make `active(Map)` delegate to the built-in catalog.

- [ ] **Step 4: Build the exact 21-entry catalog**

Generate three ordered definitions per entry below, with suffix/requirement
`25`, `50`, and `75`:

```java
entry("rapidite", StatType.RAPIDITE,
        AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED, 0.02);
entry("agility", StatType.AGILITY,
        AutomaticPerkEffect.AGILITY_MOVEMENT, 0.02);
entry("physical_endurance", StatType.PHYSICAL_ENDURANCE,
        AutomaticPerkEffect.ENDURANCE_STAMINA, 0.04);
entry("arcane_power", StatType.ARCANE_POWER,
        AutomaticPerkEffect.ARCANE_SPELL_POWER, 0.03);
entry("casting_speed", StatType.CASTING_SPEED,
        AutomaticPerkEffect.CASTING_SPEED_REDUCTIONS, 0.02);
entry("mana_pool", StatType.MANA_POOL,
        AutomaticPerkEffect.MANA_CAPACITY_REGEN, 0.03);
entry("magic_resistance", StatType.MAGIC_RESISTANCE,
        AutomaticPerkEffect.MAGIC_RESISTANCE, 0.02);
```

Validate duplicate IDs once in the static initializer and expose an immutable
ordered list plus immutable lookup map.

- [ ] **Step 5: Run focused tests and commit**

Run the Task 1 command again. Expected: `BUILD SUCCESSFUL` and all boundary,
malformed-definition, multi-requirement, ordering, and affinity-exclusion tests pass.

```powershell
git add src/main/java/tong/statmod/perks src/test/java/tong/statmod/perks
git commit -m "feat: define automatic stat perks"
```

---

### Task 2: Configurable perk bonuses in idempotent attribute refresh

**Files:**
- Create: `src/main/java/tong/statmod/perks/AutomaticPerkBonuses.java`
- Modify: `src/main/java/tong/statmod/config/StatModServerConfig.java`
- Modify: `src/main/java/tong/statmod/effects/MobilityAttributeTarget.java`
- Modify: `src/main/java/tong/statmod/effects/StaminaAttributeTarget.java`
- Modify: `src/main/java/tong/statmod/effects/MagicAttributeTarget.java`
- Modify: `src/main/java/tong/statmod/effects/PlayerAttributeEffects.java`
- Create: `src/test/java/tong/statmod/perks/AutomaticPerkBonusesTest.java`
- Modify: `src/test/java/tong/statmod/config/StatModServerConfigContractTest.java`
- Modify: `src/test/java/tong/statmod/effects/PlayerAttributeEffectsContractTest.java`

**Interfaces:**
- Consumes: `AutomaticPerkResolver.active(Map<StatType,Integer>)` from Task 1.
- Produces: `AutomaticPerkBonuses.from(PlayerStats) : AutomaticPerkBonuses`.
- Produces: `AutomaticPerkBonuses.amount(AutomaticPerkEffect) : double`.
- Produces: seven `StatModServerConfig.*PerMilestone()` accessors.

- [ ] **Step 1: Write failing aggregation and wiring tests**

Assert pure totals:

```java
assertEquals(0.0, bonusesAt(StatType.RAPIDITE, 24)
        .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED));
assertEquals(0.02, bonusesAt(StatType.RAPIDITE, 25)
        .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED));
assertEquals(0.04, bonusesAt(StatType.RAPIDITE, 50)
        .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED));
assertEquals(0.06, bonusesAt(StatType.RAPIDITE, 75)
        .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED), 1.0e-9);
```

Contract tests require seven config keys under `automaticPerks`, bounds
`0.0, 0.25`, `AutomaticPerkBonuses.from(stats)`, and exactly one call to
`replaceModifier` per existing target loop. They reject `addPermanentModifier`
and any second perk UUID.

- [ ] **Step 2: Run tests and verify RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.perks.AutomaticPerkBonusesTest" --tests "tong.statmod.config.StatModServerConfigContractTest" --tests "tong.statmod.effects.PlayerAttributeEffectsContractTest" --console=plain
```

Expected: compilation fails because `AutomaticPerkBonuses` is absent.

- [ ] **Step 3: Add bounded server configuration**

Under `builder.push("automaticPerks")`, define:

```java
rapiditeAttackSpeedPerMilestone = 0.02
agilityMovementPerMilestone = 0.02
enduranceStaminaPerMilestone = 0.04
arcaneSpellPowerPerMilestone = 0.03
castingSpeedReductionsPerMilestone = 0.02
manaCapacityRegenPerMilestone = 0.03
magicResistancePerMilestone = 0.02
```

Each uses `defineInRange(key, defaultValue, 0.0, 0.25)` and has a public static
double accessor with the camel-cased key name.

- [ ] **Step 4: Implement aggregation and combine it with existing targets**

`AutomaticPerkBonuses.from(PlayerStats)` converts `stats.snapshot()` to a level
map, resolves active definitions, and sums the configured amount selected by
effect. Use `Math.min(0.75, sum)` as a defensive aggregate bound.

Add `AutomaticPerkEffect perkEffect()` to each target enum and change each
`amount(...)` method to accept `AutomaticPerkBonuses bonuses`, returning:

```java
existingContinuousAmount + bonuses.amount(perkEffect)
```

For Casting Speed, Mana Pool, Agility, and Endurance, both related targets use
the same perk effect amount. In `PlayerAttributeEffects.refresh`, calculate
`AutomaticPerkBonuses bonuses = AutomaticPerkBonuses.from(stats);` exactly once
and pass it to all target amount calls. Keep the existing target UUID and
remove-then-add transient modifier flow.

- [ ] **Step 5: Run focused and regression tests, then commit**

Run the Task 2 command plus:

```powershell
.\gradlew.bat test --tests "tong.statmod.effects.*" --console=plain
```

Expected: all perk, config, scaling, target, and idempotence contract tests pass.

```powershell
git add src/main/java/tong/statmod/perks/AutomaticPerkBonuses.java src/main/java/tong/statmod/config/StatModServerConfig.java src/main/java/tong/statmod/effects src/test/java/tong/statmod/perks/AutomaticPerkBonusesTest.java src/test/java/tong/statmod/config/StatModServerConfigContractTest.java src/test/java/tong/statmod/effects
git commit -m "feat: apply automatic perk bonuses"
```

---

### Task 3: Bounded snapshot synchronization and non-interactive P-screen list

**Files:**
- Modify: `src/main/java/tong/statmod/network/StatsSnapshotMessage.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Modify: `src/main/java/tong/statmod/client/ClientStatsState.java`
- Modify: `src/main/java/tong/statmod/client/ClientStatsCache.java`
- Create: `src/main/java/tong/statmod/client/stats/ActivePerkPresentation.java`
- Modify: `src/main/java/tong/statmod/client/stats/StatsScreenModel.java`
- Modify: `src/main/java/tong/statmod/client/stats/StatsOverviewScreen.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/test/java/tong/statmod/network/StatsSnapshotMessageTest.java`
- Modify: `src/test/java/tong/statmod/client/stats/StatsScreenModelTest.java`
- Create: `src/test/java/tong/statmod/client/stats/AutomaticPerkLanguageTest.java`

**Interfaces:**
- `StatsSnapshotMessage(Map<StatType,StatValue>, List<String> activePerkIds)`.
- `ClientStatsCache.replace(Map<StatType,StatValue>, List<String>)`.
- `ClientStatsState(long, Map<StatType,StatValue>, List<String>)`.
- `StatsScreenModel.activePerks() : List<ActivePerkPresentation>`.

- [ ] **Step 1: Write failing snapshot/model/language tests**

Tests require:

```java
assertEquals(List.of("statmod:rapidite_25"), decoded.activePerkIds());
assertEquals(21, StatsSnapshotMessage.MAX_PERKS);
assertEquals("perk.statmod.rapidite_25", presentation.nameKey());
assertEquals("perk.statmod.rapidite_25.description", presentation.descriptionKey());
```

The codec test feeds 22 IDs and asserts only 21 are retained. It feeds an
unknown ID and asserts it is omitted. Language tests require name and description
keys for every definition in both locale files and reject affinity text.

- [ ] **Step 2: Run tests and verify RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.network.StatsSnapshotMessageTest" --tests "tong.statmod.client.stats.StatsScreenModelTest" --tests "tong.statmod.client.stats.AutomaticPerkLanguageTest" --console=plain
```

Expected: test compilation fails because snapshot/state/model perk accessors are absent.

- [ ] **Step 3: Extend the protocol with a bounded canonical ID list**

Set `StatModRuntime.NETWORK_PROTOCOL` to `"5"`. `StatsSnapshotMessage.from(stats)`
uses `AutomaticPerkResolver.active(levelMap)` and maps definitions to IDs.

After stat entries, encode:

```java
buffer.writeVarInt(message.activePerkIds().size());
message.activePerkIds().forEach(id -> buffer.writeUtf(id, 64));
```

Decode at most `MAX_PERKS = 21`, consume every declared bounded entry, retain only
IDs known by `AutomaticPerkCatalog.byId`, remove duplicates while preserving catalog
order, and reject a declared count below 0 or above 21 before allocating.

Update the channel handler to call
`ClientStatsCache.replace(message.values(), message.activePerkIds())` and update
state/cache constructors with defensive immutable copies.

- [ ] **Step 4: Add presentation model and compact list**

`ActivePerkPresentation.from(definition)` returns localization keys derived from
the path after `statmod:`. `StatsScreenModel.from` maps the server-provided IDs
through the catalog and exposes the immutable ordered list.

In `StatsOverviewScreen`, reserve 58 pixels above the card viewport. Render:

```text
Perks actifs: <localized name>, <localized name>, ...
```

Wrap names across at most three lines; if more entries remain, render
`+N autres` / `+N more`. Hovering that region shows each visible perk's localized
description. Do not add buttons, mouse handlers, nodes, currencies, or unlock packets.

Add exact names/descriptions for all 21 IDs plus:

```json
"screen.statmod.stats.active_perks": "Active perks",
"screen.statmod.stats.more_perks": "+%s more"
```

and French equivalents `Perks actifs` and `+%s autres`.

- [ ] **Step 5: Run focused tests and commit**

Run the Task 3 command and all network/client tests. Expected: the protocol,
catalog filtering, presentation order, and both locale contracts pass.

```powershell
git add src/main/java/tong/statmod/network src/main/java/tong/statmod/StatModRuntime.java src/main/java/tong/statmod/client src/main/resources/assets/statmod/lang src/test/java/tong/statmod/network src/test/java/tong/statmod/client
git commit -m "feat: show active automatic perks"
```

---

### Task 4: Documentation, full verification, smoke, and deployment

**Files:**
- Modify: `README.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Modify: `src/test/java/tong/statmod/SupportedRuntimeContractTest.java`
- Deploy: `build/libs/statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes: all Tasks 1–3 behavior.
- Produces: one verified client JAR and a backup of the previous JAR.

- [ ] **Step 1: Update user/runtime documentation and its contract**

Document the 21 automatic perks, 25/50/75 thresholds, seven covered stats,
derived activation/deactivation, no tree/points/affinities, non-interactive `P`
list, and Puffish Attributes' provider-only role. Add contract assertions for
those exact facts.

- [ ] **Step 2: Run full clean verification**

```powershell
.\gradlew.bat clean test build --console=plain
.\gradlew.bat test --rerun-tasks --console=plain
```

Expected: both commands exit 0, all XML test suites report zero failures/errors,
and `build/libs/statmod-0.1.0+1.20.1.jar` exists.

- [ ] **Step 3: Inspect the artifact**

Require exactly one entry for each new production class, no duplicate archive
entry, and zero entries under `io/redspace/ironsspellbooks/`, `yesman/epicfight/`,
or `net/puffish/attributesmod/`. Record the SHA-256 hash.

- [ ] **Step 4: Run required-provider Forge smoke**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: exit 0 and `OK required-provider Forge GameTest server smoke` with no
fatal, crash, mixin, registry, or mod-loading rejection.

- [ ] **Step 5: Back up and deploy safely**

Confirm no Minecraft `java.exe`/`javaw.exe` process is active. Copy the existing
client `statmod-*.jar` into:

```text
C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\<timestamp>-before-automatic-perks
```

Then copy only the built STAT Mod JAR to
`C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods`.
Require exactly one `statmod-*.jar`, preserve the total mod count, and require
source/target SHA-256 equality.

- [ ] **Step 6: Commit documentation and preserve branch**

```powershell
git add README.md docs/compatibility/forge-1.20.1-supported-runtime.md src/test/java/tong/statmod/SupportedRuntimeContractTest.java
git commit -m "docs: document automatic stat perks"
git status --short
```

Expected: clean worktree. Keep branch `forge-1.20.1` and its worktree as-is;
do not merge, push, or remove it.

## Completion audit

- Search this plan for forbidden placeholders; only quoted audit terminology may match.
- Confirm every specification section maps to a task above.
- Confirm all later method signatures exactly match Task 1–3 interfaces.
- Confirm no perk purchase state, Puffish Skills dependency, affinity ID, or
  Iron's Spells cast hook was introduced.
