package tong.statmod.integration.sdm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
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

    private static final String CURRENCY_NAME = "FDP_cfa";
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
            ItemStack stack = createStack(item, entry);
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

    private static ItemStack createStack(Item item, SDMShopCatalog.ShopItem entry) {
        if ("strong_healing".equals(entry.potionId())) {
            return PotionContents.createItemStack(item, Potions.STRONG_HEALING);
        }
        if ("strong_strength".equals(entry.potionId())) {
            return PotionContents.createItemStack(item, Potions.STRONG_STRENGTH);
        }
        return new ItemStack(item, entry.count());
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
