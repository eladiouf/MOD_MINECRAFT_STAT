package tong.statmod.command;

public final class StatsCommandRules {
    public static final int ADMIN_PERMISSION = 2;

    private StatsCommandRules() {
    }

    public static boolean validLevel(int level) {
        return level >= 0 && level <= 100;
    }

    public static boolean validXpAmount(int amount) {
        return amount > 0;
    }
}
