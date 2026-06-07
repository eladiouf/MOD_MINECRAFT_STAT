package tong.statmod.world;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.reload.BossRewardReloadListener;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class BossLootHandler {

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityType == null) return;

        BossRewardReloadListener.INSTANCE.getReward(entityType).ifPresent(reward -> {
            for (ItemStack stack : reward.items()) {
                entity.spawnAtLocation(stack.copy());
            }
            CapabilityHelper.withStats(player, stats -> {
                for (StatType s : StatType.values()) {
                    stats.addXp(s.index, reward.xpPerStat());
                }
            });
            player.sendSystemMessage(Component.literal(reward.message()));
        });
    }
}
