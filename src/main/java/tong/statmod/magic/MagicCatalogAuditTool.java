package tong.statmod.magic;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MagicCatalogAuditTool {

    public static void main(String[] args) {
        System.out.println("=== MAGIC CATALOG AUDIT ===\n");
        System.out.println("Total nodes: " + MagicTreeCatalog.all().size());
        System.out.println();

        int withCondition = 0;
        int withNullCondition = 0;
        for (MagicNode node : MagicTreeCatalog.all()) {
            if (node.condition() != null) {
                withCondition++;
            } else {
                withNullCondition++;
            }
        }
        System.out.println("Nodes with condition: " + withCondition);
        System.out.println("Nodes with null condition (unconditional): " + withNullCondition);

        System.out.println("\n=== SIGNATURE SPELLS DETAIL ===");
        for (MagicNode node : MagicTreeCatalog.all()) {
            if (node.kind() != MagicNodeKind.SIGNATURE_SPELL) continue;
            String spells = String.join(",", node.learnedSpells());
            String cond = node.condition() != null
                    ? node.condition().getClass().getSimpleName()
                    : "null";
            System.out.printf("  %-50s condition=%-20s %s%n", node.id(), cond, spells);
        }
    }

    private MagicCatalogAuditTool() {}
}
