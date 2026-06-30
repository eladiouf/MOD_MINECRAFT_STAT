package tong.statmod.client.cosmetic;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

/**
 * Holds the {@link ModelLayerLocation}s and {@link LayerDefinition} factories for the
 * race cosmetic overlays (ears, beard, tail). Parts are deliberately small and UV-mapped
 * into the head-skin area (UV 8,8) so they pick up the player's actual skin tone.
 *
 * Texture grid is 64x64 (standard player skin format).
 */
public final class RaceCosmeticModels {
    private static final int TEX_W = 64;
    private static final int TEX_H = 64;

    public static final ModelLayerLocation ELF_EARS       = layer("elf_ears");
    public static final ModelLayerLocation DWARF_BEARD    = layer("dwarf_beard");
    public static final ModelLayerLocation BEASTFOLK_EARS = layer("beastfolk_ears");
    public static final ModelLayerLocation BEASTFOLK_TAIL = layer("beastfolk_tail");

    private RaceCosmeticModels() {}

    public static LayerDefinition elfEars() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Two pointy ears jutting outward from the sides of the head.
        // Head origin is centered at the player's neck base; head box is 8x8x8 from (-4,-8,-4) to (4,0,4).
        root.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(8, 8).addBox(0.0F, -1.5F, -1.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, -7.0F, 0.0F, 0.0F, 0.0F, (float) Math.toRadians(-25.0)));
        root.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(8, 8).addBox(-2.0F, -1.5F, -1.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, -7.0F, 0.0F, 0.0F, 0.0F, (float) Math.toRadians(25.0)));
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    public static LayerDefinition dwarfBeard() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Beard hanging from chin: a flat block in front of the lower face.
        root.addOrReplaceChild("beard",
                CubeListBuilder.create().texOffs(8, 8).addBox(-3.0F, -3.0F, -4.5F, 6.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    public static LayerDefinition beastfolkEars() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Two upright triangular-ish ears on top of the head.
        root.addOrReplaceChild("ear_top_left",
                CubeListBuilder.create().texOffs(8, 8).addBox(0.5F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        root.addOrReplaceChild("ear_top_right",
                CubeListBuilder.create().texOffs(8, 8).addBox(-2.5F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    public static LayerDefinition beastfolkTail() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Tail extending behind the lower body. Body box: 8x12x4 from (-4,0,-2) to (4,12,2).
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(16, 16).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, (float) Math.toRadians(35.0), 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, name),
                "main"
        );
    }
}
