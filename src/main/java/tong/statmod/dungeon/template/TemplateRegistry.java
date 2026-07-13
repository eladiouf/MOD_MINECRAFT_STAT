package tong.statmod.dungeon.template;

import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TemplateRegistry {
    private static final Map<ResourceLocation, RoomTemplate> TEMPLATES = new ConcurrentHashMap<>();
    private static final Map<String, WeightedPool<ResourceLocation>> POOLS = new ConcurrentHashMap<>();
    private static boolean loaded = false;

    private TemplateRegistry() {}

    public static void register(ResourceLocation id, RoomTemplate template) {
        TEMPLATES.put(id, template);
    }

    public static RoomTemplate get(ResourceLocation id) {
        return TEMPLATES.get(id);
    }

    public static void registerPool(String poolName, List<WeightedPool.Entry<ResourceLocation>> entries) {
        POOLS.put(poolName, new WeightedPool<>(entries));
    }

    public static RoomTemplate select(String poolName, long seed, double x, double y) {
        WeightedPool<ResourceLocation> pool = POOLS.get(poolName);
        if (pool == null || pool.isEmpty()) return null;
        ResourceLocation id = pool.select(seed, x, y);
        return TEMPLATES.get(id);
    }

    public static boolean isLoaded() { return loaded; }
    public static void markLoaded() { loaded = true; }
    public static void clear() { TEMPLATES.clear(); POOLS.clear(); loaded = false; }
}
