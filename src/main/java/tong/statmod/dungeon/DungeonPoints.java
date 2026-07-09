package tong.statmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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

    /** Points minimum garantis pour n'importe quel kill. */
    private static final int MOB_POINTS_MIN = 3;
    /** Points par point de « difficulté » du mob (voir {@link #difficultyRating}). */
    private static final double POINTS_PER_DIFFICULTY = 0.6;
    /** Bonus multiplicatif de profondeur : +5 % de points par étage franchi. */
    private static final double DEPTH_BONUS_PER_FLOOR = 0.05;
    /** Plafond de points par mob (anti-abus si un mob a des PV délirants). */
    private static final int MOB_POINTS_CAP = 400;

    /** Bonus de conquête d'un étage de combat/trésor. */
    private static final int FLOOR_CLEAR_POINTS = 25;
    /** Points par boss vaincu (gros gain). */
    private static final int BOSS_POINTS = 150;

    /** Fraction des points perdue à la mort (mort punitive). */
    private static final double DEATH_LOSS_FRACTION = 0.25;
    /** Perte minimale garantie à la mort (pour que la mort pique même à faible total). */
    private static final int DEATH_LOSS_MIN = 20;

    private DungeonPoints() {}

    /**
     * « Note de difficulté » d'un mob, dérivée de ses stats réelles au moment du kill : surtout
     * ses PV max (signal universel qui capte les élites, les boss ET le scaling L2 Hostility),
     * plus un peu de dégâts d'attaque. Un gobelin ≈ 5, un chevalier ≈ 30, un colosse ≈ 200+.
     */
    public static double difficultyRating(LivingEntity mob) {
        double hp = mob.getMaxHealth();                 // PV max (10 vanilla → centaines pour un boss)
        double atk = attackDamage(mob);                 // dégâts d'attaque (0 si non armé)
        double armor = mob.getArmorValue();             // armure (tanky = plus dur)
        // Pondération : les PV dominent, l'attaque et l'armure ajustent.
        return hp + atk * 3.0 + armor * 1.5;
    }

    /** Dégâts d'attaque du mob (attribut ATTACK_DAMAGE), ou 0 si absent. */
    private static double attackDamage(LivingEntity mob) {
        var attr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        return attr != null ? attr.getValue() : 0.0;
    }

    /**
     * Points gagnés pour un mob tué : proportionnels à sa difficulté réelle, amplifiés par la
     * profondeur de l'étage. Toujours ≥ {@link #MOB_POINTS_MIN}, plafonnés à {@link #MOB_POINTS_CAP}.
     */
    public static int mobReward(LivingEntity mob, int floor) {
        double base = difficultyRating(mob) * POINTS_PER_DIFFICULTY;
        double depthMult = 1.0 + Math.max(0, floor - 1) * DEPTH_BONUS_PER_FLOOR;
        int pts = (int) Math.round(base * depthMult);
        return Math.max(MOB_POINTS_MIN, Math.min(MOB_POINTS_CAP, pts));
    }

    /** Accorde des points au joueur, avec message action-bar et resync. */
    public static void award(ServerPlayer player, int amount, String reasonKey) {
        if (amount <= 0) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        int total = data.addDungeonPoints(amount);
        SyncHelper.syncStats(player);
        player.displayClientMessage(Component.translatable(reasonKey, amount, total), true);
    }

    /**
     * Récompense de kill de mob (points ∝ difficulté du mob × profondeur), amplifiée par la
     * couche « Dungeon Rush » : combo de kills (multiplicateur croissant, brisé quand on encaisse
     * un coup) et jackpot aléatoire (petite chance de ×{@value DungeonRush#JACKPOT_MULTIPLIER}).
     */
    public static void awardMobKill(ServerPlayer player, LivingEntity mob, int floor) {
        int base = mobReward(mob, floor);

        // Combo : chaque kill dans la fenêtre fait monter le multiplicateur.
        int combo = DungeonRush.onKill(player.getUUID(), player.level().getGameTime());
        int pts = (int) Math.round(base * DungeonRush.comboMultiplier(combo));

        // Jackpot : renforcement variable — le kill banal qui explose en pluie de points.
        boolean jackpot = DungeonRush.isJackpot(player.getRandom().nextDouble());
        if (jackpot) pts *= DungeonRush.JACKPOT_MULTIPLIER;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int total = data.addDungeonPoints(pts);
        SyncHelper.syncStats(player);

        if (jackpot) {
            player.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
            player.displayClientMessage(Component.translatable("dungeon.rush.jackpot", pts, total), true);
        } else if (combo >= 2) {
            player.displayClientMessage(Component.translatable(
                    "dungeon.rush.combo", pts, combo,
                    String.format("%.2f", DungeonRush.comboMultiplier(combo)), total), true);
        } else {
            player.displayClientMessage(Component.translatable("dungeon.points.mob", pts, total), true);
        }

        // Fanfare de palier de combo : ping de plus en plus aigu tous les 5 kills — la montée.
        if (DungeonRush.isComboMilestone(combo)) {
            float pitch = Math.min(2.0f, 1.0f + combo * 0.04f);
            player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, pitch);
        }

        // Co-op : les coéquipiers (même équipe FTB) présents sur l'étage touchent une part
        // d'assist (points bruts, sans combo ni jackpot — le style, c'est personnel). Les rivaux
        // d'une autre équipe ne touchent rien. Jouer ensemble doit payer.
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            int share = assistShare(base);
            if (share > 0) {
                for (ServerPlayer mate : DungeonTeleportHandler.playersOnFloor(sl, floor)) {
                    if (mate == player) continue;
                    if (!tong.statmod.integration.ftbteams.FTBTeamsBridge.sameTeam(player, mate)) continue;
                    int mateTotal = mate.getData(ModAttachments.STATS).addDungeonPoints(share);
                    SyncHelper.syncStats(mate);
                    mate.displayClientMessage(Component.translatable(
                            "dungeon.rush.assist", share, mateTotal), true);
                }
            }
        }
    }

    /** Part d'assist co-op : 40 % des points de base du kill (0 si le kill ne vaut rien). */
    static int assistShare(int basePoints) {
        return (int) Math.floor(basePoints * 0.4);
    }

    /** Récompense de conquête d'un étage (combat/trésor), ×{@code multiplier} (sans-faute…). */
    public static void awardFloorClear(ServerPlayer player, int multiplier) {
        award(player, FLOOR_CLEAR_POINTS * Math.max(1, multiplier), "dungeon.points.floor");
    }

    /** Récompense de boss vaincu, ×{@code multiplier} (sans-faute…). */
    public static void awardBoss(ServerPlayer player, int multiplier) {
        award(player, BOSS_POINTS * Math.max(1, multiplier), "dungeon.points.boss");
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
