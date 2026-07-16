package tong.statmod.effects;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.perks.AutomaticPerkBonuses;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public final class PlayerAttributeEffects {
    private static final UUID BASE_HEALTH_MODIFIER_ID =
            UUID.fromString("c87338c2-dd37-45db-9981-8bc03c2695eb");
    private static final UUID BASE_DAMAGE_MODIFIER_ID =
            UUID.fromString("64d77f03-c995-45b3-b62e-d0d3bc4636e5");
    private static final UUID BASE_MANA_MODIFIER_ID =
            UUID.fromString("90b127dd-52b8-42d7-97f7-7d6056b7f584");
    private static final ResourceLocation MANA_REGEN_ATTRIBUTE_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_regen");
    private static final UUID MANA_REGEN_MODIFIER_ID =
            UUID.fromString("4192f4b8-0799-4a2c-b32b-9eb5e457b1cb");
    private static final String ENDURANCE_MODIFIER_NAME = "STAT Mod Physical Endurance";
    private static final String RAPIDITE_MODIFIER_NAME = "STAT Mod Rapidité";
    private static final String AGILITY_MODIFIER_NAME = "STAT Mod Agility";

    private PlayerAttributeEffects() {
    }

    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve()
                .orElse(null);
        if (stats == null) {
            return;
        }

        double previousMaxHealth = player.getMaxHealth();
        float previousHealth = player.getHealth();
        AutomaticPerkBonuses bonuses = AutomaticPerkBonuses.from(stats);
        replaceModifier(player, Attributes.MAX_HEALTH, BASE_HEALTH_MODIFIER_ID,
                "STAT Mod Base Health", PlayerBaseBalanceRules.BASE_MAX_HEALTH - 20.0D,
                AttributeModifier.Operation.ADDITION);
        replaceModifier(player, Attributes.ATTACK_DAMAGE, BASE_DAMAGE_MODIFIER_ID,
                "STAT Mod Base Attack Damage", PlayerBaseBalanceRules.BASE_ATTACK_DAMAGE - 1.0D,
                AttributeModifier.Operation.ADDITION);
        replaceModifier(player, MagicAttributeTarget.MAX_MANA.id(), BASE_MANA_MODIFIER_ID,
                "STAT Mod Base Mana", PlayerBaseBalanceRules.BASE_MAX_MANA - 100.0D,
                AttributeModifier.Operation.ADDITION);

        int manaLevel = stats.get(StatType.MANA_POOL).level();
        double manaRegen = PlayerBaseBalanceRules.manaRegenPerSecond(
                manaLevel, PlayerBaseBalanceRules.manaMilestones(manaLevel));
        replaceModifier(player, MANA_REGEN_ATTRIBUTE_ID,
                MANA_REGEN_MODIFIER_ID, "STAT Mod mana regen",
                manaRegen - PlayerBaseBalanceRules.BASE_MANA_REGEN_PER_SECOND,
                AttributeModifier.Operation.ADDITION);
        int level = stats.get(StatType.PHYSICAL_ENDURANCE).level();
        for (StaminaAttributeTarget target : StaminaAttributeTarget.values()) {
            replaceModifier(player, target.id(), target.modifierId(),
                    ENDURANCE_MODIFIER_NAME, target.amount(level, bonuses));
        }
        for (MobilityAttributeTarget target : MobilityAttributeTarget.values()) {
            String name = target.stat() == StatType.RAPIDITE
                    ? RAPIDITE_MODIFIER_NAME : AGILITY_MODIFIER_NAME;
            replaceModifier(player, target.id(), target.modifierId(), name,
                    target.amount(stats, bonuses));
        }
        for (MagicAttributeTarget target : MagicAttributeTarget.values()) {
            replaceModifier(player, target.id(), target.modifierId(),
                    "STAT Mod " + target.stat().id(), target.amount(stats, bonuses));
        }
        preserveHealthRatio(player, previousHealth, previousMaxHealth);
    }

    private static void replaceModifier(ServerPlayer player, ResourceLocation attributeId,
            UUID modifierId, String name, double amount) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return;
        }
        replaceModifier(player, attribute, modifierId, name, amount,
                AttributeModifier.Operation.MULTIPLY_BASE);
    }

    private static void preserveHealthRatio(
            ServerPlayer player, float previousHealth, double previousMaxHealth) {
        double currentMaxHealth = player.getMaxHealth();
        if (!(previousMaxHealth > 0.0D)
                || !Double.isFinite(previousMaxHealth)
                || !Double.isFinite(currentMaxHealth)
                || Math.abs(currentMaxHealth - previousMaxHealth) < 1.0e-6D) {
            return;
        }
        double ratio = previousHealth / previousMaxHealth;
        float scaledHealth = (float) Math.max(0.0D, Math.min(currentMaxHealth, currentMaxHealth * ratio));
        player.setHealth(scaledHealth);
    }

    private static void replaceModifier(ServerPlayer player, ResourceLocation attributeId,
            UUID modifierId, String name, double amount, AttributeModifier.Operation operation) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return;
        }
        replaceModifier(player, attribute, modifierId, name, amount, operation);
    }

    private static void replaceModifier(ServerPlayer player, Attribute attribute,
            UUID modifierId, String name, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(modifierId);
        if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    modifierId,
                    name,
                    amount,
                    operation));
        }
    }
}
