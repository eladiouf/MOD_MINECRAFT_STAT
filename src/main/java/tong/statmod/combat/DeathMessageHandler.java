package tong.statmod.combat;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class DeathMessageHandler {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        Component msg = StatDeathMessage.getCustomDeathMessage(event.getEntity(), event.getSource());
        if (msg != null) {
            event.getEntity().sendSystemMessage(msg);
        }
    }
}
