package tong.statmod.integration.ironspells.bridge;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.STATMod;
import tong.statmod.integration.tensura.TensuraSkillIds;

import java.util.Optional;

/**
 * Branche la mastery Tensura ({@code ManasSkillInstance.getMastery() / getMaxMastery()}) sur
 * le niveau de sort affiché par Iron's via {@link ModifySpellLevelEvent}.
 *
 * <p>Mapping : mastery 0 → Lv 1, mastery max → Lv {@link TensuraDelegatingSpell#MAX_LEVEL}.
 * Strictement informatif côté Iron's — le scaling damage réel reste piloté par Tensura natif
 * au moment du {@code onRelease}, qui lit directement l'instance du joueur (et donc sa vraie
 * mastery). On ne duplique pas la logique de scaling, juste l'affichage.
 */
public final class TensuraSpellLevelModifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);

    private TensuraSpellLevelModifier() {}

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        AbstractSpell spell = event.getSpell();
        if (!(spell instanceof TensuraDelegatingSpell wrapper)) return;
        LivingEntity caster = event.getEntity();
        if (caster == null) return;
        if (!ModList.get().isLoaded("tensura")) return;

        int level = computeLevelFromMastery(wrapper, caster);
        if (level > 0) {
            event.setLevel(level);
        }
    }

    static int computeLevelFromMastery(TensuraDelegatingSpell wrapper, LivingEntity caster) {
        try {
            SkillStorage storage = SkillAPI.getSkillsFrom(caster);
            if (storage == null) return 0;
            ResourceLocation canonical = ResourceLocation.parse(
                    TensuraSkillIds.canonicalize(wrapper.tensuraSkillId()));
            Optional<ManasSkillInstance> resolved = storage.getSkill(canonical);
            if (resolved.isEmpty()) return 0;
            ManasSkillInstance instance = resolved.get();
            double mastery = instance.getMastery();
            int maxMastery = Math.max(1, instance.getMaxMastery());
            double ratio = Math.min(1.0, Math.max(0.0, mastery / maxMastery));
            int maxLevel = wrapper.getMaxLevel();
            return Math.max(1, (int) Math.floor(1 + ratio * (maxLevel - 1)));
        } catch (Throwable t) {
            LOGGER.debug("TensuraSpellLevelModifier failed for {}: {}",
                    wrapper.tensuraSkillId(), t.toString());
            return 0;
        }
    }
}
