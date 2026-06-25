package tong.statmod.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import tong.statmod.stats.StatType;

/**
 * Affiche un actionbar message lisible quand le joueur level-up une stat.
 * Centralise le formatage pour ne pas dupliquer le code dans les ~10 handlers d'XP.
 *
 * <p>Format : {@code Stat Name → Lv. N} en jaune Volt.
 */
public final class LevelUpFeedback {
    private LevelUpFeedback() {}

    public static void notify(Player player, int statIndex, int newEffectiveLevel) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        StatType stat = StatType.byIndex(statIndex);
        if (stat == null) return;

        Component message = Component.literal("§e✦ §f" + stat.displayName + " §7→ §eLv. " + newEffectiveLevel);
        serverPlayer.displayClientMessage(message, true); // true = actionbar
    }
}
