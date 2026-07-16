package tong.statmod.dungeon.party;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonTeleportHandler;

/**
 * Outil de test : {@code /statparty [étage]} spawn un groupe d'aventuriers coordonné aux pieds du
 * joueur (tier dérivé de l'étage). La coordination ({@link PartyCoordinator}) ne tourne que dans la
 * dimension du donjon — lancer la commande depuis le donjon pour voir focus-fire / execute / formation.
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class PartyDebugCommand {

    private PartyDebugCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("statparty")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> spawn(ctx, -1))
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1, 10000))
                                .executes(ctx -> spawn(ctx,
                                        IntegerArgumentType.getInteger(ctx, "floor")))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, int floorArg)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        int detected = DungeonTeleportHandler.floorAtPos(player.getBlockX(), player.getBlockZ());
        int floor = floorArg > 0 ? floorArg : (detected > 0 ? detected : 7);

        AdventurerPartyHelper.spawnPartyAt(level, player.blockPosition(), floor);

        boolean inDungeon = level.dimension().equals(DungeonDimensions.TRIAL_DUNGEON);
        final int f = floor;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aParty d'aventuriers (tier étage " + f + ") spawnée à tes pieds."
                        + (inDungeon ? ""
                        : " §e⚠ Hors donjon : coordination inactive (le cerveau ne tourne que dans le donjon).")),
                false);
        StatMod.LOGGER.info("[Party] /statparty : 4 membres spawnés (floor {}, dungeon={})",
                f, inDungeon);
        return 1;
    }
}
