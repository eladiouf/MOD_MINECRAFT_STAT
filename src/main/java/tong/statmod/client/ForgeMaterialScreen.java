package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.economy.ForgeShopPrices;
import tong.statmod.item.ForgingBlueprints;
import tong.statmod.item.ForgingFormComponents;
import tong.statmod.item.ForgingGrips;
import tong.statmod.item.ForgingIntermediateIds;
import tong.statmod.item.ForgingTools;
import tong.statmod.network.BuyForgeItemPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ForgeMaterialScreen extends Screen {

    private static final int TAB_COUNT = 5;
    private long balance;
    private int selectedTab = 0;
    private int scrollOffset = 0;
    private final List<List<ShopEntry>> tabEntries = new ArrayList<>();

    public ForgeMaterialScreen(long balance) {
        super(Component.translatable("forge_shop.title"));
        this.balance = balance;
    }

    public static void openOrRefresh(long balance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ForgeMaterialScreen screen) {
            screen.balance = balance;
        } else {
            mc.setScreen(new ForgeMaterialScreen(balance));
        }
    }

    @Override
    protected void init() {
        tabEntries.clear();
        for (int t = 0; t < TAB_COUNT; t++) tabEntries.add(new ArrayList<>());

        buildTabEntries();

        int cx = width / 2;
        int cy = height / 2;

        String[] tabKeys = {"forge_shop.tab.roughs", "forge_shop.tab.blueprints",
                "forge_shop.tab.grips", "forge_shop.tab.components", "forge_shop.tab.tools"};
        for (int i = 0; i < TAB_COUNT; i++) {
            int tabIdx = i;
            addRenderableWidget(Button.builder(Component.translatable(tabKeys[i]), b -> {
                selectedTab = tabIdx;
                scrollOffset = 0;
                rebuildItemButtons();
            }).bounds(cx - 160 + i * 64, cy - 90, 62, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            if (scrollOffset > 0) { scrollOffset--; rebuildItemButtons(); }
        }).bounds(cx + 135, cy - 60, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            if (scrollOffset + maxVisible() < tabEntries.get(selectedTab).size()) { scrollOffset++; rebuildItemButtons(); }
        }).bounds(cx + 135, cy + 60, 20, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 40, cy + 90, 80, 20).build());
    }

    private void buildTabEntries() {
        for (String id : ForgingIntermediateIds.allIds()) {
            ItemStack stack = resolveItem(id);
            if (!stack.isEmpty()) tabEntries.get(0).add(new ShopEntry(id, stack));
        }

        for (Supplier<net.minecraft.world.item.Item> sup : ForgingBlueprints.all()) {
            String id = builtInId(sup);
            tabEntries.get(1).add(new ShopEntry(id, new ItemStack(sup.get())));
        }

        for (Supplier<net.minecraft.world.item.Item> sup : ForgingGrips.all()) {
            String id = builtInId(sup);
            tabEntries.get(2).add(new ShopEntry(id, new ItemStack(sup.get())));
        }

        for (Supplier<net.minecraft.world.item.Item> sup : ForgingFormComponents.all()) {
            String id = builtInId(sup);
            tabEntries.get(3).add(new ShopEntry(id, new ItemStack(sup.get())));
        }

        for (Supplier<net.minecraft.world.item.Item> sup : ForgingTools.all()) {
            String id = builtInId(sup);
            tabEntries.get(4).add(new ShopEntry(id, new ItemStack(sup.get())));
        }
    }

    private static String builtInId(Supplier<net.minecraft.world.item.Item> sup) {
        return BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
    }

    private static ItemStack resolveItem(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, id));
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void rebuildItemButtons() {
        clearItemButtons();
        int cx = width / 2;
        int left = cx - 160;
        int right = cx + 160;
        int y = height / 2 - 60;
        List<ShopEntry> list = tabEntries.get(selectedTab);
        int start = Math.min(scrollOffset, Math.max(0, list.size() - maxVisible()));
        int end = Math.min(start + maxVisible(), list.size());
        for (int i = start; i < end; i++) {
            ShopEntry entry = list.get(i);
            Button btn = Button.builder(Component.translatable("forge_shop.buy"),
                    b -> PacketDistributor.sendToServer(new BuyForgeItemPayload(entry.itemId)))
                    .bounds(right - 50, y + 1, 45, 14).build();
            btn.active = canAfford(entry.itemId);
            itemButtons.add(addRenderableWidget(btn));
            y += 18;
        }
    }

    private void clearItemButtons() {
        itemButtons.forEach(this::removeWidget);
        itemButtons.clear();
    }

    private int maxVisible() {
        return 18;
    }

    private final List<Button> itemButtons = new ArrayList<>();

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int cx = width / 2, cy = height / 2;
        int left = cx - 160, right = cx + 160, top = cy - 100, bottom = cy + 115;

        g.fillGradient(left, top, right, bottom, 0xE0101010, 0xF0050505);
        int borderColor = 0xFFFFD700;
        g.fill(left - 1, top - 1, right + 1, top, borderColor);
        g.fill(left - 1, bottom, right + 1, bottom + 1, borderColor);
        g.fill(left - 1, top, left, bottom, borderColor);
        g.fill(right, top, right + 1, bottom, borderColor);

        g.drawCenteredString(font, title, cx, top + 6, 0xFFFFD700);
        g.drawCenteredString(font, Component.translatable("forge_shop.balance", balance), cx, top + 22, 0xFF55FF55);

        int y = top + 40;
        List<ShopEntry> list = tabEntries.get(selectedTab);
        int start = Math.min(scrollOffset, Math.max(0, list.size() - maxVisible()));
        int end = Math.min(start + maxVisible(), list.size());
        for (int i = start; i < end; i++) {
            ShopEntry entry = list.get(i);
            String priceStr = formatPrice(entry.itemId);
            g.renderItem(entry.stack, left + 8, y);
            g.drawString(font, entry.stack.getHoverName(), left + 30, y + 4, 0xFFFFFF);
            int priceColor = canAfford(entry.itemId) ? 0x55FF55 : 0xFF5555;
            g.drawString(font, Component.literal(priceStr), right - 90, y + 4, priceColor);
            y += 18;
        }

        List<ShopEntry> currentList = tabEntries.get(selectedTab);
        if (!currentList.isEmpty()) {
            int total = currentList.size();
            int showing = Math.min(end - start, maxVisible());
            g.drawCenteredString(font,
                    Component.literal((start + 1) + "-" + (end) + "/" + total), cx, bottom - 14, 0x888888);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private boolean canAfford(String itemId) {
        var opt = ForgeShopPrices.priceOf(itemId);
        return opt.isPresent() && balance >= opt.getAsLong();
    }

    private String formatPrice(String itemId) {
        var opt = ForgeShopPrices.priceOf(itemId);
        return opt.isPresent() ? opt.getAsLong() + " FDP" : "---";
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record ShopEntry(String itemId, ItemStack stack) {}
}
