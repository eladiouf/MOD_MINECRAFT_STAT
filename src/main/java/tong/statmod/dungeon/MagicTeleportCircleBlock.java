package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tong.statmod.sound.ModSounds;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MagicTeleportCircleBlock extends Block {
    public enum CirclePart implements net.minecraft.util.StringRepresentable {
        NORTH_WEST("north_west"),
        NORTH("north"),
        NORTH_EAST("north_east"),
        WEST("west"),
        CENTER("center"),
        EAST("east"),
        SOUTH_WEST("south_west"),
        SOUTH("south"),
        SOUTH_EAST("south_east");

        private final String name;

        CirclePart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<CirclePart> PART = net.minecraft.world.level.block.state.properties.EnumProperty.create("part", CirclePart.class);

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final Map<UUID, Long> LAST_TELEPORT = new ConcurrentHashMap<>();

    public MagicTeleportCircleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, CirclePart.CENTER));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    public static void applyCooldown(UUID playerUuid, long gameTime) {
        // Enregistre un cooldown de 3 secondes + 5 secondes bonus (100 ticks)
        // pour laisser au joueur le temps de s'écarter à son retour dans l'Overworld.
        LAST_TELEPORT.put(playerUuid, gameTime + 100L);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            UUID uuid = serverPlayer.getUUID();
            long now = level.getGameTime();
            long lastTp = LAST_TELEPORT.getOrDefault(uuid, 0L);

            if (now - lastTp > 60L) { // 3 secondes de cooldown
                LAST_TELEPORT.put(uuid, now);

                PlayerStatData data = serverPlayer.getData(ModAttachments.STATS);

                data.setLastOverworldDimensionId(level.dimension().location().toString());
                data.setLastOverworldPos(pos.asLong());

                boolean ok = DungeonTeleportHandler.enterFloor(serverPlayer, 0);
                if (ok) {
                    serverPlayer.playNotifySound(ModSounds.DUNGEON_PORTAL_ENTER.get(), SoundSource.BLOCKS, 0.7f, 1.3f);
                    serverPlayer.displayClientMessage(
                            Component.translatable("block.statmod.dungeon_portal.enter_hub"), true);
                } else {
                    serverPlayer.displayClientMessage(
                            Component.translatable("block.statmod.dungeon_portal.failed"), true);
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.1D;
        double centerZ = pos.getZ() + 0.5D;

        for (int i = 0; i < 4; i++) {
            double angle = random.nextDouble() * 2.0D * Math.PI;
            double radius = 0.3D + random.nextDouble() * 0.3D;
            double px = centerX + Math.cos(angle) * radius;
            double pz = centerZ + Math.sin(angle) * radius;

            level.addParticle(ParticleTypes.WITCH, px, centerY, pz, 0.0D, 0.02D + random.nextDouble() * 0.03D, 0.0D);
            level.addParticle(ParticleTypes.PORTAL, px, centerY, pz, 0.0D, 0.01D, 0.0D);
        }
    }
}
