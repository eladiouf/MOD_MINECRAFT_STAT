package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.item.IScroll;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.magic.ScrollLearningService;
import tong.statmod.network.StatNetwork;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IronScrollLearningEvents {
    private IronScrollLearningEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickScroll(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IScroll)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<IronScrollDescriptor.Descriptor> descriptor =
                IronScrollDescriptor.describe(stack);
        if (descriptor.isEmpty()) {
            player.displayClientMessage(Component.translatable("statmod.scroll.invalid"), true);
            return;
        }
        tong.statmod.stats.PlayerStats stats = StatCapabilities.get(player);
        if (stats == null) {
            player.displayClientMessage(Component.translatable("statmod.scroll.invalid"), true);
            return;
        }
        IronScrollDescriptor.Descriptor value = descriptor.orElseThrow();
        ScrollLearningService.Outcome outcome = ScrollLearningService.apply(
                stats.learnedSpells(), value.id(), value.level(),
                value.minLevel(), value.maxLevel());
        if (outcome.consume() && !player.isCreative()) {
            stack.shrink(1);
        }
        sendOutcomeMessage(player, outcome);
        if (outcome.consume()) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                    0.8F, 1.1F);
            StatNetwork.sendSnapshot(player);
        }
    }

    private static void sendOutcomeMessage(
            ServerPlayer player, ScrollLearningService.Outcome outcome) {
        String key = switch (outcome.status()) {
            case LEARNED -> "statmod.scroll.learned";
            case UPGRADED -> "statmod.scroll.upgraded";
            case ALREADY_KNOWN -> "statmod.scroll.already_known";
            case FULL -> "statmod.scroll.library_full";
            case INVALID -> "statmod.scroll.invalid";
        };
        player.displayClientMessage(Component.translatable(
                key, outcome.spellId(), outcome.newLevel()), true);
    }
}
