package tong.statmod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.stats.StatType;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void statmod_renderStatBadge(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        int globalLevel = ClientStatsCache.getGlobalLevel();
        if (globalLevel <= 0) return;

        String text = "Lv." + globalLevel;
        int color = globalLevel >= 80 ? 0xFFFFAA00 : globalLevel >= 50 ? 0xFFAAAAFF : 0xFFFFFFFF;
        graphics.drawString(mc.font, text, 4, 4, color);
        graphics.drawString(mc.font, "§7[STAT Mod]", 4 + mc.font.width(text) + 4, 4, 0xFF888888);
    }
}
