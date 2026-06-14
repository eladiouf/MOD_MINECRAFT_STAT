package tong.statmod.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraftforge.common.util.INBTSerializable;
import tong.statmod.stats.StatType;

import java.util.HashSet;
import java.util.Set;

public class PerkManager implements INBTSerializable<CompoundTag> {
    private final Set<Integer> unlockedPerks = new HashSet<>();
    private final int[] perStatPoints = new int[23];

    public boolean isUnlocked(Perk perk) { return unlockedPerks.contains(perk.id); }
    public boolean isUnlocked(int id) { return unlockedPerks.contains(id); }
    public Set<Integer> getUnlockedPerks() { return Set.copyOf(unlockedPerks); }

    public int getPointsForStat(StatType stat) { return perStatPoints[stat.index]; }
    public int[] getPerStatPoints() { return perStatPoints.clone(); }

    public void addPointsForStat(StatType stat, int amount) {
        perStatPoints[stat.index] = Math.max(0, perStatPoints[stat.index] + amount);
    }

    public int getSpentPointsInStat(StatType stat) {
        int spent = 0;
        for (int id : unlockedPerks) {
            Perk p = Perk.byId(id);
            if (p != null && p.stat == stat) spent += p.tier.cost;
        }
        return spent;
    }

    public int getAvailablePointsForStat(StatType stat) {
        return Math.max(0, perStatPoints[stat.index] - getSpentPointsInStat(stat));
    }

    public boolean unlockPerk(Perk perk) {
        if (unlockedPerks.contains(perk.id)) return false;
        if (getAvailablePointsForStat(perk.stat) < perk.tier.cost) return false;
        unlockedPerks.add(perk.id);
        return true;
    }

    public void resetPerks() {
        unlockedPerks.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("PerStatPoints", perStatPoints);
        ListTag list = new ListTag();
        for (int id : unlockedPerks) {
            list.add(IntTag.valueOf(id));
        }
        tag.put("UnlockedPerks", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loaded = tag.getIntArray("PerStatPoints");
        if (loaded.length == 23) {
            System.arraycopy(loaded, 0, perStatPoints, 0, 23);
        } else if (tag.contains("Points")) {
            int oldPoints = Math.max(0, tag.getInt("Points"));
            perStatPoints[0] = oldPoints;
        }
        unlockedPerks.clear();
        ListTag list = tag.getList("UnlockedPerks", 3);
        for (int i = 0; i < list.size(); i++) {
            int id = list.getInt(i);
            if (Perk.byId(id) != null) {
                unlockedPerks.add(id);
            }
        }
    }
}
