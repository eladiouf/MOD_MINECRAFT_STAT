package tong.statmod.client.feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tong.statmod.client.hud.overlays.GlobalLevelOverlay;

import java.util.*;

public class CombatFeedbackRenderer {
    private static final List<FloatingText> floatingTexts = Collections.synchronizedList(new ArrayList<>());

    public record FloatingText(String text, int color, Vec3 pos, long spawnTime, float velY) {}

    public static void addDamageNumber(LivingEntity entity, float damage, boolean isCrit) {
        String text = isCrit ? "\u2727 " + Math.round(damage) + " \u2727" : "" + Math.round(damage);
        int color = isCrit ? 0xFFFF5500 : 0xFFFFFFFF;
        floatingTexts.add(new FloatingText(text, color,
            entity.position().add(0, entity.getBbHeight() / 2, 0),
            System.currentTimeMillis(), 0.05f));
        GlobalLevelOverlay.onHit();
    }

    public static void addXpPopup(LivingEntity entity, int xp, String statName) {
        floatingTexts.add(new FloatingText("+" + xp + " XP " + statName, 0xFF55FF55,
            entity.position().add(0, entity.getBbHeight() + 0.5, 0),
            System.currentTimeMillis(), 0.03f));
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        List<FloatingText> toRemove = new ArrayList<>();
        synchronized (floatingTexts) {
            for (FloatingText ft : floatingTexts) {
                long age = System.currentTimeMillis() - ft.spawnTime;
                if (age > 2000) { toRemove.add(ft); continue; }

                Vec3 playerPos = mc.player.position();
                Vec3 diff = ft.pos.subtract(playerPos);
                if (diff.length() > 50) { toRemove.add(ft); continue; }

                double renderX = mc.getWindow().getGuiScaledWidth() / 2.0 + diff.x * 10;
                double renderY = mc.getWindow().getGuiScaledHeight() / 2.0 - diff.y * 10 - ft.velY * age * 5;
                double renderZ = diff.z;

                int alpha = (int)(Math.max(0, (2000 - age) / 2000.0) * 255);
                int color = (alpha << 24) | (ft.color & 0x00FFFFFF);

                float scale = 1.0f + age / 2000.0f * 0.3f;
                graphics.pose().pushPose();
                graphics.pose().translate(renderX, renderY, 0);
                graphics.pose().scale(scale, scale, 1);
                graphics.drawCenteredString(mc.font, ft.text, 0, 0, color);
                graphics.pose().popPose();
            }
            floatingTexts.removeAll(toRemove);
        }
    }
}
