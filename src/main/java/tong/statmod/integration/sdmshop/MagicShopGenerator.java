package tong.statmod.integration.sdmshop;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.sixik.sdmshop.old_api.ShopEntryType;
import net.sixik.sdmshop.server.SDMShopServer;
import net.sixik.sdmshop.shop.BaseShop;
import net.sixik.sdmshop.shop.ShopEntry;
import net.sixik.sdmshop.shop.ShopTab;
import net.sixik.sdmshop.shop.seller_types.MoneySellerType;
import tong.statmod.StatMod;

public final class MagicShopGenerator {
    public static final String DEFAULT_SHOP_ID = "default";

    private static final DateTimeFormatter BACKUP_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final Path REPORT_PATH = Path.of("statmod", "shop-generation-report.json");

    private MagicShopGenerator() {
    }

    public static GenerationResult regenerate(MinecraftServer server) throws IOException {
        if (server == null) {
            throw new IllegalArgumentException("server must not be null");
        }
        SDMShopServer shopServer = SDMShopServer.Instance();
        Path backup = backupExistingShopData(shopServer.getShopsDir());
        ResourceLocation shopId = SDMShopServer.parseLocation(DEFAULT_SHOP_ID);
        BaseShop shop = shopServer.getShop(shopId)
                .orElseGet(() -> shopServer.createShop(DEFAULT_SHOP_ID));
        shop.clearData();

        MagicShopReport report = new MagicShopReport();
        populateScrollEntries(shop, report);
        populateSaleEntries(shop, report);
        shopServer.saveShopToFile(shop);
        writeReport(report);
        StatMod.LOGGER.info(
                "[Shop] Generated {} spells, {} scroll entries, {} sale entries, {} skipped",
                report.spellCount(), report.scrollEntryCount(), report.saleEntryCount(),
                report.skippedCount());
        return new GenerationResult(report.spellCount(), report.scrollEntryCount(),
                report.saleEntryCount(), report.skippedCount(), backup);
    }

    private static void populateScrollEntries(BaseShop shop, MagicShopReport report) {
        List<SpellSpec> spells = collectSpells(report);
        Map<String, ShopTab> tabs = new LinkedHashMap<>();
        for (SpellSpec spec : spells) {
            ShopTab tab = tabs.computeIfAbsent(spec.schoolId(), id -> {
                ShopTab created = new ShopTab(shop, MagicShopIds.tab(id));
                created.title = spec.spell().getSchoolType().getDisplayName();
                shop.addTab(created);
                return created;
            });
            try {
                addSpellLevels(shop, tab, spec, report);
            } catch (RuntimeException exception) {
                report.recordSkipped(spec.spellId(), conciseReason(exception));
                StatMod.LOGGER.warn("[Shop] Skipping malformed spell {}", spec.spellId(), exception);
            }
        }
    }

    private static List<SpellSpec> collectSpells(MagicShopReport report) {
        List<SpellSpec> spells = new ArrayList<>();
        for (AbstractSpell spell : SpellRegistry.getEnabledSpells()) {
            String fallbackId = spell == null ? "unknown:null" : spell.getClass().getName();
            try {
                if (spell == null || spell == SpellRegistry.none()) {
                    continue;
                }
                ResourceLocation spellId = SpellRegistry.REGISTRY.get().getKey(spell);
                ResourceLocation schoolId = spell.getSchoolType().getId();
                if (spellId == null || schoolId == null) {
                    throw new IllegalStateException("missing registry or school id");
                }
                spells.add(new SpellSpec(spell, spellId.toString(), schoolId.toString()));
            } catch (RuntimeException exception) {
                report.recordSkipped(fallbackId, conciseReason(exception));
            }
        }
        spells.sort(Comparator.comparing(SpellSpec::schoolId)
                .thenComparing(SpellSpec::spellId));
        return spells;
    }

    private static void addSpellLevels(
            BaseShop shop, ShopTab tab, SpellSpec spec, MagicShopReport report) {
        AbstractSpell spell = spec.spell();
        int minimum = spell.getMinLevel();
        int maximum = spell.getMaxLevel();
        if (minimum < 1 || maximum < minimum) {
            throw new IllegalStateException("invalid level range " + minimum + ".." + maximum);
        }
        for (int level = minimum; level <= maximum; level++) {
            String rarity = spell.getRarity(level).name();
            long price = MagicShopPricing.scrollPrice(rarity, level);
            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, level, scroll);

            ShopEntry entry = new ShopEntry(shop,
                    MagicShopIds.scroll(spec.spellId(), level), tab.getId(),
                    new MoneySellerType((double) price));
            entry.setEntryType(new StrictItemEntryType(entry, scroll))
                    .setPrice(price)
                    .setCount(1)
                    .changeType(ShopEntryType.Buy);
            shop.addEntry(tab, entry);
            report.recordSpell(spec.spellId(), spec.schoolId(), rarity, level);
            report.recordScrollEntry();
        }
    }

    private static void populateSaleEntries(BaseShop shop, MagicShopReport report) {
        Map<String, ShopTab> tabs = new LinkedHashMap<>();
        for (MagicShopSaleCatalog.SaleOffer offer : MagicShopSaleCatalog.offers()) {
            ResourceLocation itemId = ResourceLocation.tryParse(offer.itemId());
            Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null || item == Items.AIR) {
                StatMod.LOGGER.warn("[Shop] Skipping missing sale item {}", offer.itemId());
                continue;
            }
            ShopTab tab = tabs.computeIfAbsent(offer.category(), category -> {
                String tabId = "statmod:sales/" + category;
                ShopTab created = new ShopTab(shop, MagicShopIds.tab(tabId));
                created.title = Component.translatable("statmod.shop.tab." + category);
                shop.addTab(created);
                return created;
            });
            ShopEntry entry = new ShopEntry(shop, MagicShopIds.sale(offer.id()), tab.getId(),
                    new MoneySellerType((double) offer.value()));
            entry.setEntryType(new StrictItemEntryType(entry, new ItemStack(item)))
                    .setPrice(offer.value())
                    .setCount(offer.count())
                    .changeType(ShopEntryType.Sell);
            shop.addEntry(tab, entry);
            report.recordSaleEntry();
        }
    }

    private static Path backupExistingShopData(Path shopDirectory) throws IOException {
        if (!Files.isDirectory(shopDirectory)) {
            return null;
        }
        List<Path> files;
        try (var paths = Files.walk(shopDirectory)) {
            files = paths.filter(Files::isRegularFile).sorted().toList();
        }
        if (files.isEmpty()) {
            return null;
        }
        Path backup = FMLPaths.CONFIGDIR.get().resolve("statmod").resolve("shop-backups")
                .resolve(BACKUP_TIME.format(Instant.now()));
        for (Path source : files) {
            Path destination = backup.resolve(shopDirectory.relativize(source));
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
        }
        return backup;
    }

    private static void writeReport(MagicShopReport report) throws IOException {
        Path target = FMLPaths.CONFIGDIR.get().resolve(REPORT_PATH);
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, report.toJson(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String conciseReason(RuntimeException exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record SpellSpec(AbstractSpell spell, String spellId, String schoolId) {
    }

    public record GenerationResult(int spells, int scrollEntries, int saleEntries,
                                   int skippedSpells, Path backupDirectory) {
    }
}
