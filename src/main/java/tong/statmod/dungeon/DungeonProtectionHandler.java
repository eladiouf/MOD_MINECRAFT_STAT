package tong.statmod.dungeon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Mission M6 — Le Trial Dungeon est <b>indestructible</b>.
 *
 * <p>Trois protections dans la dimension {@code statmod:trial_dungeon} :
 * <ol>
 *   <li>Casse de bloc annulée (sauf joueur en créatif — admin).</li>
 *   <li>Pose de bloc annulée (sauf créatif).</li>
 *   <li>Explosions : la liste des blocs affectés est vidée → creepers, blazes, TNT, etc. ne
 *       peuvent plus percer le sol ni les murs (les dégâts aux entités restent).</li>
 * </ol>
 *
 * <p>Sans ça, un creeper qui explose en plein combat laissait des trous de 2 blocs dans les
 * plateformes.
 */
public final class DungeonProtectionHandler {

    private DungeonProtectionHandler() {}

    private static boolean inDungeon(Object levelAccessor) {
        return levelAccessor instanceof Level lvl
                && lvl.dimension().equals(DungeonDimensions.TRIAL_DUNGEON);
    }

    /** Un joueur en créatif (admin) garde le droit de casser/poser pour éditer/débugger. */
    private static boolean isBuilder(Player player) {
        return player != null && player.getAbilities().instabuild;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!inDungeon(event.getLevel())) return;
        if (isBuilder(event.getPlayer())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!inDungeon(event.getLevel())) return;
        if (event.getEntity() instanceof Player p && isBuilder(p)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!inDungeon(event.getLevel())) return;
        // Les explosions ne détruisent aucun bloc du donjon (mais blessent encore les entités).
        event.getAffectedBlocks().clear();
    }
}
