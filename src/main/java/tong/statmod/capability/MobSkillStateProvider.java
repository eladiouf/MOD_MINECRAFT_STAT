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

public class MobSkillStateProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<MobSkillState> MOB_SKILL_STATE =
        CapabilityManager.get(new CapabilityToken<>() {});

    private MobSkillState instance = null;
    private final LazyOptional<MobSkillState> optional = LazyOptional.of(this::getOrCreate);

    private MobSkillState getOrCreate() {
        if (instance == null) instance = new MobSkillState();
        return instance;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MOB_SKILL_STATE ? optional.cast() : LazyOptional.empty();
    }

    @Override public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }

    public void invalidate() { optional.invalidate(); }
}
