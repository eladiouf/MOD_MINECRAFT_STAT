package tong.statmod.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;

@Mixin(SkillBuilder.class)
public abstract class EpicFightSkillBuilderMixin {
    @SuppressWarnings("unchecked")
    public <T extends Skill> T build(ResourceLocation registryName, Class<T> skillClass) {
        return (T) ((SkillBuilder<?>) (Object) this).build(registryName);
    }
}
