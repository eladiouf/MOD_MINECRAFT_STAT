package tong.statmod.dungeon;

/** Règles pures d'autorisation d'une conversion auprès du changeur de donjon. */
public final class DungeonExchangeAccess {

    /** Le joueur doit rester à huit blocs maximum du changeur. */
    static final double MAX_DISTANCE_SQUARED = 64.0;

    private DungeonExchangeAccess() {}

    static boolean isAllowed(long currentTick, long expiresAtTick, boolean sameDimension,
                             boolean taggedExchanger, double distanceSquared) {
        return currentTick <= expiresAtTick
                && sameDimension
                && taggedExchanger
                && distanceSquared <= MAX_DISTANCE_SQUARED;
    }
}
