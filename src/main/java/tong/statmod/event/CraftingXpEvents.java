package tong.statmod.event;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.progression.xp.StatItemTags;
import tong.statmod.progression.xp.XpAction;
import tong.statmod.progression.xp.XpAwardService;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class CraftingXpEvents {
    private CraftingXpEvents() {
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getCrafting();
        if (stack.is(StatItemTags.FORGEABLE_EQUIPMENT)) {
            XpAwardService.award(player,
                    List.of(XpAction.forging(stack.getMaxDamage(), stack.getCount())),
                    player.serverLevel().getGameTime());
        }
    }

    @SubscribeEvent
    public static void smelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getSmelting();
        if (stack.isEdible()) {
            XpAwardService.award(player,
                    List.of(XpAction.cooking(stack.getCount())),
                    player.serverLevel().getGameTime());
        }
    }

    @SubscribeEvent
    public static void brewed(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(event.getStack());
        if (effects.isEmpty()) {
            return;
        }
        int amplifierSum = effects.stream().mapToInt(MobEffectInstance::getAmplifier).sum();
        XpAwardService.award(player,
                List.of(XpAction.alchemy(effects.size(), amplifierSum)),
                player.serverLevel().getGameTime());
    }
}
