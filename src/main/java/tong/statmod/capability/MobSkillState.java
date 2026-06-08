package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

public class MobSkillState implements INBTSerializable<CompoundTag> {

    private int mana = 0;
    private long globalCooldownEndTick = 0L;
    private final Map<ResourceLocation, Long> cooldownEnd = new HashMap<>();

    public int getMana() { return mana; }

    public void setMana(int mana) {
        this.mana = Math.max(0, mana);
    }

    public void addMana(int amount) {
        setMana(this.mana + amount);
    }

    public long getCooldownEndTick(ResourceLocation skillId) {
        Long v = cooldownEnd.get(skillId);
        return v == null ? 0L : v;
    }

    public void setCooldownEndTick(ResourceLocation skillId, long endTick) {
        cooldownEnd.put(skillId, Math.max(0L, endTick));
    }

    public long getGlobalCooldownEndTick() { return globalCooldownEndTick; }

    public void setGlobalCooldownEndTick(long tick) {
        this.globalCooldownEndTick = Math.max(0L, tick);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", mana);
        tag.putLong("GlobalCD", globalCooldownEndTick);
        ListTag list = new ListTag();
        for (var entry : cooldownEnd.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", entry.getKey().toString());
            c.putLong("End", entry.getValue());
            list.add(c);
        }
        tag.put("Cooldowns", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        setMana(tag.getInt("Mana"));
        setGlobalCooldownEndTick(tag.getLong("GlobalCD"));
        cooldownEnd.clear();
        ListTag list = tag.getList("Cooldowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            cooldownEnd.put(new ResourceLocation(c.getString("Id")), Math.max(0L, c.getLong("End")));
        }
    }
}
