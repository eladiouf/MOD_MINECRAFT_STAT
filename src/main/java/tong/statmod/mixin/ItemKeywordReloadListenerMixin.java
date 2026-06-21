package tong.statmod.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.STATMod;
import tong.statmod.progression.WeaponResolver;
import tong.statmod.stats.StatType;
import yesman.epicfight.world.capabilities.item.ItemKeywordReloadListener;
import yesman.epicfight.world.capabilities.provider.CommonItemCapabilityProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Mixin(ItemKeywordReloadListener.class)
public class ItemKeywordReloadListenerMixin {
    private static final Map<String, List<String>> EXTRA_PATTERNS = new LinkedHashMap<>();
    private static final List<String> PRIORITY_ORDER = List.of(
        "epicfight:greatsword",
        "epicfight:longsword",
        "epicfight:axe",
        "epicfight:crossbow",
        "epicfight:spear",
        "epicfight:trident",
        "epicfight:tachi",
        "epicfight:uchigatana",
        "epicfight:dagger",
        "epicfight:bow",
        "epicfight:fist",
        "epicfight:hoe",
        "epicfight:pickaxe",
        "epicfight:shovel",
        "epicfight:sword",
        "epicfight:ranged"
    );
    private static Constructor<?> ITEM_REGEX_CTOR;
    private static Method REGEXES_GETTER;
    private static Field REGEXES_FIELD;
    private static boolean READY;

    static {
        EXTRA_PATTERNS.put("epicfight:greatsword", List.of(
            "tensura:.*_great_sword", "tensura:.*_odachi", "tensura:.*_hammer"
        ));
        EXTRA_PATTERNS.put("epicfight:axe", List.of(
            "tensura:.*_scythe", "tensura:.*_mace", "tensura:.*_club"
        ));
        EXTRA_PATTERNS.put("epicfight:sword", List.of(
            "tensura:.*_staff", "tensura:.*_wand", "tensura:.*_sickle",
            "tensura:.*_rapier"
        ));
        EXTRA_PATTERNS.put("epicfight:longsword", List.of("tensura:.*_long_sword"));
        EXTRA_PATTERNS.put("epicfight:dagger", List.of("tensura:.*_kodachi"));
        EXTRA_PATTERNS.put("epicfight:fist", List.of("tensura:.*_gauntlet"));
        EXTRA_PATTERNS.put("epicfight:spear", List.of("tensura:.*_throwing"));

        try {
            Class<?> cls = Class.forName(
                "yesman.epicfight.world.capabilities.item.ItemKeywordReloadListener$ItemRegex");
            ITEM_REGEX_CTOR = cls.getDeclaredConstructor(List.class);
            REGEXES_GETTER = cls.getMethod("regexes");
            REGEXES_FIELD = ItemKeywordReloadListener.class.getDeclaredField("REGEXES");
            REGEXES_FIELD.setAccessible(true);
            READY = true;
        } catch (Exception e) {
            STATMod.LOGGER.error("Failed to init ItemRegex reflection", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void injectPatterns() {
        if (!READY) return;
        try {
            Map<ResourceLocation, Object> regexes = (Map<ResourceLocation, Object>) REGEXES_FIELD.get(null);
            if (regexes == null) {
                return;
            }

            for (Map.Entry<String, List<String>> entry : EXTRA_PATTERNS.entrySet()) {
                ResourceLocation key = ResourceLocation.parse(entry.getKey());
                Object existing = regexes.get(key);
                if (existing == null) {
                    STATMod.LOGGER.warn("No ItemRegex for {}", key);
                    continue;
                }

                List<String> oldRegexes = (List<String>) REGEXES_GETTER.invoke(existing);
                List<String> combined = new ArrayList<>(oldRegexes.size() + entry.getValue().size());
                combined.addAll(oldRegexes);
                combined.addAll(entry.getValue());

                regexes.put(key, ITEM_REGEX_CTOR.newInstance(combined));
            }

            STATMod.LOGGER.info("Injected {} EpicFight keyword pattern groups", EXTRA_PATTERNS.size());

            Map<ResourceLocation, Object> sorted = new LinkedHashMap<>();
            for (String keyStr : PRIORITY_ORDER) {
                ResourceLocation key = ResourceLocation.parse(keyStr);
                if (regexes.containsKey(key)) {
                    sorted.put(key, regexes.get(key));
                }
            }
            for (Map.Entry<ResourceLocation, Object> entry : regexes.entrySet()) {
                if (!sorted.containsKey(entry.getKey())) {
                    sorted.put(entry.getKey(), entry.getValue());
                }
            }
            regexes.clear();
            regexes.putAll(sorted);
            STATMod.LOGGER.info("Sorted REGEXES by priority");

            CommonItemCapabilityProvider.INSTANCE.clear();
            CommonItemCapabilityProvider.INSTANCE.addDefaultItems();
            STATMod.LOGGER.info("Re-ran addDefaultItems() with injected patterns");

            logWeaponClassifications();
        } catch (Exception e) {
            STATMod.LOGGER.error("Failed to inject EpicFight patterns", e);
        }
    }

    private static void logWeaponClassifications() {
        List<String> namespaces = List.of("simplyswords", "tensura", "overgeared",
                "magistuarmory", "darkagesarmory", "magistuarmoryaddon");
        for (String ns : namespaces) {
            List<String> lines = new ArrayList<>();
            for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null || !id.getNamespace().equals(ns)) continue;
                ItemStack stack = new ItemStack(item);
                if (stack.isEmpty()) continue;
                StatType stat = WeaponResolver.statFor(stack);
                String action = WeaponResolver.tensuraActionFor(stack);
                lines.add("  " + id.getPath() + " -> " + stat.displayName + " [" + action + "]");
            }
            if (!lines.isEmpty()) {
                STATMod.LOGGER.info("[WeaponScan] {} ({} items):", ns, lines.size());
                for (String l : lines) STATMod.LOGGER.info(l);
            }
        }
    }

    @Inject(
        method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void statmod$onPostApply(Map<?, ?> object, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        injectPatterns();
    }
}
