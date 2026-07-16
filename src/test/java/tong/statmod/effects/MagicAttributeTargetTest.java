package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.perks.AutomaticPerkBonuses;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

class MagicAttributeTargetTest {
    @Test
    void exposesExactIronRegistryAndStatMappings() {
        Map<String, StatType> expected = Map.of(
                "irons_spellbooks:spell_power", StatType.ARCANE_POWER,
                "irons_spellbooks:cast_time_reduction", StatType.CASTING_SPEED,
                "irons_spellbooks:cooldown_reduction", StatType.CASTING_SPEED,
                "irons_spellbooks:max_mana", StatType.MANA_POOL,
                "irons_spellbooks:spell_resist", StatType.MAGIC_RESISTANCE);

        assertEquals(expected, Arrays.stream(MagicAttributeTarget.values())
                .collect(Collectors.toMap(
                        target -> target.id().toString(), MagicAttributeTarget::stat)));
    }

    @Test
    void givesEveryTargetAUniqueStableUuid() {
        assertEquals(5, Arrays.stream(MagicAttributeTarget.values())
                .map(MagicAttributeTarget::modifierId)
                .distinct()
                .count());
    }

    @Test
    void modifierUuidsAreUniqueAcrossEveryAttributeBridge() {
        assertEquals(13, Stream.of(
                        Arrays.stream(MagicAttributeTarget.values())
                                .map(MagicAttributeTarget::modifierId),
                        Arrays.stream(MobilityAttributeTarget.values())
                                .map(MobilityAttributeTarget::modifierId),
                        Arrays.stream(StaminaAttributeTarget.values())
                                .map(StaminaAttributeTarget::modifierId))
                .flatMap(stream -> stream)
                .distinct()
                .count());
    }

    @Test
    void scalesFromZeroToConfiguredMaximum() {
        CommentedConfig config = CommentedConfig.inMemory();
        StatModServerConfig.SPEC.correct(config);
        StatModServerConfig.SPEC.setConfig(config);
        PlayerStats stats = new PlayerStats();
        assertEquals(0.0, MagicAttributeTarget.SPELL_POWER.amount(stats));

        stats.setLevel(StatType.ARCANE_POWER, 50);
        assertEquals(0.5, MagicAttributeTarget.SPELL_POWER.amount(stats));

        stats.setLevel(StatType.ARCANE_POWER, 100);
        assertEquals(1.0, MagicAttributeTarget.SPELL_POWER.amount(stats));
    }

    @Test
    void derivesMaxManaMultiplierFromTheAbsoluteCappedRule() {
        CommentedConfig config = CommentedConfig.inMemory();
        StatModServerConfig.SPEC.correct(config);
        StatModServerConfig.SPEC.setConfig(config);
        PlayerStats stats = new PlayerStats();
        AutomaticPerkBonuses none = AutomaticPerkBonuses.from(stats);
        assertEquals(0.0D, MagicAttributeTarget.MAX_MANA.amount(stats, none), 1.0e-9);

        stats.setLevel(StatType.MANA_POOL, 100);
        AutomaticPerkBonuses all = AutomaticPerkBonuses.from(stats);
        assertEquals(2.0D, MagicAttributeTarget.MAX_MANA.amount(stats, all), 1.0e-9);
    }

    @Test
    void assignsEveryApprovedBonusKindOnce() {
        for (MagicAttributeTarget.BonusKind kind : MagicAttributeTarget.BonusKind.values()) {
            assertEquals(1, Arrays.stream(MagicAttributeTarget.values())
                    .filter(target -> target.bonusKind() == kind)
                    .count());
        }
    }
}
