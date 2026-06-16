package tong.statmod.stats;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public class StatCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statlevel")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("index", IntegerArgumentType.integer(0, 22))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                .executes(ctx -> {
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    if (ctx.getSource().getEntity() instanceof Player player) {
                                        PlayerStatData data = player.getData(ModAttachments.STATS);
                                        data.addLevels(index, amount);
                                        StatType stat = StatType.byIndex(index);
                                        String name = stat != null ? stat.displayName : ("#" + index);
                                        int effective = RaceEffectApplier.getEffectiveLevel(player, index);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal(name + " → Lv." + effective), true);
                                    }
                                    return 1;
                                }))));

        dispatcher.register(Commands.literal("statxp")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("index", IntegerArgumentType.integer(0, 22))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                                .executes(ctx -> {
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    if (ctx.getSource().getEntity() instanceof Player player) {
                                        PlayerStatData data = player.getData(ModAttachments.STATS);
                                        boolean leveled = data.addXp(index, amount);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("+" + amount + " XP" + (leveled ? " (level up!)" : "")), true);
                                    }
                                    return 1;
                                }))));
    }
}
