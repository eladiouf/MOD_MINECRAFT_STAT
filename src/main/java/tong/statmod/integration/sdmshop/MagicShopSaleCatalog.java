package tong.statmod.integration.sdmshop;

import java.util.List;

public final class MagicShopSaleCatalog {
    private static final List<SaleOffer> OFFERS = List.of(
            offer("cobblestone", "basic_materials", "minecraft:cobblestone", 64, 8),
            offer("stone", "basic_materials", "minecraft:stone", 64, 8),
            offer("sand", "basic_materials", "minecraft:sand", 64, 8),
            offer("gravel", "basic_materials", "minecraft:gravel", 64, 8),
            offer("clay", "basic_materials", "minecraft:clay_ball", 64, 8),
            offer("oak_logs", "basic_materials", "minecraft:oak_log", 16, 20),
            offer("spruce_logs", "basic_materials", "minecraft:spruce_log", 16, 20),
            offer("birch_logs", "basic_materials", "minecraft:birch_log", 16, 20),
            offer("wheat", "agriculture", "minecraft:wheat", 32, 25),
            offer("carrots", "agriculture", "minecraft:carrot", 32, 25),
            offer("potatoes", "agriculture", "minecraft:potato", 32, 25),
            offer("beetroots", "agriculture", "minecraft:beetroot", 32, 25),
            offer("sugar_cane", "agriculture", "minecraft:sugar_cane", 32, 18),
            offer("cactus", "agriculture", "minecraft:cactus", 32, 15),
            offer("kelp", "agriculture", "minecraft:kelp", 32, 12),
            offer("leather", "agriculture", "minecraft:leather", 16, 35),
            offer("coal", "mining", "minecraft:coal", 16, 40),
            offer("raw_copper", "mining", "minecraft:raw_copper", 8, 60),
            offer("raw_iron", "mining", "minecraft:raw_iron", 8, 80),
            offer("raw_gold", "mining", "minecraft:raw_gold", 4, 100),
            offer("redstone", "mining", "minecraft:redstone", 16, 45),
            offer("lapis", "mining", "minecraft:lapis_lazuli", 16, 55),
            offer("quartz", "mining", "minecraft:quartz", 16, 60),
            offer("diamond", "mining", "minecraft:diamond", 1, 180),
            offer("rotten_flesh", "creature_loot", "minecraft:rotten_flesh", 16, 20),
            offer("bones", "creature_loot", "minecraft:bone", 16, 45),
            offer("string", "creature_loot", "minecraft:string", 16, 30),
            offer("gunpowder", "creature_loot", "minecraft:gunpowder", 16, 55),
            offer("spider_eyes", "creature_loot", "minecraft:spider_eye", 16, 35),
            offer("slime_balls", "creature_loot", "minecraft:slime_ball", 16, 50),
            offer("blaze_rods", "creature_loot", "minecraft:blaze_rod", 16, 70)
    );

    private MagicShopSaleCatalog() {
    }

    public static List<SaleOffer> offers() {
        return OFFERS;
    }

    private static SaleOffer offer(String id, String category, String itemId, int count, long value) {
        return new SaleOffer(id, category, itemId, count, value);
    }

    public record SaleOffer(String id, String category, String itemId, int count, long value) {
        public boolean sellOnly() {
            return true;
        }
    }
}
