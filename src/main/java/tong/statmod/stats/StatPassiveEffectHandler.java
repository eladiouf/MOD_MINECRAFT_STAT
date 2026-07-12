package tong.statmod.stats;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bonus passifs continus pour les stats qui n'avaient aucun effet level-based :
 * <ul>
 *   <li>WATER_AFFINITY — regen sous l'eau/pluie</li>
 *   <li>EARTH_AFFINITY — knockback resistance</li>
 *   <li>FIRE_AFFINITY — fire resistance partielle + bonus fire dmg</li>
 *   <li>AIR_AFFINITY — jump boost + step assist</li>
 *   <li>CASTING_SPEED — haste (dig/use speed)</li>
 *   <li>MANA_POOL — absorption passive qui regen lentement</li>
 *   <li>ERUDITION — bonus XP vanilla</li>
 * </ul>
 *
 * <p>Tous les effets sont proportionnels au niveau effectif de la stat.
 * Période : toutes les 20 ticks (1 seconde) pour les effets tick, certains
 * attribute modifiers sont mis à jour toutes les 20 ticks via cache.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class StatPassiveEffectHandler {
    private StatPassiveEffectHandler() {}

    private static final ResourceLocation EARTH_KB_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "earth_affinity_kb_resist");
    private static final ResourceLocation AIR_JUMP_ID =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "air_affinity_jump");

    // Apotheosis attribute keys
    private static final ResourceLocation APOTH_FIRE_DAMAGE = ResourceLocation.fromNamespaceAndPath("apotheosis", "fire_damage");
    private static final ResourceLocation APOTH_COLD_DAMAGE = ResourceLocation.fromNamespaceAndPath("apotheosis", "cold_damage");
    private static final ResourceLocation APOTH_LIGHTNING_DAMAGE = ResourceLocation.fromNamespaceAndPath("apotheosis", "lightning_damage");
    private static final ResourceLocation APOTH_EARTH_DAMAGE = ResourceLocation.fromNamespaceAndPath("apotheosis", "earth_damage");

    // Apotheosis modifiers
    private static final ResourceLocation APOTH_FIRE_MOD = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "apoth_fire_bonus");
    private static final ResourceLocation APOTH_COLD_MOD = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "apoth_cold_bonus");
    private static final ResourceLocation APOTH_LIGHTNING_MOD = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "apoth_lightning_bonus");
    private static final ResourceLocation APOTH_EARTH_MOD = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "apoth_earth_bonus");

    // Caches pour ne pas re-apply les mêmes modifiers si le level n'a pas changé.
    private static final Map<UUID, Integer> lastEarth = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastAir = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastApothFire = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastApothCold = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastApothLightning = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastApothEarth = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        // ── WATER AFFINITY — Regen sous l'eau ou pluie ──────────────────────
        int water = RaceEffectApplier.getEffectiveLevel(player, StatType.WATER_AFFINITY.index);
        if (water > 0 && (player.isInWater() || isInRain(player))) {
            // Regen I (amplifier 0) pendant 3s, mais seulement si le level est ≥ 5.
            // À lv 20+, Regen II. À lv 50+, Regen III.
            int amplifier = water >= 50 ? 2 : water >= 20 ? 1 : 0;
            if (water >= 5) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, amplifier, false, false));
            }
            // Bonus subtil : respiration sous l'eau améliorée (bulle de 3s supplémentaire).
            if (water >= 10 && player.isUnderWater()) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0, false, false));
            }
        }

        // ── EARTH AFFINITY — Knockback resistance ───────────────────────────
        int earth = RaceEffectApplier.getEffectiveLevel(player, StatType.EARTH_AFFINITY.index);
        applyAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, EARTH_KB_ID,
                earth, lastEarth, player.getUUID(), 0.005, 0.5);

        // ── FIRE AFFINITY — Fire resistance partielle ───────────────────────
        int fire = RaceEffectApplier.getEffectiveLevel(player, StatType.FIRE_AFFINITY.index);
        if (fire > 0) {
            // À lv 30+, fire resistance permanente (réduit le dégât de feu).
            if (fire >= 30) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
            }
            // Bonus subtil : réduction du temps de burn. On ne peut pas réduire le temps
            // directement, mais on peut éteindre le feu plus vite.
            if (fire >= 15 && player.getRemainingFireTicks() > 0) {
                int reduction = Math.max(1, fire / 10);
                player.setRemainingFireTicks(Math.max(0, player.getRemainingFireTicks() - reduction));
            }
        }

        // ── AIR AFFINITY — Jump boost + step height ─────────────────────────
        int air = RaceEffectApplier.getEffectiveLevel(player, StatType.AIR_AFFINITY.index);
        if (air >= 5) {
            // Jump Boost I à lv 5+, Jump Boost II à lv 30+.
            int jumpAmp = air >= 30 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, jumpAmp, false, false));
        }
        // Slow falling partiel à lv 20+.
        if (air >= 20 && player.fallDistance > 2.0f) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
        }

        // ── CASTING SPEED — Haste (vitesse de mine/utilisation) ─────────────
        int castSpeed = RaceEffectApplier.getEffectiveLevel(player, StatType.CASTING_SPEED.index);
        if (castSpeed >= 10) {
            // Haste I à lv 10+, Haste II à lv 40+.
            int hasteAmp = castSpeed >= 40 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, hasteAmp, false, false));
        }

        // ── MANA POOL — Absorption passive qui se régénère ──────────────────
        int manaPool = RaceEffectApplier.getEffectiveLevel(player, StatType.MANA_POOL.index);
        if (manaPool >= 5) {
            // Regen d'absorption lente : +0.02 par level toutes les secondes, cap à manaPool/5.
            float maxAbsorption = Math.min(10f, manaPool * 0.2f);
            if (player.getAbsorptionAmount() < maxAbsorption) {
                float gain = Math.min(0.5f, manaPool * 0.02f);
                player.setAbsorptionAmount(Math.min(maxAbsorption,
                        player.getAbsorptionAmount() + gain));
            }
        }

        // ── APOTHEOSIS INTEGRATION — ELEMENTAL DAMAGE SCALING ───────────────
        UUID uuid = player.getUUID();
        applyOptionalAttributeModifier(player, APOTH_FIRE_DAMAGE, APOTH_FIRE_MOD,
                fire, lastApothFire, uuid, 0.1, 50.0); // +0.1 dmg/lvl, max +50.0
        applyOptionalAttributeModifier(player, APOTH_COLD_DAMAGE, APOTH_COLD_MOD,
                water, lastApothCold, uuid, 0.1, 50.0);
        applyOptionalAttributeModifier(player, APOTH_LIGHTNING_DAMAGE, APOTH_LIGHTNING_MOD,
                air, lastApothLightning, uuid, 0.1, 50.0);
        applyOptionalAttributeModifier(player, APOTH_EARTH_DAMAGE, APOTH_EARTH_MOD,
                earth, lastApothEarth, uuid, 0.1, 50.0);
    }

    // ── ERUDITION — Bonus XP vanilla quand un mob drop de l'XP ──────────────

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() == null) return;
        Player player = event.getAttackingPlayer();

        int erudition = RaceEffectApplier.getEffectiveLevel(player, StatType.ERUDITION.index);
        if (erudition <= 0) return;

        // +0.15% XP bonus par level → à lv 50 = +7.5%, à lv 100 = +15%.
        // Petit mais constant, s'empile avec les perks.
        float multiplier = 1.0f + erudition * 0.0015f;
        event.setDroppedExperience(Math.max(1, Math.round(event.getDroppedExperience() * multiplier)));
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────

    private static boolean isInRain(Player player) {
        return player.level().isRainingAt(player.blockPosition());
    }

    private static void applyAttributeModifier(Player player, Holder<Attribute> attribute,
                                               ResourceLocation id, int level,
                                               Map<UUID, Integer> cache, UUID uuid,
                                               double perLevel, double maxValue) {
        Integer previous = cache.get(uuid);
        if (previous != null && previous == level) return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        instance.removeModifier(id);
        if (level > 0) {
            double value = Math.min(maxValue, level * perLevel);
            instance.addPermanentModifier(
                    new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }

        if (level == 0) {
            cache.remove(uuid);
        } else {
            cache.put(uuid, level);
        }
    }

    private static void applyOptionalAttributeModifier(Player player, ResourceLocation attributeRl,
                                                       ResourceLocation id, int level,
                                                       Map<UUID, Integer> cache, UUID uuid,
                                                       double perLevel, double maxValue) {
        Integer previous = cache.get(uuid);
        if (previous != null && previous == level) return;

        var opt = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(attributeRl);
        if (opt.isEmpty()) return;

        AttributeInstance instance = player.getAttribute(opt.get());
        if (instance == null) return;

        instance.removeModifier(id);
        if (level > 0) {
            double value = Math.min(maxValue, level * perLevel);
            instance.addPermanentModifier(
                    new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }

        if (level == 0) {
            cache.remove(uuid);
        } else {
            cache.put(uuid, level);
        }
    }

    public static void clearCaches(UUID uuid) {
        lastEarth.remove(uuid);
        lastAir.remove(uuid);
        lastApothFire.remove(uuid);
        lastApothCold.remove(uuid);
        lastApothLightning.remove(uuid);
        lastApothEarth.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearCaches(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        clearCaches(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearCaches(event.getEntity().getUUID());
    }
}
