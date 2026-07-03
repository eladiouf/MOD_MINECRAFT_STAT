package tong.statmod.block;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicForgeBlockBehaviorTest {

    private static final Path JAVA_SRC = Paths.get("src", "main", "java", "tong", "statmod", "block");

    @Test
    void magicForgeBlock_usesAnAnvilLikeVoxelShapeAndParticles() throws IOException {
        String content = Files.readString(JAVA_SRC.resolve("MagicForgeAnvilBlock.java"));

        assertTrue(content.contains("Shapes.or("), "magic forge block should compose a custom voxel shape");
        assertTrue(content.contains("getOcclusionShape"), "magic forge block should expose a custom occlusion shape");
        assertTrue(content.contains("ParticleTypes.ENCHANT"), "enchantment anvil should emit enchant particles");
        assertTrue(content.contains("ParticleTypes.END_ROD"), "infusion forge should emit end rod particles");
    }

    @Test
    void forgingBlocks_registerMagicForgesWithoutOcclusion() throws IOException {
        String content = Files.readString(JAVA_SRC.resolve("ForgingBlocks.java"));

        assertTrue(content.contains("InfusionForgeBlock(") || content.contains("new MagicForgeAnvilBlock"),
                "magic forge blocks should use the dedicated custom block class or its subclass");
        assertTrue(content.contains("EnchantmentAnvilBlock(") || content.contains("new MagicForgeAnvilBlock"),
                "magic forge blocks should use the dedicated custom block class or its subclass");
        assertTrue(content.contains(".noOcclusion()"),
                "magic forge blocks should opt out of full cube occlusion");
    }
}
