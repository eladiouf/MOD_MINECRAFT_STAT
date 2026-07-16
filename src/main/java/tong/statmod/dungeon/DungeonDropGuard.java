package tong.statmod.dungeon;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import tong.statmod.StatMod;

/**
 * Mission M6 — Zéro drop de mob dans le donjon (système de points, 2026-07-05).
 *
 * <p>Le donjon est passé d'un loot en cristaux (rune shards) à un <b>système de points</b>
 * ({@link DungeonPoints}) : tuer des mobs/boss donne des points, pas des objets. Les mobs ne
 * dropent donc <b>plus rien du tout</b>. Ce garde vide tous les drops de mob dans la dimension,
 * quelle qu'en soit la source (équipement, code custom SLU/MCreator, loot table). Complète la
 * suppression des Global Loot Modifiers de shards.
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonDropGuard {

    private DungeonDropGuard() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.getEntity().level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            return;
        }
        // Aucun drop dans le donjon — la récompense passe par les points.
        if (!event.getDrops().isEmpty()) {
            event.getDrops().clear();
        }
    }
}
