package tong.statmod.integration.epicfight;

import yesman.epicfight.skill.Skill;
import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class EpicFightCooldownHandler {
    private EpicFightCooldownHandler() {}

    public static float resourceMultiplier(Player player, Skill.Resource resource) {
        int rapidite = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.RAPIDITE.index);
        int casting = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.CASTING_SPEED.index);
        int agility = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index);
        return resourceMultiplier(rapidite, casting, agility, resource);
    }

    public static float resourceMultiplier(int rapidite, int casting, int agility, Skill.Resource resource) {
        if (resource == null) {
            return 1.0f;
        }
        float reduction = 0.0f;
        switch (resource) {
            case COOLDOWN -> reduction = rapidite * 0.003f + casting * 0.002f;
            case STAMINA -> reduction = agility * 0.0025f + rapidite * 0.0015f;
            case WEAPON_CHARGE -> reduction = agility * 0.002f;
            case HEALTH -> reduction = 0.0f;
            case NONE -> reduction = 0.0f;
        }

        return Math.max(0.5f, 1.0f - reduction);
    }
}
