package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import tong.statmod.dungeon.DungeonXpMultiplier;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.SoundHelper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public class CombatXPHandler {

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        Player player = resolveAttacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (player == null || player.level().isClientSide) return;

        LivingEntity target = event.getEntity();
        int xp = Math.max(1, Math.round(target.getMaxHealth() * 1.5f));
        // Mission M6 — bonus XP dans le Trial Dungeon proportionnel à l'étage.
        xp = DungeonXpMultiplier.applyToXp(xp, player.level().dimension(), player.getBlockX(), player.getBlockZ());
        StatType stat = resolveWeaponStat(player.getMainHandItem());

        PlayerStatData data = player.getData(ModAttachments.STATS);
        boolean leveled = RaceEffectApplier.addScaledXp(player, stat.index, xp, data, true);
        if (leveled) SoundHelper.playLevelUp((ServerPlayer) player);
        SyncHelper.syncStats((ServerPlayer) player);
        if (wasThreateningTarget(target, player)) {
            int mp = 1;
            data.addMagicPoints(mp);
            SyncHelper.syncMagic((ServerPlayer) player);
        }

        // Bonus TRACKING pour les kills longue distance (>= 8 blocs).
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            double distance = player.distanceTo(target);
            DefensiveXPHandler.awardTrackingForRangedKill(player, target, distance);
        }
    }

    public static Player resolveAttacker(Entity source, Entity direct) {
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

    private static boolean wasThreateningTarget(LivingEntity target, Player player) {
        return target instanceof Mob mob && mob.getTarget() == player
                || target.getLastHurtMob() == player;
    }
}
