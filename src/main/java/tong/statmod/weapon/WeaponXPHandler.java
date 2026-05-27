package tong.statmod.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.integration.EpicFightCompat;
import tong.statmod.stats.StatType;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class WeaponXPHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!EpicFightCompat.isEpicFightLoaded()) return;

        WeaponType weaponType = determineWeaponType(player);
        if (weaponType == null) return;

        player.getCapability(WeaponMasteryProvider.WEAPON_MASTERY).ifPresent(mastery -> {
            int xp = 5 + player.getRandom().nextInt(6);
            mastery.addXp(weaponType.ordinal(), xp);

            StatType primaryStat = mapWeaponToStat(weaponType);
            if (primaryStat != null) {
                player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                    stats.addXp(primaryStat.index, xp / 2);
                });
            }
        });
    }

    private static WeaponType determineWeaponType(ServerPlayer player) {
        var cap = player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
        if (cap.isPresent() && cap.resolve().isPresent()) {
            Object patch = cap.resolve().get();
            if (patch instanceof ServerPlayerPatch playerPatch) {
                CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                if (itemCap != null && !itemCap.isEmpty()) {
                    WeaponCategory cat = itemCap.getWeaponCategory();
                    for (WeaponType type : WeaponType.values()) {
                        if (type.epicFightCategory == cat) return type;
                    }
                }
            }
        }
        return null;
    }

    private static StatType mapWeaponToStat(WeaponType weapon) {
        return switch (weapon) {
            case AXE, GREATSWORD -> StatType.BRUTE_FORCE;
            case SWORD, DAGGER, UCHIGATANA, TACHI, LONGSWORD, TRIDENT -> StatType.BLADE_TECHNIQUE;
            case FIST -> StatType.RAPIDITE;
            case BOW, CROSSBOW -> StatType.PRECISION;
            case SPEAR -> StatType.AGILITY;
            case SHIELD -> StatType.PHYSICAL_ENDURANCE;
        };
    }
}
