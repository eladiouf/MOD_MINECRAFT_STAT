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
    private static Method newPlayerMethod;  // (Player) -> void

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
            try {
                newPlayerMethod = serverData.getClass().getMethod("newPlayer", playerCls);
            } catch (Throwable t) {
                // Ignore si la méthode n'existe pas dans cette version
            }
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

            // Nettoyage de toutes les autres devises
            try {
                Method getAll = apiCls.getMethod("getAllCurrency");
                Object struct = getAll.invoke(null);
                if (struct != null) {
                    Field valField = struct.getClass().getField("value");
                    Object currencyData = valField.get(struct);
                    if (currencyData != null) {
                        Field listField = currencyData.getClass().getField("currencies");
                        java.util.List<?> currenciesList = (java.util.List<?>) listField.get(currencyData);
                        if (currenciesList != null) {
                            java.util.List<Object> toDelete = new java.util.ArrayList<>();
                            for (Object curr : currenciesList) {
                                Method getName = curr.getClass().getMethod("getName");
                                String currName = (String) getName.invoke(curr);
                                if (currName != null && !currName.equalsIgnoreCase(name)) {
                                    toDelete.add(curr);
                                }
                            }
                            Method delete = apiCls.getMethod("deleteCurrencyOnServer", currencyCls);
                            for (Object curr : toDelete) {
                                delete.invoke(null, curr);
                                LOGGER.info("[Shop] Deleting unused SDM currency: {}", curr.getClass().getMethod("getName").invoke(curr));
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("[Shop] Impossible de nettoyer les autres monnaies SDM : {}", t.toString());
            }

            Constructor<?> ctor = currencyCls.getConstructor(String.class);
            Object currency = ctor.newInstance(name);
            Method create = apiCls.getMethod("createCurrencyOnServer", currencyCls);
            create.invoke(null, currency); // no-op côté SDM si déjà présente

            // Sauvegarder et synchroniser vers les clients
            try {
                Method save = apiCls.getMethod("saveCurrencyData");
                save.invoke(null);
            } catch (Throwable t) {}
            try {
                Method sync = apiCls.getMethod("syncCurrencyData", MinecraftServer.class);
                sync.invoke(null, server);
            } catch (Throwable t) {}
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec création monnaie SDM : {}", t.toString());
        }
    }

    /** Crédite {@code coins} au joueur. Retourne {@code true} si crédité, {@code false} si indisponible. */
    public static boolean addCoins(ServerPlayer player, long coins) {
        if (!available() || coins <= 0) return false;
        try {
            if (newPlayerMethod != null) {
                try {
                    newPlayerMethod.invoke(serverData, player);
                } catch (Throwable t) {}
            }
            Object result = addCurrencyValue.invoke(serverData, player, Config.getShopCurrencyName(), (double) coins);
            if (result != null) {
                String status = result.toString();
                if (status.equals("FAIL") || status.equals("NOT_FOUND") || status.equals("NOT_ACCESS")) {
                    LOGGER.warn("[Shop] Échec crédit coins (SDM a retourné {})", status);
                    return false;
                }
            }
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
            if (newPlayerMethod != null) {
                try {
                    newPlayerMethod.invoke(serverData, player);
                } catch (Throwable t) {}
            }
            Object struct = getBalance.invoke(serverData, player, Config.getShopCurrencyName());
            if (struct == null) return 0L;

            Field valueField = null;
            try {
                valueField = struct.getClass().getField("value");
            } catch (NoSuchFieldException e) {
                // Recherche par type
                for (Field f : struct.getClass().getFields()) {
                    if (f.getName().equals("value") || Number.class.isAssignableFrom(f.getType()) || Object.class.equals(f.getType())) {
                        valueField = f;
                        break;
                    }
                }
            }

            if (valueField != null) {
                Object v = valueField.get(struct);
                if (v instanceof Number n) {
                    return n.longValue();
                } else if (v instanceof String s) {
                    try {
                        return (long) Double.parseDouble(s);
                    } catch (NumberFormatException nfe) {
                        return 0L;
                    }
                }
            }
            return 0L;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec lecture solde coins : {}", t.toString());
            return 0L;
        }
    }
}
