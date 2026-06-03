package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class NonCombatXPHandler {

    // ---- Tracking / Intimidation / Willpower ----

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity killed = event.getEntity();

        if (killed instanceof EnderDragon || killed instanceof WitherBoss || killed.getMaxHealth() > 200) {
            ActionXpHelper.awardXp(player, StatType.INTIMIDATION.index, ActionXpHelper.XpTier.RARE);
            ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.RARE);
        } else if (killed instanceof Monster) {
            ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    // ---- Keen Senses ----

    @SubscribeEvent
    public static void onExplore(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ActionXpHelper.awardXp(player, StatType.KEEN_SENSES.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }
    }

    // ---- Forging / Erudition ----

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ActionXpHelper.awardXp(player, StatType.FORGING.index, ActionXpHelper.XpTier.COMMON);
            if (event.getCrafting().isEnchanted()) {
                ActionXpHelper.awardXp(player, StatType.ERUDITION.index, ActionXpHelper.XpTier.INTERMEDIATE);
            }
        }
    }

    // ---- Cooking ----

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ActionXpHelper.awardXp(player, StatType.COOKING.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    // ---- Alchemy ----
    // XP from drinking potions only (see onDrinkPotion). Removed click-on-brewingstand exploit.

    // ---- Earth Affinity (mining) + Brute Force (hard blocks) ----

    @SubscribeEvent
    public static void onMine(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        if (event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (event.getPos().getY() < 0) {
                ActionXpHelper.awardXp(player, StatType.EARTH_AFFINITY.index, ActionXpHelper.XpTier.INTERMEDIATE);
            } else {
                ActionXpHelper.awardXp(player, StatType.EARTH_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
            }
        }

        // Brute Force: breaking hard blocks
        if (event.getState().is(Blocks.OBSIDIAN) || event.getState().is(Blocks.CRYING_OBSIDIAN)
            || event.getState().is(Blocks.ANCIENT_DEBRIS) || event.getState().is(Blocks.NETHERITE_BLOCK)) {
            ActionXpHelper.awardXp(player, StatType.BRUTE_FORCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }
    }

    // ---- Magic stats ----

    @SubscribeEvent
    public static void onMagicDamage(LivingDamageEvent event) {
        boolean isMagic = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
            || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
        if (!isMagic) return;

        // Caster gains XP
        if (event.getSource().getEntity() instanceof ServerPlayer caster) {
            ActionXpHelper.awardXp(caster, StatType.ARCANE_POWER.index, ActionXpHelper.XpTier.COMMON);
            ActionXpHelper.awardXp(caster, StatType.MANA_POOL.index, ActionXpHelper.XpTier.COMMON);
            ActionXpHelper.awardXp(caster, StatType.CASTING_SPEED.index, ActionXpHelper.XpTier.COMMON);
            ActionXpHelper.awardXp(caster, StatType.ERUDITION.index, ActionXpHelper.XpTier.COMMON);
            if (event.getAmount() >= 15) {
                ActionXpHelper.awardXp(caster, StatType.ARCANE_POWER.index, ActionXpHelper.XpTier.INTERMEDIATE);
                ActionXpHelper.awardXp(caster, StatType.MANA_POOL.index, ActionXpHelper.XpTier.INTERMEDIATE);
            }
        }

        // Target gains resistance XP
        if (event.getEntity() instanceof ServerPlayer target) {
            ActionXpHelper.awardXp(target, StatType.MAGIC_RESISTANCE.index, ActionXpHelper.XpTier.COMMON);
            ActionXpHelper.awardXp(target, StatType.WILLPOWER.index, ActionXpHelper.XpTier.COMMON);
            if (event.getAmount() >= 10) {
                ActionXpHelper.awardXp(target, StatType.MAGIC_RESISTANCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
            }
        }
    }

    @SubscribeEvent
    public static void onGetPotionHit(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ActionXpHelper.awardXp(player, StatType.WILLPOWER.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    // ---- Elemental tick actions ----

    @SubscribeEvent
    public static void onTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player) || event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        if (player.tickCount % 100 != 0) return;

        if (player.isInWater() || player.isUnderWater()) {
            ActionXpHelper.awardXp(player, StatType.WATER_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
        }
        if (player.isInLava() || player.isOnFire()) {
            ActionXpHelper.awardXp(player, StatType.FIRE_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
        }
        if (!player.onGround() && player.getDeltaMovement().y > 0.5) {
            ActionXpHelper.awardXp(player, StatType.AIR_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
        }
    }
}
