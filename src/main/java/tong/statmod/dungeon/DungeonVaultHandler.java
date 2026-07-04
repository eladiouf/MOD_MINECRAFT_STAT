package tong.statmod.dungeon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import tong.statmod.STATMod;
import tong.statmod.storage.ModAttachments;

/**
 * Mission M6 — Conquête des étages trésor (« vraie aventure », 2026-07-04).
 *
 * <p>Sur un étage trésor (×5), ouvrir le coffre de la chambre forte accomplit l'objectif
 * {@link DungeonObjective#LOOT_VAULT} et débloque la sortie. On ne consomme pas l'interaction :
 * le joueur ouvre bien le coffre (loot Lootr individuel), et la conquête est un effet de bord.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonVaultHandler {

    private DungeonVaultHandler() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!player.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof ChestBlock)) return;

        int floor = DungeonTeleportHandler.floorAtPos(event.getPos().getX(), event.getPos().getZ());
        if (DungeonObjective.forFloor(floor) != DungeonObjective.LOOT_VAULT) return;

        // Idempotent : ne rien refaire si l'étage est déjà conquis.
        if (sp.getData(ModAttachments.STATS).getDungeonFloorReached() > floor) return;

        DungeonProgress.completeFloor(sp, floor, DungeonObjective.LOOT_VAULT, false);
    }
}
