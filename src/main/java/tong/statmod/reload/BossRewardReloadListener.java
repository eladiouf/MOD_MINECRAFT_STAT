package tong.statmod.reload;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.STATMod;

import java.util.*;

public class BossRewardReloadListener extends SimpleJsonResourceReloadListener {

    public static final BossRewardReloadListener INSTANCE = new BossRewardReloadListener();

    private final Map<ResourceLocation, BossReward> rewards = new HashMap<>();

    private BossRewardReloadListener() {
        super(new GsonBuilder().create(), "boss_rewards");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        rewards.clear();
        for (var entry : objects.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                ResourceLocation entityType =
                    new ResourceLocation(obj.get("entity_type").getAsString());
                List<ItemStack> items = parseItems(obj.getAsJsonArray("items"));
                int xpPerStat = obj.get("xp_per_stat").getAsInt();
                String message = obj.get("message").getAsString();
                rewards.put(entityType, new BossReward(entityType, items, xpPerStat, message));
            } catch (Exception e) {
                STATMod.LOGGER.error("[BossRewards] Failed to load {}", entry.getKey(), e);
            }
        }
        STATMod.LOGGER.info("[BossRewards] Loaded {} boss reward entries", rewards.size());
    }

    private List<ItemStack> parseItems(JsonArray array) {
        List<ItemStack> result = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            ResourceLocation itemId = new ResourceLocation(obj.get("item").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null) {
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                result.add(new ItemStack(item, count));
            } else {
                STATMod.LOGGER.warn("[BossRewards] Unknown item: {}", itemId);
            }
        }
        return List.copyOf(result);
    }

    public Optional<BossReward> getReward(ResourceLocation entityType) {
        return Optional.ofNullable(rewards.get(entityType));
    }

    public record BossReward(
        ResourceLocation entityType,
        List<ItemStack> items,
        int xpPerStat,
        String message
    ) {}
}
