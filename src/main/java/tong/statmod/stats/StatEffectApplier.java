package tong.statmod.stats;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import tong.statmod.STATMod;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

@EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        float dmg = event.getNewDamage();

        if (event.getSource().getEntity() instanceof Player attacker) {
            PlayerStatData data = attacker.getData(ModAttachments.STATS);
            int brute = data.getLevel(StatType.BRUTE_FORCE.index);
            int blade = data.getLevel(StatType.BLADE_TECHNIQUE.index);
            dmg *= 1.0f + (brute + blade) * 0.005f;
        }

        if (event.getEntity() instanceof Player victim) {
            PlayerStatData data = victim.getData(ModAttachments.STATS);
            int phys = data.getLevel(StatType.PHYSICAL_RESISTANCE.index);
            dmg *= 1.0f - Math.min(0.5f, phys * 0.005f);
        }

        if (Math.abs(dmg - event.getNewDamage()) > 0.001f) {
            event.setNewDamage(Math.max(0.0f, dmg));
        }
    }
}
