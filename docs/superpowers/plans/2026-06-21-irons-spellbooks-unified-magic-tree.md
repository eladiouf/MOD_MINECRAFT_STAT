# Iron's Spellbooks Unified Magic Tree Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Phase 1 vertical slice integrating Iron's Spellbooks as the runtime spell layer for STAT Mod's unified magic tree — common arcane trunk + fully playable Fire branch, with the four elemental trunks and five late-game branches structurally visible but locked.

**Architecture:** STAT Mod owns magical identity (race, stats, tree state, learning, unlocks). Iron's Spellbooks owns native mana, casting, spellbooks, and spell behavior. They communicate through one event bridge that listens to `SpellPreCastEvent` (lock-out check), `SpellOnCastEvent` (progression award), and `ModifySpellLevelEvent` (tier-gated spell level clamp). Tree data lives in a single immutable static catalog (`MagicTreeCatalog`) with all nodes, branches and prereqs. Player state is stored in `PlayerStatData` via three new arrays — `arcanePoints`, `schoolPoints[9 branches]`, `learnedSpells` — plus a long-encoded race id. Eligibility and unlock are factored into two pure services (`MagicEligibilityResolver`, `MagicTreeProgressionService`). The existing Puffish tree compat is extended with one new category set so the UI is free of new client code.

**Tech Stack:** NeoForge 1.21.1, Java 21, Iron's Spellbooks 3.16.1 API (`io.redspace.ironsspellbooks.api.events.*`, `api.registry.SchoolRegistry`, `api.spells.AbstractSpell`, `api.magic.MagicData`), Puffish Skills via existing reflection gateway, NeoForge `AttachmentType`, `CustomPacketPayload` network, JUnit Jupiter 5.10.

**Key spec mapping:**
- Spec §3.1 Source of Truth → Tasks 1, 6, 9
- Spec §3.2 Shared identity → Task 6 (`MagicEligibilityResolver` reads `ARCANE_POWER`, `MANA_POOL`, `CASTING_SPEED`, `ERUDITION`, affinities)
- Spec §3.3 Mana policy → Task 9 (no mana replacement; only event-based gating)
- Spec §3.4 Learning policy → Task 9 (`SpellPreCastEvent` cancel)
- Spec §4 Tree topology → Tasks 3, 4, 5, 13
- Spec §5 Race / starting structure → Tasks 6, 17, 18
- Spec §6 Spell / school unlock model → Tasks 6, 7, 10
- Spec §7 Progression resource model → Tasks 4, 5, 8
- Spec §8 Cast-driven progression → Task 10
- Spec §9 Equipment policy → Task 11 (`InscribeSpellEvent` clamp)
- Spec §10 Onboarding → Task 17
- Spec §11 Phase 1 active content → entire plan
- Spec §12 Implementation boundaries → file structure below
- Spec §13 Fail-safe rules → Tasks 1, 9, 14
- Spec §14 Test strategy → Tasks 16, 17

---

## File Structure

### New units

- `src/main/java/tong/statmod/integration/ironspells/IronSpellsCompat.java` — optional-mod bootstrap, event registration, NoOp when Iron's absent
- `src/main/java/tong/statmod/integration/ironspells/IronSchoolMapping.java` — pure mapping Iron's `SchoolType` ResourceLocation → `MagicBranch`
- `src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java` — `SpellPreCastEvent`, `SpellOnCastEvent`, `ModifySpellLevelEvent`, `InscribeSpellEvent` handlers
- `src/main/java/tong/statmod/integration/ironspells/IronSpellsApiAdapter.java` — thin facade isolating direct `irons_spellbooks` API calls for testability
- `src/main/java/tong/statmod/magic/MagicBranch.java` — enum of 9 branches (4 elemental + 5 late-game + 1 common trunk identifier)
- `src/main/java/tong/statmod/magic/MagicRace.java` — enum of 4 races with two affinity slots each
- `src/main/java/tong/statmod/magic/MagicNode.java` — record describing a single node (id, branch, tier, kind, cost, prereqs, learnedSpellIds)
- `src/main/java/tong/statmod/magic/MagicNodeKind.java` — enum `TRUNK_FOUNDATION`, `BRANCH_OPENER`, `BRANCH_TIER`, `SIGNATURE_SPELL`, `LATEGAME_GATE`
- `src/main/java/tong/statmod/magic/MagicTier.java` — enum `T1..T4` for branch-internal tiers
- `src/main/java/tong/statmod/magic/MagicTreeCatalog.java` — static immutable Phase 1 tree definition
- `src/main/java/tong/statmod/magic/MagicEligibilityResolver.java` — pure race/stat/prereq check service
- `src/main/java/tong/statmod/magic/MagicTreeProgressionService.java` — unlock node, spend points, validate
- `src/main/java/tong/statmod/magic/SchoolProgressTracker.java` — mastery accumulator + threshold → school-point award
- `src/main/java/tong/statmod/magic/CastRewardPolicy.java` — pure function: cast context → (mastery delta, arcane delta)
- `src/main/java/tong/statmod/magic/CastContext.java` — record holding the cast facts the policy needs (spellId, branch, mana fraction, hadImpact, casterStats, durationTicks)
- `src/main/java/tong/statmod/magic/MagicTreeViewModel.java` — pure builder turning catalog + player state into per-node visibility/lock data for the UI mirror
- `src/main/java/tong/statmod/network/MagicTreeSyncPayload.java` — sync arcanePoints / schoolPoints / learnedSpells / race to client
- `src/main/java/tong/statmod/network/UnlockMagicNodePayload.java` — client → server unlock request
- `src/main/java/tong/statmod/network/ChooseRaceAffinityPayload.java` — client → server initial racial branch pick
- `src/main/java/tong/statmod/integration/puffish/PuffishMagicCategoryIds.java` — mapping `MagicNode.id` ↔ Puffish category/skill ids
- `src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java` — emits Puffish category/skill/connection state from catalog + player state
- `src/main/java/tong/statmod/client/cache/ClientMagicCache.java` — client-side mirror of arcane/school points and learned spells
- `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/category.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/definitions.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/skills.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/connections.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_fire/{category,definitions,skills,connections}.json`
- `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_locked/{category,definitions,skills,connections}.json` (single category aggregating Water, Air, Earth, Holy, Blood, Ender, Evocation, Eldritch placeholders)

### New tests

- `src/test/java/tong/statmod/magic/MagicTreeCatalogTest.java`
- `src/test/java/tong/statmod/magic/MagicEligibilityResolverTest.java`
- `src/test/java/tong/statmod/magic/MagicTreeProgressionServiceTest.java`
- `src/test/java/tong/statmod/magic/SchoolProgressTrackerTest.java`
- `src/test/java/tong/statmod/magic/CastRewardPolicyTest.java`
- `src/test/java/tong/statmod/magic/MagicTreeViewModelTest.java`
- `src/test/java/tong/statmod/integration/ironspells/IronSchoolMappingTest.java`
- `src/test/java/tong/statmod/integration/puffish/PuffishMagicCategoryIdsTest.java`

### Existing files to modify

- `src/main/java/tong/statmod/storage/PlayerStatData.java` — add `arcanePoints`, `schoolPoints[]`, `learnedSpells[]`, `magicRaceId`, `chosenStartBranchId`, `schoolMasteryProgress[]` fields + getters/setters
- `src/main/java/tong/statmod/storage/ModAttachments.java` — extend serializer to persist new fields
- `src/main/java/tong/statmod/STATMod.java` — register `IronSpellsCompat.init()` after `PuffishSkillsCompat.init()`
- `src/main/java/tong/statmod/network/NetworkHandler.java` — register the 3 new payloads
- `src/main/java/tong/statmod/network/ServerPayloadHandler.java` — handle `UnlockMagicNodePayload` and `ChooseRaceAffinityPayload`
- `src/main/java/tong/statmod/network/SyncHelper.java` — call `MagicTreeSyncPayload` from the canonical sync path and mirror through Puffish builder
- `src/main/java/tong/statmod/integration/puffish/PuffishSkillsCompat.java` — route unlock callbacks for the new 3 categories to `MagicTreeProgressionService`
- `src/main/java/tong/statmod/integration/puffish/PuffishSyncService.java` — call `PuffishMagicTreeBuilder` after the existing perk sync

---

## Conventions Used Throughout

- Branch ids in catalog: `common`, `fire`, `water`, `air`, `earth`, `holy`, `blood`, `ender`, `evocation`, `eldritch`
- Node ids: `<branch>/<kind>/<slug>` e.g. `fire/opener/ignition`, `common/foundation/arcane_focus`
- Puffish category ids: `statmod:magic_common`, `statmod:magic_fire`, `statmod:magic_locked`
- All work happens server-side unless explicitly client (`isClientSide`)
- All new tests are JUnit Jupiter 5.10, no Minecraft runtime
- Each task ends with `git add <files> && git commit -m "<message>"`

---

### Task 1: Add `MagicBranch` enum

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicBranch.java`

- [ ] **Step 1: Write the failing test**

`src/test/java/tong/statmod/magic/MagicBranchTest.java`:

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagicBranchTest {
    @Test
    void elemental_branches_have_stable_ordinals() {
        assertEquals("common", MagicBranch.COMMON.id);
        assertEquals("fire", MagicBranch.FIRE.id);
        assertEquals("water", MagicBranch.WATER.id);
        assertEquals("air", MagicBranch.AIR.id);
        assertEquals("earth", MagicBranch.EARTH.id);
    }

    @Test
    void late_game_branches_present() {
        assertTrue(MagicBranch.HOLY.lateGame);
        assertTrue(MagicBranch.BLOOD.lateGame);
        assertTrue(MagicBranch.ENDER.lateGame);
        assertTrue(MagicBranch.EVOCATION.lateGame);
        assertTrue(MagicBranch.ELDRITCH.lateGame);
        assertFalse(MagicBranch.FIRE.lateGame);
    }

    @Test
    void resolve_by_id_round_trips() {
        for (MagicBranch b : MagicBranch.values()) {
            assertSame(b, MagicBranch.byId(b.id));
        }
        assertNull(MagicBranch.byId("not_a_branch"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.MagicBranchTest`
Expected: FAIL — `MagicBranch` does not exist.

- [ ] **Step 3: Implement**

```java
package tong.statmod.magic;

public enum MagicBranch {
    COMMON("common", false),
    FIRE("fire", false),
    WATER("water", false),
    AIR("air", false),
    EARTH("earth", false),
    HOLY("holy", true),
    BLOOD("blood", true),
    ENDER("ender", true),
    EVOCATION("evocation", true),
    ELDRITCH("eldritch", true);

    public final String id;
    public final boolean lateGame;

    MagicBranch(String id, boolean lateGame) {
        this.id = id;
        this.lateGame = lateGame;
    }

    public static MagicBranch byId(String id) {
        if (id == null) return null;
        for (MagicBranch b : values()) {
            if (b.id.equals(id)) return b;
        }
        return null;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.MagicBranchTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicBranch.java src/test/java/tong/statmod/magic/MagicBranchTest.java
git commit -m "Add MagicBranch enum for unified magic tree branches"
```

---

### Task 2: Add `MagicTier`, `MagicNodeKind` enums and `MagicNode` record

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicTier.java`
- Create: `src/main/java/tong/statmod/magic/MagicNodeKind.java`
- Create: `src/main/java/tong/statmod/magic/MagicNode.java`
- Test: `src/test/java/tong/statmod/magic/MagicNodeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MagicNodeTest {
    @Test
    void node_record_holds_all_fields() {
        MagicNode n = new MagicNode(
                "fire/opener/ignition",
                MagicBranch.FIRE,
                MagicNodeKind.BRANCH_OPENER,
                MagicTier.T1,
                MagicCurrency.ARCANE,
                3,
                List.of("common/foundation/arcane_focus"),
                Set.of("irons_spellbooks:firebolt"));
        assertEquals("fire/opener/ignition", n.id());
        assertEquals(MagicBranch.FIRE, n.branch());
        assertEquals(3, n.cost());
        assertEquals(Set.of("irons_spellbooks:firebolt"), n.learnedSpells());
    }

    @Test
    void node_id_must_match_branch_prefix() {
        assertThrows(IllegalArgumentException.class, () -> new MagicNode(
                "fire/opener/foo",
                MagicBranch.WATER,
                MagicNodeKind.BRANCH_OPENER,
                MagicTier.T1,
                MagicCurrency.SCHOOL,
                1,
                List.of(),
                Set.of()));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.MagicNodeTest`
Expected: FAIL — `MagicNode`, `MagicTier`, `MagicNodeKind`, `MagicCurrency` missing.

- [ ] **Step 3: Implement supporting types**

`MagicTier.java`:

```java
package tong.statmod.magic;

public enum MagicTier { T1, T2, T3, T4 }
```

`MagicNodeKind.java`:

```java
package tong.statmod.magic;

public enum MagicNodeKind {
    TRUNK_FOUNDATION,
    BRANCH_OPENER,
    BRANCH_TIER,
    SIGNATURE_SPELL,
    LATEGAME_GATE
}
```

`MagicCurrency.java`:

```java
package tong.statmod.magic;

public enum MagicCurrency { ARCANE, SCHOOL }
```

`MagicNode.java`:

```java
package tong.statmod.magic;

import java.util.List;
import java.util.Set;

public record MagicNode(
        String id,
        MagicBranch branch,
        MagicNodeKind kind,
        MagicTier tier,
        MagicCurrency currency,
        int cost,
        List<String> prerequisites,
        Set<String> learnedSpells
) {
    public MagicNode {
        if (id == null || branch == null || kind == null || tier == null || currency == null) {
            throw new IllegalArgumentException("node fields must not be null");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost must be >= 0");
        }
        String expectedPrefix = branch.id + "/";
        if (!id.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException(
                    "node id '" + id + "' does not match branch prefix '" + expectedPrefix + "'");
        }
        prerequisites = List.copyOf(prerequisites);
        learnedSpells = Set.copyOf(learnedSpells);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.MagicNodeTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/Magic*.java src/test/java/tong/statmod/magic/MagicNodeTest.java
git commit -m "Add MagicNode record with tier, kind and currency types"
```

---

### Task 3: Add `MagicRace` enum

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicRace.java`
- Test: `src/test/java/tong/statmod/magic/MagicRaceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MagicRaceTest {
    @Test
    void human_has_two_weak_universal_affinities() {
        assertEquals(Set.of(MagicBranch.FIRE, MagicBranch.WATER, MagicBranch.AIR, MagicBranch.EARTH),
                MagicRace.HUMAN.naturalAffinities());
        assertTrue(MagicRace.HUMAN.flexible);
    }

    @Test
    void elf_air_water() {
        assertEquals(Set.of(MagicBranch.AIR, MagicBranch.WATER), MagicRace.ELF.naturalAffinities());
        assertFalse(MagicRace.ELF.flexible);
    }

    @Test
    void dwarf_earth_fire() {
        assertEquals(Set.of(MagicBranch.EARTH, MagicBranch.FIRE), MagicRace.DWARF.naturalAffinities());
    }

    @Test
    void beast_water_air_weaker_purity() {
        assertEquals(Set.of(MagicBranch.WATER, MagicBranch.AIR), MagicRace.BEAST.naturalAffinities());
        assertTrue(MagicRace.BEAST.purityPenalty);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.MagicRaceTest`
Expected: FAIL — `MagicRace` missing.

- [ ] **Step 3: Implement**

```java
package tong.statmod.magic;

import java.util.Set;

public enum MagicRace {
    HUMAN(Set.of(MagicBranch.FIRE, MagicBranch.WATER, MagicBranch.AIR, MagicBranch.EARTH), true, false),
    ELF(Set.of(MagicBranch.AIR, MagicBranch.WATER), false, false),
    DWARF(Set.of(MagicBranch.EARTH, MagicBranch.FIRE), false, false),
    BEAST(Set.of(MagicBranch.WATER, MagicBranch.AIR), false, true);

    private final Set<MagicBranch> naturalAffinities;
    public final boolean flexible;
    public final boolean purityPenalty;

    MagicRace(Set<MagicBranch> naturalAffinities, boolean flexible, boolean purityPenalty) {
        this.naturalAffinities = naturalAffinities;
        this.flexible = flexible;
        this.purityPenalty = purityPenalty;
    }

    public Set<MagicBranch> naturalAffinities() { return naturalAffinities; }

    public boolean hasAffinity(MagicBranch branch) {
        return naturalAffinities.contains(branch);
    }

    public static MagicRace byOrdinalOrDefault(int ordinal) {
        MagicRace[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : HUMAN;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.MagicRaceTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicRace.java src/test/java/tong/statmod/magic/MagicRaceTest.java
git commit -m "Add MagicRace enum with racial affinities"
```

---

### Task 4: Build Phase 1 `MagicTreeCatalog`

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicTreeCatalog.java`
- Test: `src/test/java/tong/statmod/magic/MagicTreeCatalogTest.java`

The catalog defines every Phase 1 node. Common trunk holds 4 foundation nodes. Fire holds opener + 3 tier nodes + 4 signature spell nodes. Each other branch holds exactly 1 visible-but-locked placeholder anchor (`<branch>/locked/anchor`) with `cost = 0` and an impossible prerequisite, so it appears in UI but is never spendable.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MagicTreeCatalogTest {
    @Test
    void common_trunk_has_four_foundation_nodes() {
        List<MagicNode> common = MagicTreeCatalog.byBranch(MagicBranch.COMMON);
        assertEquals(4, common.size());
        for (MagicNode n : common) {
            assertEquals(MagicNodeKind.TRUNK_FOUNDATION, n.kind());
            assertEquals(MagicCurrency.ARCANE, n.currency());
        }
        assertNotNull(MagicTreeCatalog.byId("common/foundation/arcane_focus"));
    }

    @Test
    void fire_branch_has_opener_three_tiers_and_four_signatures() {
        List<MagicNode> fire = MagicTreeCatalog.byBranch(MagicBranch.FIRE);
        long openers = fire.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_OPENER).count();
        long tiers = fire.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_TIER).count();
        long sigs = fire.stream().filter(n -> n.kind() == MagicNodeKind.SIGNATURE_SPELL).count();
        assertEquals(1, openers);
        assertEquals(3, tiers);
        assertEquals(4, sigs);
    }

    @Test
    void fire_opener_requires_first_two_common_foundations() {
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        assertTrue(opener.prerequisites().contains("common/foundation/arcane_focus"));
        assertTrue(opener.prerequisites().contains("common/foundation/mana_well"));
    }

    @Test
    void fire_signature_spells_reference_real_irons_ids() {
        MagicNode firebolt = MagicTreeCatalog.byId("fire/signature/firebolt");
        assertTrue(firebolt.learnedSpells().contains("irons_spellbooks:firebolt"));
    }

    @Test
    void other_elemental_and_late_game_branches_are_locked_placeholders() {
        for (MagicBranch b : MagicBranch.values()) {
            if (b == MagicBranch.COMMON || b == MagicBranch.FIRE) continue;
            List<MagicNode> nodes = MagicTreeCatalog.byBranch(b);
            assertEquals(1, nodes.size(), "branch " + b + " has more than placeholder");
            MagicNode anchor = nodes.get(0);
            assertEquals(b.id + "/locked/anchor", anchor.id());
            assertTrue(anchor.prerequisites().contains("__never__"));
        }
    }

    @Test
    void every_prerequisite_resolves_or_is_locked_sentinel() {
        for (MagicNode n : MagicTreeCatalog.all()) {
            for (String prereq : n.prerequisites()) {
                if ("__never__".equals(prereq)) continue;
                assertNotNull(MagicTreeCatalog.byId(prereq),
                        "missing prereq " + prereq + " on " + n.id());
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.MagicTreeCatalogTest`
Expected: FAIL — `MagicTreeCatalog` missing.

- [ ] **Step 3: Implement**

```java
package tong.statmod.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MagicTreeCatalog {
    public static final String LOCKED_SENTINEL = "__never__";

    private static final Map<String, MagicNode> BY_ID = new LinkedHashMap<>();
    private static final Map<MagicBranch, List<MagicNode>> BY_BRANCH = new LinkedHashMap<>();

    static {
        // Common trunk - four arcane foundations, linear chain
        add(new MagicNode("common/foundation/arcane_focus",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of(), Set.of()));
        add(new MagicNode("common/foundation/mana_well",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("common/foundation/cast_discipline",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T2,
                MagicCurrency.ARCANE, 2, List.of("common/foundation/mana_well"), Set.of()));
        add(new MagicNode("common/foundation/multi_school_gate",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T3,
                MagicCurrency.ARCANE, 3, List.of("common/foundation/cast_discipline"), Set.of()));

        // Fire branch - opener, T1 / T2 / T3 tier nodes, 4 signature spells
        add(new MagicNode("fire/opener/ignition",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus", "common/foundation/mana_well"),
                Set.of()));
        add(new MagicNode("fire/tier/ember_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/opener/ignition"), Set.of()));
        add(new MagicNode("fire/tier/flame_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/ember_path"), Set.of()));
        add(new MagicNode("fire/tier/inferno_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("fire/tier/flame_path", "common/foundation/cast_discipline"), Set.of()));

        add(new MagicNode("fire/signature/firebolt",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/ember_path"),
                Set.of("irons_spellbooks:firebolt")));
        add(new MagicNode("fire/signature/burning_dash",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/flame_path"),
                Set.of("irons_spellbooks:burning_dash")));
        add(new MagicNode("fire/signature/fireball",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/flame_path"),
                Set.of("irons_spellbooks:fireball")));
        add(new MagicNode("fire/signature/fire_breath",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T3,
                MagicCurrency.SCHOOL, 3, List.of("fire/tier/inferno_path"),
                Set.of("irons_spellbooks:fire_breath")));

        // Locked placeholders for the other branches
        for (MagicBranch b : MagicBranch.values()) {
            if (b == MagicBranch.COMMON || b == MagicBranch.FIRE) continue;
            add(new MagicNode(b.id + "/locked/anchor",
                    b, MagicNodeKind.LATEGAME_GATE, MagicTier.T1,
                    MagicCurrency.ARCANE, 0, List.of(LOCKED_SENTINEL), Set.of()));
        }
    }

    private MagicTreeCatalog() {}

    private static void add(MagicNode node) {
        if (BY_ID.put(node.id(), node) != null) {
            throw new IllegalStateException("duplicate node id " + node.id());
        }
        BY_BRANCH.computeIfAbsent(node.branch(), k -> new ArrayList<>()).add(node);
    }

    public static MagicNode byId(String id) {
        return BY_ID.get(id);
    }

    public static List<MagicNode> all() {
        return List.copyOf(BY_ID.values());
    }

    public static List<MagicNode> byBranch(MagicBranch branch) {
        return Collections.unmodifiableList(BY_BRANCH.getOrDefault(branch, List.of()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.MagicTreeCatalogTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicCurrency.java src/main/java/tong/statmod/magic/MagicTreeCatalog.java src/test/java/tong/statmod/magic/MagicTreeCatalogTest.java
git commit -m "Define Phase 1 magic tree catalog (common trunk + Fire + locked placeholders)"
```

---

### Task 5: Extend `PlayerStatData` with magic state fields

**Files:**
- Modify: `src/main/java/tong/statmod/storage/PlayerStatData.java`
- Test: `src/test/java/tong/statmod/storage/PlayerStatDataMagicTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.storage;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatDataMagicTest {
    @Test
    void arcane_points_default_zero_and_increment() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(0, d.getArcanePoints());
        d.addArcanePoints(5);
        assertEquals(5, d.getArcanePoints());
        d.addArcanePoints(-3);
        assertEquals(2, d.getArcanePoints());
        d.addArcanePoints(-99);
        assertEquals(0, d.getArcanePoints());
    }

    @Test
    void school_points_per_branch_isolated() {
        PlayerStatData d = new PlayerStatData();
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.addSchoolPoints(MagicBranch.WATER, 1);
        assertEquals(3, d.getSchoolPoints(MagicBranch.FIRE));
        assertEquals(1, d.getSchoolPoints(MagicBranch.WATER));
        assertEquals(0, d.getSchoolPoints(MagicBranch.AIR));
    }

    @Test
    void learned_node_unlock_is_idempotent() {
        PlayerStatData d = new PlayerStatData();
        assertFalse(d.hasMagicNode("fire/opener/ignition"));
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/opener/ignition");
        assertTrue(d.hasMagicNode("fire/opener/ignition"));
        assertEquals(1, d.getMagicNodes().length);
    }

    @Test
    void learned_spells_tracked_separately() {
        PlayerStatData d = new PlayerStatData();
        d.learnSpell("irons_spellbooks:firebolt");
        d.learnSpell("irons_spellbooks:firebolt");
        d.learnSpell("irons_spellbooks:fireball");
        assertTrue(d.hasLearnedSpell("irons_spellbooks:firebolt"));
        assertEquals(2, d.getLearnedSpells().length);
    }

    @Test
    void school_mastery_progress_accumulates_to_threshold() {
        PlayerStatData d = new PlayerStatData();
        d.addSchoolMasteryProgress(MagicBranch.FIRE, 40);
        assertEquals(40, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        d.setSchoolMasteryProgress(MagicBranch.FIRE, 0);
        assertEquals(0, d.getSchoolMasteryProgress(MagicBranch.FIRE));
    }

    @Test
    void magic_race_default_unset() {
        PlayerStatData d = new PlayerStatData();
        assertNull(d.getMagicRace());
        d.setMagicRace(MagicRace.ELF);
        assertEquals(MagicRace.ELF, d.getMagicRace());
    }

    @Test
    void chosen_start_branch_defaults_null() {
        PlayerStatData d = new PlayerStatData();
        assertNull(d.getChosenStartBranch());
        d.setChosenStartBranch(MagicBranch.FIRE);
        assertEquals(MagicBranch.FIRE, d.getChosenStartBranch());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.storage.PlayerStatDataMagicTest`
Expected: FAIL — methods missing.

- [ ] **Step 3: Implement (append to `PlayerStatData.java` before closing brace)**

Add these fields and methods to `src/main/java/tong/statmod/storage/PlayerStatData.java`:

```java
    private int arcanePoints;
    private final int[] schoolPoints = new int[tong.statmod.magic.MagicBranch.values().length];
    private final int[] schoolMasteryProgress = new int[tong.statmod.magic.MagicBranch.values().length];
    private String[] magicNodes = new String[0];
    private String[] learnedSpells = new String[0];
    private tong.statmod.magic.MagicRace magicRace;
    private tong.statmod.magic.MagicBranch chosenStartBranch;

    public int getArcanePoints() { return arcanePoints; }
    public void setArcanePoints(int v) { arcanePoints = Math.max(0, v); }
    public void addArcanePoints(int delta) { arcanePoints = Math.max(0, arcanePoints + delta); }

    public int getSchoolPoints(tong.statmod.magic.MagicBranch b) {
        return b == null ? 0 : schoolPoints[b.ordinal()];
    }
    public void setSchoolPoints(tong.statmod.magic.MagicBranch b, int v) {
        if (b != null) schoolPoints[b.ordinal()] = Math.max(0, v);
    }
    public void addSchoolPoints(tong.statmod.magic.MagicBranch b, int delta) {
        if (b != null) schoolPoints[b.ordinal()] = Math.max(0, schoolPoints[b.ordinal()] + delta);
    }

    public int getSchoolMasteryProgress(tong.statmod.magic.MagicBranch b) {
        return b == null ? 0 : schoolMasteryProgress[b.ordinal()];
    }
    public void setSchoolMasteryProgress(tong.statmod.magic.MagicBranch b, int v) {
        if (b != null) schoolMasteryProgress[b.ordinal()] = Math.max(0, v);
    }
    public void addSchoolMasteryProgress(tong.statmod.magic.MagicBranch b, int delta) {
        if (b != null) schoolMasteryProgress[b.ordinal()] = Math.max(0, schoolMasteryProgress[b.ordinal()] + delta);
    }

    public String[] getMagicNodes() { return magicNodes.clone(); }
    public void setMagicNodes(String[] ids) { magicNodes = ids.clone(); }
    public boolean hasMagicNode(String id) {
        for (String s : magicNodes) if (s.equals(id)) return true;
        return false;
    }
    public boolean addMagicNode(String id) {
        if (id == null || hasMagicNode(id)) return false;
        String[] next = new String[magicNodes.length + 1];
        System.arraycopy(magicNodes, 0, next, 0, magicNodes.length);
        next[magicNodes.length] = id;
        magicNodes = next;
        return true;
    }

    public String[] getLearnedSpells() { return learnedSpells.clone(); }
    public void setLearnedSpells(String[] ids) { learnedSpells = ids.clone(); }
    public boolean hasLearnedSpell(String id) {
        for (String s : learnedSpells) if (s.equals(id)) return true;
        return false;
    }
    public boolean learnSpell(String id) {
        if (id == null || hasLearnedSpell(id)) return false;
        String[] next = new String[learnedSpells.length + 1];
        System.arraycopy(learnedSpells, 0, next, 0, learnedSpells.length);
        next[learnedSpells.length] = id;
        learnedSpells = next;
        return true;
    }

    public tong.statmod.magic.MagicRace getMagicRace() { return magicRace; }
    public void setMagicRace(tong.statmod.magic.MagicRace race) { magicRace = race; }

    public tong.statmod.magic.MagicBranch getChosenStartBranch() { return chosenStartBranch; }
    public void setChosenStartBranch(tong.statmod.magic.MagicBranch b) { chosenStartBranch = b; }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.storage.PlayerStatDataMagicTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/storage/PlayerStatData.java src/test/java/tong/statmod/storage/PlayerStatDataMagicTest.java
git commit -m "Add arcane/school points, learned spells and magic race to PlayerStatData"
```

---

### Task 6: Persist new magic state in `ModAttachments` serializer

**Files:**
- Modify: `src/main/java/tong/statmod/storage/ModAttachments.java`
- Test: `src/test/java/tong/statmod/storage/ModAttachmentsMagicSerializationTest.java`

Read the existing serializer first to find its NBT save/load section. Add corresponding tags `arcanePoints`, `schoolPoints` (`IntArrayTag`), `schoolMastery` (`IntArrayTag`), `magicNodes` (`ListTag` of `StringTag`), `learnedSpells` (`ListTag` of `StringTag`), `magicRace` (`StringTag` of enum name), `chosenStartBranch` (`StringTag` of branch id).

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import static org.junit.jupiter.api.Assertions.*;

class ModAttachmentsMagicSerializationTest {
    @Test
    void round_trip_preserves_magic_state() {
        PlayerStatData d = new PlayerStatData();
        d.setArcanePoints(7);
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.setSchoolMasteryProgress(MagicBranch.FIRE, 42);
        d.addMagicNode("common/foundation/arcane_focus");
        d.learnSpell("irons_spellbooks:firebolt");
        d.setMagicRace(MagicRace.ELF);
        d.setChosenStartBranch(MagicBranch.FIRE);

        HolderLookup.Provider lookup = null;
        CompoundTag tag = ModAttachments.STATS_SERIALIZER.serialize(d, lookup);
        PlayerStatData restored = ModAttachments.STATS_SERIALIZER.deserialize(tag, lookup);

        assertEquals(7, restored.getArcanePoints());
        assertEquals(3, restored.getSchoolPoints(MagicBranch.FIRE));
        assertEquals(42, restored.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertTrue(restored.hasMagicNode("common/foundation/arcane_focus"));
        assertTrue(restored.hasLearnedSpell("irons_spellbooks:firebolt"));
        assertEquals(MagicRace.ELF, restored.getMagicRace());
        assertEquals(MagicBranch.FIRE, restored.getChosenStartBranch());
    }
}
```

This requires the existing serializer to expose `serialize/deserialize` as a small adapter — if the current code uses `IAttachmentSerializer` lambdas, refactor minimally to expose a static `STATS_SERIALIZER` with both methods. Read the current `ModAttachments.java` first.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.storage.ModAttachmentsMagicSerializationTest`
Expected: FAIL — fields not persisted.

- [ ] **Step 3: Implement**

In `ModAttachments.java`, in the NBT save section, add (preserving existing tag names):

```java
        tag.putInt("arcanePoints", data.getArcanePoints());

        int[] sp = new int[tong.statmod.magic.MagicBranch.values().length];
        int[] sm = new int[sp.length];
        for (tong.statmod.magic.MagicBranch b : tong.statmod.magic.MagicBranch.values()) {
            sp[b.ordinal()] = data.getSchoolPoints(b);
            sm[b.ordinal()] = data.getSchoolMasteryProgress(b);
        }
        tag.putIntArray("schoolPoints", sp);
        tag.putIntArray("schoolMastery", sm);

        net.minecraft.nbt.ListTag nodes = new net.minecraft.nbt.ListTag();
        for (String s : data.getMagicNodes()) nodes.add(net.minecraft.nbt.StringTag.valueOf(s));
        tag.put("magicNodes", nodes);

        net.minecraft.nbt.ListTag spells = new net.minecraft.nbt.ListTag();
        for (String s : data.getLearnedSpells()) spells.add(net.minecraft.nbt.StringTag.valueOf(s));
        tag.put("learnedSpells", spells);

        if (data.getMagicRace() != null) tag.putString("magicRace", data.getMagicRace().name());
        if (data.getChosenStartBranch() != null) tag.putString("chosenStartBranch", data.getChosenStartBranch().id);
```

In the NBT load section:

```java
        data.setArcanePoints(tag.getInt("arcanePoints"));

        int[] sp = tag.getIntArray("schoolPoints");
        int[] sm = tag.getIntArray("schoolMastery");
        for (tong.statmod.magic.MagicBranch b : tong.statmod.magic.MagicBranch.values()) {
            int idx = b.ordinal();
            if (idx < sp.length) data.setSchoolPoints(b, sp[idx]);
            if (idx < sm.length) data.setSchoolMasteryProgress(b, sm[idx]);
        }

        net.minecraft.nbt.ListTag nodes = tag.getList("magicNodes", net.minecraft.nbt.Tag.TAG_STRING);
        String[] nodeIds = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) nodeIds[i] = nodes.getString(i);
        data.setMagicNodes(nodeIds);

        net.minecraft.nbt.ListTag spells = tag.getList("learnedSpells", net.minecraft.nbt.Tag.TAG_STRING);
        String[] spellIds = new String[spells.size()];
        for (int i = 0; i < spells.size(); i++) spellIds[i] = spells.getString(i);
        data.setLearnedSpells(spellIds);

        if (tag.contains("magicRace")) {
            try { data.setMagicRace(tong.statmod.magic.MagicRace.valueOf(tag.getString("magicRace"))); }
            catch (IllegalArgumentException ignored) {}
        }
        if (tag.contains("chosenStartBranch")) {
            data.setChosenStartBranch(tong.statmod.magic.MagicBranch.byId(tag.getString("chosenStartBranch")));
        }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.storage.ModAttachmentsMagicSerializationTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/storage/ModAttachments.java src/test/java/tong/statmod/storage/ModAttachmentsMagicSerializationTest.java
git commit -m "Persist arcane/school state and magic race in PlayerStatData serializer"
```

---

### Task 7: Add `MagicEligibilityResolver`

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicEligibilityResolver.java`
- Test: `src/test/java/tong/statmod/magic/MagicEligibilityResolverTest.java`

The resolver is pure: it takes a `PlayerStatData`, `MagicNode`, and (for affinity discount) `MagicRace`, and returns either `OK` or a typed failure. It checks prerequisites, points, and affinity-based cost multipliers.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class MagicEligibilityResolverTest {
    private PlayerStatData fresh() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.ELF);
        return d;
    }

    @Test
    void missing_prereq_fails() {
        PlayerStatData d = fresh();
        d.addArcanePoints(99);
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ,
                MagicEligibilityResolver.evaluate(d, opener).failure());
    }

    @Test
    void not_enough_arcane_fails() {
        PlayerStatData d = fresh();
        MagicNode root = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicEligibilityResolver.Failure.NOT_ENOUGH_POINTS,
                MagicEligibilityResolver.evaluate(d, root).failure());
    }

    @Test
    void ok_when_prereqs_met_and_points_enough() {
        PlayerStatData d = fresh();
        d.addArcanePoints(5);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        assertEquals(MagicEligibilityResolver.Failure.NONE,
                MagicEligibilityResolver.evaluate(d, opener).failure());
    }

    @Test
    void natural_affinity_does_not_change_cost_but_out_of_affinity_inflates_school_cost() {
        PlayerStatData d = fresh();
        d.addMagicNode("fire/opener/ignition");
        d.addSchoolPoints(MagicBranch.FIRE, 1);
        MagicNode ember = MagicTreeCatalog.byId("fire/tier/ember_path");
        // Elf is not naturally fire-aligned, so base cost 1 inflates to 2
        var eval = MagicEligibilityResolver.evaluate(d, ember);
        assertEquals(2, eval.adjustedCost());
        assertEquals(MagicEligibilityResolver.Failure.NOT_ENOUGH_POINTS, eval.failure());
    }

    @Test
    void natural_affinity_keeps_base_cost_for_dwarf_fire() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addMagicNode("fire/opener/ignition");
        d.addSchoolPoints(MagicBranch.FIRE, 1);
        MagicNode ember = MagicTreeCatalog.byId("fire/tier/ember_path");
        var eval = MagicEligibilityResolver.evaluate(d, ember);
        assertEquals(1, eval.adjustedCost());
        assertEquals(MagicEligibilityResolver.Failure.NONE, eval.failure());
    }

    @Test
    void second_natural_branch_cheap_after_first_taken() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.ELF);  // Air + Water
        d.setChosenStartBranch(MagicBranch.AIR);
        // Pretend Water locked-anchor were a real opener (we use the locked sentinel path elsewhere;
        // here we just assert the resolver's second-affinity discount applies on cost calculation)
        int adjustedWater = MagicEligibilityResolver.affinityAdjustedCost(d, MagicBranch.WATER, 4);
        int adjustedFire = MagicEligibilityResolver.affinityAdjustedCost(d, MagicBranch.FIRE, 4);
        assertTrue(adjustedWater < adjustedFire, "second natural branch must be cheaper than out-of-affinity");
    }

    @Test
    void locked_sentinel_prereq_is_unsatisfiable() {
        PlayerStatData d = fresh();
        d.addArcanePoints(99);
        MagicNode lockedAnchor = MagicTreeCatalog.byId("water/locked/anchor");
        assertEquals(MagicEligibilityResolver.Failure.LOCKED,
                MagicEligibilityResolver.evaluate(d, lockedAnchor).failure());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.MagicEligibilityResolverTest`
Expected: FAIL — resolver missing.

- [ ] **Step 3: Implement**

```java
package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

public final class MagicEligibilityResolver {
    public enum Failure { NONE, LOCKED, MISSING_PREREQ, NOT_ENOUGH_POINTS, ALREADY_UNLOCKED, NO_RACE }

    public record Result(Failure failure, int adjustedCost) {}

    private MagicEligibilityResolver() {}

    public static Result evaluate(PlayerStatData data, MagicNode node) {
        if (data == null || node == null) return new Result(Failure.MISSING_PREREQ, 0);
        if (data.hasMagicNode(node.id())) return new Result(Failure.ALREADY_UNLOCKED, node.cost());
        for (String p : node.prerequisites()) {
            if (MagicTreeCatalog.LOCKED_SENTINEL.equals(p)) {
                return new Result(Failure.LOCKED, node.cost());
            }
            if (!data.hasMagicNode(p)) {
                return new Result(Failure.MISSING_PREREQ, node.cost());
            }
        }
        int adjusted = affinityAdjustedCost(data, node.branch(), node.cost());
        int available = switch (node.currency()) {
            case ARCANE -> data.getArcanePoints();
            case SCHOOL -> data.getSchoolPoints(node.branch());
        };
        if (available < adjusted) return new Result(Failure.NOT_ENOUGH_POINTS, adjusted);
        return new Result(Failure.NONE, adjusted);
    }

    public static int affinityAdjustedCost(PlayerStatData data, MagicBranch branch, int baseCost) {
        if (baseCost <= 0 || branch == null || branch == MagicBranch.COMMON) return baseCost;
        MagicRace race = data == null ? null : data.getMagicRace();
        if (race == null) return baseCost * 2;  // unset race acts like out-of-affinity
        if (!race.hasAffinity(branch)) return baseCost * 2;
        MagicBranch start = data.getChosenStartBranch();
        if (start != null && start != branch) {
            // second natural branch is half-rounded-up, minimum 1
            return Math.max(1, (baseCost + 1) / 2);
        }
        return baseCost;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.MagicEligibilityResolverTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicEligibilityResolver.java src/test/java/tong/statmod/magic/MagicEligibilityResolverTest.java
git commit -m "Add MagicEligibilityResolver with race-affinity cost adjustment"
```

---

### Task 8: Add `MagicTreeProgressionService`

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicTreeProgressionService.java`
- Test: `src/test/java/tong/statmod/magic/MagicTreeProgressionServiceTest.java`

The service mutates `PlayerStatData`: spends the adjusted cost, records the node, then unrolls the node's `learnedSpells` into the learned-spell set.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class MagicTreeProgressionServiceTest {
    @Test
    void unlock_records_node_spends_arcane_and_learns_spells() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addArcanePoints(5);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, opener);

        assertTrue(r.success());
        assertTrue(d.hasMagicNode("fire/opener/ignition"));
        assertEquals(3, d.getArcanePoints()); // 5 - 2
    }

    @Test
    void unlock_spends_school_currency_for_school_nodes() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);  // Earth+Fire affinity
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.addMagicNode("fire/opener/ignition");
        MagicNode ember = MagicTreeCatalog.byId("fire/tier/ember_path");

        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, ember);

        assertTrue(r.success());
        assertEquals(2, d.getSchoolPoints(MagicBranch.FIRE));  // 3 - 1
    }

    @Test
    void unlock_signature_node_learns_referenced_spells() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addSchoolPoints(MagicBranch.FIRE, 5);
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/tier/ember_path");
        MagicNode firebolt = MagicTreeCatalog.byId("fire/signature/firebolt");

        assertTrue(MagicTreeProgressionService.tryUnlock(d, firebolt).success());
        assertTrue(d.hasLearnedSpell("irons_spellbooks:firebolt"));
    }

    @Test
    void unlock_failure_returns_typed_reason() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, opener);
        assertFalse(r.success());
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ, r.failure());
    }

    @Test
    void unlock_locked_anchor_refuses_even_when_currency_available() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.HUMAN);
        d.addArcanePoints(99);
        MagicNode locked = MagicTreeCatalog.byId("blood/locked/anchor");
        MagicTreeProgressionService.UnlockResult r = MagicTreeProgressionService.tryUnlock(d, locked);
        assertFalse(r.success());
        assertEquals(MagicEligibilityResolver.Failure.LOCKED, r.failure());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.MagicTreeProgressionServiceTest`
Expected: FAIL — service missing.

- [ ] **Step 3: Implement**

```java
package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

public final class MagicTreeProgressionService {
    public record UnlockResult(boolean success, MagicEligibilityResolver.Failure failure, int spent) {
        public static UnlockResult ok(int spent) {
            return new UnlockResult(true, MagicEligibilityResolver.Failure.NONE, spent);
        }
        public static UnlockResult fail(MagicEligibilityResolver.Failure f) {
            return new UnlockResult(false, f, 0);
        }
    }

    private MagicTreeProgressionService() {}

    public static UnlockResult tryUnlock(PlayerStatData data, MagicNode node) {
        MagicEligibilityResolver.Result eval = MagicEligibilityResolver.evaluate(data, node);
        if (eval.failure() != MagicEligibilityResolver.Failure.NONE) {
            return UnlockResult.fail(eval.failure());
        }
        int adjusted = eval.adjustedCost();
        switch (node.currency()) {
            case ARCANE -> data.addArcanePoints(-adjusted);
            case SCHOOL -> data.addSchoolPoints(node.branch(), -adjusted);
        }
        data.addMagicNode(node.id());
        for (String spell : node.learnedSpells()) data.learnSpell(spell);
        return UnlockResult.ok(adjusted);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.MagicTreeProgressionServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicTreeProgressionService.java src/test/java/tong/statmod/magic/MagicTreeProgressionServiceTest.java
git commit -m "Add MagicTreeProgressionService for node unlock + spell learning"
```

---

### Task 9: Add `IronSchoolMapping`

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSchoolMapping.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronSchoolMappingTest.java`

Iron's `SchoolRegistry` exposes schools as `Holder<SchoolType>` registry entries with ResourceLocations `irons_spellbooks:fire`, `irons_spellbooks:ice`, `irons_spellbooks:lightning`, `irons_spellbooks:nature`, `irons_spellbooks:holy`, `irons_spellbooks:blood`, `irons_spellbooks:ender`, `irons_spellbooks:evocation`, `irons_spellbooks:eldritch`. The mapping converts a school namespace+path to the matching `MagicBranch`.

Spec §4.2 mapping rationale:
- `Fire` ← `irons_spellbooks:fire`
- `Water` ← `irons_spellbooks:ice` (water + ice sustain/control)
- `Air` ← `irons_spellbooks:lightning` (air + lightning + mobility + tempo)
- `Earth` ← `irons_spellbooks:nature` (earth + nature + defense)
- `Holy` ← `irons_spellbooks:holy`
- `Blood` ← `irons_spellbooks:blood`
- `Ender` ← `irons_spellbooks:ender`
- `Evocation` ← `irons_spellbooks:evocation`
- `Eldritch` ← `irons_spellbooks:eldritch`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import static org.junit.jupiter.api.Assertions.*;

class IronSchoolMappingTest {
    @Test
    void irons_fire_maps_to_fire_branch() {
        assertEquals(MagicBranch.FIRE,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "fire"));
    }

    @Test
    void irons_ice_maps_to_water_branch() {
        assertEquals(MagicBranch.WATER,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "ice"));
    }

    @Test
    void irons_lightning_maps_to_air_branch() {
        assertEquals(MagicBranch.AIR,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "lightning"));
    }

    @Test
    void irons_nature_maps_to_earth_branch() {
        assertEquals(MagicBranch.EARTH,
                IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "nature"));
    }

    @Test
    void late_game_schools_map_one_to_one() {
        assertEquals(MagicBranch.HOLY, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "holy"));
        assertEquals(MagicBranch.BLOOD, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "blood"));
        assertEquals(MagicBranch.ENDER, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "ender"));
        assertEquals(MagicBranch.EVOCATION, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "evocation"));
        assertEquals(MagicBranch.ELDRITCH, IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "eldritch"));
    }

    @Test
    void unknown_school_returns_null() {
        assertNull(IronSchoolMapping.fromIronsSchoolId("irons_spellbooks", "unknown_school"));
        assertNull(IronSchoolMapping.fromIronsSchoolId(null, "fire"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.integration.ironspells.IronSchoolMappingTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

```java
package tong.statmod.integration.ironspells;

import tong.statmod.magic.MagicBranch;

import java.util.Map;

public final class IronSchoolMapping {
    private static final String IRONS_NS = "irons_spellbooks";

    private static final Map<String, MagicBranch> BY_PATH = Map.ofEntries(
            Map.entry("fire", MagicBranch.FIRE),
            Map.entry("ice", MagicBranch.WATER),
            Map.entry("lightning", MagicBranch.AIR),
            Map.entry("nature", MagicBranch.EARTH),
            Map.entry("holy", MagicBranch.HOLY),
            Map.entry("blood", MagicBranch.BLOOD),
            Map.entry("ender", MagicBranch.ENDER),
            Map.entry("evocation", MagicBranch.EVOCATION),
            Map.entry("eldritch", MagicBranch.ELDRITCH)
    );

    private IronSchoolMapping() {}

    public static MagicBranch fromIronsSchoolId(String namespace, String path) {
        if (!IRONS_NS.equals(namespace) || path == null) return null;
        return BY_PATH.get(path);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.integration.ironspells.IronSchoolMappingTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/ironspells/IronSchoolMapping.java src/test/java/tong/statmod/integration/ironspells/IronSchoolMappingTest.java
git commit -m "Map Iron's Spellbooks schools to STAT Mod magic branches"
```

---

### Task 10: Add `CastRewardPolicy`

**Files:**
- Create: `src/main/java/tong/statmod/magic/CastContext.java`
- Create: `src/main/java/tong/statmod/magic/CastRewardPolicy.java`
- Test: `src/test/java/tong/statmod/magic/CastRewardPolicyTest.java`

The policy is pure. It returns a `Reward(masteryDelta, arcaneDelta)`. Spec §8 rules:
- baseline valid cast: small mastery reward, no arcane
- meaningful impact (`hadImpact=true`) and full-mana cast: medium reward + 1 arcane
- empty spam protection: no mastery if mana fraction below 5% of base cost (i.e. spell did not actually consume meaningful mana) or if cooldown-skipping detected via `wasFreeCast=true`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CastRewardPolicyTest {
    @Test
    void valid_baseline_cast_gives_small_mastery_no_arcane() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                /*manaFraction*/1.0, /*hadImpact*/false, /*wasFreeCast*/false, /*spellLevel*/1);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertTrue(r.masteryDelta() > 0);
        assertTrue(r.masteryDelta() < 5);
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void meaningful_impact_cast_gives_larger_mastery_and_arcane() {
        CastContext ctx = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 2);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertTrue(r.masteryDelta() >= 5);
        assertEquals(1, r.arcaneDelta());
    }

    @Test
    void empty_spam_protection_for_low_mana_fraction() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                0.01, false, false, 1);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(0, r.masteryDelta());
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void free_cast_yields_no_reward() {
        CastContext ctx = new CastContext("irons_spellbooks:firebolt", MagicBranch.FIRE,
                1.0, true, /*wasFreeCast*/true, 2);
        CastRewardPolicy.Reward r = CastRewardPolicy.evaluate(ctx);
        assertEquals(0, r.masteryDelta());
        assertEquals(0, r.arcaneDelta());
    }

    @Test
    void higher_spell_levels_scale_meaningful_reward() {
        CastContext low = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 1);
        CastContext high = new CastContext("irons_spellbooks:fireball", MagicBranch.FIRE,
                1.0, true, false, 5);
        assertTrue(CastRewardPolicy.evaluate(high).masteryDelta()
                > CastRewardPolicy.evaluate(low).masteryDelta());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.CastRewardPolicyTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

`CastContext.java`:

```java
package tong.statmod.magic;

public record CastContext(
        String spellId,
        MagicBranch branch,
        double manaFraction,
        boolean hadImpact,
        boolean wasFreeCast,
        int spellLevel
) {}
```

`CastRewardPolicy.java`:

```java
package tong.statmod.magic;

public final class CastRewardPolicy {
    public record Reward(int masteryDelta, int arcaneDelta) {
        public static final Reward NONE = new Reward(0, 0);
    }

    private static final double MIN_MANA_FRACTION = 0.05;
    private static final int BASE_MASTERY = 2;
    private static final int IMPACT_MASTERY = 5;

    private CastRewardPolicy() {}

    public static Reward evaluate(CastContext ctx) {
        if (ctx == null || ctx.branch() == null) return Reward.NONE;
        if (ctx.wasFreeCast()) return Reward.NONE;
        if (ctx.manaFraction() < MIN_MANA_FRACTION) return Reward.NONE;

        int mastery = BASE_MASTERY;
        int arcane = 0;
        if (ctx.hadImpact()) {
            mastery = IMPACT_MASTERY + Math.max(0, ctx.spellLevel() - 1);
            arcane = 1;
        }
        return new Reward(mastery, arcane);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.CastRewardPolicyTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/CastContext.java src/main/java/tong/statmod/magic/CastRewardPolicy.java src/test/java/tong/statmod/magic/CastRewardPolicyTest.java
git commit -m "Add CastRewardPolicy with anti-abuse rules"
```

---

### Task 11: Add `SchoolProgressTracker`

**Files:**
- Create: `src/main/java/tong/statmod/magic/SchoolProgressTracker.java`
- Test: `src/test/java/tong/statmod/magic/SchoolProgressTrackerTest.java`

Threshold: each 100 points of mastery converts to 1 school point of that branch, with remainder carried over.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class SchoolProgressTrackerTest {
    @Test
    void below_threshold_accumulates_no_school_points() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 50);
        assertEquals(0, granted);
        assertEquals(50, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(0, d.getSchoolPoints(MagicBranch.FIRE));
    }

    @Test
    void crossing_threshold_grants_one_school_point_and_carries_remainder() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 125);
        assertEquals(1, granted);
        assertEquals(25, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(1, d.getSchoolPoints(MagicBranch.FIRE));
    }

    @Test
    void multi_threshold_batch_in_single_call() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 250);
        assertEquals(2, granted);
        assertEquals(50, d.getSchoolMasteryProgress(MagicBranch.FIRE));
    }

    @Test
    void zero_or_negative_mastery_is_a_noop() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(0, SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 0));
        assertEquals(0, SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, -10));
        assertEquals(0, d.getSchoolMasteryProgress(MagicBranch.FIRE));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.SchoolProgressTrackerTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

```java
package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

public final class SchoolProgressTracker {
    public static final int MASTERY_PER_POINT = 100;

    private SchoolProgressTracker() {}

    public static int applyMastery(PlayerStatData data, MagicBranch branch, int amount) {
        if (data == null || branch == null || amount <= 0) return 0;
        int progress = data.getSchoolMasteryProgress(branch) + amount;
        int granted = progress / MASTERY_PER_POINT;
        int remainder = progress % MASTERY_PER_POINT;
        data.setSchoolMasteryProgress(branch, remainder);
        if (granted > 0) data.addSchoolPoints(branch, granted);
        return granted;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.SchoolProgressTrackerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/SchoolProgressTracker.java src/test/java/tong/statmod/magic/SchoolProgressTrackerTest.java
git commit -m "Add SchoolProgressTracker mastery-to-points converter"
```

---

### Task 12: Add `IronSpellsApiAdapter`

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellsApiAdapter.java`

This adapter is the only file that touches Iron's API directly. It extracts spellId and school branch from `AbstractSpell` and from events. Marked as the boundary that lets event-bridge logic be tested with fake data.

- [ ] **Step 1: Implement directly (no unit test — file is a thin facade, integration-tested via runtime)**

```java
package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.magic.MagicBranch;

public final class IronSpellsApiAdapter {
    private IronSpellsApiAdapter() {}

    public static String spellId(AbstractSpell spell) {
        if (spell == null) return null;
        ResourceLocation id = spell.getSpellResource();
        return id == null ? null : id.toString();
    }

    public static MagicBranch branchOf(AbstractSpell spell) {
        if (spell == null) return null;
        Holder<SchoolType> holder = spell.getSchoolType();
        if (holder == null || !holder.isBound()) return null;
        ResourceLocation key = holder.unwrapKey().map(k -> k.location()).orElse(null);
        if (key == null) return null;
        return IronSchoolMapping.fromIronsSchoolId(key.getNamespace(), key.getPath());
    }

    public static int maxSpellLevel(AbstractSpell spell) {
        if (spell == null) return 0;
        try {
            return AbstractSpell.MAX_LEVEL;
        } catch (Throwable ignored) {
            return 10;
        }
    }
}
```

- [ ] **Step 2: Compile-check**

Run: `./gradlew compileJava`
Expected: PASS (Iron's classes resolve because the jar is in `libs/`).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/integration/ironspells/IronSpellsApiAdapter.java
git commit -m "Add IronSpellsApiAdapter facade isolating direct Iron's API calls"
```

---

### Task 13: Add `IronSpellEventBridge`

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java`
- Test: `src/test/java/tong/statmod/integration/ironspells/IronSpellEventBridgeLogicTest.java`

The bridge listens to:
- `SpellPreCastEvent` — cancels if the caster has not learned the spell (locked-spell rule §3.4 / §6.2)
- `SpellOnCastEvent` — feeds `CastRewardPolicy` and applies mastery/arcane
- `ModifySpellLevelEvent` — clamps spell level to the highest tier the player has reached
- `InscribeSpellEvent` — only allows inscribing spells the player has learned (§9 equipment policy)

The logic that needs testing is pure: a `decide` method that maps inputs to outputs. The NeoForge event glue itself is trivial.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class IronSpellEventBridgeLogicTest {
    @Test
    void unlearned_spell_pre_cast_is_cancelled() {
        PlayerStatData d = new PlayerStatData();
        assertTrue(IronSpellEventBridge.shouldCancelPreCast(d, "irons_spellbooks:firebolt"));
    }

    @Test
    void learned_spell_pre_cast_is_allowed() {
        PlayerStatData d = new PlayerStatData();
        d.learnSpell("irons_spellbooks:firebolt");
        assertFalse(IronSpellEventBridge.shouldCancelPreCast(d, "irons_spellbooks:firebolt"));
    }

    @Test
    void spell_level_clamped_to_player_tier_access() {
        PlayerStatData d = new PlayerStatData();
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/tier/ember_path");
        // T1 reached => clamp to level 2
        assertEquals(2, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));

        d.addMagicNode("fire/tier/flame_path");
        // T2 reached => clamp to level 4
        assertEquals(4, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));

        d.addMagicNode("fire/tier/inferno_path");
        // T3 reached => clamp to level 6
        assertEquals(6, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));
    }

    @Test
    void no_tier_access_clamps_to_one() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(1, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));
    }

    @Test
    void unknown_branch_returns_input_unchanged() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(7, IronSpellEventBridge.clampSpellLevel(d, null, 7));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.integration.ironspells.IronSpellEventBridgeLogicTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

```java
package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import tong.statmod.STATMod;
import tong.statmod.magic.CastContext;
import tong.statmod.magic.CastRewardPolicy;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicNodeKind;
import tong.statmod.magic.MagicTier;
import tong.statmod.magic.SchoolProgressTracker;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class IronSpellEventBridge {
    private IronSpellEventBridge() {}

    @SubscribeEvent
    public static void onPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        String spellId = IronSpellsApiAdapter.spellId(event.getSpell());
        if (shouldCancelPreCast(data, spellId)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("statmod.magic.locked_spell"), true);
        }
    }

    @SubscribeEvent
    public static void onPostCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        String spellId = IronSpellsApiAdapter.spellId(event.getSpell());
        MagicBranch branch = IronSpellsApiAdapter.branchOf(event.getSpell());
        if (branch == null || spellId == null) return;

        MagicData magicData = MagicData.getPlayerMagicData(player);
        double manaFraction = manaFractionOf(magicData);
        boolean hadImpact = guessImpact(event);
        boolean wasFreeCast = manaFraction <= 0;

        CastContext ctx = new CastContext(spellId, branch, manaFraction, hadImpact, wasFreeCast, event.getSpellLevel());
        CastRewardPolicy.Reward reward = CastRewardPolicy.evaluate(ctx);
        if (reward.masteryDelta() > 0) SchoolProgressTracker.applyMastery(data, branch, reward.masteryDelta());
        if (reward.arcaneDelta() > 0) data.addArcanePoints(reward.arcaneDelta());
        STATMod.LOGGER.debug("Cast progression: {} branch={} mastery+={} arcane+={}",
                spellId, branch, reward.masteryDelta(), reward.arcaneDelta());
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        MagicBranch branch = IronSpellsApiAdapter.branchOf(event.getSpell());
        int clamped = clampSpellLevel(data, branch, event.getSpellLevel());
        if (clamped < event.getSpellLevel()) event.setSpellLevel(clamped);
    }

    @SubscribeEvent
    public static void onInscribe(InscribeSpellEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        String spellId = IronSpellsApiAdapter.spellId(event.getSpell());
        if (shouldCancelPreCast(data, spellId)) event.setCanceled(true);
    }

    public static boolean shouldCancelPreCast(PlayerStatData data, String spellId) {
        if (data == null || spellId == null) return true;
        return !data.hasLearnedSpell(spellId);
    }

    public static int clampSpellLevel(PlayerStatData data, MagicBranch branch, int requestedLevel) {
        if (data == null || branch == null) return requestedLevel;
        int tierLevel = highestTierReached(data, branch);
        // tier T0=1, T1=2, T2=4, T3=6 (paced gating)
        int maxByTier = switch (tierLevel) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            default -> 1;
        };
        return Math.min(requestedLevel, maxByTier);
    }

    private static int highestTierReached(PlayerStatData data, MagicBranch branch) {
        int max = 0;
        for (MagicNode node : MagicTreeCatalog.byBranch(branch)) {
            if (node.kind() == MagicNodeKind.BRANCH_TIER && data.hasMagicNode(node.id())) {
                int t = node.tier().ordinal() + 1;
                if (t > max) max = t;
            }
        }
        return max;
    }

    private static double manaFractionOf(MagicData magicData) {
        if (magicData == null) return 1.0;
        float max = magicData.getMaxMana();
        if (max <= 0) return 1.0;
        return Math.max(0.0, 1.0 - (magicData.getMana() / max));
    }

    private static boolean guessImpact(SpellOnCastEvent event) {
        // Heuristic: cast that uses more than a baseline of mana and is not a self-buff is "meaningful".
        // Iron's does not expose a post-hoc impact flag, so we treat any cast that proceeded to completion
        // as baseline-valid; the policy refines based on mana fraction and free-cast.
        return event.getSpellLevel() >= 1;
    }

    private static int highestTierUsedExtended(PlayerStatData data, MagicBranch branch, MagicTier tier) {
        return tier == null ? 0 : tier.ordinal() + 1;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.integration.ironspells.IronSpellEventBridgeLogicTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/integration/ironspells/IronSpellEventBridge.java src/test/java/tong/statmod/integration/ironspells/IronSpellEventBridgeLogicTest.java
git commit -m "Bridge Iron's spell events to STAT Mod learning and progression"
```

---

### Task 14: Add `IronSpellsCompat` bootstrap

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/IronSpellsCompat.java`
- Modify: `src/main/java/tong/statmod/STATMod.java`

- [ ] **Step 1: Implement bootstrap**

```java
package tong.statmod.integration.ironspells;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import tong.statmod.STATMod;

public final class IronSpellsCompat {
    private static final String IRONS_MODID = "irons_spellbooks";
    private static boolean loaded;

    private IronSpellsCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded(IRONS_MODID);
        if (!loaded) {
            STATMod.LOGGER.info("Iron's Spellbooks not detected, skipping IronSpellsCompat");
            return;
        }
        try {
            NeoForge.EVENT_BUS.register(IronSpellEventBridge.class);
            STATMod.LOGGER.info("Iron's Spellbooks integration loaded");
        } catch (Throwable t) {
            loaded = false;
            STATMod.LOGGER.warn("Iron's Spellbooks bridge failed: {}", t.getMessage());
        }
    }

    public static boolean isLoaded() { return loaded; }
}
```

- [ ] **Step 2: Modify `STATMod.java`**

Add import:

```java
import tong.statmod.integration.ironspells.IronSpellsCompat;
```

Add bootstrap call inside the constructor after the existing `PuffishSkillsCompat.init();`:

```java
        IronSpellsCompat.init();
```

- [ ] **Step 3: Build + run minimal smoke**

Run: `./gradlew compileJava`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/integration/ironspells/IronSpellsCompat.java src/main/java/tong/statmod/STATMod.java
git commit -m "Wire IronSpellsCompat bootstrap behind ModList check"
```

---

### Task 15: Add the three network payloads

**Files:**
- Create: `src/main/java/tong/statmod/network/MagicTreeSyncPayload.java`
- Create: `src/main/java/tong/statmod/network/UnlockMagicNodePayload.java`
- Create: `src/main/java/tong/statmod/network/ChooseRaceAffinityPayload.java`
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Modify: `src/main/java/tong/statmod/network/ServerPayloadHandler.java`

- [ ] **Step 1: Implement payloads**

`MagicTreeSyncPayload.java` (server → client):

```java
package tong.statmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

import java.util.List;

public record MagicTreeSyncPayload(
        int arcanePoints,
        int[] schoolPoints,
        int[] schoolMastery,
        List<String> nodes,
        List<String> learnedSpells,
        String magicRace,
        String chosenStartBranch
) implements CustomPacketPayload {
    public static final Type<MagicTreeSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "magic_tree_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicTreeSyncPayload> CODEC =
            StreamCodec.of((buf, p) -> {
                buf.writeInt(p.arcanePoints);
                buf.writeVarInt(p.schoolPoints.length);
                for (int v : p.schoolPoints) buf.writeInt(v);
                buf.writeVarInt(p.schoolMastery.length);
                for (int v : p.schoolMastery) buf.writeInt(v);
                buf.writeVarInt(p.nodes.size());
                for (String s : p.nodes) buf.writeUtf(s);
                buf.writeVarInt(p.learnedSpells.size());
                for (String s : p.learnedSpells) buf.writeUtf(s);
                buf.writeUtf(p.magicRace == null ? "" : p.magicRace);
                buf.writeUtf(p.chosenStartBranch == null ? "" : p.chosenStartBranch);
            }, buf -> {
                int arcane = buf.readInt();
                int[] sp = new int[buf.readVarInt()];
                for (int i = 0; i < sp.length; i++) sp[i] = buf.readInt();
                int[] sm = new int[buf.readVarInt()];
                for (int i = 0; i < sm.length; i++) sm[i] = buf.readInt();
                int nc = buf.readVarInt();
                List<String> nodes = new java.util.ArrayList<>(nc);
                for (int i = 0; i < nc; i++) nodes.add(buf.readUtf());
                int lc = buf.readVarInt();
                List<String> spells = new java.util.ArrayList<>(lc);
                for (int i = 0; i < lc; i++) spells.add(buf.readUtf());
                String race = buf.readUtf();
                String start = buf.readUtf();
                return new MagicTreeSyncPayload(arcane, sp, sm, nodes, spells,
                        race.isEmpty() ? null : race,
                        start.isEmpty() ? null : start);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

`UnlockMagicNodePayload.java` (client → server):

```java
package tong.statmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record UnlockMagicNodePayload(String nodeId) implements CustomPacketPayload {
    public static final Type<UnlockMagicNodePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "unlock_magic_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockMagicNodePayload> CODEC =
            StreamCodec.of((buf, p) -> buf.writeUtf(p.nodeId),
                           buf -> new UnlockMagicNodePayload(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

`ChooseRaceAffinityPayload.java`:

```java
package tong.statmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record ChooseRaceAffinityPayload(String raceName, String startBranchId) implements CustomPacketPayload {
    public static final Type<ChooseRaceAffinityPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "choose_race_affinity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseRaceAffinityPayload> CODEC =
            StreamCodec.of((buf, p) -> {
                buf.writeUtf(p.raceName);
                buf.writeUtf(p.startBranchId);
            }, buf -> new ChooseRaceAffinityPayload(buf.readUtf(), buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 2: Register in `NetworkHandler.java`**

Open the existing `NetworkHandler.java` and inside the registrar block, add:

```java
        registrar.playToClient(MagicTreeSyncPayload.TYPE, MagicTreeSyncPayload.CODEC,
                (payload, context) -> {/* handled in client cache, see Task 18 */});
        registrar.playToServer(UnlockMagicNodePayload.TYPE, UnlockMagicNodePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        ServerPayloadHandler.handleUnlockMagicNode(sp, payload);
                    }
                }));
        registrar.playToServer(ChooseRaceAffinityPayload.TYPE, ChooseRaceAffinityPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        ServerPayloadHandler.handleChooseRaceAffinity(sp, payload);
                    }
                }));
```

- [ ] **Step 3: Implement server handlers in `ServerPayloadHandler.java`**

```java
    public static void handleUnlockMagicNode(net.minecraft.server.level.ServerPlayer player,
                                             tong.statmod.network.UnlockMagicNodePayload payload) {
        tong.statmod.storage.PlayerStatData data = player.getData(tong.statmod.storage.ModAttachments.STATS);
        tong.statmod.magic.MagicNode node = tong.statmod.magic.MagicTreeCatalog.byId(payload.nodeId());
        if (node == null) return;
        var result = tong.statmod.magic.MagicTreeProgressionService.tryUnlock(data, node);
        if (result.success()) {
            tong.statmod.sound.SoundHelper.playPerkUnlock(player);
        }
        tong.statmod.network.SyncHelper.syncMagic(player);
    }

    public static void handleChooseRaceAffinity(net.minecraft.server.level.ServerPlayer player,
                                                tong.statmod.network.ChooseRaceAffinityPayload payload) {
        tong.statmod.storage.PlayerStatData data = player.getData(tong.statmod.storage.ModAttachments.STATS);
        if (data.getMagicRace() != null && data.getChosenStartBranch() != null) return; // already chosen, ignore
        try {
            tong.statmod.magic.MagicRace race = tong.statmod.magic.MagicRace.valueOf(payload.raceName());
            tong.statmod.magic.MagicBranch branch = tong.statmod.magic.MagicBranch.byId(payload.startBranchId());
            if (branch == null || !race.hasAffinity(branch)) return;
            data.setMagicRace(race);
            data.setChosenStartBranch(branch);
        } catch (IllegalArgumentException ignored) {}
        tong.statmod.network.SyncHelper.syncMagic(player);
    }
```

- [ ] **Step 4: Compile check**

Run: `./gradlew compileJava`
Expected: PASS (Task 16 fills in `SyncHelper.syncMagic`).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/network/MagicTreeSyncPayload.java src/main/java/tong/statmod/network/UnlockMagicNodePayload.java src/main/java/tong/statmod/network/ChooseRaceAffinityPayload.java src/main/java/tong/statmod/network/NetworkHandler.java src/main/java/tong/statmod/network/ServerPayloadHandler.java
git commit -m "Add magic-tree network payloads and server handlers"
```

---

### Task 16: Extend `SyncHelper` with `syncMagic`

**Files:**
- Modify: `src/main/java/tong/statmod/network/SyncHelper.java`

- [ ] **Step 1: Implement**

Add to `SyncHelper.java`:

```java
    public static void syncMagic(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) return;
        tong.statmod.storage.PlayerStatData data = player.getData(tong.statmod.storage.ModAttachments.STATS);
        int[] sp = new int[tong.statmod.magic.MagicBranch.values().length];
        int[] sm = new int[sp.length];
        for (tong.statmod.magic.MagicBranch b : tong.statmod.magic.MagicBranch.values()) {
            sp[b.ordinal()] = data.getSchoolPoints(b);
            sm[b.ordinal()] = data.getSchoolMasteryProgress(b);
        }
        tong.statmod.network.MagicTreeSyncPayload payload = new tong.statmod.network.MagicTreeSyncPayload(
                data.getArcanePoints(), sp, sm,
                java.util.List.of(data.getMagicNodes()),
                java.util.List.of(data.getLearnedSpells()),
                data.getMagicRace() == null ? null : data.getMagicRace().name(),
                data.getChosenStartBranch() == null ? null : data.getChosenStartBranch().id);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        tong.statmod.integration.puffish.PuffishMagicTreeBuilder.applyMirror(player, data);
    }
```

Also call `syncMagic(player)` from inside the existing `syncPerks(ServerPlayer)` method so every existing perk sync now also pushes magic state.

- [ ] **Step 2: Build**

Run: `./gradlew compileJava`
Expected: FAIL — `PuffishMagicTreeBuilder` does not exist yet. Defer that line behind a comment until Task 19, OR add a temporary stub class in this task. Use a stub to keep the build green:

`src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java`:

```java
package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.storage.PlayerStatData;

public final class PuffishMagicTreeBuilder {
    private PuffishMagicTreeBuilder() {}
    public static void applyMirror(ServerPlayer player, PlayerStatData data) {
        // Implemented in Task 19
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/network/SyncHelper.java src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java
git commit -m "Sync magic state with perks and call Puffish mirror stub"
```

---

### Task 17: Add `MagicTreeViewModel`

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicTreeViewModel.java`
- Test: `src/test/java/tong/statmod/magic/MagicTreeViewModelTest.java`

A pure builder used by both server-side Puffish projection and any debug command. Returns per-node state: `LOCKED`, `LOCKED_LATE_GAME`, `AVAILABLE`, `UNLOCKED`, `INELIGIBLE_RACE`.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class MagicTreeViewModelTest {
    @Test
    void late_game_branch_anchor_appears_as_locked_late_game() {
        PlayerStatData d = new PlayerStatData();
        var view = MagicTreeViewModel.build(d);
        assertEquals(MagicTreeViewModel.State.LOCKED_LATE_GAME,
                view.stateOf("blood/locked/anchor"));
        assertEquals(MagicTreeViewModel.State.LOCKED_LATE_GAME,
                view.stateOf("eldritch/locked/anchor"));
    }

    @Test
    void water_anchor_for_non_water_race_is_locked_off_affinity() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF); // Earth+Fire only
        var view = MagicTreeViewModel.build(d);
        assertEquals(MagicTreeViewModel.State.LOCKED_OFF_AFFINITY,
                view.stateOf("water/locked/anchor"));
    }

    @Test
    void fire_opener_marked_available_when_prereqs_met() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addArcanePoints(5);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        var view = MagicTreeViewModel.build(d);
        assertEquals(MagicTreeViewModel.State.AVAILABLE, view.stateOf("fire/opener/ignition"));
    }

    @Test
    void unlocked_node_marked_unlocked() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.DWARF);
        d.addMagicNode("common/foundation/arcane_focus");
        var view = MagicTreeViewModel.build(d);
        assertEquals(MagicTreeViewModel.State.UNLOCKED, view.stateOf("common/foundation/arcane_focus"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.magic.MagicTreeViewModelTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

```java
package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

import java.util.HashMap;
import java.util.Map;

public final class MagicTreeViewModel {
    public enum State { UNLOCKED, AVAILABLE, LOCKED_OFF_AFFINITY, LOCKED_LATE_GAME, LOCKED_PREREQ, LOCKED_COST }

    private final Map<String, State> states;

    private MagicTreeViewModel(Map<String, State> states) {
        this.states = Map.copyOf(states);
    }

    public State stateOf(String nodeId) {
        return states.getOrDefault(nodeId, State.LOCKED_PREREQ);
    }

    public static MagicTreeViewModel build(PlayerStatData data) {
        Map<String, State> map = new HashMap<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            map.put(node.id(), classify(data, node));
        }
        return new MagicTreeViewModel(map);
    }

    private static State classify(PlayerStatData data, MagicNode node) {
        if (data.hasMagicNode(node.id())) return State.UNLOCKED;
        if (node.branch().lateGame) return State.LOCKED_LATE_GAME;
        // Off-affinity elemental locked placeholder
        if (node.kind() == MagicNodeKind.LATEGAME_GATE) {
            MagicRace race = data.getMagicRace();
            if (race == null || !race.hasAffinity(node.branch())) return State.LOCKED_OFF_AFFINITY;
        }
        MagicEligibilityResolver.Result eval = MagicEligibilityResolver.evaluate(data, node);
        return switch (eval.failure()) {
            case NONE -> State.AVAILABLE;
            case MISSING_PREREQ, LOCKED -> State.LOCKED_PREREQ;
            case NOT_ENOUGH_POINTS -> State.LOCKED_COST;
            case ALREADY_UNLOCKED -> State.UNLOCKED;
            case NO_RACE -> State.LOCKED_PREREQ;
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.magic.MagicTreeViewModelTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicTreeViewModel.java src/test/java/tong/statmod/magic/MagicTreeViewModelTest.java
git commit -m "Add MagicTreeViewModel for per-node UI state"
```

---

### Task 18: Add client-side `ClientMagicCache`

**Files:**
- Create: `src/main/java/tong/statmod/client/cache/ClientMagicCache.java`
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`

The cache is a singleton holding the last-synced `MagicTreeSyncPayload` values. Bind it from the `MagicTreeSyncPayload` client handler registered in Task 15.

- [ ] **Step 1: Implement**

```java
package tong.statmod.client.cache;

import tong.statmod.network.MagicTreeSyncPayload;
import java.util.List;

public final class ClientMagicCache {
    private static int arcanePoints;
    private static int[] schoolPoints = new int[0];
    private static int[] schoolMastery = new int[0];
    private static List<String> nodes = List.of();
    private static List<String> learnedSpells = List.of();
    private static String race;
    private static String chosenStartBranch;

    private ClientMagicCache() {}

    public static void apply(MagicTreeSyncPayload payload) {
        arcanePoints = payload.arcanePoints();
        schoolPoints = payload.schoolPoints().clone();
        schoolMastery = payload.schoolMastery().clone();
        nodes = List.copyOf(payload.nodes());
        learnedSpells = List.copyOf(payload.learnedSpells());
        race = payload.magicRace();
        chosenStartBranch = payload.chosenStartBranch();
    }

    public static int arcanePoints() { return arcanePoints; }
    public static int[] schoolPoints() { return schoolPoints.clone(); }
    public static List<String> nodes() { return nodes; }
    public static List<String> learnedSpells() { return learnedSpells; }
    public static String race() { return race; }
    public static String chosenStartBranch() { return chosenStartBranch; }
}
```

- [ ] **Step 2: Wire the handler in `NetworkHandler.java`**

Replace the empty client handler from Task 15 with:

```java
        registrar.playToClient(MagicTreeSyncPayload.TYPE, MagicTreeSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        tong.statmod.client.cache.ClientMagicCache.apply(payload)));
```

- [ ] **Step 3: Compile**

Run: `./gradlew compileJava`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/client/cache/ClientMagicCache.java src/main/java/tong/statmod/network/NetworkHandler.java
git commit -m "Cache synced magic state on the client"
```

---

### Task 19: Add Puffish category resources for the magic tree

**Files:**
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/category.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/definitions.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/skills.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common/connections.json`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_fire/<same four files>`
- Create: `src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_locked/<same four files>`

Use the same JSON shape already in use by the existing perk categories (read the file `src/main/resources/data/puffish_skills/puffish_skills/categories/<existing>/category.json` to mirror the schema exactly). One skill per `MagicNode`. Positions: lay out tiers vertically (common at top, branch openers below, branch tiers in columns, signature spells fanned out per tier). For the `statmod_magic_locked` category, place all 8 anchors side by side at the same y.

Required category fields (matching existing pattern):
- `category.json`: id (matches category dir), translation key, exclusive=false
- `definitions.json`: one entry per icon + tooltip
- `skills.json`: position (x,y) + which definition + cost (set Puffish-side cost to 0 because the canonical cost lives in `MagicTreeCatalog`; Puffish acts as a mirror)
- `connections.json`: edges based on `MagicNode.prerequisites()` (skip `__never__`)

- [ ] **Step 1: Read the existing perk category JSON to match the schema**

Run: `ls src/main/resources/data/puffish_skills/puffish_skills/categories/` and read one full set.

- [ ] **Step 2: Generate the three new category folders matching the schema**

(See generator pattern in `PuffishFamilyTreeExporter.java` for the exact field names. Implementer must transcribe one node group then validate at runtime — do not paste a stale schema.)

- [ ] **Step 3: Wire the categories in `PuffishSkillsCompat`**

In the existing `registerEvent` flow, ensure the `categoryId` discriminator includes the three new categories so they route to the magic-tree handler (see Task 20). No code changes needed if the existing reflection layer accepts any category id.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_common src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_fire src/main/resources/data/puffish_skills/puffish_skills/categories/statmod_magic_locked
git commit -m "Add Puffish resources for magic tree (common, fire, locked)"
```

---

### Task 20: Bridge Puffish unlock callbacks to `MagicTreeProgressionService`

**Files:**
- Create: `src/main/java/tong/statmod/integration/puffish/PuffishMagicCategoryIds.java`
- Test: `src/test/java/tong/statmod/integration/puffish/PuffishMagicCategoryIdsTest.java`
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishSkillsCompat.java`

`PuffishMagicCategoryIds` resolves a `(categoryId, skillId)` pair to a `MagicNode.id`. Puffish skill ids are flat strings, so the convention is: `skillId = MagicNode.id.replace('/', '.')`. The category id identifies the group.

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PuffishMagicCategoryIdsTest {
    @Test
    void node_id_round_trips_through_skill_id() {
        String nodeId = "fire/signature/firebolt";
        String skillId = PuffishMagicCategoryIds.toSkillId(nodeId);
        assertEquals("fire.signature.firebolt", skillId);
        assertEquals(nodeId, PuffishMagicCategoryIds.fromSkillId(skillId));
    }

    @Test
    void common_skill_in_common_category() {
        assertEquals("statmod:magic_common",
                PuffishMagicCategoryIds.categoryFor("common/foundation/arcane_focus"));
        assertEquals("statmod:magic_fire",
                PuffishMagicCategoryIds.categoryFor("fire/opener/ignition"));
        assertEquals("statmod:magic_locked",
                PuffishMagicCategoryIds.categoryFor("blood/locked/anchor"));
    }

    @Test
    void unknown_skill_returns_null() {
        assertNull(PuffishMagicCategoryIds.fromSkillId(null));
        assertNull(PuffishMagicCategoryIds.fromSkillId(""));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests tong.statmod.integration.puffish.PuffishMagicCategoryIdsTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

```java
package tong.statmod.integration.puffish;

import tong.statmod.magic.MagicBranch;

public final class PuffishMagicCategoryIds {
    public static final String COMMON_CATEGORY = "statmod:magic_common";
    public static final String FIRE_CATEGORY = "statmod:magic_fire";
    public static final String LOCKED_CATEGORY = "statmod:magic_locked";

    private PuffishMagicCategoryIds() {}

    public static String toSkillId(String nodeId) {
        return nodeId == null ? null : nodeId.replace('/', '.');
    }

    public static String fromSkillId(String skillId) {
        return skillId == null || skillId.isEmpty() ? null : skillId.replace('.', '/');
    }

    public static String categoryFor(String nodeId) {
        if (nodeId == null) return null;
        if (nodeId.startsWith("common/")) return COMMON_CATEGORY;
        if (nodeId.startsWith("fire/")) return FIRE_CATEGORY;
        for (MagicBranch b : MagicBranch.values()) {
            if (b == MagicBranch.COMMON || b == MagicBranch.FIRE) continue;
            if (nodeId.startsWith(b.id + "/")) return LOCKED_CATEGORY;
        }
        return null;
    }
}
```

- [ ] **Step 4: Hook into `PuffishSkillsCompat.java`**

In the existing `registerSkillUnlockEvent` handler, before delegating to `PerkManager.unlock`, add:

```java
                        if (categoryId.startsWith("statmod:magic_")) {
                            String nodeId = PuffishMagicCategoryIds.fromSkillId(skillId);
                            tong.statmod.magic.MagicNode node = tong.statmod.magic.MagicTreeCatalog.byId(nodeId);
                            if (node == null) return;
                            var result = tong.statmod.magic.MagicTreeProgressionService.tryUnlock(data, node);
                            if (result.success()) tong.statmod.sound.SoundHelper.playPerkUnlock(player);
                            SyncHelper.syncMagic(player);
                            return;
                        }
```

This branches Puffish unlocks: magic categories go through the magic service, perk categories continue through the existing `PerkManager` path.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests tong.statmod.integration.puffish.PuffishMagicCategoryIdsTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishMagicCategoryIds.java src/test/java/tong/statmod/integration/puffish/PuffishMagicCategoryIdsTest.java src/main/java/tong/statmod/integration/puffish/PuffishSkillsCompat.java
git commit -m "Route Puffish magic-tree unlock callbacks to MagicTreeProgressionService"
```

---

### Task 21: Fill in `PuffishMagicTreeBuilder.applyMirror`

**Files:**
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java`

Mirror the `MagicTreeViewModel.State` per node into Puffish's mirror state — unlocked nodes get the Puffish "unlocked" flag, `AVAILABLE` becomes spendable, everything else stays Puffish-locked. Use the existing `PuffishReflectionGateway` to invoke `unlockSkill` / `lockSkill` on the mirror.

- [ ] **Step 1: Implement**

```java
package tong.statmod.integration.puffish;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.magic.MagicTreeViewModel;
import tong.statmod.storage.PlayerStatData;

public final class PuffishMagicTreeBuilder {
    private PuffishMagicTreeBuilder() {}

    public static void applyMirror(ServerPlayer player, PlayerStatData data) {
        if (!ModList.get().isLoaded("puffish_skills") || !PuffishSkillsCompat.isLoaded()) return;
        PuffishReflectionGateway gateway = new PuffishReflectionGateway(player);
        MagicTreeViewModel view = MagicTreeViewModel.build(data);
        for (MagicNode node : MagicTreeCatalog.all()) {
            String category = PuffishMagicCategoryIds.categoryFor(node.id());
            String skill = PuffishMagicCategoryIds.toSkillId(node.id());
            if (category == null || skill == null) continue;
            boolean shouldBeUnlocked = view.stateOf(node.id()) == MagicTreeViewModel.State.UNLOCKED;
            try {
                if (shouldBeUnlocked) gateway.unlockSkill(category, skill);
                else gateway.lockSkill(category, skill);
            } catch (Throwable t) {
                STATMod.LOGGER.debug("Puffish mirror error on {}: {}", node.id(), t.getMessage());
            }
        }
    }
}
```

Confirm `PuffishReflectionGateway` already exposes `unlockSkill(String, String)` and `lockSkill(String, String)` — if it does not, extend it with two reflection calls to `SkillsAPI.unlockSkill`/`lockSkill` patterned on the existing event proxies. (Read the existing `PuffishReflectionGateway.java` first.)

- [ ] **Step 2: Build**

Run: `./gradlew compileJava`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java src/main/java/tong/statmod/integration/puffish/PuffishReflectionGateway.java
git commit -m "Project magic tree view model into Puffish mirror state"
```

---

### Task 22: Sync magic state on player join

**Files:**
- Modify: `src/main/java/tong/statmod/mixin/PlayerListMixin.java` (or whichever mixin currently calls `SyncHelper.syncPerks` on join)

- [ ] **Step 1: Add the call**

Inside the existing post-login hook that already calls `SyncHelper.syncPerks(player)`, add:

```java
        tong.statmod.network.SyncHelper.syncMagic(player);
```

- [ ] **Step 2: Smoke-build**

Run: `./gradlew compileJava`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/mixin/PlayerListMixin.java
git commit -m "Sync magic state on player join"
```

---

### Task 23: Add `/magic` debug command

**Files:**
- Modify: `src/main/java/tong/statmod/stats/StatCommands.java`

The command exposes:
- `/magic info` — print arcane / school points / mastery / race / start branch
- `/magic grantarcane <n>` — for testing, op-only
- `/magic pickrace <human|elf|dwarf|beast> <fire|water|air|earth>` — emulate the onboarding flow

Required for manual phase 1 verification per spec §10 (onboarding) and §14 (test strategy).

- [ ] **Step 1: Implement**

Add to `StatCommands.register`:

```java
        dispatcher.register(net.minecraft.commands.Commands.literal("magic")
                .requires(s -> s.hasPermission(0))
                .then(net.minecraft.commands.Commands.literal("info")
                        .executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                            tong.statmod.storage.PlayerStatData d = p.getData(tong.statmod.storage.ModAttachments.STATS);
                            p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "Arcane=" + d.getArcanePoints()
                                    + " Race=" + d.getMagicRace()
                                    + " Start=" + d.getChosenStartBranch()
                                    + " Nodes=" + d.getMagicNodes().length));
                            return 1;
                        }))
                .then(net.minecraft.commands.Commands.literal("grantarcane")
                        .requires(s -> s.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "n");
                                    p.getData(tong.statmod.storage.ModAttachments.STATS).addArcanePoints(n);
                                    tong.statmod.network.SyncHelper.syncMagic(p);
                                    return 1;
                                })))
                .then(net.minecraft.commands.Commands.literal("pickrace")
                        .then(net.minecraft.commands.Commands.argument("race", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .then(net.minecraft.commands.Commands.argument("branch", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .executes(ctx -> {
                                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String rn = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "race").toUpperCase();
                                            String bn = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "branch").toLowerCase();
                                            try {
                                                tong.statmod.magic.MagicRace race = tong.statmod.magic.MagicRace.valueOf(rn);
                                                tong.statmod.magic.MagicBranch branch = tong.statmod.magic.MagicBranch.byId(bn);
                                                if (branch == null || !race.hasAffinity(branch)) return 0;
                                                tong.statmod.storage.PlayerStatData d = p.getData(tong.statmod.storage.ModAttachments.STATS);
                                                d.setMagicRace(race);
                                                d.setChosenStartBranch(branch);
                                                tong.statmod.network.SyncHelper.syncMagic(p);
                                                return 1;
                                            } catch (IllegalArgumentException e) { return 0; }
                                        })))));
```

- [ ] **Step 2: Build**

Run: `./gradlew compileJava`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/stats/StatCommands.java
git commit -m "Add /magic info, /magic grantarcane and /magic pickrace commands"
```

---

### Task 24: End-to-end runtime smoke

**Files:** none (manual verification)

- [ ] **Step 1: Run client**

Run: `./gradlew runClient`

Expected: client launches without errors; logs contain `Iron's Spellbooks integration loaded` and `Puffish Skills integration loaded`.

- [ ] **Step 2: Open the perk/magic tree in-game**

Press the existing perk-tree keybind, switch to the `statmod_magic_common` category.

Expected: see the 4 common foundation nodes, all locked; the `statmod_magic_fire` and `statmod_magic_locked` categories render with their nodes; locked anchors visible but unspendable.

- [ ] **Step 3: Smoke onboarding**

Run in-game: `/magic pickrace DWARF fire` then `/magic grantarcane 10`.

Expected: `/magic info` reports `Race=DWARF Start=FIRE Arcane=10`.

- [ ] **Step 4: Smoke unlock + spell learn**

Unlock `common/foundation/arcane_focus` and `common/foundation/mana_well` via the tree, then unlock `fire/opener/ignition`, then `fire/tier/ember_path`, then `fire/signature/firebolt`.

Expected: `/magic info` shows 5 nodes unlocked. Attempt to cast `firebolt` (must be inscribed in a spellbook obtained via creative or `/give`). Expected: cast succeeds.

- [ ] **Step 5: Smoke locked-spell rejection**

`/give` a spellbook with `irons_spellbooks:fireball` inscribed.

Expected: pre-cast event cancels the cast and shows the `statmod.magic.locked_spell` actionbar.

- [ ] **Step 6: Smoke locked branch**

Open the `statmod_magic_locked` category, click on `blood/locked/anchor`.

Expected: Puffish does not unlock it; logs do not show progression mutation.

- [ ] **Step 7: Smoke cast progression**

Cast `firebolt` 5 times at full mana, hit a mob each time.

Expected: `/magic info` shows mastery progress increasing; once it crosses 100, `Fire` school points increment by 1 and mastery resets.

- [ ] **Step 8: Commit notes (no code change)**

If issues found, file follow-up commits or open separate fix tasks. If everything passes:

```bash
git commit --allow-empty -m "Phase 1 magic tree smoke-test passed"
```

---

### Task 25: Add localization keys

**Files:**
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json` (if it exists, mirror)

- [ ] **Step 1: Add keys**

```json
  "statmod.magic.locked_spell": "You have not yet learned this spell.",
  "statmod.magic.category.common": "Arcane Foundations",
  "statmod.magic.category.fire": "Fire",
  "statmod.magic.category.locked": "Other Schools",
  "statmod.magic.race.human": "Human",
  "statmod.magic.race.elf": "Elf",
  "statmod.magic.race.dwarf": "Dwarf",
  "statmod.magic.race.beast": "Beast"
```

Mirror in French if `fr_fr.json` exists.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json
git commit -m "Add localization keys for magic tree"
```

---

### Task 26: Final test sweep

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`
Expected: PASS.

- [ ] **Step 2: Run a server-only smoke**

Run: `./gradlew runServer`
Expected: server boots; no `IronSpellsCompat` warnings; `/magic info` works from a dummy account or RCON.

- [ ] **Step 3: Final commit**

```bash
git commit --allow-empty -m "Phase 1 unified magic tree vertical slice complete"
```

---

## Out of Scope (Phase 2+ Reminders)

Per spec §11.3, the following are deliberately not in this plan and must not be tackled here:

- Water, Air, Earth full branch content (only locked placeholders)
- Holy quest content
- Blood boss content
- Ender, Evocation, Eldritch full mechanics
- Iron's addon spell integration (`gametechbcs_spellbooks`, `wind_spellbooks`, `spells_gone_wrong`)
- Deep Tensura nodes inside Iron's branches
- Per-spell signature nodes for every Iron's spell — only Fire signatures are catalogued in Phase 1
- Race purity penalty for Beast (the flag is wired but no gameplay effect yet)
- The strict first-affinity enforcement (Phase 1 keeps Fire open to all races per spec §11.2)

---

## Self-Review Notes

- Spec §3.1 (source of truth) — ✓ STAT Mod owns nodes/points; Iron's owns mana (we never set mana directly)
- Spec §3.2 (shared identity) — ✓ same `PlayerStatData` carries everything; no second magic data attachment
- Spec §3.3 (mana policy) — ✓ no mana replacement; only event-driven gating
- Spec §3.4 (learning policy) — ✓ `IronSpellEventBridge.shouldCancelPreCast`
- Spec §4.1–4.4 (topology) — ✓ catalog encodes common trunk, four elemental trunks (3 visible-locked, 1 active), five late-game branches (locked anchors)
- Spec §5 (races) — ✓ `MagicRace` enum + affinity discount
- Spec §6.1–6.4 (unlock model) — ✓ tree structure rules in catalog; tier-clamped spell levels via `ModifySpellLevelEvent`
- Spec §7 (currency model) — ✓ `ARCANE` for common trunk, `SCHOOL` for branch progression
- Spec §7.3 (school points from mastery) — ✓ `SchoolProgressTracker`
- Spec §8 (cast progression + anti-abuse) — ✓ `CastRewardPolicy`
- Spec §9 (equipment) — ✓ `InscribeSpellEvent` gated
- Spec §10 (onboarding) — partial: `/magic pickrace` works as proxy; full GUI onboarding screen is Phase 2
- Spec §11 (slice scope) — ✓
- Spec §12 (file boundaries) — ✓ each service in its own file
- Spec §13 (fail safe) — ✓ optional-mod gates; locked anchors never unlock; refuses on missing data
- Spec §14 (tests) — ✓ all listed test categories covered

Known acceptable gaps:
- Onboarding UI is command-driven for Phase 1; a proper screen is Phase 2 work.
- `IronSpellEventBridge.guessImpact` is heuristic (Iron's API does not surface a clean post-hoc impact flag). Phase 2 can replace it with `SpellDamageEvent` / `SpellHealEvent` listeners that decorate a per-cast state.

---

Plan complete and saved to `docs/superpowers/plans/2026-06-21-irons-spellbooks-unified-magic-tree.md`. Two execution options:

1. **Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** — execute tasks in this session using `executing-plans`, batch execution with checkpoints

Which approach?
