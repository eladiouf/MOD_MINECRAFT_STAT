package tong.statmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tong.statmod.stats.PlayerStats;

public final class PlayerStatsProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerStats stats = new PlayerStats();
    private final LazyOptional<PlayerStats> optional = LazyOptional.of(() -> stats);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == StatCapabilities.PLAYER_STATS
                ? optional.cast()
                : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return stats.serializeNbt();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        stats.deserializeNbt(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
