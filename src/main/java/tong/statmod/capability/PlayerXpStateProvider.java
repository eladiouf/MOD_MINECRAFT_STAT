package tong.statmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tong.statmod.progression.xp.PlayerXpState;

public final class PlayerXpStateProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerXpState state = new PlayerXpState();
    private LazyOptional<PlayerXpState> optional = LazyOptional.of(() -> state);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability != StatCapabilities.PLAYER_XP_STATE) {
            return LazyOptional.empty();
        }
        // Recréer le LazyOptional après invalidation (changement de dimension) — voir PlayerStatsProvider.
        if (!optional.isPresent()) {
            optional = LazyOptional.of(() -> state);
        }
        return optional.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        return state.serializeNbt();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        state.deserializeNbt(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
