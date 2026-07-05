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
            ParticleOptions particle = particleFor(ThemePalette.forFloor(floor));
            Vec3 base = p.position();
            for (int i = 0; i < PER_BURST; i++) {
                double x = base.x + (rng.nextDouble() - 0.5) * 2 * RADIUS;
                double y = base.y + rng.nextDouble() * 4.0;
                double z = base.z + (rng.nextDouble() - 0.5) * 2 * RADIUS;
                lv.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** Particule d'ambiance selon l'arc/thème (identité visuelle cohérente avec la palette). */
    private static ParticleOptions particleFor(ThemePalette t) {
        return switch (t) {
            case AWAKENING -> ParticleTypes.WHITE_ASH;      // poussière en suspension
            case RESTLESS_DEAD -> ParticleTypes.SOUL;       // âmes flottantes
            case WARBAND -> ParticleTypes.SMOKE;            // fumée de forge/torches
            case HUNT -> ParticleTypes.SPORE_BLOSSOM_AIR;   // spores de jungle
            case SUNKEN -> ParticleTypes.UNDERWATER;        // bulles en suspension
            case FROZEN -> ParticleTypes.SNOWFLAKE;         // flocons
            case HARVEST -> ParticleTypes.ASH;              // cendres d'halloween
            case ARCANE -> ParticleTypes.WITCH;             // étincelles magiques
            case INFERNAL -> ParticleTypes.FLAME;           // braises
            case ABYSS -> ParticleTypes.PORTAL;             // motes du End
        };
    }
}
