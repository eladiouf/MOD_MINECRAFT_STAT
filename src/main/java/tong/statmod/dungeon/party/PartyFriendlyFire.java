package tong.statmod.dungeon.party;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

/**
 * Anti friendly-fire du groupe d'aventuriers : deux membres du groupe ne se blessent JAMAIS entre
 * eux et ne se prennent jamais pour cible. Couvre mêlée, flèches, crocs, invocations, et
 * <b>sorts Iron's</b> (on remonte au PROPRIÉTAIRE du projectile/sort, pas seulement à l'entité
 * directe). Fonctionne dans toutes les dimensions. Les dégâts au/du joueur passent normalement.
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class PartyFriendlyFire {

    private PartyFriendlyFire() {}

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (isPartyMember(event.getEntity()) && isPartySource(event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (isPartyMember(event.getEntity()) && isPartySource(event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && isPartyMember(event.getEntity()) && isPartyMember(target)) {
            event.setCanceled(true);
        }
    }

    /** Vrai si les dégâts proviennent (directement ou via projectile/invocation) d'un membre du groupe. */
    private static boolean isPartySource(DamageSource src) {
        return isPartyOwned(src.getEntity()) || isPartyOwned(src.getDirectEntity());
    }

    private static boolean isPartyOwned(Entity e) {
        if (e == null) return false;
        if (isPartyMember(e)) return true;
        if (e instanceof Projectile p && p.getOwner() != null && isPartyMember(p.getOwner())) return true;
        if (e instanceof OwnableEntity o && o.getOwner() != null && isPartyMember(o.getOwner())) return true;
        return false;
    }

    private static boolean isPartyMember(Entity e) {
        return e.getPersistentData().contains(PartyRole.TAG);
    }
}
