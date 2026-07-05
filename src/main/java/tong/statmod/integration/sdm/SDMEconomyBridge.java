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
            // ErrorCodeStruct<Double> : on lit le premier champ Double par réflexion défensive.
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
