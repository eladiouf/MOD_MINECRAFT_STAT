package tong.statmod.perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.util.LagDetector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class PerkEffectHandler {

    private static final int STAT_COUNT = 23;

    // ==================== TICK (every second) ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        long __start = System.nanoTime();
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        CapabilityHelper.withPerks(player, perks -> {
            // KEEN_CORE (48) — Vigilant: blindness immune, night vision
            if (perks.isUnlocked(48)) {
                if (player.hasEffect(MobEffects.BLINDNESS))
                    player.removeEffect(MobEffects.BLINDNESS);
                if (player.level().getMaxLocalRawBrightness(player.blockPosition().below()) < 4)
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }

            // AGILITY_CORE (18) — +5% move speed already in StatEffectApplier

            // AGILITY_SYNERGY (20) — Pas de Loup: no terrain penalty
            if (perks.isUnlocked(20)) {
                var below = player.level().getBlockState(player.blockPosition().below());
                if (below.is(net.minecraft.world.level.block.Blocks.SOUL_SAND) || below.is(net.minecraft.world.level.block.Blocks.HONEY_BLOCK)) {
                    player.setDeltaMovement(player.getDeltaMovement().multiply(1.3, 1, 1.3));
                }
            }

            // AGILITY_SITUATIONAL (21) — Expert Évasion: dodge removes slowness
            if (perks.isUnlocked(21)) {
                if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }

            // INTIM_CORE (72) — Regard Noir: mobs in 4 blocks deal -5% dmg
            if (perks.isUnlocked(72)) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(4), m -> m.getTarget() == player).forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
                });
            }

            // INTIM_ACTIVE (73) — Présence Menagante: weak mobs get Slowness
            if (perks.isUnlocked(73)) {
                player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(6), m -> m.getTarget() == player && m.getHealth() < 20).forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                });
            }

            // INTIM_SYNERGY (74) — handled in onLivingHurt (crits apply Weakness)
            // INTIM_SITUATIONAL (75) — handled in onLivingHurt (10% flee)
            // INTIM_MASTERY (76) — duration bonus handled in onEffectAdded

            // TRACK_CORE (42) — detection range handled server-side in mob AI targeting

            // TRACK_ACTIVE (43) — Éclaireur: see mob health (client-side, handled in mixin or overlay)

            // TRACK_SITUATIONAL (45) — Sillage: wounded mobs glow 5s (handled in onLivingHurt)

            // TRACK_MASTERY (46) — Tactique de Meute: handled in onLivingHurt

            // PHYS_ENDUR_CORE (30) — endurance regen +10% handled in StatEffectApplier

            // PHYS_ENDUR_ACTIVE (31) — Second Souffle: handled in StatEffectApplier

            // KEEN_SITUATIONAL (51) — Sens du Danger: melee range +2 (handled in mixin)

            // KEEN_SYNERGY (50) — alert when targeted from behind
            if (perks.isUnlocked(50)) {
                boolean targetedFromBehind = player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(16), m -> m.getTarget() == player).stream().anyMatch(m -> {
                    var lookVec = player.getLookAngle();
                    var toTarget = m.position().subtract(player.position()).normalize();
                    return lookVec.dot(toTarget) < -0.3;
                });
                if (targetedFromBehind && player.tickCount % 40 == 0) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("\u26a0 Derri\u00e8re toi!").withStyle(
                            net.minecraft.ChatFormatting.GOLD), true);
                }
            }

            // WILL_SITUATIONAL (81) — Résolution Purifiante: crouch 3s dispel
            if (perks.isUnlocked(81) && player.isShiftKeyDown()) {
                UUID uuid = player.getUUID();
                long crouchStart = PerkState.getCrouchStart(uuid);
                if (crouchStart == 0) {
                    PerkState.setCrouchStart(uuid, player.tickCount);
                } else if (player.tickCount - crouchStart >= 60 && !PerkState.isOnCooldown(uuid, 81, 600)) {
                    var negEffects = player.getActiveEffects().stream()
                        .filter(e -> !e.getEffect().isBeneficial()).toList();
                    if (!negEffects.isEmpty()) {
                        player.removeEffect(negEffects.get(0).getEffect());
                        PerkState.setCooldown(uuid, 81);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
                    }
                }
            } else if (player.isShiftKeyDown() == false) {
                PerkState.setCrouchStart(player.getUUID(), 0);
            }

            // ManaPool-related perk checks
            if (perks.isUnlocked(5) && player.getHealth() < player.getMaxHealth() * 0.5f) {
                PerkState.setColosseActive(player.getUUID(), true);
            } else if (perks.isUnlocked(5)) {
                PerkState.setColosseActive(player.getUUID(), false);
            }

            // ALCHEMY_ACTIVE (67) — handled via mixin for effect slot count
        });
        LagDetector.check("PerkEffectHandler:tick", __start);
    }

    // ==================== ATTACK ====================

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        long now = System.currentTimeMillis();
        UUID uuid = player.getUUID();

        CapabilityHelper.withPerks(player, perks -> {
            // BLADE_CORE (6) — Riposte: after perfect parry, next hit +30%
            if (perks.isUnlocked(6) && PerkState.isParryActive(uuid)) {
                PerkState.setNextHitBoost(uuid, 1.3f);
                PerkState.setParryActive(uuid, false);
            }

            // RAPID_SYNERGY (14) — Momentum: consecutive hits +3%
            if (perks.isUnlocked(14)) {
                int combo = PerkState.getComboCount(uuid);
                PerkState.recordComboHit(uuid, now, 3000);
            }

            // AGILITY_MASTERY (22) — Danse des Lames: sprint 10 blocks → +25%
            if (perks.isUnlocked(22) && player.isSprinting()) {
                double dist = PerkState.getSprintDistance(uuid);
                if (dist >= 10) {
                    PerkState.setNextHitBoost(uuid, 1.25f);
                    PerkState.resetSprintDistance(uuid);
                }
            }

            // TRACK_SYNERGY (44) — Patience du Prédateur: crouch 3s → +20%
            if (perks.isUnlocked(44) && player.isShiftKeyDown()) {
                long sneakStart = PerkState.getCrouchStart(uuid);
                if (sneakStart > 0 && player.tickCount - sneakStart >= 60) {
                    PerkState.setNextHitBoost(uuid, 1.2f);
                }
            }

            // BRUTE_SITUATIONAL (3) — handled in onLivingHurt (kill restores endurance)
        });
    }

    // ==================== HURT (living -> damage) ====================

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        // --- PLAYER ATTACKING ---
        if (sourceEntity instanceof ServerPlayer attacker) {
            LivingEntity target = event.getEntity();
            UUID uuid = attacker.getUUID();
            long now = System.currentTimeMillis();

            CapabilityHelper.withPerks(attacker, perks -> {
                float multiplier = 1.0f;

                // BRUTE_CORE (0) — Heavy Swing: +5% axe/mace dmg
                if (perks.isUnlocked(0)) {
                    var mainHand = attacker.getMainHandItem();
                    String itemId = mainHand.getItem().toString();
                    if (itemId.contains("axe") || itemId.contains("mace") || itemId.contains("hammer") || itemId.contains("warhammer")) {
                        multiplier += 0.05f;
                    }
                }

                // BRUTE_ACTIVE (1) — Brise-Bouclier: 15% disable shield 2s
                if (perks.isUnlocked(1) && attacker.getRandom().nextFloat() < 0.15f && target.isBlocking()) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 4, false, true));
                }

                // BRUTE_SYNERGY (2) — Frappe Écrasante: charged attacks ignore 10% armor
                if (perks.isUnlocked(2) && attacker.getAttackStrengthScale(0.5f) > 0.9f) {
                    float armorIgnore = Math.max(0, event.getAmount() * 0.10f);
                    multiplier += armorIgnore / Math.max(1, event.getAmount());
                }

                // BRUTE_SITUATIONAL (3) — Adrénaline: kill restores 2 endurance (handled in death checking)

                // BRUTE_MASTERY (4) — Carnage: kill → 180° arc
                // Handled below when target dies

                // BRUTE_TRANSCENDENCE (5) — Colosse: <50% HP + kill crit → +50%
                if (perks.isUnlocked(5) && attacker.getHealth() < attacker.getMaxHealth() * 0.5f
                    && target.getHealth() - event.getAmount() <= 0) {
                    if (!PerkState.isOnCooldown(uuid, 5, 1200)) {
                        multiplier += 0.5f;
                        PerkState.setCooldown(uuid, 5);
                        // apply knockback boost
                        target.knockback(2.0f, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                    }
                }

                // BLADE_CORE (6) — Riposte boost
                float riposteBoost = PerkState.getNextHitBoost(uuid);
                if (riposteBoost > 1.0f) {
                    multiplier *= riposteBoost;
                    PerkState.setNextHitBoost(uuid, 1.0f);
                }

                // BLADE_ACTIVE (7) — +5% attack speed (StatEffectApplier)

                // BLADE_SYNERGY (8) — Blessures: 10% hemorrhage 4s
                if (perks.isUnlocked(8) && attacker.getRandom().nextFloat() < 0.10f) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, true));
                }

                // BLADE_SITUATIONAL (9) — Danse Lames: 3 kills/10s
                if (perks.isUnlocked(9)) {
                    PerkState.recordKill(uuid, now, 10000);
                    int kills = PerkState.getRecentKills(uuid, 10000);
                    if (kills >= 3) {
                        multiplier += 0.15f * (kills / 3);
                    }
                }

                // BLADE_TRANSCENDENCE (11) — Tempête d'Acier: 3 kills streak
                if (perks.isUnlocked(11)) {
                    PerkState.recordKill(uuid, now, 10000);
                    int kills = PerkState.getRecentKills(uuid, 10000);
                    if (kills >= 3 && !PerkState.isOnCooldown(uuid, 11, 900)) {
                        PerkState.setArcAttack(uuid, true, 100);
                        PerkState.setCooldown(uuid, 11);
                    }
                }

                // RAPID_CORE (12) — +5% speed light weapons (StatEffectApplier)

                // RAPID_ACTIVE (13) — Frappe Perçante: 15% dagger ignore 15% armor
                if (perks.isUnlocked(13) && attacker.getRandom().nextFloat() < 0.15f) {
                    var mainHand = attacker.getMainHandItem();
                    String itemId = mainHand.getItem().toString();
                    if (itemId.contains("dagger") || itemId.contains("knife") || itemId.contains("kukri")) {
                        float armorIgnore = Math.max(0, event.getAmount() * 0.15f);
                        multiplier += armorIgnore / Math.max(1, event.getAmount());
                    }
                }

                // RAPID_SYNERGY (14) — Momentum boost
                if (perks.isUnlocked(14)) {
                    int combo = PerkState.getComboCount(uuid);
                    float comboBonus = Math.min(combo * 0.03f, 0.30f);
                    if (comboBonus > 0) multiplier += comboBonus;
                } else {
                    // Reset combo if not unlocked (handled in PerkState)
                }

                // RAPID_SITUATIONAL (15) — Roulade: handled in StatEffectApplier (endurance cost)

                // RAPID_MASTERY (16) — Vitesse Aveuglante: after roll, 3 hits
                if (perks.isUnlocked(16) && PerkState.getAfterRollHits(uuid) > 0) {
                    multiplier += 0.10f;
                    PerkState.decrementAfterRollHits(uuid);
                }

                // RAPID_TRANSCENDENCE (17) — Frénésie
                if (perks.isUnlocked(17) && PerkState.getEndurance(uuid) < 20) {
                    if (!PerkState.isOnCooldown(uuid, 17, 900)) {
                        PerkState.setFrenzyActive(uuid, true);
                        PerkState.setCooldown(uuid, 17);
                    }
                }
                if (PerkState.isFrenzyActive(uuid)) {
                    multiplier += 0.30f;
                }

                // AGILITY_CORE (18) — StatEffectApplier handles speed

                // AGILITY_ACTIVE (19) — StatEffectApplier handles jump/fall

                // AGILITY_MASTERY (22) — Danse des Lames boost
                float agilityBoost = PerkState.getNextHitBoost(uuid);
                if (agilityBoost > 1.0f && perks.isUnlocked(22)) {
                    multiplier *= agilityBoost;
                }

                // AGILITY_TRANSCENDENCE (23) — Zéphyr: dodge with 2+ enemies
                if (perks.isUnlocked(23) && PerkState.getDodgeCount(uuid) >= 2) {
                    if (!PerkState.isOnCooldown(uuid, 23, 900)) {
                        PerkState.setCooldown(uuid, 23);
                        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2, false, false));
                        attacker.setNoGravity(true);
                    }
                }

                // PRECISION_CORE (36) — +5% ranged (StatEffectApplier)

                // PRECISION_ACTIVE (37) — +5% crit (StatEffectApplier)

                // PRECISION_SYNERGY (38) — crits +25% dmg
                if (perks.isUnlocked(38) && attacker.getRandom().nextFloat() < 0.05f) {
                    multiplier += 0.25f;
                }

                // PRECISION_SITUATIONAL (39) — arrow velocity +20% (arrow entity mixin)

                // PRECISION_MASTERY (40) — Marque du Chasseur
                if (perks.isUnlocked(40)) {
                    PerkState.markTarget(uuid, target.getId(), 200);
                    if (PerkState.isTargetMarked(uuid, target.getId())) {
                        multiplier += 0.15f;
                    }
                }

                // PRECISION_TRANSCENDENCE (41) — Œil de l'Aigle
                if (perks.isUnlocked(41)) {
                    Entity direct = event.getSource().getDirectEntity();
                    if (direct instanceof Arrow) {
                        int consecutive = PerkState.getConsecutiveArrows(uuid);
                        PerkState.recordArrowShot(uuid, now, 20000);
                        if (consecutive >= 2 && !PerkState.isOnCooldown(uuid, 41, 400)) {
                            multiplier += 1.0f;
                            PerkState.setCooldown(uuid, 41);
                        }
                    } else {
                        PerkState.resetArrows(uuid);
                    }
                }

                // TRACK_SYNERGY (44) — Patience du Prédateur boost
                float trackBoost = PerkState.getNextHitBoost(uuid);
                if (trackBoost > 1.0f && perks.isUnlocked(44)) {
                    multiplier *= trackBoost;
                }

                // TRACK_SITUATIONAL (45) — Sillage: wounded mobs glow
                if (perks.isUnlocked(45)) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                }

                // TRACK_MASTERY (46) — Tactique de Meute: +5%/ally
                if (perks.isUnlocked(46)) {
                    long allies = attacker.level().players().stream()
                        .filter(p -> p != attacker && p.distanceTo(attacker) < 10).count();
                    if (allies > 0) multiplier += Math.min(allies * 0.05f, 0.25f);
                }

                // TRACK_TRANSCENDENCE (47) — Territoire: kill zone
                if (perks.isUnlocked(47) && target.getHealth() - event.getAmount() <= 0) {
                    if (!PerkState.isOnCooldown(uuid, 47, 1200)) {
                        PerkState.setCooldown(uuid, 47);
                        // buff the player for 20s
                        PerkState.setTerritoryActive(uuid, true);
                    }
                }
                if (PerkState.isTerritoryActive(uuid)) {
                    multiplier += 0.15f;
                }

                // INTIM_CORE (72) — weakness already applied in tick

                // INTIM_SYNERGY (74) — crits → Weakness I nearby 3s
                if (perks.isUnlocked(74) && attacker.getRandom().nextFloat() < 0.05f) {
                    attacker.level().getEntitiesOfClass(Mob.class,
                        attacker.getBoundingBox().inflate(6)).forEach(mob -> {
                        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
                    });
                }

                // INTIM_SITUATIONAL (75) — 10% chance flee 2s
                if (perks.isUnlocked(75) && attacker.getRandom().nextFloat() < 0.10f && target instanceof Mob mob) {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, true));
                    mob.setTarget(null);
                }

                // INTIM_TRANSCENDENCE (77) — Présence Absolue
                if (perks.isUnlocked(77) && attacker.getRandom().nextFloat() < 0.30f) {
                    if (!PerkState.isOnCooldown(uuid, 77, 600)) {
                        PerkState.setCooldown(uuid, 77);
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5, false, true));
                        attacker.level().getEntitiesOfClass(Mob.class,
                            attacker.getBoundingBox().inflate(6), m -> m != target).forEach(mob -> {
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, true));
                        });
                    }
                }

                // WILL_SYNERGY (80) — Volonté de Fer: handled in defense section
                // WILL_MASTERY (82) — Esprit Indomptable: handled below
                // WILL_TRANSCENDENCE (83) — Ascension: handled below

                // -- ACTUAL MULTIPLIER APPLICATION --
                if (multiplier != 1.0f) event.setAmount(event.getAmount() * multiplier);

                // -- KILL CHECK FOR BRUTE SITUATIONAL (3) --
                if (perks.isUnlocked(3)) {
                    PerkState.recordKill(uuid, now, 10000);
                    if (target.getHealth() - event.getAmount() <= 0) {
                        PerkState.restoreEndurance(uuid, 2);
                    }
                }

                // -- FORGE_SITUATIONAL (57) forged weapon dmg handled in mixin
            });
        }

        // --- PLAYER BEING HIT ---
        if (event.getEntity() instanceof ServerPlayer defender) {
            UUID uuid = defender.getUUID();

            CapabilityHelper.withPerks(defender, perks -> {
                float reduction = 0.0f;

                // PHYS_RESIST_CORE (24) — -1 dmg taken
                float flatReduction = perks.isUnlocked(24) ? 1.0f : 0.0f;

                // PHYS_RESIST_SYNERGY (26) — Mur de Bouclier: blocking +5%
                if (perks.isUnlocked(26) && defender.isBlocking()) {
                    reduction += 0.05f;
                }

                // PHYS_RESIST_SITUATIONAL (27) — Inébranlable: <30% HP
                if (perks.isUnlocked(27) && defender.getHealth() < defender.getMaxHealth() * 0.3f) {
                    reduction += 0.15f;
                    defender.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
                }

                // PHYS_RESIST_MASTERY (28) — Vétéran: DoT reduced 25%
                // This is checked elsewhere (on effect addition)

                // PHYS_RESIST_TRANSCENDENCE (29) — Implacable: <30% HP + hit → 75% reduc
                if (perks.isUnlocked(29) && defender.getHealth() < defender.getMaxHealth() * 0.3f) {
                    if (!PerkState.isOnCooldown(uuid, 29, 1800)) {
                        reduction += 0.75f;
                        PerkState.setCooldown(uuid, 29);
                        defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 1.5f);
                    }
                }

                // BLADE_MASTERY (10) — parry window +30% (handled in mixin)
                // BLADE_SITUATIONAL (9) — already checked in tick

                // WILL_SYNERGY (80) — Volonté de Fer: <20% HP → 15% less dmg
                if (perks.isUnlocked(80) && defender.getHealth() < defender.getMaxHealth() * 0.2f) {
                    reduction += 0.15f;
                }

                // BLADE_TRANSCENDENCE — defensively no effect

                // -- APPLY REDUCTION --
                float rawAmount = event.getAmount();
                float afterFlat = Math.max(0.5f, rawAmount - flatReduction);
                float finalAmount = afterFlat * (1.0f - Math.min(reduction, 0.9f));
                event.setAmount(finalAmount);

                // -- PERFECT PARRY COUNTER FOR BLADE_CORE(6) --
                if (perks.isUnlocked(6) && defender.isBlocking() && defender.invulnerableTime > 0
                    && defender.invulnerableTime < 10) {
                    PerkState.setParryActive(uuid, true);
                    Entity attacker = event.getSource().getEntity();
                    if (attacker instanceof LivingEntity le) {
                        le.knockback(1.5f, defender.getX() - le.getX(), defender.getZ() - le.getZ());
                    }
                }

                // -- ADRÉNALINE PHYS_ENDUR_SITUATIONAL (33) — take dmg → +5% endurance
                if (perks.isUnlocked(33) && !PerkState.isOnCooldown(uuid, 33, 200)) {
                    PerkState.restoreEndurance(uuid, 5);
                    PerkState.setCooldown(uuid, 33);
                }

                // -- WILL_MASTERY (82) — Esprit Indomptable: survive fatal hit
                if (perks.isUnlocked(82)) {
                    float afterHit = defender.getHealth() - event.getAmount();
                    if (afterHit <= 0 && !PerkState.isOnCooldown(uuid, 82, 6000)) {
                        defender.setHealth(1.0f);
                        defender.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 5, false, false));
                        defender.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
                        defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
                            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        event.setCanceled(true);
                        PerkState.setCooldown(uuid, 82);
                        return;
                    }
                }

                // -- WILL_TRANSCENDENCE (83) — Ascension: when death would strike
                if (perks.isUnlocked(83)) {
                    float afterHit = defender.getHealth() - event.getAmount();
                    if (afterHit <= 0 && PerkState.isOnCooldown(uuid, 82, 6000)) {
                        if (!PerkState.isOnCooldown(uuid, 83, 6000)) {
                            defender.setHealth(defender.getMaxHealth() * 0.10f);
                            defender.getActiveEffects().forEach(e -> {
                                if (!e.getEffect().isBeneficial())
                                    defender.removeEffect(e.getEffect());
                            });
                            defender.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 3, false, false));
                            defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
                                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
                            event.setCanceled(true);
                            PerkState.setCooldown(uuid, 83);
                        }
                    }
                }
            });
        }
    }

    // ==================== FALL ====================

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            if (perks.isUnlocked(19)) {
                event.setDistance(event.getDistance() * 0.85f);
            }
        });
    }

    // ==================== FOOD EATEN ====================

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            ItemStack item = event.getItem();
            FoodProperties food = item.getFoodProperties(player);
            if (food == null) return;

            // COOK_CORE (60) — Gourmand: food +2 hunger
            if (perks.isUnlocked(60)) {
                player.getFoodData().eat(2, 0);
            }

            // COOK_ACTIVE (61) — Régime Équilibré: 3 different foods in 2min
            if (perks.isUnlocked(61)) {
                String itemId = item.getItem().toString();
                PerkState.recordFoodEaten(player.getUUID(), itemId);
                if (PerkState.getDistinctFoods(player.getUUID(), 2400) >= 3) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
                }
            }

            // COOK_SYNERGY (62) — Touche du Chef: cooked food → random buff
            if (perks.isUnlocked(62) && food.isMeat()) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, false));
            }

            // COOK_SITUATIONAL (63) — Chef de Fer: golden food effects
            if (perks.isUnlocked(63)) {
                String itemId = item.getItem().toString();
                if (itemId.contains("golden") || itemId.contains("gilded")) {
                    float bonusSat = food.getSaturationModifier() * 0.5f;
                    player.getFoodData().setSaturation(
                        player.getFoodData().getSaturationLevel() + bonusSat);
                }
            }

            // COOK_MASTERY (64) — Repas Nourrissant: first post-combat meal
            if (perks.isUnlocked(64)) {
                boolean inCombat = !player.level().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(12), m -> m.getTarget() == player).isEmpty();
                if (!inCombat) {
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1, false, false));
                }
            }

            // COOK_TRANSCENDENCE (65) — Festin Royal: 10% share effect
            if (perks.isUnlocked(65) && player.getRandom().nextFloat() < 0.10f) {
                player.level().players().stream()
                    .filter(p -> p != player && p.distanceTo(player) < 10).forEach(p -> {
                    p.getFoodData().eat(food.getNutrition(), food.getSaturationModifier());
                });
            }

            // PHYS_RESIST_ACTIVE (25) — Estomac de Fer: food +10% saturation
            if (perks.isUnlocked(25)) {
                float bonusSat = food.getSaturationModifier() * 0.1f;
                player.getFoodData().setSaturation(
                    player.getFoodData().getSaturationLevel() + bonusSat);
            }

            // ALCHEMY_CORE (66) — handled in potion section below
        });
    }

    // ==================== POTION CONSUMED ====================

    @SubscribeEvent
    public static void onPotionConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack item = event.getItem();
        if (item.getItem() != Items.POTION && item.getItem() != Items.LINGERING_POTION
            && item.getItem() != Items.SPLASH_POTION) return;

        CapabilityHelper.withPerks(player, perks -> {
            var effects = PotionUtils.getMobEffects(item);

            // ALCHEMY_CORE (66) — +10% duration
            if (perks.isUnlocked(66)) {
                for (var effect : effects) {
                    int extended = (int) (effect.getDuration() * 1.10f);
                    player.addEffect(new MobEffectInstance(
                        effect.getEffect(), extended, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()));
                }
            }
        });
    }

    // ==================== EFFECT ADDED (status effects) ====================

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        if (PerkState.isEffectProcessing(uuid)) return;

        CapabilityHelper.withPerks(player, perks -> {
            var inst = event.getEffectInstance();

            // WILL_CORE (78) — Esprit Clair: negative effects -10% duration
            if (perks.isUnlocked(78) && !inst.getEffect().isBeneficial()) {
                int newDur = (int) (inst.getDuration() * 0.9f);
                if (newDur > 0 && PerkState.tryBeginEffectProcessing(uuid)) {
                    try {
                        player.removeEffect(inst.getEffect());
                        player.addEffect(new MobEffectInstance(inst.getEffect(), newDur,
                            inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                    } finally {
                        PerkState.finishEffectProcessing(uuid);
                    }
                }
            }

            // WILL_ACTIVE (79) — Fortitude Mentale: poison level reduced
            if (perks.isUnlocked(79) && inst.getEffect() == MobEffects.POISON && inst.getAmplifier() > 0) {
                if (PerkState.tryBeginEffectProcessing(uuid)) {
                    try {
                        player.removeEffect(MobEffects.POISON);
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, inst.getDuration(),
                            inst.getAmplifier() - 1, inst.isAmbient(), inst.isVisible()));
                    } finally {
                        PerkState.finishEffectProcessing(uuid);
                    }
                }
            }

            // PHYS_RESIST_MASTERY (28) — Vétéran: DoT reduced 25%
            if (perks.isUnlocked(28) && !inst.getEffect().isBeneficial() && inst.getEffect() == MobEffects.WITHER) {
                int newDur = (int) (inst.getDuration() * 0.75f);
                if (newDur > 0 && PerkState.tryBeginEffectProcessing(uuid)) {
                    try {
                        player.removeEffect(inst.getEffect());
                        player.addEffect(new MobEffectInstance(inst.getEffect(), newDur,
                            inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                    } finally {
                        PerkState.finishEffectProcessing(uuid);
                    }
                }
            }

            // INTIM_MASTERY (76) — Maître de la Peur: intimidation effects +50%
            if (perks.isUnlocked(76) && inst.getEffect() == MobEffects.WEAKNESS) {
                int newDur = (int) (inst.getDuration() * 1.5f);
                if (PerkState.tryBeginEffectProcessing(uuid)) {
                    try {
                        player.removeEffect(inst.getEffect());
                        player.addEffect(new MobEffectInstance(inst.getEffect(), newDur,
                            inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                    } finally {
                        PerkState.finishEffectProcessing(uuid);
                    }
                }
            }

            // KEEN_MASTERY (52) — Sens Aiguisés: all potion effects +15% duration
            if (perks.isUnlocked(52) && inst.getEffect().isBeneficial()) {
                int newDur = (int) (inst.getDuration() * 1.15f);
                if (PerkState.tryBeginEffectProcessing(uuid)) {
                    try {
                        player.removeEffect(inst.getEffect());
                        player.addEffect(new MobEffectInstance(inst.getEffect(), newDur,
                            inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
                    } finally {
                        PerkState.finishEffectProcessing(uuid);
                    }
                }
            }
        });
    }

    // ==================== ANVIL ====================

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            // FORGE_CORE (54) — -20% anvil cost
            if (perks.isUnlocked(54) && event.getCost() > 0) {
                event.setCost(Math.max(1, (int) (event.getCost() * 0.8f)));
            }

            // FORGE_MASTERY (58) — reroll enchants -25% cost
            if (perks.isUnlocked(58) && event.getCost() > 0) {
                event.setCost(Math.max(1, (int) (event.getCost() * 0.75f)));
            }
        });
    }

    // ==================== BREAK SPEED ====================

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            // FORGE_CORE (54) doesn't affect break speed
        });
    }

    // ==================== ITEM SMELTED ====================

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CapabilityHelper.withPerks(player, perks -> {
            ItemStack result = event.getSmelting();
            if (perks.isUnlocked(62)) {
                FoodProperties food = result.getFoodProperties(player);
                if (food != null && food.isMeat()) {
                    if (player.getRandom().nextFloat() < 0.3f) {
                        player.getInventory().add(result.copy());
                    }
                }
            }

            // FORGE_ACTIVE (55) — mining ores +20% yield
            if (perks.isUnlocked(55)) {
                String itemId = result.getItem().toString();
                if (itemId.contains("ore") || itemId.contains("raw_") || itemId.contains("ingot")) {
                    if (player.getRandom().nextFloat() < 0.20f) {
                        player.getInventory().add(result.copy());
                    }
                }
            }
        });
    }

    // ==================== LOGOUT CLEANUP ====================

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PerkState.clearPlayer(event.getEntity().getUUID());
    }
}
