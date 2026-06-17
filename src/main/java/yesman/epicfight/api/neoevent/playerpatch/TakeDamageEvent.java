package yesman.epicfight.api.neoevent.playerpatch;

import net.minecraft.world.damagesource.DamageSource;
import yesman.epicfight.api.event.CancelableEvent;
import yesman.epicfight.api.event.LivingEntityPatchEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.math.ValueModifier.ResultCalculator;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.ArrayList;
import java.util.List;

public abstract class TakeDamageEvent extends LivingEntityPatchEvent {
    private final DamageSource damageSource;
    private final float damage;

    protected TakeDamageEvent(LivingEntityPatch<?> entityPatch, DamageSource damageSource, float damage) {
        super(entityPatch);
        this.damageSource = damageSource;
        this.damage = damage;
    }

    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    public float getDamage() {
        return this.damage;
    }

    public static final class Pre extends TakeDamageEvent {
        private final ResultCalculator resultCalculator;
        private final List<ValueModifier> modifiers = new ArrayList<>();

        public Pre(LivingEntityPatch<?> entityPatch, DamageSource damageSource, ResultCalculator resultCalculator, float damage) {
            super(entityPatch, damageSource, damage);
            this.resultCalculator = resultCalculator;
        }

        public void attachValueModifier(ValueModifier modifier) {
            this.modifiers.add(modifier);
        }

        public ResultCalculator getResultCalculator() {
            return this.resultCalculator;
        }

        public List<ValueModifier> getModifiers() {
            return this.modifiers;
        }
    }

    public static final class Post extends TakeDamageEvent {
        public Post(LivingEntityPatch<?> entityPatch, DamageSource damageSource, float damage) {
            super(entityPatch, damageSource, damage);
        }
    }

    public static final class Income extends TakeDamageEvent implements CancelableEvent {
        private AttackResult.ResultType result = AttackResult.ResultType.SUCCESS;
        private boolean parried;

        public Income(LivingEntityPatch<?> entityPatch, DamageSource damageSource, float damage) {
            super(entityPatch, damageSource, damage);
        }

        public AttackResult.ResultType getResult() {
            return this.result;
        }

        public void setResult(AttackResult.ResultType result) {
            this.result = result;
        }

        public boolean isParried() {
            return this.parried;
        }

        public void setParried(boolean parried) {
            this.parried = parried;
        }
    }
}
