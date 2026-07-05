package tong.statmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;
import tong.statmod.network.SyncHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

/**
 * Mission M6 — Système de points de donjon (2026-07-05).
 *
 * <p>Remplace le loot en cristaux (rune shards) : dans le Trial Dungeon, les mobs ne dropent plus
 * rien. À la place, tuer un mob/boss ou conquérir un étage accorde des <b>points</b>. Mourir en
 * fait perdre (mort punitive). Les points sont stockés dans {@link PlayerStatData#getDungeonPoints}
 * (persistés + synchronisés au client pour le HUD). L'échange des points à la sortie viendra plus
 * tard.
 */
public final class DungeonPoints {

    /** Points de base par mob tué, avant bonus d'étage. */
    private static final int BASE_MOB_POINTS = 5;
    /** Points par mob supplémentaires par tranche de 10 étages (profondeur = plus de valeur). */
    private static final int MOB_POINTS_PER_TIER = 2;

    /** Bonus de conquête d'un étage de combat/trésor. */
    private static final int FLOOR_CLEAR_POINTS = 25;
    /** Points par boss vaincu (gros gain). */
    private static final int BOSS_POINTS = 150;

    /** Fraction des points perdue à la mort (mort punitive). */
    private static final double DEATH_LOSS_FRACTION = 0.25;
    /** Perte minimale garantie à la mort (pour que la mort pique même à faible total). */
    private static final int DEATH_LOSS_MIN = 20;

    private DungeonPoints() {}

    /** Points gagnés pour un mob tué à l'étage {@code floor} (croît avec la profondeur). */
    public static int mobReward(int floor) {
        return BASE_MOB_POINTS + Math.max(0, floor / 10) * MOB_POINTS_PER_TIER;
    }

    /** Accorde des points au joueur, avec message action-bar et resync. */
    public static void award(ServerPlayer player, int amount, String reasonKey) {
        if (amount <= 0) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int total = data.addDungeonPoints(amount);
        SyncHelper.syncStats(player);
        player.displayClientMessage(Component.translatable(reasonKey, amount, total), true);
    }

    /** Récompense de kill de mob (points ∝ étage). */
    public static void awardMobKill(ServerPlayer player, int floor) {
        award(player, mobReward(floor), "dungeon.points.mob");
    }

    /** Récompense de conquête d'un étage (combat/trésor). */
    public static void awardFloorClear(ServerPlayer player) {
        award(player, FLOOR_CLEAR_POINTS, "dungeon.points.floor");
    }

    /** Récompense de boss vaincu. */
    public static void awardBoss(ServerPlayer player) {
        award(player, BOSS_POINTS, "dungeon.points.boss");
    }

    /**
     * Applique la pénalité de mort : perd une fraction des points (au moins {@link #DEATH_LOSS_MIN}).
     * No-op si le joueur n'a aucun point. Message + resync.
     */
    public static void applyDeathPenalty(ServerPlayer player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int current = data.getDungeonPoints();
        if (current <= 0) return;
        int loss = Math.max(DEATH_LOSS_MIN, (int) Math.ceil(current * DEATH_LOSS_FRACTION));
        loss = Math.min(loss, current);
        int total = data.addDungeonPoints(-loss);
        SyncHelper.syncStats(player);
        player.displayClientMessage(Component.translatable("dungeon.points.death", loss, total), false);
        STATMod.LOGGER.info("[TrialDungeon] {} perd {} points à la mort (reste {})",
                player.getGameProfile().getName(), loss, total);
    }
}
