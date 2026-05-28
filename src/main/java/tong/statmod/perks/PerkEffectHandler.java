package tong.statmod.perks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;

/**
 * Applies all 42 perk effects via Forge events (1.20.1 compatible).
 */
@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class PerkEffectHandler {

    private static final Map<UUID, Map<Integer, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> comboCounter = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> comboTimer = new ConcurrentHashMap<>();
    // Bug 1 fix: track which mobs each player has already hit (for TRACKING_PACK)
    private static final Map<UUID, Set<Integer>> hitMobTracker = new ConcurrentHashMap<>();
    // Bug 3 fix: reentrancy guard for WILL_FOCUS
    private static final Set<UUID> effectProcessing = ConcurrentHashMap.newKeySet();

    private static boolean isOnCooldown(UUID uuid, int perkId, long ms) {
        Map<Integer, Long> pc = cooldowns.get(uuid);
        if (pc == null) return false;
        Long last = pc.get(perkId);
        return last != null && (System.currentTimeMillis() - last) < ms;
    }

    private static void setCooldown(UUID uuid, int perkId) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(perkId, System.currentTimeMillis());
    }

    // ==================== ATTACK ====================

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity)) return;

        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // RAPID_ELAN: +10% speed after attack (2s)
            if (perks.isUnlocked(Perk.RAPID_ELAN)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
            }

            // BLADE_COMBO: track hits
            // Bug 1 fix: also track hit mobs for TRACKING_PACK
            if (perks.isUnlocked(Perk.TRACKING_PACK)) {
                hitMobTracker.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet())
                    .add(event.getTarget().getId());
            }

            if (perks.isUnlocked(Perk.BLADE_COMBO)) {
                UUID uuid = player.getUUID();
                long now = System.currentTimeMillis();
                Long lastCombo = comboTimer.get(uuid);
                int count = comboCounter.getOrDefault(uuid, 0);
                if (lastCombo == null || (now - lastCombo) > 3000) count = 0;
                count++;
                comboCounter.put(uuid, count);
                comboTimer.put(uuid, now);
            }
        });
    }

    // ==================== HURT ====================

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        // --- PLAYER ATTACKING ---
        if (sourceEntity instanceof ServerPlayer attacker) {
            LivingEntity target = event.getEntity();

            attacker.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
                float multiplier = 1.0f;

                // BRUTE_DEMOLITION: +50% damage
                if (perks.isUnlocked(Perk.BRUTE_DEMOLITION)) multiplier += 0.5f;

                // BLADE_COMBO: 5 hits → +50% on next
                if (perks.isUnlocked(Perk.BLADE_COMBO)) {
                    int count = comboCounter.getOrDefault(attacker.getUUID(), 0);
                    if (count >= 5) {
                        multiplier += 0.5f;
                        comboCounter.put(attacker.getUUID(), 0);
                    }
                }

                // RAPID_DOUBLE: 10% chance double hit
                if (perks.isUnlocked(Perk.RAPID_DOUBLE) && attacker.getRandom().nextFloat() < 0.1f) {
                    multiplier += 1.0f;
                }

                // PRECISION_SNIPER: +10% at 15+ blocks
                if (perks.isUnlocked(Perk.PRECISION_SNIPER) && attacker.distanceTo(target) > 15.0) {
                    multiplier += 0.1f;
                }

                // PRECISION_CRIT: crit knockback on full-charge + falling
                if (perks.isUnlocked(Perk.PRECISION_CRIT)
                        && attacker.getAttackStrengthScale(0.5f) > 0.9f && attacker.fallDistance > 0) {
                    target.knockback(1.0f, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                }

                // INTIMIDATE_FEARLESS: +25% when outnumbered
                if (perks.isUnlocked(Perk.INTIMIDATE_FEARLESS)) {
                    long hostiles = attacker.level().getEntitiesOfClass(Mob.class,
                        attacker.getBoundingBox().inflate(10), m -> m.getTarget() == attacker).size();
                    if (hostiles >= 3) multiplier += 0.25f;
                }

                // WILL_UNBREAKABLE: <2 hearts → +30% damage
                if (perks.isUnlocked(Perk.WILL_UNBREAKABLE) && attacker.getHealth() < 4.0f) {
                    multiplier += 0.3f;
                }

                // TRACKING_PACK: +15% vs already-hit mobs
                if (perks.isUnlocked(Perk.TRACKING_PACK)) {
                    Set<Integer> hitMobs = hitMobTracker.get(attacker.getUUID());
                    if (hitMobs != null && hitMobs.contains(target.getId())) {
                        multiplier += 0.15f;
                    }
                }

                // BLADE_BLEED: charged hit → wither
                if (perks.isUnlocked(Perk.BLADE_BLEED) && attacker.getAttackStrengthScale(0.5f) > 0.8f) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                }

                // BRUTE_ARMOR_PIERCE (ID 1): +15% armor damage on hit
                if (perks.isUnlocked(Perk.BRUTE_ARMOR_PIERCE)) {
                    for (ItemStack armor : target.getArmorSlots()) {
                        if (!armor.isEmpty() && armor.isDamageableItem()) {
                            int extraDmg = Math.max(1, (int) (event.getAmount() * 0.15f));
                            armor.hurtAndBreak(extraDmg, target, e -> e.broadcastBreakEvent(e.getEquipmentSlotForItem(armor)));
                        }
                    }
                }

                // BRUTE_STUN (ID 2): charged hit → stun (Slowness V + Mining Fatigue V, 1s)
                if (perks.isUnlocked(Perk.BRUTE_STUN) && attacker.getAttackStrengthScale(0.5f) > 0.8f) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 4, false, true));
                }

                // PRECISION_PIERCE (ID 20): arrow hits → 50% damage passes through to next mob
                if (perks.isUnlocked(Perk.PRECISION_PIERCE)) {
                    Entity projectile = event.getSource().getDirectEntity();
                    if (projectile instanceof Arrow arrow) {
                        String pierceTag = "statmod_pierced";
                        if (!arrow.getTags().contains(pierceTag)) {
                            arrow.addTag(pierceTag);
                            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(0.8));
                            arrow.setPierceLevel((byte) (arrow.getPierceLevel() + 1));
                        }
                    }
                }

                // INTIMIDATE_ROAR: killing blow → nearby mobs get fear
                if (perks.isUnlocked(Perk.INTIMIDATE_ROAR)) {
                    if (target.getHealth() - event.getAmount() <= 0) {
                        attacker.level().getEntitiesOfClass(Mob.class,
                            attacker.getBoundingBox().inflate(8), m -> m != target).forEach(mob -> {
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                        });
                    }
                }

                // RAPID_INSTINCT: dodge → +50% attack speed
                if (perks.isUnlocked(Perk.RAPID_INSTINCT) && attacker.invulnerableTime > 0) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 1, false, false));
                }

                // COURSE_PERCUTANTE: sprint+jump → bonus damage
                if (perks.isUnlocked(Perk.AGILITY_SPRINT) && attacker.isSprinting() && !attacker.onGround()) {
                    multiplier += 0.3f;
                }

                if (multiplier != 1.0f) event.setAmount(event.getAmount() * multiplier);
            });
        }

        // --- PLAYER BEING HIT ---
        if (event.getEntity() instanceof ServerPlayer defender) {
            defender.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
                float reduction = 0.0f;

                // BLADE_PARRY: blocking → -25%
                if (perks.isUnlocked(Perk.BLADE_PARRY) && defender.isBlocking()) {
                    reduction += 0.25f;
                }

                // ENDURANCE_MAGIC: blocking + magic → -20%
                if (perks.isUnlocked(Perk.ENDURANCE_MAGIC) && defender.isBlocking()
                        && "indirectMagic".equals(event.getSource().getMsgId())) {
                    reduction += 0.20f;
                }

                // RESIST_TOUGHNESS: 30% chance halved
                if (perks.isUnlocked(Perk.RESIST_TOUGHNESS) && defender.getRandom().nextFloat() < 0.3f) {
                    reduction += 0.5f;
                }

                // WILL_UNBREAKABLE: <2 hearts → +30% resistance
                if (perks.isUnlocked(Perk.WILL_UNBREAKABLE) && defender.getHealth() < 4.0f) {
                    reduction += 0.3f;
                }

                if (reduction > 0) {
                    event.setAmount(event.getAmount() * (1.0f - Math.min(reduction, 0.9f)));
                }

                // RESIST_ABSORB: 1 heart absorption after hit (10s CD)
                if (perks.isUnlocked(Perk.RESIST_ABSORB) && !isOnCooldown(defender.getUUID(), 13, 10000)) {
                    defender.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, false));
                    setCooldown(defender.getUUID(), 13);
                }

                // ENDURANCE_PERFECT: block at invuln → 0 damage + knockback
                if (perks.isUnlocked(Perk.ENDURANCE_PERFECT) && defender.isBlocking()
                        && defender.invulnerableTime > 0 && defender.invulnerableTime < 10) {
                    event.setAmount(0);
                    Entity attacker = event.getSource().getEntity();
                    if (attacker instanceof LivingEntity le) {
                        le.knockback(1.5f, defender.getX() - le.getX(), defender.getZ() - le.getZ());
                    }
                }
            });
        }
    }

    // ==================== FALL ====================

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // RESIST_FALL: -10% fall distance
            if (perks.isUnlocked(Perk.RESIST_FALL)) {
                event.setDistance(event.getDistance() * 0.9f);
            }
        });
    }

    // ==================== TICK ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // KEEN_NIGHT: night vision
            if (perks.isUnlocked(Perk.KEEN_NIGHT) && player.isShiftKeyDown()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }

            // WILL_ENDURANCE: regen under poison
            if (perks.isUnlocked(Perk.WILL_ENDURANCE)
                    && player.hasEffect(MobEffects.POISON) && player.getHealth() < player.getMaxHealth()) {
                player.heal(0.5f);
            }

            // AGILITY_JUMP: +20% jump in combat
            if (perks.isUnlocked(Perk.AGILITY_JUMP)) {
                boolean inCombat = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(12), m -> m.getTarget() == player).isEmpty();
                if (inCombat) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false));
                }
            }

            // INTIMIDATE_AURA: weak mobs flee
            if (perks.isUnlocked(Perk.INTIMIDATE_AURA)) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(6), m -> m.getTarget() == player).forEach(mob -> {
                    if (mob.getMaxHealth() <= 20) {
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                        mob.setTarget(null);
                    }
                });
            }

            // ENDURANCE_MOBILE: sprint with shield
            if (perks.isUnlocked(Perk.ENDURANCE_MOBILE) && player.isBlocking() && !player.isSprinting()) {
                player.setSprinting(true);
            }

            // AGILITY_ELYTRA: +10% elytra speed in combat (additive, capped)
            if (perks.isUnlocked(Perk.AGILITY_ELYTRA) && player.isFallFlying()) {
                boolean inCombat = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), m -> m.getTarget() == player).isEmpty();
                if (inCombat) {
                    var vel = player.getDeltaMovement();
                    double maxSpeed = 2.0; // cap to prevent insane speeds
                    double bx = Math.min(vel.x + 0.02, maxSpeed);
                    double bz = Math.min(vel.z + 0.02, maxSpeed);
                    player.setDeltaMovement(bx, vel.y, bz);
                }
            }

            // TRACKING_SCENT: +50% detection range (Glowing on nearby mobs when sneaking)
            if (perks.isUnlocked(Perk.TRACKING_SCENT) && player.isShiftKeyDown()) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(20)).forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                });
            }

            // TRACKING_STALKER: mobs detect you later
            if (perks.isUnlocked(Perk.TRACKING_STALKER)) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), m -> m.getTarget() == player).forEach(mob -> {
                    if (mob.distanceTo(player) > 12) mob.setTarget(null);
                });
            }

            // KEEN_EXPLORER (ID 26): holding a filled map → night vision for better exploration
            if (perks.isUnlocked(Perk.KEEN_EXPLORER)) {
                var mainHand = player.getMainHandItem();
                var offHand = player.getOffhandItem();
                boolean holdingMap = mainHand.getItem() == Items.FILLED_MAP || offHand.getItem() == Items.FILLED_MAP;
                if (holdingMap) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
                }
            }

            // KEEN_DANGER (ID 25): alert when a mob targets you
            if (perks.isUnlocked(Perk.KEEN_DANGER)) {
                boolean targeted = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), m -> m.getTarget() == player).isEmpty();
                if (targeted && player.tickCount % 60 == 0) { // alert every 3s max
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.5f, 1.5f);
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("⚠ Danger!").withStyle(net.minecraft.ChatFormatting.YELLOW),
                        true);
                }
            }
        });
    }

    // ==================== EFFECT ADDED ====================

    @SubscribeEvent
    public static void onEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event instanceof net.minecraftforge.event.entity.living.MobEffectEvent.Added added)) return;

        // Bug 3 fix: reentrancy guard to prevent infinite recursion
        UUID uuid = player.getUUID();
        if (effectProcessing.contains(uuid)) return;

        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // WILL_FOCUS (39): -15% negative effects duration
            if (perks.isUnlocked(Perk.WILL_FOCUS)) {
                var inst = added.getEffectInstance();
                if (inst.getEffect().isBeneficial()) return;
                int newDur = (int) (inst.getDuration() * 0.85f);
                if (newDur > 0) {
                    effectProcessing.add(uuid);
                    player.removeEffect(inst.getEffect());
                    player.addEffect(new MobEffectInstance(inst.getEffect(), newDur, inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                    effectProcessing.remove(uuid);
                }
            }
        });
    }

    // ==================== ANVIL ====================

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // FORGE_REPAIR: -20% anvil cost
            if (perks.isUnlocked(Perk.FORGE_REPAIR) && event.getCost() > 0) {
                event.setCost(Math.max(1, (int) (event.getCost() * 0.8f)));
            }

            // FORGE_TEMPLATE (ID 29): templates not consumed — return template in output
            if (perks.isUnlocked(Perk.FORGE_TEMPLATE)) {
                ItemStack right = event.getRight();
                if (right.getItem() == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE
                    || (right.getItem().getClass().getSimpleName().contains("SmithingTemplate"))) {
                    // The template survives — set it as the output's "material" side
                    // In practice, we just reduce the cost to 0 for template operations
                    if (event.getMaterialCost() > 0) {
                        event.setMaterialCost(0);
                    }
                }
            }
        });
    }

    // ==================== FOOD EATEN ====================

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // COOK_FEAST: +20% saturation
            if (perks.isUnlocked(Perk.COOK_FEAST)) {
                var food = event.getItem().getItem().getFoodProperties();
                if (food != null) {
                    float bonusSat = food.getSaturationModifier() * 0.2f;
                    player.getFoodData().setSaturation(
                        player.getFoodData().getSaturationLevel() + bonusSat);
                }
            }

            // COOK_CHEF (ID 32): complex dishes (nutrition >= 6) give +50% extra saturation
            if (perks.isUnlocked(Perk.COOK_CHEF)) {
                var food = event.getItem().getItem().getFoodProperties();
                if (food != null && food.getNutrition() >= 6) {
                    float bonusSat = food.getSaturationModifier() * 0.5f;
                    player.getFoodData().setSaturation(
                        player.getFoodData().getSaturationLevel() + bonusSat);
                }
            }
        });
    }

    // ==================== POTION CONSUMED ====================

    @SubscribeEvent
    public static void onPotionConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack item = event.getItem();

        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // ALCHEMY_BREWER (ID 33): drinking a potion → re-apply with +20% duration
            if (perks.isUnlocked(Perk.ALCHEMY_BREWER) && item.getItem() == Items.POTION) {
                var potion = net.minecraft.world.item.alchemy.PotionUtils.getPotion(item);
                var effects = net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(item);
                for (var effect : effects) {
                    int extended = (int) (effect.getDuration() * 1.2f);
                    player.addEffect(new MobEffectInstance(
                        effect.getEffect(), extended, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()));
                }
            }
        });
    }

    // ==================== SMELTED ====================

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            ItemStack result = event.getSmelting();
            // COOK_GRILL (ID 31): 30% chance to double cooked meat output
            if (perks.isUnlocked(Perk.COOK_GRILL)) {
                if (result.getItem().isEdible() && result.getItem().getFoodProperties() != null
                    && result.getItem().getFoodProperties().isMeat()) {
                    if (player.getRandom().nextFloat() < 0.3f) {
                        player.getInventory().add(result.copy());
                    }
                }
            }

            // COOK_CHEF (ID 32): complex dishes give +50% food XP
            // XP is handled via the existing progression system when food is eaten
        });
    }

    // ==================== BREAK SPEED ====================

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
            // FORGE_NETHERITE: +25% durability → +10% mining speed
            if (perks.isUnlocked(Perk.FORGE_NETHERITE)) {
                event.setNewSpeed(event.getNewSpeed() * 1.1f);
            }
        });
    }

    // ==================== CLEANUP ====================

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        cooldowns.remove(uuid);
        comboCounter.remove(uuid);
        comboTimer.remove(uuid);
        hitMobTracker.remove(uuid);
    }
}
