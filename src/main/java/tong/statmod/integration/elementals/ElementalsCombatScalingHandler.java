package tong.statmod.integration.elementals;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
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
        DamageSource source = event.getSource();
        ElementalBranch damageBranch = damageBranch(source, event.getEntity());
        Entity owner = source.getEntity();
        if (!(owner instanceof Player player) || damageBranch == null) {
            return;
        }

        ElementState state = ElementalsCompat.stateFor(player, damageBranch);
        event.setAmount(event.getAmount() * ElementalsPenaltyModel.damageMultiplier(state));
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
            ElementalBranch nearbyOwnedEntityBranch = nearbyOwnedElementalBranch(target, owner);
            if (nearbyOwnedEntityBranch != null) {
                return nearbyOwnedEntityBranch;
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

    private static ElementalBranch nearbyOwnedElementalBranch(LivingEntity target, Entity owner) {
        if (target == null) {
            return null;
        }

        AABB targetBox = target.getBoundingBox();
        AABB searchBox = targetBox.inflate(1.0D);
        List<String> nearbyClassNames = target.level().getEntities(target, searchBox, entity ->
                        entity instanceof AbstractElementalsEntity<?> elemental
                                && elemental.getOwner() == owner
                                && entity.getBoundingBox().intersects(targetBox.inflate(0.25D)))
                .stream()
                .map(entity -> entity.getClass().getName())
                .toList();
        return branchFromNearbyOwnedElementalClasses(nearbyClassNames);
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

    private static ElementalBranch activeAbilityBranch(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        if (bender == null || bender.currAbility == null) {
            return null;
        }
        return branchFromAbilityClassName(bender.currAbility.getClass().getName());
    }
}
