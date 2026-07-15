# Forge Automatic Combat Perks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 12 automatic level-25/50/75 perks for Brute Force, Blade Technique, Precision, and Physical Resistance without broadening their existing classified combat eligibility.

**Architecture:** Extend the canonical automatic-perk catalog and configuration, then pass resolved perk amounts into the existing pure combat scaling functions. Keep one `LivingHurtEvent` mutation path, bump the bounded snapshot protocol from 5 to 6, and reuse the current read-only active-perk presentation.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, JUnit 5.10.2, Iron's Spells 3.16.2, Epic Fight 20.14.17, Puffish Attributes 0.8.2, Gradle 8.8.

## Global Constraints

- Work only on branch `forge-1.20.1` in `.worktrees/forge-1.20.1`.
- Preserve all 21 existing perk IDs, order, requirements, amounts, and localization keys.
- Add exactly 12 IDs after the existing 21, for a catalog total of 33.
- Keep level thresholds exactly 25, 50, and 75.
- Do not add Tracking, Keen Senses, affinity, tree, point, purchase, respec, or persisted unlock behavior.
- Do not use generic attack-damage, armor, or toughness modifiers.
- Do not modify XP, weapon tags, Iron's Spells casting, Epic Fight skills, or Puffish ownership.
- Keep one classified `LivingHurtEvent` combat mutation path.
- Change the network protocol from `"5"` to `"6"` and `MAX_PERKS` from 21 to 33.
- Deploy only after full tests, JAR inspection, and required-provider GameTest smoke pass.

---

### Task 1: Extend the canonical catalog, effect kinds, and server configuration

**Files:**
- Modify: `src/main/java/tong/statmod/perks/AutomaticPerkEffect.java`
- Modify: `src/main/java/tong/statmod/perks/AutomaticPerkCatalog.java`
- Modify: `src/main/java/tong/statmod/perks/AutomaticPerkBonuses.java`
- Modify: `src/main/java/tong/statmod/config/StatModServerConfig.java`
- Modify: `src/test/java/tong/statmod/perks/AutomaticPerkCatalogTest.java`
- Modify: `src/test/java/tong/statmod/perks/AutomaticPerkResolverTest.java`
- Modify: `src/test/java/tong/statmod/perks/AutomaticPerkBonusesTest.java`
- Modify: `src/test/java/tong/statmod/config/StatModServerConfigContractTest.java`

**Interfaces:**
- Produces four new `AutomaticPerkEffect` values: `BRUTE_FORCE_DAMAGE`, `BLADE_TECHNIQUE_DAMAGE`, `PRECISION_DAMAGE`, and `PHYSICAL_RESISTANCE`.
- Produces four config accessors ending in `PerMilestone()`.
- Preserves `AutomaticPerkResolver.active(Map<StatType,Integer>)` and `AutomaticPerkBonuses.amount(AutomaticPerkEffect)`.

- [ ] **Step 1: Write failing catalog boundary tests**

Update the catalog expectations:

```java
assertEquals(33, AutomaticPerkCatalog.definitions().size());
assertEquals(33, AutomaticPerkCatalog.definitions().stream()
        .map(AutomaticPerkDefinition::id).distinct().count());
assertEquals("statmod:rapidite_25", definitions.get(0).id());
assertEquals("statmod:magic_resistance_75", definitions.get(20).id());
assertEquals("statmod:brute_force_25", definitions.get(21).id());
assertEquals("statmod:physical_resistance_75", definitions.get(32).id());
```

Add a parameterized/helper loop for each of `BRUTE_FORCE`, `BLADE_TECHNIQUE`,
`PRECISION`, and `PHYSICAL_RESISTANCE`, asserting no activation at 24, one ID at
25/49, two at 50/74, and three at 75.

- [ ] **Step 2: Write failing bonus/config tests**

With Forge config defaults loaded, assert:

```java
assertEquals(0.15, bonusesAt(StatType.BRUTE_FORCE, 75)
        .amount(AutomaticPerkEffect.BRUTE_FORCE_DAMAGE), 1.0e-9);
assertEquals(0.15, bonusesAt(StatType.BLADE_TECHNIQUE, 75)
        .amount(AutomaticPerkEffect.BLADE_TECHNIQUE_DAMAGE), 1.0e-9);
assertEquals(0.15, bonusesAt(StatType.PRECISION, 75)
        .amount(AutomaticPerkEffect.PRECISION_DAMAGE), 1.0e-9);
assertEquals(0.06, bonusesAt(StatType.PHYSICAL_RESISTANCE, 75)
        .amount(AutomaticPerkEffect.PHYSICAL_RESISTANCE), 1.0e-9);
```

Require these exact config fragments with `0.0, 0.25` bounds:

```text
bruteForceDamagePerMilestone", 0.05
bladeTechniqueDamagePerMilestone", 0.05
precisionDamagePerMilestone", 0.05
physicalResistancePerMilestone", 0.02
```

- [ ] **Step 3: Run focused tests and verify RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.perks.*" --tests "tong.statmod.config.StatModServerConfigContractTest" --console=plain
```

Expected: compilation fails for missing effect enum constants/accessors and catalog-size assertions fail.

- [ ] **Step 4: Add effect kinds, exact catalog entries, and config accessors**

Append the four enum constants. After the seven existing catalog groups, call:

```java
addMilestones(definitions, "brute_force", StatType.BRUTE_FORCE,
        AutomaticPerkEffect.BRUTE_FORCE_DAMAGE, 0.05);
addMilestones(definitions, "blade_technique", StatType.BLADE_TECHNIQUE,
        AutomaticPerkEffect.BLADE_TECHNIQUE_DAMAGE, 0.05);
addMilestones(definitions, "precision", StatType.PRECISION,
        AutomaticPerkEffect.PRECISION_DAMAGE, 0.05);
addMilestones(definitions, "physical_resistance", StatType.PHYSICAL_RESISTANCE,
        AutomaticPerkEffect.PHYSICAL_RESISTANCE, 0.02);
```

Define the four `ForgeConfigSpec.DoubleValue` entries under the existing
`automaticPerks` section and expose:

```java
public static double bruteForceDamagePerMilestone()
public static double bladeTechniqueDamagePerMilestone()
public static double precisionDamagePerMilestone()
public static double physicalResistancePerMilestone()
```

Map each new effect to its matching accessor in
`AutomaticPerkBonuses.configuredAmount`.

- [ ] **Step 5: Run focused tests and commit**

Run the Step 3 command. Expected: every catalog, resolver, bonus, malformed-input,
affinity-exclusion, and config contract test passes.

```powershell
git add src/main/java/tong/statmod/perks src/main/java/tong/statmod/config/StatModServerConfig.java src/test/java/tong/statmod/perks src/test/java/tong/statmod/config/StatModServerConfigContractTest.java
git commit -m "feat: define automatic combat perks"
```

---

### Task 2: Apply perks inside the existing pure combat calculations

**Files:**
- Modify: `src/main/java/tong/statmod/effects/CombatStatScaling.java`
- Modify: `src/main/java/tong/statmod/event/CombatEffectEvents.java`
- Modify: `src/test/java/tong/statmod/effects/CombatStatScalingTest.java`
- Modify: `src/test/java/tong/statmod/event/CombatEffectEventsContractTest.java`

**Interfaces:**
- Produces: `CombatStatScaling.offensiveMultiplier(int, double, CombatScalingRules)`.
- Preserves: `CombatStatScaling.offensiveMultiplier(int, CombatScalingRules)` as zero-perk overload.
- Produces: `CombatStatScaling.defensiveMultiplier(int, int, double, CombatScalingRules)`.
- Preserves: `CombatStatScaling.defensiveMultiplier(int, int, CombatScalingRules)` as zero-perk overload.
- Consumes: `AutomaticPerkBonuses.from(PlayerStats)` and the four Task 1 effect kinds.

- [ ] **Step 1: Write failing pure offensive tests**

For the same continuous level/rules, assert:

```java
double continuous = CombatStatScaling.offensiveMultiplier(50, rules);
assertEquals(continuous,
        CombatStatScaling.offensiveMultiplier(50, 0.0, rules), EPSILON);
assertEquals(continuous * 1.05,
        CombatStatScaling.offensiveMultiplier(50, 0.05, rules), EPSILON);
assertEquals(continuous * 1.15,
        CombatStatScaling.offensiveMultiplier(50, 0.15, rules), EPSILON);
assertEquals(continuous,
        CombatStatScaling.offensiveMultiplier(50, Double.NaN, rules), EPSILON);
```

Also verify negative perk amounts behave as zero and huge inputs clamp to `0.75`.

- [ ] **Step 2: Write failing pure defensive tests**

Using default rules, assert:

```java
assertEquals((1.0 - 0.325) * (1.0 - 0.175),
        CombatStatScaling.defensiveMultiplier(50, 50, 0.0, rules), EPSILON);
assertEquals((1.0 - 0.385) * (1.0 - 0.175),
        CombatStatScaling.defensiveMultiplier(50, 50, 0.06, rules), EPSILON);
assertEquals(0.1885,
        CombatStatScaling.defensiveMultiplier(100, 100, 0.06, rules), EPSILON);
```

Verify NaN/negative perk amounts are zero and the resistance component never
exceeds `0.95`.

- [ ] **Step 3: Write failing event wiring contracts and verify RED**

Require the event source to contain one `AutomaticPerkBonuses.from(stats)` in
each applicable branch, map the selected offensive `StatType` to exactly one of
the three offensive effects, pass `PHYSICAL_RESISTANCE` only in defense, and
retain all existing eligibility checks. Require exactly one `event.setAmount`.
Reject imports/references to generic attribute modifiers.

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.effects.CombatStatScalingTest" --tests "tong.statmod.event.CombatEffectEventsContractTest" --console=plain
```

Expected: compilation fails for the new overloads and source contracts fail.

- [ ] **Step 4: Implement bounded pure formulas and event mapping**

Implement offensive bonus normalization:

```java
double perk = Double.isFinite(perkBonus)
        ? Math.max(0.0, Math.min(0.75, perkBonus)) : 0.0;
return continuous * (1.0 + perk);
```

Implement defense as:

```java
double perk = boundedPerk(perkBonus);
double resistance = Math.min(0.95,
        rules.physicalResistanceCap() * normalized(resistanceLevel) + perk);
double endurance = rules.physicalEnduranceCap() * normalized(enduranceLevel);
return Math.max(0.0, (1.0 - resistance) * (1.0 - endurance));
```

In `applyOffense`, resolve bonuses once after stats, choose effect by selected
stat with a switch supporting only Brute Force, Blade Technique, and Precision,
and call the new overload. In `applyDefense`, pass only the Physical Resistance
bonus. Do not change classification, source, target, or player eligibility.

- [ ] **Step 5: Run combat regressions and commit**

```powershell
.\gradlew.bat test --tests "tong.statmod.effects.*" --tests "tong.statmod.event.CombatEffectEventsContractTest" --tests "tong.statmod.progression.xp.WeaponClassifierTest" --tests "tong.statmod.progression.xp.CombatEligibilityTest" --console=plain
```

Expected: all scaling, classification, eligibility, and event contract tests pass.

```powershell
git add src/main/java/tong/statmod/effects/CombatStatScaling.java src/main/java/tong/statmod/event/CombatEffectEvents.java src/test/java/tong/statmod/effects/CombatStatScalingTest.java src/test/java/tong/statmod/event/CombatEffectEventsContractTest.java
git commit -m "feat: apply classified combat perks"
```

---

### Task 3: Expand the bounded protocol and active-perk presentation

**Files:**
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Modify: `src/main/java/tong/statmod/network/StatsSnapshotMessage.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/test/java/tong/statmod/StatModRuntimeTest.java`
- Modify: `src/test/java/tong/statmod/network/StatProgressNoticeMessageTest.java`
- Modify: `src/test/java/tong/statmod/network/StatsSnapshotMessageTest.java`
- Modify: `src/test/java/tong/statmod/client/stats/AutomaticPerkLanguageTest.java`

**Interfaces:**
- Produces: `StatModRuntime.NETWORK_PROTOCOL == "6"`.
- Produces: `StatsSnapshotMessage.MAX_PERKS == 33`.
- Preserves current canonical catalog-order filtering and oversized-payload rejection.

- [ ] **Step 1: Write failing protocol and full-catalog codec tests**

Change protocol assertions to `"6"`, maximum assertions to 33, then create a
player with all seven old and four new covered stats at 75. Round-trip the
snapshot and assert:

```java
assertEquals(33, decoded.activePerkIds().size());
assertEquals("statmod:rapidite_25", decoded.activePerkIds().get(0));
assertEquals("statmod:physical_resistance_75",
        decoded.activePerkIds().get(32));
```

Retain the malicious `MAX_PERKS + 1` rejection test.

- [ ] **Step 2: Extend localization contracts and verify RED**

The existing catalog-driven test automatically requires name and description
keys for all 33 IDs. Add direct assertions that the four new English/French
families mention their exact 5% or 2-point effects and contain no affinity IDs.

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.StatModRuntimeTest" --tests "tong.statmod.network.*" --tests "tong.statmod.client.stats.AutomaticPerkLanguageTest" --console=plain
```

Expected: protocol/maximum assertions and missing localization keys fail.

- [ ] **Step 3: Bump bounds and add 24 localized entries**

Set protocol to 6 and maximum to 33. Add localized names/descriptions for
`brute_force`, `blade_technique`, `precision`, and `physical_resistance` at
25/50/75. Use tiered names I/II/III; descriptions state `+5%` additional
classified damage or `+2` physical-reduction percentage points per milestone.

Do not modify `StatsOverviewScreen`: its existing catalog-driven three-line and
`+N` overflow presentation already supports 33 entries.

- [ ] **Step 4: Run network/client regressions and commit**

Run the Step 2 command. Expected: protocol, full-catalog round-trip, malicious
count rejection, and both locale tests pass.

```powershell
git add src/main/java/tong/statmod/StatModRuntime.java src/main/java/tong/statmod/network/StatsSnapshotMessage.java src/main/resources/assets/statmod/lang src/test/java/tong/statmod/StatModRuntimeTest.java src/test/java/tong/statmod/network src/test/java/tong/statmod/client/stats/AutomaticPerkLanguageTest.java
git commit -m "feat: sync automatic combat perks"
```

---

### Task 4: Documentation, complete verification, deployment, and branch handoff

**Files:**
- Modify: `README.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Modify: `src/test/java/tong/statmod/SupportedRuntimeContractTest.java`
- Deploy: `build/libs/statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes all Tasks 1–3 behavior.
- Produces one verified and hash-matched JAR in `test-vrai`.

- [ ] **Step 1: Write a failing documentation contract**

Require the runtime record to contain `33 automatic perks`, `12 classified
combat perks`, `protocol 6`, the 5% and 2-point defaults, and explicit exclusion
of Tracking/Keen Senses from this batch.

- [ ] **Step 2: Update README/runtime documentation and verify GREEN**

Document the four covered combat stats, thresholds, formulas, exact eligibility,
catalog total, protocol, and deferred perception batch. Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.SupportedRuntimeContractTest" --console=plain
```

Expected: the documentation contract passes.

- [ ] **Step 3: Run complete clean verification**

```powershell
.\gradlew.bat clean test build --console=plain
.\gradlew.bat test --rerun-tasks --console=plain
```

Expected: both commands exit 0 and all XML test suites report zero failures/errors.

- [ ] **Step 4: Inspect artifact and smoke required providers**

Require exactly one class entry for catalog, bonuses, scaling, and snapshot;
zero duplicate archive entries; zero bundled classes under Iron's Spells, Epic
Fight, or Puffish namespaces; and record SHA-256.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke`.

- [ ] **Step 5: Commit documentation, back up, and deploy**

Commit documentation with:

```powershell
git add README.md docs/compatibility/forge-1.20.1-supported-runtime.md src/test/java/tong/statmod/SupportedRuntimeContractTest.java
git commit -m "docs: document automatic combat perks"
```

Confirm no Minecraft process is running. Back up the existing client JAR under
`C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\<timestamp>-before-automatic-combat-perks`,
then replace only STAT Mod. Preserve total mod count, require exactly one
`statmod-*.jar`, and require source/target SHA-256 equality.

- [ ] **Step 6: Preserve and push the branch when requested**

Require a clean worktree. Keep branch/worktree intact; do not merge or remove.
If the user requests the normal project handoff, push with:

```powershell
git push origin forge-1.20.1
```

Then verify `git ls-remote origin refs/heads/forge-1.20.1` equals local `HEAD`.

## Completion audit

- Search this plan for unfinished-marker terms and resolve every match.
- Confirm every design section maps to a task.
- Confirm the overload signatures in Task 2 match their event call sites.
- Confirm all 21 existing perk definitions remain byte-for-byte unchanged.
- Confirm no generic combat attribute, second hurt handler, Tracking/Keen effect,
  affinity, cast hook, or saved unlock state was introduced.
