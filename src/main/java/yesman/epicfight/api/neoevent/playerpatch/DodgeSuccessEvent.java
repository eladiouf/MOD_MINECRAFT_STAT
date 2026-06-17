package yesman.epicfight.api.neoevent.playerpatch;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class DodgeSuccessEvent extends yesman.epicfight.api.event.types.entity.DodgeEvent {
    public DodgeSuccessEvent(LivingEntityPatch<?> entityPatch, DamageSource damageSource, Vec3 location) {
        super(entityPatch, damageSource, location);
    }
}
