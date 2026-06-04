package tong.statmod.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class WeaponXPHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        WeaponType weaponType = determineWeaponType(player);
        if (weaponType == null) return;

        CapabilityHelper.withWeaponMastery(player, mastery -> {
            int xp = 5 + player.getRandom().nextInt(6);
            mastery.addXp(weaponType.ordinal(), xp);

            StatType primaryStat = mapWeaponToStat(weaponType);
            if (primaryStat != null) {
                CapabilityHelper.withStats(player, stats -> {
                    stats.addXp(primaryStat.index, xp / 2);
                });
            }
        });
    }

    private static WeaponType determineWeaponType(ServerPlayer player) {
        var ref = new WeaponType[1];
        CapabilityHelper.withEpicFight(player, cap -> {
            if (cap instanceof ServerPlayerPatch playerPatch) {
                CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                if (itemCap != null && !itemCap.isEmpty()) {
                    WeaponCategory cat = itemCap.getWeaponCategory();
                    for (WeaponType type : WeaponType.values()) {
                        if (type.epicFightCategory == cat) {
                            ref[0] = type;
                            return;
                        }
                    }
                }
            }
        });
        return ref[0];
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
