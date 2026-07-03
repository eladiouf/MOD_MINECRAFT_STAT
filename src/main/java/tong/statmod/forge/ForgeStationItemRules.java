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
    private static final Set<String> ANVIL_SUPPORT_TOOL_IDS = Set.of(
            "statmod:basic_forge_tongs",
            "statmod:basic_smithing_hammer");

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

    public static boolean isShardLikeCatalyst(ItemStack stack) {
        return isShardLikeCatalystId(itemId(stack));
    }

    public static boolean isAnvilSupport(ItemStack stack) {
        return isAnvilSupportId(itemId(stack));
    }

    public static boolean isRoughIntermediateId(String id) {
        return id != null && (
                id.startsWith("statmod:rough_")
                        || id.startsWith("overgeared:") && (
                        id.endsWith("_sword_blade")
                                || id.endsWith("_axe_head")
                                || id.endsWith("_spear_head")
                                || id.endsWith("_bow_limb")
                                || id.endsWith("_staff_head")
                                || id.endsWith("_dagger_blade")));
    }

    public static boolean isRuneEssenceId(String id) {
        return id != null && id.startsWith("statmod:rune_essence_");
    }

    public static boolean isGripId(String id) {
        return GRIP_IDS.contains(id);
    }

    public static boolean isShardLikeCatalystId(String id) {
        return id != null
                && (id.endsWith("_shard")
                || id.endsWith("_gem")
                || isRuneEssenceId(id));
    }

    public static boolean isAnvilSupportId(String id) {
        return ANVIL_SUPPORT_TOOL_IDS.contains(id)
                || (id != null && (id.endsWith("_tongs")
                || id.endsWith("_hammer")
                || id.endsWith("_smithing_hammer")));
    }
}
