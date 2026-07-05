package tong.statmod.dungeon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import tong.statmod.STATMod;

import java.util.List;

/**
 * Mission M6 — On ne perd jamais son inventaire dans le Trial Dungeon (2026-07-04).
 *
 * <p>Dans un donjon d'entraînement, mourir ne doit <b>pas</b> coûter l'inventaire ni l'XP. Plutôt
 * que de laisser le joueur mourir puis le renvoyer (ce qui, avec les gamerules par défaut, drope
 * déjà son stuff et son XP), on <b>annule la mort</b> : le joueur est soigné, purgé de ses malus,
 * et <b>réapparaît au début de l'étage où il est tombé</b> (l'étage se régénère, une nouvelle vague
 * l'attend). Aucun écran de mort, aucun drop, aucune perte d'XP.
 *
 * <p>Priorité {@code LOWEST} : on laisse les autres mods réagir à la mort d'abord (compat), et on
 * annule en dernier si personne ne l'a déjà fait.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonRespawnHandler {

    private DungeonRespawnHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        // Étage sur lequel le joueur est tombé — DÉDUIT AVANT le tp (il change après).
        int floor = DungeonTeleportHandler.floorAtPos(player.getBlockX(), player.getBlockZ());
        if (floor < 1) floor = 1;

        // Annule la mort → pas d'écran de mort, pas de drop d'inventaire/XP.
        event.setCanceled(true);

        // Mort punitive : le joueur perd une partie de ses points de donjon.
        DungeonPoints.applyDeathPenalty(player);

        // Règle « 0 point → éjection » : si la mort a vidé les points, on renvoie le joueur à
        // l'overworld AU LIEU de le faire réapparaître dans le donjon (les coins du shop restent
        // acquis). On soigne d'abord pour ne pas le renvoyer mourant.
        if (player.getData(tong.statmod.storage.ModAttachments.STATS).getDungeonPoints() <= 0) {
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            DungeonPointsEjection.enforce(player);
            return;
        }

        // Remet le joueur en état de combattre : PV pleins, feu éteint, malus purgés, faim rétablie.
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.getFoodData().setFoodLevel(20);
        List<MobEffectInstance> effects = List.copyOf(player.getActiveEffects());
        for (MobEffectInstance effect : effects) {
            player.removeEffect(effect.getEffect());
        }
        player.setDeltaMovement(0, 0, 0);
        player.hurtTime = 0;
        player.hurtMarked = true;

        // PURGE l'étage AVANT de réapparaître : sans ça, le joueur renaissait au centre, au milieu
        // de la horde + mini-boss de sa tentative précédente → mort instantanée → boucle infinie
        // (observée à l'étage 3 « thème ORC » avec l'Orc Lord). On repart sur une vague propre.
        if (player.level() instanceof ServerLevel sl) {
            DungeonMobSpawner.clearFloorMobs(sl, floor);
        }

        // Réapparition au début du MÊME étage (régénéré, nouvelle vague) — on réessaie l'étage.
        DungeonTeleportHandler.enterFloor(player, floor);

        // Grâce de réapparition : brève invulnérabilité + résistance/lenteur des mobs autour, le
        // temps de reprendre pied (5 s d'invuln vanilla + résistance forte).
        player.invulnerableTime = 100; // ~5 s
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));

        final int retryFloor = floor;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "dungeon.defeat.reset", retryFloor), false);
        STATMod.LOGGER.info("[TrialDungeon] {} vaincu — réapparaît étage {} (inventaire préservé)",
                player.getGameProfile().getName(), retryFloor);
    }
}
