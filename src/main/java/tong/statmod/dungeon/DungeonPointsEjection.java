package tong.statmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.stats.PlayerStats;

/**
 * Mission M6 — Règle « 0 point → éjection overworld » (2026-07-05).
 *
 * <p>Dès que les points de donjon d'un joueur tombent à 0 alors qu'il est dans le donjon (mort ou
 * conversion au changeur), il est renvoyé à l'overworld. Les coins du shop, eux, restent à l'abri.
 */
public final class DungeonPointsEjection {

    private DungeonPointsEjection() {}

    /** Prédicat pur : faut-il éjecter ? Vrai seulement si dans le donjon ET points == 0. */
    public static boolean shouldEject(boolean inDungeon, int points) {
        return inDungeon && points == 0;
    }

    /** Applique la règle pour {@code player} : si éligible, message + retour overworld. */
    public static void enforce(ServerPlayer player) {
        boolean inDungeon = player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON);
        int points = StatCapabilities.get(player).getDungeonPoints();
        if (!shouldEject(inDungeon, points)) return;
        player.displayClientMessage(Component.translatable("dungeon.ejected.no_points"), false);
        DungeonTeleportHandler.returnToOverworld(player);
    }
}
