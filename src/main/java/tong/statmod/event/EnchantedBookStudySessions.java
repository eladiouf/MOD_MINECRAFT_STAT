package tong.statmod.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.network.BookStudyInputMessage.Action;
import tong.statmod.network.StatNetwork;
import tong.statmod.progression.xp.EnchantmentStudyXp;
import tong.statmod.progression.xp.EnchantmentStudyXp.Entry;
import tong.statmod.progression.xp.XpAction;
import tong.statmod.progression.xp.XpAwardService;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnchantedBookStudySessions {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, InputStamp> LAST_INPUTS = new HashMap<>();

    private EnchantedBookStudySessions() {
    }

    public static void input(ServerPlayer player, Action action,
            InteractionHand hand, long tick) {
        if (player == null || action == null || hand == null
                || player instanceof FakePlayer || player.isSpectator()) {
            return;
        }
        UUID playerId = player.getUUID();
        InputStamp previous = LAST_INPUTS.get(playerId);
        if (previous != null && previous.tick() == tick && previous.action() == action) {
            return;
        }
        LAST_INPUTS.put(playerId, new InputStamp(tick, action));

        switch (action) {
            case BEGIN -> begin(player, hand, tick);
            case HEARTBEAT -> heartbeat(player, hand, tick);
            case RELEASE -> release(playerId, hand);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        UUID playerId = player.getUUID();
        Session session = SESSIONS.get(playerId);
        if (session == null) {
            return;
        }
        long tick = player.serverLevel().getGameTime();
        if (!player.isAlive() || player.isSpectator()
                || BookStudyTimeline.timedOut(session.heartbeatTick(), tick)
                || !sameBook(player, session)) {
            SESSIONS.remove(playerId);
            return;
        }
        if (!BookStudyTimeline.complete(session.startTick(), tick)) {
            return;
        }

        SESSIONS.remove(playerId);
        ItemStack held = player.getItemInHand(session.hand());
        ItemStack visualBook = held.copyWithCount(1);
        int rawXp = studyXp(held);
        boolean awarded = XpAwardService.awardBookStudy(
                player, XpAction.bookStudied(rawXp), tick, () -> {
                    if (!player.isCreative()) {
                        player.getItemInHand(session.hand()).shrink(1);
                    }
                });
        if (awarded) {
            StatNetwork.sendBookStudyCompletion(player, visualBook);
            player.serverLevel().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            SESSIONS.remove(playerId);
            LAST_INPUTS.remove(playerId);
        }
    }

    private static void begin(ServerPlayer player, InteractionHand hand, long tick) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isAlive() || !held.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        SESSIONS.put(player.getUUID(),
                new Session(tick, tick, hand, held.copyWithCount(1)));
    }

    private static void heartbeat(ServerPlayer player, InteractionHand hand, long tick) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.hand() != hand || !sameBook(player, session)) {
            return;
        }
        SESSIONS.put(player.getUUID(), session.withHeartbeat(tick));
    }

    private static void release(UUID playerId, InteractionHand hand) {
        Session session = SESSIONS.get(playerId);
        if (session != null && session.hand() == hand) {
            SESSIONS.remove(playerId);
        }
    }

    private static boolean sameBook(ServerPlayer player, Session session) {
        ItemStack held = player.getItemInHand(session.hand());
        return held.is(Items.ENCHANTED_BOOK)
                && ItemStack.isSameItemSameTags(held, session.snapshot());
    }

    private static int studyXp(ItemStack book) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.deserializeEnchantments(
                EnchantedBookItem.getEnchantments(book));
        List<Entry> entries = new ArrayList<>(enchantments.size());
        enchantments.forEach((enchantment, level) -> entries.add(
                new Entry(level, rarityWeight(enchantment.getRarity()))));
        return EnchantmentStudyXp.calculate(entries);
    }

    private static int rarityWeight(Enchantment.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
    }

    private record Session(long startTick, long heartbeatTick,
            InteractionHand hand, ItemStack snapshot) {
        Session withHeartbeat(long tick) {
            return new Session(startTick, tick, hand, snapshot);
        }
    }

    private record InputStamp(long tick, Action action) {
    }
}
