package tong.statmod.integration.playerrevive;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import team.creative.playerrevive.api.event.PlayerRevivedEvent;
import tong.statmod.dungeon.DungeonDimensions;

/**
 * Intégration optionnelle du mod PlayerRevive (2026-07-08).
 * Permet aux joueurs d'être réanimés par les feux de camp du donjon et octroie une grâce à la réanimation.
 */
public final class PlayerReviveIntegration {

    private PlayerReviveIntegration() {}

    public static void init() {
        if (ModList.get().isLoaded("playerrevive")) {
            NeoForge.EVENT_BUS.register(PlayerReviveIntegration.class);
        }
    }

    @SubscribeEvent
    public static void onPlayerRevived(PlayerRevivedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
                // Grâce de réanimation : 5 secondes d'invulnérabilité.
                player.invulnerableTime = 100;
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));
                player.displayClientMessage(Component.translatable("dungeon.revived.resistance"), true);
            }
        }
    }

    public static void tryHealSpotRevive(ServerPlayer player) {
        if (ModList.get().isLoaded("playerrevive")) {
            try {
                Helper.tryRevive(player);
            } catch (Throwable t) {
                // Évite tout crash si le mod est absent en cours de route.
            }
        }
    }

    private static class Helper {
        static void tryRevive(ServerPlayer player) {
            var cap = player.getData(team.creative.playerrevive.PlayerRevive.BLEEDING.get());
            if (cap != null && cap.isBleeding()) {
                cap.revive(player);
                player.displayClientMessage(Component.translatable("dungeon.revived.campfire"), true);
            }
        }
    }
}
