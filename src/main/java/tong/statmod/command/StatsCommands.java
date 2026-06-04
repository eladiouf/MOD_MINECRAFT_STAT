package tong.statmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStats;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;
import tong.statmod.network.SyncAllStatsPacket;
import tong.statmod.stats.StatEffectApplier;
import tong.statmod.stats.StatType;
import net.minecraft.nbt.TagParser;
import tong.statmod.ConfigPresets;
import tong.statmod.balance.BalanceBenchmark;
import tong.statmod.profiling.Profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StatsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statmod")
            .then(Commands.literal("list")
                .executes(ctx -> listStats(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(s -> s.hasPermission(2))
                    .executes(ctx -> listStats(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("get")
                .then(Commands.argument("stat", StringArgumentType.word())
                    .suggests(StatsCommands::suggestStats)
                    .executes(ctx -> getStat(ctx, ctx.getSource().getPlayerOrException()))))
            .then(Commands.literal("set")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("stat", StringArgumentType.word())
                    .suggests(StatsCommands::suggestStats)
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setStat(ctx, ctx.getSource().getPlayerOrException())))))
            .then(Commands.literal("xp")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("stat", StringArgumentType.word())
                    .suggests(StatsCommands::suggestStats)
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> addXp(ctx, ctx.getSource().getPlayerOrException())))))
            .then(Commands.literal("reset")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> resetStats(ctx, ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("backup")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> backupStats(ctx, ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("restore")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> restoreStats(ctx, ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("preset")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("preset", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (ConfigPresets.Preset p : ConfigPresets.Preset.values()) {
                            builder.suggest(p.name().toLowerCase());
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> applyPreset(ctx, ctx.getSource().getPlayerOrException()))))
            .then(Commands.literal("profile")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    Profiler.toggle();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§6Profile: §e" + (Profiler.isEnabled() ? "ENABLED" : "DISABLED — report saved")), true);
                    return 1;
                }))
            .then(Commands.literal("top")
                .then(Commands.argument("stat", StringArgumentType.word())
                    .suggests(StatsCommands::suggestStats)
                    .executes(ctx -> showTop(ctx, StringArgumentType.getString(ctx, "stat")))))
            .then(Commands.literal("benchmark")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    BalanceBenchmark.run();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§aBenchmark complete — see config/statmod/balance-report.txt"), true);
                    return 1;
                }))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "§6StatMod §7- §e/statmod list [player] §8| §e/get <stat> §8| §e/set <stat|all> <level> §8| §e/xp <stat> <amount> §8| §e/reset §8| §e/backup §8| §e/restore §8| §e/preset §8| §e/profile §8| §e/benchmark"), false);
                return 1;
            }));
    }

    private static StatType resolveStat(String input) {
        try {
            int idx = Integer.parseInt(input);
            return StatType.byIndex(idx);
        } catch (NumberFormatException e) {
            for (StatType s : StatType.values()) {
                if (s.name().equalsIgnoreCase(input)) return s;
            }
            for (StatType s : StatType.values()) {
                if (s.displayName.equalsIgnoreCase(input)) return s;
            }
            return null;
        }
    }

    private static CompletableFuture<Suggestions> suggestStats(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        builder.suggest("all");
        for (StatType s : StatType.values()) {
            builder.suggest(s.name().toLowerCase());
        }
        return builder.buildFuture();
    }

    private static int listStats(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CapabilityHelper.withStats(player, stats -> {
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§6--- Stats de " + player.getDisplayName().getString() + " ---"), false);
            for (StatType s : StatType.values()) {
                int level = stats.getLevel(s.index);
                int xp = stats.getXp(s.index);
                int needed = PlayerStats.getXpForNextLevel(level);
                String color = level >= 100 ? "§a" : "§e";
                ctx.getSource().sendSuccess(() -> Component.literal(
                    " §7" + s.displayName + "§8: " + color + level + "§8/100 §7(" + xp + "/" + needed + " XP)"), false);
            }
            int global = 0;
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) global += stats.getLevel(i);
            int avg = Math.round(global / (float) PlayerStats.STAT_COUNT);
            ctx.getSource().sendSuccess(() -> Component.literal("§6Niveau global: §e" + avg), false);
        });
        return 1;
    }

    private static int getStat(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        StatType stat = resolveStat(StringArgumentType.getString(ctx, "stat"));
        if (stat == null) {
            ctx.getSource().sendFailure(Component.literal("§cStat inconnue. Utilise /stats list pour voir les stats disponibles."));
            return 0;
        }
        CapabilityHelper.withStats(player, stats -> {
            int level = stats.getLevel(stat.index);
            int xp = stats.getXp(stat.index);
            int needed = PlayerStats.getXpForNextLevel(level);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§6" + stat.displayName + "§8: §e" + level + "§8/100 §7(" + xp + "/" + needed + " XP)"), false);
        });
        return 1;
    }

    private static int setStat(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String statInput = StringArgumentType.getString(ctx, "stat");
        int newLevel = IntegerArgumentType.getInteger(ctx, "level");

        // Handle "all" keyword - set all stats to the given level
        if (statInput.equalsIgnoreCase("all")) {
            CapabilityHelper.withStats(player, stats -> {
                for (StatType s : StatType.values()) {
                    stats.setLevel(s.index, newLevel);
                    stats.setXp(s.index, 0);
                }
                // Sync all stats to client
                int[] levels = new int[PlayerStats.STAT_COUNT];
                int[] xp = new int[PlayerStats.STAT_COUNT];
                for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                    levels[i] = newLevel;
                    xp[i] = 0;
                }
                NetworkHandler.sendToPlayer(new SyncAllStatsPacket(levels, xp), player);
                StatEffectApplier.applyAllBonuses(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aToutes les stats §7→ §e" + newLevel + "§8/100"), true);
            });
            return 1;
        }

        // Handle single stat
        StatType stat = resolveStat(statInput);
        if (stat == null) {
            ctx.getSource().sendFailure(Component.literal("§cStat inconnue. Utilise 'all' pour toutes les stats."));
            return 0;
        }
        String name = stat.displayName;

        CapabilityHelper.withStats(player, stats -> {
            stats.setLevel(stat.index, newLevel);
            stats.setXp(stat.index, 0);
            NetworkHandler.sendToPlayer(new StatUpdatePacket(stat.index, newLevel, 0), player);
            StatEffectApplier.applyAllBonuses(player);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§a" + name + " §7→ §e" + newLevel + "§8/100"), true);
        });
        return 1;
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        StatType stat = resolveStat(StringArgumentType.getString(ctx, "stat"));
        if (stat == null) {
            ctx.getSource().sendFailure(Component.literal("§cStat inconnue."));
            return 0;
        }
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        String name = stat.displayName;

        CapabilityHelper.withStats(player, stats -> {
            int oldLevel = stats.getLevel(stat.index);
            stats.addXp(stat.index, amount);
            int newLevel = stats.getLevel(stat.index);
            int newXp = stats.getXp(stat.index);
            NetworkHandler.sendToPlayer(new StatUpdatePacket(stat.index, newLevel, newXp), player);
            if (newLevel > oldLevel) {
                StatEffectApplier.applyAllBonuses(player);
            }
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§a+" + amount + " XP §7à " + name + " §8(§e" + newLevel + "§8)"), true);
        });
        return 1;
    }

    private static int resetStats(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CapabilityHelper.withStats(player, stats -> {
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                stats.setLevel(i, 0);
                stats.setXp(i, 0);
            }
            int[] levels = new int[PlayerStats.STAT_COUNT];
            int[] xp = new int[PlayerStats.STAT_COUNT];
            NetworkHandler.sendToPlayer(new SyncAllStatsPacket(levels, xp), player);
            StatEffectApplier.applyAllBonuses(player);
            ctx.getSource().sendSuccess(() -> Component.literal("§aToutes les stats ont été réinitialisées."), true);
        });
        return 1;
    }

    private static int backupStats(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        CapabilityHelper.withStats(player, stats -> {
            var tag = stats.serializeNBT();
            String json = tag.toString();
            java.io.File backupDir = new java.io.File("config/statmod/backups");
            backupDir.mkdirs();
            java.io.File file = new java.io.File(backupDir, player.getUUID() + "_" + System.currentTimeMillis() + ".nbt");
            try (java.io.FileWriter fw = new java.io.FileWriter(file)) { fw.write(json); } catch (Exception ignored) {}
            ctx.getSource().sendSuccess(() -> Component.literal("§aStats backup saved for §e" + player.getDisplayName().getString()), true);
        });
        return 1;
    }

    private static int restoreStats(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        java.io.File backupDir = new java.io.File("config/statmod/backups");
        if (!backupDir.exists() || backupDir.listFiles() == null) {
            ctx.getSource().sendFailure(Component.literal("§cNo backups found."));
            return 0;
        }
        java.io.File[] files = backupDir.listFiles((d, n) -> n.startsWith(player.getUUID().toString()));
        if (files == null || files.length == 0) {
            ctx.getSource().sendFailure(Component.literal("§cNo backup found for this player."));
            return 0;
        }
        java.io.File latest = files[files.length - 1];
        try (java.io.FileReader fr = new java.io.FileReader(latest)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024]; int n;
            while ((n = fr.read(buf)) != -1) sb.append(buf, 0, n);
            net.minecraft.nbt.CompoundTag tag = TagParser.parseTag(sb.toString());
            CapabilityHelper.withStats(player, stats -> {
                stats.deserializeNBT(tag);
                int[] levels = new int[PlayerStats.STAT_COUNT];
                int[] xpArr = new int[PlayerStats.STAT_COUNT];
                for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                    levels[i] = stats.getLevel(i);
                    xpArr[i] = stats.getXp(i);
                }
                NetworkHandler.sendToPlayer(new SyncAllStatsPacket(levels, xpArr), player);
                StatEffectApplier.applyAllBonuses(player);
            });
            ctx.getSource().sendSuccess(() -> Component.literal("§aStats restored from backup: §e" + latest.getName()), true);
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cFailed to restore: " + e.getMessage()));
        }
        return 1;
    }

    private static int showTop(CommandContext<CommandSourceStack> ctx, String statName) {
        StatType stat = resolveStat(statName);
        if (stat == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown stat."));
            return 0;
        }
        List<ServerPlayer> players = ctx.getSource().getServer().getPlayerList().getPlayers();
        List<PlayerRank> ranks = new ArrayList<>();
        for (ServerPlayer p : players) {
            int[] level = {0};
            CapabilityHelper.withStats(p, s -> level[0] = s.getLevel(stat.index));
            ranks.add(new PlayerRank(p.getDisplayName().getString(), level[0]));
        }
        ranks.sort((a, b) -> Integer.compare(b.level, a.level));
        String title = stat.displayName;
        ctx.getSource().sendSuccess(() -> Component.literal("Top 10 - " + title), false);
        for (int i = 0; i < Math.min(10, ranks.size()); i++) {
            final int idx = i;
            PlayerRank r = ranks.get(idx);
            String color = idx == 0 ? "6" : idx < 3 ? "e" : "7";
            ctx.getSource().sendSuccess(() -> Component.literal(
                (idx + 1) + ". " + r.name + " - Level " + r.level), false);
        }
        return 1;
    }

    private record PlayerRank(String name, int level) {}

    private static int applyPreset(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String presetName = StringArgumentType.getString(ctx, "preset").toUpperCase();
        try {
            ConfigPresets.Preset preset = ConfigPresets.Preset.valueOf(presetName);
            ConfigPresets.applyPreset(player, preset);
            return 1;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("§cUnknown preset. Use: easy, normal, hard"));
            return 0;
        }
    }
}