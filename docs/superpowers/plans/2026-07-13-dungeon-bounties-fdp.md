# Dungeon-Themed FDP Bounties — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generic villager-profession bounties on the city bounty boards with hard, dungeon-themed bounties (kill dungeon mobs/bosses, haul dungeon materials, reach a floor) that pay only in physical FDP currency.

**Architecture:** 100% datapack for objectives/rewards/decrees (Bountiful pools + decree overrides), plus one small Java layer for the "reach floor N" objective: hidden advancements granted from `DungeonTeleportHandler.enterFloor` via a pure helper, targeted by Bountiful `criteria` objectives.

**Tech Stack:** NeoForge 1.21.1, Bountiful 8.0.0-beta.2 datapacks (`bounty_pools`, `bounty_decrees`), vanilla advancements, JUnit Jupiter 5.

Spec: `docs/superpowers/specs/2026-07-13-dungeon-bounties-fdp-design.md`

---

## File Structure

**Create (datapack):**
- `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_slay_objs.json` — entity-kill objectives (trash mobs, tiered)
- `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_boss_objs.json` — entity-kill objectives (bosses, EPIC)
- `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_haul_objs.json` — item-collect objectives (dungeon materials)
- `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_delve_objs.json` — criteria objectives (reach floor)
- `src/main/resources/data/statmod/advancement/dungeon/delve_10.json` … `delve_100.json` — hidden, code-granted

**Modify (datapack):**
- `src/main/resources/data/bountiful/bounty_pools/statmod/fdp_rewards.json` — raise big-note caps
- `src/main/resources/data/bountiful/bounty_decrees/bountiful/*.json` (12 files) — objectives→dungeon pools, rewards→fdp only

**Create (Java):**
- `src/main/java/tong/statmod/dungeon/DungeonBountyMilestones.java` — pure helper (thresholds crossed)
- `src/test/java/tong/statmod/dungeon/DungeonBountyMilestonesTest.java`
- `src/test/java/tong/statmod/integration/bountiful/DungeonBountyResourcesTest.java`

**Modify (Java):**
- `src/main/java/tong/statmod/dungeon/DungeonTeleportHandler.java` — award milestone advancements in `enterFloor`

---

## Task 1: Dungeon objective pools (datapack)

**Files:**
- Create: `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_slay_objs.json`
- Create: `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_boss_objs.json`
- Create: `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_haul_objs.json`
- Test: `src/test/java/tong/statmod/integration/bountiful/DungeonBountyResourcesTest.java`

- [ ] **Step 1: Write the failing test** (only the pool-content parts for now)

Create `src/test/java/tong/statmod/integration/bountiful/DungeonBountyResourcesTest.java`:

```java
package tong.statmod.integration.bountiful;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonBountyResourcesTest {
    private static final Path POOLS = Path.of("src/main/resources/data/bountiful/bounty_pools/statmod");

    private static String read(String file) throws Exception {
        return Files.readString(POOLS.resolve(file));
    }

    @Test
    void slayPoolHasVanillaBaselineAndModdedMobs() throws Exception {
        String s = read("dungeon_slay_objs.json");
        assertTrue(s.contains("\"type\": \"entity\""), "objectifs de type entity");
        assertTrue(s.contains("minecraft:zombie") && s.contains("minecraft:wither_skeleton"),
                "base vanilla garantie");
        assertTrue(s.contains("slu:knight") && s.contains("slu:elite_knight"),
                "mobs de donjon moddés");
    }

    @Test
    void bossPoolHasWardenAndSluBosses() throws Exception {
        String s = read("dungeon_boss_objs.json");
        assertTrue(s.contains("\"type\": \"entity\""));
        assertTrue(s.contains("minecraft:warden"), "boss vanilla garanti");
        assertTrue(s.contains("slu:boss_malenia") && s.contains("slu:boss_artorias"),
                "boss du roster");
    }

    @Test
    void haulPoolAsksForDungeonMaterials() throws Exception {
        String s = read("dungeon_haul_objs.json");
        assertTrue(s.contains("\"type\": \"item\""));
        assertTrue(s.contains("statmod:rune_essence_arcane")
                && s.contains("statmod:rune_essence_pyrium")
                && s.contains("statmod:rune_essence_mithril"), "rune essences");
        assertTrue(s.contains("statmod:respec_stone"), "matériau donjon signature");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.DungeonBountyResourcesTest" --console=plain`
Expected: FAIL — `NoSuchFileException` (pools not created yet).

- [ ] **Step 3: Create `dungeon_slay_objs.json`**

```json
{
  "content": {
    "d_zombie":        { "type": "entity", "timeMult": 5.0, "content": "minecraft:zombie",         "amount": {"min":4,"max":8}, "unitWorth": 220 },
    "d_skeleton":      { "type": "entity", "timeMult": 5.0, "content": "minecraft:skeleton",       "amount": {"min":4,"max":8}, "unitWorth": 240 },
    "d_hollow":        { "type": "entity", "timeMult": 5.0, "content": "slu:hollow",               "amount": {"min":3,"max":8}, "unitWorth": 260 },
    "d_armed_hollow":  { "type": "entity", "timeMult": 5.0, "content": "slu:armed_hollow",         "amount": {"min":3,"max":6}, "unitWorth": 300 },
    "d_thief":         { "type": "entity", "timeMult": 5.0, "content": "slu:thief",                "amount": {"min":2,"max":6}, "unitWorth": 320 },
    "d_wither_skele":  { "type": "entity", "rarity": "UNCOMMON", "timeMult": 6.0, "content": "minecraft:wither_skeleton", "amount": {"min":2,"max":5}, "unitWorth": 520 },
    "d_knight":        { "type": "entity", "rarity": "UNCOMMON", "timeMult": 6.0, "content": "slu:knight",              "amount": {"min":2,"max":5}, "unitWorth": 620 },
    "d_castle_guard":  { "type": "entity", "rarity": "UNCOMMON", "timeMult": 6.0, "content": "slu:castle_guard",        "amount": {"min":2,"max":4}, "unitWorth": 700 },
    "d_cultist":       { "type": "entity", "rarity": "UNCOMMON", "timeMult": 6.0, "content": "irons_spellbooks:cultist","amount": {"min":2,"max":4}, "unitWorth": 780 },
    "d_elite_knight":  { "type": "entity", "rarity": "RARE", "timeMult": 7.0, "content": "slu:elite_knight",           "amount": {"min":1,"max":3}, "unitWorth": 1400 },
    "d_dark_knight":   { "type": "entity", "rarity": "RARE", "timeMult": 7.0, "content": "slu:dark_knight",            "amount": {"min":1,"max":3}, "unitWorth": 1600 },
    "d_necromancer":   { "type": "entity", "rarity": "RARE", "timeMult": 7.0, "content": "irons_spellbooks:necromancer","amount": {"min":1,"max":2}, "unitWorth": 2000 }
  }
}
```

- [ ] **Step 4: Create `dungeon_boss_objs.json`**

```json
{
  "content": {
    "b_warden":     { "type": "entity", "rarity": "EPIC", "timeMult": 12.0, "content": "minecraft:warden",              "amount": {"min":1,"max":1}, "unitWorth": 7000 },
    "b_artorias":   { "type": "entity", "rarity": "EPIC", "timeMult": 12.0, "content": "slu:boss_artorias",             "amount": {"min":1,"max":1}, "unitWorth": 9000 },
    "b_radahn":     { "type": "entity", "rarity": "EPIC", "timeMult": 12.0, "content": "slu:boss_radahn",               "amount": {"min":1,"max":1}, "unitWorth": 11000 },
    "b_malenia":    { "type": "entity", "rarity": "EPIC", "timeMult": 14.0, "content": "slu:boss_malenia",              "amount": {"min":1,"max":1}, "unitWorth": 13000 },
    "b_gael":       { "type": "entity", "rarity": "EPIC", "timeMult": 14.0, "content": "slu:boss_gael",                 "amount": {"min":1,"max":1}, "unitWorth": 13000 },
    "b_dead_king":  { "type": "entity", "rarity": "EPIC", "timeMult": 12.0, "content": "irons_spellbooks:dead_king",    "amount": {"min":1,"max":1}, "unitWorth": 9000 },
    "b_keeper":     { "type": "entity", "rarity": "EPIC", "timeMult": 12.0, "content": "irons_spellbooks:citadel_keeper","amount": {"min":1,"max":1}, "unitWorth": 9000 }
  }
}
```

- [ ] **Step 5: Create `dungeon_haul_objs.json`**

```json
{
  "content": {
    "h_ess_arcane":   { "type": "item", "rarity": "UNCOMMON", "content": "statmod:rune_essence_arcane",  "amount": {"min":2,"max":6}, "unitWorth": 600 },
    "h_ess_pyrium":   { "type": "item", "rarity": "UNCOMMON", "content": "statmod:rune_essence_pyrium",  "amount": {"min":2,"max":5}, "unitWorth": 650 },
    "h_ess_mithril":  { "type": "item", "rarity": "UNCOMMON", "content": "statmod:rune_essence_mithril", "amount": {"min":1,"max":4}, "unitWorth": 800 },
    "h_perk_tome":    { "type": "item", "rarity": "RARE", "content": "statmod:perk_tome",     "amount": {"min":1,"max":2}, "unitWorth": 2500 },
    "h_respec":       { "type": "item", "rarity": "RARE", "content": "statmod:respec_stone",  "amount": {"min":1,"max":2}, "unitWorth": 2500 },
    "h_neth_scrap":   { "type": "item", "rarity": "RARE", "content": "minecraft:netherite_scrap", "amount": {"min":1,"max":3}, "unitWorth": 3000 },
    "h_neth_ingot":   { "type": "item", "rarity": "EPIC", "content": "minecraft:netherite_ingot", "amount": {"min":1,"max":1}, "unitWorth": 12000 },
    "h_nether_star":  { "type": "item", "rarity": "EPIC", "content": "minecraft:nether_star",     "amount": {"min":1,"max":1}, "unitWorth": 15000 }
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.DungeonBountyResourcesTest" --console=plain`
Expected: PASS (3 tests).

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_slay_objs.json \
        src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_boss_objs.json \
        src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_haul_objs.json \
        src/test/java/tong/statmod/integration/bountiful/DungeonBountyResourcesTest.java
git commit -m "feat(bounty): dungeon slay/boss/haul objective pools in FDP"
```

---

## Task 2: Reach-floor advancements + criteria pool (datapack)

**Files:**
- Create: `src/main/resources/data/statmod/advancement/dungeon/delve_10.json`, `delve_25.json`, `delve_50.json`, `delve_100.json`
- Create: `src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_delve_objs.json`
- Test: extend `DungeonBountyResourcesTest`

- [ ] **Step 1: Add failing test methods**

Append to `DungeonBountyResourcesTest`:

```java
    @Test
    void delvePoolTargetsFloorAdvancements() throws Exception {
        String s = read("dungeon_delve_objs.json");
        assertTrue(s.contains("\"type\": \"criteria\""));
        assertTrue(s.contains("statmod:dungeon/delve_10")
                && s.contains("statmod:dungeon/delve_50")
                && s.contains("statmod:dungeon/delve_100"), "cible les advancements de palier");
    }

    @Test
    void delveAdvancementsExistAndAreCodeGranted() throws Exception {
        Path adv = Path.of("src/main/resources/data/statmod/advancement/dungeon");
        for (String f : new String[]{"delve_10.json","delve_25.json","delve_50.json","delve_100.json"}) {
            String s = Files.readString(adv.resolve(f));
            assertTrue(s.contains("\"trigger\": \"minecraft:impossible\""), f + " doit être code-granted");
            assertTrue(s.contains("\"reached\""), f + " doit exposer le critère 'reached'");
        }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.DungeonBountyResourcesTest" --console=plain`
Expected: FAIL — `NoSuchFileException` for delve files.

- [ ] **Step 3: Create the 4 advancements** (identical except title/description)

`delve_10.json` (repeat for 25/50/100, swapping the number in title/description and the criterion key name stays `reached`):

```json
{
  "display": {
    "title": "Plongée : étage 10",
    "description": "Atteindre l'étage 10 du Donjon d'Épreuve",
    "icon": { "item": "statmod:dungeon_beacon" },
    "frame": "task",
    "show_toast": false,
    "announce_to_chat": false,
    "hidden": true
  },
  "criteria": { "reached": { "trigger": "minecraft:impossible" } }
}
```

`delve_25.json`: title `"Plongée : étage 25"`, description `"Atteindre l'étage 25 du Donjon d'Épreuve"`.
`delve_50.json`: title `"Plongée : étage 50"`, description `"Atteindre l'étage 50 du Donjon d'Épreuve"`.
`delve_100.json`: title `"Plongée : étage 100"`, description `"Atteindre l'étage 100 du Donjon d'Épreuve"`.

- [ ] **Step 4: Create `dungeon_delve_objs.json`**

```json
{
  "content": {
    "delve_10":  { "type": "criteria", "rarity": "RARE", "content": "statmod:dungeon/delve_10",  "amount": {"min":1,"max":1}, "unitWorth": 3000 },
    "delve_25":  { "type": "criteria", "rarity": "RARE", "content": "statmod:dungeon/delve_25",  "amount": {"min":1,"max":1}, "unitWorth": 6000 },
    "delve_50":  { "type": "criteria", "rarity": "EPIC", "content": "statmod:dungeon/delve_50",  "amount": {"min":1,"max":1}, "unitWorth": 12000 },
    "delve_100": { "type": "criteria", "rarity": "EPIC", "content": "statmod:dungeon/delve_100", "amount": {"min":1,"max":1}, "unitWorth": 24000 }
  }
}
```

> **Run-time note for the implementer:** Bountiful's `criteria` type completes when the player *earns the advancement* whose id equals `content` (it registers a `SimpleCriterionTrigger`). If in-game testing shows Bountiful expects a bare criterion id instead of the advancement path, adjust `content` to match what `/bountiful` debug reports — the advancement id `statmod:dungeon/delve_N` is the primary hypothesis.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.DungeonBountyResourcesTest" --console=plain`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/data/statmod/advancement/dungeon/ \
        src/main/resources/data/bountiful/bounty_pools/statmod/dungeon_delve_objs.json \
        src/test/java/tong/statmod/integration/bountiful/DungeonBountyResourcesTest.java
git commit -m "feat(bounty): reach-floor criteria objectives + delve advancements"
```

---

## Task 3: Override decrees to dungeon pools + FDP-only rewards

**Files:**
- Modify: all 12 files in `src/main/resources/data/bountiful/bounty_decrees/bountiful/`
- Test: extend `DungeonBountyResourcesTest`

- [ ] **Step 1: Add failing test**

Append to `DungeonBountyResourcesTest`:

```java
    @Test
    void everyDecreeUsesOnlyDungeonPoolsAndFdpRewards() throws Exception {
        Path decrees = Path.of("src/main/resources/data/bountiful/bounty_decrees/bountiful");
        try (var paths = Files.list(decrees)) {
            var files = paths.filter(p -> p.toString().endsWith(".json")).toList();
            assertTrue(files.size() >= 12, "les 12 décrees de métier");
            for (Path file : files) {
                String s = Files.readString(file);
                assertTrue(s.contains("dungeon_slay_objs") && s.contains("dungeon_boss_objs")
                        && s.contains("dungeon_haul_objs") && s.contains("dungeon_delve_objs"),
                        file + " doit lister les 4 pools donjon");
                assertTrue(s.contains("fdp_rewards"), file + " doit récompenser en FDP");
                // Plus aucun pool vanilla d'objectifs/récompenses : les seuls pools d'objectifs
                // autorisés sont les 4 pools donjon ; la seule récompense autorisée est fdp_rewards.
                assertTrue(!s.contains("_all_objs") && !s.contains("_all_rews")
                                && !s.contains("_equip_rews") && !s.contains("_metal_objs"),
                        file + " ne doit plus référencer de pool vanilla partagé");
                String profession = file.getFileName().toString().replace(".json", "");
                assertTrue(!s.contains(profession + "_objs") && !s.contains(profession + "_rews"),
                        file + " ne doit plus référencer ses pools de métier vanilla");
            }
        }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.DungeonBountyResourcesTest" --console=plain`
Expected: FAIL — decrees still contain `_all_objs` / profession pools.

- [ ] **Step 3: Rewrite each decree**

For **every** file in `bounty_decrees/bountiful/`, **preserve its existing `linkedProfessions` array** and replace only `objectives` and `rewards`. Example for `armorer.json` (was `{"linkedProfessions":["armorer"],"objectives":["armorer_objs","_metal_objs","_all_objs"],"rewards":["armorer_rews","_all_rews","_equip_rews","fdp_rewards"]}`):

```json
{
  "linkedProfessions": ["armorer"],
  "objectives": ["dungeon_slay_objs", "dungeon_boss_objs", "dungeon_haul_objs", "dungeon_delve_objs"],
  "rewards": ["fdp_rewards"]
}
```

Apply the identical `objectives`/`rewards` block to all 12 files: `armorer, butcher, cleric, farmer, fisherman, fletcher, inventor, leatherer, librarian, mapper, shepherd, toolsmith` — keeping each file's own `linkedProfessions` value untouched (some link more than one profession).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.DungeonBountyResourcesTest" --console=plain`
Expected: PASS (6 tests).

- [ ] **Step 5: Update the legacy FDP test that assumed vanilla decrees**

`BountifulFdpResourcesTest.everyStandardDecreeIncludesFdpRewards` still passes (decrees keep `fdp_rewards`). No change needed — run it to confirm:

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.BountifulFdpResourcesTest" --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/data/bountiful/bounty_decrees/bountiful/ \
        src/test/java/tong/statmod/integration/bountiful/DungeonBountyResourcesTest.java
git commit -m "feat(bounty): decrees now yield only dungeon objectives paid in FDP"
```

---

## Task 4: Raise FDP big-note caps (datapack)

**Files:**
- Modify: `src/main/resources/data/bountiful/bounty_pools/statmod/fdp_rewards.json`

- [ ] **Step 1: Edit the two largest notes**

Change `fdp_note_5000` `amount.max` from `2` to `4`, and `fdp_note_10000` `amount.max` from `1` to `3`, so EPIC bounties (worth 12k–24k) settle in a few notes instead of huge stacks. Final file:

```json
{
  "currency": true,
  "content": {
    "fdp_50": {"type":"minecraft:item","content":"statmod:fdp_coin_50","unitWorth":50.0,"amount":{"min":1,"max":8}},
    "fdp_100": {"type":"minecraft:item","content":"statmod:fdp_coin_100","unitWorth":100.0,"amount":{"min":1,"max":8}},
    "fdp_200": {"type":"minecraft:item","content":"statmod:fdp_coin_200","unitWorth":200.0,"amount":{"min":1,"max":6}},
    "fdp_500": {"type":"minecraft:item","content":"statmod:fdp_coin_500","unitWorth":500.0,"amount":{"min":1,"max":5}},
    "fdp_1000": {"type":"minecraft:item","content":"statmod:fdp_note_1000","unitWorth":1000.0,"amount":{"min":1,"max":4}},
    "fdp_2000": {"type":"minecraft:item","content":"statmod:fdp_note_2000","unitWorth":2000.0,"amount":{"min":1,"max":3}},
    "fdp_5000": {"type":"minecraft:item","content":"statmod:fdp_note_5000","unitWorth":5000.0,"amount":{"min":1,"max":4}},
    "fdp_10000": {"type":"minecraft:item","content":"statmod:fdp_note_10000","unitWorth":10000.0,"amount":{"min":1,"max":3}}
  }
}
```

- [ ] **Step 2: Run the legacy FDP pool test (invariants unchanged)**

Run: `./gradlew test --tests "tong.statmod.integration.bountiful.BountifulFdpResourcesTest" --console=plain`
Expected: PASS (denominations + unitWorth invariants untouched).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/data/bountiful/bounty_pools/statmod/fdp_rewards.json
git commit -m "balance(bounty): raise FDP big-note payout caps for EPIC bounties"
```

---

## Task 5: `DungeonBountyMilestones` pure helper

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonBountyMilestones.java`
- Test: `src/test/java/tong/statmod/dungeon/DungeonBountyMilestonesTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DungeonBountyMilestonesTest {
    @Test
    void thresholdsAreTheFourDelvePaliers() {
        assertEquals(List.of(10, 25, 50, 100), DungeonBountyMilestones.thresholds());
    }

    @Test
    void reachedReturnsOnlyPaliersUpToDeepestFloor() {
        assertEquals(List.of(), DungeonBountyMilestones.reached(9));
        assertEquals(List.of(10), DungeonBountyMilestones.reached(24));
        assertEquals(List.of(10, 25), DungeonBountyMilestones.reached(25));
        assertEquals(List.of(10, 25, 50), DungeonBountyMilestones.reached(60));
        assertEquals(List.of(10, 25, 50, 100), DungeonBountyMilestones.reached(100));
    }

    @Test
    void advancementIdMatchesFloorTheme() {
        assertEquals("statmod:dungeon/delve_50", DungeonBountyMilestones.advancementId(50));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "tong.statmod.dungeon.DungeonBountyMilestonesTest" --console=plain`
Expected: FAIL — class not found.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/tong/statmod/dungeon/DungeonBountyMilestones.java`:

```java
package tong.statmod.dungeon;

import java.util.ArrayList;
import java.util.List;

/**
 * Paliers de profondeur qui accordent un advancement « Plongée » (ciblé par les bounties de type
 * criteria). Cœur pur, testable sans serveur.
 */
public final class DungeonBountyMilestones {

    private static final List<Integer> THRESHOLDS = List.of(10, 25, 50, 100);

    private DungeonBountyMilestones() {}

    /** Paliers ordonnés. */
    public static List<Integer> thresholds() { return THRESHOLDS; }

    /** Paliers franchis quand l'étage le plus profond atteint vaut {@code deepestFloor}. */
    public static List<Integer> reached(int deepestFloor) {
        List<Integer> out = new ArrayList<>();
        for (int t : THRESHOLDS) if (deepestFloor >= t) out.add(t);
        return out;
    }

    /** Id de l'advancement du palier (ex. 50 → {@code statmod:dungeon/delve_50}). */
    public static String advancementId(int threshold) {
        return "statmod:dungeon/delve_" + threshold;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "tong.statmod.dungeon.DungeonBountyMilestonesTest" --console=plain`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonBountyMilestones.java \
        src/test/java/tong/statmod/dungeon/DungeonBountyMilestonesTest.java
git commit -m "feat(bounty): DungeonBountyMilestones pure threshold helper"
```

---

## Task 6: Grant milestone advancements from `enterFloor`

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonTeleportHandler.java` (inside `enterFloor`, after `SyncHelper.syncStats(player)` near line 196)

- [ ] **Step 1: Add the award call**

In `enterFloor`, immediately after the existing `tong.statmod.network.SyncHelper.syncStats(player);` line, insert:

```java
        // Bounties « atteindre l'étage » : accorde les advancements de palier mérités selon
        // l'étage le plus profond atteint (rétroactif + idempotent — award ne re-déclenche pas).
        awardDelveMilestones(player, data.getDungeonFloorReached());
```

- [ ] **Step 2: Add the private helper method**

Add this method to `DungeonTeleportHandler` (near the other private helpers):

```java
    /** Accorde les advancements de palier « Plongée » (ciblés par les bounties de donjon). */
    private static void awardDelveMilestones(ServerPlayer player, int deepestFloor) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        for (int threshold : DungeonBountyMilestones.reached(deepestFloor)) {
            ResourceLocation id = ResourceLocation.parse(DungeonBountyMilestones.advancementId(threshold));
            var holder = server.getAdvancements().get(id);
            if (holder == null) continue; // datapack absent → no-op sûr
            var progress = player.getAdvancements().getOrStartProgress(holder);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(holder, criterion);
                }
            }
        }
    }
```

- [ ] **Step 3: Verify imports**

Ensure these imports exist at the top of `DungeonTeleportHandler.java` (add any missing):

```java
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
```

(`MinecraftServer` and `ServerPlayer` are already used in the file; add `ResourceLocation` if absent.)

- [ ] **Step 4: Compile**

Run: `./gradlew compileJava --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Full dungeon + bounty test suite**

Run: `./gradlew test --tests "tong.statmod.dungeon.*" --tests "tong.statmod.integration.bountiful.*" --console=plain`
Expected: PASS (all green, including the new tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonTeleportHandler.java
git commit -m "feat(bounty): grant delve milestone advancements on floor entry"
```

---

## Task 7: Full build + in-game verification checklist

- [ ] **Step 1: Full build**

Run: `./gradlew build --console=plain`
Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Step 2: In-game manual verification** (`./gradlew runClient`)

```
/statdungeon unlock 100
/statdungeon tp 10          # déclenche awardDelveMilestones → advancement delve_10 accordé
# revenir à la cité (return beacon / /statdungeon tp 0)
# ouvrir un tableau de bounty de la cité → vérifier :
#   - les objectifs proposés sont donjon (tuer slu:*, rapporter rune_essence, atteindre étage…)
#   - la récompense affichée est en billets/pièces FDP uniquement
# prendre un bounty "atteindre l'étage 10" → doit être DÉJÀ complété (advancement obtenu)
# prendre un bounty "tuer X slu:hollow" → tuer dans le donjon → objectif progresse
# compléter un bounty → recevoir des items FDP → déposer chez le Banquier → solde crédité
```

If the `criteria` (reach-floor) bounty does not auto-complete, run `/advancement grant @s only statmod:dungeon/delve_10` to confirm the advancement path, then reconcile the `content` value in `dungeon_delve_objs.json` per the Task 2 run-time note.

- [ ] **Step 3: Update `CLAUDE.md` Trial Dungeon section**

Add a short note under the Trial Dungeon section recording that city bounty boards now serve dungeon-themed FDP bounties (slay/boss/haul/delve) via decree override, with delve milestones granted in `enterFloor`.

```bash
git add CLAUDE.md
git commit -m "docs: record dungeon FDP bounties in CLAUDE.md"
```

---

## Self-Review Notes

- **Spec coverage:** §4.1 pools → Tasks 1–2; §4.2 FDP caps → Task 4; §4.3 decree override → Task 3; §4.4 advancements+hook → Tasks 2, 5, 6; §6 tests → Tasks 1–3, 5. All covered.
- **Type consistency:** `DungeonBountyMilestones.reached(int)`, `thresholds()`, `advancementId(int)` used identically in Task 5 (def) and Task 6 (call). Advancement ids `statmod:dungeon/delve_{10,25,50,100}` identical across Task 2 (files), Task 2 (delve pool), Task 5 (helper), Task 6 (award).
- **Known run-time unknowns (flagged, not placeholders):** exact `criteria` `content` form (Task 2 note); Bountiful skipping absent modded entity ids (spec §8 risk 1) — vanilla baseline guarantees a generable bounty regardless.
