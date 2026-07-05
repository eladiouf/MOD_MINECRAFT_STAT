package tong.statmod.dungeon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.config.Config;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.ModSounds;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

/**
 * Mission M6 — « Vraie aventure » (2026-07-04).
 *
 * <p>Coordinateur unique de la conquête d'étage. Quand un joueur accomplit l'objectif d'un étage
 * ({@link DungeonObjective}), c'est ici qu'on débloque l'étage suivant <b>et</b> qu'on joue la
 * célébration (son + message + particules + gain de stat sur les boss). Centralise ce qui était
 * dupliqué dans {@link DungeonBossHandler}.
 *
 * <p>Idempotent : re-compléter un étage déjà conquis ne fait rien (test {@code floorReached}).
 */
public final class DungeonProgress {

    /** Stats physiques éligibles au gain direct (les magiques passent par le magic tree). */
    private static final int[] PHYSICAL_STAT_INDICES = {
            0, 1, 2, 3, 4, 5, 6, 16, 17, 18, 19, 20, 21, 22
    };

    private DungeonProgress() {}

    /**
     * Marque l'étage {@code floor} comme conquis par {@code player} : débloque l'étage suivant
     * (jamais au-delà d'un boss non vaincu — les étages boss gardent leur propre logique) et joue
     * la célébration adaptée à l'objectif. No-op si l'étage est déjà conquis.
     *
     * @param bossReward {@code true} pour accorder le gain de stat (réservé aux boss).
     */
    public static void completeFloor(ServerPlayer player, int floor, DungeonObjective objective, boolean bossReward) {
        if (floor <= 0) return;
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data.getDungeonFloorReached() > floor) return; // déjà conquis

        data.unlockDungeonFloor(floor + 1);

        if (bossReward) {
            int gain = Config.getDungeonBossStatGain();
            int statIndex = PHYSICAL_STAT_INDICES[player.getRandom().nextInt(PHYSICAL_STAT_INDICES.length)];
            RaceEffectApplier.addLevels(player, statIndex, gain, data, true);
            StatType stat = StatType.byIndex(statIndex);
            String statName = stat != null ? stat.displayName : "?";
            player.displayClientMessage(Component.translatable(
                    "block.statmod.dungeon_portal.boss_kill", gain, statName, floor + 1), false);
            // Gros gain de points pour le boss vaincu.
            DungeonPoints.awardBoss(player);
        } else {
            player.displayClientMessage(Component.translatable(
                    "dungeon.floor.conquered", floor, floor + 1), false);
            // Bonus de points pour la conquête d'un étage (remplace la récompense en cristaux).
            DungeonPoints.awardFloorClear(player);
        }

        SyncHelper.syncStats(player);
        celebrate(player, floor);
        milestone(player, floor + 1);

        STATMod.LOGGER.info("[TrialDungeon] Étage {} conquis ({}) par {} → étage {} débloqué",
                floor, objective, player.getGameProfile().getName(), floor + 1);
    }

    /** Son + gerbe de particules autour du joueur pour marquer la conquête d'un étage. */
    private static void celebrate(ServerPlayer player, int floor) {
        try {
            player.playNotifySound(ModSounds.DUNGEON_FLOOR_COMPLETE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        } catch (Exception ignored) {
            // Un son manquant ne doit jamais bloquer la progression.
        }
        if (player.level() instanceof ServerLevel sl) {
            Vec3 p = player.position();
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    p.x, p.y + 1.0, p.z, 40, 0.6, 0.9, 0.6, 0.15);
            sl.sendParticles(ParticleTypes.END_ROD,
                    p.x, p.y + 1.2, p.z, 18, 0.5, 0.7, 0.5, 0.05);
        }
    }

    /** Message de jalon tous les 10 étages franchis — donne le sens d'un voyage. */
    private static void milestone(ServerPlayer player, int reachedFloor) {
        if (reachedFloor % 10 != 1) return; // 11, 21, 31… = on vient d'entrer dans un nouveau palier
        int tierFloor = reachedFloor - 1; // 10, 20, 30…
        player.displayClientMessage(Component.translatable(
                "dungeon.milestone", tierFloor), false);
    }
}
