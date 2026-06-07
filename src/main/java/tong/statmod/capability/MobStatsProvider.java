package tong.statmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobStatsProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<MobStats> MOB_STATS =
        CapabilityManager.get(new CapabilityToken<>() {});

    private MobStats instance = null;
    private final LazyOptional<MobStats> optional =
        LazyOptional.of(this::getOrCreate);

    private MobStats getOrCreate() {
        if (instance == null) instance = new MobStats();
        return instance;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
        @NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MOB_STATS ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return getOrCreate().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getOrCreate().deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
