package tong.statmod.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.integration.EpicFightCompat;
import tong.statmod.stats.StatType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class SkillUnlockHandler {
    private static final int[] TIERS = {10, 25, 50, 75};
    private static final Set<UUID> processed = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        processed.add(player.getUUID());
        syncAllSkills(player);
    }

    public static void onLevelUp(ServerPlayer player, StatType stat, int newLevel) {
        for (int i = 0; i < TIERS.length; i++) {
            if (newLevel >= TIERS[i]) {
                var skill = SkillUnlockRegistry.getSkill(stat, i);
                if (skill != null) {
                    EpicFightCompat.grantSkill(player, skill);
                }
            }
        }
    }

    public static void syncAllSkills(ServerPlayer player) {
        CapabilityHelper.withStats(player, stats -> {
            for (StatType stat : StatType.values()) {
                int level = stats.getLevel(stat.index);
                for (int i = 0; i < TIERS.length; i++) {
                    if (level >= TIERS[i]) {
                        var skill = SkillUnlockRegistry.getSkill(stat, i);
                        if (skill != null) {
                            EpicFightCompat.grantSkill(player, skill);
                        }
                    }
                }
            }
        });
    }
}