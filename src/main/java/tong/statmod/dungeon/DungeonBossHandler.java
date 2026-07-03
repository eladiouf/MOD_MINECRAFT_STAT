package tong.statmod.dungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import tong.statmod.STATMod;
import tong.statmod.config.Config;
import tong.statmod.network.SyncHelper;
import tong.statmod.progression.CombatXPHandler;
import tong.statmod.sound.ModSounds;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

/**
 * Mission M6 — Phase γ + patch "kill boss = sortie possible".
 *
 * <p>Heuristique simple et robuste : sur un étage boss (multiple de 10), <b>n'importe quel</b>
 * mob tué par le joueur dans la dimension Trial Dungeon compte comme un boss kill et unlock
 * l'étage suivant. Cette approche est indépendante du tag {@code statmod:dungeon_boss} et des
 * mods installés — si l'altar spawn un fallback (Pig si SLU absent), tuer ce Pig unlock quand même.
 *
 * <p>Idempotent : après le premier kill qui unlock, les kills suivants sur la même île ne
 * ré-déclenchent rien (test {@code floorReached > floor}).
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonBossHandler {

    public static final TagKey<EntityType<?>> DUNGEON_BOSS_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "dungeon_boss"));

    /** Stats physiques éligibles au gain direct (les magiques sont gérées via le magic tree). */
    private static final int[] PHYSICAL_STAT_INDICES = {
            0, 1, 2, 3, 4, 5, 6, 16, 17, 18, 19, 20, 21, 22
    };

    private DungeonBossHandler() {}

    @SubscribeEvent
    public static void onBossKill(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();

        // Ignorer les morts de joueur (gérées par DungeonRespawnHandler).
        if (target instanceof ServerPlayer) return;
        // Ignorer hors de la dimension donjon.
        if (!target.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        ServerPlayer sp = findAttacker(event, target);
        if (sp == null) {
            STATMod.LOGGER.debug("[TrialDungeon] Death sans attaquant identifié : {}",
                    target.getName().getString());
            return;
        }

        int floor = DungeonTeleportHandler.floorAtPos(sp.getBlockX(), sp.getBlockZ());

        // Heuristique : seulement les étages boss (multiples de 10) unlock via kill.
        if (floor <= 0 || floor % 10 != 0) return;

        PlayerStatData data = sp.getData(ModAttachments.STATS);
        // Idempotent : ne pas re-déclencher si déjà unlocked (autres mobs du roster).
        if (data.getDungeonFloorReached() > floor) return;

        int gain = Config.getDungeonBossStatGain();
        int statIndex = PHYSICAL_STAT_INDICES[sp.getRandom().nextInt(PHYSICAL_STAT_INDICES.length)];
        data.addLevels(statIndex, gain);
        data.unlockDungeonFloor(floor + 1);
        SyncHelper.syncStats(sp);

        try {
            sp.playNotifySound(ModSounds.DUNGEON_BOSS_KILL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        } catch (Exception ignored) {
            // Sound miss ne doit pas bloquer l'unlock.
        }

        StatType stat = StatType.byIndex(statIndex);
        String statName = stat != null ? stat.displayName : "?";
        sp.displayClientMessage(
                Component.translatable("block.statmod.dungeon_portal.boss_kill",
                        gain, statName, floor + 1), false);
        STATMod.LOGGER.info("[TrialDungeon] Boss cleared floor {} by {} → unlock étage {}",
                floor, sp.getGameProfile().getName(), floor + 1);
    }

    /**
     * 3 fallbacks pour retrouver le joueur responsable :
     * <ol>
     *   <li>Source directe de dégâts (melee / ranged)</li>
     *   <li>Dernier joueur qui a hurt le mob</li>
     *   <li>Le joueur en dungeon le plus proche (AABB radius 60)</li>
     * </ol>
     */
    private static ServerPlayer findAttacker(LivingDeathEvent event, LivingEntity target) {
        Player direct = CombatXPHandler.resolveAttacker(
                event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (direct instanceof ServerPlayer sp) return sp;
        if (target.getLastHurtByMob() instanceof ServerPlayer sp) return sp;
        List<ServerPlayer> nearby = target.level().getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(target.blockPosition()).inflate(60),
                p -> p.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON));
        return nearby.isEmpty() ? null : nearby.get(0);
    }
}
