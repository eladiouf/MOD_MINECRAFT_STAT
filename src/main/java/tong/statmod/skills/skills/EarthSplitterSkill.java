package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;
import yesman.epicfight.skill.Skill;

public class EarthSplitterSkill extends Skill {
    public static final EarthSplitterSkill INSTANCE = new EarthSplitterSkill();

    private EarthSplitterSkill() {
        super(Skill.createBuilder().setRegistryName(new ResourceLocation(STATMod.MODID, "earth_splitter")));
    }
}
