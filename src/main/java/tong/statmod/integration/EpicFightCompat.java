package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import tong.statmod.STATMod;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class EpicFightCompat {
    private static boolean epicFightLoaded = false;

    public static void init() {
        epicFightLoaded = ModList.get() != null && ModList.get().isLoaded(EpicFightMod.MODID);
        if (epicFightLoaded) {
            STATMod.LOGGER.info("Epic Fight detected — deep integration enabled");
        } else {
            STATMod.LOGGER.info("Epic Fight not detected — running in standalone mode");
        }
    }

    public static boolean isEpicFightLoaded() {
        return epicFightLoaded;
    }

    public static void grantSkill(ServerPlayer player, Skill skill) {
        if (!epicFightLoaded) return;
        player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).ifPresent(cap -> {
            if (cap instanceof PlayerPatch<?> playerPatch) {
                for (SkillSlots slot : SkillSlots.values()) {
                    if (slot.category() == skill.getCategory()) {
                        SkillContainer container = playerPatch.getSkill(slot);
                        if (container != null) {
                            container.setSkill(skill);
                            STATMod.LOGGER.info("Granted Epic Fight skill: {} to slot {}", skill.getRegistryName(), slot);
                        }
                        break;
                    }
                }
            }
        });
    }
}
