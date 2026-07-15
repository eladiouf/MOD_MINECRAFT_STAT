package tong.statmod.dungeon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import tong.statmod.config.StatModServerConfig;

import java.util.ArrayList;
import java.util.List;

public final class ModdedMobPool {

    private ModdedMobPool() {}

    public static List<EntityType<?>> getModdedMobs(FloorPalette tier) {
        List<EntityType<?>> pool = new ArrayList<>();
        java.util.List<? extends String> configList = switch (tier) {
            case EARLY -> StatModServerConfig.dungeonEarlyMobs();
            case MID -> StatModServerConfig.dungeonMidMobs();
            case LATE -> StatModServerConfig.dungeonLateMobs();
            case ABYSS -> StatModServerConfig.dungeonAbyssMobs();
        };

        for (String id : configList) {
            addIfAvailable(pool, id);
        }
        return pool;
    }

    private static void addIfAvailable(List<EntityType<?>> pool, String entityId) {
        EntityType<?> type = resolve(entityId);
        if (type != null) {
            pool.add(type);
        }
    }

    public static EntityType<?> resolve(String entityId) {
        if (entityId == null) return null;
        if ("statmod:pyromancer_mob".equals(entityId)) {
            return EntityType.ZOMBIE;
        }
        if ("statmod:cryomancer_mob".equals(entityId)) {
            return EntityType.SKELETON;
        }
        if ("statmod:electromancer_mob".equals(entityId)) {
            return EntityType.SKELETON;
        }
        if ("statmod:wither_mage_mob".equals(entityId)) {
            return EntityType.WITHER_SKELETON;
        }
        if ("statmod:cleric_mob".equals(entityId)) {
            return EntityType.ZOMBIE;
        }
        if ("statmod:hollow_witch_mob".equals(entityId)) {
            return EntityType.WITCH;
        }
        if ("statmod:mage_knight_mob".equals(entityId)) {
            return EntityType.ZOMBIE;
        }
        if ("statmod:void_knight_mob".equals(entityId)) {
            return EntityType.WITHER_SKELETON;
        }

        try {
            ResourceLocation id = new ResourceLocation(entityId);
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return BuiltInRegistries.ENTITY_TYPE.get(id);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static List<EntityType<?>> resolveAll(List<String> ids) {
        List<EntityType<?>> out = new ArrayList<>();
        for (String id : ids) {
            EntityType<?> t = resolve(id);
            if (t != null) out.add(t);
        }
        return out;
    }

    public static List<EntityType<?>> getCombinedPool(int floor) {
        DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
        List<EntityType<?>> themed = resolveAll(theme.adds());
        if (!themed.isEmpty()) return themed;

        FloorPalette tier = FloorPalette.forFloor(floor);
        List<EntityType<?>> vanilla = DungeonMasterpiece.mobPool(tier);
        List<EntityType<?>> modded = getModdedMobs(tier);
        if (modded.isEmpty()) return vanilla;

        int moddedAllowed = vanilla.size();
        List<EntityType<?>> combined = new ArrayList<>(vanilla);
        if (modded.size() <= moddedAllowed) {
            combined.addAll(modded);
        } else {
            java.util.Collections.shuffle(modded);
            combined.addAll(modded.subList(0, moddedAllowed));
        }
        return combined;
    }
}
