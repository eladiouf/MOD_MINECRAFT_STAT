package tong.statmod.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.item.ModItems;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class BossLootHandler {

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        if (entity instanceof EnderDragon) {
            entity.spawnAtLocation(new ItemStack(ModItems.STAT_SCROLL.get(), 5));
            entity.spawnAtLocation(new ItemStack(ModItems.PERK_TOME.get(), 3));
            entity.spawnAtLocation(new ItemStack(ModItems.MASTERY_CRYSTAL.get(), 3));
            CapabilityHelper.withStats(player, stats -> {
                for (StatType s : StatType.values()) {
                    stats.addXp(s.index, 500);
                }
            });
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§5Dragon Slain! §6+500 XP all stats!"));
        }

        if (entity instanceof WitherBoss) {
            entity.spawnAtLocation(new ItemStack(ModItems.STAT_SCROLL.get(), 3));
            entity.spawnAtLocation(new ItemStack(ModItems.PERK_TOME.get(), 2));
            entity.spawnAtLocation(new ItemStack(ModItems.MASTERY_CRYSTAL.get(), 2));
            CapabilityHelper.withStats(player, stats -> {
                for (StatType s : StatType.values()) {
                    stats.addXp(s.index, 250);
                }
            });
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§5Wither Defeated! §6+250 XP all stats!"));
        }

        if (entity instanceof Warden) {
            entity.spawnAtLocation(new ItemStack(ModItems.STAT_SCROLL.get(), 2));
            entity.spawnAtLocation(new ItemStack(ModItems.PERK_TOME.get(), 1));
            entity.spawnAtLocation(new ItemStack(ModItems.MASTERY_CRYSTAL.get(), 2));
            CapabilityHelper.withStats(player, stats -> {
                for (StatType s : StatType.values()) {
                    stats.addXp(s.index, 100);
                }
            });
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§5Warden Vanquished! §6+100 XP all stats!"));
        }
    }
}
