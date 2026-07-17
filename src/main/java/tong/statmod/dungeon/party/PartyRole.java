package tong.statmod.dungeon.party;

public enum PartyRole {
    TANK,
    ASSASSIN,
    MAGE,
    HEALER,
    ARCHER;

    public static final String TAG = "statmod_party_role";

    public static PartyRole byIndex(int i) {
        return values()[i % values().length];
    }
}
