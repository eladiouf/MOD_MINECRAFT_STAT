# Equipment-independent Spell Casting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every STAT Mod learned spell castable through Iron's Spells' active-cast and quick-cast keys with any held item while retaining staff attribute bonuses.

**Architecture:** Learned spells receive a stable virtual selection slot that always wins over duplicate equipment-provided options. A focused mixin maps that virtual slot to Iron's `SPELLBOOK` cast source; Iron's existing packets, server-side mana/cooldown validation, animations, and `ItemStack.EMPTY` learned-spell cast path remain authoritative.

**Tech Stack:** Java 21, NeoForge 21.1.232, Minecraft 1.21.1, Iron's Spells 3.16.1, Sponge Mixin, JUnit 5, Gradle 8.10.2.

## Global Constraints

- Use Iron's Spells' existing `Cast active spell`, quick-cast keys, packets, animations, mana, cooldown, and lifecycle.
- Apply equipment independence only to spell identifiers present in server-side `PlayerStatData.getLearnedSpells()`.
- Preserve native equipment requirements for spells not learned through STAT Mod.
- Do not add right-click casting or casting components to arbitrary items.
- Preserve all staff attributes and spell bonuses.
- Gate every new Iron's Spells mixin through `StatModMixinPlugin`.

---

### Task 1: Learned-spell virtual slot policy

**Files:**
- Create: `src/main/java/tong/statmod/integration/ironspells/LearnedSpellCastPolicy.java`
- Create: `src/test/java/tong/statmod/integration/ironspells/LearnedSpellCastPolicyTest.java`

**Interfaces:**
- Produces: `LearnedSpellCastPolicy.SLOT`, `isVirtualSlot(String)`, and `canUseVirtualSelection(String, String, Collection<String>)`.
- Consumes: only Java collections and strings so authorization rules remain unit-testable without a running game.

- [ ] **Step 1: Write the failing policy tests**

```java
package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LearnedSpellCastPolicyTest {
    @Test
    void virtualSlotHasStableNamespacedIdentity() {
        assertEquals("statmod_learned", LearnedSpellCastPolicy.SLOT);
        assertTrue(LearnedSpellCastPolicy.isVirtualSlot("statmod_learned"));
        assertFalse(LearnedSpellCastPolicy.isVirtualSlot("mainhand"));
        assertFalse(LearnedSpellCastPolicy.isVirtualSlot(null));
    }

    @Test
    void virtualSelectionRequiresServerLearnedSpell() {
        List<String> learned = List.of("irons_spellbooks:fireball");

        assertTrue(LearnedSpellCastPolicy.canUseVirtualSelection(
                "statmod_learned", "irons_spellbooks:fireball", learned));
        assertFalse(LearnedSpellCastPolicy.canUseVirtualSelection(
                "statmod_learned", "irons_spellbooks:teleport", learned));
        assertFalse(LearnedSpellCastPolicy.canUseVirtualSelection(
                "mainhand", "irons_spellbooks:fireball", learned));
        assertFalse(LearnedSpellCastPolicy.canUseVirtualSelection(
                "statmod_learned", "", learned));
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew.bat test --tests tong.statmod.integration.ironspells.LearnedSpellCastPolicyTest`

Expected: compilation fails because `LearnedSpellCastPolicy` does not exist.

- [ ] **Step 3: Implement the minimal pure policy**

```java
package tong.statmod.integration.ironspells;

import java.util.Collection;

public final class LearnedSpellCastPolicy {
    public static final String SLOT = "statmod_learned";

    private LearnedSpellCastPolicy() {
    }

    public static boolean isVirtualSlot(String slot) {
        return SLOT.equals(slot);
    }

    public static boolean canUseVirtualSelection(String slot, String spellId,
                                                  Collection<String> learnedSpellIds) {
        return isVirtualSlot(slot)
                && spellId != null
                && !spellId.isBlank()
                && learnedSpellIds != null
                && learnedSpellIds.contains(spellId);
    }
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `./gradlew.bat test --tests tong.statmod.integration.ironspells.LearnedSpellCastPolicyTest`

Expected: `BUILD SUCCESSFUL`, 2 tests pass.

- [ ] **Step 5: Commit the policy**

```powershell
git add src/main/java/tong/statmod/integration/ironspells/LearnedSpellCastPolicy.java src/test/java/tong/statmod/integration/ironspells/LearnedSpellCastPolicyTest.java
git commit -m "feat(magic): define learned spell virtual cast slot"
```

---

### Task 2: Make learned selections independent and authoritative

**Files:**
- Modify: `src/main/java/tong/statmod/mixin/SpellSelectionManagerMixin.java`
- Create: `src/test/java/tong/statmod/mixin/LearnedSpellSelectionMixinSourceTest.java`

**Interfaces:**
- Consumes: `LearnedSpellCastPolicy.SLOT` from Task 1 and server/client `PlayerStatData.getLearnedSpells()`.
- Produces: one virtual `SelectionOption` per registered learned spell, with the learned option replacing any duplicate sourced from held equipment.

- [ ] **Step 1: Write failing source-contract tests**

```java
package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnedSpellSelectionMixinSourceTest {
    private static final Path SOURCE = Path.of("src", "main", "java", "tong", "statmod",
            "mixin", "SpellSelectionManagerMixin.java");

    @Test
    void learnedSpellsUseStableVirtualSlotAndReplaceEquipmentDuplicates() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("LearnedSpellCastPolicy.SLOT"));
        assertTrue(source.contains("selectionOptionList.removeIf"));
        assertTrue(source.contains("existing.spellData.getSpell().equals(spell)"));
        assertTrue(source.contains("selectionOptionList.add(option)"));
        assertFalse(source.contains("addOrMergeSelectionOption(option)"));
        assertFalse(source.contains("new SelectionOption(\n                    spellData, \"statmod\""));
    }
}
```

- [ ] **Step 2: Run the source-contract test and verify RED**

Run: `./gradlew.bat test --tests tong.statmod.mixin.LearnedSpellSelectionMixinSourceTest`

Expected: test fails because the current mixin uses the old `"statmod"` slot and `addOrMergeSelectionOption`.

- [ ] **Step 3: Replace equipment duplicates with virtual learned options**

In `SpellSelectionManagerMixin`:

```java
import tong.statmod.integration.ironspells.LearnedSpellCastPolicy;
```

Remove the shadow for `addOrMergeSelectionOption`. Replace option construction and insertion inside the learned-spell loop with:

```java
selectionOptionList.removeIf(existing ->
        existing.spellData.getSpell().equals(spell));
SpellSelectionManager.SelectionOption option = new SpellSelectionManager.SelectionOption(
        spellData,
        LearnedSpellCastPolicy.SLOT,
        i,
        selectionOptionList.size());
selectionOptionList.add(option);
```

This ensures that a learned spell cannot silently revert to `mainhand` when the same spell is also inscribed on a staff. It does not remove native options for spells absent from `PlayerStatData`.

- [ ] **Step 4: Run focused selection tests**

Run: `./gradlew.bat test --tests tong.statmod.mixin.LearnedSpellSelectionMixinSourceTest --tests tong.statmod.integration.ironspells.LearnedSpellCastPolicyTest`

Expected: `BUILD SUCCESSFUL`, all focused tests pass.

- [ ] **Step 5: Commit virtual selection precedence**

```powershell
git add src/main/java/tong/statmod/mixin/SpellSelectionManagerMixin.java src/test/java/tong/statmod/mixin/LearnedSpellSelectionMixinSourceTest.java
git commit -m "fix(magic): keep learned spells independent of held equipment"
```

---

### Task 3: Map the virtual slot to Iron's spellbook cast source

**Files:**
- Create: `src/main/java/tong/statmod/mixin/IronLearnedSpellCastSourceMixin.java`
- Modify: `src/main/resources/statmod.mixins.json`
- Modify: `src/main/java/tong/statmod/mixin/StatModMixinPlugin.java`
- Modify: `src/test/java/tong/statmod/mixin/MixinOptionalCompatPluginTest.java`
- Create: `src/test/java/tong/statmod/mixin/IronLearnedSpellCastSourceMixinTest.java`

**Interfaces:**
- Consumes: `LearnedSpellCastPolicy.isVirtualSlot(String)` and Iron's `SelectionOption.slot`.
- Produces: `CastSource.SPELLBOOK` only for `statmod_learned`; all native slots continue through Iron's original `getCastSource()` implementation.

- [ ] **Step 1: Write failing mixin registration and source tests**

```java
package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IronLearnedSpellCastSourceMixinTest {
    @Test
    void virtualLearnedSlotReturnsSpellbookCastSource() throws Exception {
        Path source = Path.of("src", "main", "java", "tong", "statmod", "mixin",
                "IronLearnedSpellCastSourceMixin.java");
        String text = Files.readString(source);

        assertTrue(text.contains("LearnedSpellCastPolicy.isVirtualSlot(this.slot)"));
        assertTrue(text.contains("cir.setReturnValue(CastSource.SPELLBOOK)"));
    }

    @Test
    void castSourceMixinIsRegistered() throws Exception {
        String config = Files.readString(Path.of("src", "main", "resources", "statmod.mixins.json"));
        String plugin = Files.readString(Path.of("src", "main", "java", "tong", "statmod",
                "mixin", "StatModMixinPlugin.java"));

        assertTrue(config.contains("\"IronLearnedSpellCastSourceMixin\""));
        assertTrue(plugin.contains("Map.entry(\"IronLearnedSpellCastSourceMixin\", \"irons_spellbooks\")"));
    }
}
```

Add the same `Map.entry` assertion to the `optionalMixins` map in `MixinOptionalCompatPluginTest`.

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew.bat test --tests tong.statmod.mixin.IronLearnedSpellCastSourceMixinTest --tests tong.statmod.mixin.MixinOptionalCompatPluginTest`

Expected: tests fail because the mixin source and registrations do not exist.

- [ ] **Step 3: Implement the cast-source mixin**

```java
package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.ironspells.LearnedSpellCastPolicy;

@Mixin(SpellSelectionManager.SelectionOption.class)
public class IronLearnedSpellCastSourceMixin {
    @Shadow
    public String slot;

    @Inject(method = "getCastSource", at = @At("HEAD"), cancellable = true)
    private void statmod$useSpellbookSourceForLearnedSpell(
            CallbackInfoReturnable<CastSource> cir) {
        if (LearnedSpellCastPolicy.isVirtualSlot(this.slot)) {
            cir.setReturnValue(CastSource.SPELLBOOK);
        }
    }
}
```

Add `IronLearnedSpellCastSourceMixin` to the `mixins` array in `statmod.mixins.json` and map it to `irons_spellbooks` in `StatModMixinPlugin.OPTIONAL_MIXINS`.

- [ ] **Step 4: Run focused mixin tests and compile**

Run: `./gradlew.bat test --tests tong.statmod.mixin.IronLearnedSpellCastSourceMixinTest --tests tong.statmod.mixin.MixinOptionalCompatPluginTest --tests tong.statmod.mixin.LearnedSpellSelectionMixinSourceTest`

Expected: `BUILD SUCCESSFUL`; the mixin compiles against Iron's Spells 3.16.1 and all focused tests pass.

- [ ] **Step 5: Commit cast-source behavior**

```powershell
git add src/main/java/tong/statmod/mixin/IronLearnedSpellCastSourceMixin.java src/main/resources/statmod.mixins.json src/main/java/tong/statmod/mixin/StatModMixinPlugin.java src/test/java/tong/statmod/mixin/MixinOptionalCompatPluginTest.java src/test/java/tong/statmod/mixin/IronLearnedSpellCastSourceMixinTest.java
git commit -m "fix(magic): cast learned spells as virtual spellbook spells"
```

---

### Task 4: Regression verification and local deployment

**Files:**
- Modify only if required by a directly related failure from Tasks 1-3.
- Deploy: `build/libs/statmod-1.2.1.jar` to the configured client `mods` directories after successful verification.

**Interfaces:**
- Consumes: all behavior from Tasks 1-3.
- Produces: a tested JAR ready for a client smoke test; no marketplace publication or version bump is included.

- [ ] **Step 1: Run all automated tests**

Run: `./gradlew.bat test`

Expected: `BUILD SUCCESSFUL` with no failing test.

- [ ] **Step 2: Build the distributable JAR**

Run: `./gradlew.bat build`

Expected: `BUILD SUCCESSFUL` and `build/libs/statmod-1.2.1.jar` exists.

- [ ] **Step 3: Verify the JAR contents**

Run:

```powershell
jar tf build/libs/statmod-1.2.1.jar | Select-String 'LearnedSpellCastPolicy|IronLearnedSpellCastSourceMixin|SpellSelectionManagerMixin'
```

Expected: all three compiled classes are listed.

- [ ] **Step 4: Deploy to local clients without touching unrelated mods**

Run:

```powershell
$jar = Resolve-Path 'build/libs/statmod-1.2.1.jar'
$targets = @(
  "$env:APPDATA\.minecraft\mods",
  "$env:APPDATA\.minecraft\versions\test\mods",
  'C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-client\mods'
)
foreach ($target in $targets) {
  if (Test-Path $target) {
    Copy-Item -LiteralPath $jar -Destination (Join-Path $target 'statmod-1.2.1.jar') -Force
  }
}
```

Expected: each existing target contains the newly built JAR with the same SHA-256 hash as `build/libs/statmod-1.2.1.jar`.

- [ ] **Step 5: Perform the client smoke test**

Start the configured test client and verify:

1. Learn and select a spell from the STAT Mod tree.
2. Cast it with `Cast active spell` while holding nothing, a sword, food, and a block.
3. Trigger the same learned spell with a quick-cast key.
4. Equip a staff, cast again, and confirm its normal magic attributes still affect the spell.
5. Select a native staff-only spell, unequip the staff, and confirm that native spell remains unavailable unless it was separately learned through STAT Mod.

Expected: learned spells cast in all five held-item cases; native equipment-only behavior remains unchanged.

- [ ] **Step 6: Commit any verification-only adjustment, otherwise record no extra commit**

If a directly related correction was required, stage only its exact files and commit with:

```powershell
git commit -m "test(magic): complete equipment-independent cast verification"
```

If no correction was required, do not create an empty commit.
