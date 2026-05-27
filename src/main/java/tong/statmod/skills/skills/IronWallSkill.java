package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import yesman.epicfight.skill.Skill;

public class IronWallSkill extends Skill {
    public static final IronWallSkill INSTANCE = new IronWallSkill();

    private IronWallSkill() {
        super(Skill.createBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "iron_wall")));
    }
}
