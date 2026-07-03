package tong.statmod.dungeon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mission M6 — Roster prédéfini des boss par étage.
 *
 * <p>Chaque étage boss (multiple de 10) a une liste ordonnée de boss.
 * Si {@code wave = false}, tous les boss de l'étage spawnent simultanément.
 * Si {@code wave = true}, les boss spawnent séquentiellement (le suivant après la mort du précédent).
 */
public final class DungeonBossRoster {

    public record BossEntry(String entityId, boolean wave) {}

    private static final Map<Integer, List<BossEntry>> ROSTER = new LinkedHashMap<>();

    static {
        R(10,  w("minecraft:wither_skeleton"));
        R(20,  w("minecraft:warden"));
        R(30,  s("slu:boss_artorias"));
        R(40,  s("slu:boss_ornstein"), s("slu:boss_smough"));
        R(50,  s("slu:boss_margit"), s("slu:boss_morgott"));
        R(60,  s("slu:boss_godskin_apostle"), s("slu:boss_godskin_noble"));
        R(70,  s("slu:boss_malenia"));
        R(80,  w("slu:boss_radahn"), w("slu:boss_radahn_2"));
        R(90,  s("slu:boss_maliketh"));
        R(100, w("slu:boss_gael"), w("slu:boss_radagon"), w("slu:boss_elden_beast"));
        R(110, s("slu:boss_mohg"));
        R(120, s("slu:boss_nameless_king"));
        R(130, w("slu:boss_godfrey"), w("slu:boss_hoarah_loux"));
        R(140, s("slu:boss_dragon_slayer_armour"));
        R(150, s("slu:boss_notch"), s("slu:boss_minecraft_lord"));
        R(160, s("slu:boss_soul_of_cinder"));
        R(170, w("slu:boss_gundyr"), w("slu:boss_abyss_watcher"));
        R(180, s("slu:boss_wukong"));
        R(190, w("slu:boss_count_robert"), w("slu:boss_crucible_knight"));
        R(200, s("slu:boss_pantheon"));
        R(210, s("slu:boss_fallen_lord"));
        R(220, s("slu:boss_aatrox"));
        R(230, w("slu:boss_looking_glass_knight"), w("slu:boss_darius"));
        R(240, s("slu:boss_beast_clergyman"));
        R(250, s("slu:boss_elden_beast"), s("slu:boss_radagon"));
        R(260, s("slu:boss_gwyndolin"));
        R(270, w("slu:boss_elemer"), w("slu:boss_ancient_warrior"));
        R(280, s("slu:boss_jax"));
        R(290, w("irons_spellbooks:dead_king"), w("irons_spellbooks:citadel_keeper"));
        R(300, s("irons_spellbooks:apocalypse_golem"));
    }

    private static void R(int floor, BossEntry... entries) {
        ROSTER.put(floor, List.of(entries));
    }

    private static BossEntry s(String id) { return new BossEntry(id, false); }
    private static BossEntry w(String id) { return new BossEntry(id, true); }

    public static List<BossEntry> forFloor(int floor) {
        return ROSTER.getOrDefault(floor, List.of(new BossEntry("minecraft:wither_skeleton", false)));
    }

    private DungeonBossRoster() {}
}
