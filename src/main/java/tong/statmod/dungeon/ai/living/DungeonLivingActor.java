package tong.statmod.dungeon.ai.living;

import java.util.Optional;
import net.minecraft.world.entity.Mob;
import tong.statmod.dungeon.ai.DungeonAiActor;
import tong.statmod.dungeon.ai.DungeonFaction;
import tong.statmod.dungeon.ai.DungeonTacticalRole;
import tong.statmod.dungeon.party.PartyRole;

public final class DungeonLivingActor {
    public static final String LIVING_ROLE_TAG = "statmod_living_role";
    public static final String NON_COMBAT_TAG = "statmod_living_non_combat";
    public static final String PRISONER_RELEASED_TAG = "statmod_prisoner_released";
    public static final String PRISONER_OWNER_TAG = "statmod_prisoner_owner";
    public static final String LIVING_MARKER_PREFIX = "statmod:living:";

    private DungeonLivingActor() {}

    public static void initializeNeutral(Mob mob, DungeonLivingRole role, int floor, int room) {
        mob.getPersistentData().putString(LIVING_ROLE_TAG, role.name());
        mob.getPersistentData().putBoolean(NON_COMBAT_TAG, true);
        mob.getPersistentData().putInt("statmod_dungeon_room", room);
        DungeonAiActor.initialize(mob, DungeonFaction.INHABITANTS, floor,
                floor + ":living:" + room, DungeonTacticalRole.WARDEN);
    }

    public static void initializeCombat(Mob mob, DungeonLivingRole role, int floor, int room) {
        mob.getPersistentData().putString(LIVING_ROLE_TAG, role.name());
        mob.getPersistentData().putBoolean(NON_COMBAT_TAG, false);
        mob.getPersistentData().putInt("statmod_dungeon_room", room);
        DungeonFaction faction = switch (role) {
            case RIVAL_EXPLORER -> DungeonFaction.ADVENTURER_RIVALS;
            case RITUALIST -> DungeonFaction.CULT_OF_CINDERS;
            case ENGINEER -> DungeonFaction.ARCANE_ORDER;
            default -> DungeonFaction.INHABITANTS;
        };
        DungeonTacticalRole tacticalRole = switch (role) {
            case RIVAL_EXPLORER -> DungeonTacticalRole.HUNTER;
            case RITUALIST -> DungeonTacticalRole.HEXER;
            case ENGINEER -> DungeonTacticalRole.WARDEN;
            default -> DungeonTacticalRole.WARDEN;
        };
        DungeonAiActor.initialize(mob, faction, floor, floor + ":room:" + room, tacticalRole);
    }

    public static PartyRole partyRole(DungeonLivingRole role) {
        return switch (role) {
            case RIVAL_EXPLORER -> PartyRole.ASSASSIN;
            case RITUALIST -> PartyRole.MAGE;
            case ENGINEER -> PartyRole.TANK;
            default -> PartyRole.ARCHER;
        };
    }

    public static String marker(DungeonLivingRole role) {
        return LIVING_MARKER_PREFIX + role.name();
    }

    public static Optional<DungeonLivingRole> fromMarker(String marker) {
        if (marker == null || !marker.startsWith(LIVING_MARKER_PREFIX)) return Optional.empty();
        try {
            return Optional.of(DungeonLivingRole.valueOf(
                    marker.substring(LIVING_MARKER_PREFIX.length())));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<DungeonLivingRole> role(Mob mob) {
        try {
            return Optional.of(DungeonLivingRole.valueOf(
                    mob.getPersistentData().getString(LIVING_ROLE_TAG)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isNonCombat(Mob mob) {
        return mob.getPersistentData().getBoolean(NON_COMBAT_TAG);
    }
}
