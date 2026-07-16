package tong.statmod.integration.sdmshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class MagicShopReport {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<String> spells = new TreeSet<>();
    private final Map<String, Integer> byAddon = new TreeMap<>();
    private final Map<String, Integer> bySchool = new TreeMap<>();
    private final Map<String, Integer> byRarity = new TreeMap<>();
    private final Map<Integer, Integer> byLevel = new TreeMap<>();
    private final List<SkippedSpell> skipped = new ArrayList<>();
    private int scrollEntryCount;
    private int saleEntryCount;

    public void recordSpell(String spellId, String schoolId, String rarity, int level) {
        spells.add(spellId);
        String namespace = spellId.contains(":")
                ? spellId.substring(0, spellId.indexOf(':')) : spellId;
        byAddon.merge(namespace, 1, Integer::sum);
        bySchool.merge(schoolId, 1, Integer::sum);
        byRarity.merge(rarity.toLowerCase(java.util.Locale.ROOT), 1, Integer::sum);
        byLevel.merge(level, 1, Integer::sum);
    }

    public void recordScrollEntry() {
        scrollEntryCount++;
    }

    public void recordSaleEntry() {
        saleEntryCount++;
    }

    public void recordSkipped(String spellId, String reason) {
        skipped.add(new SkippedSpell(spellId, reason));
        skipped.sort(java.util.Comparator.comparing(SkippedSpell::spellId));
    }

    public int spellCount() {
        return spells.size();
    }

    public int scrollEntryCount() {
        return scrollEntryCount;
    }

    public int saleEntryCount() {
        return saleEntryCount;
    }

    public int skippedCount() {
        return skipped.size();
    }

    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("spellCount", spellCount());
        root.put("scrollEntryCount", scrollEntryCount);
        root.put("saleEntryCount", saleEntryCount);
        root.put("skippedCount", skipped.size());
        root.put("byAddon", byAddon);
        root.put("bySchool", bySchool);
        root.put("byRarity", byRarity);
        root.put("byLevel", byLevel);
        root.put("skipped", skipped);
        return GSON.toJson(root) + System.lineSeparator();
    }

    private record SkippedSpell(String spellId, String reason) {
    }
}
