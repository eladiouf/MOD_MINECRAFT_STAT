package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;

@EventBusSubscriber(modid = STATMod.MODID)
public class CombatXPHandler {

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        Player player = resolveAttacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (player == null || player.level().isClientSide) return;

        LivingEntity target = event.getEntity();
        int xp = Math.max(1, Math.round(target.getMaxHealth() * 1.5f));
        StatType stat = resolveWeaponStat(player.getMainHandItem());

        boolean leveled = RaceEffectApplier.addScaledXp(player, stat.index, xp, player.getData(ModAttachments.STATS), true);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);

        // Bonus TRACKING pour les kills longue distance (>= 8 blocs).
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            double distance = player.distanceTo(target);
            DefensiveXPHandler.awardTrackingForRangedKill(player, target, distance);
        }
    }

    static Player resolveAttacker(Entity source, Entity direct) {
        if (source instanceof Player player) {
            return player;
        }
        if (source instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }

    static StatType resolveWeaponStat(ItemStack stack) {
        return WeaponResolver.statFor(stack);
    }
}
