package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.integration.overgeared.OvergearedRecipeGate.MaterialGate;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

/**
 * Politique pure pour le gating des recettes d'assemblage universel.
 *
 * <p>Centralise le calcul de la gate effective (matériau détecté + discounts race/perk)
 * afin que le runtime mixin et les tests parlent exactement la même langue.
 */
public final class AssemblyForgingPolicy {

    private AssemblyForgingPolicy() {}

    public static boolean canAssemble(ResourceLocation resultId, PlayerStatData data) {
        MaterialGate gate = effectiveGate(resultId, data);
        return gate == null || gate.satisfies(data);
    }

    public static String missingRequirements(ResourceLocation resultId, PlayerStatData data) {
        MaterialGate gate = effectiveGate(resultId, data);
        if (gate == null || gate.satisfies(data)) {
            return null;
        }

        int forging = level(data, StatType.FORGING);
        int erudition = level(data, StatType.ERUDITION);
        int arcane = level(data, StatType.ARCANE_POWER);

        StringBuilder missing = new StringBuilder();
        appendIfShort(missing, "FORGING", forging, gate.forging());
        appendIfShort(missing, "ERUDITION", erudition, gate.erudition());
        appendIfShort(missing, "ARCANE_POWER", arcane, gate.arcanePower());
        return missing.toString();
    }

    public static MaterialGate effectiveGate(ResourceLocation resultId, PlayerStatData data) {
        MaterialGate baseGate = OvergearedRecipeGate.gateForWeapon(resultId);
        if (baseGate == null) {
            return null;
        }
        return ForgingRaceBonus.applyBonuses(baseGate, data);
    }

    private static int level(PlayerStatData data, StatType stat) {
        return data == null ? 0 : data.getLevel(stat.index);
    }

    private static void appendIfShort(StringBuilder sb, String label, int have, int need) {
        if (have < need) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(label).append(" ").append(have).append("/").append(need);
        }
    }
}
