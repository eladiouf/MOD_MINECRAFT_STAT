package tong.statmod.integration.sdm;

import java.util.List;

/** Données déclaratives du shop, séparées de la sérialisation NBT de SDM Shop. */
public final class SDMShopCatalog {
    public static final int VERSION = 2;
    private static final List<String> FORBIDDEN_ID_PARTS = List.of(
        "spawn_egg", "boss_summoner", "creative", "command_block",
        "structure_block", "debug_stick"
    );
    private static final List<ShopTab> TABS = List.of(
        tab("Armes", "minecraft:iron_sword"),
        tab("Armures", "minecraft:iron_chestplate"),
        tab("Potions", "minecraft:potion"),
        tab("Artisanat", "minecraft:crafting_table"),
        tab("Magie", "irons_spellbooks:scroll"),
        tab("Équipement Épique", "epicfight:iron_greatsword"),
        tab("Chasseur de monstres", "iceandfire:dragonbone"),
        tab("Artisanat Avancé", "minecraft:netherite_ingot")
    );

    private static final List<ShopItem> ITEMS = List.of(
        item("Armes", "minecraft:wooden_sword", 5), item("Armes", "minecraft:stone_sword", 10),
        item("Armes", "minecraft:iron_sword", 25), item("Armes", "minecraft:diamond_sword", 100),
        item("Armes", "minecraft:netherite_sword", 400), item("Armes", "minecraft:iron_axe", 20),
        item("Armes", "minecraft:diamond_axe", 90), item("Armes", "minecraft:bow", 15),
        item("Armes", "minecraft:crossbow", 20), item("Armes", "minecraft:arrow", 1, 64),

        item("Armures", "minecraft:leather_chestplate", 10), item("Armures", "minecraft:iron_helmet", 20),
        item("Armures", "minecraft:iron_chestplate", 40), item("Armures", "minecraft:iron_leggings", 30),
        item("Armures", "minecraft:iron_boots", 15), item("Armures", "minecraft:shield", 15),
        item("Armures", "minecraft:diamond_chestplate", 150), item("Armures", "minecraft:netherite_helmet", 300),
        item("Armures", "minecraft:netherite_chestplate", 500), item("Armures", "minecraft:netherite_leggings", 450),
        item("Armures", "minecraft:netherite_boots", 275), item("Armures", "iceandfire:armor_red_chestplate", 650),

        item("Potions", "minecraft:golden_apple", 50), item("Potions", "minecraft:enchanted_golden_apple", 500),
        item("Potions", "minecraft:golden_carrot", 8), item("Potions", "minecraft:cooked_beef", 2),
        item("Potions", "minecraft:ender_pearl", 15), item("Potions", "minecraft:experience_bottle", 5),
        potion("Potions", "minecraft:potion", "strong_healing", 75),
        potion("Potions", "minecraft:potion", "strong_strength", 90),

        item("Artisanat", "minecraft:coal", 1), item("Artisanat", "minecraft:iron_ingot", 5),
        item("Artisanat", "minecraft:gold_ingot", 10), item("Artisanat", "minecraft:diamond", 50),
        item("Artisanat", "minecraft:emerald", 15), item("Artisanat", "minecraft:obsidian", 20),
        item("Artisanat", "minecraft:anvil", 35), item("Artisanat", "minecraft:lapis_lazuli", 3),

        item("Magie", "irons_spellbooks:scroll", 35), item("Magie", "irons_spellbooks:common_ink", 15),
        item("Magie", "irons_spellbooks:uncommon_ink", 40), item("Magie", "irons_spellbooks:rare_ink", 90),
        item("Magie", "irons_spellbooks:epic_ink", 200), item("Magie", "irons_spellbooks:legendary_ink", 450),
        item("Magie", "irons_spellbooks:blank_rune", 25), item("Magie", "irons_spellbooks:arcane_rune", 75),
        item("Magie", "irons_spellbooks:arcane_essence", 30), item("Magie", "irons_spellbooks:arcane_ingot", 60),
        item("Magie", "tensura:high_magisteel_ingot", 180), item("Magie", "tensura:adamantite_ingot", 350),

        item("Équipement Épique", "epicfight:uchigatana", 180),
        item("Équipement Épique", "epicfight:iron_greatsword", 140),
        item("Équipement Épique", "epicfight:diamond_greatsword", 350),
        item("Équipement Épique", "epicfight:iron_spear", 110),
        item("Équipement Épique", "epicfight:diamond_spear", 300),
        item("Équipement Épique", "epicfight:netherite_tachi", 700),
        item("Équipement Épique", "apotheosis:gem", 250),
        item("Équipement Épique", "apotheosis:gem_dust", 60),
        item("Équipement Épique", "apotheosis:epic_material", 300),
        item("Équipement Épique", "apotheosis:mythic_material", 700),

        item("Chasseur de monstres", "iceandfire:dragonbone", 120),
        item("Chasseur de monstres", "iceandfire:fire_dragon_blood", 240),
        item("Chasseur de monstres", "iceandfire:ice_dragon_blood", 240),
        item("Chasseur de monstres", "iceandfire:lightning_dragon_blood", 260),
        item("Chasseur de monstres", "iceandfire:dragonscales_red", 90),
        item("Chasseur de monstres", "iceandfire:witherbone", 45),
        item("Chasseur de monstres", "mutantmonsters:chemical_x", 275),
        item("Chasseur de monstres", "mutantmonsters:creeper_shard", 325),

        item("Artisanat Avancé", "minecraft:netherite_scrap", 100),
        item("Artisanat Avancé", "minecraft:netherite_ingot", 350),
        item("Artisanat Avancé", "minecraft:emerald_block", 120),
        item("Artisanat Avancé", "minecraft:diamond_block", 400),
        item("Artisanat Avancé", "minecraft:netherite_upgrade_smithing_template", 500),
        item("Artisanat Avancé", "apotheosis:sigil_of_socketing", 450),
        item("Artisanat Avancé", "apotheosis:sigil_of_enhancement", 500)
    );

    private SDMShopCatalog() {}

    public static List<ShopTab> tabs() { return TABS; }
    public static List<ShopItem> items() { return ITEMS; }
    public static boolean isCurrentVersion(int version) { return version == VERSION; }
    public static boolean isForbiddenItemId(String itemId) {
        return FORBIDDEN_ID_PARTS.stream().anyMatch(itemId::contains);
    }

    private static ShopTab tab(String name, String iconId) { return new ShopTab(name, iconId); }
    private static ShopItem item(String tab, String id, int price) { return item(tab, id, price, 1); }
    private static ShopItem item(String tab, String id, int price, int count) {
        return new ShopItem(tab, id, price, count, null);
    }
    private static ShopItem potion(String tab, String id, String potionId, int price) {
        return new ShopItem(tab, id, price, 1, potionId);
    }

    public record ShopTab(String name, String iconId) {}
    public record ShopItem(String tab, String itemId, int price, int count, String potionId) {}
}
