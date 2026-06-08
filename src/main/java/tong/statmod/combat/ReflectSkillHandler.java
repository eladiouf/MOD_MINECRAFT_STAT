package tong.statmod.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.MobSkillState;
import tong.statmod.capability.MobSkillStateProvider;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.combat.skills.impl.ReflectSkill;
import tong.statmod.reload.MobSkillReloadListener;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class ReflectSkillHandler {

    @SubscribeEvent
    public static void onMobHurt(LivingHurtEvent event) {
        if (!Config.mobSkillsEnabled) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        MobStats stats = mob.getCapability(MobStatsProvider.MOB_STATS).orElse(null);
        if (stats == null) return;
        if (stats.getLevel(StatType.PHYSICAL_RESISTANCE.index) < 50) return;

        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityType == null) return;

        var loadout = MobSkillReloadListener.INSTANCE.get(entityType).orElse(null);
        if (loadout == null) return;
        boolean hasReflect = loadout.entries().stream()
            .anyMatch(e -> e.skillId().equals(ReflectSkill.ID));
        if (!hasReflect) return;

        MobSkillState state = mob.getCapability(MobSkillStateProvider.MOB_SKILL_STATE).orElse(null);
        if (state == null) return;

        long now = mob.level().getGameTime();
        if (state.getCooldownEndTick(ReflectSkill.ID) > now) return;

        float reflected = event.getAmount() * ReflectSkill.REFLECT_FRACTION;
        attacker.hurt(attacker.damageSources().thorns(mob), reflected);
        state.setCooldownEndTick(ReflectSkill.ID, now + 40);
    }
}
