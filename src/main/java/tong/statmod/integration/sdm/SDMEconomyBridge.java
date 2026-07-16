package tong.statmod.integration.sdm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.network.SyncHelper;

public final class SDMEconomyBridge {
    public static final String CURRENCY_ID = "sdm_coin";

    private static final Logger LOGGER = LoggerFactory.getLogger(StatMod.class);
    private static Boolean available;
    private static Object serverData;
    private static Method newPlayer;
    private static Method addCurrencyValue;
    private static Method setCurrencyValue;
    private static Method getBalance;
    private static Method savePlayerData;
    private static Method syncPlayer;

    private SDMEconomyBridge() {
    }

    public static boolean available() {
        if (available != null) {
            return available;
        }
        if (!ModList.get().isLoaded("sdmeconomy")) {
            available = false;
            return false;
        }
        try {
            Class<?> api = Class.forName("net.sixik.sdmeconomy.api.EconomyAPI");
            serverData = api.getMethod("getPlayerCurrencyServerData").invoke(null);
            Class<?> dataType = serverData.getClass();
            newPlayer = dataType.getMethod("newPlayer", Player.class);
            addCurrencyValue = dataType.getMethod(
                    "addCurrencyValue", Player.class, String.class, double.class);
            setCurrencyValue = dataType.getMethod(
                    "setCurrencyValue", Player.class, String.class, double.class);
            getBalance = dataType.getMethod("getBalance", Player.class, String.class);
            savePlayerData = api.getMethod("savePlayerData", MinecraftServer.class);
            syncPlayer = api.getMethod("syncPlayer", ServerPlayer.class);
            available = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("[Shop] SDMEconomy 2.2.0 API unavailable", exception);
            available = false;
        }
        return available;
    }

    public static boolean addCoins(ServerPlayer player, long coins) {
        if (coins <= 0 || !prepare(player)) {
            return false;
        }
        try {
            Object result = addCurrencyValue.invoke(serverData, player, CURRENCY_ID, (double) coins);
            return finishMutation(player, result);
        } catch (ReflectiveOperationException exception) {
            return mutationFailed("credit", player, exception);
        }
    }

    public static boolean removeCoins(ServerPlayer player, long coins) {
        if (coins <= 0 || !prepare(player)) {
            return false;
        }
        try {
            double balance = balanceValue(getBalance.invoke(serverData, player, CURRENCY_ID));
            if (balance < coins) {
                return false;
            }
            Object result = setCurrencyValue.invoke(
                    serverData, player, CURRENCY_ID, balance - coins);
            return finishMutation(player, result);
        } catch (ReflectiveOperationException exception) {
            return mutationFailed("debit", player, exception);
        }
    }

    public static long getCoins(ServerPlayer player) {
        if (!prepare(player)) {
            return 0L;
        }
        try {
            return (long) Math.floor(balanceValue(
                    getBalance.invoke(serverData, player, CURRENCY_ID)));
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("[Shop] Could not read {} balance for {}",
                    CURRENCY_ID, player.getGameProfile().getName(), exception);
            return 0L;
        }
    }

    public static boolean ensureStartingBalance(ServerPlayer player, long amount) {
        if (amount <= 0) {
            return false;
        }
        boolean[] completed = {false};
        player.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(stats -> {
            if (stats.hasReceivedShopStartingBalance()) {
                completed[0] = true;
                return;
            }
            if (addCoins(player, amount)) {
                stats.markShopStartingBalanceReceived();
                SyncHelper.syncStats(player);
                completed[0] = true;
            }
        });
        return completed[0];
    }

    private static boolean prepare(ServerPlayer player) {
        if (player == null || !available()) {
            return false;
        }
        try {
            newPlayer.invoke(serverData, player);
            return true;
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("[Shop] Could not initialize SDMEconomy data for {}",
                    player.getGameProfile().getName(), exception);
            return false;
        }
    }

    private static boolean finishMutation(ServerPlayer player, Object result)
            throws ReflectiveOperationException {
        if (!isSuccess(result)) {
            return false;
        }
        savePlayerData.invoke(null, player.server);
        Object syncResult = syncPlayer.invoke(null, player);
        if (!isSuccess(syncResult)) {
            LOGGER.warn("[Shop] Balance saved but sync failed for {}",
                    player.getGameProfile().getName());
        }
        return true;
    }

    private static double balanceValue(Object result) throws ReflectiveOperationException {
        Field codes = result.getClass().getField("codes");
        if (!isSuccess(codes.get(result))) {
            return 0D;
        }
        Object value = result.getClass().getField("value").get(result);
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private static boolean isSuccess(Object errorCode) throws ReflectiveOperationException {
        return errorCode != null
                && Boolean.TRUE.equals(errorCode.getClass().getMethod("isSuccess").invoke(errorCode));
    }

    private static boolean mutationFailed(
            String operation, ServerPlayer player, ReflectiveOperationException exception) {
        LOGGER.warn("[Shop] SDMEconomy {} failed for {}",
                operation, player.getGameProfile().getName(), exception);
        return false;
    }
}
