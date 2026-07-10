package tong.statmod.dungeon;

import java.util.List;

/**
 * Mission M6 — Roster de boss organisé par <b>arc thématique</b> (refonte 2026-07-10).
 *
 * <p>10 arcs ({@link DungeonThemes}), chacun avec ~10 boss entries rangées par difficulté
 * croissante (pass 0 = plus facile, pass 9 = plus dur). Les mods additionnels
 * (Ice and Fire, Cataclysm, BoMD, BFB) fournissent des boss pour les arcs qui leur
 * correspondent thématiquement.
 *
 * <p>Quand un joueur atteint un étage ×10, on calcule :
 * <pre>
 *   arcIndex = ((floor / 10) - 1) % 10
 *   pass     = (floor / 10 - 1) / 10
 *   entry    = ARC_POOLS[arcIndex].get(pass % pool.size())
 * </pre>
 */
public final class DungeonBossRoster {

    public record BossEntry(String entityId, int mode) {} // 0=simple, 1=duo, 2=wave

    private static final List<List<BossEntry>> ARC_POOLS = List.of(
        arc0(), // DÉCHARNÉS
        arc1(), // FAUVES
        arc2(), // TRIBUS
        arc3(), // LÉGION NOIRE
        arc4(), // ABYSSES
        arc5(), // CERCLE DES MAGES
        arc6(), // MOISSON DE L'EFFROI
        arc7(), // FOURNAISE
        arc8(), // GESTE DÉMONIAQUE
        arc9()  // TRÔNE DU NÉANT
    );

    private DungeonBossRoster() {}

    /** DÉCHARNÉS — boss de morts-vivants, squelettes, hollows. */
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

    /** FAUVES — créatures, bêtes, monstres de la nature. */
    private static List<BossEntry> arc1() {
        return List.of(
            s("minecraft:warden"),
            s("bosses_of_mass_destruction:void_blossom"),
            s("block_factorys_bosses:yeti"),
            s("block_factorys_bosses:sandworm"),
            s("block_factorys_bosses:kraken"),
            s("slu:boss_aatrox"),
            s("slu:boss_jax"),
            s("iceandfire:hydra"),
            s("cataclysm:scylla"),
            s("slu:boss_radahn")
        );
    }

    /** TRIBUS — guerriers, chevaliers, combattants organisés. */
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
            s("iceandfire:cyclops")
        );
    }

    /** LÉGION NOIRE — ténèbres, malédiction, âmes damnées. */
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
            s("cataclysm:the_harbinger"),
            s("iceandfire:dread_lich")
        );
    }

    /** ABYSSES — aquatique, corail, profondeurs. */
    private static List<BossEntry> arc4() {
        return List.of(
            d("slu:boss_godskin_apostle","slu:boss_godskin_noble"),
            s("bosses_of_mass_destruction:obsidilith"),
            s("cataclysm:coralssus"),
            s("cataclysm:wadjet"),
            s("iceandfire:sea_serpent"),
            d("block_factorys_bosses:pirate_captain","block_factorys_bosses:pirate_rook"),
            s("cataclysm:the_leviathan"),
            s("cataclysm:scylla"),
            s("iceandfire:hydra"),
            w("slu:boss_gael","slu:boss_radagon","slu:boss_elden_beast")
        );
    }

    /** CERCLE DES MAGES — magie, arcanes, sorcellerie. */
    private static List<BossEntry> arc5() {
        return List.of(
            d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"),
            s("slu:boss_gwyndolin"),
            s("bosses_of_mass_destruction:lich"),
            s("irons_spellbooks:citadel_keeper"),
            s("iceandfire:gorgon"),
            s("slu:boss_soul_of_cinder"),
            s("slu:boss_wukong"),
            s("cataclysm:netherite_monstrosity"),
            d("slu:boss_malenia","slu:boss_malenia_2"),
            w("slu:boss_gael","slu:boss_soul_of_cinder","slu:boss_artorias")
        );
    }

    /** MOISSON DE L'EFFROI — citrouilles, moissons, cauchemar féerique. */
    private static List<BossEntry> arc6() {
        return List.of(
            s("slu:boss_malenia"),
            s("slu:boss_malenia_2"),
            s("block_factorys_bosses:infernal_dragon"),
            d("slu:boss_nameless_king","slu:boss_wukong"),
            s("slu:boss_godskin_apostle"),
            s("slu:boss_godskin_noble"),
            w("slu:boss_gundyr","slu:boss_abyss_watcher"),
            s("iceandfire:cockatrice"),
            d("slu:boss_margit","slu:boss_morgott"),
            s("cataclysm:netherite_monstrosity")
        );
    }

    /** FOURNAISE — feu, lave, braises, forges infernales. */
    private static List<BossEntry> arc7() {
        return List.of(
            s("slu:boss_radahn"),
            s("slu:boss_radahn_2"),
            s("block_factorys_bosses:infernal_dragon"),
            s("cataclysm:netherite_monstrosity"),
            s("iceandfire:fire_dragon"),
            s("slu:boss_aatrox"),
            s("slu:boss_jax"),
            d("minecraft:warden","slu:boss_darius"),
            s("slu:boss_godskin_noble"),
            s("cataclysm:the_leviathan")
        );
    }

    /** GESTE DÉMONIAQUE — démons, abomination, fléau. */
    private static List<BossEntry> arc8() {
        return List.of(
            s("slu:boss_maliketh"),
            d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"),
            s("slu:boss_pantheon"),
            s("slu:boss_mohg"),
            s("iceandfire:ice_dragon"),
            s("cataclysm:the_harbinger"),
            d("slu:boss_fallen_lord","slu:boss_ancient_warrior"),
            s("slu:boss_dragon_slayer_armour"),
            d("slu:boss_count_robert","slu:boss_crucible_knight"),
            w("slu:boss_notch","slu:boss_minecraft_lord","slu:boss_pantheon")
        );
    }

    /** TRÔNE DU NÉANT — vide, end, gardiens de la fin. */
    private static List<BossEntry> arc9() {
        return List.of(
            w("slu:boss_gael","slu:boss_radagon","slu:boss_elden_beast"),
            s("slu:boss_pantheon"),
            s("slu:boss_wukong"),
            s("iceandfire:lightning_dragon"),
            s("cataclysm:ender_golem"),
            d("slu:boss_notch","slu:boss_minecraft_lord"),
            s("cataclysm:scylla"),
            w("slu:boss_radahn_2","slu:boss_malenia_2","slu:boss_elden_beast"),
            s("iceandfire:ghost"),
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
