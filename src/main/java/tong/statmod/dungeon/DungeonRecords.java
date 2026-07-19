package tong.statmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.stats.PlayerStats;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records personnels du Trial Dungeon (2026-07-09) — le hook « bats ton propre fantôme ».
 *
 * <p>Deux records persistés dans {@link PlayerStats} :
 * <ul>
 *   <li><b>Meilleur combo</b> — comparé à chaque kill (célébré à partir de
 *       {@value #COMBO_RECORD_FLOOR} pour ne pas fanfaronner sur un combo de 2) ;</li>
 *   <li><b>Meilleur temps de nettoyage</b> d'un étage de combat — chrono démarré à l'entrée
 *       ({@link #onFloorEnter}), arrêté à la conquête {@code CLEAR_WAVE}.</li>
 * </ul>
 * Battre un record → titre « ★ NOUVEAU RECORD » + son. Le chrono en mémoire ne survit pas à la
 * session (un record de vitesse se fait d'une traite, c'est le principe).
 */
public final class DungeonRecords {

    /** Combo minimal pour qu'un record de combo soit célébré. */
    static final int COMBO_RECORD_FLOOR = 5;

    private record FloorTimer(int floor, long startTick) {}

    private static final Map<UUID, FloorTimer> TIMERS = new ConcurrentHashMap<>();

    private DungeonRecords() {}

    // ── Fonctions pures (testables) ──────────────────────────────────────────

    /** {@code true} si {@code combo} bat {@code best} et mérite une célébration. */
    static boolean isComboRecord(int combo, int best) {
        return combo >= COMBO_RECORD_FLOOR && combo > best;
    }

    /** {@code true} si {@code clearTicks} bat {@code best} (0 = pas encore de record). */
    static boolean isClearRecord(int clearTicks, int best) {
        return clearTicks > 0 && (best == 0 || clearTicks < best);
    }

    /** Format mm:ss.t d'une durée en ticks. */
    static String formatTicks(int ticks) {
        int totalTenths = ticks / 2; // 20 ticks/s → 2 ticks par dixième
        int tenths = totalTenths % 10;
        int seconds = (totalTenths / 10) % 60;
        int minutes = totalTenths / 600;
        return String.format("%d:%02d.%d", minutes, seconds, tenths);
    }

    // ── Câblage ──────────────────────────────────────────────────────────────

    /** Démarre le chrono de l'étage (appelé par {@code enterFloor}). */
    public static void onFloorEnter(ServerPlayer player, int floor, long gameTick) {
        TIMERS.put(player.getUUID(), new FloorTimer(floor, gameTick));
    }

    /** Un kill vient de porter le combo à {@code combo} : record ? */
    public static void onCombo(ServerPlayer player, int combo) {
        PlayerStats data = StatCapabilities.get(player);
        if (!isComboRecord(combo, data.getDungeonBestCombo())) return;
        data.setDungeonBestCombo(combo);
        celebrate(player, Component.translatable("dungeon.record.combo", combo));
    }

    /** L'étage {@code floor} vient d'être conquis : record de vitesse (combat uniquement) ? */
    public static void onFloorClear(ServerPlayer player, int floor, DungeonObjective objective, long gameTick) {
        FloorTimer timer = TIMERS.remove(player.getUUID());
        if (objective != DungeonObjective.CLEAR_WAVE) return;
        if (timer == null || timer.floor() != floor) return;

        int clearTicks = (int) Math.max(1, gameTick - timer.startTick());
        PlayerStats data = StatCapabilities.get(player);
        if (!isClearRecord(clearTicks, data.getDungeonBestClearTicks())) return;
        data.setDungeonBestClearTicks(clearTicks);
        celebrate(player, Component.translatable("dungeon.record.clear", formatTicks(clearTicks)));
    }

    /** Oublie le chrono (logout). */
    public static void clear(UUID playerId) {
        TIMERS.remove(playerId);
    }

    private static void celebrate(ServerPlayer player, Component subtitle) {
        DungeonProgress.title(player, Component.translatable("dungeon.record.title"), subtitle);
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);
        player.playNotifySound(SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.9f, 1.1f);
    }
}
