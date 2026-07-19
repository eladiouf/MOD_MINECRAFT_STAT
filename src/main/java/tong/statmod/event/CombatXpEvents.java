package tong.statmod.event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.progression.xp.CombatEligibility;
import tong.statmod.progression.xp.WeaponClassification;
import tong.statmod.progression.xp.WeaponClassifier;
import tong.statmod.progression.xp.XpAction;
import tong.statmod.progression.xp.XpActionKind;
import tong.statmod.progression.xp.XpAwardService;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class CombatXpEvents {
    private CombatXpEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        float damage = event.getAmount();
        if (!Float.isFinite(damage) || damage <= 0F) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && XpAwardService.isEligible(attacker)
                && CombatEligibility.eligibleTarget(attacker, target)) {
            awardOffense(attacker, target, event, damage);
        }
        if (target instanceof ServerPlayer victim && XpAwardService.isEligible(victim)) {
            awardDefense(victim, event, damage);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shield(ShieldBlockEvent event) {
        float blocked = event.getBlockedDamage();
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !XpAwardService.isEligible(player)
                || !Float.isFinite(blocked) || blocked <= 0F) {
            return;
        }
        XpAction action = withOpponent(
                XpAction.damage(XpActionKind.SHIELD_BLOCKED, blocked),
                event.getDamageSource().getEntity());
        XpAwardService.award(player, List.of(action), player.serverLevel().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Enemy)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !XpAwardService.isEligible(player)
                || !CombatEligibility.eligibleTarget(player, target)) {
            return;
        }
        boolean dangerous = target.getType().is(Tags.EntityTypes.BOSSES)
                || target.getMaxHealth() >= 100F;
        XpAwardService.award(player,
                List.of(XpAction.kill(target.getMaxHealth(), dangerous)),
                player.serverLevel().getGameTime());
    }

    private static void awardOffense(
            ServerPlayer attacker, LivingEntity target, LivingDamageEvent event, float damage) {
        boolean projectile = event.getSource().is(DamageTypeTags.IS_PROJECTILE);
        boolean directMelee = !projectile && event.getSource().getDirectEntity() == attacker;
        WeaponClassification classification =
                WeaponClassifier.classify(attacker.getMainHandItem(), projectile);
        List<XpAction> actions = new ArrayList<>();
        if (classification == WeaponClassification.PRECISION) {
            actions.add(XpAction.damage(XpActionKind.PROJECTILE, damage));
        } else if (directMelee && classification == WeaponClassification.HEAVY) {
            actions.add(XpAction.damage(XpActionKind.MELEE_HEAVY, damage));
        } else if (directMelee && classification == WeaponClassification.BLADE) {
            actions.add(XpAction.damage(XpActionKind.MELEE_BLADE, damage));
        }

        if (directMelee) {
            attacker.getCapability(StatCapabilities.PLAYER_XP_STATE).ifPresent(state -> {
                int combo = state.recordMeleeHit(attacker.serverLevel().getGameTime());
                if (combo >= 3) {
                    actions.add(XpAction.combo(combo));
                }
            });
        }
        if (!actions.isEmpty()) {
            UUID opponent = target instanceof Player ? target.getUUID() : null;
            List<XpAction> attributed = actions.stream()
                    .map(action -> action.withOpponent(opponent)).toList();
            XpAwardService.award(attacker, attributed, attacker.serverLevel().getGameTime());
        }
    }

    private static void awardDefense(
            ServerPlayer victim, LivingDamageEvent event, float damage) {
        long tick = victim.serverLevel().getGameTime();
        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
            victim.getCapability(StatCapabilities.PLAYER_XP_STATE)
                    .ifPresent(state -> state.markFallDamage());
        }
        if (!CombatEligibility.physicalProfile(event.getSource(), victim)) {
            return;
        }
        List<XpAction> actions = new ArrayList<>();
        actions.add(withOpponent(
                XpAction.damage(XpActionKind.PHYSICAL_DAMAGE_RECEIVED, damage),
                event.getSource().getEntity()));
        float remainingHealth = victim.getHealth() - damage;
        if (remainingHealth > 0F
                && remainingHealth / victim.getMaxHealth() <= 0.30F) {
            victim.getCapability(StatCapabilities.PLAYER_XP_STATE).ifPresent(state -> {
                if (state.tryWillpower(tick)) {
                    actions.add(withOpponent(
                            XpAction.damage(XpActionKind.WILLPOWER_SURVIVAL, damage),
                            event.getSource().getEntity()));
                }
            });
        }
        XpAwardService.award(victim, actions, tick);
    }

    private static XpAction withOpponent(XpAction action, Object responsible) {
        return responsible instanceof Player player
                ? action.withOpponent(player.getUUID())
                : action;
    }
}
