package tong.statmod.integration.sdm;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.STATMod;
import tong.statmod.economy.FdpDenomination;

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
                    Field valField = pc.getClass().getField("balance");
                    Object curr = currField.get(pc);
                    Object val = valField.get(pc);
                    String name = "unknown";
                    if (curr != null) {
                        try { name = (String) curr.getClass().getMethod("getName").invoke(curr); } catch (Throwable ignored) {}
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
                Field valField = pc.getClass().getField("balance");

                Object curr = currField.get(pc);
                double val = valField.getDouble(pc);
                
                String name = "unknown";
                if (curr != null) {
                    try { name = (String) curr.getClass().getMethod("getName").invoke(curr); } catch (Throwable ignored) {}
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

    /**
     * Garantit que le joueur possède une entrée de devise {@code FDP_cfa}.
     *
     * <p>On <b>n'utilise pas</b> {@code newPlayer} : il reconstruit la liste depuis
     * {@code CurrencyData.SERVER.currencies} et surtout <b>remet à zéro toutes les devises
     * existantes</b> (bug SDM : {@code playersCurrencyMap.put(uuid, listeNeuve)}). On injecte
     * donc directement l'entrée manquante dans la liste du joueur, sans toucher aux soldes
     * existants, et sans dépendre de l'état du registre runtime.
     */
    private static void ensurePlayerHasCurrency(ServerPlayer player) {
        if (serverData == null) return;
        try {
            Field mapField = serverData.getClass().getField("playersCurrencyMap");
            @SuppressWarnings("unchecked")
            Map<UUID, LinkedList<Object>> pMap = (Map<UUID, LinkedList<Object>>) mapField.get(serverData);
            LinkedList<Object> list = pMap.get(player.getUUID());
            if (list != null) {
                for (Object pc : list) {
                    if (FdpDenomination.CURRENCY_ID.equals(currencyName(pc))) return; // déjà présente
                }
            } else {
                list = new LinkedList<>();
                pMap.put(player.getUUID(), list);
            }
            Class<?> currencyCls = Class.forName("net.sixik.sdmeconomy.economy.Currency");
            Object currency = currencyCls.getConstructor(String.class).newInstance(FdpDenomination.CURRENCY_ID);
            Class<?> pcCls = Class.forName("net.sixik.sdmeconomy.economyData.CurrencyPlayerData$PlayerCurrency");
            Object pc = pcCls.getConstructor(currencyCls, double.class).newInstance(currency, 0.0);
            list.add(pc);
            LOGGER.info("[Shop] Devise {} injectée pour {}", FdpDenomination.CURRENCY_ID, player.getName().getString());
        } catch (Throwable t) {
            LOGGER.warn("[Shop] ensurePlayerHasCurrency échoué: {}", t.toString());
        }
    }

    /** Nom de la devise via {@code Currency.getName()}. */
    private static String currencyName(Object playerCurrency) {
        try {
            Object curr = playerCurrency.getClass().getField("currency").get(playerCurrency);
            if (curr == null) return null;
            return (String) curr.getClass().getMethod("getName").invoke(curr);
        } catch (Throwable t) {
            return null;
        }
    }

    public static void ensureCurrency(MinecraftServer server) {}

    /** Trouve l'entrée FDP_cfa dans le PlayerCurrency du joueur. */
    private static Object findFdpEntry(ServerPlayer player) {
        try {
            Field mapField = serverData.getClass().getField("playersCurrencyMap");
            @SuppressWarnings("unchecked")
            Map<UUID, LinkedList<Object>> pMap = (Map<UUID, LinkedList<Object>>) mapField.get(serverData);
            LinkedList<Object> list = pMap.get(player.getUUID());
            if (list == null) return null;
            for (Object pc : list) {
                if (FdpDenomination.CURRENCY_ID.equals(currencyName(pc))) return pc;
            }
        } catch (Exception e) {
            LOGGER.warn("[Shop] findFdpEntry échoué: {}", e.toString());
        }
        return null;
    }

    /** Persiste et sync le wallet côté client. */
    private static void saveAndSync(ServerPlayer player) {
        try {
            Class<?> dataCls = Class.forName("net.sixik.sdmeconomy.economyData.CurrencyPlayerData");
            dataCls.getMethod("save", MinecraftServer.class).invoke(null, player.server);
            Class<?> helperCls = Class.forName("net.sixik.sdmeconomy.utils.CurrencyHelper");
            helperCls.getMethod("syncPlayer", net.minecraft.server.level.ServerPlayer.class).invoke(null, player);
        } catch (Throwable t) {
            LOGGER.warn("[Shop] saveAndSync échoué: {}", t.toString());
        }
    }

    public static boolean addCoins(ServerPlayer player, long coins) {
        if (!available() || coins <= 0) return false;
        try {
            deduplicateCurrencies(player);
            ensurePlayerHasCurrency(player);
            Object pc = findFdpEntry(player);
            if (pc == null) return false;
            Field bf = pc.getClass().getField("balance");
            double current = bf.getDouble(pc);
            bf.setDouble(pc, current + coins);
            LOGGER.info("[Shop] addCoins({}) FDP_cfa balance={} → {}", coins, current, current + coins);
            saveAndSync(player);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec crédit coins : {}", t.toString());
            return false;
        }
    }

    public static boolean removeCoins(ServerPlayer player, long coins) {
        if (!available() || coins <= 0) return false;
        try {
            deduplicateCurrencies(player);
            ensurePlayerHasCurrency(player);
            Object pc = findFdpEntry(player);
            if (pc == null) return false;
            Field bf = pc.getClass().getField("balance");
            double current = bf.getDouble(pc);
            if (current < coins) return false;
            bf.setDouble(pc, current - coins);
            LOGGER.info("[Shop] removeCoins({}) FDP_cfa balance={} → {}", coins, current, current - coins);
            saveAndSync(player);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec débit coins : {}", t.toString());
            return false;
        }
    }

    public static long getCoins(ServerPlayer player) {
        if (!available()) return 0L;
        try {
            deduplicateCurrencies(player);
            ensurePlayerHasCurrency(player);
            Object pc = findFdpEntry(player);
            if (pc == null) return 0L;
            return (long) pc.getClass().getField("balance").getDouble(pc);
        } catch (Throwable t) {
            LOGGER.warn("[Shop] Échec lecture solde coins : {}", t.toString());
            return 0L;
        }
    }
}
