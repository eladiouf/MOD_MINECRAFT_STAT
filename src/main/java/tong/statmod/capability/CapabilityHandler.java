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
import tong.statmod.perks.PerkManager;
import tong.statmod.perks.PerkProvider;
import tong.statmod.stats.StatEffectApplier;
import tong.statmod.weapon.WeaponMasteryManager;
import tong.statmod.world.thirst.ThirstManager;
import tong.statmod.world.thirst.ThirstProvider;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class CapabilityHandler {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerStats.class);
        event.register(FatigueManager.class);
        event.register(WeaponMasteryManager.class);
        event.register(ThirstManager.class);
        event.register(PerkManager.class);
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
            event.addCapability(
                new ResourceLocation(STATMod.MODID, "thirst"),
                new ThirstProvider());
            event.addCapability(
                new ResourceLocation(STATMod.MODID, "perks"),
                new PerkProvider());
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
            event.getOriginal().getCapability(PerkProvider.PERKS).ifPresent(oldPerks -> {
                event.getEntity().getCapability(PerkProvider.PERKS).ifPresent(newPerks -> {
                    newPerks.deserializeNBT(oldPerks.serializeNBT());
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Re-apply attribute bonuses after respawn (player entity is recreated)
            StatEffectApplier.applyAllBonuses(serverPlayer);
        }
    }
}
