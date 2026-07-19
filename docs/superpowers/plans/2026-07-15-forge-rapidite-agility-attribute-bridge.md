# Forge Rapidité and Agility Attribute Bridge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add passive Rapidité/Agility attribute scaling and require the audited Epic Fight and Pufferfish's Attributes runtime versions.

**Architecture:** Generalize the existing safe linear formula, define registry-ID mobility targets with stable UUIDs, and reuse the idempotent player attribute adapter. Forge metadata enforces required providers while Java remains registry-based and does not import provider internals.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, ForgeGradle 6, JUnit 5, Epic Fight 20.14.17+, Pufferfish's Attributes 0.8.2+.

## Global Constraints

- Require `epicfight` version `[20.14.17,)`, `ordering="AFTER"`, `side="BOTH"`.
- Require `puffish_attributes` version `[0.8.2,)`, `ordering="AFTER"`, `side="BOTH"`.
- Do not import Epic Fight or Puffish implementation classes and do not embed their classes in the STAT Mod JAR.
- Rapidité level 100 defaults to +30% vanilla attack speed and +30% Epic Fight off-hand attack speed.
- Agility level 100 defaults to +20% vanilla movement speed and +10% Puffish sprinting speed.
- Do not add jump strength, fall reduction, damage, dodge chance, or a tick handler in this slice.
- Use transient `MULTIPLY_BASE` modifiers with stable unique UUIDs and remove before add.
- Keep ParCool optional and preserve existing Endurance UUIDs and behavior.
- Preserve the unrelated dirty modpack script files.

---

### Task 1: Generalize the bounded linear scaling utility

**Files:**
- Create: `src/test/java/tong/statmod/effects/LinearStatScalingTest.java`
- Create: `src/main/java/tong/statmod/effects/LinearStatScaling.java`
- Delete: `src/test/java/tong/statmod/effects/PhysicalEnduranceScalingTest.java`
- Delete: `src/main/java/tong/statmod/effects/PhysicalEnduranceScaling.java`
- Modify: `src/main/java/tong/statmod/effects/StaminaAttributeTarget.java`

**Interfaces:**
- Produces: `LinearStatScaling.bonus(int level, double bonusAt100)`.
- Preserves: exact existing Endurance numerical behavior.

- [ ] **Step 1: Add the failing generalized test**

Create `LinearStatScalingTest` with assertions for level 0, level 50, level 100, level clamping, negative bonus, NaN, and infinity. Reference `LinearStatScaling.bonus(...)` before the production class exists.

- [ ] **Step 2: Confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.effects.LinearStatScalingTest`

Expected: test compilation fails because `LinearStatScaling` is missing.

- [ ] **Step 3: Implement and migrate the utility**

```java
public final class LinearStatScaling {
    private LinearStatScaling() {}

    public static double bonus(int level, double bonusAt100) {
        if (!Double.isFinite(bonusAt100) || bonusAt100 <= 0.0) return 0.0;
        int safeLevel = Math.max(0, Math.min(100, level));
        return bonusAt100 * safeLevel / 100.0;
    }
}
```

Change `StaminaAttributeTarget.amount` to call `LinearStatScaling.bonus`, then remove the endurance-specific class and test.

- [ ] **Step 4: Confirm GREEN**

Run the focused test and `.\gradlew.bat test`.

Expected: both commands report `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add -A src/main/java/tong/statmod/effects src/test/java/tong/statmod/effects
git commit -m "refactor: generalize linear stat scaling"
```

### Task 2: Required provider metadata, mobility config, and targets

**Files:**
- Modify: `src/main/resources/META-INF/mods.toml`
- Modify: `src/main/java/tong/statmod/config/StatModServerConfig.java`
- Create: `src/main/java/tong/statmod/effects/MobilityAttributeTarget.java`
- Create: `src/test/java/tong/statmod/RequiredProviderMetadataTest.java`
- Create: `src/test/java/tong/statmod/effects/MobilityAttributeTargetTest.java`
- Modify: `src/test/java/tong/statmod/config/StatModServerConfigContractTest.java`

**Interfaces:**
- Produces: four exact mobility targets and three config accessors.
- Consumes: `LinearStatScaling.bonus(int, double)` and `PlayerStats`.

- [ ] **Step 1: Write failing metadata and target tests**

Metadata test reads `mods.toml` and verifies mandatory entries with exact mod IDs, version floors, `AFTER`, and `BOTH`. Target test verifies exactly:

```text
minecraft:generic.attack_speed       RAPIDITE  RAPIDITE_ATTACK_SPEED
epicfight:offhand_attack_speed       RAPIDITE  RAPIDITE_ATTACK_SPEED
minecraft:generic.movement_speed     AGILITY   AGILITY_MOVEMENT_SPEED
puffish_attributes:sprinting_speed   AGILITY   AGILITY_SPRINTING_SPEED
```

It also verifies four distinct UUIDs and default amount outcomes `0.30`, `0.30`, `0.20`, `0.10` at level 100.

- [ ] **Step 2: Confirm RED**

Run the three focused test classes.

Expected: missing dependency blocks, target enum, and config keys cause failures.

- [ ] **Step 3: Add exact Forge metadata**

```toml
[[dependencies.${mod_id}]]
modId="epicfight"
mandatory=true
versionRange="[20.14.17,)"
ordering="AFTER"
side="BOTH"

[[dependencies.${mod_id}]]
modId="puffish_attributes"
mandatory=true
versionRange="[0.8.2,)"
ordering="AFTER"
side="BOTH"
```

- [ ] **Step 4: Add mobility config and enum**

Add a `mobility` config section with `rapiditeAttackSpeedBonusAt100=0.30`, `agilityMovementSpeedBonusAt100=0.20`, and `agilitySprintingSpeedBonusAt100=0.10`, each in `0.0..2.0`. Define `MobilityAttributeTarget` fields `ResourceLocation id`, `UUID modifierId`, `StatType stat`, and `BonusKind bonusKind`; `amount(PlayerStats)` selects the source level and config maximum, then calls `LinearStatScaling.bonus`.

- [ ] **Step 5: Confirm GREEN and commit**

Run focused tests and full tests, then commit only the listed files with:

```powershell
git commit -m "feat: require mobility attribute providers"
```

### Task 3: Apply mobility attributes and refresh relevant XP level changes

**Files:**
- Modify: `src/main/java/tong/statmod/effects/PlayerAttributeEffects.java`
- Create: `src/main/java/tong/statmod/effects/AttributeEffectLevels.java`
- Modify: `src/main/java/tong/statmod/progression/xp/XpAwardService.java`
- Modify: `src/test/java/tong/statmod/effects/PlayerAttributeEffectsContractTest.java`
- Create: `src/test/java/tong/statmod/effects/AttributeEffectLevelsTest.java`
- Modify: `src/test/java/tong/statmod/effects/PlayerAttributeEffectsWiringTest.java`

**Interfaces:**
- Produces: `AttributeEffectLevels.from(PlayerStats)` and mobility application through `PlayerAttributeEffects.refresh(ServerPlayer)`.
- Preserves: one refresh at most per automatic XP award.

- [ ] **Step 1: Write failing behavior/contract tests**

Test `AttributeEffectLevels.from` captures Rapidité, Agility, and Physical Endurance and that records compare by value. Extend adapter contract assertions for `MobilityAttributeTarget.values()` and one shared replacement helper. Update wiring assertions to require before/after `AttributeEffectLevels` comparison instead of Endurance-only integers.

- [ ] **Step 2: Confirm RED**

Run the three focused effect test classes.

Expected: failures because the level record and mobility loop are missing.

- [ ] **Step 3: Implement the level snapshot**

```java
public record AttributeEffectLevels(int rapidite, int agility, int physicalEndurance) {
    public static AttributeEffectLevels from(PlayerStats stats) {
        return new AttributeEffectLevels(
                stats.get(StatType.RAPIDITE).level(),
                stats.get(StatType.AGILITY).level(),
                stats.get(StatType.PHYSICAL_ENDURANCE).level());
    }
}
```

- [ ] **Step 4: Refactor the adapter and XP service**

Add a shared private helper accepting registry ID, UUID, name, and amount. Iterate Endurance and mobility targets, routing both through the helper. In `XpAwardService`, capture `AttributeEffectLevels.from(stats)` before coordinator application and refresh once only when the after snapshot differs.

- [ ] **Step 5: Confirm GREEN and commit**

Run focused and full tests. Commit the six listed files with:

```powershell
git commit -m "feat: apply rapidity and agility attributes"
```

### Task 4: Documentation, artifact contract, and live provider verification

**Files:**
- Modify: `docs/compatibility/stat-attribute-provider-matrix.md`
- Modify: `scripts/verify-clean-foundation.ps1`
- Modify: `src/test/java/tong/statmod/SupportedRuntimeContractTest.java`

**Interfaces:**
- Verifies: metadata, bridge classes, required-provider runtime, and absence of embedded provider classes.

- [ ] **Step 1: Write the failing runtime documentation contract**

Extend `SupportedRuntimeContractTest` to require documented minimum versions `Epic Fight 20.14.17` and `Pufferfish's Attributes 0.8.2`, and the exact four mobility mappings.

- [ ] **Step 2: Confirm RED**

Run: `.\gradlew.bat test --tests tong.statmod.SupportedRuntimeContractTest`

Expected: failure until compatibility documentation is updated.

- [ ] **Step 3: Update documentation and artifact verifier**

Mark Rapidité/Agility implemented, document required providers and defaults, and advance implementation order to Iron's Spells. Require `LinearStatScaling.class`, `MobilityAttributeTarget.class`, and `AttributeEffectLevels.class` in the JAR; remove the deleted `PhysicalEnduranceScaling.class` requirement.

- [ ] **Step 4: Run complete build verification**

```powershell
.\gradlew.bat clean test build
powershell -ExecutionPolicy Bypass -File scripts\verify-clean-foundation.ps1 -Mode After
```

Expected: tests/build succeed and verifier reports 74/74 addon entries with one valid STAT Mod JAR.

- [ ] **Step 5: Run GameTest with exact required providers**

Invoke `scripts/smoke-gametest-server.ps1 -ProviderModsDirectory <validated-client-mods>`. The script copies the two exact audited filenames to a safe build directory, exposes them through a temporary flat-directory repository, applies `fg.deobf`, enables Mixin refmap remapping, runs GameTest, and removes only its exact temporary files in a `finally` block. Expected: Epic Fight 20.14.17 and Puffish Attributes 0.8.2 load; server starts, saves, stops, and the smoke script reports OK.

- [ ] **Step 6: Final diff and commit**

Run `git diff --check`, confirm unrelated modpack script changes are unstaged, then commit the three listed files with:

```powershell
git commit -m "docs: validate mobility attribute bridge"
```
