package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AssemblyResultCache {
    private static volatile Set<ResourceLocation> ASSEMBLY_RESULTS = Collections.emptySet();

    private AssemblyResultCache() {}

    public static void rebuild(Set<ResourceLocation> results) {
        ASSEMBLY_RESULTS = Set.copyOf(results);
    }

    public static boolean hasAssembly(ResourceLocation itemId) {
        return itemId != null && ASSEMBLY_RESULTS.contains(itemId);
    }

    public static int count() {
        return ASSEMBLY_RESULTS.size();
    }
}
