package tong.statmod.integration.sdm;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.STATMod;
import tong.statmod.config.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Bridge vers SDM Economy.
 */
public final class SDMEconomyBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);
    private static Boolean available;   
    private static Object serverData;   
    private static Method addCurrencyValue; 
    private static Method getBalance;       
    private static Method getPlayerCurrency; 

    private static Object customCurrenciesMap; 
    private static Method currenciesGet;       
    private static Method newPlayerMethod;

    private SDMEconomyBridge() {}

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
                getPlayerCurrency = serverData.getClass().getMethod("getPlayerCurrency", playerCls, String.class);
            } catch (Throwable t) {
                LOGGER.warn("[Shop] getPlayerCurrency non trouvé: {}", t.toString());
            }
            try {
                newPlayerMethod = serverData.getClass().getMethod("newPlayer", playerCls);
            } catch (Throwable t) {
                LOGGER.warn("[Shop] newPlayer non trouvé: {}", t.toString());
            }
            try {
                Class<?> customCls = Class.forName("net.sixik.sdmeconomy.api.CustomCurrencies");
                Field currField = customCls.getField("CURRENCIES");
                customCurrenciesMap = currField.get(null);
                currenciesGet = customCurrenciesMap.getClass().getMethod("get", Object.class);
            } catch (Throwable t) {
                LOGGER.warn("[Shop] CustomCurrencies non résolu: {}", t.toString());
            }
            available = true;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] API non résolue : {}", t.toString());
            available = false;
        }
        return available;
    }

    private static void logAllCurrencies(ServerPlayer player, String prefix) {
        if (serverData == null) return;
        try {
            Field mapField = serverData.getClass().getField("playersCurrencyMap");
            @SuppressWarnings("unchecked")
            Map<UUID, LinkedList<Object>> pMap = (Map<UUID, LinkedList<Object>>) mapField.get(serverData);
            LinkedList<Object> list = pMap.get(player.getUUID());
            LOGGER.info("[Banker-Debug] {} Player {} (UUID={}) has {} currencies in map", prefix, player.getName().getString(), player.getUUID(), list == null ? 0 : list.size());
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Object pc = list.get(i);
                    Field currField = pc.getClass().getField("currency");
                    Field valField = pc.getClass().getField("value");
                    Object curr = currField.get(pc);
                    Object val = valField.get(pc);
                    String name = "unknown";
                    if (curr != null) {
                        Field nameField = curr.getClass().getField("name");
                        name = (String) nameField.get(curr);
                    }
                    LOGGER.info("[Banker-Debug] {}   [{}] {} = {}", prefix, i, name, val);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[Banker-Debug] Error logging currencies", e);
        }
    }

    private static void deduplicateCurrencies(ServerPlayer player) {
        if (serverData == null) return;
        try {
            Field mapField = serverData.getClass().getField("playersCurrencyMap");
            @SuppressWarnings("unchecked")
            Map<UUID, LinkedList<Object>> pMap = (Map<UUID, LinkedList<Object>>) mapField.get(serverData);
            LinkedList<Object> list = pMap.get(player.getUUID());
            if (list == null || list.isEmpty()) return;

            java.util.Map<String, Object> bestEntries = new java.util.HashMap<>();
            java.util.Map<String, Double> maxValues = new java.util.HashMap<>();
            boolean hasDuplicates = false;

            for (Object pc : list) {
                Field currField = pc.getClass().getField("currency");
                Field valField = null;
                for (Field f : pc.getClass().getFields()) {
                    if (f.getName().equals("value") || Number.class.isAssignableFrom(f.getType())) {
                        valField = f;
                        break;
                    }
                }
                if (valField == null) continue;

                Object curr = currField.get(pc);
                Object valObj = valField.get(pc);
                double val = 0.0;
                if (valObj instanceof Number n) val = n.doubleValue();
                else if (valObj instanceof String s) {
                    try { val = Double.parseDouble(s); } catch (Exception ignored) {}
                }
                
                String name = "unknown";
                if (curr != null) {
                    Field nameField = curr.getClass().getField("name");
                    name = (String) nameField.get(curr);
                }

                if (!bestEntries.containsKey(name)) {
                    bestEntries.put(name, pc);
                    maxValues.put(name, val);
                } else {
                    hasDuplicates = true;
                    if (val > maxValues.get(name)) {
                        bestEntries.put(name, pc);
                        maxValues.put(name, val);
                    }
                }
            }

            if (hasDuplicates) {
                list.clear();
                list.addAll(bestEntries.values());
                LOGGER.info("[Banker-Debug] Deduplicated currencies for {}. New size: {}", player.getName().getString(), list.size());
            }
        } catch (Exception e) {
            LOGGER.error("[Banker-Debug] Error deduplicating currencies", e);
        }
    }

    private static void ensurePlayerHasCurrency(ServerPlayer player) {
        deduplicateCurrencies(player);
        if (getPlayerCurrency == null) return;
        try {
            String name = Config.getShopCurrencyName();
            Optional<?> opt = (Optional<?>) getPlayerCurrency.invoke(serverData, player, name);
            if (opt != null && opt.isPresent()) {
                return; 
            }
            if (newPlayerMethod != null) {
                newPlayerMethod.invoke(serverData, player);
                deduplicateCurrencies(player);
            }
        } catch (Throwable t) {
            LOGGER.warn("[Shop] ensurePlayerHasCurrency échoué: {}", t.toString());
        }
    }

    public static void ensureCurrency(MinecraftServer server) {}

    public static boolean addCoins(ServerPlayer player, long coins) {
        if (!available() || coins <= 0) return false;
        try {
            ensurePlayerHasCurrency(player);
            Object result = addCurrencyValue.invoke(serverData, player, Config.getShopCurrencyName(), (double) coins);
            if (result != null) {
                String status = result.toString();
                if (status.equals("FAIL") || status.equals("NOT_FOUND") || status.equals("NOT_ACCESS")) {
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec crédit coins : {}", t.toString());
            return false;
        }
    }

    public static boolean removeCoins(ServerPlayer player, long coins) {
        if (!available() || coins <= 0) return false;
        long current = getCoins(player);
        if (current < coins) {
            return false;
        }
        try {
            Object result = addCurrencyValue.invoke(serverData, player, Config.getShopCurrencyName(), -(double) coins);
            if (result != null) {
                String status = result.toString();
                if (status.equals("FAIL") || status.equals("NOT_FOUND") || status.equals("NOT_ACCESS")) {
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec débit coins : {}", t.toString());
            return false;
        }
    }

    public static long getCoins(ServerPlayer player) {
        if (!available()) return 0L;
        try {
            ensurePlayerHasCurrency(player);
            String currencyName = Config.getShopCurrencyName();
            Object struct = getBalance.invoke(serverData, player, currencyName);
            if (struct == null) return 0L;

            Field valueField = null;
            try {
                valueField = struct.getClass().getField("value");
            } catch (NoSuchFieldException e) {
                for (Field f : struct.getClass().getFields()) {
                    if (f.getName().equals("value") || Number.class.isAssignableFrom(f.getType())) {
                        valueField = f;
                        break;
                    }
                }
            }

            if (valueField != null) {
                Object v = valueField.get(struct);
                long val = 0;
                if (v instanceof Number n) val = n.longValue();
                else if (v instanceof String s) {
                    try { val = (long) Double.parseDouble(s); } catch (NumberFormatException ignored) {}
                }
                return val;
            }
            return 0L;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec lecture solde coins : {}", t.toString());
            return 0L;
        }
    }
}
