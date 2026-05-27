package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import yesman.epicfight.skill.Skill;

public class ShadowStepSkill extends Skill {
    public static final ShadowStepSkill INSTANCE = new ShadowStepSkill();

    private ShadowStepSkill() {
        super(Skill.createBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "shadow_step")));
    }
}
