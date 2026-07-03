package tong.statmod.dungeon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

/**
 * Mission M6 — Commandes admin pour le Trial Dungeon.
 *
 * <p>Enregistré dans {@link tong.statmod.stats.StatCommands#register(CommandDispatcher)}
 * sous {@code /statdungeon}.
 */
public final class DungeonCommands {

    private DungeonCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statdungeon")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "Trial Dungeon — Floor reached: " + data.getDungeonFloorReached()), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("tp")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1, 10000))
                                .executes(ctx -> {
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        boolean ok = DungeonTeleportHandler.enterFloor(player, floor);
                                        if (ok) {
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Teleported to floor " + floor), true);
                                        } else {
                                            ctx.getSource().sendFailure(Component.literal(
                                                    "Failed to teleport — floor locked or dimension missing"));
                                        }
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1, 10000))
                                .executes(ctx -> {
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        PlayerStatData data = player.getData(ModAttachments.STATS);
                                        data.unlockDungeonFloor(floor);
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "Unlocked floor " + floor), true);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                data.unlockDungeonFloor(1);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "Dungeon progress reset to floor 1"), true);
                            }
                            return 1;
                        }))
                .then(Commands.literal("regen")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1, 10000))
                                .executes(ctx -> {
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    ServerLevel dungeon = ctx.getSource().getServer()
                                            .getLevel(DungeonDimensions.TRIAL_DUNGEON);
                                    if (dungeon == null) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "Dimension statmod:trial_dungeon introuvable"));
                                        return 0;
                                    }
                                    // Efface toute la bounding box (surface + underside + décor)
                                    // puis régénère — même seed, même île.
                                    BoundingBox box = IslandGenerator.floorBoundingBox(floor);
                                    for (int x = box.minX(); x <= box.maxX(); x++) {
                                        for (int y = box.minY(); y <= box.maxY(); y++) {
                                            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                                                dungeon.setBlock(new BlockPos(x, y, z),
                                                        Blocks.AIR.defaultBlockState(), 2);
                                            }
                                        }
                                    }
                                    IslandGenerator.generateFloor(dungeon, floor);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Regenerated floor " + floor), true);
                                    return 1;
                                }))));
    }
}
