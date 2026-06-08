package tong.statmod.reload;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import tong.statmod.STATMod;
import tong.statmod.combat.skills.MobSkillLoadout;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MobSkillReloadListener extends SimpleJsonResourceReloadListener {

    public static final MobSkillReloadListener INSTANCE = new MobSkillReloadListener();

    private final Map<ResourceLocation, MobSkillLoadout> loadouts = new HashMap<>();

    private MobSkillReloadListener() {
        super(new GsonBuilder().create(), "mob_skills");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        loadouts.clear();
        for (var entry : objects.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                MobSkillLoadout loadout = MobSkillLoadout.fromJson(obj);
                loadouts.put(loadout.entityType(), loadout);
            } catch (Exception e) {
                STATMod.LOGGER.error("[MobSkills] Failed to load {}", entry.getKey(), e);
            }
        }
        STATMod.LOGGER.info("[MobSkills] Loaded {} mob skill loadouts", loadouts.size());
    }

    public Optional<MobSkillLoadout> get(ResourceLocation entityType) {
        return Optional.ofNullable(loadouts.get(entityType));
    }
}
