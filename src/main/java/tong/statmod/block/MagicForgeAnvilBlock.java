package tong.statmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bloc d'enclume magique avec silhouette non cubique et particules ambiantes.
 */
public class MagicForgeAnvilBlock extends Block {

    public enum AmbientParticle {
        ENCHANTMENT,
        INFUSION
    }

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 1.0D, 13.0D, 3.0D, 4.0D),
            Block.box(3.0D, 0.0D, 12.0D, 13.0D, 3.0D, 15.0D),
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 3.0D, 12.0D),
            Block.box(5.0D, 3.0D, 3.0D, 11.0D, 4.0D, 13.0D),
            Block.box(6.0D, 4.0D, 4.0D, 10.0D, 9.0D, 12.0D),
            Block.box(3.0D, 9.0D, 3.0D, 13.0D, 16.0D, 13.0D),
            Block.box(4.0D, 11.0D, 13.0D, 12.0D, 16.0D, 16.0D),
            Block.box(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 3.0D));

    private final AmbientParticle ambientParticle;

    public MagicForgeAnvilBlock(AmbientParticle ambientParticle, BlockBehaviour.Properties properties) {
        super(properties);
        this.ambientParticle = ambientParticle;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + 0.22D + random.nextDouble() * 0.56D;
            double y = pos.getY() + 0.82D + random.nextDouble() * 0.48D;
            double z = pos.getZ() + 0.22D + random.nextDouble() * 0.56D;
            if (ambientParticle == AmbientParticle.ENCHANTMENT) {
                double dx = (pos.getX() + 0.5D - x) * 0.03D;
                double dy = 0.02D + random.nextDouble() * 0.02D;
                double dz = (pos.getZ() + 0.5D - z) * 0.03D;
                level.addParticle(ParticleTypes.ENCHANT, x, y, z, dx, dy, dz);
                continue;
            }

            double dx = (random.nextDouble() - 0.5D) * 0.01D;
            double dy = 0.015D + random.nextDouble() * 0.025D;
            double dz = (random.nextDouble() - 0.5D) * 0.01D;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, dx, dy, dz);
        }
    }
}
