package tong.statmod.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

public class PerkManager implements INBTSerializable<CompoundTag> {
    private final Set<Integer> unlockedPerks = new HashSet<>();
    private int availablePoints = 0;

    public boolean isUnlocked(Perk perk) { return unlockedPerks.contains(perk.id); }
    public boolean isUnlocked(int id) { return unlockedPerks.contains(id); }
    public Set<Integer> getUnlockedPerks() { return unlockedPerks; }
    public int getAvailablePoints() { return availablePoints; }
    public void addPoints(int amount) { this.availablePoints = Math.max(0, this.availablePoints + amount); }

    public boolean unlockPerk(Perk perk, int currentStatLevel) {
        if (availablePoints <= 0) return false;
        if (unlockedPerks.contains(perk.id)) return false;
        if (currentStatLevel < perk.getEffectiveLevelRequired()) return false;
        unlockedPerks.add(perk.id);
        availablePoints--;
        return true;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Points", availablePoints);
        ListTag list = new ListTag();
        for (int id : unlockedPerks) {
            list.add(IntTag.valueOf(id));
        }
        tag.put("UnlockedPerks", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.availablePoints = tag.getInt("Points");
        unlockedPerks.clear();
        ListTag list = tag.getList("UnlockedPerks", 3);
        for (int i = 0; i < list.size(); i++) {
            unlockedPerks.add(list.getInt(i));
        }
    }
}
