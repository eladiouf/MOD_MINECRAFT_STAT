package tong.statmod.dungeon.ai;

public final class DungeonTacticalRolePolicy {
    private static final DungeonTacticalRole[] EARLY = {
            DungeonTacticalRole.SHIELD_CAPTAIN, DungeonTacticalRole.SPEAR_KEEPER,
            DungeonTacticalRole.SCOUT, DungeonTacticalRole.WARDEN
    };
    private static final DungeonTacticalRole[] MID = {
            DungeonTacticalRole.SHIELD_CAPTAIN, DungeonTacticalRole.SPEAR_KEEPER,
            DungeonTacticalRole.SCOUT, DungeonTacticalRole.WARDEN,
            DungeonTacticalRole.BERSERKER, DungeonTacticalRole.HUNTER
    };
    private static final DungeonTacticalRole[] ADVANCED = {
            DungeonTacticalRole.SHIELD_CAPTAIN, DungeonTacticalRole.SPEAR_KEEPER,
            DungeonTacticalRole.BERSERKER, DungeonTacticalRole.WARDEN,
            DungeonTacticalRole.SPELLBREAKER, DungeonTacticalRole.HEXER,
            DungeonTacticalRole.SAPPER, DungeonTacticalRole.SCOUT,
            DungeonTacticalRole.HUNTER, DungeonTacticalRole.AMBUSHER
    };
    private static final DungeonTacticalRole[] DEEP = DungeonTacticalRole.values();
    private static final DungeonTacticalRole[] BOSS = {
            DungeonTacticalRole.SHIELD_CAPTAIN, DungeonTacticalRole.ELEMENTAL_CASTER,
            DungeonTacticalRole.BATTLE_CLERIC, DungeonTacticalRole.SPELLBREAKER,
            DungeonTacticalRole.ARCANE_ARTILLERY, DungeonTacticalRole.JAILER
    };

    private DungeonTacticalRolePolicy() {}

    public static DungeonTacticalRole roleFor(DungeonFaction faction, int floor,
                                              int room, int ordinal, boolean boss) {
        DungeonTacticalRole[] roles = boss ? BOSS
                : floor <= 10 ? EARLY
                : floor <= 30 ? MID
                : floor <= 50 ? ADVANCED : DEEP;
        if (boss) return roles[Math.floorMod(ordinal, roles.length)];
        int seed = floor * 31 + room * 17 + ordinal + faction.ordinal() * 13;
        return roles[Math.floorMod(seed, roles.length)];
    }
}
