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
import tong.statmod.config.Config;
import tong.statmod.integration.sdm.SDMEconomyBridge;
import tong.statmod.network.SyncHelper;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.stats.PlayerStats;

import net.minecraft.world.item.ItemStack;

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
                                PlayerStats data = StatCapabilities.get(player);
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
                                        PlayerStats data = StatCapabilities.get(player);
                                        data.unlockDungeonFloor(floor);
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "Unlocked floor " + floor), true);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                PlayerStats data = StatCapabilities.get(player);
                                data.unlockDungeonFloor(1);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "Dungeon progress reset to floor 1"), true);
                            }
                            return 1;
                        }))
                .then(Commands.literal("givemages")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.ZOMBIE_SPAWN_EGG, "statmod:pyromancer_mob", "§cŒuf de Zombie Pyromancien", "minecraft:zombie"));
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.SKELETON_SPAWN_EGG, "statmod:cryomancer_mob", "§bŒuf de Squelette Cryomancien", "minecraft:skeleton"));
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.SKELETON_SPAWN_EGG, "statmod:electromancer_mob", "§eŒuf de Squelette Électromancien", "minecraft:skeleton"));
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.WITHER_SKELETON_SPAWN_EGG, "statmod:wither_mage_mob", "§dŒuf de Mage du Wither", "minecraft:wither_skeleton"));
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.ZOMBIE_SPAWN_EGG, "statmod:cleric_mob", "§aŒuf de Clerc de Combat (Spawn l'escouade !)", "minecraft:zombie"));
                                
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.ZOMBIE_SPAWN_EGG, "statmod:hollow_witch_mob", "§8Œuf de Carcasse Sorcière (SLU)", "minecraft:zombie"));
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.ZOMBIE_SPAWN_EGG, "statmod:mage_knight_mob", "§6Œuf de Chevalier Mage (SLU)", "minecraft:zombie"));
                                player.getInventory().add(createMageSpawnEgg(net.minecraft.world.item.Items.ZOMBIE_SPAWN_EGG, "statmod:void_knight_mob", "§5Œuf de Chevalier du Néant (SLU)", "minecraft:zombie"));
                                
                                ctx.getSource().sendSuccess(() -> Component.literal("§a8 œufs de spawn magiques ajoutés à votre inventaire !"), true);
                            }
                            return 1;
                        }))
                .then(Commands.literal("arena")
                        .then(Commands.literal("tp").executes(ctx -> {
                            ServerLevel dungeon = ctx.getSource().getServer()
                                    .getLevel(DungeonDimensions.TRIAL_DUNGEON);
                            if (dungeon == null || !(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                return 0;
                            }
                            tong.statmod.dungeon.city.CityArenaController.teleport(player, dungeon);
                            ctx.getSource().sendSuccess(() -> Component.literal("Téléporté dans l'arène"), false);
                            return 1;
                        }))
                        .then(Commands.literal("open").executes(ctx -> {
                            ServerLevel dungeon = ctx.getSource().getServer()
                                    .getLevel(DungeonDimensions.TRIAL_DUNGEON);
                            if (dungeon == null) return 0;
                            tong.statmod.dungeon.city.CityArenaController.openGate(dungeon);
                            ctx.getSource().sendSuccess(() -> Component.literal("Porte de l'arène ouverte"), true);
                            return 1;
                        }))
                        .then(Commands.literal("close").executes(ctx -> {
                            ServerLevel dungeon = ctx.getSource().getServer()
                                    .getLevel(DungeonDimensions.TRIAL_DUNGEON);
                            if (dungeon == null) return 0;
                            tong.statmod.dungeon.city.CityArenaController.closeGate(dungeon);
                            ctx.getSource().sendSuccess(() -> Component.literal("Porte de l'arène fermée"), true);
                            return 1;
                        })))
                .then(Commands.literal("regen")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(0, 10000))
                                .executes(ctx -> {
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    ServerLevel dungeon = ctx.getSource().getServer()
                                            .getLevel(DungeonDimensions.TRIAL_DUNGEON);
                                    if (dungeon == null) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "Dimension statmod:trial_dungeon introuvable"));
                                        return 0;
                                    }
                                    // Étage 0 = Cité des Aventuriers : purge + rebuild en tâche de fond.
                                    if (floor == 0) {
                                        tong.statmod.dungeon.city.CityGenerator.regen(dungeon);
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "Regen de la Cité programmée (progression en tâche de fond)"), true);
                                        return 1;
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

    /**
     * Commande joueur (sans OP) pour convertir les points de donjon en coins FDP_cfa.
     * {@code /dungeon convert [amount]} — sans argument convertit tout ce qui est possible
     * en gardant 1 point (évite l'éjection). Réutilise l'infrastructure existante.
     */
    public static void registerPlayerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dungeon")
                .then(Commands.literal("convert")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000))
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        int requested = IntegerArgumentType.getInteger(ctx, "amount");
                                        return doConvert(player, requested);
                                    }
                                    return 0;
                                }))
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                // Convertit tous les points sauf 1 (évite éjection)
                                return doConvertAll(player);
                            }
                            return 0;
                        })));
    }

    private static int doConvert(ServerPlayer player, int requested) {
        PlayerStats data = StatCapabilities.get(player);
        int points = data.getDungeonPoints();
        if (points <= 0) {
            player.sendSystemMessage(Component.translatable("dungeon.convert.no_points"));
            return 0;
        }
        double rate = Config.getPointToCoinRate();
        PointExchange.Result r = PointExchange.compute(requested, points, rate);
        if (r.converted() <= 0) {
            player.sendSystemMessage(Component.translatable("dungeon.convert.no_points"));
            return 0;
        }
        boolean credited = SDMEconomyBridge.addCoins(player, r.coins());
        if (!credited) {
            player.sendSystemMessage(Component.translatable("shop.unavailable"));
            return 0;
        }
        data.addDungeonPoints(-r.converted());
        SyncHelper.syncStats(player);
        player.sendSystemMessage(Component.translatable("dungeon.convert.success",
                r.converted(), r.coins(), data.getDungeonPoints()));
        return 1;
    }

    private static int doConvertAll(ServerPlayer player) {
        PlayerStats data = StatCapabilities.get(player);
        int points = data.getDungeonPoints();
        // Garder 1 point pour éviter l'éjection (DungeonPointsEjection)
        int max = Math.max(0, points - 1);
        if (max <= 0) {
            player.sendSystemMessage(Component.translatable("dungeon.convert.no_points"));
            return 0;
        }
        return doConvert(player, max);
    }

    private static ItemStack createMageSpawnEgg(net.minecraft.world.item.Item eggType, String mageType, String name, String entityId) {
        ItemStack stack = new ItemStack(eggType);
        net.minecraft.nbt.CompoundTag entityData = new net.minecraft.nbt.CompoundTag();
        entityData.putString("id", entityId);
        
        // Under Forge 1.20.1, getPersistentData() resolves to ForgeData (instead of NeoForgeData)
        net.minecraft.nbt.CompoundTag forgeData = new net.minecraft.nbt.CompoundTag();
        forgeData.putString("statmod_custom_mage_type", mageType);
        forgeData.putBoolean(DungeonSpawnGuard.AUTHORIZED_TAG, true);
        entityData.put("ForgeData", forgeData);
        
        stack.getOrCreateTag().put("EntityTag", entityData);
        stack.setHoverName(Component.literal(name));
        return stack;
    }
}
