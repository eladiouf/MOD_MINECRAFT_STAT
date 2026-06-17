package tong.statmod.integration.tensura;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatType;

import java.util.Map;

public final class TensuraSpellGate {
    private static final Map<String, String> PERK_TO_TENSURA = Map.of(
            "fire_bolt", "tensura:fire_bolt",
            "water_heal", "tensura:healing_rain",
            "wind_blade", "tensura:wind_cutter",
            "earth_wall", "tensura:earth_barrier",
            "darkness_veil", "tensura:darkness",
            "light_bind", "tensura:light_binding",
            "space_shift", "tensura:spatial_movement"
    );

    private static final Map<String, String> PERK_TO_MAHOU = Map.of(
            "mystic_staff", "mahoutsukai:mystic_staff_spell_scroll",
            "gandr", "mahoutsukai:gandr_spell_scroll",
            "rho_aias", "mahoutsukai:rho_aias_spell_scroll",
            "fallen_down", "mahoutsukai:fallen_down_spell_scroll"
    );

    private TensuraSpellGate() {}

    public static String resolveTensuraSpellId(String perkKey) {
        return TensuraSkillIds.canonicalize(PERK_TO_TENSURA.get(perkKey));
    }

    public static String resolveMahouScrollId(String perkKey) {
        return PERK_TO_MAHOU.get(perkKey);
    }

    public static String resolveForPerk(Perk perk) {
        if (perk == null) return null;
        String key = perk.name().toLowerCase();
        String mapped = PERK_TO_TENSURA.get(key);
        if (mapped != null) {
            return TensuraSkillIds.canonicalize(mapped);
        }
        mapped = PERK_TO_MAHOU.get(key);
        if (mapped != null) {
            return mapped;
        }
        return TensuraSkillIds.canonicalize(resolveByStat(perk.stat, perk.tier));
    }

    public static boolean grantReward(Player player, Perk perk) {
        String resolved = resolveForPerk(perk);
        if (player == null || resolved == null) {
            return false;
        }

        if (resolved.startsWith("tensura:")) {
            return SkillAPI.getSkillsFrom(player).learnSkill(ResourceLocation.parse(resolved));
        }

        ResourceLocation itemId = ResourceLocation.parse(resolved);
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(ItemStack::new)
                .map(stack -> player.getInventory().add(stack))
                .orElse(false);
    }

    public static boolean grantReward(Player player, String perkKey) {
        if (player == null || perkKey == null) return false;
        String resolved = PERK_TO_TENSURA.getOrDefault(perkKey, PERK_TO_MAHOU.get(perkKey));
        if (resolved == null) return false;

        if (resolved.startsWith("tensura:")) {
            return SkillAPI.getSkillsFrom(player).learnSkill(ResourceLocation.parse(TensuraSkillIds.canonicalize(resolved)));
        }

        ResourceLocation itemId = ResourceLocation.parse(resolved);
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(ItemStack::new)
                .map(stack -> player.getInventory().add(stack))
                .orElse(false);
    }

    private static String resolveByStat(StatType stat, PerkTier tier) {
        if (stat == null) return null;

        return switch (stat) {
            case BRUTE_FORCE -> "tensura:berserk";
            case BLADE_TECHNIQUE -> tier == PerkTier.MASTERY || tier == PerkTier.TRANSCENDENCE
                    ? "tensura:iaijutsu" : "tensura:sword_meister";
            case RAPIDITE -> "tensura:accelerated_thoughts";
            case AGILITY -> tier == PerkTier.MASTERY || tier == PerkTier.TRANSCENDENCE
                    ? "tensura:space_manipulation" : "tensura:agility_enhance";
            case PHYSICAL_RESISTANCE -> "tensura:resistance_enhance";
            case PHYSICAL_ENDURANCE -> "tensura:endurance";
            case PRECISION -> "tensura:precision";
            case TRACKING -> "tensura:tracking";
            case KEEN_SENSES -> "tensura:magic_sense";
            case FORGING -> "tensura:blacksmithing";
            case COOKING -> "tensura:cooking";
            case ALCHEMY -> "tensura:alchemy";
            case INTIMIDATION -> "tensura:domination";
            case WILLPOWER -> "tensura:willpower";
            default -> null;
        };
    }
}
