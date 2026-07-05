# Dungeon Shop (SDM) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permettre au joueur de convertir ses points de donjon en une monnaie SDM (« Dungeon Coins ») via un villageois changeur et un écran GUI, avec éjection vers l'overworld si les points tombent à 0.

**Architecture:** Monnaies séparées (points = score interne ; coins = solde SDM). Un villageois changeur dans les étages trésor ouvre un écran ; l'écran envoie un packet C2S de conversion ; le serveur débite les points, crédite les coins via un bridge SDM par réflexion (mod optionnel), et éjecte le joueur si les points atteignent 0.

**Tech Stack:** NeoForge 1.21.1, CustomPacketPayload/StreamCodec, ModConfigSpec, réflexion (ModList guardée), JUnit 5.

Spec : `docs/superpowers/specs/2026-07-05-dungeon-shop-sdm-design.md`

---

## File Structure

- Create `src/main/java/tong/statmod/dungeon/PointExchange.java` — calcul pur (clamp + coins).
- Create `src/main/java/tong/statmod/dungeon/DungeonPointsEjection.java` — règle 0 point → overworld.
- Create `src/main/java/tong/statmod/integration/sdm/SDMEconomyBridge.java` — bridge réflexion optionnel.
- Create `src/main/java/tong/statmod/network/OpenExchangePayload.java` — S2C (ouvre l'écran).
- Create `src/main/java/tong/statmod/network/ConvertPointsPayload.java` — C2S (convertit).
- Create `src/main/java/tong/statmod/client/PointExchangeScreen.java` — écran GUI.
- Create `src/main/java/tong/statmod/dungeon/DungeonExchanger.java` — spawn villageois + interact handler.
- Modify `src/main/java/tong/statmod/config/Config.java` — 2 valeurs + getters.
- Modify `src/main/java/tong/statmod/network/NetworkHandler.java` — enregistre les 2 payloads.
- Modify `src/main/java/tong/statmod/network/ServerPayloadHandler.java` — handler C2S convert.
- Modify `src/main/java/tong/statmod/network/ClientPayloadHandler.java` — handler S2C open.
- Modify `src/main/java/tong/statmod/dungeon/DungeonPoints.java` — éjection après pénalité de mort.
- Modify `src/main/java/tong/statmod/dungeon/DungeonRoomChain.java` — pose le changeur dans la salle trésor.
- Modify `src/main/resources/assets/statmod/lang/en_us.json` + `fr_fr.json` — clés de texte.
- Test `src/test/java/tong/statmod/dungeon/PointExchangeTest.java`.
- Test `src/test/java/tong/statmod/dungeon/DungeonPointsEjectionTest.java`.

---

### Task 1: Config — devise & taux

**Files:**
- Modify: `src/main/java/tong/statmod/config/Config.java`

- [ ] **Step 1: Déclarer les 2 champs** (près des autres `DUNGEON_*`, après la ligne `DUNGEON_HOSTILITY_CAP;`)

```java
    public static final ModConfigSpec.ConfigValue<String> SHOP_CURRENCY_NAME;
    public static final ModConfigSpec.DoubleValue POINT_TO_COIN_RATE;
```

- [ ] **Step 2: Les définir dans le bloc `trial_dungeon`** (juste avant `BUILDER.pop();` à la fin de `push("trial_dungeon")`)

```java
        SHOP_CURRENCY_NAME = BUILDER
                .comment("Nom de la monnaie SDM créditée à l'échange de points (doit correspondre à la devise du shop SDM).")
                .define("shopCurrencyName", "dungeon_coins");
        POINT_TO_COIN_RATE = BUILDER
                .comment("Taux de conversion : 1 point échangé = ce nombre de coins.")
                .defineInRange("pointToCoinRate", 1.0, 0.0, 100.0);
```

- [ ] **Step 3: Ajouter les getters** (avec les autres getters `getDungeon*`)

```java
    public static String getShopCurrencyName() { return SHOP_CURRENCY_NAME.get(); }
    public static double getPointToCoinRate() { return POINT_TO_COIN_RATE.get(); }
```

- [ ] **Step 4: Compiler**

Run: `./gradlew compileJava --console=plain -q`
Expected: pas d'erreur.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/config/Config.java
git commit -m "feat(shop): config shopCurrencyName + pointToCoinRate"
```

---

### Task 2: PointExchange — calcul pur (TDD)

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/PointExchange.java`
- Test: `src/test/java/tong/statmod/dungeon/PointExchangeTest.java`

- [ ] **Step 1: Écrire le test qui échoue**

```java
package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PointExchangeTest {

    @Test
    void convertsClampedAmountAndFloorsCoins() {
        // Demande 50 sur 320 points, taux 1.0 → 50 convertis, 50 coins, 270 restants.
        PointExchange.Result r = PointExchange.compute(50, 320, 1.0);
        assertEquals(50, r.converted());
        assertEquals(50, r.coins());
        assertEquals(270, r.remainingPoints());
    }

    @Test
    void clampsToAvailablePoints() {
        // Demande 999 sur 100 → convertit 100, il reste 0.
        PointExchange.Result r = PointExchange.compute(999, 100, 1.0);
        assertEquals(100, r.converted());
        assertEquals(0, r.remainingPoints());
    }

    @Test
    void rejectsNegativeRequest() {
        PointExchange.Result r = PointExchange.compute(-5, 100, 1.0);
        assertEquals(0, r.converted());
        assertEquals(100, r.remainingPoints());
    }

    @Test
    void floorsCoinsWithFractionalRate() {
        // 7 points à 0.5 → floor(3.5) = 3 coins.
        PointExchange.Result r = PointExchange.compute(7, 100, 0.5);
        assertEquals(7, r.converted());
        assertEquals(3, r.coins());
    }
}
```

- [ ] **Step 2: Lancer le test → échec**

Run: `./gradlew test --tests "tong.statmod.dungeon.PointExchangeTest" --console=plain`
Expected: FAIL (PointExchange n'existe pas).

- [ ] **Step 3: Écrire l'implémentation**

```java
package tong.statmod.dungeon;

/**
 * Mission M6 — Calcul pur de la conversion points → coins du shop (2026-07-05).
 * Sans dépendance monde → testable.
 */
public final class PointExchange {

    /** Résultat d'une conversion : points convertis, coins gagnés, points restants. */
    public record Result(int converted, long coins, int remainingPoints) {}

    private PointExchange() {}

    /**
     * Convertit {@code requested} points parmi {@code points} disponibles au taux {@code rate}.
     * Montant borné à [0, points] ; coins = floor(converted * rate).
     */
    public static Result compute(int requested, int points, double rate) {
        int converted = Math.max(0, Math.min(requested, points));
        long coins = (long) Math.floor(converted * rate);
        return new Result(converted, coins, points - converted);
    }
}
```

- [ ] **Step 4: Lancer le test → succès**

Run: `./gradlew test --tests "tong.statmod.dungeon.PointExchangeTest" --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/PointExchange.java src/test/java/tong/statmod/dungeon/PointExchangeTest.java
git commit -m "feat(shop): PointExchange (calcul pur clamp + coins)"
```

---

### Task 3: DungeonPointsEjection — règle 0 point (TDD sur le prédicat)

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonPointsEjection.java`
- Test: `src/test/java/tong/statmod/dungeon/DungeonPointsEjectionTest.java`

- [ ] **Step 1: Écrire le test qui échoue**

```java
package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DungeonPointsEjectionTest {

    @Test
    void ejectsWhenInDungeonAndZeroPoints() {
        assertTrue(DungeonPointsEjection.shouldEject(true, 0));
    }

    @Test
    void doesNotEjectWithPositivePoints() {
        assertFalse(DungeonPointsEjection.shouldEject(true, 5));
    }

    @Test
    void doesNotEjectOutsideDungeon() {
        assertFalse(DungeonPointsEjection.shouldEject(false, 0));
    }
}
```

- [ ] **Step 2: Lancer le test → échec**

Run: `./gradlew test --tests "tong.statmod.dungeon.DungeonPointsEjectionTest" --console=plain`
Expected: FAIL (classe absente).

- [ ] **Step 3: Écrire l'implémentation**

```java
package tong.statmod.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.storage.ModAttachments;

/**
 * Mission M6 — Règle « 0 point → éjection overworld » (2026-07-05).
 *
 * <p>Dès que les points de donjon d'un joueur tombent à 0 alors qu'il est dans le donjon (mort ou
 * conversion au changeur), il est renvoyé à l'overworld. Les coins du shop, eux, restent à l'abri.
 */
public final class DungeonPointsEjection {

    private DungeonPointsEjection() {}

    /** Prédicat pur : faut-il éjecter ? Vrai seulement si dans le donjon ET points == 0. */
    public static boolean shouldEject(boolean inDungeon, int points) {
        return inDungeon && points == 0;
    }

    /** Applique la règle pour {@code player} : si éligible, message + retour overworld. */
    public static void enforce(ServerPlayer player) {
        boolean inDungeon = player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON);
        int points = player.getData(ModAttachments.STATS).getDungeonPoints();
        if (!shouldEject(inDungeon, points)) return;
        player.displayClientMessage(Component.translatable("dungeon.ejected.no_points"), false);
        DungeonTeleportHandler.returnToOverworld(player);
    }
}
```

- [ ] **Step 4: Lancer le test → succès**

Run: `./gradlew test --tests "tong.statmod.dungeon.DungeonPointsEjectionTest" --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonPointsEjection.java src/test/java/tong/statmod/dungeon/DungeonPointsEjectionTest.java
git commit -m "feat(shop): règle éjection à 0 point (prédicat testé + action)"
```

---

### Task 4: SDMEconomyBridge — bridge réflexion optionnel

**Files:**
- Create: `src/main/java/tong/statmod/integration/sdm/SDMEconomyBridge.java`

Pas de test unitaire (réflexion / mod runtime) — vérifié au run. Suit la convention des bridges existants (`ModList.isLoaded` + réflexion).

- [ ] **Step 1: Écrire le bridge**

```java
package tong.statmod.integration.sdm;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.STATMod;
import tong.statmod.config.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Mission M6 — Bridge optionnel vers SDM Economy (2026-07-05).
 *
 * <p>Crédite/lit la monnaie « coins » du shop SDM par réflexion (mod optionnel). Si SDM est absent
 * ou l'API a changé, tout devient un no-op sûr — le donjon reste jouable. Convention identique aux
 * bridges L2/Waystones.
 */
public final class SDMEconomyBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);
    private static Boolean available;   // null = non résolu
    private static Object serverData;   // CurrencyPlayerData$Server (SERVER)
    private static Method addCurrencyValue; // (Player, String, double) -> ErrorCodes
    private static Method getBalance;       // (Player, String) -> ErrorCodeStruct<Double>

    private SDMEconomyBridge() {}

    /** {@code true} si SDM Economy est chargé et l'API résolue. */
    public static boolean available() {
        if (available != null) return available;
        if (!ModList.get().isLoaded("sdmeconomy")) { available = false; return false; }
        try {
            Class<?> dataCls = Class.forName("net.sixik.sdmeconomy.economyData.CurrencyPlayerData");
            Field serverField = dataCls.getField("SERVER");
            serverData = serverField.get(null);
            Class<?> playerCls = net.minecraft.world.entity.player.Player.class;
            addCurrencyValue = serverData.getClass().getMethod("addCurrencyValue", playerCls, String.class, double.class);
            getBalance = serverData.getClass().getMethod("getBalance", playerCls, String.class);
            available = true;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] SDM Economy présent mais API non résolue — bridge désactivé : {}", t.toString());
            available = false;
        }
        return available;
    }

    /** Crée la monnaie du shop si elle n'existe pas (idempotent). À appeler au démarrage serveur. */
    public static void ensureCurrency(MinecraftServer server) {
        if (!available()) return;
        try {
            String name = Config.getShopCurrencyName();
            Class<?> currencyCls = Class.forName("net.sixik.sdmeconomy.economy.Currency");
            Class<?> apiCls = Class.forName("net.sixik.sdmeconomy.api.EconomyAPI");
            Constructor<?> ctor = currencyCls.getConstructor(String.class);
            Object currency = ctor.newInstance(name);
            Method create = apiCls.getMethod("createCurrencyOnServer", currencyCls);
            create.invoke(null, currency); // no-op côté SDM si déjà présente
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec création monnaie SDM : {}", t.toString());
        }
    }

    /** Crédite {@code coins} au joueur. Retourne {@code true} si crédité, {@code false} si indisponible. */
    public static boolean addCoins(ServerPlayer player, long coins) {
        if (!available() || coins <= 0) return false;
        try {
            addCurrencyValue.invoke(serverData, player, Config.getShopCurrencyName(), (double) coins);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec crédit coins : {}", t.toString());
            return false;
        }
    }

    /** Solde de coins du joueur, ou 0 si indisponible. */
    public static long getCoins(ServerPlayer player) {
        if (!available()) return 0L;
        try {
            Object struct = getBalance.invoke(serverData, player, Config.getShopCurrencyName());
            // ErrorCodeStruct<Double> : champ 'value' (double). On lit par réflexion défensive.
            for (Field f : struct.getClass().getFields()) {
                Object v = f.get(struct);
                if (v instanceof Double d) return d.longValue();
            }
            return 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }
}
```

- [ ] **Step 2: Appeler `ensureCurrency` au démarrage serveur.** Ouvrir `src/main/java/tong/statmod/dungeon/DungeonExchanger.java`… (voir Task 7 — l'appel sera fait là via l'event `ServerStartedEvent`). Pour l'instant, seulement compiler le bridge.

Run: `./gradlew compileJava --console=plain -q`
Expected: pas d'erreur.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/integration/sdm/SDMEconomyBridge.java
git commit -m "feat(shop): bridge SDM Economy par réflexion (optionnel)"
```

---

### Task 5: Payloads réseau (S2C open + C2S convert)

**Files:**
- Create: `src/main/java/tong/statmod/network/OpenExchangePayload.java`
- Create: `src/main/java/tong/statmod/network/ConvertPointsPayload.java`
- Modify: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Modify: `src/main/java/tong/statmod/network/ServerPayloadHandler.java`
- Modify: `src/main/java/tong/statmod/network/ClientPayloadHandler.java`

- [ ] **Step 1: OpenExchangePayload (S2C)**

```java
package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/** S2C : ouvre/rafraîchit l'écran d'échange avec les points + coins courants. */
public record OpenExchangePayload(int points, long coins) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenExchangePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "open_exchange"));

    public static final StreamCodec<ByteBuf, OpenExchangePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenExchangePayload::points,
                    ByteBufCodecs.VAR_LONG, OpenExchangePayload::coins,
                    OpenExchangePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 2: ConvertPointsPayload (C2S)**

```java
package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/** C2S : demande de convertir {@code amount} points en coins. */
public record ConvertPointsPayload(int amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConvertPointsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "convert_points"));

    public static final StreamCodec<ByteBuf, ConvertPointsPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ConvertPointsPayload::amount,
                    ConvertPointsPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 3: Enregistrer dans NetworkHandler** (ajouter dans la méthode `register`, avec les autres)

```java
        registrar.playToClient(OpenExchangePayload.TYPE, OpenExchangePayload.CODEC,
                ClientPayloadHandler::handleOpenExchange);
        registrar.playToServer(ConvertPointsPayload.TYPE, ConvertPointsPayload.CODEC,
                ServerPayloadHandler::handleConvertPoints);
```

- [ ] **Step 4: Handler serveur** (dans `ServerPayloadHandler`)

```java
    public static void handleConvertPoints(ConvertPointsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            var data = sp.getData(tong.statmod.storage.ModAttachments.STATS);
            tong.statmod.dungeon.PointExchange.Result r = tong.statmod.dungeon.PointExchange.compute(
                    payload.amount(), data.getDungeonPoints(), tong.statmod.config.Config.getPointToCoinRate());
            if (r.converted() <= 0) return;
            boolean credited = tong.statmod.integration.sdm.SDMEconomyBridge.addCoins(sp, r.coins());
            if (!credited) {
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("shop.unavailable"), false);
                return; // ne pas consommer les points si le crédit a échoué
            }
            data.addDungeonPoints(-r.converted());
            tong.statmod.network.SyncHelper.syncStats(sp);
            long coins = tong.statmod.integration.sdm.SDMEconomyBridge.getCoins(sp);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp,
                    new OpenExchangePayload(data.getDungeonPoints(), coins)); // rafraîchit l'écran
            tong.statmod.dungeon.DungeonPointsEjection.enforce(sp); // 0 point → overworld
        });
    }
```

- [ ] **Step 5: Handler client** (dans `ClientPayloadHandler`)

```java
    public static void handleOpenExchange(OpenExchangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> tong.statmod.client.PointExchangeScreen.openOrRefresh(payload.points(), payload.coins()));
    }
```

- [ ] **Step 6: Compiler** (échouera tant que `PointExchangeScreen` n'existe pas — c'est attendu, on la crée Task 6)

Run: `./gradlew compileJava --console=plain -q 2>&1 | grep -iE "error:" | grep -v "PointExchangeScreen"`
Expected: aucune erreur autre que la référence à `PointExchangeScreen` (créée ensuite).

- [ ] **Step 7: Commit** (après Task 6 pour un état compilable — voir Task 6 Step 5)

---

### Task 6: PointExchangeScreen — écran GUI

**Files:**
- Create: `src/main/java/tong/statmod/client/PointExchangeScreen.java`

- [ ] **Step 1: Écrire l'écran**

```java
package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.network.ConvertPointsPayload;

/** Écran d'échange points → coins. Ouvert par OpenExchangePayload ; envoie ConvertPointsPayload. */
public class PointExchangeScreen extends Screen {

    private int points;
    private long coins;
    private EditBox amountBox;

    public PointExchangeScreen(int points, long coins) {
        super(Component.translatable("shop.exchange.title"));
        this.points = points;
        this.coins = coins;
    }

    /** Ouvre l'écran s'il n'est pas déjà ouvert, sinon met à jour ses valeurs (rafraîchissement). */
    public static void openOrRefresh(int points, long coins) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PointExchangeScreen s) {
            s.points = points;
            s.coins = coins;
        } else {
            mc.setScreen(new PointExchangeScreen(points, coins));
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        amountBox = new EditBox(this.font, cx - 60, cy - 10, 120, 20, Component.translatable("shop.exchange.amount"));
        amountBox.setValue("");
        addRenderableWidget(amountBox);
        addRenderableWidget(Button.builder(Component.translatable("shop.exchange.convert"),
                b -> convert(parseAmount())).bounds(cx - 60, cy + 16, 58, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("shop.exchange.max_safe"),
                b -> convert(Math.max(0, points - 1))).bounds(cx + 2, cy + 16, 58, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(cx - 60, cy + 40, 120, 20).build());
    }

    private int parseAmount() {
        try { return Integer.parseInt(amountBox.getValue().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void convert(int amount) {
        if (amount > 0) PacketDistributor.sendToServer(new ConvertPointsPayload(amount));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, this.title, cx, cy - 60, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("shop.exchange.points", points), cx, cy - 44, 0xFFE066);
        g.drawCenteredString(this.font, Component.translatable("shop.exchange.coins", coins), cx, cy - 32, 0x66FF66);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
```

- [ ] **Step 2: Compiler tout**

Run: `./gradlew compileJava --console=plain -q`
Expected: pas d'erreur.

- [ ] **Step 3: Lancer les tests existants dungeon**

Run: `./gradlew test --tests "tong.statmod.dungeon.*" --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (payloads + écran ensemble, état compilable)**

```bash
git add src/main/java/tong/statmod/network/OpenExchangePayload.java \
        src/main/java/tong/statmod/network/ConvertPointsPayload.java \
        src/main/java/tong/statmod/network/NetworkHandler.java \
        src/main/java/tong/statmod/network/ServerPayloadHandler.java \
        src/main/java/tong/statmod/network/ClientPayloadHandler.java \
        src/main/java/tong/statmod/client/PointExchangeScreen.java
git commit -m "feat(shop): payloads open/convert + écran d'échange GUI"
```

---

### Task 7: DungeonExchanger — villageois changeur + interaction + création monnaie

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonExchanger.java`

- [ ] **Step 1: Écrire la classe** (spawn villageois + handler interact + création monnaie au démarrage)

```java
package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.network.OpenExchangePayload;
import tong.statmod.storage.ModAttachments;

/**
 * Mission M6 — Villageois changeur (points → coins) des étages trésor (2026-07-05).
 *
 * <p>Bloqué (NoAI, invulnérable, persistant), tagué {@link #TAG}, sans trade vanilla. Clic-droit →
 * ouvre l'écran d'échange. La monnaie SDM est créée au démarrage serveur.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonExchanger {

    /** Marqueur NBT du changeur (exempté du nettoyage des mobs, comme les marchands). */
    public static final String TAG = "statmod_dungeon_exchanger";

    private DungeonExchanger() {}

    /** Pose un villageois changeur à {@code pos}. */
    public static void spawn(ServerLevel lv, BlockPos pos) {
        Villager v = EntityType.VILLAGER.create(lv);
        if (v == null) return;
        v.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180f, 0f);
        v.setYBodyRot(180f);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CARTOGRAPHER, 5));
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setPersistenceRequired();
        v.setSilent(true);
        v.setCustomName(net.minecraft.network.chat.Component.translatable("shop.exchanger.name"));
        v.setCustomNameVisible(true);
        v.getPersistentData().putBoolean(TAG, true);
        v.getPersistentData().putBoolean(DungeonMerchant.MERCHANT_TAG, true); // exempt du nettoyage
        DungeonSpawnGuard.spawnAuthorized(() -> { lv.addFreshEntity(v); return v; });
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SDMEconomyBridge.ensureCurrency(event.getServer());
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!event.getTarget().getPersistentData().getBoolean(TAG)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        int points = sp.getData(ModAttachments.STATS).getDungeonPoints();
        long coins = SDMEconomyBridge.getCoins(sp);
        PacketDistributor.sendToPlayer(sp, new OpenExchangePayload(points, coins));
    }
}
```

- [ ] **Step 2: Compiler**

Run: `./gradlew compileJava --console=plain -q`
Expected: pas d'erreur.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonExchanger.java
git commit -m "feat(shop): villageois changeur + interaction + création monnaie SDM"
```

---

### Task 8: Poser le changeur dans la salle trésor

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonRoomChain.java`

- [ ] **Step 1: Repérer `treasureRoom`** et ajouter le spawn du changeur à la fin de la méthode (juste avant sa `}` finale), à un coin de la salle pour ne pas gêner les coffres du centre :

```java
        // Villageois changeur (points → coins) au bord ouest de la salle au trésor.
        DungeonExchanger.spawn(lv, O(sp, r.minX() + 3, 0, r.centerZ()));
```

- [ ] **Step 2: Compiler + tests dungeon**

Run: `./gradlew compileJava --console=plain -q && ./gradlew test --tests "tong.statmod.dungeon.*" --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonRoomChain.java
git commit -m "feat(shop): pose le changeur dans la salle au trésor"
```

---

### Task 9: Éjection après la pénalité de mort

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonPoints.java`

- [ ] **Step 1: Trouver `applyDeathPenalty(ServerPlayer player)`** et, juste après le `SyncHelper.syncStats(player)` final (ou à la fin de la méthode après avoir retiré les points), ajouter :

```java
        // Si la pénalité a vidé les points, le joueur est éjecté vers l'overworld.
        DungeonPointsEjection.enforce(player);
```

- [ ] **Step 2: Compiler**

Run: `./gradlew compileJava --console=plain -q`
Expected: pas d'erreur.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/dungeon/DungeonPoints.java
git commit -m "feat(shop): éjection overworld si la mort vide les points"
```

---

### Task 10: Textes (lang)

**Files:**
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`

- [ ] **Step 1: Ajouter au `en_us.json`** (avant la `}` finale, avec une virgule sur la ligne précédente)

```json
  "shop.exchange.title": "Point Exchange",
  "shop.exchange.points": "Points: %s",
  "shop.exchange.coins": "Coins: %s",
  "shop.exchange.amount": "Amount",
  "shop.exchange.convert": "Convert",
  "shop.exchange.max_safe": "Max safe",
  "shop.exchanger.name": "Coin Changer",
  "shop.unavailable": "The shop is unavailable (SDM not installed).",
  "dungeon.ejected.no_points": "Out of points — ejected from the dungeon!"
```

- [ ] **Step 2: Ajouter au `fr_fr.json`**

```json
  "shop.exchange.title": "Échange de points",
  "shop.exchange.points": "Points : %s",
  "shop.exchange.coins": "Coins : %s",
  "shop.exchange.amount": "Montant",
  "shop.exchange.convert": "Convertir",
  "shop.exchange.max_safe": "Max sûr",
  "shop.exchanger.name": "Changeur",
  "shop.unavailable": "Le shop est indisponible (SDM non installé).",
  "dungeon.ejected.no_points": "Plus de points — éjecté du donjon !"
```

- [ ] **Step 3: Build complet + tests**

Run: `./gradlew build -x test --console=plain -q && ./gradlew test --tests "tong.statmod.dungeon.*" --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json
git commit -m "feat(shop): textes en/fr de l'échange et de l'éjection"
```

---

### Task 11: Vérification en jeu (manuelle)

- [ ] **Step 1: Lancer** `./gradlew runClient`
- [ ] **Step 2:** `/statdungeon unlock 20` puis `/statdungeon tp 5` (étage trésor).
- [ ] **Step 3:** Tuer des mobs sur des étages combat pour accumuler des points (HUD).
- [ ] **Step 4:** Sur l'étage trésor, clic-droit le **Changeur** → l'écran s'ouvre (points + coins).
- [ ] **Step 5:** Saisir un montant < points → Convertir → points baissent, coins montent (`/sdmeconomy` ou le shop pour vérifier le solde).
- [ ] **Step 6:** Tester **Max sûr** puis convertir le reste jusqu'à 0 → **éjection vers l'overworld** + message.
- [ ] **Step 7:** Configurer le shop SDM en jeu (devise `dungeon_coins`) et acheter un article avec les coins.

---

## Self-Review

- **Spec coverage :** bridge (T4), villageois changeur (T7), payloads (T5), écran (T6), éjection (T3+T9), config (T1), placement (T8), textes (T10), tests (T2+T3). ✓
- **Placeholders :** aucun — tout le code est fourni.
- **Cohérence des types :** `PointExchange.Result(converted, coins, remainingPoints)` utilisé identiquement en T2/T5 ; `SDMEconomyBridge.addCoins/getCoins` signatures cohérentes T4/T5/T7 ; `OpenExchangePayload(int,long)` / `ConvertPointsPayload(int)` cohérents T5/T6/T7 ; `DungeonExchanger.TAG` et `DungeonMerchant.MERCHANT_TAG` réutilisés. ✓
- **WIP utilisateur :** aucune modification des fichiers perk/NonCombat/mixin/SyncHelper au-delà de l'ajout de handlers réseau standards.
