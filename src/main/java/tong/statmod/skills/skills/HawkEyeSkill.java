package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import yesman.epicfight.skill.Skill;

public class HawkEyeSkill extends Skill {
    public static final HawkEyeSkill INSTANCE = new HawkEyeSkill();

    private HawkEyeSkill() {
        super(Skill.createBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "hawk_eye")));
    }
}
