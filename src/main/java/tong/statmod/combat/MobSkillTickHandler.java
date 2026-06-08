package tong.statmod.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.MobSkillState;
import tong.statmod.capability.MobSkillStateProvider;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.combat.skills.MobSkillLoadout;
import tong.statmod.combat.skills.MobSkillRegistry;
import tong.statmod.reload.MobSkillReloadListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class MobSkillTickHandler {

    private static final Random RNG = new Random();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.mobSkillsEnabled) return;

        tickCounter++;
        if (tickCounter < Config.mobSkillTickInterval) return;
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof Mob mob) tickMob(mob);
            }
        }
    }

    private static void tickMob(Mob mob) {
        if (!mob.isAlive()) return;

        MobStats stats = mob.getCapability(MobStatsProvider.MOB_STATS).orElse(null);
        if (stats == null) return;

        MobSkillState state = mob.getCapability(MobSkillStateProvider.MOB_SKILL_STATE).orElse(null);
        if (state == null) return;

        state.addMana(1);

        long now = mob.level().getGameTime();
        if (state.getGlobalCooldownEndTick() > now) return;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityType == null) return;

        MobSkillLoadout loadout = MobSkillReloadListener.INSTANCE.get(entityType).orElse(null);
        if (loadout == null) return;
        if (loadout.entries().isEmpty()) return;

        List<Candidate> castable = new ArrayList<>();
        int totalWeight = 0;
        for (var entry : loadout.entries()) {
            MobSkill skill = MobSkillRegistry.get(entry.skillId());
            if (skill == null) continue;
            if (skill.isReactive()) continue;
            if (stats.getLevel(skill.requiredStat().index) < skill.requiredLevel()) continue;
            if (state.getMana() < skill.manaCost()) continue;
            if (state.getCooldownEndTick(skill.id()) > now) continue;
            if (mob.distanceTo(target) > skill.maxRange()) continue;
            if (!skill.canExecute(mob, target, stats)) continue;
            castable.add(new Candidate(skill, entry.weight()));
            totalWeight += entry.weight();
        }
        if (castable.isEmpty()) return;

        int pick = RNG.nextInt(totalWeight);
        MobSkill chosen = null;
        int acc = 0;
        for (var c : castable) {
            acc += c.weight;
            if (pick < acc) { chosen = c.skill; break; }
        }
        if (chosen == null) chosen = castable.get(0).skill;

        try {
            chosen.execute(mob, target, stats);
        } catch (Exception e) {
            STATMod.LOGGER.error("[MobSkills] {} crashed during execute on {}",
                chosen.id(), mob.getType(), e);
            return;
        }

        long cd = Math.round(chosen.baseCooldownTicks() * Config.mobSkillCooldownMult);
        state.setCooldownEndTick(chosen.id(), now + cd);
        state.setMana(state.getMana() - chosen.manaCost());
        state.setGlobalCooldownEndTick(now + loadout.globalCooldownTicks());
    }

    private record Candidate(MobSkill skill, int weight) {}
}
