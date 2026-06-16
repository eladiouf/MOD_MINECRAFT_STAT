package tong.statmod.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.client.ClientStatCache;
import tong.statmod.integration.epicfight.EpicFightSkillRequirementResolver;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;

import java.util.Map;

@Mixin(SkillBookScreen.class)
public abstract class EpicFightSkillBookScreenMixin {
    @Shadow @Final protected Player opener;
    @Shadow @Final protected Skill skill;
    @Shadow private Button learnButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void statmod$disableLearnButtonWhenStatsMissing(CallbackInfo ci) {
        if (this.learnButton != null && !hasClientRequirements()) {
            this.learnButton.active = false;
        }
    }

    @Inject(method = "acquireSkillTo", at = @At("HEAD"), cancellable = true)
    private void statmod$preventAcquireWhenStatsMissing(SkillContainer container, CallbackInfo ci) {
        if (!hasClientRequirements()) {
            ci.cancel();
        }
    }

    private boolean hasClientRequirements() {
        if (this.skill == null || this.skill.getRegistryName() == null) {
            return true;
        }

        Map<Integer, Integer> requirements =
                EpicFightSkillRequirementResolver.requirementsFor(this.skill.getRegistryName().getPath());
        if (requirements.isEmpty()) {
            return true;
        }

        return requirements.entrySet().stream()
                .allMatch(entry -> ClientStatCache.getLevel(entry.getKey()) >= entry.getValue());
    }
}
