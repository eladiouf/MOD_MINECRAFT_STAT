package tong.statmod.integration.sdm;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = "statmod")
public final class SDMWeaponPoisonHandler {

    static final String TAG_POISON_DURATION = "statmod_poison_duration";
    static final String TAG_POISON_AMPLIFIER = "statmod_poison_amplifier";

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack weapon = player.getMainHandItem();
        CustomData custom = weapon.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = custom.copyTag();
        if (!tag.contains(TAG_POISON_DURATION) || !tag.contains(TAG_POISON_AMPLIFIER)) return;

        int duration = tag.getInt(TAG_POISON_DURATION);
        int amplifier = tag.getInt(TAG_POISON_AMPLIFIER);
        event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
    }
}
