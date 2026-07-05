package tong.statmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonTeleportHandler;
import tong.statmod.sound.ModSounds;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.List;

/**
 * Mission M6 — Balise du Trial Dungeon (2026-07-04).
 *
 * <p>Item « portail de poche » : clic-droit <b>n'importe où</b> hors du donjon → téléporte au plus
 * haut étage débloqué du Trial Dungeon, après avoir sauvegardé la position de retour (comme le bloc
 * portail). Réutilisable (non consommé). Répond à la demande « entrer au donjon depuis n'importe où ».
 *
 * <p>Depuis le donjon, le clic-droit renvoie vers l'overworld (retour rapide).
 */
public class DungeonBeaconItem extends Item {

    public DungeonBeaconItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // Depuis le donjon → retour overworld.
        if (level.dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            DungeonTeleportHandler.returnToOverworld(sp);
            return InteractionResultHolder.success(stack);
        }

        // Depuis l'overworld → sauvegarde la position de retour puis entre au plus haut étage.
        PlayerStatData data = sp.getData(ModAttachments.STATS);
        data.setLastOverworldDimensionId(level.dimension().location().toString());
        data.setLastOverworldPos(sp.blockPosition().asLong());

        int floor = data.getDungeonFloorReached();
        boolean ok = DungeonTeleportHandler.enterFloor(sp, floor);
        if (ok) {
            sp.playNotifySound(ModSounds.DUNGEON_PORTAL_ENTER.get(), SoundSource.PLAYERS, 0.7f, 1.3f);
            sp.displayClientMessage(Component.translatable("item.statmod.dungeon_beacon.enter", floor), true);
            return InteractionResultHolder.success(stack);
        }
        sp.displayClientMessage(Component.translatable("item.statmod.dungeon_beacon.failed"), true);
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.statmod.dungeon_beacon.tooltip")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
