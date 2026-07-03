package tong.statmod.integration.overgeared;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tong.statmod.integration.overgeared.OvergearedRecipeGate.MaterialGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Mission M5 — Phase γ tests.
 *
 * <p>Garde-fous sur la détection du matériau d'une arme externe + l'application de la
 * gate FORGING/ERUDITION/ARCANE_POWER au craft.
 */
class WeaponMaterialDetectorTest {

    @Test
    void detectsBasicMaterials() {
        assertEquals("iron",     WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:iron_cutlass")));
        assertEquals("gold",     WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:gold_katana")));
        assertEquals("diamond",  WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:diamond_rapier")));
        assertEquals("netherite",WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:netherite_spear")));
    }

    @Test
    void detectsMagisteelVariants_specificFirst() {
        assertEquals("high_magisteel",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("tensura:high_magisteel_axe")));
        assertEquals("pure_magisteel",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("tensura:pure_magisteel_sword")));
        assertEquals("low_magisteel",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("tensura:low_magisteel_dagger")));
        assertEquals("magisteel",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("tensura:magisteel_katana")));
    }

    @Test
    void detectsHihiirokane() {
        assertEquals("hihiirokane",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("tensura:hihiirokane_katana")));
    }

    @Test
    void detectsAliasMaterials() {
        assertEquals("arcane",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:runic_rapier")));
        assertEquals("high_magisteel",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:soulkeeper_sword")));
        assertEquals("adamantite",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:watcher_claymore")));
        assertEquals("orichalcum",
                WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:ancient_blade")));
    }

    @Test
    void returnsNullForUnknownPath() {
        assertNull(WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("foo:unicorn_horn")));
        assertNull(WeaponMaterialDetector.detectMaterial(ResourceLocation.parse("simplyswords:lore_book")));
    }

    @Test
    void gateForWeapon_returnsExpectedGate() {
        MaterialGate gate = OvergearedRecipeGate.gateForWeapon(
                ResourceLocation.parse("tensura:hihiirokane_katana"));
        assertNotNull(gate);
        assertEquals(70, gate.forging());
        assertEquals(30, gate.erudition());
        assertEquals(22, gate.arcanePower());
    }

    @Test
    void gateForWeapon_aliasResolves() {
        // runic_rapier → arcane → FORGING 15, ERUDITION 5
        MaterialGate gate = OvergearedRecipeGate.gateForWeapon(
                ResourceLocation.parse("simplyswords:runic_rapier"));
        assertNotNull(gate);
        assertEquals(15, gate.forging());
        assertEquals(5, gate.erudition());
    }

    @Test
    void gateForWeapon_unknown_returnsNull() {
        assertNull(OvergearedRecipeGate.gateForWeapon(ResourceLocation.parse("foo:bar")));
    }

    @Test
    void gateForWeapon_ironLowTier() {
        MaterialGate gate = OvergearedRecipeGate.gateForWeapon(
                ResourceLocation.parse("simplyswords:iron_cutlass"));
        assertNotNull(gate);
        assertEquals(5, gate.forging());
        assertEquals(0, gate.erudition());
    }

    @Test
    void gateForMaterial_byName() {
        assertEquals(70, OvergearedRecipeGate.gateForMaterial("hihiirokane").forging());
        assertEquals(15, OvergearedRecipeGate.gateForMaterial("steel").forging());
        assertEquals(0, OvergearedRecipeGate.gateForMaterial("wood").forging());
        assertNull(OvergearedRecipeGate.gateForMaterial("unknown"));
        assertNull(OvergearedRecipeGate.gateForMaterial(null));
    }
}
