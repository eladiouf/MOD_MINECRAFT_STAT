package tong.statmod.progression;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import tong.statmod.integration.EpicFightCompat;
import tong.statmod.perks.PerkProvider;
import tong.statmod.skills.SkillUnlockRegistry;
import tong.statmod.sound.ModSounds;
import tong.statmod.stats.StatEffectApplier;
import tong.statmod.stats.StatType;
import tong.statmod.advancement.StatAdvancementTrigger;
import tong.statmod.Config;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;

public class LevelUpHandler {

    public static void onLevelUp(ServerPlayer player, int statIndex, int newLevel) {
        if (newLevel <= 0) return;

        // Check if this is a milestone (every 10 levels)
        if (newLevel % 10 == 0) {
            handleMilestone(player, statIndex, newLevel);
        }

        // Refresh passive bonuses on every level-up
        StatEffectApplier.applyAllBonuses(player);

        CapabilityHelper.withStats(player, stats -> {
            int global = 0;
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) global += stats.getLevel(i);
            int avgGlobal = Math.round(global / (float) PlayerStats.STAT_COUNT);
            StatAdvancementTrigger.INSTANCE.trigger(player, avgGlobal);
        });
    }

    private static void handleMilestone(ServerPlayer player, int statIndex, int level) {
        // Play sound
        if (level == 100) {
            player.level().playSound(null, player.blockPosition(),
                ModSounds.MILESTONE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            player.level().playSound(null, player.blockPosition(),
                ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // Spawn particles
        spawnMilestoneParticles(player, level);

        int tier1 = Config.perkTier1Level;
        int tier2 = Config.perkTier2Level;
        int tier3 = Config.perkTier3Level;
        int points = level == 100 ? 3 : (level == tier1 || level == 40 || level == 50 || level == 60 || level == 80) ? 1 : 0;
        if (points > 0) {
            int p = points;
            CapabilityHelper.withPerks(player, perks -> perks.addPoints(p));
        }

        StatType stat = StatType.byIndex(statIndex);
        int s1 = Config.skillTier1Level;
        int s2 = Config.skillTier2Level;
        int s3 = Config.skillTier3Level;
        int sa = Config.skillActiveLevel;
        int skillTier;
        if (level == s1) skillTier = 0;
        else if (level == s2) skillTier = 1;
        else if (level == s3) skillTier = 2;
        else if (level == sa) skillTier = 3;
        else skillTier = -1;
        if (skillTier >= 0) {
            var skill = SkillUnlockRegistry.getSkill(stat, skillTier);
            if (skill != null) {
                EpicFightCompat.grantSkill(player, skill);
            }
        }
    }

    private static void spawnMilestoneParticles(ServerPlayer player, int level) {
        if (!(player.level() instanceof ServerLevel levelWorld)) return;

        Vec3 pos = player.position();
        double x = pos.x;
        double y = pos.y + 1.0;
        double z = pos.z;

        switch (level) {
            case 10 -> {
                for (int i = 0; i < 20; i++) {
                    double angle = i * Math.PI * 2 / 20;
                    double px = x + Math.cos(angle) * 1.5;
                    double pz = z + Math.sin(angle) * 1.5;
                    levelWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, y + 0.5, pz, 1, 0, 0, 0, 0.1);
                }
            }
            case 20 -> {
                for (int i = 0; i < 15; i++) {
                    levelWorld.sendParticles(ParticleTypes.END_ROD, x, y + i * 0.3, z, 2, 0.2, 0, 0.2, 0.02);
                }
            }
            case 30 -> {
                for (int i = 0; i < 30; i++) {
                    double angle = i * Math.PI * 2 / 30;
                    double px = x + Math.cos(angle) * 2.0;
                    double pz = z + Math.sin(angle) * 2.0;
                    levelWorld.sendParticles(ParticleTypes.FLAME, px, y, pz, 1, 0, 0.1, 0, 0.02);
                    levelWorld.sendParticles(ParticleTypes.ENCHANTED_HIT, px, y + 1, pz, 1, 0, 0, 0, 0.05);
                }
            }
            case 40 -> {
                for (int i = 0; i < 25; i++) {
                    double angle = i * Math.PI * 2 / 25;
                    double px = x + Math.cos(angle) * 1.2;
                    double pz = z + Math.sin(angle) * 1.2;
                    levelWorld.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, y + Math.sin(angle * 2) * 0.5 + 0.5, pz, 1, 0, 0, 0, 0.03);
                }
            }
            case 50 -> {
                levelWorld.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.5, z, 1, 0, 0, 0, 0);
                for (int i = 0; i < 40; i++) {
                    double angle = i * Math.PI * 2 / 40;
                    double px = x + Math.cos(angle) * 2.5;
                    double pz = z + Math.sin(angle) * 2.5;
                    levelWorld.sendParticles(ParticleTypes.GLOW, px, y, pz, 1, 0, 0.2, 0, 0.03);
                }
                levelWorld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.5, z, 15, 0.5, 0.5, 0.5, 0.5);
            }
            case 60 -> {
                for (int i = 0; i < 20; i++) {
                    double angle = i * Math.PI * 2 / 20;
                    double px = x + Math.cos(angle) * 1.8;
                    double pz = z + Math.sin(angle) * 1.8;
                    levelWorld.sendParticles(ParticleTypes.WAX_ON, px, y + 0.3, pz, 1, 0, 0.1, 0, 0.02);
                }
            }
            case 70 -> {
                for (int i = 0; i < 35; i++) {
                    double angle = i * Math.PI * 2 / 35;
                    double px = x + Math.cos(angle) * 2.2;
                    double pz = z + Math.sin(angle) * 2.2;
                    levelWorld.sendParticles(ParticleTypes.SONIC_BOOM, px, y + 0.5, pz, 1, 0, 0, 0, 0);
                    levelWorld.sendParticles(ParticleTypes.FLAME, px, y + 1, pz, 1, 0, 0, 0, 0.02);
                }
            }
            case 80 -> {
                for (int i = 0; i < 30; i++) {
                    double angle = i * Math.PI * 2 / 30;
                    double px = x + Math.cos(angle) * 2.0;
                    double pz = z + Math.sin(angle) * 2.0;
                    levelWorld.sendParticles(ParticleTypes.WAX_ON, px, y, pz, 1, 0, 0.2, 0, 0.03);
                    levelWorld.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, y + 0.5, pz, 1, 0, 0.1, 0, 0.02);
                }
            }
            case 90 -> {
                for (int i = 0; i < 30; i++) {
                    double angle = i * Math.PI * 2 / 30;
                    double px = x + Math.cos(angle + i * 0.3) * 1.5;
                    double pz = z + Math.sin(angle + i * 0.3) * 1.5;
                    levelWorld.sendParticles(ParticleTypes.END_ROD, px, y + i * 0.2, pz, 1, 0, 0, 0, 0.03);
                }
            }
            case 100 -> {
                levelWorld.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 2, z, 3, 0, 0, 0, 0);
                levelWorld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 2, z, 30, 1.5, 1.0, 1.5, 1.0);
                for (int ring = 0; ring < 3; ring++) {
                    int count = 30 + ring * 10;
                    double radius = 2.0 + ring * 0.8;
                    for (int i = 0; i < count; i++) {
                        double angle = i * Math.PI * 2 / count + ring * 0.5;
                        double px = x + Math.cos(angle) * radius;
                        double pz = z + Math.sin(angle) * radius;
                        levelWorld.sendParticles(ParticleTypes.FIREWORK, px, y + ring * 0.4, pz, 1, 0, 0.1, 0, 0.05);
                        levelWorld.sendParticles(ParticleTypes.GLOW, px, y + 1 + ring * 0.3, pz, 1, 0, 0, 0, 0.03);
                    }
                }
                for (int i = 0; i < 20; i++) {
                    levelWorld.sendParticles(ParticleTypes.HEART,
                        x + (Math.random() - 0.5) * 4,
                        y + Math.random() * 3,
                        z + (Math.random() - 0.5) * 4,
                        1, 0, 0, 0, 0);
                }
            }
        }
    }
}
