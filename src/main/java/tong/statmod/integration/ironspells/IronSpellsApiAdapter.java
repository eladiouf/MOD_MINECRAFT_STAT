package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.magic.MagicBranch;

public final class IronSpellsApiAdapter {
    private IronSpellsApiAdapter() {}

    public static String spellId(AbstractSpell spell) {
        if (spell == null) return null;
        ResourceLocation id = spell.getSpellResource();
        return id == null ? null : id.toString();
    }

    public static MagicBranch branchOf(AbstractSpell spell) {
        if (spell == null) return null;
        SchoolType school = spell.getSchoolType();
        if (school == null) return null;
        ResourceLocation key = school.getId();
        if (key == null) return null;
        return IronSchoolMapping.fromIronsSchoolId(key.getNamespace(), key.getPath());
    }

    public static int maxSpellLevel(AbstractSpell spell) {
        if (spell == null) return 0;
        try {
            return spell.getMaxLevel();
        } catch (Throwable ignored) {
            return 10;
        }
    }
}
