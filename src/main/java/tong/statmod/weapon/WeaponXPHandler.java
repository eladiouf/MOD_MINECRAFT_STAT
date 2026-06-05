package tong.statmod.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.WeaponMasteryPacket;
import tong.statmod.sound.ModSounds;
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
            int prevLevel = mastery.getLevel(weaponType.ordinal());
            int xp = Config.weaponXpMin + player.getRandom().nextInt(Config.weaponXpMax - Config.weaponXpMin + 1);
            mastery.addXp(weaponType.ordinal(), xp);
            int newLevel = mastery.getLevel(weaponType.ordinal());
            if (newLevel > prevLevel) {
                player.level().playSound(null, player.blockPosition(),
                    ModSounds.WEAPON_LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            StatType primaryStat = mapWeaponToStat(weaponType);
            if (primaryStat != null) {
                CapabilityHelper.withStats(player, stats -> {
                    stats.addXp(primaryStat.index, xp / 2);
                });
            }

            int[] wLevels = new int[WeaponMasteryManager.WEAPON_COUNT];
            int[] wXp = new int[WeaponMasteryManager.WEAPON_COUNT];
            for (int i = 0; i < WeaponMasteryManager.WEAPON_COUNT; i++) {
                wLevels[i] = mastery.getLevel(i);
                wXp[i] = mastery.getXp(i);
            }
            NetworkHandler.sendToPlayer(new WeaponMasteryPacket(wLevels, wXp), player);
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
