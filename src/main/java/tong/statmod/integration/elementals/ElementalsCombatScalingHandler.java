package tong.statmod.integration.elementals;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
import dev.saperate.elementals.entities.water.WaterHelmetEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ElementalsCombatScalingHandler {
    private ElementalsCombatScalingHandler() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        ElementalDamageContext context = damageContext(event.getSource(), event.getEntity());
        if (context == null) {
            return;
        }

        ElementState state = ElementalsCompat.stateFor(context.player(), context.branch());
        event.setAmount(event.getAmount() * ElementalsPenaltyModel.damageMultiplier(state));
    }

    static ElementalDamageContext damageContext(DamageSource source, LivingEntity target) {
        ElementalBranch branch = damageBranch(source, target);
        Entity owner = source.getEntity();
        if (owner instanceof Player player && branch != null) {
            return new ElementalDamageContext(player, branch);
        }
        if (!allowsNearbyContextFallback(source.getMsgId())) {
            return null;
        }
        return nearbyElementalContext(target);
    }

    static ElementalBranch damageBranch(DamageSource source) {
        return damageBranch(source, null);
    }

    static ElementalBranch damageBranch(DamageSource source, LivingEntity target) {
        Entity direct = source.getDirectEntity();
        ElementalBranch branch = direct == null ? null : branchFromEntityClassName(direct.getClass().getName());
        if (branch != null) {
            return branch;
        }

        Entity owner = source.getEntity();
        if (direct != null && direct == owner && owner instanceof ServerPlayer serverPlayer) {
            ElementalDamageContext nearbyContext = nearbyElementalContext(target);
            if (nearbyContext != null && nearbyContext.player() == owner) {
                return nearbyContext.branch();
            }
            return activeAbilityBranch(serverPlayer);
        }
        return null;
    }

    static ElementalBranch branchFromEntityClassName(String className) {
        if (className == null || !className.startsWith("dev.saperate.elementals.entities.")) {
            return null;
        }

        if (className.contains(".entities.air.")) {
            return ElementalBranch.AIR;
        }
        if (className.contains(".entities.water.")) {
            return ElementalBranch.WATER;
        }
        if (className.contains(".entities.earth.")) {
            return ElementalBranch.EARTH;
        }
        if (className.contains(".entities.fire.")) {
            return ElementalBranch.FIRE;
        }
        if (className.contains(".entities.lightning.")) {
            return ElementalBranch.LIGHTNING;
        }
        if (className.contains(".entities.blood.")) {
            return ElementalBranch.BLOOD;
        }
        if (className.contains(".entities.metal.")) {
            return ElementalBranch.METAL;
        }
        return null;
    }

    static ElementalBranch branchFromAbilityClassName(String className) {
        if (className == null || !className.startsWith("dev.saperate.elementals.elements.")) {
            return null;
        }

        if (className.contains(".elements.air.")) {
            return ElementalBranch.AIR;
        }
        if (className.contains(".elements.water.")) {
            return ElementalBranch.WATER;
        }
        if (className.contains(".elements.earth.")) {
            return ElementalBranch.EARTH;
        }
        if (className.contains(".elements.fire.")) {
            return ElementalBranch.FIRE;
        }
        if (className.contains(".elements.lightning.")) {
            return ElementalBranch.LIGHTNING;
        }
        if (className.contains(".elements.blood.")) {
            return ElementalBranch.BLOOD;
        }
        if (className.contains(".elements.metal.")) {
            return ElementalBranch.METAL;
        }
        return null;
    }

    static ElementalContextHint contextHintFromEntityClassName(String className) {
        ElementalBranch branch = branchFromEntityClassName(className);
        if (branch == null) {
            return null;
        }
        ContextOwnerSource ownerSource = className != null && className.endsWith(".WaterHelmetEntity")
                ? ContextOwnerSource.CASTER
                : ContextOwnerSource.OWNER;
        return new ElementalContextHint(branch, ownerSource);
    }

    static boolean allowsNearbyContextFallback(String damageTypeMsgId) {
        return "drown".equals(damageTypeMsgId) || "inFire".equals(damageTypeMsgId);
    }

    static ElementalBranch branchFromNearbyOwnedElementalClasses(List<String> classNames) {
        List<ElementalBranch> branches = new ArrayList<>();
        for (String className : classNames) {
            ElementalBranch branch = branchFromEntityClassName(className);
            if (branch != null) {
                branches.add(branch);
            }
        }
        return uniqueBranch(branches);
    }

    private static ElementalDamageContext nearbyElementalContext(LivingEntity target) {
        if (target == null) {
            return null;
        }

        AABB targetBox = target.getBoundingBox();
        AABB searchBox = targetBox.inflate(1.0D);
        List<ElementalDamageContext> contexts = target.level().getEntities(target, searchBox, entity ->
                        entity.getBoundingBox().intersects(targetBox.inflate(0.25D)))
                .stream()
                .map(entity -> contextFromNearbyEntity(entity, target))
                .filter(context -> context != null)
                .toList();
        return uniqueContext(contexts);
    }

    private static ElementalBranch uniqueBranch(Collection<ElementalBranch> branches) {
        ElementalBranch resolved = null;
        for (ElementalBranch branch : branches) {
            if (resolved == null) {
                resolved = branch;
                continue;
            }
            if (resolved != branch) {
                return null;
            }
        }
        return resolved;
    }

    private static ElementalDamageContext contextFromNearbyEntity(Entity entity, LivingEntity target) {
        ElementalContextHint hint = contextHintFromEntityClassName(entity.getClass().getName());
        if (hint == null) {
            return null;
        }

        Player player = switch (hint.ownerSource()) {
            case OWNER -> ownerPlayer(entity);
            case CASTER -> casterPlayer(entity, target);
        };
        return player == null ? null : new ElementalDamageContext(player, hint.branch());
    }

    private static Player ownerPlayer(Entity entity) {
        if (entity instanceof AbstractElementalsEntity<?> elemental && elemental.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static Player casterPlayer(Entity entity, LivingEntity target) {
        if (entity instanceof WaterHelmetEntity waterHelmet
                && allowsWaterHelmetCasterFallback(waterHelmet.suffocate, waterHelmet.getOwner() == target)
                && waterHelmet.getCaster() instanceof Player player) {
            return player;
        }
        return null;
    }

    static boolean allowsWaterHelmetCasterFallback(boolean suffocate, boolean ownerMatchesTarget) {
        return suffocate && ownerMatchesTarget;
    }

    private static ElementalDamageContext uniqueContext(Collection<ElementalDamageContext> contexts) {
        ElementalDamageContext resolved = null;
        for (ElementalDamageContext context : contexts) {
            if (resolved == null) {
                resolved = context;
                continue;
            }
            if (resolved.branch() != context.branch() || resolved.player() != context.player()) {
                return null;
            }
        }
        return resolved;
    }

    private static ElementalBranch activeAbilityBranch(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        if (bender == null || bender.currAbility == null) {
            return null;
        }
        return branchFromAbilityClassName(bender.currAbility.getClass().getName());
    }

    record ElementalDamageContext(Player player, ElementalBranch branch) {}

    record ElementalContextHint(ElementalBranch branch, ContextOwnerSource ownerSource) {}

    enum ContextOwnerSource {
        OWNER,
        CASTER
    }
}
