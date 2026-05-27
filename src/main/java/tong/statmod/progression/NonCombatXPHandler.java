package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class NonCombatXPHandler {

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity killed = event.getEntity();
        if (killed instanceof EnderDragon || killed instanceof WitherBoss || killed.getMaxHealth() > 200) {
            awardXp(player, ActionType.KILL_BOSS);
        } else if (killed instanceof Monster) {
            awardXp(player, ActionType.KILL_MOB);
        }
    }

    @SubscribeEvent
    public static void onExplore(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.EXPLORE);
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.CRAFT);
            if (event.getCrafting().isEnchanted()) {
                awardXp(player, ActionType.ENCHANT);
            }
        }
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.COOK);
        }
    }

    @SubscribeEvent
    public static void onBrew(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
            && event.getLevel().getBlockEntity(event.getPos()) instanceof BrewingStandBlockEntity) {
            awardXp(player, ActionType.BREW);
        }
    }

    @SubscribeEvent
    public static void onMine(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            awardXp(player, ActionType.MINE_BLOCK);
        }
    }

    @SubscribeEvent
    public static void onDealMagicDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.MAGIC_DAMAGE);
        }
    }

    @SubscribeEvent
    public static void onGetPotionHit(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.POTION_HIT);
        }
    }

    @SubscribeEvent
    public static void onUsePotion(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.USE_ITEM);
        }
    }

    @SubscribeEvent
    public static void onTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player) || event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        if (player.tickCount % 100 == 0) {
            if (player.isInWater() || player.isUnderWater()) awardXp(player, ActionType.UNDERWATER_ACTION);
            if (player.isInLava() || player.isOnFire()) awardXp(player, ActionType.FIRE_ACTION);
            if (!player.onGround() && player.getDeltaMovement().y > 0.5) awardXp(player, ActionType.AIR_ACTION);
        }
    }

    public static void awardXp(ServerPlayer player, ActionType action) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            stats.addXp(action.primaryStat.index, action.baseXp);
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(action.primaryStat.index, stats.getLevel(action.primaryStat.index), stats.getXp(action.primaryStat.index)),
                player);
        });
    }
}
