package tong.statmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.nameless.indestructible.main.Indestructible")
public class IndestructibleServerMixin {

    @Redirect(
        method = "<init>(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;)V",
        at = @At(
            value = "FIELD",
            target = "Lcom/nameless/indestructible/client/UIConfig;SPEC:Lnet/neoforged/neoforge/common/ModConfigSpec;",
            opcode = 178 // GETSTATIC
        ),
        remap = false
    )
    private net.neoforged.neoforge.common.ModConfigSpec statmod$redirectUIConfigSpec() {
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            try {
                Class<?> clazz = Class.forName("com.nameless.indestructible.client.UIConfig");
                return (net.neoforged.neoforge.common.ModConfigSpec) clazz.getField("SPEC").get(null);
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    @Redirect(
        method = "<init>(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/fml/ModContainer;registerConfig(Lnet/neoforged/fml/config/ModConfig$Type;Lnet/neoforged/fml/config/IConfigSpec;)V"
        ),
        remap = false
    )
    private void statmod$redirectRegisterConfig(net.neoforged.fml.ModContainer container, net.neoforged.fml.config.ModConfig.Type type, net.neoforged.fml.config.IConfigSpec spec) {
        if (type == net.neoforged.fml.config.ModConfig.Type.CLIENT && net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER) {
            // Ignorer l'enregistrement de la configuration cliente sur le serveur dédié
            return;
        }
        container.registerConfig(type, spec);
    }
}
