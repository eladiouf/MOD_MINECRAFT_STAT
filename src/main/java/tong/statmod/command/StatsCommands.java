package tong.statmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.effects.PlayerAttributeEffects;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatProgress;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class StatsCommands {
    private static final DynamicCommandExceptionType UNKNOWN_STAT =
            new DynamicCommandExceptionType(id ->
                    Component.translatable("command.statmod.unknown_stat", id));

    private StatsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statmod")
                .then(Commands.literal("stats")
                        .executes(context -> showSelf(context.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .executes(context -> show(context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("stat")
                        .then(Commands.literal("get")
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(statArgument().executes(StatsCommands::get))))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(statArgument().then(Commands.argument("level",
                                                        IntegerArgumentType.integer(0, 100))
                                                .executes(StatsCommands::set)))))
                        .then(Commands.literal("addxp")
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(statArgument().then(Commands.argument("amount",
                                                        IntegerArgumentType.integer(1))
                                                .executes(StatsCommands::addXp)))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> statArgument() {
        return Commands.argument("stat", StringArgumentType.word())
                .suggests((context, builder) -> {
                    Arrays.stream(StatType.values())
                            .map(StatType::id)
                            .forEach(builder::suggest);
                    return builder.buildFuture();
                });
    }

    private static StatType stat(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "stat");
        return StatType.fromId(id).orElseThrow(() -> UNKNOWN_STAT.create(id));
    }

    private static int showSelf(CommandSourceStack source) throws CommandSyntaxException {
        return show(source, source.getPlayerOrException());
    }

    private static int show(CommandSourceStack source, ServerPlayer target) {
        var optional = target.getCapability(StatCapabilities.PLAYER_STATS).resolve();
        if (optional.isEmpty()) {
            return missing(source, target);
        }

        PlayerStats stats = optional.get();
        source.sendSuccess(() -> Component.translatable(
                "command.statmod.stats.header", target.getDisplayName()), false);
        for (StatType type : StatType.values()) {
            StatValue value = stats.get(type);
            source.sendSuccess(() -> Component.translatable("command.statmod.stats.entry",
                    type.id(), value.level(), value.xp(), required(value)), false);
        }
        return StatType.values().length;
    }

    private static int get(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        StatType type = stat(context);
        var optional = target.getCapability(StatCapabilities.PLAYER_STATS).resolve();
        if (optional.isEmpty()) {
            return missing(source, target);
        }

        StatValue value = optional.get().get(type);
        source.sendSuccess(() -> Component.translatable("command.statmod.stat.value",
                target.getDisplayName(), type.id(), value.level(), value.xp(), required(value)), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        StatType type = stat(context);
        int level = IntegerArgumentType.getInteger(context, "level");
        return mutate(context.getSource(), target, type, stats -> stats.setLevel(type, level));
    }

    private static int addXp(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        StatType type = stat(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        return mutate(context.getSource(), target, type, stats -> stats.addXp(type, amount));
    }

    private static int mutate(CommandSourceStack source, ServerPlayer target, StatType type,
            Consumer<PlayerStats> mutation) {
        var optional = target.getCapability(StatCapabilities.PLAYER_STATS).resolve();
        if (optional.isEmpty()) {
            return missing(source, target);
        }

        PlayerStats stats = optional.get();
        mutation.accept(stats);
        PlayerAttributeEffects.refresh(target);
        StatNetwork.sendSnapshot(target);
        StatValue value = stats.get(type);
        source.sendSuccess(() -> Component.translatable("command.statmod.stat.updated",
                type.id(), target.getDisplayName(), value.level(), value.xp(), required(value)), true);
        return 1;
    }

    private static int required(StatValue value) {
        return StatProgress.requiredXp(value.level());
    }

    private static int missing(CommandSourceStack source, ServerPlayer target) {
        source.sendFailure(Component.translatable(
                "command.statmod.capability_missing", target.getDisplayName()));
        return 0;
    }
}
