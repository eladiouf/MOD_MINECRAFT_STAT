package tong.statmod.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import tong.statmod.progression.xp.PlayerXpState;
import tong.statmod.stats.PlayerStats;

public final class StatCapabilities {
    public static final Capability<PlayerStats> PLAYER_STATS =
            CapabilityManager.get(new CapabilityToken<>() {
            });
    public static final Capability<PlayerXpState> PLAYER_XP_STATE =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private StatCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerStats.class);
        event.register(PlayerXpState.class);
    }
}
