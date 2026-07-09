package tong.statmod.integration.epicfight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.progression.WeaponResolver;
import tong.statmod.stamina.StaminaManager;
import tong.statmod.stamina.StaminaRules;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.subscription.DefaultEventSubscription;
import yesman.epicfight.api.event.types.entity.ApplyStunEvent;
import yesman.epicfight.api.event.types.entity.DealDamageEvent;
import yesman.epicfight.api.event.types.entity.DodgeEvent;
import yesman.epicfight.api.event.types.entity.KillEntityEvent;
import yesman.epicfight.api.event.types.entity.ModifyAttackSpeedEvent;
import yesman.epicfight.api.event.types.entity.ModifyBaseDamageEvent;
import yesman.epicfight.api.event.types.entity.StunnedEvent;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.api.event.types.player.ComboAttackEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.skill.Skill;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class EpicFightCompat {
    private static boolean loaded = false;
    private static final Map<UUID, Integer> lastWeight = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastStunArmor = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastImpact = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> lastReach = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> lastServerMaxStamina = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> lastClientMaxStamina = new ConcurrentHashMap<>();
    private static final class ResourceIds {
        private static final net.minecraft.resources.ResourceLocation MAX_STAMINA =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "epicfight_max_stamina");
        private static final net.minecraft.resources.ResourceLocation WEIGHT =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "epicfight_weight");
        private static final net.minecraft.resources.ResourceLocation STUN_ARMOR =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "epicfight_stun_armor");
        private static final net.minecraft.resources.ResourceLocation IMPACT =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "epicfight_impact");
        private static final net.minecraft.resources.ResourceLocation REACH =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "epicfight_reach");
    }

    private EpicFightCompat() {}

    public static void init() {
        loaded = ModList.get().isLoaded("epicfight");
        if (!loaded) {
            STATMod.LOGGER.info("Epic Fight not detected, skipping EpicFightCompat");
            return;
        }
        registerHooks();
        NeoForge.EVENT_BUS.register(EpicFightCompat.class);
        STATMod.LOGGER.info("Epic Fight integration loaded");
    }

    private static void registerHooks() {
        EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE.registerEvent(
            new DefaultEventSubscription<DealDamageEvent.Pre>() {
                @Override
                public void fire(DealDamageEvent.Pre event) {
                    onDealDamage(event);
                }
            }
        );
        EpicFightEventHooks.Entity.MODIFY_ATTACK_DAMAGE.registerEvent(
            new DefaultEventSubscription<ModifyBaseDamageEvent>() {
                @Override
                public void fire(ModifyBaseDamageEvent event) {
                    onModifyAttackDamage(event);
                }
            }
        );
        EpicFightEventHooks.Entity.MODIFY_ATTACK_SPEED.registerEvent(
            new DefaultEventSubscription<ModifyAttackSpeedEvent>() {
                @Override
                public void fire(ModifyAttackSpeedEvent event) {
                    onModifyAttackSpeed(event);
                }
            }
        );
        EpicFightEventHooks.Entity.APPLY_STUN.registerEvent(
            new DefaultEventSubscription<ApplyStunEvent>() {
                @Override
                public void fire(ApplyStunEvent event) {
                    onApplyStun(event);
                }
            }
        );
        EpicFightEventHooks.Entity.ON_STUNNED.registerEvent(
            new DefaultEventSubscription<StunnedEvent>() {
                @Override
                public void fire(StunnedEvent event) {
                    onStunned(event);
                }
            }
        );
        EpicFightEventHooks.Entity.KILL_ENTITY.registerEvent(
            new DefaultEventSubscription<KillEntityEvent>() {
                @Override
                public void fire(KillEntityEvent event) {
                    onKill(event);
                }
            }
        );
        EpicFightEventHooks.Entity.ON_DODGE.registerEvent(
            new DefaultEventSubscription<DodgeEvent>() {
                @Override
                public void fire(DodgeEvent event) {
                    onDodge(event);
                }
            }
        );
        EpicFightEventHooks.Player.COMBO_ATTACK.registerEvent(
            new DefaultEventSubscription<ComboAttackEvent>() {
                @Override
                public void fire(ComboAttackEvent event) {
                    onCombo(event);
                }
            }
        );
        EpicFightEventHooks.Player.CONSUME_SKILL.registerEvent(
            new DefaultEventSubscription<SkillConsumeEvent>() {
                @Override
                public void fire(SkillConsumeEvent event) {
                    onConsumeSkill(event);
                }
            }
        );
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        syncEpicFightStaminaDisplay(player);
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        UUID uuid = player.getUUID();
        applyAttribute(player, EpicFightAttributes.WEIGHT, ResourceIds.WEIGHT,
                RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index),
                lastWeight, uuid, weightModifierAmount(
                        RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index)));
        applyAttribute(player, EpicFightAttributes.STUN_ARMOR, ResourceIds.STUN_ARMOR,
                RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index) +
                        RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index),
                lastStunArmor, uuid, stunArmorModifierAmount(
                        RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index)
                                + RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index)));
        applyAttribute(player, EpicFightAttributes.IMPACT, ResourceIds.IMPACT,
                RaceEffectApplier.getEffectiveLevel(player, StatType.BRUTE_FORCE.index),
                lastImpact, uuid, impactModifierAmount(
                        RaceEffectApplier.getEffectiveLevel(player, StatType.BRUTE_FORCE.index)));
        applyFlatAttribute(player, Attributes.ENTITY_INTERACTION_RANGE, ResourceIds.REACH,
                EpicFightWeaponReachHandler.reachBonus(player), lastReach, uuid);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        clearCaches(event.getEntity().getUUID());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearCaches(event.getEntity().getUUID());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        clearCaches(event.getOriginal().getUUID());
        clearCaches(event.getEntity().getUUID());
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        clearCaches(event.getEntity().getUUID());
    }

    private static void onDealDamage(DealDamageEvent.Pre event) {
        LivingEntity source = event.getEntityPatch().getOriginal();
        if (!(source instanceof Player player)) return;
        if (player.level().isClientSide) return;

        EpicFightDamageSource ds = event.getDamageSource();
        if (EpicFightExecuteHandler.shouldExecute(player, event.getTarget())) {
            ds.setExecute();
        }
        event.setModifiedDamage(event.getModifiedDamage() * airAttackMultiplier(
                RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index),
                !player.onGround()));
        var used = ds.getUsedItem();
        boolean leveled = false;
        int dmg = Math.round(event.getOriginalDamage());

        if (used != null) {
            CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(used);
            if (cap != null) {
                WeaponCategory cat = cap.getWeaponCategory();
                if (cat == CapabilityItem.WeaponCategories.SWORD
                        || cat == CapabilityItem.WeaponCategories.LONGSWORD
                        || cat == CapabilityItem.WeaponCategories.UCHIGATANA
                        || cat == CapabilityItem.WeaponCategories.DAGGER
                        || cat == CapabilityItem.WeaponCategories.TACHI) {
                    leveled = addXp(player, StatType.BLADE_TECHNIQUE, dmg);
                } else if (cat == CapabilityItem.WeaponCategories.AXE
                        || cat == CapabilityItem.WeaponCategories.GREATSWORD) {
                    leveled = addXp(player, StatType.BRUTE_FORCE, dmg);
                } else if (cat == CapabilityItem.WeaponCategories.SPEAR
                        || cat == CapabilityItem.WeaponCategories.TRIDENT) {
                    leveled = addXp(player, StatType.PRECISION, dmg);
                } else if (cat == CapabilityItem.WeaponCategories.RANGED) {
                    leveled = addXp(player, StatType.PRECISION, dmg);
                } else if (cat == CapabilityItem.WeaponCategories.FIST) {
                    leveled = addXp(player, StatType.BRUTE_FORCE, Math.round(dmg / 2f));
                    leveled |= addXp(player, StatType.AGILITY, Math.round(dmg / 2f));
                } else {
                    leveled = addXp(player, WeaponResolver.statFor(used), dmg);
                }
            } else {
                leveled = addXp(player, WeaponResolver.statFor(used), dmg);
            }
        }

        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }

    private static void onModifyAttackDamage(ModifyBaseDamageEvent event) {
        LivingEntity source = event.getEntityPatch().getOriginal();
        if (!(source instanceof Player player)) return;
        if (player.level().isClientSide) return;

        int brute = RaceEffectApplier.getEffectiveLevel(player, StatType.BRUTE_FORCE.index);
        int blade = RaceEffectApplier.getEffectiveLevel(player, StatType.BLADE_TECHNIQUE.index);
        int precision = RaceEffectApplier.getEffectiveLevel(player, StatType.PRECISION.index);
        int rapidite = RaceEffectApplier.getEffectiveLevel(player, StatType.RAPIDITE.index);

        float multiplier = 1.0f
                + brute * 0.008f
                + blade * 0.006f
                + precision * 0.004f
                + rapidite * 0.003f;

        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        float statModMax = StaminaRules.maxStamina(endurance);
        multiplier *= EpicFightStaminaBridge.damageMultiplier(
                StaminaRules.threshold(player.getData(ModAttachments.STAMINA).currentStamina(), statModMax));

        event.attachValueModifier(ValueModifier.multiplier(multiplier));
    }

    private static void onModifyAttackSpeed(ModifyAttackSpeedEvent event) {
        Player player = event.getEntityPatch().getOriginal() instanceof Player p ? p : null;
        if (player == null || player.level().isClientSide) return;

        int rapidite = RaceEffectApplier.getEffectiveLevel(player, StatType.RAPIDITE.index);
        int agility = RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index);
        float multiplier = 1.0f + rapidite * 0.004f + agility * 0.002f;
        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        float statModMax = StaminaRules.maxStamina(endurance);
        multiplier *= EpicFightStaminaBridge.attackSpeedMultiplier(
                StaminaRules.threshold(player.getData(ModAttachments.STAMINA).currentStamina(), statModMax));
        event.setAttackSpeed(event.getAttackSpeed() * multiplier);
    }

    private static void onApplyStun(ApplyStunEvent event) {
        LivingEntity source = event.getEntityPatch().getOriginal();
        if (!(source instanceof Player player)) return;
        if (player.level().isClientSide) return;

        float multiplier = EpicFightStunResistanceHandler.stunTimeMultiplier(
                RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index),
                RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index),
                event.getStunType());
        event.setStunTime(event.getStunTime() * multiplier);
    }

    private static void onStunned(StunnedEvent event) {
        LivingEntity source = event.getEntityPatch().getOriginal();
        if (!(source instanceof Player player)) return;
        if (player.level().isClientSide) return;

        if (EpicFightHyperArmorHandler.shouldNegateStun(
                RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index),
                RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index),
                event.getStunType())) {
            event.cancel();
        }
    }

    private static void onKill(KillEntityEvent event) {
        LivingEntity killer = event.getEntityPatch().getOriginal();
        if (!(killer instanceof Player player)) return;
        if (player.level().isClientSide) return;

        LivingEntity target = event.getKilledEntity();
        int xp = Math.max(1, Math.round(target.getMaxHealth() * 0.5f));
        boolean leveled = addXp(player, StatType.INTIMIDATION, xp);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }

    private static void onCombo(ComboAttackEvent event) {
        Player player = event.getPlayerPatch().getOriginal();
        if (player.level().isClientSide) return;

        boolean leveled = addXp(player, StatType.RAPIDITE, 3);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }

    private static void onConsumeSkill(SkillConsumeEvent event) {
        Player player = event.getEntityPatch().getOriginal() instanceof Player p ? p : null;
        if (player == null) return;

        float multiplier = EpicFightCooldownHandler.resourceMultiplier(
                RaceEffectApplier.getEffectiveLevel(player, StatType.RAPIDITE.index),
                RaceEffectApplier.getEffectiveLevel(player, StatType.CASTING_SPEED.index),
                RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index),
                event.getResourceType());
        if (event.getResourceType() == Skill.Resource.STAMINA) {
            if (player.level().isClientSide) {
                int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
                float statModMax = StaminaRules.maxStamina(endurance);
                float adjustedAmount = EpicFightStaminaBridge.sustainableSkillCost(
                        event.getAmount(), multiplier, statModMax);
                StaminaManager.consume(player.getData(ModAttachments.STAMINA), adjustedAmount);
                event.setAmount(0.0f);
                return;
            }
            int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
            float statModMax = StaminaRules.maxStamina(endurance);
            float adjustedAmount = EpicFightStaminaBridge.sustainableSkillCost(
                    event.getAmount(), multiplier, statModMax);
            var stamina = player.getData(ModAttachments.STAMINA);
            var threshold = StaminaRules.threshold(stamina.currentStamina(), statModMax);
            if (!EpicFightStaminaBridge.canUseSkill(threshold, stamina.currentStamina(), adjustedAmount)) {
                event.cancel();
                return;
            }
            StaminaManager.consume(stamina, adjustedAmount);
            syncEpicFightStaminaDisplay(player);
            event.setAmount(0.0f);
            SyncHelper.syncStamina((ServerPlayer) player);
            return;
        }
        if (player.level().isClientSide) return;
        float adjustedAmount = event.getAmount() * multiplier;
        event.setAmount(adjustedAmount);
    }

    private static void syncEpicFightStaminaDisplay(Player player) {
        var patch = EpicFightCapabilities.getPlayerPatch(player);
        if (patch == null) {
            return;
        }
        int endurance = RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        float statModMax = StaminaRules.maxStamina(endurance);
        Map<UUID, Float> maxStaminaCache = player.level().isClientSide ? lastClientMaxStamina : lastServerMaxStamina;
        applyEpicFightMaxStamina(player, statModMax, player.getUUID(), maxStaminaCache);
        float display = EpicFightStaminaBridge.patchDisplayStamina(
                player.getData(ModAttachments.STAMINA).currentStamina(),
                statModMax,
                patch.getMaxStamina());
        patch.setStamina(display);
    }

    private static void applyEpicFightMaxStamina(Player player, float statModMax, UUID uuid,
                                                 Map<UUID, Float> maxStaminaCache) {
        Float previous = maxStaminaCache.get(uuid);
        if (previous != null && Float.compare(previous, statModMax) == 0) {
            return;
        }

        var instance = player.getAttribute(EpicFightAttributes.MAX_STAMINA);
        if (instance == null) {
            return;
        }

        instance.removeModifier(ResourceIds.MAX_STAMINA);
        double amount = statModMax - instance.getValue();
        if (Math.abs(amount) > 1.0e-6d) {
            instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    ResourceIds.MAX_STAMINA, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
        maxStaminaCache.put(uuid, statModMax);
    }

    public static float airAttackMultiplier(int agility, boolean airborne) {
        if (!airborne) {
            return 1.0f;
        }
        return 1.0f + Math.max(0, agility) * 0.005f;
    }

    static double weightModifierAmount(int endurance) {
        return -Math.max(0, endurance) * 0.02d;
    }

    static double impactModifierAmount(int bruteForce) {
        return Math.max(0, bruteForce) * 0.02d;
    }

    static double stunArmorModifierAmount(int totalDefense) {
        return Math.max(0, totalDefense) * 0.01d;
    }

    private static void applyAttribute(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                       net.minecraft.resources.ResourceLocation id, int level,
                                       Map<UUID, Integer> cache, UUID uuid, double amount) {
        Integer previous = cache.get(uuid);
        if (previous != null && previous == level) return;

        var instance = player.getAttribute(attribute);
        if (instance == null) return;

        instance.removeModifier(id);
        if (level > 0) {
            instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    id, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        if (level == 0) {
            cache.remove(uuid);
        } else {
            cache.put(uuid, level);
        }
    }

    private static void applyFlatAttribute(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                           net.minecraft.resources.ResourceLocation id, float amount,
                                           Map<UUID, Float> cache, UUID uuid) {
        Float previous = cache.get(uuid);
        if (previous != null && Float.compare(previous, amount) == 0) return;

        var instance = player.getAttribute(attribute);
        if (instance == null) return;

        instance.removeModifier(id);
        if (amount > 0.0f) {
            instance.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    id, amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            cache.put(uuid, amount);
        } else {
            cache.remove(uuid);
        }
    }

    private static void onDodge(DodgeEvent event) {
        LivingEntity source = event.getEntityPatch().getOriginal();
        if (!(source instanceof Player player)) return;
        if (player.level().isClientSide) return;

        boolean leveled = addXp(player, StatType.AGILITY, 5);
        // KEEN_SENSES — esquiver demande de la perception, pas juste de l'agilité.
        tong.statmod.progression.DefensiveXPHandler.awardKeenSensesForDodge(player, 3);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
    }

    private static boolean addXp(Player player, StatType stat, int amount) {
        return RaceEffectApplier.addScaledXp(player, stat.index, amount,
                player.getData(ModAttachments.STATS), true);
    }

    private static void clearCaches(UUID uuid) {
        lastWeight.remove(uuid);
        lastStunArmor.remove(uuid);
        lastImpact.remove(uuid);
        lastReach.remove(uuid);
        lastServerMaxStamina.remove(uuid);
        lastClientMaxStamina.remove(uuid);
    }
}
