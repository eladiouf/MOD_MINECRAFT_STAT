package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.skills.StatModSkillSlots;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Handles the CLASS_ARTS key binding to activate weapon innate skills.
 */
@Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT)
public class ClassArtsKeyHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Check if CLASS_ARTS key is pressed
        if (StatModKeyMappings.CLASS_ARTS_SKILL.consumeClick()) {
            LocalPlayer player = mc.player;

            try {
                // Get the player's Epic Fight capability
                player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).ifPresent(cap -> {
                    if (cap instanceof PlayerPatch<?> playerPatch) {
                        // Get the CLASS_ARTS skill container
                        SkillContainer container = playerPatch.getSkill(StatModSkillSlots.CLASS_ARTS);
                        if (container != null && container.hasSkill()) {
                            // Get the ClientEngine and ControlEngine
                            ClientEngine clientEngine = ClientEngine.getInstance();
                            if (clientEngine != null && clientEngine.controlEngine != null) {
                                // Send cast request to server via Epic Fight's system
                                container.sendCastRequest(
                                    (yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch) playerPatch,
                                    clientEngine.controlEngine
                                );
                                STATMod.LOGGER.info("Activated CLASS_ARTS skill: {}", container.getSkill().getRegistryName());
                            } else {
                                STATMod.LOGGER.warn("ClientEngine or ControlEngine is null");
                            }
                        } else {
                            STATMod.LOGGER.warn("CLASS_ARTS container is null or has no skill");
                        }
                    }
                });
            } catch (Exception e) {
                STATMod.LOGGER.error("Error activating CLASS_ARTS skill", e);
            }
        }
    }
}
