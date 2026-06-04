package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.challenge.DailyChallenge;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;
import tong.statmod.party.PartyManager;
import tong.statmod.stats.StatType;

import java.util.UUID;

public class ActionXpHelper {

    public enum XpTier {
        COMMON,
        INTERMEDIATE,
        RARE;

        public int minXp() {
            return switch (this) {
                case COMMON -> tong.statmod.Config.xpTierCommonMin;
                case INTERMEDIATE -> tong.statmod.Config.xpTierIntermediateMin;
                case RARE -> tong.statmod.Config.xpTierRareMin;
            };
        }
        public int maxXp() {
            return switch (this) {
                case COMMON -> tong.statmod.Config.xpTierCommonMax;
                case INTERMEDIATE -> tong.statmod.Config.xpTierIntermediateMax;
                case RARE -> tong.statmod.Config.xpTierRareMax;
            };
        }
    }

    public static void awardXp(ServerPlayer player, int statIndex, XpTier tier) {
        CapabilityHelper.withStats(player, stats -> {
            int oldLevel = stats.getLevel(statIndex);
            int xp = tier.minXp() + player.getRandom().nextInt(tier.maxXp() - tier.minXp() + 1);
            stats.addXp(statIndex, xp);
            int newLevel = stats.getLevel(statIndex);
            if (newLevel > oldLevel && newLevel > 0) {
                LevelUpHandler.onLevelUp(player, statIndex, newLevel);
            }
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(statIndex, newLevel, stats.getXp(statIndex)),
                player);
            DailyChallenge.onXpAward(player, StatType.byIndex(statIndex), xp);

            if (PartyManager.isInParty(player.getUUID())) {
                for (UUID memberId : PartyManager.getPartyMembers(player.getUUID())) {
                    ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                    if (member != null && member != player && member.distanceTo(player) < 50) {
                        float share = PartyManager.getPartyShareBonus(PartyManager.getPartyMembers(player.getUUID()).size());
                        int sharedXp = Math.round(xp * share);
                        CapabilityHelper.withStats(member, ms -> ms.addXp(statIndex, sharedXp));
                    }
                }
            }
        });
    }
}
