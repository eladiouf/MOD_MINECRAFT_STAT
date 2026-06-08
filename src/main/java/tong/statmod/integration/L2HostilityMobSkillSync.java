package tong.statmod.integration;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import tong.statmod.combat.skills.MobSkillLoadout;
import tong.statmod.combat.skills.impl.BattleCrySkill;
import tong.statmod.combat.skills.impl.ChargeSkill;
import tong.statmod.combat.skills.impl.CurseSkill;
import tong.statmod.combat.skills.impl.FireballSkill;
import tong.statmod.combat.skills.impl.MagicMissileSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * Augments a mob's loadout with extra skill entries unlocked by L2H traits.
 * Direct references to L2H classes — never call without first checking
 * {@code ModList.isLoaded("l2hostility")}.
 */
public class L2HostilityMobSkillSync {

    private L2HostilityMobSkillSync() {}

    public static MobSkillLoadout augment(Mob mob, MobSkillLoadout base) {
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        if (cap == null) return base;

        List<MobSkillLoadout.Entry> entries = new ArrayList<>(base.entries());
        for (MobTrait trait : cap.traits.keySet()) {
            ResourceLocation id = trait.getRegistryName();
            if (id == null) continue;
            switch (id.getPath()) {
                case "aura"        -> entries.add(new MobSkillLoadout.Entry(BattleCrySkill.ID, 60));
                case "killer_aura" -> entries.add(new MobSkillLoadout.Entry(MagicMissileSkill.ID, 80));
                case "fiery"       -> entries.add(new MobSkillLoadout.Entry(FireballSkill.ID, 70));
                case "gravity"     -> entries.add(new MobSkillLoadout.Entry(ChargeSkill.ID, 60));
                case "dispell"     -> entries.add(new MobSkillLoadout.Entry(CurseSkill.ID, 70));
                case "master"      -> {
                    entries.add(new MobSkillLoadout.Entry(BattleCrySkill.ID, 50));
                    entries.add(new MobSkillLoadout.Entry(MagicMissileSkill.ID, 50));
                    entries.add(new MobSkillLoadout.Entry(FireballSkill.ID, 50));
                }
                default -> {}
            }
        }
        if (entries.size() == base.entries().size()) return base;
        return new MobSkillLoadout(base.entityType(), base.globalCooldownTicks(), List.copyOf(entries));
    }
}
