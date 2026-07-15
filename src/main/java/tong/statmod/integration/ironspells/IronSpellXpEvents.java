package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
                || event.getCastSource() != CastSource.SPELLBOOK
                || event.getSpellId() == null
                || event.getSpellId().isBlank()
                || event.getOriginalSpellLevel() <= 0) {
            return;
        }

        XpAwardService.awardSpellCast(
                player,
                XpAction.spellCast(
                        event.getOriginalSpellLevel(),
                        event.getOriginalManaCost()),
                player.serverLevel().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellInscribed(InscribeSpellEvent event) {
        SpellData data = event.getSpellData();
        if (event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || data == null
                || data.getSpell() == null
                || data.getRarity() == null
                || data.getLevel() <= 0) {
            return;
        }
        XpAwardService.award(
                player,
                List.of(XpAction.spellInscribed(
                        data.getLevel(), data.getRarity().getValue())),
                player.serverLevel().getGameTime());
    }

    @SubscribeEvent
    public static void onSpellDamageReceived(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getSource() instanceof SpellDamageSource)
                || !Float.isFinite(event.getAmount())
                || event.getAmount() <= 0F) {
            return;
        }
        XpAction action = XpAction.magicDamageReceived(event.getAmount());
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && attacker != player) {
            action = action.withOpponent(attacker.getUUID());
        }
        XpAwardService.award(
                player, List.of(action), player.serverLevel().getGameTime());
    }
}
