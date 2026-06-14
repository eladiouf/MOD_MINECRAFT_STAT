package tong.statmod.perks;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.network.SyncBus;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.UUID;

/**
 * Applique les effets des perks via les events vanilla.
 * Subset MVP des 84 perks — les autres sont gardés en TODO pour itération ultérieure.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class PerkEffectHandler {
    private PerkEffectHandler() {}

    private static PerkManager managerFor(Player player) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkManager manager = new PerkManager(data);
        manager.setFromIds(SyncBus.cachedUnlockedIds(player.getUUID()));
        return manager;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        PerkManager perks = managerFor(player);
        UUID uuid = player.getUUID();

        // ENDUR_CORE id=30: +2 absorption hearts (4 absorption units)
        if (perks.isUnlocked(Perk.byId(30)) && player.getAbsorptionAmount() < 4f) {
            player.setAbsorptionAmount(4f);
        }

        // TRACK_CORE id=42: Glow nearby mobs periodically
        if (perks.isUnlocked(Perk.byId(42)) && player.tickCount % 40 == 0) {
            player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    player.getBoundingBox().inflate(16), LivingEntity::isAlive)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false)));
        }

        // BLADE_ACTIVE id=7: record combo hits for Flowing Strike
        if (perks.isUnlocked(Perk.byId(7))) {
            PerkState.recordComboHit(uuid, System.currentTimeMillis(), 5000);
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        float dmg = event.getNewDamage();

        // Attacker side
        if (event.getSource().getEntity() instanceof Player attacker) {
            PerkManager perks = managerFor(attacker);

            if (perks.isUnlocked(Perk.byId(0))) dmg *= 1.05f;   // BRUTE_CORE
            if (perks.isUnlocked(Perk.byId(6))) dmg *= 1.05f;   // BLADE_CORE
            if (perks.isUnlocked(Perk.byId(72))                 // INTIM_CORE
                    && PerkState.isTrackedTarget(attacker.getUUID(), event.getEntity().getId())) {
                dmg *= 1.05f;
            }
            // BRUTE_SITUATIONAL id=3 : Berserker — +20% below 30% HP
            if (perks.isUnlocked(Perk.byId(3))
                    && attacker.getHealth() < attacker.getMaxHealth() * 0.3f) {
                dmg *= 1.20f;
            }
        }

        // Victim side
        if (event.getEntity() instanceof Player victim) {
            PerkManager perks = managerFor(victim);
            UUID uuid = victim.getUUID();

            if (perks.isUnlocked(Perk.byId(24))) dmg *= 0.95f;  // RESIST_CORE

            // RESIST_SITUATIONAL id=27: Last Stand
            if (perks.isUnlocked(Perk.byId(27)) && victim.getHealth() < victim.getMaxHealth() * 0.2f) {
                dmg *= 0.7f;
            }

            // WILL_SITUATIONAL id=81: Last Breath — survive at 1 HP once per 30s
            if (perks.isUnlocked(Perk.byId(81))
                    && !PerkState.isOnCooldown(uuid, 81, 30_000L)
                    && victim.getHealth() - dmg <= 0) {
                dmg = Math.max(0f, victim.getHealth() - 1f);
                PerkState.setCooldown(uuid, 81, 30_000L);
            }

            // RESIST_TRANSCENDENCE id=29: Immortal — once per 30s
            if (perks.isUnlocked(Perk.byId(29))
                    && !PerkState.isOnCooldown(uuid, 29, 30_000L)
                    && victim.getHealth() - dmg <= 0) {
                dmg = Math.max(0f, victim.getHealth() - 1f);
                victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, false));
                PerkState.setCooldown(uuid, 29, 30_000L);
            }

            // RESIST_MASTERY id=28: Diamond Skin — brief Resistance after hit
            if (perks.isUnlocked(Perk.byId(28)) && !PerkState.isOnCooldown(uuid, 28, 5_000L)) {
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, false, false));
                PerkState.setCooldown(uuid, 28, 5_000L);
            }
        }

        if (Math.abs(dmg - event.getNewDamage()) > 0.001f) {
            event.setNewDamage(Math.max(0f, dmg));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        PerkManager perks = managerFor(player);
        UUID uuid = player.getUUID();
        PerkState.recordKill(uuid);

        // ENDUR_ACTIVE id=31: Second Wind
        if (perks.isUnlocked(Perk.byId(31))) {
            player.setAbsorptionAmount(Math.min(20f, player.getAbsorptionAmount() + 4f));
        }
        // ENDUR_SITUATIONAL id=33: Adrenaline
        if (perks.isUnlocked(Perk.byId(33))) {
            FoodData food = player.getFoodData();
            food.setFoodLevel(Math.min(20, food.getFoodLevel() + 2));
            food.setSaturation(Math.min(20f, food.getSaturationLevel() + 2f));
        }
        // BLADE_MASTERY id=10: Blade Storm AOE on kill
        if (perks.isUnlocked(Perk.byId(10))) {
            player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(4),
                    e -> e != player && e.isAlive() && e != event.getEntity())
                    .forEach(e -> e.hurt(player.damageSources().mobAttack(player), 4f));
        }
    }
}
