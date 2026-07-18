package tong.statmod.dungeon.ai.living;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonLivingInteractionEvents {
    private DungeonLivingInteractionEvents() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof Mob prisoner)
                || DungeonLivingActor.role(prisoner).orElse(null) != DungeonLivingRole.PRISONER
                || prisoner.getPersistentData().getBoolean(
                        DungeonLivingActor.PRISONER_RELEASED_TAG)) return;
        prisoner.getPersistentData().putBoolean(DungeonLivingActor.PRISONER_RELEASED_TAG, true);
        prisoner.getPersistentData().putString(DungeonLivingActor.PRISONER_OWNER_TAG,
                event.getEntity().getUUID().toString());
        prisoner.setCustomName(Component.literal("Prisonnier libéré"));
        event.getEntity().displayClientMessage(
                Component.literal("Le prisonnier va désormais vous suivre."), true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
