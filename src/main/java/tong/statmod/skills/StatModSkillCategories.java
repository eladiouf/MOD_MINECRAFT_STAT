package tong.statmod.skills;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.skill.SkillCategory;

/**
 * Custom skill categories for STAT Mod.
 * CLASS_ARTS is a custom skill slot for our weapon innate skills.
 */
public enum StatModSkillCategories implements SkillCategory {
    CLASS_ARTS(true, true, true, new ResourceLocation("statmod", "textures/gui/skills/class_arts.png"));

    private final boolean shouldSave;
    private final boolean shouldSynchronize;
    private final boolean learnable;
    private final ResourceLocation bookIcon;
    private final int id;

    StatModSkillCategories(boolean shouldSave, boolean shouldSynchronize, boolean learnable, ResourceLocation bookIcon) {
        this.shouldSave = shouldSave;
        this.shouldSynchronize = shouldSynchronize;
        this.learnable = learnable;
        this.bookIcon = bookIcon;
        this.id = SkillCategory.ENUM_MANAGER.assign(this);
    }

    @Override
    public boolean shouldSave() { return shouldSave; }

    @Override
    public boolean shouldSynchronize() { return shouldSynchronize; }

    @Override
    public boolean learnable() { return learnable; }

    @Override
    public int universalOrdinal() { return id; }

    @Override
    public ResourceLocation bookIcon() { return bookIcon; }
}
