package yesman.epicfight.api.neoevent.playerpatch;

import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class SetTargetEvent extends yesman.epicfight.api.event.types.player.SetTargetEvent {
    public SetTargetEvent(ServerPlayerPatch playerPatch, LivingEntity target) {
        super(playerPatch, target);
    }
}
