package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;
import tong.statmod.stats.StatType;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class CombatXPHandler {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        StatType primaryStat = determinePrimaryStat(player);
        if (primaryStat == null) return;

        int xp = 5 + player.getRandom().nextInt(6);
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            stats.addXp(primaryStat.index, xp);
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(primaryStat.index, stats.getLevel(primaryStat.index), stats.getXp(primaryStat.index)),
                player);
        });
    }

    private static StatType determinePrimaryStat(ServerPlayer player) {
        var cap = player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
        if (cap.isPresent() && cap.resolve().isPresent()) {
            Object patch = cap.resolve().get();
            if (patch instanceof ServerPlayerPatch playerPatch) {
                CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                if (itemCap != null && !itemCap.isEmpty()) {
                    WeaponCategory cat = itemCap.getWeaponCategory();
                    if (cat == CapabilityItem.WeaponCategories.AXE || cat == CapabilityItem.WeaponCategories.GREATSWORD) return StatType.BRUTE_FORCE;
                    if (cat == CapabilityItem.WeaponCategories.SWORD || cat == CapabilityItem.WeaponCategories.DAGGER
                        || cat == CapabilityItem.WeaponCategories.UCHIGATANA || cat == CapabilityItem.WeaponCategories.TACHI
                        || cat == CapabilityItem.WeaponCategories.TRIDENT || cat == CapabilityItem.WeaponCategories.LONGSWORD) return StatType.BLADE_TECHNIQUE;
                    if (cat == CapabilityItem.WeaponCategories.FIST) return StatType.RAPIDITE;
                    if (cat == CapabilityItem.WeaponCategories.BOW || cat == CapabilityItem.WeaponCategories.CROSSBOW) return StatType.PRECISION;
                    if (cat == CapabilityItem.WeaponCategories.SPEAR) return StatType.AGILITY;
                    if (cat == CapabilityItem.WeaponCategories.SHIELD) return StatType.PHYSICAL_ENDURANCE;
                }
            }
        }
        return StatType.BRUTE_FORCE;
    }
}
