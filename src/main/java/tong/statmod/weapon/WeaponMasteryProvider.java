package tong.statmod.weapon;

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

public class WeaponMasteryProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<WeaponMasteryManager> WEAPON_MASTERY = CapabilityManager.get(new CapabilityToken<>() {});

    private WeaponMasteryManager manager;
    private final LazyOptional<WeaponMasteryManager> lazyOptional = LazyOptional.of(this::getOrCreate);

    private WeaponMasteryManager getOrCreate() {
        if (this.manager == null) this.manager = new WeaponMasteryManager();
        return this.manager;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == WEAPON_MASTERY ? lazyOptional.cast() : LazyOptional.empty();
    }

    public void invalidate() {
        lazyOptional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }
}
