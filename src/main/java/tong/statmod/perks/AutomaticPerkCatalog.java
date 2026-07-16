package tong.statmod.perks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tong.statmod.stats.StatType;

public final class AutomaticPerkCatalog {
    private static final int[] MILESTONES = {25, 50, 75};
    private static final List<AutomaticPerkDefinition> DEFINITIONS;
    private static final Map<String, AutomaticPerkDefinition> BY_ID;

    static {
        List<AutomaticPerkDefinition> definitions = new ArrayList<>();
        addMilestones(definitions, "rapidite", StatType.RAPIDITE,
                AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED, 0.02);
        addMilestones(definitions, "agility", StatType.AGILITY,
                AutomaticPerkEffect.AGILITY_MOVEMENT, 0.02);
        addMilestones(definitions, "physical_endurance", StatType.PHYSICAL_ENDURANCE,
                AutomaticPerkEffect.ENDURANCE_STAMINA, 0.04);
        addMilestones(definitions, "arcane_power", StatType.ARCANE_POWER,
                AutomaticPerkEffect.ARCANE_SPELL_POWER, 0.03);
        addMilestones(definitions, "casting_speed", StatType.CASTING_SPEED,
                AutomaticPerkEffect.CASTING_SPEED_REDUCTIONS, 0.02);
        addMilestones(definitions, "mana_pool", StatType.MANA_POOL,
                AutomaticPerkEffect.MANA_CAPACITY_REGEN, 0.03);
        addMilestones(definitions, "magic_resistance", StatType.MAGIC_RESISTANCE,
                AutomaticPerkEffect.MAGIC_RESISTANCE, 0.02);
        addMilestones(definitions, "brute_force", StatType.BRUTE_FORCE,
                AutomaticPerkEffect.BRUTE_FORCE_DAMAGE, 0.05);
        addMilestones(definitions, "blade_technique", StatType.BLADE_TECHNIQUE,
                AutomaticPerkEffect.BLADE_TECHNIQUE_DAMAGE, 0.05);
        addMilestones(definitions, "precision", StatType.PRECISION,
                AutomaticPerkEffect.PRECISION_DAMAGE, 0.05);
        addMilestones(definitions, "physical_resistance", StatType.PHYSICAL_RESISTANCE,
                AutomaticPerkEffect.PHYSICAL_RESISTANCE, 0.02);
        addMilestones(definitions, "willpower", StatType.WILLPOWER,
                AutomaticPerkEffect.WILLPOWER_KNOCKBACK_RESISTANCE, 0.1);
        addMilestones(definitions, "intimidation", StatType.INTIMIDATION,
                AutomaticPerkEffect.INTIMIDATION_ARMOR_TOUGHNESS, 1.0);
        DEFINITIONS = List.copyOf(definitions);

        LinkedHashMap<String, AutomaticPerkDefinition> byId = new LinkedHashMap<>();
        for (AutomaticPerkDefinition definition : DEFINITIONS) {
            if (byId.put(definition.id(), definition) != null) {
                throw new IllegalStateException("duplicate automatic perk id: " + definition.id());
            }
        }
        BY_ID = Map.copyOf(byId);
    }

    private AutomaticPerkCatalog() {
    }

    public static List<AutomaticPerkDefinition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<AutomaticPerkDefinition> byId(String id) {
        return Optional.ofNullable(id == null ? null : BY_ID.get(id));
    }

    private static void addMilestones(List<AutomaticPerkDefinition> definitions,
            String path, StatType stat, AutomaticPerkEffect effect, double amount) {
        for (int milestone : MILESTONES) {
            definitions.add(new AutomaticPerkDefinition(
                    "statmod:" + path + "_" + milestone,
                    definitions.size(),
                    List.of(new AutomaticPerkRequirement(stat, milestone)),
                    effect,
                    amount));
        }
    }
}
