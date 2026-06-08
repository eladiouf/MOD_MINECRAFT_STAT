package tong.statmod.combat;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import tong.statmod.stats.StatType;

/**
 * Reduces damage from L2Hostility traits according to the player's stats.
 * Registered manually only when l2hostility is loaded.
 */
public class L2HPlayerResistance {

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof Mob mob)) return;

        MobTraitCap traitCap = MobTraitCap.HOLDER.get(mob);
        if (traitCap == null) return;

        CapabilityHelper.withStats(player, stats -> {
            float reduction = computeReduction(stats, traitCap);
            if (reduction > 0) {
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        });
    }

    private static float computeReduction(PlayerStats stats, MobTraitCap traitCap) {
        float total = 0f;
        for (var trait : traitCap.traits.keySet()) {
            ResourceLocation id = trait.getRegistryName();
            if (id == null) continue;
            total += switch (id.getPath()) {
                case "fiery"       -> stats.getLevel(StatType.FIRE_AFFINITY.index)      / 200.0f;
                case "drain"       -> stats.getLevel(StatType.ARCANE_POWER.index)       / 200.0f;
                case "killer_aura" -> stats.getLevel(StatType.MAGIC_RESISTANCE.index)   / 200.0f;
                case "corrosion"   -> stats.getLevel(StatType.ALCHEMY.index)            / 200.0f;
                case "dementor"    -> stats.getLevel(StatType.WILLPOWER.index)          / 100.0f;
                case "gravity"     -> stats.getLevel(StatType.PHYSICAL_RESISTANCE.index)/ 200.0f;
                case "arena"       -> stats.getLevel(StatType.WILLPOWER.index)          / 150.0f;
                default            -> 0f;
            };
        }
        return Math.min(total, 0.75f);
    }
}
