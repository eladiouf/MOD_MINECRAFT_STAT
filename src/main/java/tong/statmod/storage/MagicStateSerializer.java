package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import tong.statmod.STATMod;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;

public final class MagicStateSerializer {
    private MagicStateSerializer() {}

    public static CompoundTag serialize(PlayerStatData data) {
        CompoundTag tag = new CompoundTag();
        // Monnaie unifiée — la seule source de vérité depuis Mission δ.
        tag.putInt("magicPoints", data.getMagicPoints());

        // Mastery par école : tracker pour les paliers de Mission ε. Conservé tel quel.
        int[] sm = new int[MagicBranch.values().length];
        for (MagicBranch b : MagicBranch.values()) {
            sm[b.ordinal()] = data.getSchoolMasteryProgress(b);
        }
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

    @SuppressWarnings("deprecation")
    public static void deserialize(CompoundTag tag, PlayerStatData data) {
        // --- Migration legacy ---
        // Saves pré-Mission-δ contiennent arcanePoints + schoolPoints. On les charge dans les
        // anciens champs, puis on appelle migrateLegacyPointsToUnified() qui draine vers
        // magicPoints (capped). Les saves post-δ ont juste "magicPoints".
        boolean hasLegacyArcane = tag.contains("arcanePoints");
        boolean hasLegacySchool = tag.contains("schoolPoints");
        boolean hasUnified = tag.contains("magicPoints");

        if (hasUnified) {
            data.setMagicPoints(tag.getInt("magicPoints"));
        }
        if (hasLegacyArcane) {
            data.setArcanePoints(tag.getInt("arcanePoints"));
        }
        if (hasLegacySchool) {
            int[] sp = tag.getIntArray("schoolPoints");
            for (MagicBranch b : MagicBranch.values()) {
                int idx = b.ordinal();
                if (idx < sp.length) data.setSchoolPoints(b, sp[idx]);
            }
        }
        if (hasLegacyArcane || hasLegacySchool) {
            int migrated = data.migrateLegacyPointsToUnified();
            if (migrated > 0) {
                STATMod.LOGGER.info("Migrated {} legacy magic points → unified pool (capped at {})",
                        migrated, PlayerStatData.MIGRATION_CAP);
            }
        }

        // --- Mastery (inchangé) ---
        int[] sm = tag.getIntArray("schoolMastery");
        for (MagicBranch b : MagicBranch.values()) {
            int idx = b.ordinal();
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
