package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.progression.xp.XpAction;
import tong.statmod.progression.xp.XpAwardService;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IronSpellXpEvents {
    private IronSpellXpEvents() {
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !XpAwardService.isEligible(player)
                || event.getCastSource() != CastSource.SPELLBOOK
                || event.getSpellId() == null
                || event.getSpellId().isBlank()
                || event.getOriginalSpellLevel() <= 0) {
            return;
        }

        XpAwardService.award(
                player,
                List.of(XpAction.spellCast(
                        event.getOriginalSpellLevel(),
                        event.getOriginalManaCost())),
                player.serverLevel().getGameTime());
    }
}
