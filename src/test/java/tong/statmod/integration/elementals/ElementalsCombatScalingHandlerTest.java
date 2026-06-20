package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

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
}
