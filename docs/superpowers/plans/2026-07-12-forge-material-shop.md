# Forge Material Shop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a standalone NPC shop selling Overgeared forge materials (roughs, blueprints, grips, components, tools) in FDP_cfa.

**Architecture:** Villager NPC (WEAPONSMITH) opens a custom Screen via S2C payload; C2S payload handles purchases through `SDMEconomyBridge`. Prices defined in a pure testable class. No new items registered — references existing `Supplier<Item>` fields.

**Tech Stack:** NeoForge 1.21.1, Minecraft Screen, CustomPacketPayload, StreamCodec, SDMEconomyBridge (SDM economy soft dep)

---

### Task 1: ForgeShopPrices (pure + test)

**Files:**
- Create: `src/main/java/tong/statmod/economy/ForgeShopPrices.java`
- Create: `src/test/java/tong/statmod/economy/ForgeShopPricesTest.java`

- [ ] **Step 1: Write the failing test**

```java
package tong.statmod.economy;

import org.junit.jupiter.api.Test;
import tong.statmod.item.ForgingBlueprints;
import tong.statmod.item.ForgingFormComponents;
import tong.statmod.item.ForgingGrips;
import tong.statmod.item.ForgingIntermediateIds;
import tong.statmod.item.ForgingTools;
import java.util.OptionalLong;
import static org.junit.jupiter.api.Assertions.*;

class ForgeShopPricesTest {
    @Test
    void allRoughsHavePrice() {
        for (String id : ForgingIntermediateIds.allIds()) {
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
            assertTrue(price.getAsLong() > 0, "Zero/negative price for " + id);
        }
    }

    @Test
    void allBlueprintsHavePrice() {
        for (var sup : ForgingBlueprints.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
            assertTrue(price.getAsLong() >= 500, "Blueprint " + id + " must be expensive");
        }
    }

    @Test
    void allGripsHavePrice() {
        for (var sup : ForgingGrips.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
        }
    }

    @Test
    void allComponentsHavePrice() {
        for (var sup : ForgingFormComponents.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
        }
    }

    @Test
    void allToolsHavePrice() {
        for (var sup : ForgingTools.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
        }
    }

    @Test
    void unknownItemReturnsEmpty() {
        assertEquals(OptionalLong.empty(), ForgeShopPrices.priceOf("heated_gold_ingot"));
        assertEquals(OptionalLong.empty(), ForgeShopPrices.priceOf("nonexistent_item"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew test --tests "*ForgeShopPrices*"`
Expected: FAIL — `ForgeShopPrices` class not found

- [ ] **Step 3: Write minimal implementation**

```java
package tong.statmod.economy;

import java.util.Map;
import java.util.OptionalLong;

public final class ForgeShopPrices {

    private static final Map<String, Long> MATERIAL_PRICES = Map.ofEntries(
            Map.entry("tin", 50L), Map.entry("bronze", 80L), Map.entry("gold", 100L), Map.entry("iron", 150L),
            Map.entry("silver", 200L), Map.entry("steel", 250L), Map.entry("diamond", 300L), Map.entry("pyrium", 400L),
            Map.entry("arcane", 500L), Map.entry("mithril", 600L), Map.entry("orichalcum", 700L), Map.entry("low_magisteel", 900L),
            Map.entry("magisteel", 1000L), Map.entry("netherite", 1200L), Map.entry("pure_magisteel", 1500L),
            Map.entry("high_magisteel", 2000L), Map.entry("adamantite", 2500L), Map.entry("hihiirokane", 3000L)
    );

    private static final Map<String, Long> BLUEPRINT_PRICES = Map.of(
            "blueprint_universal_blade", 500L,
            "blueprint_universal_pole", 1_500L,
            "blueprint_runic_blade", 5_000L,
            "blueprint_legendary", 10_000L
    );

    private static final Map<String, Long> GRIP_PRICES = Map.of(
            "wooden_grip", 50L, "leather_wrap", 200L, "wire_wrap", 500L, "runic_grip", 1_000L
    );

    private static final long COMPONENT_PRICE = 200L;
    private static final long TOOL_PRICE = 100L;

    public static OptionalLong priceOf(String itemId) {
        // Check blueprints
        Long bp = BLUEPRINT_PRICES.get(itemId);
        if (bp != null) return OptionalLong.of(bp);
        // Check grips
        Long grip = GRIP_PRICES.get(itemId);
        if (grip != null) return OptionalLong.of(grip);
        // Check components
        if (itemId.equals("katana_tsuba") || itemId.equals("rapier_guard") || itemId.equals("claymore_pommel")
                || itemId.equals("halberd_socket") || itemId.equals("warhammer_core") || itemId.equals("staff_focus")) {
            return OptionalLong.of(COMPONENT_PRICE);
        }
        // Check tools
        if (itemId.equals("basic_forge_tongs") || itemId.equals("basic_smithing_hammer")) {
            return OptionalLong.of(TOOL_PRICE);
        }
        // Check roughs: rough_<class>_<material>
        if (itemId.startsWith("rough_")) {
            String material = extractMaterialFromRough(itemId);
            if (material != null) {
                Long price = MATERIAL_PRICES.get(material);
                if (price != null) return OptionalLong.of(price);
            }
        }
        return OptionalLong.empty();
    }

    private static String extractMaterialFromRough(String id) {
        // rough_blade_gold → gold, rough_spear_tip_high_magisteel → high_magisteel
        String[] parts = id.split("_");
        if (parts.length < 3) return null;
        // parts[0] = "rough", parts[1] = class, parts[2..] = material (may be multi-word)
        StringBuilder mat = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (mat.length() > 0) mat.append("_");
            mat.append(parts[i]);
        }
        String material = mat.toString();
        return MATERIAL_PRICES.containsKey(material) ? material : null;
    }

    private ForgeShopPrices() {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew test --tests "*ForgeShopPrices*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/economy/ForgeShopPrices.java src/test/java/tong/statmod/economy/ForgeShopPricesTest.java
git commit -m "feat: add ForgeShopPrices with material-tier pricing"
```

---

### Task 2: Network Payloads (S2C + C2S)

**Files:**
- Create: `src/main/java/tong/statmod/network/OpenForgeShopPayload.java`
- Create: `src/main/java/tong/statmod/network/BuyForgeItemPayload.java`

- [ ] **Step 1: Write OpenForgeShopPayload.java**

```java
package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record OpenForgeShopPayload(long balance) implements CustomPacketPayload {
    public static final Type<OpenForgeShopPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_forge_shop"));
    public static final StreamCodec<ByteBuf, OpenForgeShopPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, OpenForgeShopPayload::balance,
            OpenForgeShopPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 2: Write BuyForgeItemPayload.java**

```java
package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record BuyForgeItemPayload(String itemId) implements CustomPacketPayload {
    public static final Type<BuyForgeItemPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "buy_forge_item"));
    public static final StreamCodec<ByteBuf, BuyForgeItemPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BuyForgeItemPayload::itemId,
            BuyForgeItemPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/network/OpenForgeShopPayload.java src/main/java/tong/statmod/network/BuyForgeItemPayload.java
git commit -m "feat: add forge shop network payloads"
```

---

### Task 3: Server Payload Handler + Client Actions

**Files:**
- Modify: `src/main/java/tong/statmod/network/ServerPayloadHandler.java`
- Modify: `src/main/java/tong/statmod/network/ClientPayloadActions.java`

- [ ] **Step 1: Add import and handler method to ServerPayloadHandler**

Add these imports at the top of `ServerPayloadHandler.java`:
```java
import tong.statmod.economy.ForgeMaterialShop;
import tong.statmod.economy.ForgeShopPrices;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
```

Add this method after existing handlers:
```java
public static void handleBuyForgeItem(BuyForgeItemPayload payload, IPayloadContext context) {
    context.enqueueWork(() -> {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!ForgeMaterialShop.canUse(player)) {
            player.displayClientMessage(Component.translatable("forge_shop.invalid_session"), false);
            return;
        }
        var optPrice = ForgeShopPrices.priceOf(payload.itemId());
        if (optPrice.isEmpty()) {
            player.displayClientMessage(Component.translatable("forge_shop.invalid_item"), false);
            return;
        }
        long price = optPrice.getAsLong();
        long balance = SDMEconomyBridge.getCoins(player);
        if (balance < price) {
            player.displayClientMessage(Component.translatable("forge_shop.insufficient_funds", balance, price), false);
            PacketDistributor.sendToPlayer(player, new OpenForgeShopPayload(balance));
            return;
        }
        // Resolve item supplier
        var sup = resolveItem(payload.itemId());
        if (sup == null) {
            player.displayClientMessage(Component.translatable("forge_shop.invalid_item"), false);
            return;
        }
        boolean debited = SDMEconomyBridge.removeCoins(player, price);
        if (!debited) return;
        ItemStack stack = new ItemStack(sup.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.displayClientMessage(Component.translatable("forge_shop.purchased",
                stack.getHoverName(), price), false);
        long newBalance = SDMEconomyBridge.getCoins(player);
        PacketDistributor.sendToPlayer(player, new OpenForgeShopPayload(newBalance));
    });
}

private static Supplier<Item> resolveItem(String itemId) {
    // Roughs
    if (itemId.startsWith("rough_")) {
        Supplier<Item> sup = tong.statmod.item.ForgingIntermediates.byId(itemId);
        if (sup != null) return sup;
    }
    // Blueprints
    if (itemId.equals("blueprint_universal_blade")) return tong.statmod.item.ForgingBlueprints.BLUEPRINT_UNIVERSAL_BLADE;
    if (itemId.equals("blueprint_universal_pole")) return tong.statmod.item.ForgingBlueprints.BLUEPRINT_UNIVERSAL_POLE;
    if (itemId.equals("blueprint_runic_blade")) return tong.statmod.item.ForgingBlueprints.BLUEPRINT_RUNIC_BLADE;
    if (itemId.equals("blueprint_legendary")) return tong.statmod.item.ForgingBlueprints.BLUEPRINT_LEGENDARY;
    // Grips
    if (itemId.equals("wooden_grip")) return tong.statmod.item.ForgingGrips.WOODEN_GRIP;
    if (itemId.equals("leather_wrap")) return tong.statmod.item.ForgingGrips.LEATHER_WRAP;
    if (itemId.equals("wire_wrap")) return tong.statmod.item.ForgingGrips.WIRE_WRAP;
    if (itemId.equals("runic_grip")) return tong.statmod.item.ForgingGrips.RUNIC_GRIP;
    // Form components
    if (itemId.equals("katana_tsuba")) return tong.statmod.item.ForgingFormComponents.KATANA_TSUBA;
    if (itemId.equals("rapier_guard")) return tong.statmod.item.ForgingFormComponents.RAPIER_GUARD;
    if (itemId.equals("claymore_pommel")) return tong.statmod.item.ForgingFormComponents.CLAYMORE_POMMEL;
    if (itemId.equals("halberd_socket")) return tong.statmod.item.ForgingFormComponents.HALBERD_SOCKET;
    if (itemId.equals("warhammer_core")) return tong.statmod.item.ForgingFormComponents.WARHAMMER_CORE;
    if (itemId.equals("staff_focus")) return tong.statmod.item.ForgingFormComponents.STAFF_FOCUS;
    // Tools
    if (itemId.equals("basic_forge_tongs")) return tong.statmod.item.ForgingTools.BASIC_FORGE_TONGS;
    if (itemId.equals("basic_smithing_hammer")) return tong.statmod.item.ForgingTools.BASIC_SMITHING_HAMMER;
    return null;
}
```

Also add:
```java
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
```

- [ ] **Step 2: Add client action handler**

In `ClientPayloadActions.java`, add:
```java
import tong.statmod.client.ForgeMaterialScreen;

static void handleOpenForgeShop(OpenForgeShopPayload payload, IPayloadContext context) {
    context.enqueueWork(() -> ForgeMaterialScreen.openOrRefresh(payload.balance()));
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/network/ServerPayloadHandler.java src/main/java/tong/statmod/network/ClientPayloadActions.java
git commit -m "feat: add forge shop server handler and client actions"
```

---

### Task 4: Register Payloads in NetworkHandler

**Files:**
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`

- [ ] **Step 1: Add imports and register in onRegisterPayloads**

Add imports:
```java
import tong.statmod.network.OpenForgeShopPayload;
import tong.statmod.network.BuyForgeItemPayload;
import tong.statmod.network.ClientPayloadActions;
```

Add in `onRegisterPayloads`:
```java
registrar.playToClient(OpenForgeShopPayload.TYPE, OpenForgeShopPayload.CODEC,
        (payload, context) -> ClientPayloadActions.handleOpenForgeShop(payload, context));
registrar.playToServer(BuyForgeItemPayload.TYPE, BuyForgeItemPayload.CODEC,
        ServerPayloadHandler::handleBuyForgeItem);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tong/statmod/network/NetworkHandler.java
git commit -m "feat: register forge shop network payloads"
```

---

### Task 5: ForgeMaterialShop NPC

**Files:**
- Create: `src/main/java/tong/statmod/economy/ForgeMaterialShop.java`

- [ ] **Step 1: Create the class**

Pattern follows `MagicBanker.java` exactly:
```java
package tong.statmod.economy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.dungeon.DungeonSpawnGuard;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.network.OpenForgeShopPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ForgeMaterialShop {

    public static final String TAG = "statmod_forge_merchant";

    private static final long SESSION_TICKS = 20L * 30L;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private record Session(String dimension, UUID merchant, long expiresAt) {}

    public static Villager spawn(ServerLevel level, BlockPos pos) {
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) return null;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180f, 0f);
        villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.WEAPONSMITH, 5));
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        villager.setPersistenceRequired();
        villager.setSilent(true);
        villager.setCustomName(Component.translatable("forge_shop.name"));
        villager.setCustomNameVisible(true);
        villager.getPersistentData().putBoolean(TAG, true);
        DungeonSpawnGuard.spawnAuthorized(() -> { level.addFreshEntity(villager); return villager; });
        return villager;
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!event.getTarget().getPersistentData().getBoolean(TAG)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Level level = event.getLevel();
        SESSIONS.put(player.getUUID(), new Session(
                level.dimension().location().toString(),
                event.getTarget().getUUID(),
                level.getGameTime() + SESSION_TICKS));
        sendSnapshot(player);
    }

    public static boolean canUse(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return false;
        Level level = player.level();
        if (!level.dimension().location().toString().equals(session.dimension)) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        if (level.getGameTime() >= session.expiresAt) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        var entity = level.getEntity(session.merchant);
        if (entity == null || !entity.getPersistentData().getBoolean(TAG)
                || player.distanceToSqr(entity) > 64.0) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void sendSnapshot(ServerPlayer player) {
        long balance = SDMEconomyBridge.getCoins(player);
        PacketDistributor.sendToPlayer(player, new OpenForgeShopPayload(balance));
    }

    private ForgeMaterialShop() {}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tong/statmod/economy/ForgeMaterialShop.java
git commit -m "feat: add ForgeMaterialShop NPC with session handling"
```

---

### Task 6: ForgeMaterialScreen GUI

**Files:**
- Create: `src/main/java/tong/statmod/client/ForgeMaterialScreen.java`

- [ ] **Step 1: Write the screen class**

```java
package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.economy.ForgeShopPrices;
import tong.statmod.item.ForgingBlueprints;
import tong.statmod.item.ForgingFormComponents;
import tong.statmod.item.ForgingGrips;
import tong.statmod.item.ForgingIntermediateIds;
import tong.statmod.item.ForgingIntermediates;
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
    private final List<ShopEntry>[] tabEntries = new List[TAB_COUNT];
    private final List<Button> itemButtons = new ArrayList<>();

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
            if (scrollOffset + 18 < tabEntries[selectedTab].size()) { scrollOffset++; rebuildItemButtons(); }
        }).bounds(cx + 135, cy + 60, 20, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 40, cy + 90, 80, 20).build());

        rebuildItemButtons();
    }

    private void buildTabEntries() {
        for (int t = 0; t < TAB_COUNT; t++) tabEntries[t] = new ArrayList<>();

        for (String id : ForgingIntermediateIds.allIds()) {
            Supplier<Item> sup = ForgingIntermediates.byId(id);
            if (sup != null) tabEntries[0].add(new ShopEntry(id, new ItemStack(sup.get())));
        }
        for (Supplier<Item> sup : ForgingBlueprints.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            tabEntries[1].add(new ShopEntry(id, new ItemStack(sup.get())));
        }
        for (Supplier<Item> sup : ForgingGrips.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            tabEntries[2].add(new ShopEntry(id, new ItemStack(sup.get())));
        }
        for (Supplier<Item> sup : ForgingFormComponents.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            tabEntries[3].add(new ShopEntry(id, new ItemStack(sup.get())));
        }
        for (Supplier<Item> sup : ForgingTools.all()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sup.get()).getPath();
            tabEntries[4].add(new ShopEntry(id, new ItemStack(sup.get())));
        }
    }

    private void rebuildItemButtons() {
        itemButtons.forEach(this::removeWidget);
        itemButtons.clear();
        int cx = width / 2, left = cx - 160, right = cx + 160;
        int y = height / 2 - 60;
        List<ShopEntry> list = tabEntries[selectedTab];
        int start = Math.min(scrollOffset, Math.max(0, list.size() - 18));
        int end = Math.min(start + 18, list.size());
        for (int i = start; i < end; i++) {
            ShopEntry entry = list.get(i);
            String itemId = entry.itemId;
            Button btn = Button.builder(Component.translatable("forge_shop.buy"),
                    b -> PacketDistributor.sendToServer(new BuyForgeItemPayload(itemId)))
                    .bounds(right - 50, y + 1, 45, 14)
                    .build();
            btn.active = canAfford(itemId);
            Button wBtn = addRenderableWidget(btn);
            itemButtons.add(wBtn);
            y += 18;
        }
    }

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
        List<ShopEntry> list = tabEntries[selectedTab];
        int start = Math.min(scrollOffset, Math.max(0, list.size() - 18));
        int end = Math.min(start + 18, list.size());
        for (int i = start; i < end; i++) {
            ShopEntry entry = list.get(i);
            String priceStr = formatPrice(entry.itemId);
            g.renderItem(entry.stack, left + 8, y);
            g.drawString(font, entry.stack.getHoverName(), left + 30, y + 4, 0xFFFFFF);
            g.drawString(font, Component.literal(priceStr), right - 90, y + 4,
                    canAfford(entry.itemId) ? 0x55FF55 : 0xFF5555);
            y += 18;
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tong/statmod/client/ForgeMaterialScreen.java
git commit -m "feat: add ForgeMaterialScreen GUI with tabs and purchase buttons"
```

---

### Task 7: VillageForgeMerchantSpawner

**Files:**
- Create: `src/main/java/tong/statmod/economy/VillageForgeMerchantSpawner.java`

- [ ] **Step 1: Write the spawner**

```java
package tong.statmod.economy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerTickEvent;
import tong.statmod.dungeon.DungeonDimensions;

import java.util.List;

public final class VillageForgeMerchantSpawner {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 200 != 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (level.dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;
        if (!level.isVillage(player.blockPosition())) return;

        var area = player.getBoundingBox().inflate(64.0);
        List<Villager> existing = level.getEntitiesOfClass(Villager.class, area,
                v -> v.getPersistentData().getBoolean(ForgeMaterialShop.TAG));
        if (!existing.isEmpty()) return;

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area,
                v -> !v.getPersistentData().getBoolean(ForgeMaterialShop.TAG));
        if (villagers.isEmpty()) return;

        ForgeMaterialShop.spawn(level, villagers.getFirst().blockPosition().offset(2, 0, 0));
    }

    private VillageForgeMerchantSpawner() {}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tong/statmod/economy/VillageForgeMerchantSpawner.java
git commit -m "feat: add VillageForgeMerchantSpawner auto-spawn in villages"
```

---

### Task 8: STATMod Registration

**Files:**
- Modify: `src/main/java/tong/statmod/STATMod.java`

- [ ] **Step 1: Add event bus registrations**

In STATMod.java, add after the existing `NeoForge.EVENT_BUS.register(...)` lines:
```java
NeoForge.EVENT_BUS.register(tong.statmod.economy.ForgeMaterialShop.class);
NeoForge.EVENT_BUS.register(tong.statmod.economy.VillageForgeMerchantSpawner.class);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/tong/statmod/STATMod.java
git commit -m "feat: register forge shop event handlers"
```

---

### Task 9: Lang Entries

**Files:**
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`

- [ ] **Step 1: Add English lang entries**

Append to `en_us.json`:
```json
  "forge_shop.name": "Forge Merchant",
  "forge_shop.title": "Forge Material Shop",
  "forge_shop.balance": "Balance: %s FDP",
  "forge_shop.buy": "Buy",
  "forge_shop.tab.roughs": "Roughs",
  "forge_shop.tab.blueprints": "Blueprints",
  "forge_shop.tab.grips": "Grips",
  "forge_shop.tab.components": "Components",
  "forge_shop.tab.tools": "Tools",
  "forge_shop.purchased": "§aPurchased %s for %s FDP",
  "forge_shop.insufficient_funds": "§cInsufficient funds: %s / %s FDP",
  "forge_shop.invalid_session": "§cToo far from the merchant",
  "forge_shop.invalid_item": "§cThis item cannot be purchased here"
```

- [ ] **Step 2: Add French lang entries**

Append to `fr_fr.json`:
```json
  "forge_shop.name": "Marchand de Forge",
  "forge_shop.title": "Matériaux de Forge",
  "forge_shop.balance": "Solde : %s FDP",
  "forge_shop.buy": "Acheter",
  "forge_shop.tab.roughs": "Ébauches",
  "forge_shop.tab.blueprints": "Plans",
  "forge_shop.tab.grips": "Prises",
  "forge_shop.tab.components": "Composants",
  "forge_shop.tab.tools": "Outils",
  "forge_shop.purchased": "§aAcheté %s pour %s FDP",
  "forge_shop.insufficient_funds": "§cFonds insuffisants : %s / %s FDP",
  "forge_shop.invalid_session": "§cTrop loin du marchand",
  "forge_shop.invalid_item": "§cCet article n'est pas disponible ici"
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json
git commit -m "feat: add forge shop lang entries"
```

---

### Task 10: Build + Verify

- [ ] **Step 1: Full build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Verify all tests**

Run: `.\gradlew test`
Expected: All 900+ tests pass (including new ForgeShopPricesTest)

- [ ] **Step 3: (If any failures) fix and re-build**

- [ ] **Step 4: Final commit if needed**

```bash
git add -A
git commit -m "fix: address build issues"
```
