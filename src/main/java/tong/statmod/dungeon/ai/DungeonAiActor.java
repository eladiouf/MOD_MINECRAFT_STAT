package tong.statmod.dungeon.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

public final class DungeonAiActor {
    public static final String FACTION_TAG = "statmod_ai_faction";
    public static final String ALERT_TAG = "statmod_ai_alert";
    public static final String FLOOR_TAG = "statmod_ai_floor";
    public static final String SQUAD_TAG = "statmod_ai_squad";
    public static final String LAST_SEEN_X = "statmod_ai_last_seen_x";
    public static final String LAST_SEEN_Y = "statmod_ai_last_seen_y";
    public static final String LAST_SEEN_Z = "statmod_ai_last_seen_z";
    public static final String LAST_SEEN_TICK = "statmod_ai_last_seen_tick";
    public static final String TACTICAL_ROLE_TAG = "statmod_ai_tactical_role";

    private DungeonAiActor() {}

    public static void initialize(Mob mob, DungeonFaction faction, int floor, String squadId) {
        initialize(mob, faction, floor, squadId, DungeonTacticalRole.WARDEN);
    }

    public static void initialize(Mob mob, DungeonFaction faction, int floor, String squadId,
                                  DungeonTacticalRole role) {
        CompoundTag data = mob.getPersistentData();
        data.putString(FACTION_TAG, faction.name());
        data.putString(ALERT_TAG, DungeonAlertState.IDLE.name());
        data.putInt(FLOOR_TAG, floor);
        data.putString(SQUAD_TAG, squadId == null ? "" : squadId);
        data.putString(TACTICAL_ROLE_TAG, role.name());
    }

    public static DungeonFaction faction(Mob mob) {
        try {
            return DungeonFaction.valueOf(mob.getPersistentData().getString(FACTION_TAG));
        } catch (IllegalArgumentException ignored) {
            return DungeonFaction.RESTLESS_DEAD;
        }
    }

    public static DungeonAlertState alert(Mob mob) {
        try {
            return DungeonAlertState.valueOf(mob.getPersistentData().getString(ALERT_TAG));
        } catch (IllegalArgumentException ignored) {
            return DungeonAlertState.IDLE;
        }
    }

    public static DungeonTacticalRole tacticalRole(Mob mob) {
        try {
            return DungeonTacticalRole.valueOf(
                    mob.getPersistentData().getString(TACTICAL_ROLE_TAG));
        } catch (IllegalArgumentException ignored) {
            return DungeonTacticalRole.WARDEN;
        }
    }

    public static DungeonFaction factionFor(String entityId) {
        String id = entityId == null ? "" : entityId;
        if (id.startsWith("statmod:adventurer")) return DungeonFaction.ADVENTURER_RIVALS;
        if (id.contains("pyromancer") || id.contains("cultist") || id.contains("blaze")) {
            return DungeonFaction.CULT_OF_CINDERS;
        }
        if (id.contains("cryo") || id.contains("frost") || id.contains("stray")) {
            return DungeonFaction.FROZEN_COVEN;
        }
        if (id.contains("iron_golem") || id.contains("armor_stand") || id.contains("shulker")) {
            return DungeonFaction.DUNGEON_CONSTRUCTS;
        }
        if (id.contains("spider") || id.contains("wolf") || id.contains("beast")) {
            return DungeonFaction.BEAST_PACKS;
        }
        if (id.startsWith("irons_spellbooks:") || id.contains("evoker") || id.contains("witch")) {
            return DungeonFaction.ARCANE_ORDER;
        }
        return DungeonFaction.RESTLESS_DEAD;
    }
}
