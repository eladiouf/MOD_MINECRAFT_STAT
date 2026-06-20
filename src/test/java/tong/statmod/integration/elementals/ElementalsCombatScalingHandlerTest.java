package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ElementalsCombatScalingHandlerTest {
    @Test
    void resolvesDamageBranchFromElementalEntityPackage() {
        assertEquals(ElementalBranch.FIRE,
                ElementalsCombatScalingHandler.branchFromEntityClassName(
                        "dev.saperate.elementals.entities.fire.FireBallEntity"));
        assertEquals(ElementalBranch.LIGHTNING,
                ElementalsCombatScalingHandler.branchFromEntityClassName(
                        "dev.saperate.elementals.entities.lightning.LightningArcEntity"));
        assertEquals(ElementalBranch.METAL,
                ElementalsCombatScalingHandler.branchFromEntityClassName(
                        "dev.saperate.elementals.entities.metal.MetalBulletEntity"));
    }

    @Test
    void ignoresCommonElementalsEntitiesWithoutElementBranch() {
        assertNull(ElementalsCombatScalingHandler.branchFromEntityClassName(
                "dev.saperate.elementals.entities.common.BoomerangEntity"));
        assertNull(ElementalsCombatScalingHandler.branchFromEntityClassName(
                "dev.saperate.elementals.entities.common.DirtBottleEntity"));
    }

    @Test
    void ignoresNonElementalsEntities() {
        assertNull(ElementalsCombatScalingHandler.branchFromEntityClassName(
                "net.minecraft.world.entity.projectile.Arrow"));
        assertNull(ElementalsCombatScalingHandler.branchFromEntityClassName(null));
    }

    @Test
    void resolvesDamageBranchFromElementalsAbilityPackage() {
        assertEquals(ElementalBranch.FIRE,
                ElementalsCombatScalingHandler.branchFromAbilityClassName(
                        "dev.saperate.elementals.elements.fire.AbilityFlameThrower"));
        assertEquals(ElementalBranch.BLOOD,
                ElementalsCombatScalingHandler.branchFromAbilityClassName(
                        "dev.saperate.elementals.elements.blood.AbilityBloodControl"));
    }

    @Test
    void ignoresNonElementalsAbilities() {
        assertNull(ElementalsCombatScalingHandler.branchFromAbilityClassName(
                "net.minecraft.world.item.SwordItem"));
        assertNull(ElementalsCombatScalingHandler.branchFromAbilityClassName(null));
    }

    @Test
    void resolvesOwnedElementalEntityBranchesFromNearbyClasses() {
        assertEquals(ElementalBranch.AIR,
                ElementalsCombatScalingHandler.branchFromNearbyOwnedElementalClasses(List.of(
                        "dev.saperate.elementals.entities.air.AirBulletEntity")));
        assertEquals(ElementalBranch.WATER,
                ElementalsCombatScalingHandler.branchFromNearbyOwnedElementalClasses(List.of(
                        "dev.saperate.elementals.entities.water.WaterJetEntity",
                        "dev.saperate.elementals.entities.water.WaterBladeEntity")));
    }

    @Test
    void ignoresAmbiguousOrNonElementalNearbyOwnedClasses() {
        assertNull(ElementalsCombatScalingHandler.branchFromNearbyOwnedElementalClasses(List.of(
                "dev.saperate.elementals.entities.fire.FireArcEntity",
                "dev.saperate.elementals.entities.air.AirBulletEntity")));
        assertNull(ElementalsCombatScalingHandler.branchFromNearbyOwnedElementalClasses(List.of(
                "net.minecraft.world.entity.projectile.Arrow",
                "dev.saperate.elementals.entities.common.BoomerangEntity")));
    }

    @Test
    void resolvesContextHintForOwnerDrivenElementalsEntities() {
        ElementalsCombatScalingHandler.ElementalContextHint hint =
                ElementalsCombatScalingHandler.contextHintFromEntityClassName(
                        "dev.saperate.elementals.entities.fire.FireShieldEntity");
        assertEquals(ElementalBranch.FIRE, hint.branch());
        assertEquals(ElementalsCombatScalingHandler.ContextOwnerSource.OWNER, hint.ownerSource());
    }

    @Test
    void resolvesContextHintForCasterDrivenElementalsEntities() {
        ElementalsCombatScalingHandler.ElementalContextHint hint =
                ElementalsCombatScalingHandler.contextHintFromEntityClassName(
                        "dev.saperate.elementals.entities.water.WaterHelmetEntity");
        assertEquals(ElementalBranch.WATER, hint.branch());
        assertEquals(ElementalsCombatScalingHandler.ContextOwnerSource.CASTER, hint.ownerSource());
    }

    @Test
    void ignoresContextHintForUnknownOrCommonEntities() {
        assertNull(ElementalsCombatScalingHandler.contextHintFromEntityClassName(
                "dev.saperate.elementals.entities.common.BoomerangEntity"));
        assertNull(ElementalsCombatScalingHandler.contextHintFromEntityClassName(
                "net.minecraft.world.entity.projectile.Arrow"));
    }
}
