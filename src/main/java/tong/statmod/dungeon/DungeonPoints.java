package tong.statmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import tong.statmod.StatMod;
import tong.statmod.network.SyncHelper;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.stats.PlayerStats;


/**
 * Mission M6 — Système de points de donjon (2026-07-05).
 *
 * <p>Remplace le loot en cristaux (rune shards) : dans le Trial Dungeon, les mobs ne dropent plus
 * rien. À la place, tuer un mob/boss ou conquérir un étage accorde des <b>points</b>. Mourir en
 * fait perdre (mort punitive). Les points sont stockés dans {@link PlayerStats#getDungeonPoints}
 * (persistés + synchronisés au client pour le HUD). L'échange des points à la sortie viendra plus
 * tard.
 */
public final class DungeonPoints {

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
     * Points gagnés pour un mob tué : difficulté réelle et profondeur sont combinées par la
     * politique centrale, avec minimum et plafond progressifs anti-abus.
     */
    public static int mobReward(LivingEntity mob, int floor) {
        return DungeonPointBalance.mobReward(difficultyRating(mob), floor);
    }

    /** Accorde des points au joueur, avec message action-bar et resync. */
    public static void award(ServerPlayer player, int amount, String reasonKey) {
        if (amount <= 0) return;
        PlayerStats data = StatCapabilities.get(player);
        int total = data.addDungeonPoints(amount);
        SyncHelper.syncStats(player);
        player.displayClientMessage(Component.translatable(reasonKey, amount, total), true);
    }

    /**
     * Récompense de kill de mob (points ∝ difficulté réelle du mob), amplifiée par la
     * couche « Dungeon Rush » : combo de kills (multiplicateur croissant, brisé quand on encaisse
     * un coup) et jackpot aléatoire (petite chance de ×{@value DungeonRush#JACKPOT_MULTIPLIER}).
     * Les coéquipiers sur l'étage touchent une part d'assist (40 % des points de base du kill).
     */
    public static void awardMobKill(ServerPlayer player, LivingEntity mob, int floor) {
        int base = mobReward(mob, floor);

        // Combo : chaque kill dans la fenêtre fait monter le multiplicateur.
        int combo = DungeonRush.onKill(player.getUUID(), player.level().getGameTime());
        int pts = (int) Math.round(base * DungeonRush.comboMultiplier(combo));

        // Jackpot : renforcement variable — le kill banal qui explose en pluie de points.
        boolean jackpot = DungeonRush.isJackpot(player.getRandom().nextDouble());
        if (jackpot) pts *= DungeonRush.JACKPOT_MULTIPLIER;

        PlayerStats data = StatCapabilities.get(player);
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

        // Record personnel de combo ? (célébré à partir de 5 — battre son fantôme.)
        DungeonRecords.onCombo(player, combo);

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
                for (ServerPlayer mate : tong.statmod.integration.ftbteams.FTBTeamsBridge
                        .teammatesOnFloor(player, sl, floor)) {
                    if (mate == player) continue;
                    int mateTotal = StatCapabilities.get(mate).addDungeonPoints(share);
                    SyncHelper.syncStats(mate);
                    mate.displayClientMessage(Component.translatable(
                            "dungeon.rush.assist", share, mateTotal), true);
                }
            }
        }
    }

    /** Part d'assist co-op : 40 % des points de base du kill (0 si le kill ne vaut rien). */
    static int assistShare(int basePoints) {
        return DungeonPointBalance.assistShare(basePoints);
    }

    /** Récompense de conquête d'un étage (combat/trésor), ×{@code multiplier} (sans-faute…). */
    public static void awardFloorClear(ServerPlayer player, int floor, int multiplier) {
        award(player, DungeonPointBalance.floorClearReward(floor) * Math.max(1, multiplier),
                "dungeon.points.floor");
    }

    /** Récompense de boss vaincu, ×{@code multiplier} (sans-faute…). */
    public static void awardBoss(ServerPlayer player, int floor, int multiplier) {
        award(player, DungeonPointBalance.bossReward(floor) * Math.max(1, multiplier),
                "dungeon.points.boss");
    }

    /**
     * Applique la pénalité de mort progressive de l'étage.
     * No-op si le joueur n'a aucun point. Message + resync.
     */
    public static void applyDeathPenalty(ServerPlayer player, int floor) {
        PlayerStats data = StatCapabilities.get(player);
        int current = data.getDungeonPoints();
        if (current <= 0) return;
        int loss = DungeonPointBalance.deathLoss(current, floor);
        int total = data.addDungeonPoints(-loss);
        SyncHelper.syncStats(player);
        player.displayClientMessage(Component.translatable("dungeon.points.death", loss, total), false);
        StatMod.LOGGER.info("[TrialDungeon] {} perd {} points à la mort (reste {})",
                player.getGameProfile().getName(), loss, total);
    }
}
