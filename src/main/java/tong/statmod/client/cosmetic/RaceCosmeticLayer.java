package tong.statmod.client.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.integration.PlayerDataBridge;

@OnlyIn(Dist.CLIENT)
public class RaceCosmeticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ModelPart elfEars;
    private final ModelPart dwarfBeard;
    private final ModelPart beastfolkEars;
    private final ModelPart beastfolkTail;

    public RaceCosmeticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                             EntityModelSet modelSet) {
        super(parent);
        this.elfEars       = modelSet.bakeLayer(RaceCosmeticModels.ELF_EARS);
        this.dwarfBeard    = modelSet.bakeLayer(RaceCosmeticModels.DWARF_BEARD);
        this.beastfolkEars = modelSet.bakeLayer(RaceCosmeticModels.BEASTFOLK_EARS);
        this.beastfolkTail = modelSet.bakeLayer(RaceCosmeticModels.BEASTFOLK_TAIL);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks,
                       float headYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) {
            return;
        }

        String raceId;
        try {
            raceId = PlayerDataBridge.getRaceId(player);
        } catch (Throwable t) {
            return;
        }
        if (raceId == null) {
            return;
        }

        ResourceLocation skin = player.getSkin().texture();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        switch (raceId) {
            case "tensura:elf" -> renderOnHead(pose, vc, packedLight, model, elfEars);
            case "tensura:dwarf" -> renderOnHead(pose, vc, packedLight, model, dwarfBeard);
            case "tensura:beastfolk" -> {
                renderOnHead(pose, vc, packedLight, model, beastfolkEars);
                renderOnBody(pose, vc, packedLight, model, beastfolkTail);
            }
            default -> { /* human or unknown — render nothing */ }
        }
    }

    private void renderOnHead(PoseStack pose, VertexConsumer vc, int light,
                              PlayerModel<AbstractClientPlayer> model, ModelPart part) {
        pose.pushPose();
        model.head.translateAndRotate(pose);
        part.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private void renderOnBody(PoseStack pose, VertexConsumer vc, int light,
                              PlayerModel<AbstractClientPlayer> model, ModelPart part) {
        pose.pushPose();
        model.body.translateAndRotate(pose);
        part.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
