package tong.statmod.integration.overgeared;

import tong.statmod.integration.overgeared.OvergearedRecipeGate.MaterialGate;
import tong.statmod.magic.MagicRace;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

/**
 * Mission M5 — Phase ζ (Race + Perk Crossover).
 *
 * <p>Calcule les bonus/discounts appliqués aux gates de forge en fonction de la
 * {@link MagicRace} du joueur et de ses perks FORGE_* unlockés.
 *
 * <ul>
 *   <li>DWARF : -3 FORGING requis sur toutes les gates (natural forgers)</li>
 *   <li>ELF   : +25% durability sur arme assemblée (voir {@link #durabilityMultiplier})</li>
 *   <li>HUMAN : +1 quality tier chance (versatile) — appliqué en Phase δ, non implémenté ici</li>
 *   <li>BEAST : +5% chance double output — appliqué en Phase δ, non implémenté ici</li>
 * </ul>
 *
 * <p>Perks :
 * <ul>
 *   <li>FORGE_CORE      → -1 FORGING requis</li>
 *   <li>FORGE_MASTERY   → +25% durability</li>
 *   <li>FORGE_TRANSCENDENCE → -3 FORGING requis (cumulable avec DWARF pour un total de -6)</li>
 * </ul>
 */
public final class ForgingRaceBonus {

    private ForgingRaceBonus() {}

    /** Retourne le discount total sur le niveau FORGING requis. */
    public static int forgingLevelDiscount(PlayerStatData data) {
        if (data == null) return 0;
        int discount = 0;
        MagicRace race = data.getMagicRace();
        if (race == MagicRace.DWARF) discount += 3;
        if (data.isPerkUnlocked(Perk.FORGE_CORE.id)) discount += 1;
        if (data.isPerkUnlocked(Perk.FORGE_TRANSCENDENCE.id)) discount += 3;
        return discount;
    }

    /** Multiplier appliqué au max_damage de l'arme assemblée. 1.0 = pas de bonus. */
    public static float durabilityMultiplier(PlayerStatData data) {
        if (data == null) return 1.0f;
        float bonus = 1.0f;
        MagicRace race = data.getMagicRace();
        if (race == MagicRace.ELF) bonus += 0.25f;
        if (data.isPerkUnlocked(Perk.FORGE_MASTERY.id)) bonus += 0.25f;
        return bonus;
    }

    /**
     * Applique le discount FORGING au gate de matériau. ERUDITION et ARCANE_POWER
     * restent inchangés (la magie ne se compense pas par la race).
     */
    public static MaterialGate applyBonuses(MaterialGate original, PlayerStatData data) {
        if (original == null) return null;
        int discount = forgingLevelDiscount(data);
        if (discount <= 0) return original;
        return new MaterialGate(
                Math.max(0, original.forging() - discount),
                original.erudition(),
                original.arcanePower()
        );
    }
}
