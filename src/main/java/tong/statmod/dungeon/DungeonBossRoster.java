package tong.statmod.dungeon;

import java.util.List;

/**
 * Mission M6 — Roster de boss pour Forge 1.20.1.
 *
 * <p>10 arcs, basés sur les mods RÉELLEMENT disponibles :
 * slu, irons_spellbooks, block_factorys_bosses, bosses_of_mass_destruction,
 * epic_mobs, deeperdarker, darkdoppelganger.
 *
 * <p>Mods RETIRÉS (absents en 1.20.1) : iceandfire, cataclysm.
 */
public final class DungeonBossRoster {

    public record BossEntry(String entityId, int mode) {}

    private static final List<List<BossEntry>> ARC_POOLS = List.of(
        arc0(),
        arc1(),
        arc2(),
        arc3(),
        arc4(),
        arc5(),
        arc6(),
        arc7(),
        arc8(),
        arc9()
    );

    private DungeonBossRoster() {}

    private static List<BossEntry> arc0() {
        return List.of(
            s("minecraft:wither_skeleton"),
            s("slu:boss_gundyr"),
            s("slu:boss_abyss_watcher"),
            s("slu:boss_fallen_lord"),
            s("slu:boss_beast_clergyman"),
            s("slu:boss_ancient_warrior"),
            s("bosses_of_mass_destruction:lich"),
            s("slu:boss_artorias"),
            s("slu:boss_soul_of_cinder"),
            s("slu:boss_gael")
        );
    }

    private static List<BossEntry> arc1() {
        return List.of(
            s("minecraft:warden"),
            s("bosses_of_mass_destruction:void_blossom"),
            s("block_factorys_bosses:yeti"),
            s("block_factorys_bosses:sandworm"),
            s("block_factorys_bosses:kraken"),
            s("slu:boss_aatrox"),
            s("slu:boss_jax"),
            s("epic_mobs:phoenix_fight"),
            s("deeperdarker:stalker"),
            s("slu:boss_radahn")
        );
    }

    private static List<BossEntry> arc2() {
        return List.of(
            s("slu:boss_artorias"),
            d("slu:boss_ornstein","slu:boss_smough"),
            s("slu:boss_looking_glass_knight"),
            s("slu:boss_crucible_knight"),
            s("slu:boss_dragon_slayer_armour"),
            s("slu:boss_count_robert"),
            s("bosses_of_mass_destruction:gauntlet"),
            s("slu:boss_wukong"),
            d("slu:boss_notch","slu:boss_minecraft_lord"),
            s("epic_mobs:the_knight")
        );
    }

    private static List<BossEntry> arc3() {
        return List.of(
            d("slu:boss_margit","slu:boss_morgott"),
            s("slu:boss_maliketh"),
            s("slu:boss_mohg"),
            s("slu:boss_elemer"),
            d("slu:boss_godfrey","slu:boss_hoarah_loux"),
            d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"),
            s("block_factorys_bosses:underworld_knight"),
            s("slu:boss_pantheon"),
            s("darkdoppelganger:dark_doppelganger"),
            s("bosses_of_mass_destruction:lich")
        );
    }

    private static List<BossEntry> arc4() {
        return List.of(
            d("slu:boss_godskin_apostle","slu:boss_godskin_noble"),
            s("bosses_of_mass_destruction:obsidilith"),
            s("epic_mobs:pillager_king"),
            s("epic_mobs:the_knight"),
            s("block_factorys_bosses:kraken"),
            d("block_factorys_bosses:pirate_captain","block_factorys_bosses:pirate_rook"),
            s("deeperdarker:stalker"),
            s("epic_mobs:micky"),
            s("epic_mobs:karin"),
            w("slu:boss_gael","slu:boss_radagon","slu:boss_elden_beast")
        );
    }

    private static List<BossEntry> arc5() {
        return List.of(
            d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"),
            s("slu:boss_gwyndolin"),
            s("bosses_of_mass_destruction:lich"),
            s("irons_spellbooks:citadel_keeper"),
            s("slu:boss_soul_of_cinder"),
            s("slu:boss_wukong"),
            s("darkdoppelganger:dark_doppelganger"),
            d("slu:boss_malenia","slu:boss_malenia_2"),
            s("epic_mobs:phoenix_fight"),
            w("slu:boss_gael","slu:boss_soul_of_cinder","slu:boss_artorias")
        );
    }

    private static List<BossEntry> arc6() {
        return List.of(
            s("slu:boss_malenia"),
            s("slu:boss_malenia_2"),
            s("block_factorys_bosses:infernal_dragon"),
            d("slu:boss_nameless_king","slu:boss_wukong"),
            s("slu:boss_godskin_apostle"),
            s("slu:boss_godskin_noble"),
            w("slu:boss_gundyr","slu:boss_abyss_watcher"),
            s("epic_mobs:pillager_king"),
            d("slu:boss_margit","slu:boss_morgott"),
            s("deeperdarker:stalker")
        );
    }

    private static List<BossEntry> arc7() {
        return List.of(
            s("slu:boss_radahn"),
            s("slu:boss_radahn_2"),
            s("block_factorys_bosses:infernal_dragon"),
            s("epic_mobs:the_knight"),
            s("bosses_of_mass_destruction:gauntlet"),
            s("slu:boss_aatrox"),
            s("slu:boss_jax"),
            d("minecraft:warden","slu:boss_darius"),
            s("slu:boss_godskin_noble"),
            s("epic_mobs:phoenix_fight")
        );
    }

    private static List<BossEntry> arc8() {
        return List.of(
            s("slu:boss_maliketh"),
            d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"),
            s("slu:boss_pantheon"),
            s("slu:boss_mohg"),
            s("epic_mobs:micky"),
            s("darkdoppelganger:dark_doppelganger"),
            d("slu:boss_fallen_lord","slu:boss_ancient_warrior"),
            s("slu:boss_dragon_slayer_armour"),
            d("slu:boss_count_robert","slu:boss_crucible_knight"),
            w("slu:boss_notch","slu:boss_minecraft_lord","slu:boss_pantheon")
        );
    }

    private static List<BossEntry> arc9() {
        return List.of(
            w("slu:boss_gael","slu:boss_radagon","slu:boss_elden_beast"),
            s("slu:boss_pantheon"),
            s("slu:boss_wukong"),
            s("bosses_of_mass_destruction:void_blossom"),
            s("bosses_of_mass_destruction:obsidilith"),
            d("slu:boss_notch","slu:boss_minecraft_lord"),
            s("epic_mobs:karin"),
            w("slu:boss_radahn_2","slu:boss_malenia_2","slu:boss_elden_beast"),
            s("deeperdarker:stalker"),
            s("slu:boss_artorias")
        );
    }

    public static List<BossEntry> forFloor(int floor) {
        int bossN = floor / 10;
        if (bossN <= 0) return List.of(s("minecraft:wither_skeleton"));
        int arcIndex = (bossN - 1) % 10;
        int pass = (bossN - 1) / 10;
        List<BossEntry> pool = ARC_POOLS.get(arcIndex);
        return List.of(pool.get(pass % pool.size()));
    }

    static BossEntry s(String id) { return new BossEntry(id, 0); }
    static BossEntry d(String id1, String id2) { return new BossEntry(id1+","+id2, 1); }
    static BossEntry w(String... ids) { return new BossEntry(String.join(",", ids), 2); }
}
