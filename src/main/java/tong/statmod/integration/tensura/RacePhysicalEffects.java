package tong.statmod.integration.tensura;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;

/**
 * Phase A — physical race differentiation via vanilla attributes + night vision.
 * Applied through TensuraRaceHandler.applyRaceBonuses and refreshed each tick for the buff effect.
 */
public final class RacePhysicalEffects {
    private static final ResourceLocation ID_SCALE      = id("race_physical_scale");
    private static final ResourceLocation ID_SPEED      = id("race_physical_speed");
    private static final ResourceLocation ID_STEP       = id("race_physical_step_height");
    private static final ResourceLocation ID_JUMP       = id("race_physical_jump");
    private static final ResourceLocation ID_MAX_HEALTH = id("race_physical_max_health");
    private static final ResourceLocation ID_ATTACK     = id("race_physical_attack_damage");
    private static final ResourceLocation ID_ARMOR      = id("race_physical_armor");
    private static final ResourceLocation ID_BLOCK_REACH = id("race_physical_block_reach");
    private static final ResourceLocation ID_ENTITY_REACH = id("race_physical_entity_reach");
    private static final ResourceLocation ID_FALL_DAMAGE = id("race_physical_fall_damage");
    private static final ResourceLocation ID_SAFE_FALL   = id("race_physical_safe_fall");
    private static final ResourceLocation ID_KNOCKBACK   = id("race_physical_knockback");

    private static final Map<Holder<Attribute>, ResourceLocation> ATTR_TO_ID = Map.ofEntries(
            Map.entry(Attributes.SCALE,                  ID_SCALE),
            Map.entry(Attributes.MOVEMENT_SPEED,          ID_SPEED),
            Map.entry(Attributes.STEP_HEIGHT,             ID_STEP),
            Map.entry(Attributes.JUMP_STRENGTH,           ID_JUMP),
            Map.entry(Attributes.MAX_HEALTH,              ID_MAX_HEALTH),
            Map.entry(Attributes.ATTACK_DAMAGE,           ID_ATTACK),
            Map.entry(Attributes.ARMOR,                   ID_ARMOR),
            Map.entry(Attributes.BLOCK_INTERACTION_RANGE, ID_BLOCK_REACH),
            Map.entry(Attributes.ENTITY_INTERACTION_RANGE,ID_ENTITY_REACH),
            Map.entry(Attributes.FALL_DAMAGE_MULTIPLIER,  ID_FALL_DAMAGE),
            Map.entry(Attributes.SAFE_FALL_DISTANCE,      ID_SAFE_FALL),
            Map.entry(Attributes.KNOCKBACK_RESISTANCE,    ID_KNOCKBACK)
    );

    private static final Map<String, List<ModSpec>> RACES = Map.of(
            "tensura:human", List.of(
                    new ModSpec(Attributes.MAX_HEALTH,              80.0,   ADD_VALUE)
            ),
            "tensura:elf", List.of(
                    new ModSpec(Attributes.SCALE,                  0.10,   ADD_VALUE),
                    new ModSpec(Attributes.MOVEMENT_SPEED,         0.15,   ADD_MULTIPLIED_BASE),
                    new ModSpec(Attributes.STEP_HEIGHT,            0.20,   ADD_VALUE),
                    new ModSpec(Attributes.JUMP_STRENGTH,          0.05,   ADD_MULTIPLIED_BASE),
                    new ModSpec(Attributes.BLOCK_INTERACTION_RANGE,0.5,    ADD_VALUE),
                    new ModSpec(Attributes.ENTITY_INTERACTION_RANGE,0.5,   ADD_VALUE),
                    new ModSpec(Attributes.MAX_HEALTH,             40.0,   ADD_VALUE),
                    new ModSpec(Attributes.FALL_DAMAGE_MULTIPLIER, 0.10,   ADD_MULTIPLIED_BASE)
            ),
            "tensura:dwarf", List.of(
                    new ModSpec(Attributes.SCALE,                  -0.15,  ADD_VALUE),
                    new ModSpec(Attributes.MOVEMENT_SPEED,         -0.15,  ADD_MULTIPLIED_BASE),
                    new ModSpec(Attributes.JUMP_STRENGTH,          -0.10,  ADD_MULTIPLIED_BASE),
                    new ModSpec(Attributes.MAX_HEALTH,              130.0,   ADD_VALUE),
                    new ModSpec(Attributes.ATTACK_DAMAGE,           0.5,   ADD_VALUE),
                    new ModSpec(Attributes.ARMOR,                   2.0,   ADD_VALUE),
                    new ModSpec(Attributes.KNOCKBACK_RESISTANCE,    0.3,   ADD_VALUE),
                    new ModSpec(Attributes.SAFE_FALL_DISTANCE,      3.0,   ADD_VALUE),
                    new ModSpec(Attributes.FALL_DAMAGE_MULTIPLIER, -0.40,  ADD_MULTIPLIED_BASE)
            ),
            "tensura:beastfolk", List.of(
                    new ModSpec(Attributes.STEP_HEIGHT,             0.5,   ADD_VALUE),
                    new ModSpec(Attributes.MOVEMENT_SPEED,          0.20,  ADD_MULTIPLIED_BASE),
                    new ModSpec(Attributes.JUMP_STRENGTH,           0.20,  ADD_MULTIPLIED_BASE),
                    new ModSpec(Attributes.MAX_HEALTH,              160.0,   ADD_VALUE),
                    new ModSpec(Attributes.ATTACK_DAMAGE,           0.5,   ADD_VALUE),
                    new ModSpec(Attributes.FALL_DAMAGE_MULTIPLIER, -0.20,  ADD_MULTIPLIED_BASE)
            )
    );

    private static final Set<String> NIGHT_VISION_RACES = Set.of("tensura:elf", "tensura:dwarf");

    private RacePhysicalEffects() {}

    public static float getScaleFactor(Player player) {
        if (player == null) return 1.0f;
        try {
            return getScaleFactor(TensuraRaceHandler.getRaceName(player));
        } catch (Exception e) {
            return 1.0f;
        }
    }

    public static float getScaleFactor(String raceId) {
        List<ModSpec> specs = RACES.get(raceId);
        if (specs == null) return 1.0f;
        for (ModSpec spec : specs) {
            if (spec.attribute == Attributes.SCALE) {
                return 1.0f + (float) spec.amount;
            }
        }
        return 1.0f;
    }

    public static void apply(Player player, String raceId) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        clearModifiers(player);

        List<ModSpec> specs = RACES.get(raceId);
        if (specs != null) {
            for (ModSpec spec : specs) {
                AttributeInstance instance = player.getAttribute(spec.attribute);
                if (instance == null) continue;
                ResourceLocation id = ATTR_TO_ID.get(spec.attribute);
                instance.addPermanentModifier(new AttributeModifier(id, spec.amount, spec.op));
            }
        }

        if (!NIGHT_VISION_RACES.contains(raceId)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void clearModifiers(Player player) {
        for (Map.Entry<Holder<Attribute>, ResourceLocation> entry : ATTR_TO_ID.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance != null) {
                instance.removeModifier(entry.getValue());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 100 != 0) return;

        String raceId = TensuraRaceHandler.getRaceName(player);
        if (!NIGHT_VISION_RACES.contains(raceId)) return;

        MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
        if (existing == null || existing.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    MobEffectInstance.INFINITE_DURATION,
                    0,
                    true,
                    false,
                    false
            ));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(STATMod.MODID, path);
    }

    private record ModSpec(Holder<Attribute> attribute, double amount, AttributeModifier.Operation op) {}
}
