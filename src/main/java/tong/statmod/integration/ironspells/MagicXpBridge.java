package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.magic.MagicBranch;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public class MagicXpBridge {

    @SubscribeEvent
    public static void onInscribeSpell(InscribeSpellEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        AbstractSpell spell = event.getSpellData().getSpell();
        if (spell == null) return;

        boolean leveled = false;

        leveled |= RaceEffectApplier.addScaledXp(player, StatType.ARCANE_POWER.index, 1, data, false);

        String spellId = IronSpellsApiAdapter.spellId(spell);
        if (!data.hasLearnedSpell(spellId)) {
            leveled |= RaceEffectApplier.addScaledXp(player, StatType.ERUDITION.index, 2, data, false);
        }

        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }

    @SubscribeEvent
    public static void onPostCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null) return;

        boolean leveled = false;

        MagicBranch branch = IronSpellsApiAdapter.branchOf(spell);
        // Affinités élémentales — chaque branche élémentale donne de l'XP à sa propre affinité.
        // Les 5 branches non-élémentales (Holy/Blood/Ender/Evocation/Eldritch) ne touchent
        // aucune affinité élémentale ; elles renforcent ARCANE_POWER via onInscribeSpell et
        // MANA_POOL via cette même méthode.
        if (branch != null) {
            StatType affinityStat = switch (branch) {
                case FIRE -> StatType.FIRE_AFFINITY;
                case WATER -> StatType.WATER_AFFINITY;
                case AIR -> StatType.AIR_AFFINITY;
                case EARTH -> StatType.EARTH_AFFINITY;
                default -> null;
            };
            if (affinityStat != null) {
                leveled |= RaceEffectApplier.addScaledXp(player, affinityStat.index, 1, data, false);
            }
        }

        double manaCost = event.getManaCost();
        int manaXp = Math.max(1, (int) Math.round(manaCost / 30.0));
        leveled |= RaceEffectApplier.addScaledXp(player, StatType.MANA_POOL.index, manaXp, data, false);

        // CASTING_SPEED gagne sur cast complété (1× par cast), pas sur ModifySpellLevelEvent
        // qui fire en boucle pendant le rendu UI + chaque tick de cast LONG.
        // Amount calé sur la durée du cast pour récompenser les sorts difficiles à canaliser.
        int castTime = spell.getCastTime(event.getSpellLevel());
        int castingSpeedXp = Math.max(1, castTime / 10);
        leveled |= RaceEffectApplier.addScaledXp(player, StatType.CASTING_SPEED.index, castingSpeedXp, data, false);

        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }

    /**
     * @deprecated Plus utilisé pour XP — l'event fire trop souvent (rendu UI, tick de cast).
     *             Subscriber retiré ; CASTING_SPEED XP est maintenant calculé dans {@link
     *             #onPostCast(SpellOnCastEvent)}.
     */
    @Deprecated
    static void __removed_onModifySpellLevel_doNotResurrect(ModifySpellLevelEvent event) {
        // intentionally empty — keeping the marker to discourage re-adding the broken subscriber
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypes.MAGIC) && !event.getSource().is(DamageTypes.INDIRECT_MAGIC)) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);

        int xp = Math.max(1, Math.round(event.getOriginalDamage() / 6.0f));
        boolean leveled = RaceEffectApplier.addScaledXp(player, StatType.MAGIC_RESISTANCE.index, xp, data, false);
        if (leveled) SoundHelper.playLevelUp(player);
        SyncHelper.syncStats(player);
    }
}
