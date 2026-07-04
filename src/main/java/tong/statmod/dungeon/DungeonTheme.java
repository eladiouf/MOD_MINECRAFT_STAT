package tong.statmod.dungeon;

import java.util.List;

/**
 * Mission M6 — Étages à thème « Solo Leveling » (2026-07-04).
 *
 * <p>Certains étages de combat sont <b>mono-thème</b> : au lieu d'un mélange, on n'affronte qu'une
 * seule famille de monstres (une horde d'orcs, une nuée d'insectes, une légion de démons…) menée
 * par un <b>mini-boss</b> assorti. C'est le donjon-portail de Solo Leveling : « un étage d'orcs
 * avec un énorme orc au fond ».
 *
 * <p>Chaque thème liste des IDs d'entités (mods détectés au runtime via {@link ModdedMobPool} ; les
 * IDs absents sont simplement ignorés). {@code adds} = les mobs de base, {@code miniBoss} = le chef.
 * Tout est vérifié contre les jars réels (Tensura surtout, qui a un immense bestiaire).
 */
public enum DungeonTheme {

    ORC("§2Orc Warband", "☠ Orc Disaster",
            List.of("tensura:orc", "tensura:orc", "tensura:goblin", "tensura:lizardman"),
            List.of("tensura:orc_lord", "tensura:orc_disaster")),

    UNDEAD("§8Legion of the Dead", "☠ Bone Colossus",
            List.of("tensura:skeleton", "tensura:zombie", "slu:hollow", "slu:armed_hollow", "irons_spellbooks:catacombs_zombie"),
            List.of("tensura:bone_golem", "irons_spellbooks:necromancer")),

    DEMON("§4Demonic Incursion", "☠ Arch Daemon",
            List.of("tensura:lesser_daemon", "tensura:lesser_daemon", "tensura:greater_daemon", "irons_spellbooks:cultist"),
            List.of("tensura:arch_daemon")),

    ELEMENTAL("§bElemental Rift", "☠ Elemental Colossus",
            List.of("tensura:ifrit", "tensura:undine", "tensura:salamander", "tensura:sylphide"),
            List.of("tensura:elemental_colossus")),

    BEAST("§6Beast Den", "☠ Alpha Direwolf",
            List.of("tensura:direwolf", "tensura:giant_bear", "tensura:horned_bear", "tensura:barghest", "tensura:basilisk"),
            List.of("tensura:charybdis", "tensura:giant_bear")),

    INSECT("§aHive Swarm", "☠ Broodmother",
            List.of("tensura:army_wasp", "tensura:giant_ant", "tensura:black_spider", "tensura:hell_moth", "tensura:hell_caterpillar"),
            List.of("tensura:evil_centipede", "tensura:knight_spider")),

    MAGE("§dArcane Conclave", "☠ Archevoker",
            List.of("irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer", "irons_spellbooks:cultist", "irons_spellbooks:apothecarist"),
            List.of("irons_spellbooks:archevoker", "irons_spellbooks:priest")),

    PIRATE("§3Kraken's Crew", "☠ Pirate Captain",
            List.of("block_factorys_bosses:crossbow_pirate", "block_factorys_bosses:pirate_rook", "block_factorys_bosses:soul_skeleton"),
            List.of("block_factorys_bosses:pirate_captain", "block_factorys_bosses:underworld_knight")),

    FROST("§bFrozen Depths", "☠ Yeti",
            List.of("block_factorys_bosses:frozen_skeleton", "block_factorys_bosses:soul_skeleton", "tensura:direwolf"),
            List.of("block_factorys_bosses:yeti", "block_factorys_bosses:underworld_knight")),

    DRAGON("§cDragon's Roost", "☠ Infernal Dragon",
            List.of("block_factorys_bosses:dragon_guard_sword", "block_factorys_bosses:flaming_skeleton_guard_sword", "block_factorys_bosses:soul_knight_wither_skeleton"),
            List.of("block_factorys_bosses:infernal_dragon", "xbbsroaringknightmod:roaring_knight")),

    DRAUGR("§8Draugr Warband", "☠ The Harbinger",
            List.of("cataclysm:draugr", "cataclysm:elite_draugr", "cataclysm:koboleton", "cataclysm:royal_draugr"),
            List.of("cataclysm:the_harbinger", "cataclysm:royal_draugr")),

    DEEP("§3The Deep", "☠ Leviathan Spawn",
            List.of("cataclysm:deepling", "cataclysm:deepling_brute", "cataclysm:deepling_warlock", "cataclysm:deepling_priest"),
            List.of("cataclysm:wadjet", "cataclysm:aptrgangr")),

    NIGHTMARE("§5Nightmare Legion", "☠ Nightmare Stalker",
            List.of("born_in_chaos_v1:decaying_zombie", "born_in_chaos_v1:bonescaller", "born_in_chaos_v1:dread_hound", "born_in_chaos_v1:fallen_chaos_knight"),
            List.of("born_in_chaos_v1:nightmare_stalker", "born_in_chaos_v1:supreme_bonescaller")),

    PUMPKIN("§6Hollow Harvest", "☠ Sir Pumpkinhead",
            List.of("born_in_chaos_v1:pumpkin_bruiser", "born_in_chaos_v1:pumpkin_dunce", "born_in_chaos_v1:mr_pumpkin", "born_in_chaos_v1:zombie_clown"),
            List.of("born_in_chaos_v1:sir_pumpkinhead", "born_in_chaos_v1:lord_pumpkinhead")),

    MUTANT("§2Mutant Outbreak", "☠ Mutant Zombie",
            List.of("mutantmonsters:mutant_zombie", "mutantmonsters:mutant_skeleton", "mutantmonsters:mutant_creeper"),
            List.of("mutantmonsters:mutant_enderman", "mutantmonsters:mutant_zombie")),

    GUARDIAN("§eForgotten Guardians", "☠ Ferrous Wroughtnaut",
            List.of("mowziesmobs:umvuthana_raptor", "mowziesmobs:umvuthana_crane", "mowziesmobs:foliaath"),
            List.of("mowziesmobs:ferrous_wroughtnaut", "mowziesmobs:umvuthi"));

    private final String displayName;
    private final String bossName;
    private final List<String> adds;
    private final List<String> miniBoss;

    DungeonTheme(String displayName, String bossName, List<String> adds, List<String> miniBoss) {
        this.displayName = displayName;
        this.bossName = bossName;
        this.adds = adds;
        this.miniBoss = miniBoss;
    }

    public String displayName() { return displayName; }
    public String bossName() { return bossName; }
    /** IDs des mobs de base du thème (certains peuvent être absents selon les mods installés). */
    public List<String> addIds() { return adds; }
    /** IDs candidats pour le mini-boss (le premier disponible est utilisé). */
    public List<String> miniBossIds() { return miniBoss; }

    /**
     * Thème d'un étage, ou {@code null} si l'étage n'est pas thématique.
     *
     * <p>Règle : les étages de combat (ni ×5 ni ×10) dont le numéro est ≡ 3 (mod 10) — soit 3, 13,
     * 23, 33… — sont thématiques. Le thème tourne de façon déterministe selon l'étage, donc chaque
     * palier de 10 a son étage à thème, et le thème varie d'un palier à l'autre.
     */
    public static DungeonTheme forFloor(int floor) {
        if (floor <= 0) return null;
        if (floor % 10 == 0 || floor % 5 == 0) return null; // boss / trésor : pas de thème
        if (floor % 10 != 3) return null;                   // seul l'étage ×3 du palier est thématique
        DungeonTheme[] all = values();
        int idx = ((floor / 10) % all.length + all.length) % all.length;
        return all[idx];
    }

    /** {@code true} si l'étage est thématique. */
    public static boolean isThemed(int floor) {
        return forFloor(floor) != null;
    }
}
