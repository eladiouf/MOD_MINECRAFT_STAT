package tong.statmod.client.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

import java.util.EnumMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class EpicFightRaceCosmeticLayer extends PatchedLayer<
        AbstractClientPlayer,
        AbstractClientPlayerPatch<AbstractClientPlayer>,
        PlayerModel<AbstractClientPlayer>,
        RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> {
    private final Map<PlayerSkin.Model, RaceCosmeticLayer> layersByModel = new EnumMap<>(PlayerSkin.Model.class);

    @Override
    protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> playerPatch,
                               AbstractClientPlayer player,
                               RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderLayer,
                               PoseStack poseStack,
                               MultiBufferSource buffer,
                               int packedLight,
                               OpenMatrix4f[] poses,
                               float bob,
                               float yRot,
                               float xRot,
                               float partialTicks) {
        PlayerRenderer playerRenderer = resolveRenderer(player);
        if (playerRenderer == null) {
            return;
        }

        RaceCosmeticLayer cosmeticLayer = layersByModel.computeIfAbsent(
                player.getSkin().model(),
                model -> new RaceCosmeticLayer(playerRenderer, Minecraft.getInstance().getEntityModels())
        );
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();

        float limbSwingAmount = 0.0f;
        float limbSwing = 0.0f;
        if (!player.isPassenger() && player.isAlive()) {
            limbSwingAmount = Math.min(player.walkAnimation.speed(partialTicks), 1.0f);
            limbSwing = player.walkAnimation.position(partialTicks);
            if (player.isBaby()) {
                limbSwing *= 3.0f;
            }
        }

        float bodyYaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTicks, player.yHeadRotO, player.yHeadRot) - bodyYaw;
        if (player.isPassenger() && player.getVehicle() instanceof LivingEntity living) {
            bodyYaw = Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot);
            headYaw = Mth.wrapDegrees(Mth.rotLerp(partialTicks, player.yHeadRotO, player.yHeadRot) - bodyYaw);
        }

        float headPitch = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
        float ageInTicks = player.tickCount + partialTicks;
        boolean shouldSit = player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit();

        playerModel.attackTime = player.getAttackAnim(partialTicks);
        playerModel.riding = shouldSit;
        playerModel.young = player.isBaby();
        playerModel.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        playerModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);

        cosmeticLayer.render(
                poseStack,
                buffer,
                packedLight,
                player,
                limbSwing,
                limbSwingAmount,
                partialTicks,
                ageInTicks,
                headYaw,
                headPitch
        );
    }

    private static PlayerRenderer resolveRenderer(AbstractClientPlayer player) {
        if (player == null) {
            return null;
        }
        return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer playerRenderer
                ? playerRenderer
                : null;
    }
}
