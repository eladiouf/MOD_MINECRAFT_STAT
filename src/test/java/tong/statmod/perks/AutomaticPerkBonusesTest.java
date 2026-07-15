package tong.statmod.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

class AutomaticPerkBonusesTest {
    @BeforeAll
    static void loadDefaults() {
        CommentedConfig config = CommentedConfig.inMemory();
        StatModServerConfig.SPEC.correct(config);
        StatModServerConfig.SPEC.setConfig(config);
    }

    @Test
    void accumulatesOnlyReachedRapiditeMilestones() {
        assertEquals(0.0, bonusesAt(StatType.RAPIDITE, 24)
                .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED));
        assertEquals(0.02, bonusesAt(StatType.RAPIDITE, 25)
                .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED), 1.0e-9);
        assertEquals(0.04, bonusesAt(StatType.RAPIDITE, 50)
                .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED), 1.0e-9);
        assertEquals(0.06, bonusesAt(StatType.RAPIDITE, 75)
                .amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED), 1.0e-9);
    }

    @Test
    void keepsEffectsSeparateAndUsesEachConfiguredDefault() {
        PlayerStats stats = new PlayerStats();
        for (StatType type : new StatType[] {StatType.RAPIDITE, StatType.AGILITY,
                StatType.PHYSICAL_ENDURANCE, StatType.ARCANE_POWER,
                StatType.CASTING_SPEED, StatType.MANA_POOL,
                StatType.MAGIC_RESISTANCE}) {
            stats.setLevel(type, 25);
        }

        AutomaticPerkBonuses bonuses = AutomaticPerkBonuses.from(stats);
        assertEquals(0.02, bonuses.amount(AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED), 1.0e-9);
        assertEquals(0.02, bonuses.amount(AutomaticPerkEffect.AGILITY_MOVEMENT), 1.0e-9);
        assertEquals(0.04, bonuses.amount(AutomaticPerkEffect.ENDURANCE_STAMINA), 1.0e-9);
        assertEquals(0.03, bonuses.amount(AutomaticPerkEffect.ARCANE_SPELL_POWER), 1.0e-9);
        assertEquals(0.02, bonuses.amount(AutomaticPerkEffect.CASTING_SPEED_REDUCTIONS), 1.0e-9);
        assertEquals(0.03, bonuses.amount(AutomaticPerkEffect.MANA_CAPACITY_REGEN), 1.0e-9);
        assertEquals(0.02, bonuses.amount(AutomaticPerkEffect.MAGIC_RESISTANCE), 1.0e-9);
    }

    private static AutomaticPerkBonuses bonusesAt(StatType type, int level) {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(type, level);
        return AutomaticPerkBonuses.from(stats);
    }
}
