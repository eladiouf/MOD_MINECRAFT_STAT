package tong.statmod.dungeon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import tong.statmod.STATMod;

/**
 * Discipline des mobs du Trial Dungeon (feedback playtest 2026-07-09).
 *
 * <p>Le pool multi-mods mélange des espèces naturellement hostiles entre elles (piglins vs wither
 * squelettes, flèches de squelette perdues qui déclenchent des représailles…) → les mobs de la
 * vague <b>s'entretuaient</b> et l'étage se vidait sans le joueur. Deux verrous :
 * <ul>
 *   <li>{@link LivingChangeTargetEvent} : un mob autorisé du donjon ne peut jamais PRENDRE POUR
 *       CIBLE un autre mob autorisé — leur seule cible légitime est le joueur.</li>
 *   <li>{@link LivingIncomingDamageEvent} : les dégâts entre deux mobs autorisés sont annulés
 *       (flèche perdue, AoE, sorts…) — pas de friendly fire dans la vague.</li>
 * </ul>
 * Les dégâts environnementaux (pièges, chute, feu) et ceux du joueur passent normalement.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonMobDiscipline {

    private DungeonMobDiscipline() {}

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) return;
        if (!mob.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;
        if (isDungeonMob(mob) && isDungeonMob(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;
        if (!isDungeonMob(victim)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity le && isDungeonMob(le)) {
            event.setCanceled(true);
        }
    }

    private static boolean isDungeonMob(Entity e) {
        return e.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG);
    }
}
