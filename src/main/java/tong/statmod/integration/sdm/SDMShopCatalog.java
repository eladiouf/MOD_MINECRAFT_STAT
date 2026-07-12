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
        tab("Minerais bruts", "minecraft:raw_iron"),
        tab("Lingots et gemmes", "minecraft:iron_ingot"),
        tab("Matériaux avancés", "minecraft:netherite_ingot"),
        tab("Forge et amélioration", "minecraft:smithing_table"),
        tab("Armes légères", "epicfight:dagger"),
        tab("Armes lourdes", "epicfight:iron_greatsword"),
        tab("Lances et armes d'hast", "epicfight:iron_spear"),
        tab("Armes à distance", "minecraft:bow"),
        tab("Armures classiques", "minecraft:iron_chestplate"),
        tab("Armures fantastiques", "iceandfire:armor_red_chestplate"),
        tab("Magie et parchemins", "irons_spellbooks:scroll"),
        tab("Runes et composants magiques", "irons_spellbooks:arcane_rune"),
        tab("Potions et soins", "minecraft:potion"),
        tab("Composants de monstres", "iceandfire:dragonbone"),
        tab("Nourriture", "minecraft:golden_apple"),
        tab("Construction", "minecraft:bricks"),
        tab("Utilitaires", "minecraft:compass"),
        tab("Objets rares contrôlés", "apotheosis:gem")
    );

    private static final List<ShopItem> ITEMS = SDMShopCatalogEntries.items();

    private SDMShopCatalog() {}

    public static List<ShopTab> tabs() { return TABS; }
    public static List<ShopItem> items() { return ITEMS; }
    public static boolean isCurrentVersion(int version) { return version == VERSION; }
    public static boolean isForbiddenItemId(String itemId) {
        return FORBIDDEN_ID_PARTS.stream().anyMatch(itemId::contains);
    }

    private static ShopTab tab(String name, String iconId) {
        return new ShopTab(name, iconId);
    }

    public record ShopTab(String name, String iconId) {}
    public record ShopItem(String tab, String itemId, int price, int count, String potionId) {}
}
