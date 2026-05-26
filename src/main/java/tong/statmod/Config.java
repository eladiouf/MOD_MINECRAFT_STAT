package tong.statmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = STATMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG = BUILDER
            .comment("Activer les logs de debug pour les stats")
            .define("enableDebug", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableDebug;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enableDebug = ENABLE_DEBUG.get();
    }
}
