package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.skills.StatModSkillSlots;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
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

            // Get the player's Epic Fight capability
            player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).ifPresent(cap -> {
                if (cap instanceof PlayerPatch<?> playerPatch) {
                    // Get the CLASS_ARTS skill container using the slot's universal ordinal
                    SkillContainer container = playerPatch.getSkill(StatModSkillSlots.CLASS_ARTS);
                    if (container != null && container.hasSkill()) {
                        // Send cast request to server
                        container.sendCastRequest(
                            (yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch) playerPatch,
                            null
                        );
                    }
                }
            });
        }
    }
}
