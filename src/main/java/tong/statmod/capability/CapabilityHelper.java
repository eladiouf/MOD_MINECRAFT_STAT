package tong.statmod.capability;

import net.minecraft.world.entity.player.Player;
import tong.statmod.fatigue.FatigueManager;
import tong.statmod.fatigue.FatigueProvider;
import tong.statmod.perks.PerkManager;
import tong.statmod.perks.PerkProvider;
import tong.statmod.weapon.WeaponMasteryManager;
import tong.statmod.weapon.WeaponMasteryProvider;
import tong.statmod.world.thirst.ThirstManager;
import tong.statmod.world.thirst.ThirstProvider;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.Optional;
import java.util.function.Consumer;

public class CapabilityHelper {
    private CapabilityHelper() {}

    public static void withStats(Player player, Consumer<PlayerStats> action) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(s -> action.accept(s));
    }

    public static Optional<PlayerStats> getStats(Player player) {
        return player.getCapability(PlayerStatsProvider.PLAYER_STATS).resolve();
    }

    public static void withPerks(Player player, Consumer<PerkManager> action) {
        player.getCapability(PerkProvider.PERKS).ifPresent(s -> action.accept(s));
    }

    public static void withFatigue(Player player, Consumer<FatigueManager> action) {
        player.getCapability(FatigueProvider.FATIGUE).ifPresent(s -> action.accept(s));
    }

    public static void withThirst(Player player, Consumer<ThirstManager> action) {
        player.getCapability(ThirstProvider.THIRST).ifPresent(s -> action.accept(s));
    }

    public static void withWeaponMastery(Player player, Consumer<WeaponMasteryManager> action) {
        player.getCapability(WeaponMasteryProvider.WEAPON_MASTERY).ifPresent(s -> action.accept(s));
    }

    @SuppressWarnings("unchecked")
    public static void withEpicFight(Player player, Consumer<PlayerPatch<?>> action) {
        var cap = player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
        cap.ifPresent(e -> {
            if (e instanceof PlayerPatch<?> pp) action.accept(pp);
        });
    }
}
