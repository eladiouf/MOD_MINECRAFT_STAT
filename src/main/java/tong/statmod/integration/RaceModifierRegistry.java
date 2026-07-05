package tong.statmod.integration;

import tong.statmod.stats.StatType;

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
                mod(StatType.WILLPOWER, 1, 1.0),  // +WILLPOWER
                mod(StatType.CASTING_SPEED, 1, 1.0),  // +CASTING_SPEED
                mod(StatType.MANA_POOL, 1, 1.0)   // +MANA_POOL
        ), List.of());
        put("tensura:divine_human",     List.of(
                mod(StatType.WILLPOWER, 2, 1.0),  // +WILLPOWER
                mod(StatType.ERUDITION, 1, 1.0),  // +ERUDITION
                mod(StatType.CASTING_SPEED, 1, 1.0),  // +CASTING_SPEED
                mod(StatType.MANA_POOL, 1, 1.0)   // +MANA_POOL
        ), List.of());

        // ── OGRE family (warriors) ────────────────────────
        put("tensura:ogre",              List.of(
                mod(StatType.BRUTE_FORCE, 2, 1.2),   // +BRUTE_FORCE, +20% XP
                mod(StatType.PHYSICAL_RESISTANCE, 1, 1.1)    // +PHYSICAL_RESISTANCE
        ), List.of());
        put("tensura:kijin",             List.of(
                mod(StatType.BRUTE_FORCE, 3, 1.3),
                mod(StatType.BLADE_TECHNIQUE, 2, 1.2),   // +BLADE_TECHNIQUE
                mod(StatType.PHYSICAL_RESISTANCE, 2, 1.1),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:enlightened_ogre",  List.of(
                mod(StatType.BRUTE_FORCE, 4, 1.4),
                mod(StatType.BLADE_TECHNIQUE, 3, 1.3),
                mod(StatType.PHYSICAL_RESISTANCE, 3, 1.2)
        ), List.of());
        put("tensura:mystic_oni",        List.of(
                mod(StatType.BRUTE_FORCE, 5, 1.5),
                mod(StatType.BLADE_TECHNIQUE, 4, 1.4),
                mod(StatType.PHYSICAL_RESISTANCE, 4, 1.3),
                mod(StatType.ARCANE_POWER, 2, 1.2),   // +ARCANE_POWER
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:wicked_oni",        List.of(
                mod(StatType.BRUTE_FORCE, 6, 1.6),
                mod(StatType.BLADE_TECHNIQUE, 5, 1.5),
                mod(StatType.PHYSICAL_RESISTANCE, 5, 1.4),
                mod(StatType.INTIMIDATION, 3, 1.3)   // +INTIMIDATION
        ), List.of());
        put("tensura:spirit_oni",        List.of(
                mod(StatType.BRUTE_FORCE, 7, 1.7),
                mod(StatType.BLADE_TECHNIQUE, 6, 1.6),
                mod(StatType.PHYSICAL_RESISTANCE, 6, 1.5),
                mod(StatType.ARCANE_POWER, 4, 1.3),
                mod(StatType.AIR_AFFINITY, 2, 1.2),  // +AIR_AFFINITY
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:death_oni",         List.of(
                mod(StatType.BRUTE_FORCE, 8, 1.8),
                mod(StatType.BLADE_TECHNIQUE, 7, 1.7),
                mod(StatType.PHYSICAL_RESISTANCE, 7, 1.6),
                mod(StatType.INTIMIDATION, 5, 1.4),
                mod(StatType.WILLPOWER, 3, 1.3),  // +WILLPOWER
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:divine_oni",        List.of(
                mod(StatType.BRUTE_FORCE, 9, 2.0),
                mod(StatType.BLADE_TECHNIQUE, 8, 1.8),
                mod(StatType.PHYSICAL_RESISTANCE, 8, 1.7),
                mod(StatType.INTIMIDATION, 6, 1.5),
                mod(StatType.WILLPOWER, 4, 1.4),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:divine_fighter",    List.of(
                mod(StatType.BRUTE_FORCE, 10, 2.5),
                mod(StatType.BLADE_TECHNIQUE, 9, 2.0),
                mod(StatType.PHYSICAL_RESISTANCE, 9, 2.0),
                mod(StatType.INTIMIDATION, 7, 1.8),
                mod(StatType.WILLPOWER, 5, 1.5)
        ), List.of());

        // ── ELF family (magic users) ──────────────────────
        put("tensura:elf",               List.of(
                mod(StatType.PRECISION, 2, 1.2),
                mod(StatType.MAGIC_RESISTANCE, 1, 1.1),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:enlightened_elf",   List.of(
                mod(StatType.PRECISION, 3, 1.3),
                mod(StatType.MAGIC_RESISTANCE, 2, 1.2),
                mod(StatType.ARCANE_POWER, 1, 1.1),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:elf_saint",         List.of(
                mod(StatType.PRECISION, 4, 1.4),
                mod(StatType.MAGIC_RESISTANCE, 3, 1.3),
                mod(StatType.ARCANE_POWER, 2, 1.2),
                mod(StatType.ERUDITION, 2, 1.2),  // +ERUDITION
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:divine_elf",        List.of(
                mod(StatType.PRECISION, 5, 1.5),
                mod(StatType.MAGIC_RESISTANCE, 4, 1.4),
                mod(StatType.ARCANE_POWER, 3, 1.3),
                mod(StatType.ERUDITION, 3, 1.3),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());

        // ── DWARF family (crafters) ───────────────────────
        put("tensura:dwarf",             List.of(
                mod(StatType.FORGING, 2, 1.3),  // +FORGING
                mod(StatType.PHYSICAL_RESISTANCE, 1, 1.1)
        ), List.of());
        put("tensura:enlightened_dwarf", List.of(
                mod(StatType.FORGING, 3, 1.5),
                mod(StatType.PHYSICAL_RESISTANCE, 2, 1.2),
                mod(StatType.COOKING, 1, 1.1)   // +COOKING
        ), List.of());
        put("tensura:dwarf_saint",       List.of(
                mod(StatType.FORGING, 4, 1.7),
                mod(StatType.PHYSICAL_RESISTANCE, 3, 1.3),
                mod(StatType.COOKING, 2, 1.2),
                mod(StatType.ALCHEMY, 2, 1.2)   // +ALCHEMY
        ), List.of());
        put("tensura:divine_dwarf",      List.of(
                mod(StatType.FORGING, 5, 2.0),
                mod(StatType.PHYSICAL_RESISTANCE, 4, 1.4),
                mod(StatType.COOKING, 3, 1.3),
                mod(StatType.ALCHEMY, 3, 1.3)
        ), List.of());

        // ── SLIME family (tanks) ──────────────────────────
        put("tensura:slime",             List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 2, 1.2),   // +PHYSICAL_ENDURANCE
                mod(StatType.WILLPOWER, 1, 1.1),  // +WILLPOWER
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:metal_slime",       List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 3, 1.3),
                mod(StatType.WILLPOWER, 2, 1.2),
                mod(StatType.PHYSICAL_RESISTANCE, 2, 1.2),   // +PHYSICAL_RESISTANCE
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:demon_slime",       List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 4, 1.4),
                mod(StatType.WILLPOWER, 3, 1.3),
                mod(StatType.PHYSICAL_RESISTANCE, 3, 1.3),
                mod(StatType.ARCANE_POWER, 2, 1.2),   // +ARCANE_POWER
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:god_slime",         List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 5, 1.5),
                mod(StatType.WILLPOWER, 4, 1.4),
                mod(StatType.PHYSICAL_RESISTANCE, 4, 1.4),
                mod(StatType.ARCANE_POWER, 3, 1.3),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());

        // ── LIZARDMAN / DRAGON family ─────────────────────
        put("tensura:lizardman",         List.of(
                mod(StatType.BRUTE_FORCE, 2, 1.2),
                mod(StatType.PHYSICAL_RESISTANCE, 1, 1.1),
                mod(StatType.FIRE_AFFINITY, 1, 1.1)   // +FIRE_AFFINITY
        ), List.of());
        put("tensura:dragonewt",         List.of(
                mod(StatType.BRUTE_FORCE, 3, 1.3),
                mod(StatType.PHYSICAL_RESISTANCE, 2, 1.2),
                mod(StatType.FIRE_AFFINITY, 2, 1.2),
                mod(StatType.PHYSICAL_ENDURANCE, 1, 1.1),   // +PHYSICAL_ENDURANCE
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:true_dragonewt",    List.of(
                mod(StatType.BRUTE_FORCE, 4, 1.4),
                mod(StatType.PHYSICAL_RESISTANCE, 3, 1.3),
                mod(StatType.FIRE_AFFINITY, 3, 1.3),
                mod(StatType.PHYSICAL_ENDURANCE, 2, 1.2),
                mod(StatType.AIR_AFFINITY, 1, 1.1),  // +AIR_AFFINITY
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:divine_dragon",     List.of(
                mod(StatType.BRUTE_FORCE, 5, 1.5),
                mod(StatType.PHYSICAL_RESISTANCE, 4, 1.4),
                mod(StatType.FIRE_AFFINITY, 4, 1.4),
                mod(StatType.PHYSICAL_ENDURANCE, 3, 1.3),
                mod(StatType.AIR_AFFINITY, 2, 1.2),
                mod(StatType.WILLPOWER, 2, 1.2),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());

        // ── BEASTFOLK family (scouts) ─────────────────────
        put("tensura:beastfolk",         List.of(
                mod(StatType.TRACKING, 2, 1.3),  // +TRACKING
                mod(StatType.KEEN_SENSES, 1, 1.2),  // +KEEN_SENSES
                mod(StatType.AGILITY, 1, 1.1)    // +AGILITY
        ), List.of());
        put("tensura:beast_lord",        List.of(
                mod(StatType.TRACKING, 3, 1.5),
                mod(StatType.KEEN_SENSES, 2, 1.3),
                mod(StatType.AGILITY, 2, 1.2),
                mod(StatType.BRUTE_FORCE, 1, 1.1)
        ), List.of());
        put("tensura:spirit_beast",      List.of(
                mod(StatType.TRACKING, 4, 1.7),
                mod(StatType.KEEN_SENSES, 3, 1.5),
                mod(StatType.AGILITY, 3, 1.3),
                mod(StatType.BRUTE_FORCE, 2, 1.2),
                mod(StatType.ARCANE_POWER, 1, 1.1)
        ), List.of());
        put("tensura:divine_beast",      List.of(
                mod(StatType.TRACKING, 5, 2.0),
                mod(StatType.KEEN_SENSES, 4, 1.7),
                mod(StatType.AGILITY, 4, 1.4),
                mod(StatType.BRUTE_FORCE, 3, 1.3),
                mod(StatType.ARCANE_POWER, 2, 1.2)
        ), List.of());

        // ── HARPY family (swift aerial) ───────────────────
        put("tensura:harpy",             List.of(
                mod(StatType.AIR_AFFINITY, 2, 1.3),  // +AIR_AFFINITY
                mod(StatType.AGILITY, 2, 1.2)    // +AGILITY
        ), List.of());
        put("tensura:harpy_queen",       List.of(
                mod(StatType.AIR_AFFINITY, 3, 1.5),
                mod(StatType.AGILITY, 3, 1.3),
                mod(StatType.PRECISION, 1, 1.1)    // +PRECISION
        ), List.of());
        put("tensura:spirit_bird",       List.of(
                mod(StatType.AIR_AFFINITY, 4, 1.7),
                mod(StatType.AGILITY, 4, 1.4),
                mod(StatType.PRECISION, 2, 1.2),
                mod(StatType.ARCANE_POWER, 1, 1.1)
        ), List.of());
        put("tensura:divine_bird",       List.of(
                mod(StatType.AIR_AFFINITY, 5, 2.0),
                mod(StatType.AGILITY, 5, 1.5),
                mod(StatType.PRECISION, 3, 1.3),
                mod(StatType.ARCANE_POWER, 2, 1.2)
        ), List.of());

        // ── GOBLIN family (swift daggers) ─────────────────
        put("tensura:goblin",            List.of(
                mod(StatType.RAPIDITE, 2, 1.2),   // +RAPIDITE
                mod(StatType.AGILITY, 1, 1.2)    // +AGILITY
        ), List.of());
        put("tensura:hobgoblin",         List.of(
                mod(StatType.RAPIDITE, 3, 1.3),
                mod(StatType.AGILITY, 2, 1.3),
                mod(StatType.BLADE_TECHNIQUE, 1, 1.1)
        ), List.of());
        put("tensura:enlightened_hobgoblin", List.of(
                mod(StatType.RAPIDITE, 4, 1.4),
                mod(StatType.AGILITY, 3, 1.4),
                mod(StatType.BLADE_TECHNIQUE, 2, 1.2),
                mod(StatType.BRUTE_FORCE, 1, 1.1)
        ), List.of());
        put("tensura:hobgoblin_saint",   List.of(
                mod(StatType.RAPIDITE, 5, 1.5),
                mod(StatType.AGILITY, 4, 1.5),
                mod(StatType.BLADE_TECHNIQUE, 3, 1.3),
                mod(StatType.BRUTE_FORCE, 2, 1.2)
        ), List.of());

        // ── GIANT family (tanks) ──────────────────────────
        put("tensura:giant",             List.of(
                mod(StatType.BRUTE_FORCE, 3, 1.3),
                mod(StatType.PHYSICAL_ENDURANCE, 2, 1.2)
        ), List.of());
        put("tensura:ancient_giant",     List.of(
                mod(StatType.BRUTE_FORCE, 4, 1.4),
                mod(StatType.PHYSICAL_ENDURANCE, 3, 1.3),
                mod(StatType.PHYSICAL_RESISTANCE, 2, 1.2)
        ), List.of());
        put("tensura:divine_giant",      List.of(
                mod(StatType.BRUTE_FORCE, 5, 1.5),
                mod(StatType.PHYSICAL_ENDURANCE, 4, 1.4),
                mod(StatType.PHYSICAL_RESISTANCE, 3, 1.3),
                mod(StatType.WILLPOWER, 2, 1.2)
        ), List.of());

        // ── ORC family (brute endurance) ──────────────────
        put("tensura:orc",               List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 2, 1.3),
                mod(StatType.PHYSICAL_RESISTANCE, 1, 1.1)
        ), List.of());
        put("tensura:high_orc",          List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 3, 1.4),
                mod(StatType.PHYSICAL_RESISTANCE, 2, 1.2),
                mod(StatType.BRUTE_FORCE, 1, 1.1)
        ), List.of());
        put("tensura:spirit_boar",       List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 4, 1.5),
                mod(StatType.PHYSICAL_RESISTANCE, 3, 1.3),
                mod(StatType.BRUTE_FORCE, 2, 1.2)
        ), List.of());
        put("tensura:orc_lord",          List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 5, 1.6),
                mod(StatType.PHYSICAL_RESISTANCE, 4, 1.4),
                mod(StatType.BRUTE_FORCE, 3, 1.3),
                mod(StatType.INTIMIDATION, 2, 1.2)   // +INTIMIDATION
        ), List.of());
        put("tensura:orc_disaster",      List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 6, 1.7),
                mod(StatType.PHYSICAL_RESISTANCE, 5, 1.5),
                mod(StatType.BRUTE_FORCE, 4, 1.4),
                mod(StatType.INTIMIDATION, 3, 1.3),
                mod(StatType.WILLPOWER, 2, 1.2)
        ), List.of());
        put("tensura:divine_boar",       List.of(
                mod(StatType.PHYSICAL_ENDURANCE, 7, 1.8),
                mod(StatType.PHYSICAL_RESISTANCE, 6, 1.6),
                mod(StatType.BRUTE_FORCE, 5, 1.5),
                mod(StatType.INTIMIDATION, 4, 1.4),
                mod(StatType.WILLPOWER, 3, 1.3)
        ), List.of());

        // ── MERFOLK family (water) ────────────────────────
        put("tensura:merfolk",           List.of(
                mod(StatType.WATER_AFFINITY, 2, 1.3),   // +WATER_AFFINITY
                mod(StatType.PHYSICAL_ENDURANCE, 1, 1.1)
        ), List.of());
        put("tensura:enlightened_merfolk", List.of(
                mod(StatType.WATER_AFFINITY, 3, 1.5),
                mod(StatType.PHYSICAL_ENDURANCE, 2, 1.2),
                mod(StatType.ARCANE_POWER, 1, 1.1)
        ), List.of());
        put("tensura:merfolk_saint",     List.of(
                mod(StatType.WATER_AFFINITY, 4, 1.7),
                mod(StatType.PHYSICAL_ENDURANCE, 3, 1.3),
                mod(StatType.ARCANE_POWER, 2, 1.2),
                mod(StatType.MAGIC_RESISTANCE, 1, 1.1)
        ), List.of());
        put("tensura:divine_fish",       List.of(
                mod(StatType.WATER_AFFINITY, 5, 2.0),
                mod(StatType.PHYSICAL_ENDURANCE, 4, 1.4),
                mod(StatType.ARCANE_POWER, 3, 1.3),
                mod(StatType.MAGIC_RESISTANCE, 2, 1.2)
        ), List.of());

        // ── VAMPIRE family (dark rogues) ──────────────────
        put("tensura:ghoul",             List.of(
                mod(StatType.INTIMIDATION, 2, 1.2),  // +INTIMIDATION
                mod(StatType.RAPIDITE, 1, 1.1)
        ), List.of());
        put("tensura:vampire",           List.of(
                mod(StatType.INTIMIDATION, 3, 1.3),
                mod(StatType.RAPIDITE, 2, 1.2),
                mod(StatType.WILLPOWER, 1, 1.1),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:vampire_overcomer", List.of(
                mod(StatType.INTIMIDATION, 4, 1.4),
                mod(StatType.RAPIDITE, 3, 1.3),
                mod(StatType.WILLPOWER, 2, 1.2),
                mod(StatType.ARCANE_POWER, 1, 1.1),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:vampire_lord",      List.of(
                mod(StatType.RAPIDITE, 4, 1.4),
                mod(StatType.WILLPOWER, 3, 1.3),
                mod(StatType.ARCANE_POWER, 2, 1.2),
                mod(StatType.INTIMIDATION, 6, 1.5),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:divine_vampire",    List.of(
                mod(StatType.INTIMIDATION, 7, 1.7),
                mod(StatType.RAPIDITE, 5, 1.5),
                mod(StatType.WILLPOWER, 4, 1.4),
                mod(StatType.ARCANE_POWER, 3, 1.3),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());

        // ── DAEMON family (dark magic) ────────────────────
        put("tensura:lesser_daemon",     List.of(
                mod(StatType.ARCANE_POWER, 2, 1.3),   // +ARCANE_POWER
                mod(StatType.INTIMIDATION, 1, 1.2),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:greater_daemon",    List.of(
                mod(StatType.ARCANE_POWER, 3, 1.5),
                mod(StatType.INTIMIDATION, 2, 1.3),
                mod(StatType.WILLPOWER, 1, 1.1),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:arch_daemon",       List.of(
                mod(StatType.ARCANE_POWER, 4, 1.7),
                mod(StatType.INTIMIDATION, 3, 1.4),
                mod(StatType.WILLPOWER, 2, 1.2),
                mod(StatType.MAGIC_RESISTANCE, 2, 1.2),  // +MAGIC_RESISTANCE
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:daemon_lord",       List.of(
                mod(StatType.ARCANE_POWER, 5, 2.0),
                mod(StatType.INTIMIDATION, 4, 1.5),
                mod(StatType.WILLPOWER, 3, 1.3),
                mod(StatType.MAGIC_RESISTANCE, 3, 1.3),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());
        put("tensura:devil_lord",        List.of(
                mod(StatType.ARCANE_POWER, 6, 2.5),
                mod(StatType.INTIMIDATION, 5, 1.7),
                mod(StatType.WILLPOWER, 4, 1.4),
                mod(StatType.MAGIC_RESISTANCE, 4, 1.4),
                mod(StatType.CASTING_SPEED, 1, 1.0),
                mod(StatType.MANA_POOL, 1, 1.0)
        ), List.of());

        // ── WIGHT / UNDEAD family ─────────────────────────
        put("tensura:wight",             List.of(
                mod(StatType.WILLPOWER, 2, 1.3),  // +WILLPOWER
                mod(StatType.MAGIC_RESISTANCE, 1, 1.1)
        ), List.of());
        put("tensura:wight_king",        List.of(
                mod(StatType.WILLPOWER, 3, 1.4),
                mod(StatType.MAGIC_RESISTANCE, 2, 1.2),
                mod(StatType.INTIMIDATION, 1, 1.1)
        ), List.of());
        put("tensura:spirit_skeleton",   List.of(
                mod(StatType.WILLPOWER, 4, 1.5),
                mod(StatType.MAGIC_RESISTANCE, 3, 1.3),
                mod(StatType.INTIMIDATION, 2, 1.2)
        ), List.of());
        put("tensura:divine_skeleton",   List.of(
                mod(StatType.WILLPOWER, 5, 1.7),
                mod(StatType.MAGIC_RESISTANCE, 4, 1.4),
                mod(StatType.INTIMIDATION, 3, 1.3),
                mod(StatType.ARCANE_POWER, 1, 1.1)
        ), List.of());
    }

    private static RaceModifier mod(StatType stat, int bonus, double xpMul) {
        return new RaceModifier(stat.index, bonus, xpMul);
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
