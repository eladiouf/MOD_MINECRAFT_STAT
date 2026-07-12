package tong.statmod.integration.overgeared;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OvergearedRecipeGate {
    private OvergearedRecipeGate() {}

    /**
     * Gating dual-stat des métaux source du modpack. Clé = id de l'ingot froid
     * (modid:item_path). Valeur = niveaux requis FORGING/ERUDITION/ARCANE_POWER pour
     * pouvoir le chauffer en {@code statmod:heated_<material>_ingot}.
     *
     * <p>Voir spec {@code 2026-06-30-overgeared-universal-forge-design.md} §P4.
     */
    public static final Map<ResourceLocation, MaterialGate> MATERIAL_GATES = buildMaterialGates();

    public record MaterialGate(int forging, int erudition, int arcanePower) {
        public boolean satisfies(PlayerStatData data) {
            if (data == null) return false;
            return data.getLevel(StatType.FORGING.index) >= forging
                    && data.getLevel(StatType.ERUDITION.index) >= erudition
                    && data.getLevel(StatType.ARCANE_POWER.index) >= arcanePower;
        }
    }

    private static Map<ResourceLocation, MaterialGate> buildMaterialGates() {
        Map<ResourceLocation, MaterialGate> m = new LinkedHashMap<>();
        // Vanilla manquants — aligné sur MATERIAL_NAME_GATES (heating = weapon gate)
        m.put(ResourceLocation.parse("minecraft:gold_ingot"),                 new MaterialGate(8, 0, 0));
        m.put(ResourceLocation.parse("minecraft:diamond"),                    new MaterialGate(20, 0, 0));
        // Overgeared natif — gate explicite pour cohérence avec MATERIAL_NAME_GATES
        m.put(ResourceLocation.parse("minecraft:netherite_ingot"),            new MaterialGate(35, 0, 0));
        // magistuarmory
        m.put(ResourceLocation.parse("magistuarmory:tin_ingot"),              new MaterialGate(3, 0, 0));
        m.put(ResourceLocation.parse("magistuarmory:bronze_ingot"),           new MaterialGate(5, 0, 0));
        // Iron's Spellbooks
        m.put(ResourceLocation.parse("irons_spellbooks:pyrium_ingot"),        new MaterialGate(12, 0, 0));
        m.put(ResourceLocation.parse("irons_spellbooks:arcane_ingot"),        new MaterialGate(15, 5, 0));
        m.put(ResourceLocation.parse("irons_spellbooks:mithril_ingot"),       new MaterialGate(20, 8, 0));
        // Tensura
        m.put(ResourceLocation.parse("tensura:mithril_ingot"),                new MaterialGate(20, 8, 0));
        m.put(ResourceLocation.parse("tensura:low_magisteel_ingot"),          new MaterialGate(25, 10, 5));
        m.put(ResourceLocation.parse("tensura:magisteel_ingot"),              new MaterialGate(30, 12, 8));
        m.put(ResourceLocation.parse("tensura:pure_magisteel_ingot"),         new MaterialGate(40, 15, 10));
        m.put(ResourceLocation.parse("tensura:high_magisteel_ingot"),         new MaterialGate(50, 20, 12));
        m.put(ResourceLocation.parse("tensura:orichalcum_ingot"),             new MaterialGate(55, 22, 15));
        m.put(ResourceLocation.parse("tensura:adamantite_ingot"),             new MaterialGate(60, 25, 18));
        m.put(ResourceLocation.parse("tensura:hihiirokane_ingot"),            new MaterialGate(70, 30, 22));
        return Map.copyOf(m);
    }

    /**
     * Retourne true si {@code player} avec ses stats {@code data} peut chauffer
     * l'ingot {@code ingotId}. Les ingots non listés (copper/iron/steel/silver
     * gérés par Overgeared natif) ne sont jamais gates côté STAT MOD : on retourne true.
     * Netherite est listé (gate 35) car Overgeared ne le heat pas nativement.
     */
    public static boolean canHeat(ResourceLocation ingotId, PlayerStatData data) {
        if (ingotId == null) return false;
        MaterialGate gate = MATERIAL_GATES.get(ingotId);
        if (gate == null) return true;
        return gate.satisfies(data);
    }

    public static MaterialGate gateFor(ResourceLocation ingotId) {
        if (ingotId == null) return null;
        return MATERIAL_GATES.get(ingotId);
    }

    /**
     * Mission M5 Phase γ — map material name (string canonique, ex: "hihiirokane") vers
     * sa MaterialGate. Utilisé par {@link #gateForWeapon} pour appliquer le gating à
     * l'assemblage final d'une arme externe.
     *
     * <p>Pour les matériaux mineurs (silver/netherite/copper/iron/steel) on attribue une
     * gate proportionnée même si le rough natif d'Overgeared n'est pas STAT MOD. Cela
     * évite qu'un newbie craft un netherite_sword sans FORGING.
     */
    private static final Map<String, MaterialGate> MATERIAL_NAME_GATES = Map.ofEntries(
            Map.entry("wood",            new MaterialGate(0, 0, 0)),
            Map.entry("stone",           new MaterialGate(0, 0, 0)),
            Map.entry("copper",          new MaterialGate(0, 0, 0)),
            Map.entry("tin",             new MaterialGate(3, 0, 0)),
            Map.entry("iron",            new MaterialGate(5, 0, 0)),
            Map.entry("bronze",          new MaterialGate(5, 0, 0)),
            Map.entry("gold",            new MaterialGate(8, 0, 0)),
            Map.entry("silver",          new MaterialGate(8, 0, 0)),
            Map.entry("steel",           new MaterialGate(15, 0, 0)),
            Map.entry("pyrium",          new MaterialGate(12, 0, 0)),
            Map.entry("arcane",          new MaterialGate(15, 5, 0)),
            Map.entry("diamond",         new MaterialGate(20, 0, 0)),
            Map.entry("mithril",         new MaterialGate(20, 8, 0)),
            Map.entry("low_magisteel",   new MaterialGate(25, 10, 5)),
            Map.entry("magisteel",       new MaterialGate(30, 12, 8)),
            Map.entry("netherite",       new MaterialGate(35, 0, 0)),
            Map.entry("pure_magisteel",  new MaterialGate(40, 15, 10)),
            Map.entry("high_magisteel",  new MaterialGate(50, 20, 12)),
            Map.entry("orichalcum",      new MaterialGate(55, 22, 15)),
            Map.entry("adamantite",      new MaterialGate(60, 25, 18)),
            Map.entry("hihiirokane",     new MaterialGate(70, 30, 22))
    );

    public static MaterialGate gateForMaterial(String materialName) {
        if (materialName == null) return null;
        return MATERIAL_NAME_GATES.get(materialName);
    }

    /**
     * Mission M5 Phase γ — retourne la gate à appliquer à l'assemblage final d'une arme
     * externe. Détecte le matériau via {@link WeaponMaterialDetector} puis lookup dans
     * {@link #MATERIAL_NAME_GATES}.
     *
     * @return gate à appliquer, ou null si pas de gating (matériau inconnu ou arme
     *         hors scope).
     */
    public static MaterialGate gateForWeapon(ResourceLocation weaponId) {
        String material = WeaponMaterialDetector.detectMaterial(weaponId);
        if (material == null) return null;
        return MATERIAL_NAME_GATES.get(material);
    }

    // ─── Legacy API : recipe results de statmod (perk_tome, respec_stone) ──────────────

    public static boolean isForgingRecipeResult(ResourceLocation resultId) {
        if (resultId == null) {
            return false;
        }
        return "statmod".equals(resultId.getNamespace())
                && ("perk_tome".equals(resultId.getPath()) || "respec_stone".equals(resultId.getPath()));
    }

    public static boolean isForgingRecipeResult(ItemStack resultStack) {
        if (resultStack == null || resultStack.isEmpty()) {
            return false;
        }
        return isForgingRecipeResult(BuiltInRegistries.ITEM.getKey(resultStack.getItem()));
    }

    public static int requiredForgingLevel(ResourceLocation resultId) {
        if (resultId == null || !"statmod".equals(resultId.getNamespace())) {
            return 0;
        }
        return switch (resultId.getPath()) {
            case "respec_stone" -> 20;
            case "perk_tome" -> 35;
            default -> 0;
        };
    }

    public static int requiredForgingLevel(ItemStack resultStack) {
        if (resultStack == null || resultStack.isEmpty()) {
            return 0;
        }
        return requiredForgingLevel(BuiltInRegistries.ITEM.getKey(resultStack.getItem()));
    }

    public static boolean canCraft(ResourceLocation resultId, int forgingLevel) {
        return Math.max(0, forgingLevel) >= requiredForgingLevel(resultId);
    }

    public static boolean canCraft(ItemStack resultStack, int forgingLevel) {
        return Math.max(0, forgingLevel) >= requiredForgingLevel(resultStack);
    }
}
