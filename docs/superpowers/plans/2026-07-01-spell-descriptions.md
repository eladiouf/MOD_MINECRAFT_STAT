# Spell Descriptions for Puffish Tree — Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add rich spell descriptions from mod JARs to the Puffish magic tree signature nodes.

**Architecture:** `SpellDescriptionProvider` extracts `spell.<ns>.<name>.guide` entries from JARs in `libs/` at class init time, plus a hardcoded override map for mods without guide entries (Tensura, spells_gone_wrong). `PuffishMagicTreeBuilder.baseDescriptionFor()` uses it to replace the formulaic "Learn X." with actual spell description text.

**Tech Stack:** Java ZIP/IO for JAR extraction, regex for JSON key parsing, existing PuffishMagicTreeBuilder infrastructure.

---

### Task 1: SpellDescriptionProvider

**Files:**
- Create: `src/main/java/tong/statmod/integration/puffish/SpellDescriptionProvider.java`

- [ ] **Step 1: Create the class skeleton**

```java
package tong.statmod.integration.puffish;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class SpellDescriptionProvider {
    private static final Map<String, String> JAR_DESCRIPTIONS = new LinkedHashMap<>();
    private static final Map<String, String> OVERRIDES = new LinkedHashMap<>();
    private static boolean loaded = false;

    private static final Pattern GUIDE_PATTERN = Pattern.compile(
        "\"spell\\.([a-z0-9_\\-]+)\\.([a-z0-9_]+)\\.guide\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    );
}
```

- [ ] **Step 2: Add loading logic**

```java
    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Path libsDir = Path.of("libs");
            if (Files.isDirectory(libsDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(libsDir, "*.jar")) {
                    for (Path jar : stream) loadFromJar(jar);
                }
            }
        } catch (IOException e) {
            // libs not available — skip JAR extraction
        }
        loadOverrides();
    }

    private static void loadFromJar(Path jarPath) {
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry langEntry = zip.stream()
                .filter(e -> e.getName().matches("assets/[^/]+/lang/en_us\\.json"))
                .findFirst().orElse(null);
            if (langEntry == null) return;
            String json = new String(zip.getInputStream(langEntry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = GUIDE_PATTERN.matcher(json);
            while (m.find()) {
                JAR_DESCRIPTIONS.put(m.group(1) + ":" + m.group(2), unescapeJson(m.group(3)));
            }
        } catch (IOException e) {
            // skip corrupt JAR
        }
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
    }
```

- [ ] **Step 3: Add override map**

```java
    private static void loadOverrides() {
        // spells_gone_wrong — no guide entries
        OVERRIDES.put("spells_gone_wrong:shotgun_creeper", "Summon a volley of creeping explosives that blast your foes.");
        OVERRIDES.put("spells_gone_wrong:nucreeper_strike", "Call down a catastrophic creeper strike from the heavens.");

        // Tensura spells — no guide entries
        OVERRIDES.put("tensura:fire_bolt", "Launch a bolt of flame toward your target.");
        OVERRIDES.put("tensura:fire_ball", "Hurl a blazing sphere that erupts on impact.");
        OVERRIDES.put("tensura:fire_aspectual", "Imbue your weapon with searing fire aspect.");
        OVERRIDES.put("tensura:fire_lance", "Conjure a spear of pure fire and hurl it forward.");
        OVERRIDES.put("tensura:fire_wall", "Raise a wall of roaring flames before you.");
        OVERRIDES.put("tensura:fire_storm", "Summon a blazing storm that scorches the battlefield.");
        OVERRIDES.put("tensura:hellfire", "Unleash infernal flames that consume all in their path.");

        OVERRIDES.put("tensura:healing", "Channel holy energy to mend your wounds.");
        OVERRIDES.put("tensura:recovery", "Accelerate natural regeneration and restore vitality.");
        OVERRIDES.put("tensura:antidote", "Purge poisons and harmful effects from your body.");
        OVERRIDES.put("tensura:water_jail", "Bind your foe in a prison of churning water.");
        OVERRIDES.put("tensura:healing_rain", "Call down a restorative downpour that heals allies.");
        OVERRIDES.put("tensura:full_recovery", "Fully restore health and remove all debilitations.");

        OVERRIDES.put("tensura:wind_gust", "Unleash a powerful gust that pushes enemies away.");
        OVERRIDES.put("tensura:wind_protection", "Shroud yourself in a veil of wind that deflects projectiles.");
        OVERRIDES.put("tensura:wind_blade", "Fire a razor-sharp blade of compressed air.");
        OVERRIDES.put("tensura:tornado_blade", "Summon a swirling vortex blade that rends all in its path.");
        OVERRIDES.put("tensura:wind_cutter", "Slice through the air with a devastating wind cutter.");
        OVERRIDES.put("tensura:lightning_lance", "Call down a spear of lightning from the sky.");
        OVERRIDES.put("tensura:aerial_blade", "Rain down blades of wind from above.");

        OVERRIDES.put("tensura:earth_wall", "Raise a barrier of stone from the ground.");
        OVERRIDES.put("tensura:earth_lock", "Anchors a target in place with earthen bindings.");
        OVERRIDES.put("tensura:earth_spikes", "Jagged stone spikes erupt from the ground beneath your enemies.");
        OVERRIDES.put("tensura:earth_jail", "Imprison your target within a tomb of solid rock.");
        OVERRIDES.put("tensura:earth_storm", "Summon a devastating storm of stone and debris.");
        OVERRIDES.put("tensura:magma_surge", "The ground splits open, releasing a surge of molten magma.");

        OVERRIDES.put("tensura:magic_wall", "Erect a shimmering barrier of pure magic.");
        OVERRIDES.put("tensura:barrier", "Wrap yourself in a protective magical barrier.");
        OVERRIDES.put("tensura:reinforced_barrier", "Fortify your barrier with additional magical layers.");
        OVERRIDES.put("tensura:anti_shock_area", "Create a zone that nullifies shockwave damage.");
        OVERRIDES.put("tensura:magic_barrier", "Erect a powerful magical ward against all forms of attack.");
        OVERRIDES.put("tensura:healthcare", "Bathe an ally in restorative light, healing over time.");
        OVERRIDES.put("tensura:multilayer_barrier", "Weave multiple barrier layers for unmatched protection.");
        OVERRIDES.put("tensura:anti_magic_area", "Create a field that suppresses all magic within.");

        OVERRIDES.put("tensura:lighten", "Reduce your weight, allowing higher jumps and slower falls.");
        OVERRIDES.put("tensura:float", "Defy gravity and levitate above the ground.");
        OVERRIDES.put("tensura:escape", "Warp away from danger in the blink of an eye.");
        OVERRIDES.put("tensura:warp_portal", "Open a portal that connects two locations.");
        OVERRIDES.put("tensura:teleport", "Instantly transport yourself to a targeted location.");
        OVERRIDES.put("tensura:gate", "Open a massive gate that bridges vast distances.");

        OVERRIDES.put("tensura:analyze", "Magically assess your target to reveal its strengths and weaknesses.");
        OVERRIDES.put("tensura:search_enemy", "Scan the area to reveal hidden or invisible enemies.");
        OVERRIDES.put("tensura:doppelganger", "Create a magical duplicate of yourself to confuse foes.");
        OVERRIDES.put("tensura:magic_aura", "Surround yourself with an aura of magical energy.");
        OVERRIDES.put("tensura:magic_bullet", "Fire a condensed bolt of raw magic.");
        OVERRIDES.put("tensura:clairvoyance", "Pierce the veil of magic to reveal hidden truths.");
        OVERRIDES.put("tensura:spatial_storage", "Access a pocket dimension to store your belongings.");
        OVERRIDES.put("tensura:magic_space_transform", "Alter the space around you, warping reality itself.");
        OVERRIDES.put("tensura:dimension_cutter", "Slice through the fabric of dimensions.");
        OVERRIDES.put("tensura:maximum_magic_bullet", "Unleash the ultimate condensed magical projectile.");

        OVERRIDES.put("tensura:darkness", "Shroud the area in impenetrable magical darkness.");
        OVERRIDES.put("tensura:shadow_bind", "Bind your target in place with living shadows.");
        OVERRIDES.put("tensura:dark_cube", "Create a cube of solid darkness to crush your enemies.");
        OVERRIDES.put("tensura:curse_bind", "Shackle your foe with a debilitating curse.");
        OVERRIDES.put("tensura:darkness_cannon", "Fire a devastating cannonball of condensed darkness.");
        OVERRIDES.put("tensura:true_darkness", "Unleash the primordial darkness that consumes all light.");
    }
```

- [ ] **Step 4: Add public API**

```java
    public static String get(String spellId) {
        ensureLoaded();
        String ov = OVERRIDES.get(spellId);
        if (ov != null) return ov;
        return JAR_DESCRIPTIONS.get(spellId);
    }

    public static boolean has(String spellId) {
        ensureLoaded();
        return OVERRIDES.containsKey(spellId) || JAR_DESCRIPTIONS.containsKey(spellId);
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL (new class compiles)

---

### Task 2: Tests for SpellDescriptionProvider

**Files:**
- Create: `src/test/java/tong/statmod/integration/puffish/SpellDescriptionProviderTest.java`

- [ ] **Step 1: Create the test class**

```java
package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpellDescriptionProviderTest {

    @Test
    void ironsSpellbooksGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("irons_spellbooks:fireball");
        assertNotNull(desc, "expected fireball guide from irons_spellbooks JAR");
        assertTrue(desc.contains("explod") || desc.contains("ball of fire"),
            "description should describe the spell, got: " + desc);
    }

    @Test
    void ironsSpellbooksHasAllGuides() {
        assertNotNull(SpellDescriptionProvider.get("irons_spellbooks:firebolt"));
        assertNotNull(SpellDescriptionProvider.get("irons_spellbooks:blaze_storm"));
        assertNotNull(SpellDescriptionProvider.get("irons_spellbooks:burning_dash"));
    }

    @Test
    void windSpellbooksGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("wind_spellbooks:wind_jump");
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void legendarymageGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("legendarymage:pyromaniac");
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void gametechbcsGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("gametechbcs_spellbooks:nullflare");
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void override_takesPrecedenceOverJar() {
        String desc = SpellDescriptionProvider.get("spells_gone_wrong:shotgun_creeper");
        assertNotNull(desc);
        assertTrue(desc.contains("creeper") || desc.contains("explo"),
            "description should reference creepers, got: " + desc);
    }

    @Test
    void tensuraOverride_isProvided() {
        String desc = SpellDescriptionProvider.get("tensura:fire_bolt");
        assertNotNull(desc, "tensura spells should have override descriptions");
        assertTrue(desc.contains("flame") || desc.contains("fire") || desc.contains("bolt"),
            "description should describe the spell, got: " + desc);
    }

    @Test
    void missingSpell_returnsNull() {
        assertNull(SpellDescriptionProvider.get("nonexistent:missing_spell"));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew test --tests "tong.statmod.integration.puffish.SpellDescriptionProviderTest"`
Expected: All tests pass (or some fail if JAR paths differ — fix as needed)

---

### Task 3: Update PuffishMagicTreeBuilder.baseDescriptionFor()

**Files:**
- Modify: `src/main/java/tong/statmod/integration/puffish/PuffishMagicTreeBuilder.java` (around line 470)

- [ ] **Step 1: Update the SIGNATURE_SPELL case in baseDescriptionFor()**

Find and replace:

```java
            case SIGNATURE_SPELL -> "Learn " + titleFor(node) + ".";
```

With:

```java
            case SIGNATURE_SPELL -> {
                // Try to get a rich description from JAR guides or overrides
                String spellDesc = null;
                for (String spellId : node.learnedSpells()) {
                    spellDesc = SpellDescriptionProvider.get(spellId);
                    if (spellDesc != null) break;
                }
                yield spellDesc != null ? spellDesc : "Learn " + titleFor(node) + ".";
            }
```

And add the import at the top of the file (check existing imports):

```java
// Check if import is needed — SpellDescriptionProvider is in the same package
// No import needed since both are in tong.statmod.integration.puffish
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 4: Regenerate JSON and verify

**Files:**
- Modify: `src/main/resources/data/statmod/puffish_skills/categories/statmod_magic/definitions.json`

- [ ] **Step 1: Re-run tests**

Run: `./gradlew check`
Expected: All 439+ tests pass

- [ ] **Step 2: Regenerate Puffish JSON**

Run: `./gradlew runClient` (or just verify the JSON regeneration logic compiles correctly)

The JSON files are regenerated at runtime via `/regenPuffishJson` command, but in tests we verify the builder output. The existing `PuffishMagicResourceConsistencyTest` already validates the generated JSON structure.

- [ ] **Step 3: Spot-check a generated description**

Read: Look at signature entries in `definitions.json` to verify descriptions changed from "Learn Fireball." to actual guide text.

Run: `Select-String -Pattern '"description"' src/main/resources/data/statmod/puffish_skills/categories/statmod_magic/definitions.json | Select-Object -First 10`

Expected: Fire signature entries should have rich descriptions (e.g., contain "shoot", "fire", "projectile", etc.)
