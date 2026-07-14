package tong.statmod.integration.sdm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import static tong.statmod.integration.sdm.SDMWeaponPoisonHandler.TAG_POISON_DURATION;
import static tong.statmod.integration.sdm.SDMWeaponPoisonHandler.TAG_POISON_AMPLIFIER;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Initialise automatiquement la base de données de SDM Shop (onglets et articles)
 * au démarrage du serveur si elle est vide, avec des onglets et articles cohérents RPG.
 * Enregistre aussi la monnaie FDP_cfa dans SDM Economy si absente.
 */
public final class SDMShopDatabaseInitializer {

    private static final String CURRENCY_NAME = tong.statmod.economy.FdpDenomination.CURRENCY_ID;
    private static boolean currencyRegistered = false;

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Path worldDir = server.getWorldPath(LevelResource.ROOT);

        // 1. Enregistre la monnaie FDP_cfa dans SDM Economy (fait une seule fois)
        registerCurrency();

        // 2. Assure que le fichier currencies.data existe dans le monde
        ensureCurrencyDataFile(worldDir);

        // 3. Vérifie les fichiers shop et regénère si besoin (mauvaise monnaie ou fichier corrompu)
        ensureShopDataFiles(worldDir, server);

        // 4. Log de confirmation des fichiers écrits
        Path tabFile = worldDir.resolve("SDMShopData").resolve("SDMTovarTab.sdm");
        Path listFile = worldDir.resolve("SDMShopData").resolve("SDMTovarList.sdm");
        try {
            if (tabFile.toFile().exists()) {
                var tag = NbtIo.read(tabFile);
                int tabCount = tag != null ? tag.getList("tabName", 10).size() : -1;
                tong.statmod.STATMod.LOGGER.info("[Shop] ServerAboutToStart: SDMTovarTab.sdm contient {} tabs", tabCount);
            }
            if (listFile.toFile().exists()) {
                var tag = NbtIo.read(listFile);
                int itemCount = tag != null ? tag.getList("tovarList", 10).size() : -1;
                tong.statmod.STATMod.LOGGER.info("[Shop] ServerAboutToStart: SDMTovarList.sdm contient {} items", itemCount);
            }
        } catch (Exception e) {
            tong.statmod.STATMod.LOGGER.error("[Shop] Erreur vérification post-écriture", e);
        }
    }

    private static void registerCurrency() {
        if (currencyRegistered) return;
        currencyRegistered = true;
        try {
            net.sixik.sdmeconomy.api.CustomCurrencies.CURRENCIES.putIfAbsent(
                CURRENCY_NAME,
                (Supplier<net.sixik.sdmeconomy.economy.Currency>) () ->
                    new net.sixik.sdmeconomy.economy.Currency(CURRENCY_NAME)
            );
            tong.statmod.STATMod.LOGGER.info("[Shop] Monnaie {} enregistrée dans SDM Economy", CURRENCY_NAME);
        } catch (Exception e) {
            tong.statmod.STATMod.LOGGER.error("[Shop] Impossible d'enregistrer la monnaie {}", CURRENCY_NAME, e);
        }
    }

    private static void ensureCurrencyDataFile(Path worldDir) {
        Path currencyFile = worldDir.resolve("SDMEconomy/currencies/currencies.data");
        if (Files.exists(currencyFile)) return;

        try {
            Files.createDirectories(currencyFile.getParent());
            CompoundTag root = new CompoundTag();
            ListTag currencies = new ListTag();
            CompoundTag currency = new CompoundTag();
            currency.putString("name", CURRENCY_NAME);
            CompoundTag symbol = new CompoundTag();
            symbol.putByte("type", (byte) 0);
            symbol.putString("value", "\u25CE");
            currency.put("symbol", symbol);
            currency.putDouble("defaultValue", 0.0);
            currencies.add(currency);
            root.put("currencies", currencies);
            NbtIo.write(root, currencyFile);
            tong.statmod.STATMod.LOGGER.info("[Shop] Fichier currencies.data créé avec {}", CURRENCY_NAME);
        } catch (Exception e) {
            tong.statmod.STATMod.LOGGER.error("[Shop] Impossible de créer currencies.data", e);
        }
    }

    private static void ensureShopDataFiles(Path worldDir, MinecraftServer server) {
        Path sdmShopDir = worldDir.resolve("SDMShopData");
        Path tabFile = sdmShopDir.resolve("SDMTovarTab.sdm");
        Path listFile = sdmShopDir.resolve("SDMTovarList.sdm");

        try {
            boolean needsRegen = !Files.exists(tabFile) || !Files.exists(listFile)
                || Files.size(tabFile) < 20 || Files.size(listFile) < 20
                || !hasCorrectCurrency(listFile)
                || !hasCurrentCatalogVersion(tabFile, listFile);

            if (needsRegen) {
                tong.statmod.STATMod.LOGGER.info("[Shop] Régénération des fichiers SDM Shop (currency={})", CURRENCY_NAME);
                Files.createDirectories(sdmShopDir);
                initializeShopData(tabFile, listFile, server);
            }
        } catch (IOException e) {
            tong.statmod.STATMod.LOGGER.error("[Shop] Erreur vérification fichiers SDM Shop", e);
        }
    }

    private static boolean hasCorrectCurrency(Path listFile) throws IOException {
        if (!Files.exists(listFile) || Files.size(listFile) < 20) return false;
        CompoundTag root = NbtIo.read(listFile);
        if (root == null) return false;
        ListTag tovarList = root.getList("tovarList", 10);
        if (tovarList.isEmpty()) return false;
        CompoundTag first = tovarList.getCompound(0);
        return CURRENCY_NAME.equals(first.getString("currency"));
    }

    private static boolean hasCurrentCatalogVersion(Path tabFile, Path listFile) throws IOException {
        CompoundTag tabs = NbtIo.read(tabFile);
        CompoundTag items = NbtIo.read(listFile);
        return tabs != null && items != null
            && SDMShopCatalog.isCurrentVersion(tabs.getInt("statmodCatalogVersion"))
            && SDMShopCatalog.isCurrentVersion(items.getInt("statmodCatalogVersion"));
    }

    private static void initializeShopData(Path tabFile, Path listFile, MinecraftServer server) {
        var registryAccess = server.registryAccess();

        CompoundTag tabRoot = new CompoundTag();
        ListTag tabListTag = new ListTag();
        for (SDMShopCatalog.ShopTab tab : SDMShopCatalog.tabs()) {
            Item icon = resolveItem(tab.iconId());
            if (icon == null) {
                tong.statmod.STATMod.LOGGER.warn("[Shop] Icône absente {}, onglet {} ignoré", tab.iconId(), tab.name());
                continue;
            }
            CompoundTag tabTag = new CompoundTag();
            tabTag.putString("name", tab.name());
            tabTag.put("item", new ItemStack(icon).save(registryAccess));
            tabListTag.add(tabTag);
        }
        tabRoot.putInt("statmodCatalogVersion", SDMShopCatalog.VERSION);
        tabRoot.put("tabName", tabListTag);

        CompoundTag listRoot = new CompoundTag();
        ListTag tovarListTag = new ListTag();
        for (SDMShopCatalog.ShopItem entry : SDMShopCatalog.items()) {
            Item item = resolveItem(entry.itemId());
            if (item == null) {
                tong.statmod.STATMod.LOGGER.warn("[Shop] Objet absent {}, entrée ignorée", entry.itemId());
                continue;
            }
            ItemStack stack = createStack(item, entry, registryAccess);
            addShopItem(server, tovarListTag, entry.tab(), stack, entry.price());
        }

        listRoot.putInt("statmodCatalogVersion", SDMShopCatalog.VERSION);
        listRoot.put("tovarList", tovarListTag);

        try {
            NbtIo.write(tabRoot, tabFile);
            NbtIo.write(listRoot, listFile);
            tong.statmod.STATMod.LOGGER.info("[Shop] Base de données SDM Shop générée et écrite avec succès !");
        } catch (IOException e) {
            tong.statmod.STATMod.LOGGER.error("[Shop] Erreur lors de l'écriture des fichiers NBT de SDM Shop", e);
        }
    }

    private static Item resolveItem(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return null;
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    private static ItemStack createStack(Item item, SDMShopCatalog.ShopItem entry, RegistryAccess registryAccess) {
        if ("strong_healing".equals(entry.potionId())) {
            return PotionContents.createItemStack(item, Potions.STRONG_HEALING);
        }
        if ("strong_strength".equals(entry.potionId())) {
            return PotionContents.createItemStack(item, Potions.STRONG_STRENGTH);
        }
        ItemStack stack = new ItemStack(item, entry.count());
        applyTier5Enchantments(stack, entry.itemId(), registryAccess);
        return stack;
    }

    private static void applyTier5Enchantments(ItemStack stack, String itemId, RegistryAccess registryAccess) {
        var enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);

        // 300k FDP — Mythic unique weapons
        if (itemId.contains("spatial_blade") || itemId.contains("dead_end") || itemId.contains("stormbringer")
            || itemId.contains("hearthflame") || itemId.contains("soulpyre") || itemId.contains("vorpal")) {
            stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 30);
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 10);
            stack.enchant(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), 5);
            stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 10);
            stack.enchant(enchantments.getOrThrow(Enchantments.KNOCKBACK), 5);
            tagPoison(stack, 200, 4);
            return;
        }

        // 240k FDP — Dragonsteel swords
        if (itemId.contains("dragonsteel")) {
            stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 25);
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 8);
            stack.enchant(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), 4);
            stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 8);
            tagPoison(stack, 160, 4);
            return;
        }

        // 225k FDP — Adamantite (Tensura top tier)
        if (itemId.contains("adamantite")) {
            stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 20);
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 7);
            stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 6);
            tagPoison(stack, 160, 4);
            return;
        }

        // 180k FDP — Runic / Soul weapons
        if (itemId.contains("runic_") || itemId.contains("soulkeeper") || itemId.contains("soulrender")
            || itemId.contains("soulstealer") || itemId.contains("twisted_blade")) {
            stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 15);
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 5);
            stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 4);
            tagPoison(stack, 120, 3);
            return;
        }

        // 120k FDP — Unique named weapons
        if (itemId.contains("dread_sword") || itemId.contains("tide_trident")
            || itemId.contains("ghost_sword") || itemId.contains("hippogryph_sword")
            || itemId.contains("ice_blade") || itemId.contains("mad_swords")
            || itemId.contains("mirrorguard") || itemId.contains("wildvine")
            || itemId.contains("bloomsoul")) {
            stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 12);
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 4);
            tagPoison(stack, 100, 2);
            return;
        }

        // 112.5k FDP — High magisteel
        if (itemId.contains("high_magisteel")) {
            stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 10);
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 3);
            tagPoison(stack, 80, 2);
        }
    }

    private static void tagPoison(ItemStack stack, int duration, int amplifier) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            var tag = data.copyTag();
            tag.putInt(TAG_POISON_DURATION, duration);
            tag.putInt(TAG_POISON_AMPLIFIER, amplifier);
            return CustomData.of(tag);
        });
    }

    private static void addShopItem(MinecraftServer server, ListTag list, String tabName, ItemStack stack, int price) {
        CompoundTag itemTag = new CompoundTag();
        
        // Champs communs AbstractTovar
        itemTag.putString("id", "ItemType");
        itemTag.putString("tovarType", "ItemType");
        itemTag.putUUID("uuid", UUID.randomUUID());
        itemTag.putString("tab", tabName);
        itemTag.putInt("cost", price);
        itemTag.putLong("limit", 999999L); // infini
        itemTag.putString("currency", CURRENCY_NAME);
        itemTag.putBoolean("toSell", false); // Achat par le joueur

        // Champs spécifiques TovarItem
        itemTag.putBoolean("byTag", false);
        
        itemTag.put("item", stack.save(server.registryAccess()));

        list.add(itemTag);
    }

    // ──────────────────────────────────────────────
    // Debug : log l'état des données SDM Shop
    // ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var server = event.getServer();
        var reg = server.registryAccess();
        tong.statmod.STATMod.LOGGER.info("===== SDM SHOP DEBUG =====");

        // 1) Log TovarTab.SERVER (chargé par SDM Shop sur SERVER_STARTED)
        var tabServer = net.sixk.sdmshop.shop.Tab.TovarTab.SERVER;
        if (tabServer == null) {
            tong.statmod.STATMod.LOGGER.info("[SDM] TovarTab.SERVER = null");
        } else {
            tong.statmod.STATMod.LOGGER.info("[SDM] TovarTab.SERVER.tabList size: {}", tabServer.tabList.size());
            for (var t : tabServer.tabList) {
                tong.statmod.STATMod.LOGGER.info("[SDM]   Tab: '{}' item={}", t.name, t.item);
            }
        }

        // 2) Log TovarList.SERVER (chargé par SDM Shop sur PLAYER_JOIN, donc normalement vide ici)
        var listServer = net.sixk.sdmshop.shop.Tovar.TovarList.SERVER;
        if (listServer == null) {
            tong.statmod.STATMod.LOGGER.info("[SDM] TovarList.SERVER = null");
        } else {
            tong.statmod.STATMod.LOGGER.info("[SDM] TovarList.SERVER.tovarList size: {}", listServer.tovarList.size());
        }

        // 3) Log le contenu des fichiers sur le disque
        var worldDir = server.getWorldPath(LevelResource.ROOT);
        var sdmDir = worldDir.resolve("SDMShopData");
        var tabFile = sdmDir.resolve("SDMTovarTab.sdm");
        var listFile = sdmDir.resolve("SDMTovarList.sdm");
        try {
            if (tabFile.toFile().exists()) {
                var tag = NbtIo.read(tabFile);
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarTab.sdm présent, contenu: {}",
                    tag != null ? tag.getList("tabName", 10).size() + " tabs" : "null");
            } else {
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarTab.sdm ABSENT !");
            }
            if (listFile.toFile().exists()) {
                var tag = NbtIo.read(listFile);
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarList.sdm présent, contenu: {}",
                    tag != null ? tag.getList("tovarList", 10).size() + " items" : "null");
            } else {
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarList.sdm ABSENT !");
            }
        } catch (Exception e) {
            tong.statmod.STATMod.LOGGER.error("[SDM] Erreur lecture fichiers debug", e);
        }

        tong.statmod.STATMod.LOGGER.info("===== FIN SDM DEBUG =====");
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var tabServer = net.sixk.sdmshop.shop.Tab.TovarTab.SERVER;
        var listServer = net.sixk.sdmshop.shop.Tovar.TovarList.SERVER;

        tong.statmod.STATMod.LOGGER.info("[SDM] PlayerJoin {} : TovarTab.SERVER.tabList={} TovarList.SERVER.tovarList={}",
            player.getScoreboardName(),
            tabServer != null ? tabServer.tabList.size() : -1,
            listServer != null ? listServer.tovarList.size() : -1);
        var worldDir = player.server.getWorldPath(LevelResource.ROOT);
        try {
            var tabFile = worldDir.resolve("SDMShopData").resolve("SDMTovarTab.sdm");
            if (tabFile.toFile().exists()) {
                var tag = NbtIo.read(tabFile);
                int tabCount = tag != null ? tag.getList("tabName", 10).size() : -1;
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarTab.sdm: {} tabs", tabCount);
            } else {
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarTab.sdm ABSENT au join !");
            }
            var listFile = worldDir.resolve("SDMShopData").resolve("SDMTovarList.sdm");
            if (listFile.toFile().exists()) {
                var tag = NbtIo.read(listFile);
                int itemCount = tag != null ? tag.getList("tovarList", 10).size() : -1;
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarList.sdm: {} items", itemCount);
            } else {
                tong.statmod.STATMod.LOGGER.info("[SDM] SDMTovarList.sdm ABSENT au join !");
            }
        } catch (Exception e) {
            tong.statmod.STATMod.LOGGER.error("[SDM] Erreur au player join", e);
        }
    }
}
