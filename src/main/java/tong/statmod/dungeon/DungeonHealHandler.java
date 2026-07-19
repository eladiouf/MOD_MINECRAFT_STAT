package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import tong.statmod.StatMod;

import java.util.HashSet;
import java.util.Set;

/**
 * Mission M6 — Points de soin des salles de récompense (2026-07-04).
 *
 * <p>Les fontaines des salles trésor/boss ({@link DungeonRoomDressing}) enregistrent ici leur
 * position. Toutes les secondes, un joueur du donjon proche d'un point de soin reçoit
 * Régénération + Résistance courtes — un refuge pour récupérer entre les combats.
 *
 * <p>En mémoire uniquement (reconstruit à la génération de chaque étage). Ne persiste pas : c'est
 * un simple index runtime, réalimenté quand une salle est (re)générée.
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonHealHandler {

    /** Rayon (blocs) d'action d'un point de soin. */
    private static final double HEAL_RADIUS = 4.0;
    /** Période de la passe de soin (ticks). */
    private static final int PERIOD = 20;

    private static final Set<Long> HEAL_SPOTS = new HashSet<>();
    private static long serverTick = 0L;

    private DungeonHealHandler() {}

    /** Enregistre un point de soin (idempotent). Appelé à la génération d'une salle de récompense. */
    public static void registerHealSpot(ResourceKey<Level> dim, BlockPos pos) {
        if (!DungeonDimensions.TRIAL_DUNGEON.equals(dim)) return;
        HEAL_SPOTS.add(pos.asLong());
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (++serverTick % PERIOD != 0) return;
        if (HEAL_SPOTS.isEmpty()) return;

        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null || lv.players().isEmpty()) return;

        for (ServerPlayer player : lv.players()) {
            if (nearHealSpot(player.blockPosition())) {
                // Régénération II + Résistance I courtes (renouvelées tant qu'on reste).
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false));

                // Intégration PlayerRevive : réanimation près des feux de camp / fontaines.
                tong.statmod.integration.playerrevive.PlayerReviveIntegration.tryHealSpotRevive(player);
            }
        }
    }

    private static boolean nearHealSpot(BlockPos p) {
        for (long packed : HEAL_SPOTS) {
            BlockPos spot = BlockPos.of(packed);
            if (spot.distToCenterSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)
                    <= HEAL_RADIUS * HEAL_RADIUS) {
                return true;
            }
        }
        return false;
    }
}
