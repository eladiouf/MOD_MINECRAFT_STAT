package tong.statmod.perks;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerkFeedbackMessageFactoryTest {
    @Test
    void formatsLevelFailureWithExactStatNumbers() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(Perk.BRUTE_CORE.stat.index, 8);

        PerkFeedbackMessageFactory.PerkFeedbackMessage message =
                PerkFeedbackMessageFactory.forFailure(Perk.BRUTE_CORE, PerkManager.UnlockFailure.LEVEL_TOO_LOW, data, null);

        assertEquals("Heavy Hitter", message.title().getString());
        assertEquals("Brute Force 8/10 required", message.message().getString());
    }

    @Test
    void formatsPointFailureWithExactPointNumbers() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(Perk.BRUTE_CORE.stat.index, 10);
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 0);

        PerkFeedbackMessageFactory.PerkFeedbackMessage message =
                PerkFeedbackMessageFactory.forFailure(Perk.BRUTE_CORE, PerkManager.UnlockFailure.NOT_ENOUGH_POINTS, data, null);

        assertEquals("Heavy Hitter", message.title().getString());
        assertEquals("Family points 0/1 required", message.message().getString());
    }

    @Test
    void formatsSynergyFailureWithExactSynergyNumbers() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(Perk.BRUTE_SYNERGY.stat.index, 40);
        data.setPerkPoints(Perk.BRUTE_SYNERGY.stat.index, 2);
        data.setLevel(Perk.BRUTE_SYNERGY.synergyStat.index, 20);

        PerkFeedbackMessageFactory.PerkFeedbackMessage message =
                PerkFeedbackMessageFactory.forFailure(Perk.BRUTE_SYNERGY, PerkManager.UnlockFailure.SYNERGY_TOO_LOW, data, null);

        assertEquals("Crushing Force", message.title().getString());
        assertEquals("Physical Endurance 20/40 required", message.message().getString());
    }

    @Test
    void formatsExternalRequirementWithRaceAndSkills() {
        PlayerStatData data = new PlayerStatData();

        PerkFeedbackMessageFactory.PerkFeedbackMessage message =
                PerkFeedbackMessageFactory.forFailure(Perk.BRUTE_TRANSCENDENCE, PerkManager.UnlockFailure.EXTERNAL_REQUIREMENT, data, null);

        assertEquals("Titan's Wrath", message.title().getString());
        assertEquals("Requires Giant race and Giant Strength, Berserk", message.message().getString());
    }
}
