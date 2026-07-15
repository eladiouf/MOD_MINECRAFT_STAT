# Forge 1.20.1 Iron's Spells Core Attribute Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retire the four unused affinity statistics and make Arcane Power, Casting Speed, Mana Pool, and Magic Resistance drive the pinned Iron's Spells 3.16.2 attributes.

**Architecture:** Require Iron's Spells 3.16.2+ at runtime while resolving its six public attributes only by registry ID. Extend the existing idempotent transient-modifier service, use server-configured linear curves, and migrate the canonical roster from 23/six to 19/five without importing Iron classes or creating a second mana pool.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, Gradle 8.8, JUnit 5, Iron's Spells 3.16.2 runtime registry IDs.

## Global Constraints

- Work only in branch `forge-1.20.1` and its existing isolated worktree.
- Use test-first red/green cycles for every behavior change.
- The canonical roster is exactly 19 stats in five non-empty families.
- Player-stat schema becomes 2; schema-1 affinity NBT is ignored while retained stats survive.
- Never import `io.redspace.ironsspellbooks` classes or add an Iron compile/runtime dependency.
- Never write current mana, refill mana, or create a STAT Mod mana capability.
- Use stable unique UUIDs and `AttributeModifier.Operation.MULTIPLY_BASE`.
- `mods.toml` requires `irons_spellbooks` version `[3.16.2,)`, ordered `AFTER`, on `BOTH` sides.
- An unexpectedly missing registry attribute is still skipped safely.
- Do not add school-specific progression or recreate affinities under another name.
- Preserve Epic Fight, Puffish Attributes, ParCool, and every user/launcher JAR during deployment.

---

### Task 1: Retire Affinities and Migrate the Roster Schema

**Files:**
- Modify: `src/main/java/tong/statmod/stats/StatType.java`
- Modify: `src/main/java/tong/statmod/stats/StatFamily.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Modify: `src/test/java/tong/statmod/stats/StatTypeTest.java`
- Modify: `src/test/java/tong/statmod/stats/PlayerStatsTest.java`
- Modify: `src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java`
- Modify: `src/test/java/tong/statmod/StatModRuntimeTest.java`
- Modify: `src/test/java/tong/statmod/client/stats/StatsScreenModelTest.java`

**Interfaces:**
- Consumes: enum-backed `PlayerStats` initialization and defensive NBT loader.
- Produces: `StatType.values()` with 19 entries, `StatFamily.values()` with five entries, `PLAYER_STATS_SCHEMA == 2`.

- [ ] **Step 1: Write the failing roster and migration assertions**

Update the tests to assert the new contract:

```java
assertEquals(19, StatType.values().length);
assertEquals(19, ids.size());
assertEquals(5, families.size());
assertEquals(5, StatFamily.values().length);
assertTrue(StatType.fromId("fire_affinity").isEmpty());
assertTrue(StatType.fromId("water_affinity").isEmpty());
assertTrue(StatType.fromId("earth_affinity").isEmpty());
assertTrue(StatType.fromId("air_affinity").isEmpty());
assertEquals(19, new PlayerStats().snapshot().size());
assertEquals(2, StatModRuntime.PLAYER_STATS_SCHEMA);
assertEquals(5, StatsScreenModel.from(new ClientStatsState(0, Map.of())).families().size());
```

Add this schema-1 migration case to `PlayerStatsNbtTest`:

```java
@Test
void ignoresRetiredSchemaOneAffinitiesAndPreservesKnownStats() {
    CompoundTag root = new CompoundTag();
    root.putInt("schema", 1);
    CompoundTag entries = new CompoundTag();
    CompoundTag agility = new CompoundTag();
    agility.putInt("level", 17);
    agility.putInt("xp", 12);
    entries.put("agility", agility);
    CompoundTag retired = new CompoundTag();
    retired.putInt("level", 99);
    retired.putInt("xp", 3);
    entries.put("fire_affinity", retired);
    root.put("stats", entries);

    PlayerStats loaded = new PlayerStats();
    loaded.deserializeNbt(root);

    assertEquals(new StatValue(17, 12), loaded.get(StatType.AGILITY));
    assertEquals(19, loaded.snapshot().size());
    assertEquals(2, PlayerStats.serializedSchema(loaded.serializeNbt()));
    assertFalse(loaded.serializeNbt().getCompound("stats").contains("fire_affinity"));
}
```

- [ ] **Step 2: Run the focused tests and confirm RED**

Run:

```powershell
.\gradlew.bat test --tests tong.statmod.stats.StatTypeTest --tests tong.statmod.stats.PlayerStatsTest --tests tong.statmod.stats.PlayerStatsNbtTest --tests tong.statmod.StatModRuntimeTest --tests tong.statmod.client.stats.StatsScreenModelTest --console=plain
```

Expected: failures reporting 23 vs 19, six vs five, schema 1 vs 2, and affinity IDs still present.

- [ ] **Step 3: Remove the four enum constants and empty family**

Delete these constants from `StatType`:

```java
FIRE_AFFINITY
WATER_AFFINITY
EARTH_AFFINITY
AIR_AFFINITY
```

Delete this constant from `StatFamily`:

```java
ELEMENTAL_SPECIALIZATION
```

Set the schema constant exactly:

```java
public static final int PLAYER_STATS_SCHEMA = 2;
```

Do not add migration branches to `PlayerStats.deserializeNbt`; its current loop over known `StatType` values is the intended safe migration.

- [ ] **Step 4: Run the focused tests and confirm GREEN**

Run the command from Step 2.

Expected: `BUILD SUCCESSFUL` and all roster/migration tests pass.

- [ ] **Step 5: Commit the roster migration**

```powershell
git add src/main/java/tong/statmod/stats src/main/java/tong/statmod/StatModRuntime.java src/test/java/tong/statmod/stats src/test/java/tong/statmod/StatModRuntimeTest.java src/test/java/tong/statmod/client/stats/StatsScreenModelTest.java
git commit -m "feat: retire elemental affinity stats"
```

---

### Task 2: Add Bounded Magical Attribute Configuration

**Files:**
- Modify: `src/main/java/tong/statmod/config/StatModServerConfig.java`
- Modify: `src/test/java/tong/statmod/config/StatModServerConfigContractTest.java`

**Interfaces:**
- Consumes: Forge `ForgeConfigSpec.DoubleValue` and current server config registration.
- Produces: six public getters ending in `BonusAt100()` for magic target scaling.

- [ ] **Step 1: Add failing source-contract assertions**

Append these assertions:

```java
assertTrue(config.contains("builder.push(\"magic\")"));
assertTrue(config.contains("arcanePowerSpellPowerBonusAt100\", 1.00, 0.0, 10.0"));
assertTrue(config.contains("castingSpeedCastTimeBonusAt100\", 0.30, 0.0, 0.90"));
assertTrue(config.contains("castingSpeedCooldownBonusAt100\", 0.20, 0.0, 0.90"));
assertTrue(config.contains("manaPoolCapacityBonusAt100\", 2.00, 0.0, 20.0"));
assertTrue(config.contains("manaPoolRegenBonusAt100\", 0.50, 0.0, 10.0"));
assertTrue(config.contains("magicResistanceBonusAt100\", 0.50, 0.0, 0.90"));
```

- [ ] **Step 2: Run the contract test and confirm RED**

```powershell
.\gradlew.bat test --tests tong.statmod.config.StatModServerConfigContractTest --console=plain
```

Expected: assertions fail because the `magic` section is absent.

- [ ] **Step 3: Define the six values and exact getters**

Add six `DoubleValue` fields, define them inside `builder.push("magic")`, and expose:

```java
public static double arcanePowerSpellPowerBonusAt100() { return ARCANE_POWER_SPELL_POWER_BONUS_AT_100.get(); }
public static double castingSpeedCastTimeBonusAt100() { return CASTING_SPEED_CAST_TIME_BONUS_AT_100.get(); }
public static double castingSpeedCooldownBonusAt100() { return CASTING_SPEED_COOLDOWN_BONUS_AT_100.get(); }
public static double manaPoolCapacityBonusAt100() { return MANA_POOL_CAPACITY_BONUS_AT_100.get(); }
public static double manaPoolRegenBonusAt100() { return MANA_POOL_REGEN_BONUS_AT_100.get(); }
public static double magicResistanceBonusAt100() { return MAGIC_RESISTANCE_BONUS_AT_100.get(); }
```

Use exactly the keys, defaults, and bounds asserted in Step 1, then call `builder.pop()`.

- [ ] **Step 4: Run the config contract test and confirm GREEN**

Run the command from Step 2.

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/config/StatModServerConfig.java src/test/java/tong/statmod/config/StatModServerConfigContractTest.java
git commit -m "feat: configure Iron magic attribute curves"
```

---

### Task 3: Model the Six Iron Attribute Targets

**Files:**
- Create: `src/main/java/tong/statmod/effects/MagicAttributeTarget.java`
- Create: `src/test/java/tong/statmod/effects/MagicAttributeTargetTest.java`

**Interfaces:**
- Consumes: `LinearStatScaling.bonus(int, double)`, `PlayerStats`, and Task 2 config getters.
- Produces: `MagicAttributeTarget.values()`, `id()`, `modifierId()`, `stat()`, `bonusKind()`, and `amount(PlayerStats)`.

- [ ] **Step 1: Write the failing mapping/scaling test**

Create a test that asserts this exact mapping:

```java
Map<String, StatType> expected = Map.of(
        "irons_spellbooks:spell_power", StatType.ARCANE_POWER,
        "irons_spellbooks:cast_time_reduction", StatType.CASTING_SPEED,
        "irons_spellbooks:cooldown_reduction", StatType.CASTING_SPEED,
        "irons_spellbooks:max_mana", StatType.MANA_POOL,
        "irons_spellbooks:mana_regen", StatType.MANA_POOL,
        "irons_spellbooks:spell_resist", StatType.MAGIC_RESISTANCE);
assertEquals(expected, Arrays.stream(MagicAttributeTarget.values())
        .collect(Collectors.toMap(target -> target.id().toString(), MagicAttributeTarget::stat)));
assertEquals(6, Arrays.stream(MagicAttributeTarget.values())
        .map(MagicAttributeTarget::modifierId).distinct().count());

PlayerStats stats = new PlayerStats();
stats.setLevel(StatType.ARCANE_POWER, 100);
assertEquals(1.0, MagicAttributeTarget.SPELL_POWER.amount(stats));
stats.setLevel(StatType.ARCANE_POWER, 50);
assertEquals(0.5, MagicAttributeTarget.SPELL_POWER.amount(stats));
```

- [ ] **Step 2: Run the test and confirm RED**

```powershell
.\gradlew.bat test --tests tong.statmod.effects.MagicAttributeTargetTest --console=plain
```

Expected: test compilation fails because `MagicAttributeTarget` does not exist.

- [ ] **Step 3: Implement the enum**

Create the six constants with these stable UUIDs:

```java
SPELL_POWER("spell_power", "c4073a9e-3932-4dbd-a03e-067b34a7a47f", StatType.ARCANE_POWER, BonusKind.SPELL_POWER),
CAST_TIME("cast_time_reduction", "5414ef15-24e9-47cb-a6ce-fc060aa04788", StatType.CASTING_SPEED, BonusKind.CAST_TIME),
COOLDOWN("cooldown_reduction", "f7ea3251-9737-4105-8c95-9ec84ed1c3ba", StatType.CASTING_SPEED, BonusKind.COOLDOWN),
MAX_MANA("max_mana", "8c82cb39-5ca4-4f14-b0e1-34cc2c9d98ba", StatType.MANA_POOL, BonusKind.MANA_CAPACITY),
MANA_REGEN("mana_regen", "4192f4b8-0799-4a2c-b32b-9eb5e457b1cb", StatType.MANA_POOL, BonusKind.MANA_REGEN),
SPELL_RESIST("spell_resist", "e57b31fc-563c-4191-a680-02301fd82869", StatType.MAGIC_RESISTANCE, BonusKind.SPELL_RESIST);
```

Construct IDs with namespace `irons_spellbooks`. Implement `BonusKind.maximumAt100()` as a switch over the six Task 2 getters and implement:

```java
public double amount(PlayerStats stats) {
    return LinearStatScaling.bonus(stats.get(stat).level(), bonusKind.maximumAt100());
}
```

- [ ] **Step 4: Run the target test and confirm GREEN**

Run the command from Step 2.

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/effects/MagicAttributeTarget.java src/test/java/tong/statmod/effects/MagicAttributeTargetTest.java
git commit -m "feat: model Iron magic attribute targets"
```

---

### Task 3A: Require the Audited Iron's Spells Runtime

**Files:**
- Modify: `src/main/resources/META-INF/mods.toml`
- Modify: `src/test/java/tong/statmod/RequiredProviderMetadataTest.java`

**Interfaces:**
- Consumes: Forge dependency metadata and pinned Iron's Spells 3.16.2 profile.
- Produces: a mandatory `irons_spellbooks` `[3.16.2,)` dependency on both sides.

- [ ] **Step 1: Add the failing metadata assertion**

```java
assertRequired(metadata, "irons_spellbooks", "[3.16.2,)");
```

- [ ] **Step 2: Run the focused test and confirm RED**

```powershell
.\gradlew.bat test --tests tong.statmod.RequiredProviderMetadataTest --console=plain
```

Expected: failure reports missing dependency `irons_spellbooks`.

- [ ] **Step 3: Add the exact dependency block**

```toml
[[dependencies.${mod_id}]]
modId="irons_spellbooks"
mandatory=true
versionRange="[3.16.2,)"
ordering="AFTER"
side="BOTH"
```

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run the command from Step 2. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/META-INF/mods.toml src/test/java/tong/statmod/RequiredProviderMetadataTest.java docs/superpowers
git commit -m "feat: require Iron's Spells runtime"
```

---

### Task 4: Apply Magical Modifiers Idempotently

**Files:**
- Modify: `src/main/java/tong/statmod/effects/AttributeEffectLevels.java`
- Modify: `src/main/java/tong/statmod/effects/PlayerAttributeEffects.java`
- Modify: `src/test/java/tong/statmod/effects/AttributeEffectLevelsTest.java`
- Modify: `src/test/java/tong/statmod/effects/PlayerAttributeEffectsContractTest.java`
- Modify: `src/test/java/tong/statmod/effects/PlayerAttributeEffectsWiringTest.java`

**Interfaces:**
- Consumes: Task 3 `MagicAttributeTarget` and existing registry-based `replaceModifier` helper.
- Produces: magical level-change detection and one transient modifier per available target.

- [ ] **Step 1: Write failing snapshot and wiring assertions**

Set magical levels and expect:

```java
stats.setLevel(StatType.ARCANE_POWER, 11);
stats.setLevel(StatType.CASTING_SPEED, 22);
stats.setLevel(StatType.MANA_POOL, 33);
stats.setLevel(StatType.MAGIC_RESISTANCE, 44);
assertEquals(new AttributeEffectLevels(12, 34, 56, 11, 22, 33, 44),
        AttributeEffectLevels.from(stats));
```

Add source assertions:

```java
assertTrue(source.contains("for (MagicAttributeTarget target : MagicAttributeTarget.values())"));
assertTrue(source.contains("target.amount(stats)"));
assertTrue(source.contains("AttributeModifier.Operation.MULTIPLY_BASE"));
assertFalse(source.contains("io.redspace.ironsspellbooks"));
```

- [ ] **Step 2: Run focused tests and confirm RED**

```powershell
.\gradlew.bat test --tests tong.statmod.effects.AttributeEffectLevelsTest --tests tong.statmod.effects.PlayerAttributeEffectsContractTest --tests tong.statmod.effects.PlayerAttributeEffectsWiringTest --console=plain
```

Expected: constructor mismatch and missing magic-loop assertions.

- [ ] **Step 3: Expand the level snapshot and refresh loop**

Replace the record header and factory with:

```java
public record AttributeEffectLevels(
        int rapidite, int agility, int physicalEndurance,
        int arcanePower, int castingSpeed, int manaPool, int magicResistance) {
    public static AttributeEffectLevels from(PlayerStats stats) {
        return new AttributeEffectLevels(
                stats.get(StatType.RAPIDITE).level(),
                stats.get(StatType.AGILITY).level(),
                stats.get(StatType.PHYSICAL_ENDURANCE).level(),
                stats.get(StatType.ARCANE_POWER).level(),
                stats.get(StatType.CASTING_SPEED).level(),
                stats.get(StatType.MANA_POOL).level(),
                stats.get(StatType.MAGIC_RESISTANCE).level());
    }
}
```

Add this loop to `PlayerAttributeEffects.refresh`:

```java
for (MagicAttributeTarget target : MagicAttributeTarget.values()) {
    replaceModifier(player, target.id(), target.modifierId(),
            "STAT Mod " + target.stat().id(), target.amount(stats));
}
```

Do not change `replaceModifier`; its remove-before-add behavior and `MULTIPLY_BASE` operation are already correct.

- [ ] **Step 4: Run focused tests and confirm GREEN**

Run the command from Step 2.

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/effects/AttributeEffectLevels.java src/main/java/tong/statmod/effects/PlayerAttributeEffects.java src/test/java/tong/statmod/effects
git commit -m "feat: apply Iron magic stat attributes"
```

---

### Task 5: Refresh Connected Players After Server Config Reload

**Files:**
- Create: `src/main/java/tong/statmod/config/StatModConfigEvents.java`
- Create: `src/test/java/tong/statmod/config/StatModConfigEventsContractTest.java`

**Interfaces:**
- Consumes: `ModConfigEvent.Reloading`, `StatModServerConfig.SPEC`, `ServerLifecycleHooks.getCurrentServer()`.
- Produces: one server-thread `PlayerAttributeEffects.refresh` call per connected player after STAT Mod server-config reload.

- [ ] **Step 1: Write the failing event contract test**

Read the new source path and assert:

```java
assertTrue(source.contains("ModConfigEvent.Reloading"));
assertTrue(source.contains("event.getConfig().getSpec() != StatModServerConfig.SPEC"));
assertTrue(source.contains("ServerLifecycleHooks.getCurrentServer()"));
assertTrue(source.contains("server.execute"));
assertTrue(source.contains("getPlayerList().getPlayers()"));
assertTrue(source.contains("PlayerAttributeEffects::refresh"));
```

- [ ] **Step 2: Run the test and confirm RED**

```powershell
.\gradlew.bat test --tests tong.statmod.config.StatModConfigEventsContractTest --console=plain
```

Expected: assertions fail because the source file is absent.

- [ ] **Step 3: Implement the MOD-bus reload handler**

Create a `@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)` class with:

```java
@SubscribeEvent
public static void reload(ModConfigEvent.Reloading event) {
    if (event.getConfig().getSpec() != StatModServerConfig.SPEC) {
        return;
    }
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) {
        return;
    }
    server.execute(() -> server.getPlayerList().getPlayers()
            .forEach(PlayerAttributeEffects::refresh));
}
```

- [ ] **Step 4: Run the contract test plus Java compilation**

```powershell
.\gradlew.bat test --tests tong.statmod.config.StatModConfigEventsContractTest --console=plain
```

Expected: `BUILD SUCCESSFUL`; Forge 1.20.1 types compile.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/config/StatModConfigEvents.java src/test/java/tong/statmod/config/StatModConfigEventsContractTest.java
git commit -m "feat: refresh stat attributes on config reload"
```

---

### Task 6: Make the Screen and Localization Truthful

**Files:**
- Modify: `src/main/java/tong/statmod/client/stats/StatPresentation.java`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/test/java/tong/statmod/client/stats/StatsScreenModelTest.java`
- Modify: `src/test/java/tong/statmod/client/stats/StatsLanguageResourcesTest.java`
- Modify: `README.md`
- Modify: `docs/compatibility/stat-attribute-provider-matrix.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`

**Interfaces:**
- Consumes: Task 1 roster and Task 4 implemented magic effects.
- Produces: five-family/19-stat localized UI with four magic stats marked `ACTIVE` and Erudition marked `FOUNDATION`.

- [ ] **Step 1: Write failing presentation assertions**

Replace the former magic-foundation test with:

```java
assertEquals(StatDisplayState.ACTIVE, StatPresentation.of(StatType.ARCANE_POWER).state());
assertEquals(StatDisplayState.ACTIVE, StatPresentation.of(StatType.CASTING_SPEED).state());
assertEquals(StatDisplayState.ACTIVE, StatPresentation.of(StatType.MANA_POOL).state());
assertEquals(StatDisplayState.ACTIVE, StatPresentation.of(StatType.MAGIC_RESISTANCE).state());
assertEquals(StatDisplayState.FOUNDATION, StatPresentation.of(StatType.ERUDITION).state());
```

In `StatsLanguageResourcesTest`, explicitly reject retired keys:

```java
assertFalse(json.contains("fire_affinity"));
assertFalse(json.contains("water_affinity"));
assertFalse(json.contains("earth_affinity"));
assertFalse(json.contains("air_affinity"));
assertFalse(json.contains("elemental_specialization"));
```

- [ ] **Step 2: Run client model/language tests and confirm RED**

```powershell
.\gradlew.bat test --tests tong.statmod.client.stats.StatsScreenModelTest --tests tong.statmod.client.stats.StatsLanguageResourcesTest --console=plain
```

Expected: four magic states are still foundation and retired translation keys remain.

- [ ] **Step 3: Update presentation and translations**

Change the foundation set to exactly:

```java
private static final Set<StatType> FOUNDATION = EnumSet.of(StatType.ERUDITION);
```

Remove the four affinity name/description pairs and elemental-family key from both locale files. Use these meanings in both languages:

```text
Arcane Power: increases Iron's Spells global spell power.
Casting Speed: reduces Iron's Spells cast time and cooldowns.
Mana Pool: increases Iron's Spells maximum mana and mana regeneration.
Magic Resistance: increases resistance to Iron's Spells magic damage.
Erudition: saved foundation awaiting its dedicated knowledge progression.
```

Translate naturally in French, preserve every remaining stat/family/UI key, and keep JSON valid.

- [ ] **Step 4: Update supported documentation**

Change all current-runtime claims from 23/six to 19/five, remove affinities from automatic/future lists, record schema 2, and mark the four Iron attribute effects implemented. Do not claim magical XP, Curios casting, or addon gameplay validation.

- [ ] **Step 5: Run model/language tests and confirm GREEN**

Run the command from Step 2.

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/tong/statmod/client/stats/StatPresentation.java src/main/resources/assets/statmod/lang src/test/java/tong/statmod/client/stats README.md docs/compatibility
git commit -m "feat: present active Iron magic stats"
```

---

### Task 7: Full Verification, Runtime Smoke, and `test-vrai` Deployment

**Files:**
- Modify: `scripts/verify-clean-foundation.ps1`
- Deploy: `build/libs/statmod-0.1.0+1.20.1.jar`
- Deploy target: `C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods\statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes: completed roster, config, targets, refresh lifecycle, and client presentation.
- Produces: verified/deployed JAR and preserved rollback copy.

- [ ] **Step 1: Extend the JAR surface verifier**

Require these entries in `verify-clean-foundation.ps1`:

```powershell
'tong/statmod/effects/MagicAttributeTarget.class'
'tong/statmod/config/StatModConfigEvents.class'
```

- [ ] **Step 2: Run complete automated verification**

```powershell
.\gradlew.bat clean test build --console=plain
git diff --check
```

Expected: all tests pass, reobfuscated JAR builds, and no whitespace errors.

- [ ] **Step 3: Inspect the JAR even if the historical 74-addon archive is unavailable**

Run the normal verifier first:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-clean-foundation.ps1 -Mode After
```

Expected supported result: `OK mode=After ... jars=74 manifest=74`. If the ignored 74 JAR archive is absent, record that exact environmental limitation and independently open the built ZIP to verify the required entries, duplicate-entry absence, and forbidden external-class prefixes. Do not weaken or remove the historical archive check.

- [ ] **Step 4: Run required-provider GameTest**

```powershell
powershell -ExecutionPolicy Bypass -File scripts\smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke` and normal world save/shutdown.

- [ ] **Step 5: Back up and deploy only STAT Mod**

Resolve source/target paths, require exactly one existing `statmod-*.jar`, copy it to:

```text
C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\<timestamp>-before-irons-magic-bridge\
```

Then overwrite only `statmod-0.1.0+1.20.1.jar` and require equal SHA-256 hashes. Do not remove `tl_skin_cape` or any provider/addon JAR.

- [ ] **Step 6: Client acceptance checklist**

In `test-vrai`, verify:

```text
P shows exactly five families and 19 stats.
The elemental family and four affinities are absent.
Arcane Power, Casting Speed, Mana Pool, and Magic Resistance are active.
At levels 0/50/100, Iron attributes follow the configured linear values.
Relog and dimension change do not stack modifiers.
Stat refresh does not refill current mana.
No crash, mixin failure, missing dependency, or STAT Mod error appears in latest.log.
```

- [ ] **Step 7: Commit verifier and report final state**

```powershell
git add scripts/verify-clean-foundation.ps1
git commit -m "test: verify Iron magic bridge jar surface"
git status --short
git log -10 --oneline
```

Expected: clean `forge-1.20.1` worktree, deployed JAR hash recorded, and branch preserved without merge or push.
