package yesman.epicfight.api.neoevent.playerpatch;

import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

public abstract class DealDamageEvent extends yesman.epicfight.api.event.types.entity.DealDamageEvent {
    protected DealDamageEvent(LivingEntityPatch<?> entityPatch, LivingEntity target, EpicFightDamageSource damageSource, float damage) {
        super(entityPatch, target, damageSource, damage);
    }

    public static final class Post extends DealDamageEvent {
        private float modifiedDamage;

        public Post(LivingEntityPatch<?> entityPatch, LivingEntity target, EpicFightDamageSource damageSource, float damage) {
            super(entityPatch, target, damageSource, damage);
            this.modifiedDamage = damage;
        }

        public float getModifiedDamage() {
            return this.modifiedDamage;
        }

        public void setModifiedDamage(float modifiedDamage) {
            this.modifiedDamage = modifiedDamage;
        }
    }
}
