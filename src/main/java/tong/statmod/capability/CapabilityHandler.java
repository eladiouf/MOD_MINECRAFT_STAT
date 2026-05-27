package tong.statmod.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.fatigue.FatigueManager;
import tong.statmod.fatigue.FatigueProvider;
import tong.statmod.weapon.WeaponMasteryManager;
import tong.statmod.weapon.WeaponMasteryProvider;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class CapabilityHandler {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerStats.class);
        event.register(FatigueManager.class);
        event.register(WeaponMasteryManager.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                new ResourceLocation(STATMod.MODID, "player_stats"),
                new PlayerStatsProvider());
            event.addCapability(
                new ResourceLocation(STATMod.MODID, "fatigue"),
                new FatigueProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(oldStats -> {
                event.getEntity().getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(newStats -> {
                    newStats.copyFrom(oldStats);
                });
            });
            event.getOriginal().getCapability(FatigueProvider.FATIGUE).ifPresent(oldFatigue -> {
                event.getEntity().getCapability(FatigueProvider.FATIGUE).ifPresent(newFatigue -> {
                    newFatigue.reset();
                });
            });
        }
    }
}
