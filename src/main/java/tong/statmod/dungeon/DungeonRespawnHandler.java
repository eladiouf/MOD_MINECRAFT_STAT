package tong.statmod.dungeon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import tong.statmod.STATMod;

import java.util.List;

/**
 * Mission M6 — On ne perd jamais son inventaire dans le Trial Dungeon (2026-07-04).
 *
 * <p>Dans un donjon d'entraînement, mourir ne doit <b>pas</b> coûter l'inventaire ni l'XP. Plutôt
 * que de laisser le joueur mourir puis le renvoyer (ce qui, avec les gamerules par défaut, drope
 * déjà son stuff et son XP), on <b>annule la mort</b> : le joueur est soigné, purgé de ses malus,
 * et renvoyé à l'étage 1. Aucun écran de mort, aucun drop, aucune perte d'XP.
 *
 * <p>Priorité {@code LOWEST} : on laisse les autres mods réagir à la mort d'abord (compat), et on
 * annule en dernier si personne ne l'a déjà fait.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonRespawnHandler {

    private DungeonRespawnHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        // Annule la mort → pas d'écran de mort, pas de drop d'inventaire/XP.
        event.setCanceled(true);

        // Remet le joueur en état de combattre : PV pleins, feu éteint, malus purgés, faim rétablie.
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.getFoodData().setFoodLevel(20);
        List<MobEffectInstance> effects = List.copyOf(player.getActiveEffects());
        for (MobEffectInstance effect : effects) {
            player.removeEffect(effect.getEffect());
        }
        player.setDeltaMovement(0, 0, 0);
        player.hurtTime = 0;
        player.hurtMarked = true;

        // Retour à l'étage 1 (recommence le run, mais avec tout son stuff).
        DungeonTeleportHandler.enterFloor(player, 1);

        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "dungeon.defeat.reset"), false);
        STATMod.LOGGER.info("[TrialDungeon] {} vaincu — renvoyé étage 1 (inventaire préservé)",
                player.getGameProfile().getName());
    }
}
