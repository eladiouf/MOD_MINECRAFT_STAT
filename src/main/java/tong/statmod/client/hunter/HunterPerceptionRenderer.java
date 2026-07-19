package tong.statmod.client.hunter;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, value = Dist.CLIENT)
public final class HunterPerceptionRenderer {
    private static final float AMBER_RED = 1.0F;
    private static final float AMBER_GREEN = 0.62F;
    private static final float AMBER_BLUE = 0.10F;
    private static final float THREAT_RED = 1.0F;
    private static final float THREAT_GREEN = 0.15F;
    private static final float THREAT_BLUE = 0.15F;

    private HunterPerceptionRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }

        OptionalInt marked = ClientHunterPerception.markedEntityId();
        int markedId = marked.orElse(-1);
        if (markedId < 0 && ClientHunterPerception.threatEntityIds().isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        RenderSystem.disableDepthTest();
        try {
            if (markedId >= 0) {
                renderEntity(level.getEntity(markedId), poseStack, lines, camera, event.getPartialTick(),
                        AMBER_RED, AMBER_GREEN, AMBER_BLUE);
            }
            for (int entityId : ClientHunterPerception.threatEntityIds()) {
                if (entityId == markedId) {
                    continue;
                }
                renderEntity(level.getEntity(entityId), poseStack, lines, camera, event.getPartialTick(),
                        THREAT_RED, THREAT_GREEN, THREAT_BLUE);
            }
            buffers.endBatch(RenderType.lines());
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderEntity(
            Entity entity,
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 camera,
            float partialTick,
            float red,
            float green,
            float blue) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
            return;
        }
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        AABB box = entity.getBoundingBox()
                .move(x - entity.getX() - camera.x, y - entity.getY() - camera.y, z - entity.getZ() - camera.z)
                .inflate(0.05D);
        LevelRenderer.renderLineBox(poseStack, lines, box, red, green, blue, 1.0F);
    }
}
