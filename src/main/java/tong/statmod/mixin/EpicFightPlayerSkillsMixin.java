package tong.statmod.mixin;

import net.neoforged.bus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import yesman.epicfight.world.capabilities.skill.PlayerSkills;

@Mixin(PlayerSkills.class)
public abstract class EpicFightPlayerSkillsMixin {
    public Event fireSkillEvents(String modId, Event event) {
        return event;
    }
}
