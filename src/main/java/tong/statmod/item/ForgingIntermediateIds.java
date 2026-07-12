package tong.statmod.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mission M5 — Phase β.
 *
 * <p>Constants pures et testables pour les intermédiaires de forge — pas de
 * DeferredRegister ici (qui demande un registry Minecraft bound). Utilisable en JUnit pur
 * sans environnement runtime.
 *
 * <p>{@link ForgingIntermediates} consomme cette classe pour construire son registry.
 */
public final class ForgingIntermediateIds {

    public static final List<String> WEAPON_CLASSES = List.of(
            "blade", "axe_head", "spear_tip", "bow_limb", "staff_core", "dagger_blade"
    );

    public static final List<String> MATERIALS = List.of(
            "gold", "tin", "bronze", "diamond",
            "pyrium", "arcane", "mithril",
            "low_magisteel", "magisteel", "pure_magisteel", "high_magisteel",
            "orichalcum", "adamantite", "hihiirokane", "netherite"
    );

    private static final List<String> ALL_IDS = buildAllIds();
    private static final Set<String> ALL_IDS_SET = Set.copyOf(ALL_IDS);

    private static List<String> buildAllIds() {
        List<String> ids = new ArrayList<>(WEAPON_CLASSES.size() * MATERIALS.size());
        for (String cls : WEAPON_CLASSES) {
            for (String mat : MATERIALS) {
                ids.add("rough_" + cls + "_" + mat);
            }
        }
        return List.copyOf(ids);
    }

    public static List<String> allIds() {
        return ALL_IDS;
    }

    public static boolean isKnownId(String id) {
        return id != null && ALL_IDS_SET.contains(id);
    }

    public static int count() {
        return ALL_IDS.size();
    }

    private ForgingIntermediateIds() {}
}
