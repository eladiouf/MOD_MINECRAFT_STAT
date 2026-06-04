package tong.statmod.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import tong.statmod.stats.StatType;

public class StatAdvancementTrigger extends SimpleCriterionTrigger<StatAdvancementTrigger.TriggerInstance> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "stat_level");
    public static final StatAdvancementTrigger INSTANCE = new StatAdvancementTrigger();

    private StatAdvancementTrigger() {}

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
        int globalLevel = json.has("global_level") ? json.get("global_level").getAsInt() : 0;
        String stat = json.has("stat") ? json.get("stat").getAsString() : null;
        int level = json.has("level") ? json.get("level").getAsInt() : 0;
        String feature = json.has("feature") ? json.get("feature").getAsString() : null;
        return new TriggerInstance(player, globalLevel, stat, level, feature);
    }

    public void trigger(ServerPlayer player, int globalLevel) {
        this.trigger(player, instance -> instance.matches(globalLevel));
    }

    public void triggerFeature(ServerPlayer player, String feature) {
        this.trigger(player, instance -> instance.matchesFeature(feature));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final int requiredGlobalLevel;
        private final String statName;
        private final int requiredLevel;
        private final String feature;

        public TriggerInstance(ContextAwarePredicate player, int requiredGlobalLevel, String statName, int requiredLevel, String feature) {
            super(ID, player);
            this.requiredGlobalLevel = requiredGlobalLevel;
            this.statName = statName;
            this.requiredLevel = requiredLevel;
            this.feature = feature;
        }

        public boolean matches(int globalLevel) {
            return requiredGlobalLevel <= 0 || globalLevel >= requiredGlobalLevel;
        }

        public boolean matchesFeature(String featureName) {
            return feature != null && feature.equals(featureName);
        }

        private boolean checkAllCombat100(ServerPlayer player) {
            int[] combatIndices = {0, 1, 2, 3, 4, 5, 6};
            boolean[] result = {true};
            CapabilityHelper.withStats(player, stats -> {
                for (int idx : combatIndices) {
                    if (stats.getLevel(idx) < 100) { result[0] = false; return; }
                }
            });
            return result[0];
        }

        private boolean checkAll50Plus(ServerPlayer player) {
            boolean[] result = {true};
            CapabilityHelper.withStats(player, stats -> {
                for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                    if (stats.getLevel(i) < 50) { result[0] = false; return; }
                }
            });
            return result[0];
        }

        @Override
        public JsonObject serializeToJson(SerializationContext ctx) {
            JsonObject obj = super.serializeToJson(ctx);
            if (requiredGlobalLevel > 0) obj.addProperty("global_level", requiredGlobalLevel);
            if (statName != null) obj.addProperty("stat", statName);
            if (requiredLevel > 0) obj.addProperty("level", requiredLevel);
            if (feature != null) obj.addProperty("feature", feature);
            return obj;
        }
    }
}
