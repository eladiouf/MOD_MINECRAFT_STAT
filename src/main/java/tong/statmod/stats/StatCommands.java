package tong.statmod.stats;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import tong.statmod.magic.SchoolProgressTracker;
import tong.statmod.network.SyncHelper;
import tong.statmod.progression.WeaponResolver;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public class StatCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerMagic(dispatcher);
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

        dispatcher.register(Commands.literal("statmod")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("classify")
                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String input = StringArgumentType.getString(ctx, "item");
                                    ResourceLocation id = ResourceLocation.tryParse(input);
                                    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                                        ctx.getSource().sendFailure(Component.literal("Item not found: " + input));
                                        return 0;
                                    }
                                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                                    StatType stat = WeaponResolver.statFor(stack);
                                    String action = WeaponResolver.tensuraActionFor(stack);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§6" + id + "§r → §b" + stat.displayName + "§r [" + action + "]"), true);
                                    return 1;
                                }))));
    }

    private static void registerMagic(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("magic")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§6=== Magic Status ==="), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§bArcane Points:§r " + data.getArcanePoints()), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§bRace:§r " + (data.getMagicRace() != null ? data.getMagicRace().name() : "§7not set§r")), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§bStart Branch:§r " + (data.getChosenStartBranch() != null ? data.getChosenStartBranch().id : "§7not set§r")), true);
                                for (MagicBranch b : MagicBranch.values()) {
                                    if (b == MagicBranch.COMMON) continue;
                                    int sp = data.getSchoolPoints(b);
                                    int mp = data.getSchoolMasteryProgress(b);
                                    if (sp > 0 || mp > 0) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("§e" + b.id + "§r points=" + sp + " mastery=" + mp), true);
                                    }
                                }
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§6Nodes unlocked:§r " + data.getMagicNodes().length), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§6Spells learned:§r " + data.getLearnedSpells().length), true);
                            }
                            return 1;
                        }))
                .then(Commands.literal("grantarcane")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 9999))
                                .executes(ctx -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        PlayerStatData data = player.getData(ModAttachments.STATS);
                                        data.addArcanePoints(amount);
                                        SyncHelper.syncMagic(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("+ " + amount + " arcane points"), true);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("grantschool")
                        .then(Commands.argument("branch", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 9999))
                                        .executes(ctx -> {
                                            String branchId = StringArgumentType.getString(ctx, "branch");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            MagicBranch b = MagicBranch.byId(branchId);
                                            if (b == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown branch: " + branchId));
                                                return 0;
                                            }
                                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                                data.addSchoolPoints(b, amount);
                                                SyncHelper.syncMagic(player);
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("+ " + amount + " school points for " + b.id), true);
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("grantmastery")
                        .then(Commands.argument("branch", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 9999))
                                        .executes(ctx -> {
                                            String branchId = StringArgumentType.getString(ctx, "branch");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            MagicBranch b = MagicBranch.byId(branchId);
                                            if (b == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown branch: " + branchId));
                                                return 0;
                                            }
                                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                                SchoolProgressTracker.applyMastery(data, b, amount);
                                                SyncHelper.syncMagic(player);
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("+ " + amount + " mastery for " + b.id), true);
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("pickrace")
                        .then(Commands.argument("race", StringArgumentType.word())
                                .then(Commands.argument("branch", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String raceId = StringArgumentType.getString(ctx, "race");
                                            String branchId = StringArgumentType.getString(ctx, "branch");
                                            MagicRace race = MagicRace.byId(raceId);
                                            MagicBranch start = MagicBranch.byId(branchId);
                                            if (race == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown race: " + raceId + " (human/elf/dwarf/beast)"));
                                                return 0;
                                            }
                                            if (start == null || start.lateGame) {
                                                ctx.getSource().sendFailure(Component.literal("Invalid start branch: " + branchId + " (fire/water/air/earth)"));
                                                return 0;
                                            }
                                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                                data.setMagicRace(race);
                                                data.setChosenStartBranch(start);
                                                SyncHelper.syncMagic(player);
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("Race set to " + raceId + " with " + branchId + " affinity"), true);
                                            }
                                            return 1;
                                        })))));
    }
}
