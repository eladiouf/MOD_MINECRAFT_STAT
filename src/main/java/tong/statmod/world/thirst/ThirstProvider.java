package tong.statmod.world.thirst;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThirstProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<ThirstManager> THIRST = CapabilityManager.get(new CapabilityToken<>() {});

    private ThirstManager manager;
    private final LazyOptional<ThirstManager> lazyOptional = LazyOptional.of(this::getOrCreate);

    private ThirstManager getOrCreate() {
        if (this.manager == null) this.manager = new ThirstManager();
        return this.manager;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == THIRST ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }
}
