package tong.statmod.forge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class ForgeStationItemRules {

    private static final Set<String> GRIP_IDS = Set.of(
            "statmod:wooden_grip",
            "statmod:leather_wrap",
            "statmod:wire_wrap",
            "statmod:runic_grip");

    private ForgeStationItemRules() {}

    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static boolean isRoughIntermediate(ItemStack stack) {
        return isRoughIntermediateId(itemId(stack));
    }

    public static boolean isRuneEssence(ItemStack stack) {
        return isRuneEssenceId(itemId(stack));
    }

    public static boolean isGrip(ItemStack stack) {
        return isGripId(itemId(stack));
    }

    public static boolean isRoughIntermediateId(String id) {
        return id != null && id.startsWith("statmod:rough_");
    }

    public static boolean isRuneEssenceId(String id) {
        return id != null && id.startsWith("statmod:rune_essence_");
    }

    public static boolean isGripId(String id) {
        return GRIP_IDS.contains(id);
    }
}
