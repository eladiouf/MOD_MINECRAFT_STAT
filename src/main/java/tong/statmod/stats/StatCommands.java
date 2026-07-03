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
        tong.statmod.dungeon.DungeonCommands.register(dispatcher);
        dispatcher.register(Commands.literal("statlevel")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("all")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                .executes(ctx -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        PlayerStatData data = player.getData(ModAttachments.STATS);
                                        for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
                                            data.addLevels(i, amount);
                                        }
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("All stats → +" + amount + " levels"), true);
                                        SyncHelper.syncStats(player);
                                    }
                                    return 1;
                                })))
                .then(Commands.argument("index", IntegerArgumentType.integer(0, 22))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                .executes(ctx -> {
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        PlayerStatData data = player.getData(ModAttachments.STATS);
                                        data.addLevels(index, amount);
                                        StatType stat = StatType.byIndex(index);
                                        String name = stat != null ? stat.displayName : ("#" + index);
                                        int effective = RaceEffectApplier.getEffectiveLevel(player, index);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal(name + " → Lv." + effective), true);
                                        SyncHelper.syncStats(player);
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
        dispatcher.register(Commands.literal("statperk")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 9999))
                        .executes(ctx -> {
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                for (int i = 0; i < PlayerStatData.PERK_FAMILY_COUNT; i++) {
                                    data.addPerkPointsForFamily(StatFamily.values()[i], amount);
                                }
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("+ " + amount + " perk points to all families"), true);
                            }
                            return 1;
                        })));
    }

    private static void registerMagic(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("magic")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("codex")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                // SyncMagic d'abord pour que le client ait l'état frais.
                                tong.statmod.network.SyncHelper.syncMagic(player);
                                tong.statmod.network.SyncHelper.syncStats(player);
                                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                                        player, new tong.statmod.network.OpenMageCodexPayload());
                            }
                            return 1;
                        }))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                PlayerStatData data = player.getData(ModAttachments.STATS);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§6=== Magic Status ==="), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§bMagic Points:§r " + data.getMagicPoints()
                                                + " §8(unified pool)§r"), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§bRace:§r " + (data.getMagicRace() != null ? data.getMagicRace().name() : "§7not set§r")), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§bStart Branch:§r " + (data.getChosenStartBranch() != null ? data.getChosenStartBranch().id : "§7not set§r")), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§bArcane Power §7Lv§r " + data.getLevel(tong.statmod.stats.StatType.ARCANE_POWER.index)
                                                + "  §bErudition §7Lv§r " + data.getLevel(tong.statmod.stats.StatType.ERUDITION.index)), true);
                                for (MagicBranch b : MagicBranch.values()) {
                                    if (b == MagicBranch.COMMON) continue;
                                    int sp = 0; // pool legacy non utilisé sous l'économie unifiée
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

                                // Mission Q : audit éligibilité des nœuds disponibles.
                                int unlockable = 0;
                                int blockedByPoints = 0;
                                int blockedByStats = 0;
                                int blockedByPrereq = 0;
                                tong.statmod.magic.MagicEligibilityResolver.Failure dominant =
                                        tong.statmod.magic.MagicEligibilityResolver.Failure.NONE;
                                for (tong.statmod.magic.MagicNode node : tong.statmod.magic.MagicTreeCatalog.all()) {
                                    if (data.hasMagicNode(node.id())) continue;
                                    var result = tong.statmod.magic.MagicEligibilityResolver.evaluate(data, node);
                                    switch (result.failure()) {
                                        case NONE -> unlockable++;
                                        case NOT_ENOUGH_POINTS -> blockedByPoints++;
                                        case STAT_REQUIREMENT_NOT_MET -> blockedByStats++;
                                        case MISSING_PREREQ -> blockedByPrereq++;
                                        default -> {}
                                    }
                                }
                                final int finalUnlockable = unlockable;
                                final int finalByPoints = blockedByPoints;
                                final int finalByStats = blockedByStats;
                                final int finalByPrereq = blockedByPrereq;
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§a✓ Unlockable now:§r " + finalUnlockable), true);
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§c✗ Blocked:§r §epoints=" + finalByPoints
                                                + "§r §estats=" + finalByStats
                                                + "§r §eprereqs=" + finalByPrereq), true);
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
                                            if (start == null || !race.canChooseStartBranch(start)) {
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
