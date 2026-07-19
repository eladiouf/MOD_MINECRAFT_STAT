package tong.statmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.entity.AdventurerEntity;

/**
 * Rend l'aventurier avec le <b>modèle de joueur</b> et un <b>skin de joueur</b> (skins par défaut
 * de Minecraft : Steve, Ari, Kai, Sunny, Zuri — aucun asset à embarquer). L'armure et l'arme tenue
 * s'affichent (couche armure + couche item-en-main héritée de HumanoidMobRenderer).
 */
public class AdventurerRenderer extends HumanoidMobRenderer<AdventurerEntity, PlayerModel<AdventurerEntity>> {

    private static final ResourceLocation[] SKINS = {
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png"),
            new ResourceLocation("minecraft", "textures/entity/player/wide/ari.png"),
            new ResourceLocation("minecraft", "textures/entity/player/wide/kai.png"),
            new ResourceLocation("minecraft", "textures/entity/player/wide/sunny.png"),
            new ResourceLocation("minecraft", "textures/entity/player/wide/zuri.png"),
    };

    public static final int SKIN_COUNT = SKINS.length;

    public AdventurerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(AdventurerEntity entity) {
        return SKINS[Math.floorMod(entity.getSkin(), SKINS.length)];
    }
}
