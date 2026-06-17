package yesman.epicfight.api.neoevent.playerpatch;

import net.minecraft.nbt.CompoundTag;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class SkillCastEvent extends yesman.epicfight.api.event.types.player.SkillCastEvent {
    public SkillCastEvent(PlayerPatch<?> playerPatch, SkillContainer skillContainer, CompoundTag arguments) {
        super(playerPatch, skillContainer, arguments);
    }
}
