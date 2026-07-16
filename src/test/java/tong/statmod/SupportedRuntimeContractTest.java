package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SupportedRuntimeContractTest {
    @Test
    void recordsExactPlatformAndRequiredProviderVersions() throws IOException {
        String record = Files.readString(Path.of(
                "docs/compatibility/forge-1.20.1-supported-runtime.md"));
        String attributes = Files.readString(Path.of(
                "docs/compatibility/stat-attribute-provider-matrix.md"));

        assertTrue(record.contains("Minecraft | 1.20.1 | verified"));
        assertTrue(record.contains("Forge | 47.4.10 | verified"));
        assertTrue(record.contains("Java | 17 | verified"));
        assertTrue(record.contains("Iron's Spells 'n Spellbooks | 3.16.2 | required"));
        assertTrue(record.contains("Epic Fight | 20.14.17 | required"));
        assertTrue(record.contains("Pufferfish's Attributes | 0.8.2 | required"));
        assertTrue(record.contains("Tensura | excluded | unsupported"));
        assertTrue(attributes.contains("minecraft:generic.attack_speed"));
        assertTrue(attributes.contains("epicfight:offhand_attack_speed"));
        assertTrue(attributes.contains("minecraft:generic.movement_speed"));
        assertTrue(attributes.contains("puffish_attributes:sprinting_speed"));
        assertTrue(attributes.contains("irons_spellbooks:spell_power"));
        assertTrue(attributes.contains("irons_spellbooks:max_mana"));
        assertTrue(attributes.contains("irons_spellbooks:spell_resist"));
        assertTrue(record.contains("39 automatic perks"));
        assertTrue(record.contains("12 classified combat perks"));
        assertTrue(record.contains("6 hunter perception perks"));
        assertTrue(record.contains("protocol 7"));
        assertTrue(record.contains("5% classified damage per milestone"));
        assertTrue(record.contains("2 physical-reduction percentage points per milestone"));
        assertTrue(record.contains("personal marked-prey contour"));
        assertTrue(record.contains("crouched personal threat scan"));
        assertTrue(record.contains("no damage, dodge, loot, or global glowing state"));
        assertTrue(record.contains("levels 25, 50, and 75"));
        assertTrue(record.contains("no tree, perk points, purchases, respecs, or affinities"));
        assertTrue(record.contains("Pufferfish's Attributes remains an attribute provider"));
        assertTrue(record.contains("active-perk list in the native `P` screen"));
    }
}
