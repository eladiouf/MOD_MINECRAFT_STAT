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
     * Marque l'étage {@code floor} comme conquis : débloque l'étage suivant et joue la célébration
     * pour <b>tous les joueurs présents sur l'étage</b> (co-op : le groupe conquiert ensemble —
     * avant 2026-07-09, seul le tueur du dernier mob progressait et ses coéquipiers restaient
     * scellés derrière le téléporteur). No-op par joueur si son étage est déjà conquis.
     *
     * @param player     le joueur déclencheur (tueur du dernier mob / ouvreur du coffre).
     * @param bossReward {@code true} pour accorder le gain de stat (réservé aux boss).
     */
    public static void completeFloor(ServerPlayer player, int floor, DungeonObjective objective, boolean bossReward) {
        if (floor <= 0) return;
        if (player.level() instanceof ServerLevel sl
                && sl.dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            // FTB Teams : seule l'ÉQUIPE du déclencheur conquiert (2026-07-09). Les rivaux
            // présents sur l'étage ne profitent pas du kill — ils voient la victoire adverse.
            for (ServerPlayer present : DungeonTeleportHandler.playersOnFloor(sl, floor)) {
                if (tong.statmod.integration.ftbteams.FTBTeamsBridge.sameTeam(player, present)) {
                    completeForPlayer(present, floor, objective, bossReward);
                } else if (present.getData(ModAttachments.STATS).getDungeonFloorReached() <= floor) {
                    net.minecraft.network.chat.Component team =
                            tong.statmod.integration.ftbteams.FTBTeamsBridge.teamName(player);
                    present.displayClientMessage(Component.translatable("dungeon.coop.rival_conquered",
                            team != null ? team : player.getDisplayName(), floor), false);
                }
            }
        } else {
            // Filet de sécurité (déclencheur hors donjon — commandes/tests) : au moins lui.
            completeForPlayer(player, floor, objective, bossReward);
        }
    }

    /** Conquête pour UN joueur : unlock + récompense + célébration. Idempotent. */
    private static void completeForPlayer(ServerPlayer player, int floor, DungeonObjective objective, boolean bossReward) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (data.getDungeonFloorReached() > floor) return; // déjà conquis

        data.unlockDungeonFloor(floor + 1);

        // Dungeon Rush : étage conquis sans un coup reçu → récompense de conquête doublée.
        boolean flawless = DungeonRush.isFlawless(player.getUUID());

        if (bossReward) {
            int gain = Config.getDungeonBossStatGain();
            int statIndex = PHYSICAL_STAT_INDICES[player.getRandom().nextInt(PHYSICAL_STAT_INDICES.length)];
            RaceEffectApplier.addLevels(player, statIndex, gain, data, true);
            StatType stat = StatType.byIndex(statIndex);
            String statName = stat != null ? stat.displayName : "?";
            player.displayClientMessage(Component.translatable(
                    "block.statmod.dungeon_portal.boss_kill", gain, statName, floor + 1), false);
            // Gros gain de points pour le boss vaincu.
            DungeonPoints.awardBoss(player, flawless ? DungeonRush.FLAWLESS_MULTIPLIER : 1);
        } else {
            player.displayClientMessage(Component.translatable(
                    "dungeon.floor.conquered", floor, floor + 1), false);
            // Bonus de points pour la conquête d'un étage (remplace la récompense en cristaux).
            DungeonPoints.awardFloorClear(player, flawless ? DungeonRush.FLAWLESS_MULTIPLIER : 1);
        }

        SyncHelper.syncStats(player);
        celebrate(player, floor);
        if (flawless) {
            // Titre dédié APRÈS la célébration standard : le sans-faute est le moment de gloire.
            title(player, Component.translatable("dungeon.title.flawless"),
                    Component.translatable("dungeon.title.flawless.sub"));
            player.playNotifySound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        // Ré-arme le sans-faute : rester jouer sur l'étage conquis ne doit pas fausser le suivant.
        DungeonRush.beginFloor(player.getUUID());
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
        // Titre à l'écran : moment fort, plus satisfaisant qu'une simple ligne de chat.
        title(player, Component.translatable("dungeon.title.floor_cleared"),
                Component.translatable("dungeon.title.floor_cleared.sub", floor + 1));
        // Soin de récompense : on souffle entre deux étages (mi-PV rendus + brève régénération).
        player.heal(player.getMaxHealth() * 0.5f);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1, false, true));
    }

    /** Envoie un titre + sous-titre à l'écran du joueur (animation standard fade/stay/fade). */
    static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 50, 20));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));
    }

    /** Message de jalon tous les 10 étages franchis — donne le sens d'un voyage. */
    private static void milestone(ServerPlayer player, int reachedFloor) {
        if (reachedFloor % 10 != 1) return; // 11, 21, 31… = on vient d'entrer dans un nouveau palier
        int tierFloor = reachedFloor - 1; // 10, 20, 30…
        player.displayClientMessage(Component.translatable(
                "dungeon.milestone", tierFloor), false);
    }
}
