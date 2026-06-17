package yesman.epicfight.api.neoevent;

import net.neoforged.bus.api.Event;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class InitAnimatorEvent extends Event {
    private final LivingEntityPatch<?> entityPatch;
    private final Animator animator;

    public InitAnimatorEvent(LivingEntityPatch<?> entityPatch, Animator animator) {
        this.entityPatch = entityPatch;
        this.animator = animator;
    }

    public LivingEntityPatch<?> getEntityPatch() {
        return this.entityPatch;
    }

    public Animator getAnimator() {
        return this.animator;
    }
}
