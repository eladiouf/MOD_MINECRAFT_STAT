package tong.statmod.dungeon;

import java.util.List;
import java.util.Map;

/**
 * Mission M6 — 100 thèmes de donjon (1 par étage), générés (2026-07-04).
 *
 * <p>Chaque étage 1-100 a son thème : un nom d'affichage, une horde d'adds et un mini-boss,
 * organisés en 10 arcs de 10 étages à difficulté croissante. Tous les IDs sont vérifiés
 * contre les vrais jars ; les absents sont ignorés à la résolution (dégradation propre).
 *
 * <p>Au-delà de l'étage 100, on boucle sur le thème (floor-1)%100+1 avec un préfixe de
 * profondeur — le donjon reste infini.
 *
 * <p><b>Généré</b> par {@code scratchpad/gen_themes.py} — ne pas éditer à la main ; régénérer.
 */
public final class DungeonThemes {

    /** Un thème d'étage : nom, adds (horde), mini-boss (premier dispo utilisé). */
    public record Theme(String displayName, List<String> adds, List<String> miniBoss) {}

    private DungeonThemes() {}

    private static final Theme[] THEMES = {
        new Theme("§7The Awakening — Threshold", List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider"), List.of("minecraft:iron_golem")), // floor 1
        new Theme("§7The Awakening — Descent", List.of("slu:hollow", "slu:armed_hollow", "slu:hollow_soldier_sword"), List.of("slu:havel")), // floor 2
        new Theme("§7The Awakening — Warren", List.of("tensura:goblin", "tensura:lizardman", "tensura:orc"), List.of("tensura:orc_lord")), // floor 3
        new Theme("§7The Awakening — Gauntlet", List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider"), List.of("minecraft:iron_golem")), // floor 4
        new Theme("§7The Awakening — Crossing", List.of("slu:armed_hollow", "slu:hollow_soldier_sword", "slu:hollow_soldier_spear"), List.of("slu:executor")), // floor 5
        new Theme("§7The Awakening — Bastion", List.of("tensura:lizardman", "tensura:orc", "tensura:orc"), List.of("tensura:orc_lord")), // floor 6
        new Theme("§7The Awakening — Hollow", List.of("minecraft:skeleton", "minecraft:spider", "minecraft:husk"), List.of("minecraft:iron_golem")), // floor 7
        new Theme("§7The Awakening — Sanctum", List.of("slu:armed_hollow", "slu:hollow_soldier_sword", "slu:hollow_soldier_spear"), List.of("slu:executor")), // floor 8
        new Theme("§7The Awakening — Crucible", List.of("tensura:orc", "tensura:orc", "tensura:orc_lord"), List.of("tensura:orc_disaster")), // floor 9
        new Theme("§7The Awakening — §lThrone", List.of("minecraft:spider", "minecraft:husk", "minecraft:cave_spider"), List.of("cataclysm:ignited_revenant")), // floor 10
        new Theme("§8Restless Dead — Threshold", List.of("born_in_chaos_v1:decaying_zombie", "born_in_chaos_v1:barrel_zombie", "born_in_chaos_v1:zombie_fisherman"), List.of("born_in_chaos_v1:lifestealer")), // floor 11
        new Theme("§8Restless Dead — Descent", List.of("born_in_chaos_v1:baby_skeleton", "born_in_chaos_v1:decrepit_skeleton", "born_in_chaos_v1:skeleton_thrasher"), List.of("born_in_chaos_v1:bonescaller")), // floor 12
        new Theme("§8Restless Dead — Warren", List.of("slu:hollow", "slu:armed_hollow", "slu:hollow_soldier_sword"), List.of("slu:havel")), // floor 13
        new Theme("§8Restless Dead — Gauntlet", List.of("born_in_chaos_v1:decaying_zombie", "born_in_chaos_v1:barrel_zombie", "born_in_chaos_v1:zombie_fisherman"), List.of("born_in_chaos_v1:lifestealer")), // floor 14
        new Theme("§8Restless Dead — Crossing", List.of("born_in_chaos_v1:decrepit_skeleton", "born_in_chaos_v1:skeleton_thrasher", "born_in_chaos_v1:skeleton_demoman"), List.of("born_in_chaos_v1:supreme_bonescaller")), // floor 15
        new Theme("§8Restless Dead — Bastion", List.of("slu:armed_hollow", "slu:hollow_soldier_sword", "slu:hollow_soldier_spear"), List.of("slu:executor")), // floor 16
        new Theme("§8Restless Dead — Hollow", List.of("born_in_chaos_v1:barrel_zombie", "born_in_chaos_v1:zombie_fisherman", "born_in_chaos_v1:zombie_lumberjack"), List.of("born_in_chaos_v1:lifestealer")), // floor 17
        new Theme("§8Restless Dead — Sanctum", List.of("born_in_chaos_v1:decrepit_skeleton", "born_in_chaos_v1:skeleton_thrasher", "born_in_chaos_v1:skeleton_demoman"), List.of("born_in_chaos_v1:supreme_bonescaller")), // floor 18
        new Theme("§8Restless Dead — Crucible", List.of("slu:hollow_soldier_sword", "slu:hollow_soldier_spear", "slu:thief"), List.of("slu:executor")), // floor 19
        new Theme("§8Restless Dead — §lThrone", List.of("born_in_chaos_v1:zombie_fisherman", "born_in_chaos_v1:zombie_lumberjack", "born_in_chaos_v1:zombie_bruiser"), List.of("cataclysm:ignited_revenant")), // floor 20
        new Theme("§2Warband — Threshold", List.of("tensura:goblin", "tensura:lizardman", "tensura:orc"), List.of("tensura:orc_lord")), // floor 21
        new Theme("§2Warband — Descent", List.of("minecraft:pillager", "minecraft:vindicator", "minecraft:evoker"), List.of("minecraft:ravager")), // floor 22
        new Theme("§2Warband — Warren", List.of("slu:knight", "slu:castle_guard", "slu:dungeon_knight"), List.of("slu:havel")), // floor 23
        new Theme("§2Warband — Gauntlet", List.of("tensura:goblin", "tensura:lizardman", "tensura:orc"), List.of("tensura:orc_lord")), // floor 24
        new Theme("§2Warband — Crossing", List.of("minecraft:vindicator", "minecraft:evoker", "minecraft:ravager"), List.of("minecraft:ravager")), // floor 25
        new Theme("§2Warband — Bastion", List.of("slu:castle_guard", "slu:dungeon_knight", "slu:elite_knight"), List.of("slu:monster_tower_knight")), // floor 26
        new Theme("§2Warband — Hollow", List.of("tensura:lizardman", "tensura:orc", "tensura:orc"), List.of("tensura:orc_lord")), // floor 27
        new Theme("§2Warband — Sanctum", List.of("minecraft:vindicator", "minecraft:evoker", "minecraft:ravager"), List.of("minecraft:ravager")), // floor 28
        new Theme("§2Warband — Crucible", List.of("slu:dungeon_knight", "slu:elite_knight", "slu:noble_knight"), List.of("slu:monster_crucible_knight")), // floor 29
        new Theme("§2Warband — §lThrone", List.of("tensura:orc", "tensura:orc", "tensura:orc_lord"), List.of("cataclysm:the_prowler")), // floor 30
        new Theme("§6The Hunt — Threshold", List.of("tensura:horned_rabbit", "tensura:hound_dog", "tensura:direwolf"), List.of("tensura:barghest")), // floor 31
        new Theme("§6The Hunt — Descent", List.of("tensura:army_wasp", "tensura:giant_ant", "tensura:black_spider"), List.of("tensura:evil_centipede")), // floor 32
        new Theme("§6The Hunt — Warren", List.of("mowziesmobs:foliaath", "mowziesmobs:umvuthana_raptor", "mowziesmobs:umvuthana_crane"), List.of("mowziesmobs:ferrous_wroughtnaut")), // floor 33
        new Theme("§6The Hunt — Gauntlet", List.of("tensura:horned_rabbit", "tensura:hound_dog", "tensura:direwolf"), List.of("tensura:barghest")), // floor 34
        new Theme("§6The Hunt — Crossing", List.of("tensura:giant_ant", "tensura:black_spider", "tensura:hell_moth"), List.of("tensura:knight_spider")), // floor 35
        new Theme("§6The Hunt — Bastion", List.of("mowziesmobs:umvuthana_raptor", "mowziesmobs:umvuthana_crane", "mowziesmobs:grottol"), List.of("mowziesmobs:naga")), // floor 36
        new Theme("§6The Hunt — Hollow", List.of("tensura:hound_dog", "tensura:direwolf", "tensura:giant_bear"), List.of("tensura:charybdis")), // floor 37
        new Theme("§6The Hunt — Sanctum", List.of("tensura:giant_ant", "tensura:black_spider", "tensura:hell_moth"), List.of("tensura:knight_spider")), // floor 38
        new Theme("§6The Hunt — Crucible", List.of("mowziesmobs:umvuthana_crane", "mowziesmobs:grottol", "mowziesmobs:naga"), List.of("mowziesmobs:umvuthi")), // floor 39
        new Theme("§6The Hunt — §lThrone", List.of("tensura:direwolf", "tensura:giant_bear", "tensura:barghest"), List.of("cataclysm:the_prowler")), // floor 40
        new Theme("§3Sunken Halls — Threshold", List.of("cataclysm:deepling", "cataclysm:deepling_angler", "cataclysm:deepling_brute"), List.of("cataclysm:wadjet")), // floor 41
        new Theme("§3Sunken Halls — Descent", List.of("cataclysm:amethyst_crab", "cataclysm:urchinkin", "cataclysm:coral_golem"), List.of("cataclysm:coralssus")), // floor 42
        new Theme("§3Sunken Halls — Warren", List.of("block_factorys_bosses:crossbow_pirate", "block_factorys_bosses:pirate_rook", "block_factorys_bosses:soul_skeleton"), List.of("block_factorys_bosses:pirate_captain")), // floor 43
        new Theme("§3Sunken Halls — Gauntlet", List.of("cataclysm:deepling", "cataclysm:deepling_angler", "cataclysm:deepling_brute"), List.of("cataclysm:wadjet")), // floor 44
        new Theme("§3Sunken Halls — Crossing", List.of("cataclysm:urchinkin", "cataclysm:coral_golem", "cataclysm:hippocamtus"), List.of("cataclysm:coralssus")), // floor 45
        new Theme("§3Sunken Halls — Bastion", List.of("block_factorys_bosses:pirate_rook", "block_factorys_bosses:soul_skeleton", "block_factorys_bosses:pirate_captain"), List.of("block_factorys_bosses:underworld_knight")), // floor 46
        new Theme("§3Sunken Halls — Hollow", List.of("cataclysm:deepling_angler", "cataclysm:deepling_brute", "cataclysm:deepling_warlock"), List.of("cataclysm:wadjet")), // floor 47
        new Theme("§3Sunken Halls — Sanctum", List.of("cataclysm:urchinkin", "cataclysm:coral_golem", "cataclysm:hippocamtus"), List.of("cataclysm:coralssus")), // floor 48
        new Theme("§3Sunken Halls — Crucible", List.of("block_factorys_bosses:soul_skeleton", "block_factorys_bosses:pirate_captain", "block_factorys_bosses:underworld_knight"), List.of("block_factorys_bosses:infernal_dragon")), // floor 49
        new Theme("§3Sunken Halls — §lThrone", List.of("cataclysm:deepling_brute", "cataclysm:deepling_warlock", "cataclysm:deepling_priest"), List.of("cataclysm:ender_golem")), // floor 50
        new Theme("§bFrozen Reach — Threshold", List.of("block_factorys_bosses:frozen_skeleton", "block_factorys_bosses:soul_skeleton", "block_factorys_bosses:soul_knight_wither_skeleton"), List.of("block_factorys_bosses:yeti")), // floor 51
        new Theme("§bFrozen Reach — Descent", List.of("cataclysm:draugr", "cataclysm:koboleton", "cataclysm:elite_draugr"), List.of("cataclysm:royal_draugr")), // floor 52
        new Theme("§bFrozen Reach — Warren", List.of("mutantmonsters:mutant_zombie", "mutantmonsters:mutant_skeleton", "mutantmonsters:mutant_creeper"), List.of("mutantmonsters:mutant_skeleton")), // floor 53
        new Theme("§bFrozen Reach — Gauntlet", List.of("block_factorys_bosses:frozen_skeleton", "block_factorys_bosses:soul_skeleton", "block_factorys_bosses:soul_knight_wither_skeleton"), List.of("block_factorys_bosses:yeti")), // floor 54
        new Theme("§bFrozen Reach — Crossing", List.of("cataclysm:koboleton", "cataclysm:elite_draugr", "cataclysm:royal_draugr"), List.of("cataclysm:the_harbinger")), // floor 55
        new Theme("§bFrozen Reach — Bastion", List.of("mutantmonsters:mutant_skeleton", "mutantmonsters:mutant_creeper", "mutantmonsters:mutant_enderman"), List.of("mutantmonsters:mutant_enderman")), // floor 56
        new Theme("§bFrozen Reach — Hollow", List.of("block_factorys_bosses:soul_skeleton", "block_factorys_bosses:soul_knight_wither_skeleton", "block_factorys_bosses:flaming_skeleton_guard_sword"), List.of("block_factorys_bosses:underworld_knight")), // floor 57
        new Theme("§bFrozen Reach — Sanctum", List.of("cataclysm:koboleton", "cataclysm:elite_draugr", "cataclysm:royal_draugr"), List.of("cataclysm:the_harbinger")), // floor 58
        new Theme("§bFrozen Reach — Crucible", List.of("mutantmonsters:mutant_creeper", "mutantmonsters:mutant_enderman", "mutantmonsters:mutant_zombie"), List.of("mutantmonsters:mutant_enderman")), // floor 59
        new Theme("§bFrozen Reach — §lThrone", List.of("block_factorys_bosses:soul_knight_wither_skeleton", "block_factorys_bosses:flaming_skeleton_guard_sword", "block_factorys_bosses:dragon_guard_sword"), List.of("cataclysm:ender_golem")), // floor 60
        new Theme("§eHollow Harvest — Threshold", List.of("born_in_chaos_v1:pumpkin_dunce", "born_in_chaos_v1:mr_pumpkin", "born_in_chaos_v1:zombie_clown"), List.of("born_in_chaos_v1:sir_pumpkinhead")), // floor 61
        new Theme("§eHollow Harvest — Descent", List.of("born_in_chaos_v1:restless_spirit", "born_in_chaos_v1:seared_spirit", "born_in_chaos_v1:infernal_spirit"), List.of("born_in_chaos_v1:nightmare_stalker")), // floor 62
        new Theme("§eHollow Harvest — Warren", List.of("slu:dark_knight", "slu:ringed_knight", "slu:mad_knight"), List.of("slu:monster_crucible_knight")), // floor 63
        new Theme("§eHollow Harvest — Gauntlet", List.of("born_in_chaos_v1:pumpkin_dunce", "born_in_chaos_v1:mr_pumpkin", "born_in_chaos_v1:zombie_clown"), List.of("born_in_chaos_v1:sir_pumpkinhead")), // floor 64
        new Theme("§eHollow Harvest — Crossing", List.of("born_in_chaos_v1:seared_spirit", "born_in_chaos_v1:infernal_spirit", "born_in_chaos_v1:pumpkin_spirit"), List.of("born_in_chaos_v1:nightmare_stalker")), // floor 65
        new Theme("§eHollow Harvest — Bastion", List.of("slu:ringed_knight", "slu:mad_knight", "slu:shadow_assassin"), List.of("slu:monster_successor")), // floor 66
        new Theme("§eHollow Harvest — Hollow", List.of("born_in_chaos_v1:mr_pumpkin", "born_in_chaos_v1:zombie_clown", "born_in_chaos_v1:pumpkin_bruiser"), List.of("born_in_chaos_v1:lord_pumpkinhead")), // floor 67
        new Theme("§eHollow Harvest — Sanctum", List.of("born_in_chaos_v1:seared_spirit", "born_in_chaos_v1:infernal_spirit", "born_in_chaos_v1:pumpkin_spirit"), List.of("born_in_chaos_v1:nightmare_stalker")), // floor 68
        new Theme("§eHollow Harvest — Crucible", List.of("slu:mad_knight", "slu:shadow_assassin", "slu:wither_skeleton_knight"), List.of("slu:monster_blasphemy_knight")), // floor 69
        new Theme("§eHollow Harvest — §lThrone", List.of("born_in_chaos_v1:zombie_clown", "born_in_chaos_v1:pumpkin_bruiser", "born_in_chaos_v1:senor_pumpkin"), List.of("cataclysm:netherite_monstrosity")), // floor 70
        new Theme("§dArcane Storm — Threshold", List.of("irons_spellbooks:cultist", "irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer"), List.of("irons_spellbooks:archevoker")), // floor 71
        new Theme("§dArcane Storm — Descent", List.of("tensura:ifrit", "tensura:undine", "tensura:salamander"), List.of("tensura:elemental_colossus")), // floor 72
        new Theme("§dArcane Storm — Warren", List.of("slu:monster_godrick_soldier", "slu:monster_godrick_knight", "slu:monster_tower_knight"), List.of("slu:monster_crucible_knight_2")), // floor 73
        new Theme("§dArcane Storm — Gauntlet", List.of("irons_spellbooks:cultist", "irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer"), List.of("irons_spellbooks:archevoker")), // floor 74
        new Theme("§dArcane Storm — Crossing", List.of("tensura:undine", "tensura:salamander", "tensura:sylphide"), List.of("tensura:elemental_colossus")), // floor 75
        new Theme("§dArcane Storm — Bastion", List.of("slu:monster_godrick_knight", "slu:monster_tower_knight", "slu:monster_crucible_knight"), List.of("slu:bad_omen_giant")), // floor 76
        new Theme("§dArcane Storm — Hollow", List.of("irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer", "irons_spellbooks:archevoker"), List.of("irons_spellbooks:necromancer")), // floor 77
        new Theme("§dArcane Storm — Sanctum", List.of("tensura:undine", "tensura:salamander", "tensura:sylphide"), List.of("tensura:elemental_colossus")), // floor 78
        new Theme("§dArcane Storm — Crucible", List.of("slu:monster_tower_knight", "slu:monster_crucible_knight", "slu:monster_blasphemy_knight"), List.of("slu:magma_giant")), // floor 79
        new Theme("§dArcane Storm — §lThrone", List.of("irons_spellbooks:cryomancer", "irons_spellbooks:archevoker", "irons_spellbooks:necromancer"), List.of("cataclysm:netherite_monstrosity")), // floor 80
        new Theme("§4Infernal Legion — Threshold", List.of("tensura:lesser_daemon", "tensura:greater_daemon", "tensura:arch_daemon"), List.of("tensura:arch_daemon")), // floor 81
        new Theme("§4Infernal Legion — Descent", List.of("born_in_chaos_v1:fallen_chaos_knight", "born_in_chaos_v1:door_knight", "born_in_chaos_v1:missionary_raider"), List.of("born_in_chaos_v1:dire_hound_leader")), // floor 82
        new Theme("§4Infernal Legion — Warren", List.of("cataclysm:draugr", "cataclysm:koboleton", "cataclysm:elite_draugr"), List.of("cataclysm:royal_draugr")), // floor 83
        new Theme("§4Infernal Legion — Gauntlet", List.of("tensura:lesser_daemon", "tensura:greater_daemon", "tensura:arch_daemon"), List.of("tensura:arch_daemon")), // floor 84
        new Theme("§4Infernal Legion — Crossing", List.of("born_in_chaos_v1:door_knight", "born_in_chaos_v1:missionary_raider", "born_in_chaos_v1:scarlet_persecutor"), List.of("born_in_chaos_v1:mother_spider")), // floor 85
        new Theme("§4Infernal Legion — Bastion", List.of("cataclysm:koboleton", "cataclysm:elite_draugr", "cataclysm:royal_draugr"), List.of("cataclysm:the_harbinger")), // floor 86
        new Theme("§4Infernal Legion — Hollow", List.of("tensura:greater_daemon", "tensura:arch_daemon", "tensura:hell_caterpillar"), List.of("tensura:arch_daemon")), // floor 87
        new Theme("§4Infernal Legion — Sanctum", List.of("born_in_chaos_v1:door_knight", "born_in_chaos_v1:missionary_raider", "born_in_chaos_v1:scarlet_persecutor"), List.of("born_in_chaos_v1:mother_spider")), // floor 88
        new Theme("§4Infernal Legion — Crucible", List.of("cataclysm:elite_draugr", "cataclysm:royal_draugr", "cataclysm:ignited_berserker"), List.of("cataclysm:the_harbinger")), // floor 89
        new Theme("§4Infernal Legion — §lThrone", List.of("tensura:arch_daemon", "tensura:hell_caterpillar", "tensura:evil_centipede"), List.of("cataclysm:the_leviathan")), // floor 90
        new Theme("§5The Abyss — Threshold", List.of("slu:monster_godrick_soldier", "slu:monster_godrick_knight", "slu:monster_tower_knight"), List.of("slu:monster_crucible_knight_2")), // floor 91
        new Theme("§5The Abyss — Descent", List.of("tensura:lesser_daemon", "tensura:greater_daemon", "tensura:arch_daemon"), List.of("tensura:arch_daemon")), // floor 92
        new Theme("§5The Abyss — Warren", List.of("cataclysm:amethyst_crab", "cataclysm:urchinkin", "cataclysm:coral_golem"), List.of("cataclysm:coralssus")), // floor 93
        new Theme("§5The Abyss — Gauntlet", List.of("slu:monster_godrick_soldier", "slu:monster_godrick_knight", "slu:monster_tower_knight"), List.of("slu:monster_crucible_knight_2")), // floor 94
        new Theme("§5The Abyss — Crossing", List.of("tensura:greater_daemon", "tensura:arch_daemon", "tensura:hell_caterpillar"), List.of("tensura:arch_daemon")), // floor 95
        new Theme("§5The Abyss — Bastion", List.of("cataclysm:urchinkin", "cataclysm:coral_golem", "cataclysm:hippocamtus"), List.of("cataclysm:coralssus")), // floor 96
        new Theme("§5The Abyss — Hollow", List.of("slu:monster_godrick_knight", "slu:monster_tower_knight", "slu:monster_crucible_knight"), List.of("slu:bad_omen_giant")), // floor 97
        new Theme("§5The Abyss — Sanctum", List.of("tensura:greater_daemon", "tensura:arch_daemon", "tensura:hell_caterpillar"), List.of("tensura:arch_daemon")), // floor 98
        new Theme("§5The Abyss — Crucible", List.of("cataclysm:coral_golem", "cataclysm:hippocamtus", "cataclysm:drowned_host"), List.of("cataclysm:scylla", "cataclysm:coralssus")), // floor 99
        new Theme("§5The Abyss — §lThrone", List.of("slu:monster_tower_knight", "slu:monster_crucible_knight", "slu:monster_blasphemy_knight"), List.of("cataclysm:the_leviathan")), // floor 100
    };

    /** Nombre de thèmes définis (100). */
    public static int count() { return THEMES.length; }

    /**
     * Thème de l'étage {@code floor} (jamais null pour floor≥1). Boucle au-delà de 100.
     */
    public static Theme forFloor(int floor) {
        if (floor < 1) floor = 1;
        int idx = (floor - 1) % THEMES.length;
        return THEMES[idx];
    }
}
