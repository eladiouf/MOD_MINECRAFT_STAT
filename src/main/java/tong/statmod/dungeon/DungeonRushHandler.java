package tong.statmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import tong.statmod.STATMod;

/**
 * « Dungeon Rush » (2026-07-09) — câblage évènementiel de {@link DungeonRush}.
 *
 * <p>Encaisser un coup dans le Trial Dungeon brise le combo et fait perdre le sans-faute de
 * l'étage. Le feedback n'est joué que si le joueur avait un combo qui valait quelque chose
 * (≥ 5) — perdre un combo de 2 ne mérite pas une punition sonore.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonRushHandler {

    /** Combo minimal pour que sa perte mérite un feedback (son + message). */
    private static final int COMBO_LOSS_FEEDBACK_THRESHOLD = 5;

    private DungeonRushHandler() {}

    @SubscribeEvent
    public static void onPlayerDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;
        if (event.getNewDamage() <= 0.0f) return;

        int lostCombo = DungeonRush.currentCombo(player.getUUID());
        DungeonRush.onHit(player.getUUID());

        if (lostCombo >= COMBO_LOSS_FEEDBACK_THRESHOLD) {
            player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.7f, 0.6f);
            player.displayClientMessage(
                    Component.translatable("dungeon.rush.combo_broken", lostCombo), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DungeonRush.clear(event.getEntity().getUUID());
        DungeonRecords.clear(event.getEntity().getUUID());
    }
}
