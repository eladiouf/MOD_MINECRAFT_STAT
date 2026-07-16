package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tong.statmod.capability.StatCapabilities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonNextFloorTeleporterBlock extends Block {

    private static final Map<UUID, Long> MSG_COOLDOWN = new ConcurrentHashMap<>();

    public DungeonNextFloorTeleporterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        tryTeleport(sp, pos, level, true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            tryTeleport(sp, pos, level, false);
        }
    }

    private void tryTeleport(ServerPlayer sp, BlockPos pos, Level level, boolean isRightClick) {
        int currentFloor = DungeonTeleportHandler.floorAtPos(pos.getX(), pos.getZ());
        int nextFloor = currentFloor + 1;
        long now = level.getGameTime();
        long lastMsg = MSG_COOLDOWN.getOrDefault(sp.getUUID(), 0L);

        sp.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(data -> {
            // Bypass lock if in creative mode (instabuild)
            if (sp.getAbilities().instabuild || nextFloor <= data.getDungeonFloorReached()) {
                DungeonTeleportHandler.enterFloor(sp, nextFloor);
            } else {
                if (isRightClick || (now - lastMsg > 60L)) {
                    MSG_COOLDOWN.put(sp.getUUID(), now);
                    DungeonObjective objective = DungeonObjective.forFloor(currentFloor);
                    sp.displayClientMessage(Component.translatable(
                            "block.statmod.next_floor_teleporter.locked_objective",
                            Component.translatable(objective.translationKey())), true);
                }
            }
        });
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
            double y = pos.getY() + 1.0D + random.nextDouble() * 0.5D;
            double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
            double dx = (pos.getX() + 0.5D - x) * 0.02D;
            double dy = 0.03D + random.nextDouble() * 0.03D;
            double dz = (pos.getZ() + 0.5D - z) * 0.02D;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.WITCH, x, y, z, dx, dy, dz);
        }
    }
}
