package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import yesman.epicfight.skill.Skill;

public class IaijutsuSkill extends Skill {
    public static final IaijutsuSkill INSTANCE = new IaijutsuSkill();

    private IaijutsuSkill() {
        super(Skill.createBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "iaijutsu")));
    }
}
