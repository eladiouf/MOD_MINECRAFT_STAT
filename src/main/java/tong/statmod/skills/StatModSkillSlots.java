package tong.statmod.skills;

import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

/**
 * Custom skill slots for STAT Mod.
 * CLASS_ARTS slot holds our weapon innate skills.
 */
public enum StatModSkillSlots implements SkillSlot {
    CLASS_ARTS(StatModSkillCategories.CLASS_ARTS);

    private final StatModSkillCategories category;
    private final int id;

    StatModSkillSlots(StatModSkillCategories category) {
        this.category = category;
        this.id = SkillSlot.ENUM_MANAGER.assign(this);
    }

    @Override
    public SkillCategory category() { return category; }

    @Override
    public int universalOrdinal() { return id; }
}
