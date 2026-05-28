package tong.statmod.perks;

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

public class PerkProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PerkManager> PERKS = CapabilityManager.get(new CapabilityToken<>() {});

    private PerkManager manager;
    private final LazyOptional<PerkManager> lazyOptional = LazyOptional.of(this::getOrCreate);

    private PerkManager getOrCreate() {
        if (this.manager == null) this.manager = new PerkManager();
        return this.manager;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PERKS ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }
}
