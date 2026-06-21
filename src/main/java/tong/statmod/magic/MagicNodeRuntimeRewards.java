package tong.statmod.magic;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import tong.statmod.integration.tensura.TensuraSkillIds;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MagicNodeRuntimeRewards {
    @FunctionalInterface
    public interface TensuraGrantSink {
        boolean grant(String skillId);
    }

    public record GrantSummary(int tensuraGranted, int ignored) {}

    private MagicNodeRuntimeRewards() {}

    public static GrantSummary apply(Iterable<String> spellIds, TensuraGrantSink sink) {
        if (spellIds == null) {
            return new GrantSummary(0, 0);
        }

        int tensuraGranted = 0;
        int ignored = 0;
        Set<String> distinct = new LinkedHashSet<>();
        for (String spellId : spellIds) {
            if (spellId == null || !distinct.add(spellId)) {
                continue;
            }
            if (spellId.startsWith("tensura:")) {
                if (sink != null && sink.grant(TensuraSkillIds.canonicalize(spellId))) {
                    tensuraGranted++;
                }
            } else {
                ignored++;
            }
        }
        return new GrantSummary(tensuraGranted, ignored);
    }

    public static GrantSummary apply(Player player, Iterable<String> spellIds) {
        if (player == null || !ModList.get().isLoaded("tensura")) {
            return apply(spellIds, null);
        }
        return apply(spellIds, skillId -> SkillAPI.getSkillsFrom(player).learnSkill(ResourceLocation.parse(skillId)));
    }
}
