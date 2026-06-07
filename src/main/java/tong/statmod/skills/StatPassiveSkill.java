package tong.statmod.skills;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.passive.PassiveSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import tong.statmod.stats.StatType;

public class StatPassiveSkill extends PassiveSkill {

    private final UUID passiveUuid;
    private final StatType stat;
    private final int tier;

    public StatPassiveSkill(SkillBuilder<? extends PassiveSkill> builder, StatType stat, int tier) {
        super(builder);
        this.stat = stat;
        this.tier = tier;
        this.passiveUuid = UUID.nameUUIDFromBytes(
            ("statmod:passive:" + stat.name() + ":tier" + tier).getBytes());
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        applyEffect(playerPatch.getOriginal());
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        if (!(container.getExecutor() instanceof ServerPlayerPatch playerPatch)) return;
        removeEffect(playerPatch.getOriginal());
    }

    void applyEffect(ServerPlayer player) {
        switch (stat) {
            // --- COMBAT ---
            case BRUTE_FORCE         -> applyBruteForce(player);
            case BLADE_TECHNIQUE     -> applyBladeTechnique(player);
            case RAPIDITE            -> applyRapidite(player);
            case AGILITY             -> applyAgility(player);
            case PHYSICAL_RESISTANCE -> applyPhysicalResistance(player);
            case PHYSICAL_ENDURANCE  -> applyPhysicalEndurance(player);
            case PRECISION           -> { /* marker — arrow bonus in StatEffectApplier */ }

            // --- MAGIC ---
            case ARCANE_POWER        -> { /* marker — magic dmg in StatEffectApplier.onLivingHurt */ }
            case WATER_AFFINITY      -> { /* marker — drowning resist in StatEffectApplier */ }
            case EARTH_AFFINITY      -> { /* marker — fall reduction in StatEffectApplier */ }
            case FIRE_AFFINITY       -> { /* marker — fire dmg in StatEffectApplier */ }
            case AIR_AFFINITY        -> applyAirAffinity(player);
            case MAGIC_RESISTANCE    -> applyMagicResistance(player);
            case CASTING_SPEED       -> { /* marker — Epic Fight cooldown reduction */ }
            case MANA_POOL           -> { /* handled by ManaTickHandler */ }
            case ERUDITION           -> { /* marker — XP orb bonus in StatEffectApplier */ }

            // --- SURVIVAL ---
            case TRACKING            -> { /* marker — luck bonus in StatEffectApplier */ }
            case KEEN_SENSES         -> { /* marker — invisible detection in StatEffectApplier */ }

            // --- CRAFTING ---
            case FORGING             -> { /* marker — tool durability in event */ }
            case COOKING             -> { /* marker — food saturation in event */ }
            case ALCHEMY             -> { /* marker — potion duration in event */ }

            // --- MENTAL ---
            case INTIMIDATION        -> { /* marker — mob aura in StatEffectApplier */ }
            case WILLPOWER           -> applyWillpower(player);
        }
    }

    private void removeEffect(ServerPlayer player) {
        for (var attrKey : List.of(
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.MOVEMENT_SPEED,
            Attributes.ARMOR,
            Attributes.MAX_HEALTH,
            Attributes.JUMP_STRENGTH,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE
        )) {
            AttributeInstance inst = player.getAttribute(attrKey);
            if (inst != null) inst.removeModifier(passiveUuid);
        }
    }

    // --- COMBAT ---

    private void applyBruteForce(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.10; case 2 -> 0.20; default -> 0.30; };
        addModifier(player, Attributes.ATTACK_DAMAGE, bonus, "brute_passive");
    }

    private void applyBladeTechnique(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.05; case 2 -> 0.10; default -> 0.15; };
        addModifier(player, Attributes.ATTACK_SPEED, bonus, "blade_passive");
    }

    private void applyRapidite(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.10; case 2 -> 0.20; default -> 0.30; };
        addModifier(player, Attributes.ATTACK_SPEED, bonus, "rapid_passive");
    }

    private void applyAgility(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.10; case 2 -> 0.15; default -> 0.20; };
        addModifier(player, Attributes.MOVEMENT_SPEED, bonus, "agility_passive");
    }

    private void applyPhysicalResistance(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 4.0 / 20.0; case 2 -> 8.0 / 20.0; default -> 12.0 / 20.0; };
        addModifier(player, Attributes.ARMOR, bonus, "resist_passive");
    }

    private void applyPhysicalEndurance(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 4.0 / 20.0; case 2 -> 8.0 / 20.0; default -> 12.0 / 20.0; };
        addModifier(player, Attributes.MAX_HEALTH, bonus, "endurance_passive");
    }

    // --- MAGIC ---

    private void applyAirAffinity(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.05; case 2 -> 0.10; default -> 0.15; };
        addModifier(player, Attributes.JUMP_STRENGTH, bonus, "air_passive");
    }

    private void applyMagicResistance(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 2.0 / 20.0; case 2 -> 4.0 / 20.0; default -> 6.0 / 20.0; };
        addModifier(player, Attributes.ARMOR_TOUGHNESS, bonus, "magicres_passive");
    }

    // --- MENTAL ---

    private void applyWillpower(ServerPlayer player) {
        double bonus = switch (tier) { case 1 -> 0.15; case 2 -> 0.30; default -> 0.45; };
        addModifier(player, Attributes.KNOCKBACK_RESISTANCE, bonus, "willpower_passive");
    }

    // --- Shared helper ---

    private void addModifier(ServerPlayer player,
                              Attribute attr,
                              double amount, String name) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) {
            instance.removeModifier(passiveUuid);
            instance.addPermanentModifier(new AttributeModifier(
                passiveUuid, "statmod:" + name, amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // --- Test helpers (package-private) ---

    /** Runtime null-check; exhaustive switch coverage is enforced by the compiler. */
    static void assertStatIsNonNull(StatType stat) {
        if (stat == null) throw new IllegalArgumentException("stat cannot be null");
    }

    static List<String> getRemoveEffectAttributes() {
        return List.of(
            "MAX_HEALTH", "JUMP_STRENGTH", "ARMOR_TOUGHNESS", "KNOCKBACK_RESISTANCE",
            "ATTACK_DAMAGE", "ATTACK_SPEED", "MOVEMENT_SPEED", "ARMOR"
        );
    }

    public StatType getStat() { return stat; }
    public int getTier()      { return tier; }
}
