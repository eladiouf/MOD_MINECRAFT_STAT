package tong.statmod.progression;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import tong.statmod.stats.StatType;

import java.util.List;

public final class WeaponResolver {
    private WeaponResolver() {}

    private static final List<WeaponPattern> PATTERNS = List.of(
        // BRUTE (priority: longer/more specific first)
        pattern("great_sword", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("greatsword", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("scythe", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("club", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("hammer", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("mace", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("axe", StatType.BRUTE_FORCE, "melee_axe"),

        // BLADE (longer swords before generic "sword")
        pattern("long_sword", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("short_sword", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("needle_sword", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("katana", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("tachi", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("kodachi", StatType.RAPIDITE, "melee_sword"),

        // BRUTE (odachi after kodachi so "kodachi".contains("odachi") doesn't misclassify)
        pattern("odachi", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("dagger", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("sickle", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("rapier", StatType.BLADE_TECHNIQUE, "melee_sword"),
        pattern("sword", StatType.BLADE_TECHNIQUE, "melee_sword"),

        // PRECISION (crossbow before bow)
        pattern("crossbow", StatType.PRECISION, "ranged"),
        pattern("throwing", StatType.PRECISION, "ranged"),
        pattern("spear", StatType.PRECISION, "ranged"),
        pattern("trident", StatType.PRECISION, "ranged"),
        pattern("bow", StatType.PRECISION, "ranged"),
        pattern("ranged", StatType.PRECISION, "ranged"),

        // ARCANE
        pattern("staff", StatType.ARCANE_POWER, "magic_fire"),
        pattern("wand", StatType.ARCANE_POWER, "magic_fire"),
        pattern("spell", StatType.ARCANE_POWER, "magic_fire"),
        pattern("magic", StatType.ARCANE_POWER, "magic_fire"),

        // FIST
        pattern("gauntlet", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("knuckle", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("fist", StatType.BRUTE_FORCE, "melee_axe"),
        pattern("hand", StatType.BRUTE_FORCE, "melee_axe")
    );

    private record WeaponPattern(String keyword, StatType stat, String tensuraAction) {
        boolean matches(String path) {
            return path.contains(keyword);
        }
    }

    private static WeaponPattern pattern(String keyword, StatType stat, String tensuraAction) {
        return new WeaponPattern(keyword.toLowerCase(), stat, tensuraAction);
    }

    private static String pathFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
    }

    public static StatType statFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return StatType.BRUTE_FORCE;
        if (stack.getItem() instanceof TridentItem || stack.getItem() instanceof ProjectileWeaponItem) {
            return StatType.PRECISION;
        }
        String path = pathFor(stack);
        for (WeaponPattern p : PATTERNS) {
            if (p.matches(path)) return p.stat();
        }
        return StatType.BRUTE_FORCE;
    }

    public static String tensuraActionFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "melee_axe";
        if (stack.getItem() instanceof TridentItem || stack.getItem() instanceof ProjectileWeaponItem) {
            return "ranged";
        }
        String path = pathFor(stack);
        for (WeaponPattern p : PATTERNS) {
            if (p.matches(path)) return p.tensuraAction();
        }
        return "melee_axe";
    }

    public static String tensuraActionFor(String itemId) {
        String path = itemId == null ? "" : itemId.toLowerCase();
        for (WeaponPattern p : PATTERNS) {
            if (p.matches(path)) return p.tensuraAction();
        }
        return "melee_axe";
    }

    public static StatType statForPath(String path) {
        if (path == null) return StatType.BRUTE_FORCE;
        path = path.toLowerCase();
        for (WeaponPattern p : PATTERNS) {
            if (p.matches(path)) return p.stat();
        }
        return StatType.BRUTE_FORCE;
    }

    public static StatType combatStatForWeaponCategory(String weaponCategory) {
        if (weaponCategory == null) return StatType.BRUTE_FORCE;
        return switch (weaponCategory.toUpperCase()) {
            case "SWORD", "LONGSWORD", "UCHIGATANA", "DAGGER", "TACHI" -> StatType.BLADE_TECHNIQUE;
            case "AXE", "GREATSWORD", "SCYTHE", "CLUB" -> StatType.BRUTE_FORCE;
            case "BOW", "CROSSBOW", "TRIDENT", "SPEAR", "RANGED" -> StatType.PRECISION;
            case "FIST" -> StatType.BRUTE_FORCE;
            case "KODACHI" -> StatType.RAPIDITE;
            case "STAFF", "WAND" -> StatType.ARCANE_POWER;
            case "GAUNTLET" -> StatType.BRUTE_FORCE;
            default -> StatType.BRUTE_FORCE;
        };
    }
}
