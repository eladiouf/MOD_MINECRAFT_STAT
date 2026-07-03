package tong.statmod.item;

import java.util.ArrayList;
import java.util.List;

/**
 * Mission M6 — Phase δ.
 *
 * <p>Constants pures pour les 25 {@code RuneShard} du Trial Dungeon (5 raretés × 5 familles).
 * Séparé de {@link RuneShards} pour être testable sans initialiser le {@code DeferredRegister}
 * (pattern établi Mission M5 : {@code ForgingIntermediateIds} → {@code ForgingIntermediates}).
 */
public final class RuneShardIds {

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    }

    public enum Family {
        PHYSICAL, MAGICAL, VITAL, AGILE, SPIRITUAL
    }

    private RuneShardIds() {}

    /** Construit l'ID d'un shard : {@code common_physical_shard}, {@code legendary_spiritual_shard}, etc. */
    public static String id(Rarity rarity, Family family) {
        return rarity.name().toLowerCase() + "_" + family.name().toLowerCase() + "_shard";
    }

    /** Liste des 25 IDs (raretés × familles). Ordre stable pour les tests. */
    public static List<String> allIds() {
        List<String> ids = new ArrayList<>(Rarity.values().length * Family.values().length);
        for (Rarity r : Rarity.values()) {
            for (Family f : Family.values()) {
                ids.add(id(r, f));
            }
        }
        return ids;
    }

    /** Parse un ID pour retrouver sa rareté. Retourne {@code null} si l'ID n'est pas un shard. */
    public static Rarity rarityOf(String id) {
        if (id == null) return null;
        for (Rarity r : Rarity.values()) {
            String prefix = r.name().toLowerCase() + "_";
            if (id.startsWith(prefix)) return r;
        }
        return null;
    }

    /** Parse un ID pour retrouver sa famille. */
    public static Family familyOf(String id) {
        if (id == null) return null;
        for (Family f : Family.values()) {
            if (id.contains("_" + f.name().toLowerCase() + "_")) return f;
        }
        return null;
    }
}
