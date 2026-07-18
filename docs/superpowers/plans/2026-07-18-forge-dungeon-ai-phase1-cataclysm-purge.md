# Dungeon AI Phase 1 — Cataclysm Purge and Smart Replacements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove every Cataclysm entity from the active Trial Dungeon and replace those slots with Stat Mod tactical adventurers, Iron's Spells casters, and compatible non-Cataclysm mobs.

**Architecture:** A source-contract test makes the no-Cataclysm rule permanent. Static rosters are rewritten by encounter function, while `statmod:adventurer` actors receive deterministic tactical roles at spawn time. Boss replacements use named multi-actor encounters where a single Cataclysm boss previously supplied all mechanics.

**Tech Stack:** Java 17, Forge 47.4.4, Minecraft 1.20.1, JUnit 5, Iron's Spells 3.16.2, existing STAT Mod dungeon and party systems.

## Global Constraints

- Do not remove the Cataclysm JAR from the modpack; only forbid `cataclysm:*` entities in the Trial Dungeon.
- No active source, resource or default server configuration may contain `cataclysm:` after this phase.
- Historical documents under `docs/` are exempt from the source contract.
- Iron's Spells remains required at version `1.20.1-3.16.2` or newer.
- Existing user-owned untracked install and website directories remain untouched.
- Every implementation task follows red-green-refactor and ends in a scoped commit.

---

### Task 1: Permanent no-Cataclysm source contract

**Files:**
- Create: `src/test/java/tong/statmod/dungeon/DungeonCataclysmExclusionContractTest.java`

**Interfaces:**
- Consumes: active source roots `src/main/java/tong/statmod/config`, `src/main/java/tong/statmod/dungeon`, and `src/main/resources/data/statmod`.
- Produces: a build-breaking contract whenever an active dungeon file contains `cataclysm:`.

- [ ] **Step 1: Write the failing contract test**

```java
package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DungeonCataclysmExclusionContractTest {
    @Test
    void activeDungeonContentNeverReferencesCataclysmEntities() throws Exception {
        List<Path> roots = List.of(
                Path.of("src/main/java/tong/statmod/config"),
                Path.of("src/main/java/tong/statmod/dungeon"),
                Path.of("src/main/resources/data/statmod"));
        List<String> offenders = new ArrayList<>();
        for (Path root : roots) {
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        if (Files.readString(path).toLowerCase().contains("cataclysm:")) {
                            offenders.add(path.toString());
                        }
                    } catch (java.io.IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            }
        }
        assertTrue(offenders.isEmpty(), "Cataclysm dungeon references: " + offenders);
    }
}
```

- [ ] **Step 2: Run the test and verify the expected red state**

Run: `./gradlew test --tests tong.statmod.dungeon.DungeonCataclysmExclusionContractTest`

Expected: FAIL listing `StatModServerConfig`, `DungeonBossRoster`, `DungeonThemes`, `DungeonMobSpawner`, `DungeonSecretRoom`, `DungeonUltraVault`, `ModdedMobPool`, and `dungeon_boss.json`.

- [ ] **Step 3: Commit the failing contract**

```bash
git add src/test/java/tong/statmod/dungeon/DungeonCataclysmExclusionContractTest.java
git commit -m "test: forbid Cataclysm entities in trial dungeon"
```

---

### Task 2: Deterministic tactical roles for replacement adventurers

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/party/AdventurerPartyHelper.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java`
- Create: `src/test/java/tong/statmod/dungeon/party/DungeonAdventurerRolePolicyTest.java`
- Create: `src/main/java/tong/statmod/dungeon/party/DungeonAdventurerRolePolicy.java`

**Interfaces:**
- Produces: `PartyRole DungeonAdventurerRolePolicy.roleFor(int floor, int roomIndex, int ordinal, boolean boss)`.
- Produces: `void AdventurerPartyHelper.configureRole(Mob entity, PartyRole role, int floor)`.
- Consumes: `PartyRole`, existing equipment methods and `ensureRoleAi(Mob)`.

- [ ] **Step 1: Write the failing pure policy test**

```java
package tong.statmod.dungeon.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class DungeonAdventurerRolePolicyTest {
    @Test void selectionIsDeterministic() {
        assertEquals(
                DungeonAdventurerRolePolicy.roleFor(42, 3, 1, false),
                DungeonAdventurerRolePolicy.roleFor(42, 3, 1, false));
    }

    @Test void bossSquadStartsWithTankThenUsesMagicSupport() {
        assertEquals(PartyRole.TANK, DungeonAdventurerRolePolicy.roleFor(60, 0, 0, true));
        assertEquals(PartyRole.MAGE, DungeonAdventurerRolePolicy.roleFor(60, 0, 1, true));
        assertEquals(PartyRole.HEALER, DungeonAdventurerRolePolicy.roleFor(60, 0, 2, true));
    }
}
```

- [ ] **Step 2: Verify the policy test fails to compile**

Run: `./gradlew test --tests tong.statmod.dungeon.party.DungeonAdventurerRolePolicyTest`

Expected: FAIL because `DungeonAdventurerRolePolicy` does not exist.

- [ ] **Step 3: Implement the pure role policy**

```java
package tong.statmod.dungeon.party;

public final class DungeonAdventurerRolePolicy {
    private static final PartyRole[] STANDARD = {
            PartyRole.TANK, PartyRole.ARCHER, PartyRole.ASSASSIN,
            PartyRole.MAGE, PartyRole.HEALER
    };
    private static final PartyRole[] BOSS = {
            PartyRole.TANK, PartyRole.MAGE, PartyRole.HEALER,
            PartyRole.ASSASSIN, PartyRole.ARCHER
    };

    private DungeonAdventurerRolePolicy() {}

    public static PartyRole roleFor(int floor, int roomIndex, int ordinal, boolean boss) {
        PartyRole[] roles = boss ? BOSS : STANDARD;
        int offset = boss ? 0 : Math.floorMod(floor * 31 + roomIndex * 17, roles.length);
        return roles[Math.floorMod(offset + ordinal, roles.length)];
    }
}
```

- [ ] **Step 4: Expose idempotent role configuration**

Move the existing tag, equipment, follow-range, spawn-effect and goal-attachment sequence into:

```java
public static void configureRole(Mob entity, PartyRole role, int floor) {
    entity.getPersistentData().putString(PartyRole.TAG, role.name());
    entity.setPersistenceRequired();
    equipForRole(entity, role, floor);
    var followRange = entity.getAttribute(Attributes.FOLLOW_RANGE);
    if (followRange != null) followRange.setBaseValue(48.0);
    applySpawnEffects(entity, role);
    ensureRoleAi(entity);
}
```

Update `spawnPartyAt` to call `configureRole` rather than duplicating those operations.

- [ ] **Step 5: Configure replacement adventurers at both spawn boundaries**

In the deferred mob spawn path, after a successful spawn:

```java
if (entity instanceof tong.statmod.entity.AdventurerEntity adventurer) {
    int ordinal = Math.floorMod(entity.getId(), PartyRole.values().length);
    PartyRole role = DungeonAdventurerRolePolicy.roleFor(
            p.floor(), p.roomIndex(), ordinal, p.role() == DungeonMobScaling.MobRole.BOSS);
    AdventurerPartyHelper.configureRole(adventurer, role, p.floor());
}
```

In `DungeonBossAltarBlock`, configure each spawned `AdventurerEntity` with `boss=true` and the current boss slot before registering it with `DungeonBossTracker`.

- [ ] **Step 6: Run focused and existing party tests**

Run: `./gradlew test --tests "tong.statmod.dungeon.party.*"`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/party src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java src/test/java/tong/statmod/dungeon/party
git commit -m "feat: assign tactical roles to dungeon adventurers"
```

---

### Task 3: Replace default pools and scripted encounters

**Files:**
- Modify: `src/main/java/tong/statmod/config/StatModServerConfig.java`
- Modify: `src/main/java/tong/statmod/dungeon/ModdedMobPool.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonSecretRoom.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonUltraVault.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonSpawnGuard.java`

**Interfaces:**
- Consumes: registered `statmod:adventurer`, Iron's caster entity IDs, and existing `ModdedMobPool.resolve`.
- Produces: ordinary and scripted pools with zero Cataclysm identifiers.

- [ ] **Step 1: Replace Cataclysm defaults by encounter function**

Use this fixed replacement vocabulary throughout server defaults:

```text
soldier / draugr / koboleton -> statmod:adventurer, slu:armed_hollow, slu:elite_knight
fire elite -> statmod:adventurer, irons_spellbooks:pyromancer, minecraft:blaze
abyss caster -> irons_spellbooks:necromancer, irons_spellbooks:cryomancer
aquatic unit -> minecraft:drowned, minecraft:guardian, minecraft:elder_guardian
end unit -> minecraft:enderman, minecraft:shulker, slu:white_phantom
construct -> irons_spellbooks:cursed_armor_stand, minecraft:iron_golem
```

The three config lists remain non-empty and preserve increasing difficulty.

- [ ] **Step 2: Remove the Cataclysm pool method and registration**

Delete `addCataclysmMobs` and its call. Add `statmod:adventurer` to every tier in `addDungeonMages`, then update `resolve` so the real registered entity resolves normally; retain only the legacy pseudo-mage fallbacks still used elsewhere.

- [ ] **Step 3: Replace scripted archetypes**

Use these exact candidate lists:

```java
case 3 -> candidates = List.of(
        "statmod:adventurer", "irons_spellbooks:necromancer",
        "epic_mobs:lost_wanderer", "minecraft:stray");
default -> candidates = List.of(
        "statmod:adventurer", "irons_spellbooks:magehunter_vindicator",
        "epic_mobs:nameless_knight", "minecraft:piglin_brute", "minecraft:vindicator");
```

- [ ] **Step 4: Replace secret-room and ultra-vault champions**

Replace Harbinger slots with `irons_spellbooks:dead_king` and Prowler slots with `statmod:adventurer`. Preserve existing vanilla fallback behavior.

- [ ] **Step 5: Remove Cataclysm from the boss-summon namespace allowance**

Delete the `cataclysm` namespace branch from `DungeonSpawnGuard.isBossModNamespace`. Do not alter authorization for Iron's Spells, SLU, Epic Mobs, Mowzie's Mobs or Bosses of Mass Destruction.

- [ ] **Step 6: Run the exclusion contract**

Run: `./gradlew test --tests tong.statmod.dungeon.DungeonCataclysmExclusionContractTest`

Expected: still FAIL, but only `DungeonThemes`, `DungeonBossRoster`, and `dungeon_boss.json` remain.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/tong/statmod/config/StatModServerConfig.java src/main/java/tong/statmod/dungeon
git commit -m "refactor: replace Cataclysm dungeon mob pools"
```

---

### Task 4: Rewrite all 100 themes without Cataclysm

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonThemes.java`
- Create: `src/test/java/tong/statmod/dungeon/DungeonThemesContractTest.java`

**Interfaces:**
- Consumes: `DungeonThemes.forFloor(int)` and `Theme(String, List<String>, List<String>)`.
- Produces: 100 non-empty themed encounter definitions using compatible mobs and `statmod:adventurer` tactical actors.

- [ ] **Step 1: Add the theme shape test**

```java
package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DungeonThemesContractTest {
    @Test void everyBaseFloorHasUsableCataclysmFreePools() {
        for (int floor = 1; floor <= 100; floor++) {
            DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
            assertNotNull(theme, "floor " + floor);
            assertFalse(theme.adds().isEmpty(), "adds floor " + floor);
            assertFalse(theme.miniBoss().isEmpty(), "mini boss floor " + floor);
            theme.adds().forEach(id -> assertFalse(id.startsWith("cataclysm:"), id));
            theme.miniBoss().forEach(id -> assertFalse(id.startsWith("cataclysm:"), id));
        }
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `./gradlew test --tests tong.statmod.dungeon.DungeonThemesContractTest`

Expected: FAIL on the first remaining Cataclysm theme.

- [ ] **Step 3: Apply the complete contextual replacement map**

Replace every occurrence according to this exact map:

```text
ignited_berserker -> irons_spellbooks:pyromancer
ignited_revenant -> statmod:adventurer
royal_draugr -> statmod:adventurer
elite_draugr -> slu:elite_knight
draugr -> slu:armed_hollow
koboleton -> statmod:adventurer
the_prowler -> statmod:adventurer
deepling -> minecraft:drowned
deepling_brute -> minecraft:guardian
deepling_warlock -> irons_spellbooks:cryomancer
deepling_angler -> minecraft:drowned
deepling_priest -> irons_spellbooks:necromancer
amethyst_crab -> minecraft:guardian
urchinkin -> minecraft:pufferfish
coral_golem -> minecraft:elder_guardian
coralssus -> minecraft:elder_guardian
wadjet -> irons_spellbooks:cryomancer
drowned_host -> minecraft:drowned
hippocamtus -> alexsmobs:bone_serpent
netherite_monstrosity -> slu:magma_giant
the_harbinger -> irons_spellbooks:dead_king
aptrgangr -> slu:dark_knight
endermaptera -> minecraft:endermite
ender_golem -> irons_spellbooks:cursed_armor_stand
the_leviathan -> bosses_of_mass_destruction:obsidilith
scylla -> slu:monster_successor
```

Update the class documentation to list Stat Mod adventurers and Iron's Spells instead of Cataclysm.

- [ ] **Step 4: Run both theme and exclusion tests**

Run: `./gradlew test --tests tong.statmod.dungeon.DungeonThemesContractTest --tests tong.statmod.dungeon.DungeonCataclysmExclusionContractTest`

Expected: exclusion test now reports only boss roster/resource occurrences.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonThemes.java src/test/java/tong/statmod/dungeon/DungeonThemesContractTest.java
git commit -m "feat: replace Cataclysm theme encounters with tactical AI"
```

---

### Task 5: Replace Cataclysm boss slots and boss tag

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossRoster.java`
- Modify: `src/main/resources/data/statmod/tags/entity_type/dungeon_boss.json`
- Create: `src/test/java/tong/statmod/dungeon/DungeonBossRosterContractTest.java`

**Interfaces:**
- Consumes: `DungeonBossRoster.forFloor(int)`.
- Produces: boss entries containing one to three compatible entity IDs and no Cataclysm namespace.

- [ ] **Step 1: Write the roster test**

```java
package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DungeonBossRosterContractTest {
    @Test void firstThousandFloorsNeverSelectCataclysm() {
        for (int floor = 10; floor <= 1000; floor += 10) {
            var roster = DungeonBossRoster.forFloor(floor);
            assertFalse(roster.isEmpty(), "floor " + floor);
            for (var entry : roster) {
                assertFalse(entry.entityId().contains("cataclysm:"), entry.entityId());
                assertTrue(entry.entityId().split(",").length <= 3, entry.entityId());
            }
        }
    }
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew test --tests tong.statmod.dungeon.DungeonBossRosterContractTest`

Expected: FAIL on a Cataclysm boss entry.

- [ ] **Step 3: Replace boss entries with tactical equivalents**

Use these substitutions:

```text
ender_golem -> irons_spellbooks:citadel_keeper
the_leviathan -> bosses_of_mass_destruction:obsidilith
netherite_monstrosity -> slu:magma_giant
the_harbinger -> irons_spellbooks:dead_king + irons_spellbooks:citadel_keeper (duo)
the_prowler -> statmod:adventurer + statmod:adventurer + statmod:adventurer (wave)
```

The triple adventurer wave is configured as tank, mage and healer by `DungeonAdventurerRolePolicy`.

- [ ] **Step 4: Remove Cataclysm entries from the entity tag**

Delete all six `cataclysm:*` objects from `dungeon_boss.json`. Ensure the JSON remains valid and retains all non-Cataclysm bosses.

- [ ] **Step 5: Run all three contracts**

Run: `./gradlew test --tests tong.statmod.dungeon.DungeonBossRosterContractTest --tests tong.statmod.dungeon.DungeonThemesContractTest --tests tong.statmod.dungeon.DungeonCataclysmExclusionContractTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonBossRoster.java src/main/resources/data/statmod/tags/entity_type/dungeon_boss.json src/test/java/tong/statmod/dungeon/DungeonBossRosterContractTest.java
git commit -m "feat: replace Cataclysm bosses with tactical encounters"
```

---

### Task 6: Full validation and deployment readiness

**Files:**
- Modify: `docs/forge-1.20.1-server-validation.md`

**Interfaces:**
- Consumes: completed phase and build artifact.
- Produces: auditable validation record and a deployable Forge JAR.

- [ ] **Step 1: Verify active source has no forbidden identifiers**

Run: `rg -n -i "cataclysm:" src/main/java/tong/statmod/config src/main/java/tong/statmod/dungeon src/main/resources/data/statmod`

Expected: exit 1 with no matches.

- [ ] **Step 2: Run a clean full build**

Run: `./gradlew clean test build`

Expected: `BUILD SUCCESSFUL`; all tests and reobfuscation tasks pass.

- [ ] **Step 3: Record the validation**

Append the commit, test command, zero-match scan and built JAR SHA-256 to `docs/forge-1.20.1-server-validation.md`.

- [ ] **Step 4: Commit validation evidence**

```bash
git add docs/forge-1.20.1-server-validation.md
git commit -m "docs: validate Cataclysm-free dungeon AI baseline"
```

- [ ] **Step 5: Push the stable phase**

Run: `git push origin forge-1.20.1`

Expected: remote branch advances to the validation commit.
