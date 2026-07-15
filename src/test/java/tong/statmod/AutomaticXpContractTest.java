package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tong.statmod.progression.xp.StatXpAward;
import tong.statmod.progression.xp.XpAction;
import tong.statmod.progression.xp.XpActionKind;
import tong.statmod.progression.xp.XpRewardPolicy;
import tong.statmod.stats.StatType;

class AutomaticXpContractTest {
    private static final String CSV_HEADER = "addon,version,weapon_tags,melee_xp,combo_xp,"
            + "projectile_xp,crafting_xp,client_test,server_test,status,notes";

    @Test
    void documentsPublicTagsHonestStatusesAndBothTestProcedures() throws IOException {
        String guide = Files.readString(
                Path.of("docs/compatibility/epic-fight-addon-xp.md")).toLowerCase();

        for (String tag : List.of("statmod:heavy_weapons", "statmod:blade_weapons",
                "statmod:precision_weapons", "statmod:forgeable_equipment")) {
            assertTrue(guide.contains(tag));
        }
        assertTrue(guide.contains("\"replace\": false"));
        for (String status : List.of("untested", "compatible", "partial", "incompatible")) {
            assertTrue(guide.contains(status));
        }
        assertTrue(guide.contains("client"));
        assertTrue(guide.contains("dedicated server"));
        assertTrue(guide.contains("untested addon is not claimed compatible"));

        List<String> matrix = Files.readAllLines(
                Path.of("docs/compatibility/epic-fight-addon-matrix.csv"));
        assertFalse(matrix.isEmpty());
        assertEquals(CSV_HEADER, matrix.get(0));
        assertTrue(matrix.stream().anyMatch(line -> line.contains("EXAMPLE_DO_NOT_SHIP_AS_RESULT")
                && line.contains(",untested,")));
    }

    @Test
    void mainCodeHasNoOptionalModImports() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            String source = files.filter(path -> path.toString().endsWith(".java"))
                    .map(AutomaticXpContractTest::readUnchecked)
                    .collect(Collectors.joining("\n")).toLowerCase();
            assertFalse(hasOptionalModImport(source));
        }
    }

    @Test
    void rewardPolicyActivatesExactlyFourteenNonMagicalStats() {
        List<XpAction> actions = List.of(
                XpAction.damage(XpActionKind.MELEE_HEAVY, 5),
                XpAction.damage(XpActionKind.MELEE_BLADE, 5),
                XpAction.damage(XpActionKind.PROJECTILE, 5),
                XpAction.damage(XpActionKind.PHYSICAL_DAMAGE_RECEIVED, 5),
                XpAction.damage(XpActionKind.SHIELD_BLOCKED, 5),
                XpAction.damage(XpActionKind.WILLPOWER_SURVIVAL, 5),
                XpAction.combo(3), XpAction.landing(8), XpAction.biome(),
                XpAction.kill(120, true), XpAction.forging(100, 1),
                XpAction.cooking(1), XpAction.alchemy(1, 0));
        Set<StatType> emitted = actions.stream().flatMap(action ->
                        XpRewardPolicy.awards(action).stream())
                .map(StatXpAward::stat).collect(Collectors.toCollection(
                        () -> EnumSet.noneOf(StatType.class)));

        Set<StatType> deferred = EnumSet.of(
                StatType.ARCANE_POWER, StatType.CASTING_SPEED, StatType.MANA_POOL,
                StatType.ERUDITION, StatType.MAGIC_RESISTANCE, StatType.FIRE_AFFINITY,
                StatType.WATER_AFFINITY, StatType.EARTH_AFFINITY, StatType.AIR_AFFINITY);
        assertEquals(14, emitted.size());
        assertTrue(emitted.stream().noneMatch(deferred::contains));
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean hasOptionalModImport(String source) {
        return source.lines()
                .map(String::strip)
                .filter(line -> line.startsWith("import "))
                .anyMatch(line -> line.contains("epicfight")
                        || line.contains("ironsspellbooks")
                        || line.contains("irons_spellbooks")
                        || line.contains("tensura")
                        || line.contains("parcool"));
    }
}
