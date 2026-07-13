package tong.statmod.dungeon;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import tong.statmod.STATMod;
import tong.statmod.config.Config;

/**
 * Boost des dégâts de MÊLÉE des joueurs dans le Trial Dungeon (2026-07-13).
 *
 * <p>Les mobs du donjon ont des PV très gonflés (L2 Hostility par étage + pools de mobs élites) :
 * sans compensation, la mêlée devient anecdotique face aux sorts. Ce handler multiplie les coups
 * physiques directs du joueur par {@code trial_dungeon.meleeDamageMultiplier} (~×3 par défaut),
 * uniquement dans la dimension, jamais contre un autre joueur, jamais sur les projectiles/sorts
 * (la magie a son propre équilibrage via l'arbre unifié).
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonMeleeBoost {

    private DungeonMeleeBoost() {}

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide) return;
        if (!attacker.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        DamageSource source = event.getSource();
        boolean directHit = source.getDirectEntity() == attacker;
        boolean physical = isPhysicalRole(source);
        boolean targetIsPlayer = event.getEntity() instanceof Player;

        float mult = multiplierFor(true, directHit, physical, targetIsPlayer,
                Config.getDungeonMeleeDamageMultiplier());
        if (mult != 1.0f) {
            event.setNewDamage(event.getNewDamage() * mult);
        }
    }

    /** Même définition du rôle physique que {@code StatEffectApplier.damageRole}. */
    private static boolean isPhysicalRole(DamageSource source) {
        return tong.statmod.stats.StatCombatScaling.damageRole(
                source.is(DamageTypes.MAGIC),
                source.is(DamageTypes.INDIRECT_MAGIC),
                source.getMsgId()) == tong.statmod.stats.StatCombatScaling.IncomingDamageRole.PHYSICAL;
    }

    /**
     * Cœur pur (testable) : multiplicateur à appliquer.
     *
     * @param inDungeon      la victime est dans la dimension du donjon
     * @param directHit      le joueur est l'entité directe de la source (coup de mêlée, pas un projectile/sort)
     * @param physicalRole   le rôle du dégât est PHYSICAL (pas magie/statut)
     * @param targetIsPlayer la victime est un joueur (jamais boosté)
     * @param configMult     {@code trial_dungeon.meleeDamageMultiplier}
     */
    static float multiplierFor(boolean inDungeon, boolean directHit, boolean physicalRole,
                               boolean targetIsPlayer, double configMult) {
        if (!inDungeon || !directHit || !physicalRole || targetIsPlayer) {
            return 1.0f;
        }
        return (float) Math.max(1.0, configMult);
    }
}
