package tong.statmod.dungeon;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;

/**
 * Mission M6 — Ambiance atmosphérique par thème (2026-07-05).
 *
 * <p>Fait « vivre » l'étage avec quelques particules d'ambiance choisies selon le thème/arc
 * (braises en enfer, flocons en glace, spores en jungle, motes du End dans l'abysse…). Envoyées
 * autour de chaque joueur du donjon, toutes les {@value #TICKS} ticks — léger, purement cosmétique.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonAmbience {

    private static final int TICKS = 10;         // intervalle
    private static final int PER_BURST = 3;      // particules par joueur et par passe
    private static final double RADIUS = 10.0;   // rayon autour du joueur

    private static int tick = 0;

    private DungeonAmbience() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tick % TICKS != 0) return;
        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null || lv.players().isEmpty()) return;

        RandomSource rng = lv.random;
        for (ServerPlayer p : lv.players()) {
            int floor = DungeonTeleportHandler.floorAtPos(p.getBlockX(), p.getBlockZ());
            ThemePalette theme = ThemePalette.forFloor(floor);
            ParticleOptions particle = particleFor(theme);
            Vec3 base = p.position();
            for (int i = 0; i < PER_BURST; i++) {
                double x = base.x + (rng.nextDouble() - 0.5) * 2 * RADIUS;
                double y = base.y + rng.nextDouble() * 4.0;
                double z = base.z + (rng.nextDouble() - 0.5) * 2 * RADIUS;
                lv.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }

            // Periodically play themed ambient sounds (~8% chance per tick pass)
            if (rng.nextFloat() < 0.08f) {
                net.minecraft.sounds.SoundEvent sound = soundFor(theme);
                if (sound != null) {
                    lv.playSound(null, p.getX(), p.getY(), p.getZ(), sound, net.minecraft.sounds.SoundSource.AMBIENT, 0.5F, 0.8F + rng.nextFloat() * 0.4F);
                }
            }
        }
    }

    private static net.minecraft.sounds.SoundEvent soundFor(ThemePalette t) {
        return switch (t) {
            case FOURNAISE, TRIBUS -> net.minecraft.sounds.SoundEvents.LAVA_AMBIENT;
            case DECHARNES, LEGION -> net.minecraft.sounds.SoundEvents.WITHER_AMBIENT;
            case MAGES, GESTE, NEANT -> net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT;
            default -> net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value();
        };
    }

    /** Particule d'ambiance selon l'arc/thème (identité visuelle cohérente avec la palette). */
    private static ParticleOptions particleFor(ThemePalette t) {
        return switch (t) {
            case DECHARNES -> ParticleTypes.SOUL;              // âmes flottantes
            case FAUVES -> ParticleTypes.SPORE_BLOSSOM_AIR;   // spores de jungle
            case TRIBUS -> ParticleTypes.SMOKE;               // fumée de forge/torches
            case LEGION -> ParticleTypes.WHITE_ASH;            // poussière sépulcrale
            case ABYSSES -> ParticleTypes.UNDERWATER;          // bulles en suspension
            case MAGES -> ParticleTypes.WITCH;                 // étincelles magiques
            case MOISSON -> ParticleTypes.ASH;                 // cendres d'halloween
            case FOURNAISE -> ParticleTypes.FLAME;             // braises
            case GESTE -> ParticleTypes.PORTAL;                // motes du End démoniaques
            case NEANT -> ParticleTypes.SCULK_SOUL;            // âmes du vide
        };
    }
}
