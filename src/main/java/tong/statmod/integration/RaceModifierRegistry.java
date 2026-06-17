package tong.statmod.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RaceModifierRegistry {
    private static final Map<String, RaceData> REGISTRY = new HashMap<>();

    private RaceModifierRegistry() {}

    public static RaceData get(String raceId) {
        return REGISTRY.getOrDefault(raceId, RaceData.EMPTY);
    }

    public static boolean hasRaceData(String raceId) {
        return REGISTRY.containsKey(raceId);
    }

    static {
        // ── HUMAN family ──────────────────────────────────
        put("tensura:human",            List.of(), List.of());
        put("tensura:enlightened_human",List.of(), List.of());
        put("tensura:human_saint",      List.of(
                mod(22, 1, 1.0),  // +WILLPOWER
                mod(13, 1, 1.0),  // +CASTING_SPEED
                mod(14, 1, 1.0)   // +MANA_POOL
        ), List.of());
        put("tensura:divine_human",     List.of(
                mod(22, 2, 1.0),  // +WILLPOWER
                mod(15, 1, 1.0),  // +ERUDITION
                mod(13, 1, 1.0),  // +CASTING_SPEED
                mod(14, 1, 1.0)   // +MANA_POOL
        ), List.of());

        // ── OGRE family (warriors) ────────────────────────
        put("tensura:ogre",              List.of(
                mod(0, 2, 1.2),   // +BRUTE_FORCE, +20% XP
                mod(4, 1, 1.1)    // +PHYSICAL_RESISTANCE
        ), List.of());
        put("tensura:kijin",             List.of(
                mod(0, 3, 1.3),
                mod(1, 2, 1.2),   // +BLADE_TECHNIQUE
                mod(4, 2, 1.1),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:enlightened_ogre",  List.of(
                mod(0, 4, 1.4),
                mod(1, 3, 1.3),
                mod(4, 3, 1.2)
        ), List.of());
        put("tensura:mystic_oni",        List.of(
                mod(0, 5, 1.5),
                mod(1, 4, 1.4),
                mod(4, 4, 1.3),
                mod(7, 2, 1.2),   // +ARCANE_POWER
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:wicked_oni",        List.of(
                mod(0, 6, 1.6),
                mod(1, 5, 1.5),
                mod(4, 5, 1.4),
                mod(21, 3, 1.3)   // +INTIMIDATION
        ), List.of());
        put("tensura:spirit_oni",        List.of(
                mod(0, 7, 1.7),
                mod(1, 6, 1.6),
                mod(4, 6, 1.5),
                mod(7, 4, 1.3),
                mod(11, 2, 1.2),  // +AIR_AFFINITY
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:death_oni",         List.of(
                mod(0, 8, 1.8),
                mod(1, 7, 1.7),
                mod(4, 7, 1.6),
                mod(21, 5, 1.4),
                mod(22, 3, 1.3),  // +WILLPOWER
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:divine_oni",        List.of(
                mod(0, 9, 2.0),
                mod(1, 8, 1.8),
                mod(4, 8, 1.7),
                mod(21, 6, 1.5),
                mod(22, 4, 1.4),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:divine_fighter",    List.of(
                mod(0, 10, 2.5),
                mod(1, 9, 2.0),
                mod(4, 9, 2.0),
                mod(21, 7, 1.8),
                mod(22, 5, 1.5)
        ), List.of());

        // ── ELF family (magic users) ──────────────────────
        put("tensura:elf",               List.of(
                mod(6, 2, 1.2),
                mod(12, 1, 1.1),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:enlightened_elf",   List.of(
                mod(6, 3, 1.3),
                mod(12, 2, 1.2),
                mod(7, 1, 1.1),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:elf_saint",         List.of(
                mod(6, 4, 1.4),
                mod(12, 3, 1.3),
                mod(7, 2, 1.2),
                mod(15, 2, 1.2),  // +ERUDITION
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:divine_elf",        List.of(
                mod(6, 5, 1.5),
                mod(12, 4, 1.4),
                mod(7, 3, 1.3),
                mod(15, 3, 1.3),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());

        // ── DWARF family (crafters) ───────────────────────
        put("tensura:dwarf",             List.of(
                mod(18, 2, 1.3),  // +FORGING
                mod(4, 1, 1.1)
        ), List.of());
        put("tensura:enlightened_dwarf", List.of(
                mod(18, 3, 1.5),
                mod(4, 2, 1.2),
                mod(19, 1, 1.1)   // +COOKING
        ), List.of());
        put("tensura:dwarf_saint",       List.of(
                mod(18, 4, 1.7),
                mod(4, 3, 1.3),
                mod(19, 2, 1.2),
                mod(20, 2, 1.2)   // +ALCHEMY
        ), List.of());
        put("tensura:divine_dwarf",      List.of(
                mod(18, 5, 2.0),
                mod(4, 4, 1.4),
                mod(19, 3, 1.3),
                mod(20, 3, 1.3)
        ), List.of());

        // ── SLIME family (tanks) ──────────────────────────
        put("tensura:slime",             List.of(
                mod(5, 2, 1.2),   // +PHYSICAL_ENDURANCE
                mod(22, 1, 1.1),  // +WILLPOWER
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:metal_slime",       List.of(
                mod(5, 3, 1.3),
                mod(22, 2, 1.2),
                mod(4, 2, 1.2),   // +PHYSICAL_RESISTANCE
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:demon_slime",       List.of(
                mod(5, 4, 1.4),
                mod(22, 3, 1.3),
                mod(4, 3, 1.3),
                mod(7, 2, 1.2),   // +ARCANE_POWER
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:god_slime",         List.of(
                mod(5, 5, 1.5),
                mod(22, 4, 1.4),
                mod(4, 4, 1.4),
                mod(7, 3, 1.3),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());

        // ── LIZARDMAN / DRAGON family ─────────────────────
        put("tensura:lizardman",         List.of(
                mod(0, 2, 1.2),
                mod(4, 1, 1.1),
                mod(10, 1, 1.1)   // +FIRE_AFFINITY
        ), List.of());
        put("tensura:dragonewt",         List.of(
                mod(0, 3, 1.3),
                mod(4, 2, 1.2),
                mod(10, 2, 1.2),
                mod(5, 1, 1.1),   // +PHYSICAL_ENDURANCE
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:true_dragonewt",    List.of(
                mod(0, 4, 1.4),
                mod(4, 3, 1.3),
                mod(10, 3, 1.3),
                mod(5, 2, 1.2),
                mod(11, 1, 1.1),  // +AIR_AFFINITY
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:divine_dragon",     List.of(
                mod(0, 5, 1.5),
                mod(4, 4, 1.4),
                mod(10, 4, 1.4),
                mod(5, 3, 1.3),
                mod(11, 2, 1.2),
                mod(22, 2, 1.2),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());

        // ── BEASTFOLK family (scouts) ─────────────────────
        put("tensura:beastfolk",         List.of(
                mod(16, 2, 1.3),  // +TRACKING
                mod(17, 1, 1.2),  // +KEEN_SENSES
                mod(3, 1, 1.1)    // +AGILITY
        ), List.of());
        put("tensura:beast_lord",        List.of(
                mod(16, 3, 1.5),
                mod(17, 2, 1.3),
                mod(3, 2, 1.2),
                mod(0, 1, 1.1)
        ), List.of());
        put("tensura:spirit_beast",      List.of(
                mod(16, 4, 1.7),
                mod(17, 3, 1.5),
                mod(3, 3, 1.3),
                mod(0, 2, 1.2),
                mod(7, 1, 1.1)
        ), List.of());
        put("tensura:divine_beast",      List.of(
                mod(16, 5, 2.0),
                mod(17, 4, 1.7),
                mod(3, 4, 1.4),
                mod(0, 3, 1.3),
                mod(7, 2, 1.2)
        ), List.of());

        // ── HARPY family (swift aerial) ───────────────────
        put("tensura:harpy",             List.of(
                mod(11, 2, 1.3),  // +AIR_AFFINITY
                mod(3, 2, 1.2)    // +AGILITY
        ), List.of());
        put("tensura:harpy_queen",       List.of(
                mod(11, 3, 1.5),
                mod(3, 3, 1.3),
                mod(6, 1, 1.1)    // +PRECISION
        ), List.of());
        put("tensura:spirit_bird",       List.of(
                mod(11, 4, 1.7),
                mod(3, 4, 1.4),
                mod(6, 2, 1.2),
                mod(7, 1, 1.1)
        ), List.of());
        put("tensura:divine_bird",       List.of(
                mod(11, 5, 2.0),
                mod(3, 5, 1.5),
                mod(6, 3, 1.3),
                mod(7, 2, 1.2)
        ), List.of());

        // ── GOBLIN family (swift daggers) ─────────────────
        put("tensura:goblin",            List.of(
                mod(2, 2, 1.2),   // +RAPIDITE
                mod(3, 1, 1.2)    // +AGILITY
        ), List.of());
        put("tensura:hobgoblin",         List.of(
                mod(2, 3, 1.3),
                mod(3, 2, 1.3),
                mod(1, 1, 1.1)
        ), List.of());
        put("tensura:enlightened_hobgoblin", List.of(
                mod(2, 4, 1.4),
                mod(3, 3, 1.4),
                mod(1, 2, 1.2),
                mod(0, 1, 1.1)
        ), List.of());
        put("tensura:hobgoblin_saint",   List.of(
                mod(2, 5, 1.5),
                mod(3, 4, 1.5),
                mod(1, 3, 1.3),
                mod(0, 2, 1.2)
        ), List.of());

        // ── GIANT family (tanks) ──────────────────────────
        put("tensura:giant",             List.of(
                mod(0, 3, 1.3),
                mod(5, 2, 1.2)
        ), List.of());
        put("tensura:ancient_giant",     List.of(
                mod(0, 4, 1.4),
                mod(5, 3, 1.3),
                mod(4, 2, 1.2)
        ), List.of());
        put("tensura:divine_giant",      List.of(
                mod(0, 5, 1.5),
                mod(5, 4, 1.4),
                mod(4, 3, 1.3),
                mod(22, 2, 1.2)
        ), List.of());

        // ── ORC family (brute endurance) ──────────────────
        put("tensura:orc",               List.of(
                mod(5, 2, 1.3),
                mod(4, 1, 1.1)
        ), List.of());
        put("tensura:high_orc",          List.of(
                mod(5, 3, 1.4),
                mod(4, 2, 1.2),
                mod(0, 1, 1.1)
        ), List.of());
        put("tensura:spirit_boar",       List.of(
                mod(5, 4, 1.5),
                mod(4, 3, 1.3),
                mod(0, 2, 1.2)
        ), List.of());
        put("tensura:orc_lord",          List.of(
                mod(5, 5, 1.6),
                mod(4, 4, 1.4),
                mod(0, 3, 1.3),
                mod(21, 2, 1.2)   // +INTIMIDATION
        ), List.of());
        put("tensura:orc_disaster",      List.of(
                mod(5, 6, 1.7),
                mod(4, 5, 1.5),
                mod(0, 4, 1.4),
                mod(21, 3, 1.3),
                mod(22, 2, 1.2)
        ), List.of());
        put("tensura:divine_boar",       List.of(
                mod(5, 7, 1.8),
                mod(4, 6, 1.6),
                mod(0, 5, 1.5),
                mod(21, 4, 1.4),
                mod(22, 3, 1.3)
        ), List.of());

        // ── MERFOLK family (water) ────────────────────────
        put("tensura:merfolk",           List.of(
                mod(8, 2, 1.3),   // +WATER_AFFINITY
                mod(5, 1, 1.1)
        ), List.of());
        put("tensura:enlightened_merfolk", List.of(
                mod(8, 3, 1.5),
                mod(5, 2, 1.2),
                mod(7, 1, 1.1)
        ), List.of());
        put("tensura:merfolk_saint",     List.of(
                mod(8, 4, 1.7),
                mod(5, 3, 1.3),
                mod(7, 2, 1.2),
                mod(12, 1, 1.1)
        ), List.of());
        put("tensura:divine_fish",       List.of(
                mod(8, 5, 2.0),
                mod(5, 4, 1.4),
                mod(7, 3, 1.3),
                mod(12, 2, 1.2)
        ), List.of());

        // ── VAMPIRE family (dark rogues) ──────────────────
        put("tensura:ghoul",             List.of(
                mod(21, 2, 1.2),  // +INTIMIDATION
                mod(2, 1, 1.1)
        ), List.of());
        put("tensura:vampire",           List.of(
                mod(21, 3, 1.3),
                mod(2, 2, 1.2),
                mod(22, 1, 1.1),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:vampire_overcomer", List.of(
                mod(21, 4, 1.4),
                mod(2, 3, 1.3),
                mod(22, 2, 1.2),
                mod(7, 1, 1.1),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:vampire_lord",      List.of(
                mod(21, 5, 1.5),
                mod(2, 4, 1.4),
                mod(22, 3, 1.3),
                mod(7, 2, 1.2),
                mod(21, 6, 1.5),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:divine_vampire",    List.of(
                mod(21, 7, 1.7),
                mod(2, 5, 1.5),
                mod(22, 4, 1.4),
                mod(7, 3, 1.3),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());

        // ── DAEMON family (dark magic) ────────────────────
        put("tensura:lesser_daemon",     List.of(
                mod(7, 2, 1.3),   // +ARCANE_POWER
                mod(21, 1, 1.2),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:greater_daemon",    List.of(
                mod(7, 3, 1.5),
                mod(21, 2, 1.3),
                mod(22, 1, 1.1),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:arch_daemon",       List.of(
                mod(7, 4, 1.7),
                mod(21, 3, 1.4),
                mod(22, 2, 1.2),
                mod(12, 2, 1.2),  // +MAGIC_RESISTANCE
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:daemon_lord",       List.of(
                mod(7, 5, 2.0),
                mod(21, 4, 1.5),
                mod(22, 3, 1.3),
                mod(12, 3, 1.3),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());
        put("tensura:devil_lord",        List.of(
                mod(7, 6, 2.5),
                mod(21, 5, 1.7),
                mod(22, 4, 1.4),
                mod(12, 4, 1.4),
                mod(13, 1, 1.0),
                mod(14, 1, 1.0)
        ), List.of());

        // ── WIGHT / UNDEAD family ─────────────────────────
        put("tensura:wight",             List.of(
                mod(22, 2, 1.3),  // +WILLPOWER
                mod(12, 1, 1.1)
        ), List.of());
        put("tensura:wight_king",        List.of(
                mod(22, 3, 1.4),
                mod(12, 2, 1.2),
                mod(21, 1, 1.1)
        ), List.of());
        put("tensura:spirit_skeleton",   List.of(
                mod(22, 4, 1.5),
                mod(12, 3, 1.3),
                mod(21, 2, 1.2)
        ), List.of());
        put("tensura:divine_skeleton",   List.of(
                mod(22, 5, 1.7),
                mod(12, 4, 1.4),
                mod(21, 3, 1.3),
                mod(7, 1, 1.1)
        ), List.of());
    }

    private static RaceModifier mod(int stat, int bonus, double xpMul) {
        return new RaceModifier(stat, bonus, xpMul);
    }

    private static void put(String raceId, List<RaceModifier> modifiers, List<Integer> exclusivePerks) {
        REGISTRY.put(raceId, new RaceData(modifiers, exclusivePerks));
    }

    // ── special race groups ───────────────────────────────

    public static boolean isHumanoid(String raceId) {
        String path = racePath(raceId);
        return isRaceFamily(path, "human")
                || isRaceFamily(path, "elf")
                || isRaceFamily(path, "dwarf");
    }

    public static boolean isMonster(String raceId) {
        return hasRaceData(raceId) && !isHumanoid(raceId);
    }

    private static String racePath(String raceId) {
        if (raceId == null) {
            return "";
        }
        int separator = raceId.indexOf(':');
        return separator >= 0 ? raceId.substring(separator + 1) : raceId;
    }

    private static boolean isRaceFamily(String path, String family) {
        return path.equals(family)
                || path.startsWith(family + "_")
                || path.endsWith("_" + family)
                || path.contains("_" + family + "_");
    }
}
