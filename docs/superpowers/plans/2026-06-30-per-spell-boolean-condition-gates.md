# Per-Spell Boolean Condition Gates — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the universal D1/D2/D3 stat-gate tables with per-spell boolean expression trees (AND/OR/leaf conditions), removing `SpellRole`, `SpellRoleInference`, and `MagicNodeStatRequirements`.

**Architecture:** An expression-tree `Condition` interface (And, Or, StatCondition, RaceCondition, HasSpellCondition, HasNodeCondition, BranchTierCondition, GlobalLevelCondition) replaces the rigid 3-gate system. Each `MagicNode` carries its own `Condition` object. A migration helper generates equivalent conditions from the old tables, then spell-by-spell customization follows.

**Tech Stack:** NeoForge 1.21.1, Java 21, JUnit 5

---

### Task 1: Create Condition.java (interface + records + factory helpers)

**Files:**
- Create: `src/main/java/tong/statmod/magic/Condition.java`

- [ ] **Step 1: Write Condition.java with complete type hierarchy**

```java
package tong.statmod.magic;

import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.List;
import java.util.Objects;

public sealed interface Condition {

    boolean evaluate(PlayerStatData data, ConditionContext ctx);

    // ──────────────────────────────────────────────
    // Operators
    // ──────────────────────────────────────────────

    record And(List<Condition> children) implements Condition {
        public And {
            children = List.copyOf(children);
            if (children.isEmpty()) throw new IllegalArgumentException("And must have at least one child");
        }
        public static And of(Condition... cs) { return new And(List.of(cs)); }

        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            for (Condition c : children) {
                if (!c.evaluate(data, ctx)) return false;
            }
            return true;
        }
    }

    record Or(List<Condition> children) implements Condition {
        public Or {
            children = List.copyOf(children);
            if (children.isEmpty()) throw new IllegalArgumentException("Or must have at least one child");
        }
        public static Or of(Condition... cs) { return new Or(List.of(cs)); }

        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            for (Condition c : children) {
                if (c.evaluate(data, ctx)) return true;
            }
            return false;
        }
    }

    // ──────────────────────────────────────────────
    // Leaf conditions
    // ──────────────────────────────────────────────

    record StatCondition(StatType stat, int minLevel) implements Condition {
        public StatCondition {
            Objects.requireNonNull(stat);
            if (minLevel < 0) throw new IllegalArgumentException("minLevel must be >= 0");
        }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.getLevel(stat.index) >= minLevel;
        }
    }

    record RaceCondition(MagicRace race) implements Condition {
        public RaceCondition { Objects.requireNonNull(race); }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.getMagicRace() == race;
        }
    }

    record HasSpellCondition(String spellId) implements Condition {
        public HasSpellCondition { Objects.requireNonNull(spellId); }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.hasLearnedSpell(spellId);
        }
    }

    record HasNodeCondition(String nodeId) implements Condition {
        public HasNodeCondition { Objects.requireNonNull(nodeId); }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.hasMagicNode(nodeId);
        }
    }

    record BranchTierCondition(MagicBranch branch, MagicTier minTier) implements Condition {
        public BranchTierCondition {
            Objects.requireNonNull(branch);
            Objects.requireNonNull(minTier);
        }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return ctx.highestTierInBranch(data, branch) != null
                    && ctx.highestTierInBranch(data, branch).compareTo(minTier) >= 0;
        }
    }

    record GlobalLevelCondition(int minLevel) implements Condition {
        public GlobalLevelCondition {
            if (minLevel < 0) throw new IllegalArgumentException("minLevel must be >= 0");
        }
        @Override
        public boolean evaluate(PlayerStatData data, ConditionContext ctx) {
            return data.getLevel(StatType.GLOBAL.index) >= minLevel;
        }
    }
}
```

- [ ] **Step 2: Create ConditionContext.java**

```java
package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;
import java.util.List;

public interface ConditionContext {
    MagicTier highestTierInBranch(PlayerStatData data, MagicBranch branch);

    /** Default implementation using MagicTreeCatalog. */
    static ConditionContext defaultContext() {
        return (data, branch) -> {
            MagicTier best = null;
            for (MagicNode node : MagicTreeCatalog.byBranch(branch)) {
                if (data.hasMagicNode(node.id())) {
                    if (best == null || node.tier().compareTo(best) > 0) {
                        best = node.tier();
                    }
                }
            }
            return best;
        };
    }
}
```

- [ ] **Step 3: Write edge-case unit tests**

**File:** `src/test/java/tong/statmod/magic/ConditionTest.java`

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.*;

class ConditionTest {

    private final ConditionContext ctx = ConditionContext.defaultContext();
    private final PlayerStatData data = new PlayerStatData();

    @Test void statCondition_passes_when_level_meets_min() {
        data.setLevel(StatType.ARCANE_POWER.index, 5);
        assertTrue(new Condition.StatCondition(StatType.ARCANE_POWER, 5).evaluate(data, ctx));
    }

    @Test void statCondition_fails_when_level_below_min() {
        data.setLevel(StatType.ARCANE_POWER.index, 3);
        assertFalse(new Condition.StatCondition(StatType.ARCANE_POWER, 5).evaluate(data, ctx));
    }

    @Test void raceCondition_passes_when_matches() {
        data.setMagicRace(MagicRace.ELF);
        assertTrue(new Condition.RaceCondition(MagicRace.ELF).evaluate(data, ctx));
    }

    @Test void raceCondition_fails_when_different() {
        data.setMagicRace(MagicRace.HUMAN);
        assertFalse(new Condition.RaceCondition(MagicRace.ELF).evaluate(data, ctx));
    }

    @Test void raceCondition_fails_when_null() {
        assertFalse(new Condition.RaceCondition(MagicRace.ELF).evaluate(data, ctx));
    }

    @Test void hasSpellCondition_passes_when_learned() {
        data.learnSpell("irons_spellbooks:firebolt");
        assertTrue(new Condition.HasSpellCondition("irons_spellbooks:firebolt").evaluate(data, ctx));
    }

    @Test void hasSpellCondition_fails_when_not_learned() {
        assertFalse(new Condition.HasSpellCondition("irons_spellbooks:firebolt").evaluate(data, ctx));
    }

    @Test void hasNodeCondition_passes_when_unlocked() {
        data.addMagicNode("fire/opener/ignition");
        assertTrue(new Condition.HasNodeCondition("fire/opener/ignition").evaluate(data, ctx));
    }

    @Test void globalLevelCondition_passes_when_level_high_enough() {
        data.setLevel(StatType.GLOBAL.index, 10);
        assertTrue(new Condition.GlobalLevelCondition(10).evaluate(data, ctx));
    }

    @Test void and_all_true_returns_true() {
        data.setLevel(StatType.ARCANE_POWER.index, 5);
        data.setLevel(StatType.ERUDITION.index, 3);
        assertTrue(Condition.And.of(
                new Condition.StatCondition(StatType.ARCANE_POWER, 5),
                new Condition.StatCondition(StatType.ERUDITION, 3)
        ).evaluate(data, ctx));
    }

    @Test void and_one_false_returns_false() {
        data.setLevel(StatType.ARCANE_POWER.index, 5);
        data.setLevel(StatType.ERUDITION.index, 2);
        assertFalse(Condition.And.of(
                new Condition.StatCondition(StatType.ARCANE_POWER, 5),
                new Condition.StatCondition(StatType.ERUDITION, 3)
        ).evaluate(data, ctx));
    }

    @Test void or_any_true_returns_true() {
        data.setMagicRace(MagicRace.ELF);
        assertTrue(Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        ).evaluate(data, ctx));
    }

    @Test void or_all_false_returns_false() {
        assertFalse(Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        ).evaluate(data, ctx));
    }

    @Test void nested_and_or_works() {
        // (arcane >= 5 AND erudition >= 3) OR (race = ELF AND fire_affinity >= 5)
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 2);
        data.setMagicRace(MagicRace.ELF);
        data.setLevel(StatType.FIRE_AFFINITY.index, 5);
        assertTrue(Condition.Or.of(
                Condition.And.of(
                        new Condition.StatCondition(StatType.ARCANE_POWER, 5),
                        new Condition.StatCondition(StatType.ERUDITION, 3)
                ),
                Condition.And.of(
                        new Condition.RaceCondition(MagicRace.ELF),
                        new Condition.StatCondition(StatType.FIRE_AFFINITY, 5)
                )
        ).evaluate(data, ctx));
    }

    @Test void and_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Condition.And(List.of()));
    }

    @Test void or_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Condition.Or(List.of()));
    }

    @Test void stat_minLevel_negative_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Condition.StatCondition(StatType.ARCANE_POWER, -1));
    }
}
```

- [ ] **Step 4: Run tests to verify they fail (no Condition.java yet)**

Run: `./gradlew test --tests "tong.statmod.magic.ConditionTest"` — expected: compilation error

- [ ] **Step 5: Create Condition.java and ConditionContext.java with the code from Step 1 + 2**

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew test --tests "tong.statmod.magic.ConditionTest"` — expected: BUILD SUCCESSFUL, all 13+ tests pass

- [ ] **Step 7: Commit**

```bash
git add src/main/java/tong/statmod/magic/Condition.java src/main/java/tong/statmod/magic/ConditionContext.java src/test/java/tong/statmod/magic/ConditionTest.java
git commit -m "feat(magic): add Condition expression tree (AND/OR/leaf types)"
```

---

### Task 2: Create ConditionEvaluator (describe missing conditions)

**Files:**
- Create: `src/main/java/tong/statmod/magic/ConditionEvaluator.java`

- [ ] **Step 1: Write ConditionEvaluator.java**

```java
package tong.statmod.magic;

import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

public final class ConditionEvaluator {

    public static boolean evaluate(MagicNode node, PlayerStatData data) {
        if (node.condition() == null) return true;
        return node.condition().evaluate(data, ConditionContext.defaultContext());
    }

    /**
     * Returns human-readable descriptions of the conditions that the player
     * does NOT meet. Only the immediate failing leaves are collected (not the
     * entire AND/OR structure). For AND nodes, all failing children are listed.
     * For OR nodes, if at least one child passes, nothing is listed; otherwise
     * all children are listed.
     */
    public static List<String> describeMissing(MagicNode node, PlayerStatData data) {
        if (node.condition() == null) return List.of();
        return collectMissing(node.condition(), data, ConditionContext.defaultContext());
    }

    private static List<String> collectMissing(Condition cond, PlayerStatData data, ConditionContext ctx) {
        return switch (cond) {
            case Condition.And and -> {
                List<String> result = new ArrayList<>();
                for (Condition c : and.children()) {
                    result.addAll(collectMissing(c, data, ctx));
                }
                yield result;
            }
            case Condition.Or or -> {
                // If any child passes, OR is satisfied — nothing missing
                for (Condition c : or.children()) {
                    if (c.evaluate(data, ctx)) yield List.of();
                }
                // All children fail — collect all
                List<String> result = new ArrayList<>();
                for (Condition c : or.children()) {
                    result.addAll(collectMissing(c, data, ctx));
                }
                yield result;
            }
            case Condition.StatCondition sc -> {
                if (sc.evaluate(data, ctx)) yield List.of();
                yield List.of(sc.stat().displayName + " ≥ " + sc.minLevel());
            }
            case Condition.RaceCondition rc -> {
                if (rc.evaluate(data, ctx)) yield List.of();
                yield List.of("Race: " + rc.race().name());
            }
            case Condition.HasSpellCondition hsc -> {
                if (hsc.evaluate(data, ctx)) yield List.of();
                yield List.of("Spell: " + hsc.spellId());
            }
            case Condition.HasNodeCondition hnc -> {
                if (hnc.evaluate(data, ctx)) yield List.of();
                yield List.of("Node: " + hnc.nodeId());
            }
            case Condition.BranchTierCondition btc -> {
                if (btc.evaluate(data, ctx)) yield List.of();
                yield List.of(btc.branch().id + " tier ≥ " + btc.minTier());
            }
            case Condition.GlobalLevelCondition glc -> {
                if (glc.evaluate(data, ctx)) yield List.of();
                yield List.of("Global Level ≥ " + glc.minLevel());
            }
        };
    }

    private ConditionEvaluator() {}
}
```

- [ ] **Step 2: Write ConditionEvaluator test**

**File:** `src/test/java/tong/statmod/magic/ConditionEvaluatorTest.java`

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEvaluatorTest {

    @Test void nullCondition_alwaysPasses() {
        MagicNode node = new MagicNode("common/foundation/arcane_focus",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of(), java.util.Set.of(), null);
        assertTrue(ConditionEvaluator.evaluate(node, new PlayerStatData()));
    }

    @Test void statCondition_failing_showsInDescribeMissing() {
        Condition cond = new Condition.StatCondition(StatType.ARCANE_POWER, 5);
        MagicNode node = node(cond);
        List<String> missing = ConditionEvaluator.describeMissing(node, new PlayerStatData());
        assertTrue(missing.stream().anyMatch(m -> m.contains("Arcane Power") && m.contains("5")));
    }

    @Test void statCondition_passing_returnsEmptyMissing() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(StatType.ARCANE_POWER.index, 10);
        Condition cond = new Condition.StatCondition(StatType.ARCANE_POWER, 5);
        MagicNode node = node(cond);
        assertTrue(ConditionEvaluator.describeMissing(node, data).isEmpty());
    }

    @Test void or_withOnePassing_returnsEmptyMissing() {
        PlayerStatData data = new PlayerStatData();
        data.setMagicRace(MagicRace.ELF);
        Condition cond = Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        );
        MagicNode node = node(cond);
        assertTrue(ConditionEvaluator.describeMissing(node, data).isEmpty());
    }

    @Test void or_withAllFailing_listsBothBranches() {
        Condition cond = Condition.Or.of(
                new Condition.RaceCondition(MagicRace.ELF),
                new Condition.StatCondition(StatType.ERUDITION, 80)
        );
        MagicNode node = node(cond);
        List<String> missing = ConditionEvaluator.describeMissing(node, new PlayerStatData());
        assertEquals(2, missing.size(), "Both branches should show since all fail");
    }

    private static MagicNode node(Condition cond) {
        return new MagicNode("fire/signature/firebolt",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/ember_path"),
                java.util.Set.of("irons_spellbooks:firebolt"), cond);
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests "tong.statmod.magic.ConditionEvaluatorTest"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/magic/ConditionEvaluator.java src/test/java/tong/statmod/magic/ConditionEvaluatorTest.java
git commit -m "feat(magic): add ConditionEvaluator (evaluate + describeMissing)"
```

---

### Task 3: Modify MagicNode (add Condition, remove SpellRole)

**Files:**
- Modify: `src/main/java/tong/statmod/magic/MagicNode.java`
- Modify: `src/test/java/tong/statmod/magic/MagicNodeTest.java`

- [ ] **Step 1: Update MagicNode.java**

Replace the complete file:

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
        Set<String> learnedSpells,
        Condition condition
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
        // condition may be null — means unconditional (prerequisites + cost still apply)
    }
}
```

- [ ] **Step 2: Update MagicNodeTest.java**

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MagicNodeTest {
    @Test
    void node_record_holds_all_fields() {
        Condition cond = new Condition.StatCondition(tong.statmod.stats.StatType.ARCANE_POWER, 5);
        MagicNode n = new MagicNode(
                "fire/opener/ignition",
                MagicBranch.FIRE,
                MagicNodeKind.BRANCH_OPENER,
                MagicTier.T1,
                MagicCurrency.ARCANE,
                3,
                List.of("common/foundation/arcane_focus"),
                Set.of("irons_spellbooks:firebolt"),
                cond);
        assertEquals("fire/opener/ignition", n.id());
        assertEquals(MagicBranch.FIRE, n.branch());
        assertEquals(3, n.cost());
        assertEquals(Set.of("irons_spellbooks:firebolt"), n.learnedSpells());
        assertNotNull(n.condition());
    }

    @Test
    void node_condition_can_be_null() {
        MagicNode n = new MagicNode(
                "fire/opener/ignition",
                MagicBranch.FIRE,
                MagicNodeKind.BRANCH_OPENER,
                MagicTier.T1,
                MagicCurrency.ARCANE,
                3,
                List.of("common/foundation/arcane_focus"),
                Set.of("irons_spellbooks:firebolt"),
                null);
        assertNull(n.condition());
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
                Set.of(),
                null));
    }
}
```

- [ ] **Step 3: Fix MagicTreeProgressionServiceTest.java (SpellRole → null condition)**

Replace all `SpellRole.ELEMENTAL_DAMAGE_FIRE` constructor last-arg with `null`:

Lines 78-87:
```java
        MagicNode node = new MagicNode(
                "fire/signature/test_tensura_fire",
                MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL,
                MagicTier.T1,
                MagicCurrency.SCHOOL,
                1,
                java.util.List.of("fire/tier/ember_path"),
                java.util.Set.of("tensura:fire_bolt"),
                null); // SpellRole removed; condition null = auto-migrate in catalog
```

Lines 104-113:
```java
        MagicNode node = new MagicNode(
                "fire/signature/test_failed_tensura_fire",
                MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL,
                MagicTier.T1,
                MagicCurrency.SCHOOL,
                1,
                java.util.List.of("fire/tier/ember_path"),
                java.util.Set.of("tensura:fire_bolt"),
                null); // SpellRole removed
```

- [ ] **Step 4: Run tests**

Run: `./gradlew test --tests "tong.statmod.magic.MagicNodeTest" --tests "tong.statmod.magic.MagicTreeProgressionServiceTest"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicNode.java src/test/java/tong/statmod/magic/MagicNodeTest.java
git commit -m "refactor(magic): replace SpellRole role with Condition condition in MagicNode"
```

---

### Task 4: Write migration helper (old D1/D2/D3 → Condition tree)

**Files:**
- Create: `src/main/java/tong/statmod/magic/MagicNodeMigration.java`

- [ ] **Step 1: Write the helper that replicates old D1/D2/D3 as Condition trees**

```java
package tong.statmod.magic;

import tong.statmod.stats.StatType;

/**
 * Temporary migration helper that builds a Condition tree equivalent to the
 * old D1/D2/D3 universal gate tables. Used to auto-migrate all 249 existing
 * catalog nodes so that behaviour is preserved. Once a node's condition is
 * customised, this helper is no longer used for that node.
 */
public final class MagicNodeMigration {

    public static Condition defaultCondition(MagicNode node) {
        int arcane = universalArcane(node.kind(), node.tier());
        int erudition = universalErudition(node.kind(), node.tier());
        Condition tertiary = tertiaryCondition(node);

        // Build: arcane >= X AND erudition >= Y AND (tertiary if present)
        if (tertiary == null) {
            return Condition.And.of(
                    new Condition.StatCondition(StatType.ARCANE_POWER, arcane),
                    new Condition.StatCondition(StatType.ERUDITION, erudition)
            );
        }
        return Condition.And.of(
                new Condition.StatCondition(StatType.ARCANE_POWER, arcane),
                new Condition.StatCondition(StatType.ERUDITION, erudition),
                tertiary
        );
    }

    private static int universalArcane(MagicNodeKind kind, MagicTier tier) {
        return switch (kind) {
            case TRUNK_FOUNDATION -> switch (tier) {
                case T1 -> 1; case T2 -> 2; case T3, T4 -> 3;
            };
            case BRANCH_OPENER -> 2;
            case BRANCH_TIER, SIGNATURE_SPELL -> switch (tier) {
                case T1 -> 2; case T2 -> 4; case T3 -> 6; case T4 -> 10;
            };
            case LATEGAME_GATE -> 10;
        };
    }

    private static int universalErudition(MagicNodeKind kind, MagicTier tier) {
        return switch (kind) {
            case TRUNK_FOUNDATION -> switch (tier) {
                case T1 -> 1; case T2 -> 2; case T3, T4 -> 3;
            };
            case BRANCH_OPENER -> 1;
            case BRANCH_TIER, SIGNATURE_SPELL -> switch (tier) {
                case T1 -> 2; case T2 -> 3; case T3 -> 5; case T4 -> 7;
            };
            case LATEGAME_GATE -> 7;
        };
    }

    private static Condition tertiaryCondition(MagicNode node) {
        if (node.kind() == MagicNodeKind.TRUNK_FOUNDATION) return null;

        // SIGNATURE_SPELL with known role logic from old SpellRole
        return switch (node.id()) {
            // ── Fire ──
            case "fire/signature/firebolt" -> stat(StatType.FIRE_AFFINITY, 1);
            case "fire/signature/blaze_storm" -> stat(StatType.FIRE_AFFINITY, 3);
            case "fire/signature/meteor" -> stat(StatType.INTIMIDATION, 5);
            case "fire/signature/burning_dash" -> stat(StatType.AGILITY, 3);

            // ── Water ──
            case "water/signature/icicle" -> stat(StatType.WATER_AFFINITY, 1);
            case "water/signature/frost_step" -> stat(StatType.AGILITY, 1);
            case "water/signature/blizzard" -> stat(StatType.WATER_AFFINITY, 3);
            case "water/signature/healing_circle" -> stat(StatType.WILLPOWER, 5);

            // ── Air ──
            case "air/signature/lightning_bolt" -> stat(StatType.AIR_AFFINITY, 1);
            case "air/signature/wind_jump" -> stat(StatType.AGILITY, 1);
            case "air/signature/thunderstorm" -> stat(StatType.AIR_AFFINITY, 3);
            case "air/signature/tornado" -> stat(StatType.AGILITY, 5);

            // ── Earth ──
            case "earth/signature/poison_breath" -> stat(StatType.EARTH_AFFINITY, 1);
            case "earth/signature/acid_rain" -> stat(StatType.WILLPOWER, 3);
            case "earth/signature/earthquake" -> stat(StatType.INTIMIDATION, 5);

            // ── Holy ──
            case "holy/signature/divine_smite" -> stat(StatType.WILLPOWER, 1);
            case "holy/signature/angel_wings" -> stat(StatType.PHYSICAL_ENDURANCE, 3);
            case "holy/signature/blessing_of_life" -> stat(StatType.WILLPOWER, 5);

            // ── Blood ──
            case "blood/signature/blood_slash" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "blood/signature/raise_dead" -> stat(StatType.TRACKING, 3);
            case "blood/signature/raise_hell" -> stat(StatType.INTIMIDATION, 5);

            // ── Ender ──
            case "ender/signature/magic_arrow" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/signature/teleport" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/signature/starfall" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/signature/black_hole" -> stat(StatType.KEEN_SENSES, 5);

            // ── Evocation ──
            case "evocation/signature/gust" -> stat(StatType.TRACKING, 1);
            case "evocation/signature/invisibility" -> stat(StatType.TRACKING, 3);
            case "evocation/signature/chain_creeper" -> stat(StatType.ERUDITION, 5);

            // ── Eldritch ──
            case "eldritch/signature/eldritch_blast" -> stat(StatType.WILLPOWER, 1);
            case "eldritch/signature/telekinesis" -> stat(StatType.MAGIC_RESISTANCE, 3);
            case "eldritch/signature/abyssal_shroud" -> stat(StatType.WILLPOWER, 5);

            // ── Branch openers (kind = BRANCH_OPENER) ──
            case "fire/opener/ignition" -> stat(StatType.FIRE_AFFINITY, 2);
            case "water/opener/ice_awakening" -> stat(StatType.WATER_AFFINITY, 2);
            case "air/opener/spark_awakening" -> stat(StatType.AIR_AFFINITY, 2);
            case "earth/opener/nature_awakening" -> stat(StatType.EARTH_AFFINITY, 2);
            case "holy/opener/bless_awakening" -> stat(StatType.WILLPOWER, 2);
            case "blood/opener/hemo_awakening" -> stat(StatType.PHYSICAL_ENDURANCE, 2);
            case "ender/opener/void_awakening" -> stat(StatType.KEEN_SENSES, 2);
            case "evocation/opener/trick_awakening" -> stat(StatType.TRACKING, 2);
            case "eldritch/opener/dark_awakening" -> stat(StatType.WILLPOWER, 2);

            // ── Fire tiers ──
            case "fire/tier/ember_path" -> stat(StatType.FIRE_AFFINITY, 1);
            case "fire/tier/flame_path" -> stat(StatType.FIRE_AFFINITY, 3);
            case "fire/tier/inferno_path" -> stat(StatType.INTIMIDATION, 5);

            // ── Water tiers ──
            case "water/tier/frost_path" -> stat(StatType.WATER_AFFINITY, 1);
            case "water/tier/chill_path" -> stat(StatType.WATER_AFFINITY, 3);
            case "water/tier/glacier_path" -> stat(StatType.MAGIC_RESISTANCE, 5);

            // ── Air tiers ──
            case "air/tier/spark_path" -> stat(StatType.AIR_AFFINITY, 1);
            case "air/tier/storm_path" -> stat(StatType.AIR_AFFINITY, 3);
            case "air/tier/thunder_path" -> stat(StatType.AGILITY, 5);

            // ── Earth tiers ──
            case "earth/tier/poison_path" -> stat(StatType.EARTH_AFFINITY, 1);
            case "earth/tier/toxin_path" -> stat(StatType.EARTH_AFFINITY, 3);
            case "earth/tier/grand_nature_path" -> stat(StatType.PHYSICAL_ENDURANCE, 5);

            // ── Holy tiers ──
            case "holy/tier/bless_path" -> stat(StatType.WILLPOWER, 1);
            case "holy/tier/grace_path" -> stat(StatType.ERUDITION, 3);
            case "holy/tier/divine_path" -> stat(StatType.WILLPOWER, 5);

            // ── Blood tiers ──
            case "blood/tier/hemo_path" -> stat(StatType.PHYSICAL_ENDURANCE, 1);
            case "blood/tier/drain_path" -> stat(StatType.INTIMIDATION, 3);
            case "blood/tier/exsanguinate_path" -> stat(StatType.WILLPOWER, 5);

            // ── Ender tiers ──
            case "ender/tier/void_path" -> stat(StatType.KEEN_SENSES, 1);
            case "ender/tier/rift_path" -> stat(StatType.KEEN_SENSES, 3);
            case "ender/tier/abyss_path" -> stat(StatType.KEEN_SENSES, 5);

            // ── Evocation tiers ──
            case "evocation/tier/trick_path" -> stat(StatType.TRACKING, 1);
            case "evocation/tier/illusion_path" -> stat(StatType.TRACKING, 3);
            case "evocation/tier/mastery_path" -> stat(StatType.ERUDITION, 5);

            // ── Eldritch tiers ──
            case "eldritch/tier/dark_path" -> stat(StatType.MAGIC_RESISTANCE, 1);
            case "eldritch/tier/void_gaze_path" -> stat(StatType.WILLPOWER, 3);
            case "eldritch/tier/abyssal_path" -> stat(StatType.WILLPOWER, 5);

            // Tensura signatures — use branch affinity as tertiary
            default -> {
                if (node.id().contains("/tensura_")) {
                    StatType branchAffinity = branchAffinity(node.branch());
                    if (branchAffinity != null) {
                        int threshold = switch (node.tier()) {
                            case T1 -> 1; case T2 -> 3; case T3 -> 5; case T4 -> 7;
                        };
                        yield stat(branchAffinity, threshold);
                    }
                }
                yield null;
            }
        };
    }

    private static Condition stat(StatType type, int level) {
        return new Condition.StatCondition(type, level);
    }

    private static StatType branchAffinity(MagicBranch branch) {
        return switch (branch) {
            case FIRE -> StatType.FIRE_AFFINITY;
            case WATER -> StatType.WATER_AFFINITY;
            case AIR -> StatType.AIR_AFFINITY;
            case EARTH -> StatType.EARTH_AFFINITY;
            case HOLY, BLOOD, ELDRITCH -> StatType.WILLPOWER;
            case ENDER -> StatType.KEEN_SENSES;
            case EVOCATION -> StatType.TRACKING;
            case COMMON -> null;
        };
    }

    private MagicNodeMigration() {}
}
```

- [ ] **Step 2: Write migration test**

**File:** `src/test/java/tong/statmod/magic/MagicNodeMigrationTest.java`

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MagicNodeMigrationTest {

    private final PlayerStatData data = new PlayerStatData();
    private final ConditionContext ctx = ConditionContext.defaultContext();

    @Test void migrated_firebolt_requires_arcane2_erudition2_affinity1() {
        Condition c = MagicNodeMigration.defaultCondition(firebolt());
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 2);
        data.setLevel(StatType.FIRE_AFFINITY.index, 1);
        assertTrue(c.evaluate(data, ctx));
    }

    @Test void migrated_firebolt_fails_without_affinity() {
        Condition c = MagicNodeMigration.defaultCondition(firebolt());
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 2);
        data.setLevel(StatType.FIRE_AFFINITY.index, 0);
        assertFalse(c.evaluate(data, ctx));
    }

    @Test void migrated_fire_opener_requires_arcane2_erudition1_affinity2() {
        Condition c = MagicNodeMigration.defaultCondition(fireOpener());
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 1);
        data.setLevel(StatType.FIRE_AFFINITY.index, 2);
        assertTrue(c.evaluate(data, ctx));
    }

    @Test void migrated_trunk_has_no_tertiary() {
        Condition c = MagicNodeMigration.defaultCondition(trunk());
        data.setLevel(StatType.ARCANE_POWER.index, 1);
        data.setLevel(StatType.ERUDITION.index, 1);
        assertTrue(c.evaluate(data, ctx));
        // Trunk should NOT require any tertiary stat
        assertInstanceOf(Condition.And.class, c);
        Condition.And and = (Condition.And) c;
        assertEquals(2, and.children().size(), "Trunk should only have 2 conditions (no tertiary)");
    }

    @Test void migrated_meteor_requires_intimidation5() {
        Condition c = MagicNodeMigration.defaultCondition(meteor());
        data.setLevel(StatType.ARCANE_POWER.index, 6);
        data.setLevel(StatType.ERUDITION.index, 5);
        data.setLevel(StatType.INTIMIDATION.index, 5);
        assertTrue(c.evaluate(data, ctx));
    }

    @Test void migrated_burningDash_requires_agility3() {
        Condition c = MagicNodeMigration.defaultCondition(burningDash());
        data.setLevel(StatType.ARCANE_POWER.index, 4);
        data.setLevel(StatType.ERUDITION.index, 3);
        data.setLevel(StatType.AGILITY.index, 3);
        assertTrue(c.evaluate(data, ctx));
    }

    private static MagicNode firebolt() {
        return new MagicNode("fire/signature/firebolt", MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1, MagicCurrency.SCHOOL,
                1, List.of("fire/tier/ember_path"), Set.of("irons_spellbooks:firebolt"), null);
    }

    private static MagicNode fireOpener() {
        return new MagicNode("fire/opener/ignition", MagicBranch.FIRE,
                MagicNodeKind.BRANCH_OPENER, MagicTier.T1, MagicCurrency.ARCANE,
                2, List.of("common/foundation/arcane_focus"), Set.of(), null);
    }

    private static MagicNode trunk() {
        return new MagicNode("common/foundation/arcane_focus", MagicBranch.COMMON,
                MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1, MagicCurrency.ARCANE,
                1, List.of(), Set.of(), null);
    }

    private static MagicNode meteor() {
        return new MagicNode("fire/signature/meteor", MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL, MagicTier.T3, MagicCurrency.SCHOOL,
                3, List.of("fire/signature/firebolt"), Set.of("irons_spellbooks:meteor"), null);
    }

    private static MagicNode burningDash() {
        return new MagicNode("fire/signature/burning_dash", MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2, MagicCurrency.SCHOOL,
                2, List.of("fire/signature/firebolt"), Set.of("irons_spellbooks:burning_dash"), null);
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests "tong.statmod.magic.MagicNodeMigrationTest"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicNodeMigration.java src/test/java/tong/statmod/magic/MagicNodeMigrationTest.java
git commit -m "feat(magic): migration helper from old D1/D2/D3 tables to Condition trees"
```

---

### Task 5: Update MagicTreeCatalog (migrate all 249 nodes)

**Files:**
- Modify: `src/main/java/tong/statmod/magic/MagicTreeCatalog.java`

Replace the `add(MagicNode)` method and `ensureRoleTagged()`:

- [ ] **Step 1: Replace `add()` and `ensureRoleTagged()`**

In `MagicTreeCatalog.java`, replace the `add` and `ensureRoleTagged` methods:

```java
private static void add(MagicNode node) {
    // If node has no condition yet, auto-generate one from old D1/D2/D3 tables
    // This keeps runtime behavior identical until per-spell customization happens.
    MagicNode finalNode = node.condition() == null
            ? new MagicNode(node.id(), node.branch(), node.kind(), node.tier(),
                    node.currency(), node.cost(), node.prerequisites(),
                    node.learnedSpells(), MagicNodeMigration.defaultCondition(node))
            : node;
    if (BY_ID.put(finalNode.id(), finalNode) != null) {
        throw new IllegalStateException("duplicate node id " + finalNode.id());
    }
    BY_BRANCH.computeIfAbsent(finalNode.branch(), k -> new ArrayList<>()).add(finalNode);
}
```

Remove the entire `ensureRoleTagged` method.

- [ ] **Step 2: Run compile to verify**

Run: `./gradlew compileJava` — expected: 0 errors (the catalog `new MagicNode(...)` calls compile because `null` is a valid value for the new `Condition` field, or they already pass `null` implicitly through the old 8-arg constructor... wait, the old 8-arg constructor was removed).

Actually, the old 8-arg constructor is removed in Task 3. So now every `new MagicNode(...)` call in the catalog only has a 9-arg constructor that requires `Condition` as last param. The `sig()` helper method needs updating too.

- [ ] **Step 2b: Fix the `sig()` helper**

```java
private static MagicNode sig(String id, MagicBranch branch, MagicTier tier, int cost, String prereq, String... spells) {
    return new MagicNode(id, branch, MagicNodeKind.SIGNATURE_SPELL, tier, MagicCurrency.SCHOOL,
            cost, List.of(prereq), Set.of(spells), null);
    // null condition → MagicNodeMigration.defaultCondition() applied in add()
}
```

- [ ] **Step 2c: Fix the `tensuraSig()` helper (identical pattern)**

The `tensuraSig()` calls `sig()` internally, so it's already covered.

- [ ] **Step 3: Run compileJava**

Run: `./gradlew compileJava` — expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run any test that uses MagicTreeCatalog**

Run: `./gradlew test` — verify everything passes. Existing tests might fail if they reference `MagicNodeStatRequirements` — this is expected and will be fixed in subsequent tasks.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicTreeCatalog.java
git commit -m "refactor(magic): migrate MagicTreeCatalog to use Condition trees via MagicNodeMigration"
```

---

### Task 6: Update MagicEligibilityResolver

**Files:**
- Modify: `src/main/java/tong/statmod/magic/MagicEligibilityResolver.java`
- Modify: `src/test/java/tong/statmod/magic/MagicEligibilityResolverTest.java`

- [ ] **Step 1: Replace MagicNodeStatRequirements calls with ConditionEvaluator**

Replace the whole file:

```java
package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

import java.util.List;

public final class MagicEligibilityResolver {
    public enum Failure {
        NONE,
        LOCKED,
        MISSING_PREREQ,
        NOT_ENOUGH_POINTS,
        ALREADY_UNLOCKED,
        NO_RACE,
        RUNTIME_GRANT_FAILED,
        CONDITION_NOT_MET
    }

    public record Result(Failure failure, int adjustedCost, List<String> missingConditions) {
        public Result(Failure failure, int adjustedCost) {
            this(failure, adjustedCost, List.of());
        }
    }

    private MagicEligibilityResolver() {}

    public static Result evaluate(PlayerStatData data, MagicNode node) {
        if (data == null || node == null) {
            return new Result(Failure.MISSING_PREREQ, 0);
        }
        if (data.hasMagicNode(node.id())) {
            return new Result(Failure.ALREADY_UNLOCKED, node.cost());
        }
        if (node.branch() != MagicBranch.COMMON && data.getMagicRace() == null) {
            return new Result(Failure.NO_RACE, 0);
        }
        for (String p : node.prerequisites()) {
            if (MagicTreeCatalog.LOCKED_SENTINEL.equals(p)) {
                return new Result(Failure.LOCKED, node.cost());
            }
            if (!data.hasMagicNode(p)) {
                return new Result(Failure.MISSING_PREREQ, node.cost());
            }
        }

        // Condition tree check (replaces old 3-stat-gates)
        if (!ConditionEvaluator.evaluate(node, data)) {
            List<String> missing = ConditionEvaluator.describeMissing(node, data);
            return new Result(Failure.CONDITION_NOT_MET, node.cost(), missing);
        }

        int cost = node.cost();
        if (data.getMagicPoints() < cost) {
            return new Result(Failure.NOT_ENOUGH_POINTS, cost);
        }
        return new Result(Failure.NONE, cost);
    }

    @Deprecated
    public static int affinityAdjustedCost(PlayerStatData data, MagicBranch branch, int baseCost) {
        return baseCost;
    }
}
```

- [ ] **Step 2: Update MagicEligibilityResolverTest.java**

Replace failure references from `STAT_REQUIREMENT_NOT_MET` to `CONDITION_NOT_MET`, and replace `result.missingStats()` with `result.missingConditions()`:

```java
package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.*;

class MagicEligibilityResolverTest {

    private static PlayerStatData fullyEquipped(MagicRace race, MagicBranch chosenStart) {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(race);
        d.setChosenStartBranch(chosenStart);
        d.addMagicPoints(99);
        d.setLevel(StatType.ARCANE_POWER.index, 20);
        d.setLevel(StatType.ERUDITION.index, 20);
        for (StatType s : StatType.values()) {
            d.setLevel(s.index, Math.max(d.getLevel(s.index), 20));
        }
        return d;
    }

    @Test void missing_prereq_fails() {
        PlayerStatData d = fullyEquipped(MagicRace.ELF, MagicBranch.AIR);
        MagicNode fireOpener = MagicTreeCatalog.byId("fire/opener/ignition");
        assertEquals(MagicEligibilityResolver.Failure.MISSING_PREREQ,
                MagicEligibilityResolver.evaluate(d, fireOpener).failure());
    }

    @Test void not_enough_points_fails() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.ELF);
        d.setLevel(StatType.ARCANE_POWER.index, 20);
        d.setLevel(StatType.ERUDITION.index, 20);
        MagicNode root = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicEligibilityResolver.Failure.NOT_ENOUGH_POINTS,
                MagicEligibilityResolver.evaluate(d, root).failure());
    }

    @Test void common_trunk_allowed_without_race() {
        PlayerStatData d = new PlayerStatData();
        d.addMagicPoints(5);
        d.setLevel(StatType.ARCANE_POWER.index, 5);
        d.setLevel(StatType.ERUDITION.index, 5);
        MagicNode node = MagicTreeCatalog.byId("common/foundation/arcane_focus");
        assertEquals(MagicEligibilityResolver.Failure.NONE,
                MagicEligibilityResolver.evaluate(d, node).failure());
    }

    @Test void non_common_node_fails_with_no_race_selected() {
        PlayerStatData d = new PlayerStatData();
        d.addMagicPoints(99);
        d.setLevel(StatType.ARCANE_POWER.index, 20);
        d.setLevel(StatType.ERUDITION.index, 20);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode node = MagicTreeCatalog.byId("fire/opener/ignition");
        assertEquals(MagicEligibilityResolver.Failure.NO_RACE,
                MagicEligibilityResolver.evaluate(d, node).failure());
    }

    @Test void low_arcane_power_blocks_with_CONDITION_NOT_MET() {
        PlayerStatData d = fullyEquipped(MagicRace.ELF, MagicBranch.FIRE);
        d.setLevel(StatType.ARCANE_POWER.index, 0);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        MagicEligibilityResolver.Result result = MagicEligibilityResolver.evaluate(d, opener);
        assertEquals(MagicEligibilityResolver.Failure.CONDITION_NOT_MET, result.failure());
        assertFalse(result.missingConditions().isEmpty());
    }

    @Test void low_tertiary_blocks_for_mobility_spell() {
        PlayerStatData d = fullyEquipped(MagicRace.HUMAN, MagicBranch.FIRE);
        d.setLevel(StatType.AGILITY.index, 0);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        d.addMagicNode("water/opener/ice_awakening");
        d.addMagicNode("water/tier/frost_path");
        MagicNode frostStep = MagicTreeCatalog.byId("water/signature/frost_step");
        MagicEligibilityResolver.Result result = MagicEligibilityResolver.evaluate(d, frostStep);
        assertEquals(MagicEligibilityResolver.Failure.CONDITION_NOT_MET, result.failure());
    }

    @Test void all_gates_met_returns_NONE() {
        PlayerStatData d = fullyEquipped(MagicRace.HUMAN, MagicBranch.FIRE);
        d.addMagicNode("common/foundation/arcane_focus");
        d.addMagicNode("common/foundation/mana_well");
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        MagicEligibilityResolver.Result result = MagicEligibilityResolver.evaluate(d, opener);
        assertEquals(MagicEligibilityResolver.Failure.NONE, result.failure());
        assertTrue(result.missingConditions().isEmpty());
    }

    @Test void deprecated_affinity_adjusted_cost_returns_base() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicRace(MagicRace.BEAST);
        int cost = MagicEligibilityResolver.affinityAdjustedCost(d, MagicBranch.FIRE, 4);
        assertEquals(4, cost);
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests "tong.statmod.magic.MagicEligibilityResolverTest"`
Expected: BUILD SUCCESSFUL (all 8 tests pass)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicEligibilityResolver.java src/test/java/tong/statmod/magic/MagicEligibilityResolverTest.java
git commit -m "refactor(magic): replace 3-gate Stats with Condition tree in MagicEligibilityResolver"
```

---

### Task 7: Update MagicUnlockFeedbackMessageFactory

**Files:**
- Modify: `src/main/java/tong/statmod/magic/MagicUnlockFeedbackMessageFactory.java`
- Modify: `src/main/java/tong/statmod/magic/MagicTreeProgressionService.java`

- [ ] **Step 1: Update MagicUnlockFeedbackMessageFactory to use new Result type**

Replace:

```java
    public static MagicUnlockFeedbackMessage forFailure(MagicNode node,
                                                          MagicEligibilityResolver.Failure failure,
                                                          java.util.List<MagicNodeStatRequirements.StatGate> missingStats) {
        return buildMessage(node, failure, missingStats == null ? java.util.List.of() : missingStats);
    }
```

With:

```java
    public static MagicUnlockFeedbackMessage forFailure(MagicNode node,
                                                          MagicEligibilityResolver.Failure failure,
                                                          java.util.List<String> missingConditions) {
        return buildMessage(node, failure, missingConditions == null ? java.util.List.of() : missingConditions);
    }
```

And update `buildMessage`:

```java
    private static MagicUnlockFeedbackMessage buildMessage(MagicNode node,
                                                             MagicEligibilityResolver.Failure failure,
                                                             java.util.List<String> missingConditions) {
        String message = messageFor(failure);
        if (failure == MagicEligibilityResolver.Failure.CONDITION_NOT_MET
                && missingConditions != null && !missingConditions.isEmpty()) {
            StringBuilder sb = new StringBuilder(message).append(':');
            for (String cond : missingConditions) {
                sb.append("\n• ").append(cond);
            }
            message = sb.toString();
        }
        return new MagicUnlockFeedbackMessage(
                Component.literal(titleFor(node)),
                Component.literal(message)
        );
    }
```

And update the messageFor switch:

```java
    case CONDITION_NOT_MET -> "Conditions not met";
```

- [ ] **Step 2: Update MagicTreeProgressionService references to StatGate**

In `MagicTreeProgressionService.java`:

```java
    public record UnlockResult(boolean success,
                                MagicEligibilityResolver.Failure failure,
                                int spent,
                                java.util.List<String> missingConditions) {
        public static UnlockResult ok(int spent) {
            return new UnlockResult(true, MagicEligibilityResolver.Failure.NONE, spent, java.util.List.of());
        }
        public static UnlockResult fail(MagicEligibilityResolver.Failure f) {
            return new UnlockResult(false, f, 0, java.util.List.of());
        }
        public static UnlockResult fail(MagicEligibilityResolver.Result eval) {
            return new UnlockResult(false, eval.failure(), 0, eval.missingConditions());
        }
    }
```

- [ ] **Step 3: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run tests**

Run: `./gradlew test`
Expected: all pass (MagicNodeStatRequirementsTest and SpellRoleTest will fail — they test deleted classes)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/magic/MagicUnlockFeedbackMessageFactory.java src/main/java/tong/statmod/magic/MagicTreeProgressionService.java
git commit -m "refactor(magic): StatGate references replaced with String conditions"
```

---

### Task 8: Update PuffishMagicTreeBuilder (UI rendering)

**Files:**
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java`

- [ ] **Step 1: Replace `requirementsLineFor` and `appendGate`**

Replace the old methods:

```java
    private static String requirementsLineFor(MagicNode node) {
        if (node.condition() == null) return "";
        return "Requirements:\n" + formatCondition(node.condition(), 0);
    }

    private static String formatCondition(Condition cond, int indent) {
        String prefix = "  ".repeat(indent);
        return switch (cond) {
            case Condition.And and -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < and.children().size(); i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(prefix).append("• ").append(formatCondition(and.children().get(i), indent));
                }
                yield sb.toString();
            }
            case Condition.Or or -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < or.children().size(); i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(prefix).append("• ").append(formatCondition(or.children().get(i), indent));
                }
                yield sb.toString();
            }
            case Condition.StatCondition sc ->
                    sc.stat().displayName + " ≥ " + sc.minLevel();
            case Condition.RaceCondition rc ->
                    "Race: " + rc.race().name();
            case Condition.HasSpellCondition hsc ->
                    "Spell: " + hsc.spellId();
            case Condition.HasNodeCondition hnc ->
                    "Node: " + hnc.nodeId();
            case Condition.BranchTierCondition btc ->
                    btc.branch().id + " Tier ≥ " + btc.minTier();
            case Condition.GlobalLevelCondition glc ->
                    "Global Level ≥ " + glc.minLevel();
        };
    }
```

- [ ] **Step 2: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java
git commit -m "refactor(puffish): render Condition tree instead of old 3-gate stats"
```

---

### Task 9: Delete deprecated files

**Files:**
- Delete: `src/main/java/tong/statmod/magic/MagicNodeStatRequirements.java`
- Delete: `src/main/java/tong/statmod/magic/SpellRole.java`
- Delete: `src/main/java/tong/statmod/magic/SpellRoleInference.java`
- Delete: `src/test/java/tong/statmod/magic/MagicNodeStatRequirementsTest.java`
- Delete: `src/test/java/tong/statmod/magic/SpellRoleTest.java`

- [ ] **Step 1: Delete the files**

```bash
git rm src/main/java/tong/statmod/magic/MagicNodeStatRequirements.java
git rm src/main/java/tong/statmod/magic/SpellRole.java
git rm src/main/java/tong/statmod/magic/SpellRoleInference.java
git rm src/test/java/tong/statmod/magic/MagicNodeStatRequirementsTest.java
git rm src/test/java/tong/statmod/magic/SpellRoleTest.java
git rm src/test/java/tong/statmod/magic/SpellRoleInferenceTest.java
```

- [ ] **Step 2: Check for remaining references**

Search the entire codebase for `MagicNodeStatRequirements`, `SpellRole`, `SpellRoleInference` to make sure nothing else references them.

Run: `rg "MagicNodeStatRequirements|SpellRole|SpellRoleInference" src/`

Expected: no results (all references removed in previous tasks)

- [ ] **Step 3: Compile and test**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit**

```bash
git commit -m "refactor(magic): remove deprecated D1/D2/D3 classes (SpellRole, StatRequirements, Inference)"
```

---

### Task 10: Verify full build

- [ ] **Step 1: Full clean build**

Run: `./gradlew clean check`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: If any tests fail, diagnose and fix**

Check the test report at `build/reports/tests/test/index.html` for any failures. Expected possible failures:
- Any test that directly constructs MagicNodeStatRequirements or SpellRole → already deleted
- Any test that uses `STAT_REQUIREMENT_NOT_MET` → already renamed to `CONDITION_NOT_MET`
- Any test that calls `missingStats()` → already renamed to `missingConditions()`

- [ ] **Step 3: Final commit**

```bash
git commit --allow-empty -m "chore(magic): complete per-spell boolean condition gate migration"
```
