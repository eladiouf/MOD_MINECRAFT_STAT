package tong.statmod.dungeon.party;

public final class DungeonAdventurerRolePolicy {
    private static final PartyRole[] STANDARD = {
            PartyRole.TANK, PartyRole.ARCHER, PartyRole.ASSASSIN,
            PartyRole.MAGE, PartyRole.HEALER
    };
    private static final PartyRole[] BOSS = {
            PartyRole.TANK, PartyRole.MAGE, PartyRole.HEALER,
            PartyRole.ASSASSIN, PartyRole.ARCHER
    };

    private DungeonAdventurerRolePolicy() {}

    public static PartyRole roleFor(int floor, int roomIndex, int ordinal, boolean boss) {
        PartyRole[] roles = boss ? BOSS : STANDARD;
        int offset = boss ? 0 : Math.floorMod(floor * 31 + roomIndex * 17, roles.length);
        return roles[Math.floorMod(offset + ordinal, roles.length)];
    }
}
