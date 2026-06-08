package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.challenge.DailyChallenge;
import tong.statmod.integration.FtbTeamsIntegration;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;
import tong.statmod.party.PartyManager;
import tong.statmod.stats.StatType;
import tong.statmod.world.RandomEvents;

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
        awardXp(player, statIndex, tier, 1.0f);
    }

    /** Award XP with an external multiplier (e.g. L2Hostility level scaling). */
    public static void awardXp(ServerPlayer player, int statIndex, XpTier tier, float multiplier) {
        CapabilityHelper.withStats(player, stats -> {
            int oldLevel = stats.getLevel(statIndex);
            int xp = tier.minXp() + player.getRandom().nextInt(tier.maxXp() - tier.minXp() + 1);
            if (RandomEvents.isBonusXpActive()) {
                xp *= 2;
            }
            if (multiplier != 1.0f) {
                xp = Math.max(1, Math.round(xp * multiplier));
            }
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
                java.util.List<ServerPlayer> allPlayers = player.getServer().getPlayerList().getPlayers();
                java.util.Collection<ServerPlayer> allies = FtbTeamsIntegration.getAllies(player, allPlayers);
                float share = PartyManager.getPartyShareBonus(PartyManager.getPartyMembers(player.getUUID()).size());
                for (ServerPlayer ally : allies) {
                    if (ally.distanceTo(player) < 50) {
                        int sharedXp = Math.round(xp * share);
                        CapabilityHelper.withStats(ally, ms -> ms.addXp(statIndex, sharedXp));
                    }
                }
            }
        });
    }
}
