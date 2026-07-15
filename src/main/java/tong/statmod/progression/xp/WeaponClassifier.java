package tong.statmod.progression.xp;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class WeaponClassifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> LOGGED_AMBIGUOUS_ITEMS = ConcurrentHashMap.newKeySet();

    private WeaponClassifier() {
    }

    public static WeaponClassification classify(ItemStack stack, boolean projectile) {
        if (stack == null || stack.isEmpty()) {
            return projectile ? WeaponClassification.PRECISION : WeaponClassification.UNCLASSIFIED;
        }
        WeaponClassification classification = resolve(
                stack.is(StatItemTags.HEAVY_WEAPONS),
                stack.is(StatItemTags.BLADE_WEAPONS),
                stack.is(StatItemTags.PRECISION_WEAPONS),
                projectile);
        if (classification == WeaponClassification.AMBIGUOUS) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (LOGGED_AMBIGUOUS_ITEMS.add(itemId)) {
                LOGGER.warn("Item {} is both a heavy and blade weapon; no melee specialization XP will be awarded",
                        itemId);
            }
        }
        return classification;
    }

    public static WeaponClassification resolve(
            boolean heavy, boolean blade, boolean precision, boolean projectile) {
        if (projectile) {
            return WeaponClassification.PRECISION;
        }
        if (heavy && blade) {
            return WeaponClassification.AMBIGUOUS;
        }
        if (heavy) {
            return WeaponClassification.HEAVY;
        }
        if (blade) {
            return WeaponClassification.BLADE;
        }
        if (precision) {
            return WeaponClassification.PRECISION;
        }
        return WeaponClassification.UNCLASSIFIED;
    }
}
