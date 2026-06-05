package tong.statmod.challenge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;
import java.util.*;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class DailyChallenge {
    private static final Map<UUID, ChallengeState> challenges = new HashMap<>();
    private static final Random RANDOM = new Random();

    public record ChallengeState(StatType stat, int target, int progress, long assignedDay) {}

    public static void assignDaily(ServerPlayer player) {
        StatType randomStat = StatType.values()[RANDOM.nextInt(StatType.values().length)];
        int target = 50 + RANDOM.nextInt(151);
        long currentDay = player.level().getDayTime() / 24000;
        ChallengeState state = new ChallengeState(randomStat, target, 0, currentDay);
        challenges.put(player.getUUID(), state);
        player.sendSystemMessage(Component.literal(
            "\u00a76Daily Challenge: \u00a7eEarn " + target + " XP in \u00a7b" + randomStat.displayName + " \u00a76today!"));
    }

    public static void onXpAward(ServerPlayer player, StatType stat, int xp) {
        ChallengeState state = challenges.get(player.getUUID());
        if (state == null) return;
        long currentDay = player.level().getDayTime() / 24000;
        if (currentDay != state.assignedDay) return;
        if (state.stat != stat) return;

        int newProgress = state.progress + xp;
        challenges.put(player.getUUID(), new ChallengeState(state.stat, state.target, newProgress, state.assignedDay));

        if (newProgress >= state.target && state.progress < state.target) {
            player.sendSystemMessage(Component.literal("\u00a7aChallenge Complete! \u00a7e+3 bonus perk points!"));
            CapabilityHelper.withPerks(player, perks -> perks.addPoints(3));
        }
    }

    public static ChallengeState getChallenge(UUID id) { return challenges.get(id); }

    public static void cleanup(UUID uuid) { challenges.remove(uuid); }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cleanup(event.getEntity().getUUID());
    }
}
