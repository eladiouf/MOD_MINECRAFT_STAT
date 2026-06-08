package tong.statmod.combat.skills;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MobSkillLoadout(
    ResourceLocation entityType,
    int globalCooldownTicks,
    List<Entry> entries
) {

    public record Entry(ResourceLocation skillId, int weight) {}

    public static MobSkillLoadout fromJson(JsonObject obj) {
        ResourceLocation entityType = new ResourceLocation(obj.get("entity_type").getAsString());
        int gcd = obj.has("global_cooldown_ticks") ? obj.get("global_cooldown_ticks").getAsInt() : 20;
        List<Entry> entries = new ArrayList<>();
        if (obj.has("skills")) {
            JsonArray arr = obj.getAsJsonArray("skills");
            for (var el : arr) {
                JsonObject e = el.getAsJsonObject();
                ResourceLocation id = new ResourceLocation(e.get("id").getAsString());
                int weight = e.has("weight") ? e.get("weight").getAsInt() : 100;
                entries.add(new Entry(id, weight));
            }
        }
        return new MobSkillLoadout(entityType, gcd, List.copyOf(entries));
    }
}
