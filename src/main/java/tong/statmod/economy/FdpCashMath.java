package tong.statmod.economy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Calculs purs de décomposition des montants FDP. */
public final class FdpCashMath {
    private FdpCashMath() {}

    public static boolean isWithdrawable(long amount) {
        return amount > 0 && amount % 50L == 0L;
    }

    public static Map<Long, Integer> breakdown(long amount) {
        if (!isWithdrawable(amount)) throw new IllegalArgumentException("Le montant doit être positif et multiple de 50");
        Map<Long, Integer> result = new LinkedHashMap<>();
        long remaining = amount;
        for (var denomination : FdpDenomination.descending()) {
            int count = (int) (remaining / denomination.value());
            if (count > 0) result.put(denomination.value(), count);
            remaining %= denomination.value();
        }
        return result;
    }
}
