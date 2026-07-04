package tong.statmod.dungeon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mission M6 — Roster prédéfini de 100 boss (étages 10→1000).
 *
 * <p>{@code s(id)} = simple (1 boss), {@code d(id1, id2)} = duo simultané,
 * {@code w(id...)} = vague séquentielle.
 */
public final class DungeonBossRoster {

    public record BossEntry(String entityId, int mode) {} // 0=simple, 1=duo, 2=wave

    private static final Map<Integer, List<BossEntry>> ROSTER = new LinkedHashMap<>();

    static {
        // ÉTAGES 10-100
        R(10,  s("minecraft:wither_skeleton"));
        R(20,  s("minecraft:warden"));
        R(30,  s("slu:boss_artorias"));
        R(40,  d("slu:boss_ornstein","slu:boss_smough"));
        R(50,  d("slu:boss_margit","slu:boss_morgott"));
        R(60,  d("slu:boss_godskin_apostle","slu:boss_godskin_noble"));
        R(70,  s("slu:boss_malenia"));
        R(80,  w("slu:boss_radahn","slu:boss_radahn_2"));
        R(90,  s("slu:boss_maliketh"));
        R(100, w("slu:boss_gael","slu:boss_radagon","slu:boss_elden_beast"));
        // ÉTAGES 110-200
        R(110, s("slu:boss_mohg"));
        R(120, s("slu:boss_nameless_king"));
        R(130, w("slu:boss_godfrey","slu:boss_hoarah_loux"));
        R(140, s("slu:boss_dragon_slayer_armour"));
        R(150, d("slu:boss_notch","slu:boss_minecraft_lord"));
        R(160, s("slu:boss_soul_of_cinder"));
        R(170, w("slu:boss_gundyr","slu:boss_abyss_watcher"));
        R(180, s("slu:boss_wukong"));
        R(190, d("slu:boss_count_robert","slu:boss_crucible_knight"));
        R(200, s("slu:boss_pantheon"));
        // ÉTAGES 210-300
        R(210, s("slu:boss_fallen_lord"));
        R(220, s("slu:boss_aatrox"));
        R(230, d("slu:boss_looking_glass_knight","slu:boss_darius"));
        R(240, w("slu:boss_beast_clergyman","slu:boss_malenia"));
        R(250, d("slu:boss_elden_beast","slu:boss_radagon"));
        R(260, s("slu:boss_gwyndolin"));
        R(270, d("slu:boss_elemer","slu:boss_ancient_warrior"));
        R(280, s("slu:boss_jax"));
        R(290, d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"));
        R(300, w("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper")); // fix: apocalypse_golem n'existe pas
        // ── BFB Bosses (310-360) — block_factorys_bosses ──
        R(310, s("block_factorys_bosses:infernal_dragon"));
        R(320, s("block_factorys_bosses:yeti"));
        R(330, s("block_factorys_bosses:sandworm"));
        R(340, s("block_factorys_bosses:underworld_knight"));
        R(350, s("block_factorys_bosses:kraken"));
        R(360, d("block_factorys_bosses:pirate_captain","block_factorys_bosses:pirate_rook"));
        // ÉTAGES 370-400 — rotation SLU (310-360 réservés aux boss BFB ci-dessus)
        R(370,d("slu:boss_mohg","slu:boss_maliketh"));
        R(380,w("slu:boss_nameless_king","slu:boss_wukong"));
        R(390,d("slu:boss_dragon_slayer_armour","slu:boss_looking_glass_knight"));
        R(400,w("slu:boss_notch","slu:boss_minecraft_lord","slu:boss_pantheon"));
        // ÉTAGES 410-500
        R(410,d("slu:boss_aatrox","slu:boss_jax"));
        R(420,w("slu:boss_gundyr","slu:boss_abyss_watcher"));
        R(430,d("slu:boss_fallen_lord","slu:boss_ancient_warrior"));
        R(440,w("slu:boss_beast_clergyman","slu:boss_elemer"));
        R(450,d("slu:boss_count_robert","slu:boss_crucible_knight"));
        R(460,w("minecraft:warden","slu:boss_darius"));
        R(470,d("slu:boss_radagon","slu:boss_elden_beast"));
        R(480,w("slu:boss_godfrey","slu:boss_hoarah_loux"));
        R(490,d("slu:boss_malenia","slu:boss_malenia_2"));
        R(500,w("slu:boss_gael","slu:boss_soul_of_cinder","slu:boss_artorias"));
        // ÉTAGES 510-600
        R(510,d("slu:boss_radahn","slu:boss_margit"));
        R(520,w("slu:boss_nameless_king","slu:boss_ornstein","slu:boss_smough"));
        R(530,d("slu:boss_maliketh","slu:boss_mohg"));
        R(540,w("slu:boss_godskin_apostle","slu:boss_godskin_noble"));
        R(550,d("slu:boss_wukong","slu:boss_jax"));
        R(560,w("slu:boss_abyss_watcher","slu:boss_dragon_slayer_armour"));
        R(570,d("slu:boss_morgott","slu:boss_hoarah_loux"));
        R(580,w("slu:boss_pantheon","slu:boss_notch","slu:boss_minecraft_lord"));
        R(590,d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"));
        R(600,w("irons_spellbooks:citadel_keeper","minecraft:warden"));
        // ÉTAGES 610-700
        R(610,w("slu:boss_radahn_2","slu:boss_malenia_2","slu:boss_elden_beast"));
        R(620,d("slu:boss_malenia","slu:boss_maliketh"));
        R(630,w("slu:boss_gael","slu:boss_soul_of_cinder"));
        R(640,d("slu:boss_aatrox","slu:boss_fallen_lord"));
        R(650,w("slu:boss_godfrey","slu:boss_margit","slu:boss_morgott"));
        R(660,d("slu:boss_beast_clergyman","slu:boss_elemer"));
        R(670,w("slu:boss_radagon","slu:boss_nameless_king"));
        R(680,d("slu:boss_looking_glass_knight","slu:boss_crucible_knight"));
        R(690,w("slu:boss_gundyr","slu:boss_ancient_warrior"));
        R(700,w("slu:boss_notch","slu:boss_minecraft_lord","slu:boss_pantheon","slu:boss_wukong"));
        // ÉTAGES 710-800
        R(710,d("slu:boss_artorias","slu:boss_abyss_watcher"));
        R(720,w("slu:boss_soul_of_cinder","slu:boss_gael","slu:boss_malenia"));
        R(730,d("slu:boss_radahn_2","slu:boss_dragon_slayer_armour"));
        R(740,w("slu:boss_maliketh","slu:boss_mohg"));
        R(750,d("slu:boss_godskin_apostle","slu:boss_godskin_noble"));
        R(760,w("slu:boss_ornstein","slu:boss_smough","slu:boss_count_robert"));
        R(770,d("slu:boss_elden_beast","slu:boss_radagon"));
        R(780,w("slu:boss_hoarah_loux","slu:boss_godfrey"));
        R(790,d("slu:boss_malenia_2","slu:boss_jax"));
        R(800,w("slu:boss_gael","slu:boss_radahn","slu:boss_artorias","slu:boss_soul_of_cinder"));
        // ÉTAGES 810-900
        R(810,d("slu:boss_margit","slu:boss_morgott"));
        R(820,w("slu:boss_nameless_king","slu:boss_wukong","slu:boss_abyss_watcher"));
        R(830,d("slu:boss_notch","slu:boss_minecraft_lord"));
        R(840,w("slu:boss_malenia","slu:boss_malenia_2"));
        R(850,d("slu:boss_radahn","slu:boss_radahn_2"));
        R(860,w("minecraft:warden","slu:boss_darius","slu:boss_elemer"));
        R(870,d("slu:boss_pantheon","slu:boss_fallen_lord"));
        R(880,w("slu:boss_beast_clergyman","slu:boss_ancient_warrior"));
        R(890,d("slu:boss_looking_glass_knight","slu:boss_crucible_knight"));
        R(900,w("slu:boss_gael","slu:boss_artorias","slu:boss_soul_of_cinder","slu:boss_malenia","slu:boss_radahn"));
        // ÉTAGES 910-1000 — boss ultimes
        R(910,d("slu:boss_godskin_apostle","slu:boss_godskin_noble"));
        R(920,w("slu:boss_maliketh","slu:boss_mohg","slu:boss_godfrey"));
        R(930,d("slu:boss_elden_beast","slu:boss_radagon"));
        R(940,w("slu:boss_hoarah_loux","slu:boss_nameless_king"));
        R(950,d("slu:boss_radahn_2","slu:boss_malenia_2"));
        R(960,w("slu:boss_wukong","slu:boss_abyss_watcher","slu:boss_dragon_slayer_armour"));
        R(970,d("slu:boss_notch","slu:boss_pantheon"));
        R(980,w("slu:boss_minecraft_lord","slu:boss_gael","slu:boss_soul_of_cinder"));
        R(990,d("irons_spellbooks:dead_king","irons_spellbooks:citadel_keeper"));
        R(1000,w("slu:boss_malenia","slu:boss_radahn","slu:boss_artorias","slu:boss_gael","slu:boss_soul_of_cinder"));
    }

    static void R(int f, BossEntry... e) { ROSTER.put(f, List.of(e)); }
    static BossEntry s(String id) { return new BossEntry(id, 0); }
    static BossEntry d(String id1, String id2) { return new BossEntry(id1+","+id2, 1); }
    static BossEntry w(String... ids) { return new BossEntry(String.join(",", ids), 2); }

    public static List<BossEntry> forFloor(int floor) {
        return ROSTER.getOrDefault(floor, List.of(s("minecraft:wither_skeleton")));
    }

    private DungeonBossRoster() {}
}
