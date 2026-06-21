package tong.statmod.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;

public final class MagicStateSerializer {
    private MagicStateSerializer() {}

    public static CompoundTag serialize(PlayerStatData data) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("arcanePoints", data.getArcanePoints());

        int[] sp = new int[MagicBranch.values().length];
        int[] sm = new int[sp.length];
        for (MagicBranch b : MagicBranch.values()) {
            sp[b.ordinal()] = data.getSchoolPoints(b);
            sm[b.ordinal()] = data.getSchoolMasteryProgress(b);
        }
        tag.putIntArray("schoolPoints", sp);
        tag.putIntArray("schoolMastery", sm);

        ListTag nodes = new ListTag();
        for (String s : data.getMagicNodes()) nodes.add(StringTag.valueOf(s));
        tag.put("magicNodes", nodes);

        ListTag spells = new ListTag();
        for (String s : data.getLearnedSpells()) spells.add(StringTag.valueOf(s));
        tag.put("learnedSpells", spells);

        if (data.getMagicRace() != null) tag.putString("magicRace", data.getMagicRace().name());
        if (data.getChosenStartBranch() != null) tag.putString("chosenStartBranch", data.getChosenStartBranch().id);

        return tag;
    }

    public static void deserialize(CompoundTag tag, PlayerStatData data) {
        data.setArcanePoints(tag.getInt("arcanePoints"));

        int[] sp = tag.getIntArray("schoolPoints");
        int[] sm = tag.getIntArray("schoolMastery");
        for (MagicBranch b : MagicBranch.values()) {
            int idx = b.ordinal();
            if (idx < sp.length) data.setSchoolPoints(b, sp[idx]);
            if (idx < sm.length) data.setSchoolMasteryProgress(b, sm[idx]);
        }

        ListTag nodes = tag.getList("magicNodes", Tag.TAG_STRING);
        String[] nodeIds = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) nodeIds[i] = nodes.getString(i);
        data.setMagicNodes(nodeIds);

        ListTag spells = tag.getList("learnedSpells", Tag.TAG_STRING);
        String[] spellIds = new String[spells.size()];
        for (int i = 0; i < spells.size(); i++) spellIds[i] = spells.getString(i);
        data.setLearnedSpells(spellIds);

        if (tag.contains("magicRace")) {
            try { data.setMagicRace(MagicRace.valueOf(tag.getString("magicRace"))); }
            catch (IllegalArgumentException ignored) {}
        }
        if (tag.contains("chosenStartBranch")) {
            data.setChosenStartBranch(MagicBranch.byId(tag.getString("chosenStartBranch")));
        }
    }
}
