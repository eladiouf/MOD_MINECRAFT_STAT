package tong.statmod.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.SyncAllStatsPacket;
import tong.statmod.network.SyncPerksPacket;
import tong.statmod.network.WeaponMasteryPacket;
import tong.statmod.weapon.WeaponMasteryManager;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void statmod_onPlayerJoin(Connection connection, ServerPlayer player, CallbackInfo ci) {
        CapabilityHelper.withStats(player, stats -> {
            int[] levels = new int[PlayerStats.STAT_COUNT];
            int[] xp = new int[PlayerStats.STAT_COUNT];
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                levels[i] = stats.getLevel(i);
                xp[i] = stats.getXp(i);
            }
            NetworkHandler.sendToPlayer(new SyncAllStatsPacket(levels, xp), player);
        });
        CapabilityHelper.withPerks(player, perks -> {
            int[] perkIds = perks.getUnlockedPerks().stream().mapToInt(i -> i).toArray();
            NetworkHandler.sendToPlayer(new SyncPerksPacket(perkIds, perks.getPerStatPoints()), player);
        });
        CapabilityHelper.withWeaponMastery(player, mastery -> {
            int[] wLevels = new int[WeaponMasteryManager.WEAPON_COUNT];
            int[] wXp = new int[WeaponMasteryManager.WEAPON_COUNT];
            for (int i = 0; i < WeaponMasteryManager.WEAPON_COUNT; i++) {
                wLevels[i] = mastery.getLevel(i);
                wXp[i] = mastery.getXp(i);
            }
            NetworkHandler.sendToPlayer(new WeaponMasteryPacket(wLevels, wXp), player);
        });
    }
}
