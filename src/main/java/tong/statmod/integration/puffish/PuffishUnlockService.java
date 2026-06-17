package tong.statmod.integration.puffish;

import net.minecraft.world.entity.player.Player;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.storage.PlayerStatData;

public final class PuffishUnlockService {
    private PuffishUnlockService() {}

    public static boolean tryUnlock(PlayerStatData data, Perk perk, Player player) {
        return new PerkManager(data).unlock(perk, player);
    }

    public static boolean tryUnlock(PlayerStatData data, String categoryId, String skillId, Player player) {
        Perk perk = PuffishPerkIds.resolve(categoryId, skillId);
        if (perk == null) {
            return false;
        }
        return tryUnlock(data, perk, player);
    }
}
