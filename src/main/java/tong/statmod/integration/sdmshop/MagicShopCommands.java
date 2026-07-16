package tong.statmod.integration.sdmshop;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import tong.statmod.StatMod;

public final class MagicShopCommands {
    private MagicShopCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statmod")
                .then(Commands.literal("shop")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.literal("regenerate")
                                .executes(context -> regenerate(context.getSource())))));
    }

    private static int regenerate(CommandSourceStack source) {
        try {
            MagicShopGenerator.GenerationResult result =
                    MagicShopGenerator.regenerate(source.getServer());
            source.sendSuccess(() -> Component.translatable(
                    "command.statmod.shop.generated", result.spells(), result.scrollEntries(),
                    result.saleEntries(), result.skippedSpells()), true);
            return result.scrollEntries() + result.saleEntries();
        } catch (Exception exception) {
            StatMod.LOGGER.error("[Shop] Manual regeneration failed", exception);
            source.sendFailure(Component.translatable(
                    "command.statmod.shop.failed", exception.getClass().getSimpleName()));
            return 0;
        }
    }
}
