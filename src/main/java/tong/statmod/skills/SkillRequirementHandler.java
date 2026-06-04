package tong.statmod.skills;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

/**
 * Intercepts skill casts and blocks them if the player doesn't meet stat requirements.
 * Attaches a SKILL_CAST_EVENT listener to each player's Epic Fight event system on login.
 */
@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class SkillRequirementHandler {

    private static final UUID LISTENER_UUID = UUID.fromString("a3f1c2d4-5678-9abc-def0-1234567890ab");
    private static final Set<UUID> attachedPlayers = new HashSet<>();

    /**
     * Called when a player logs in. Attaches the skill cast interceptor
     * to the player's Epic Fight event listener system.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CapabilityHelper.withEpicFight(player, cap -> {
            if (cap instanceof ServerPlayerPatch playerPatch) {
                attachListener(playerPatch, player);
            }
        });
    }

    /**
     * Also handle player respawn (dimension change, death) to re-attach the listener.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        attachedPlayers.remove(player.getUUID());

        CapabilityHelper.withEpicFight(player, cap -> {
            if (cap instanceof ServerPlayerPatch playerPatch) {
                attachListener(playerPatch, player);
            }
        });
    }

    /**
     * Clean up when player logs out to prevent memory leak.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            attachedPlayers.remove(player.getUUID());
        }
    }

    private static void attachListener(ServerPlayerPatch playerPatch, ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (attachedPlayers.contains(uuid)) return;

        try {
            playerPatch.getEventListener().addEventListener(
                PlayerEventListener.EventType.SKILL_CAST_EVENT,
                LISTENER_UUID,
                SkillRequirementHandler::onSkillCast
            );
            attachedPlayers.add(uuid);
            STATMod.LOGGER.debug("Attached skill requirement listener to player {}", player.getName().getString());
        } catch (Exception e) {
            STATMod.LOGGER.warn("Failed to attach skill requirement listener: {}", e.getMessage());
        }
    }

    /**
     * Called by Epic Fight when a player tries to cast a skill.
     * Checks stat requirements and cancels if not met.
     */
    private static void onSkillCast(SkillCastEvent event) {
        if (!(event.getPlayerPatch() instanceof ServerPlayerPatch serverPatch)) return;

        Skill skill = event.getSkillContainer().getSkill();
        if (skill == null) return;

        ResourceLocation skillId = skill.getRegistryName();
        if (skillId == null) return;

        ServerPlayer player = serverPatch.getOriginal();

        if (!SkillRequirementRegistry.meetsRequirements(player, skillId)) {
            // Block the skill — use both mechanisms for compatibility
            event.setCanceled(true);
            event.setSkillExecutable(false);

            // Send action bar feedback
            String reqText = SkillRequirementRegistry.getRequirementText(skillId);
            player.displayClientMessage(
                Component.literal("Requiert: " + reqText).withStyle(ChatFormatting.RED),
                true // action bar
            );
        }
    }
}
