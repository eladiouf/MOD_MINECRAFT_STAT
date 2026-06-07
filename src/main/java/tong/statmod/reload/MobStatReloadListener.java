package tong.statmod.reload;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import tong.statmod.STATMod;
import tong.statmod.stats.StatType;

import java.util.*;

public class MobStatReloadListener extends SimpleJsonResourceReloadListener {

    public static final MobStatReloadListener INSTANCE = new MobStatReloadListener();

    private final Map<ResourceLocation, int[]> mobDefaults = new HashMap<>();

    private MobStatReloadListener() {
        super(new GsonBuilder().create(), "mob_stats");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        mobDefaults.clear();
        for (var entry : objects.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                ResourceLocation entityType =
                    new ResourceLocation(obj.get("entity_type").getAsString());
                int[] levels = new int[StatType.values().length];
                JsonObject baseLevels = obj.getAsJsonObject("base_levels");
                for (StatType stat : StatType.values()) {
                    if (baseLevels.has(stat.name())) {
                        levels[stat.index] = baseLevels.get(stat.name()).getAsInt();
                    }
                }
                mobDefaults.put(entityType, levels);
            } catch (Exception e) {
                STATMod.LOGGER.error("[MobStats] Failed to load {}", entry.getKey(), e);
            }
        }
        STATMod.LOGGER.info("[MobStats] Loaded {} mob stat entries", mobDefaults.size());
    }

    public Optional<int[]> getDefaults(ResourceLocation entityType) {
        return Optional.ofNullable(mobDefaults.get(entityType));
    }
}
