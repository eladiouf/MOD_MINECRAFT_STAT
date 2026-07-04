package tong.statmod.dungeon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.config.Config;
import tong.statmod.item.RuneShardIds;
import tong.statmod.item.RuneShards;
import tong.statmod.network.SyncHelper;
import tong.statmod.sound.ModSounds;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.function.Supplier;

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
            data.addLevels(statIndex, gain);
            StatType stat = StatType.byIndex(statIndex);
            String statName = stat != null ? stat.displayName : "?";
            player.displayClientMessage(Component.translatable(
                    "block.statmod.dungeon_portal.boss_kill", gain, statName, floor + 1), false);
        } else {
            player.displayClientMessage(Component.translatable(
                    "dungeon.floor.conquered", floor, floor + 1), false);
        }

        // Récompense GARANTIE de conquête : des rune shards dont la quantité/rareté monte avec le
        // tier. S'ajoute au loot aléatoire des mobs — conquérir un étage vaut toujours le coup.
        grantConquestReward(player, floor);

        SyncHelper.syncStats(player);
        celebrate(player, floor);
        milestone(player, floor + 1);

        STATMod.LOGGER.info("[TrialDungeon] Étage {} conquis ({}) par {} → étage {} débloqué",
                floor, objective, player.getGameProfile().getName(), floor + 1);
    }

    /**
     * Donne une récompense garantie de rune shards à la conquête d'un étage. Quantité et rareté
     * croissent avec le tier. Remise directe à l'inventaire (drop aux pieds si plein).
     */
    private static void grantConquestReward(ServerPlayer player, int floor) {
        FloorPalette tier = FloorPalette.forFloor(floor);
        int count = switch (tier) { case EARLY -> 2; case MID -> 3; case LATE -> 4; case ABYSS -> 5; };

        int granted = 0;
        for (int i = 0; i < count; i++) {
            RuneShardIds.Rarity rarity = rewardRarity(tier, player);
            RuneShardIds.Family family = RuneShardIds.Family.values()[
                    player.getRandom().nextInt(RuneShardIds.Family.values().length)];
            Supplier<Item> supplier = RuneShards.SHARDS.get(RuneShardIds.id(rarity, family));
            if (supplier == null) continue;
            ItemStack stack = new ItemStack(supplier.get());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false); // inventaire plein → au sol
            }
            granted++;
        }
        if (granted > 0) {
            player.displayClientMessage(Component.translatable("dungeon.reward.shards", granted), true);
        }
    }

    /** Rareté de récompense pondérée par tier (meilleure en profondeur). */
    private static RuneShardIds.Rarity rewardRarity(FloorPalette tier, ServerPlayer player) {
        int roll = player.getRandom().nextInt(100);
        return switch (tier) {
            case EARLY -> roll < 80 ? RuneShardIds.Rarity.COMMON : RuneShardIds.Rarity.UNCOMMON;
            case MID -> roll < 55 ? RuneShardIds.Rarity.COMMON
                    : roll < 90 ? RuneShardIds.Rarity.UNCOMMON : RuneShardIds.Rarity.RARE;
            case LATE -> roll < 40 ? RuneShardIds.Rarity.UNCOMMON
                    : roll < 85 ? RuneShardIds.Rarity.RARE : RuneShardIds.Rarity.EPIC;
            case ABYSS -> roll < 50 ? RuneShardIds.Rarity.RARE
                    : roll < 90 ? RuneShardIds.Rarity.EPIC : RuneShardIds.Rarity.LEGENDARY;
        };
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
