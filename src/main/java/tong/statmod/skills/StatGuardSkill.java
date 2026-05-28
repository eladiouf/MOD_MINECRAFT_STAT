package tong.statmod.skills;

import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Guard skills — defensive blocking with three tiers:
 * parry (counter), fortress (stability), iron_wall (reflection).
 */
public class StatGuardSkill extends GuardSkill {

    private final GuardType type;

    public enum GuardType {
        PARRY,      // Parry window +20%, counter +50% damage
        FORTRESS,   // Blocking: -40% damage, +30% stability
        IRON_WALL   // Blocking: -60% damage, reflect 20%
    }

    public StatGuardSkill(Builder builder, GuardType type) {
        super(builder);
        this.type = type;
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        // Guard effects are applied in combat events (PerkEffectHandler)
        // This class registers the skill; the actual blocking logic is event-driven
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
    }

    public GuardType getGuardType() { return type; }
}
