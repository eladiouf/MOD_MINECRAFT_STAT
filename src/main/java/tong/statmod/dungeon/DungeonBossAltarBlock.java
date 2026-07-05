package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import tong.statmod.STATMod;
import tong.statmod.sound.ModSounds;

import java.util.List;

public class DungeonBossAltarBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public DungeonBossAltarBlock() {
        super(Properties.of()
                .strength(50.0F, 1200.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> state.getValue(ACTIVE) ? 12 : 3)
                .noOcclusion()
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!state.getValue(ACTIVE)) {
            sp.displayClientMessage(Component.literal("Cet autel a déjà été utilisé."), true);
            return InteractionResult.CONSUME;
        }

        int floor = DungeonTeleportHandler.floorAtPos(pos.getX(), pos.getZ());
        List<DungeonBossRoster.BossEntry> roster = DungeonBossRoster.forFloor(floor);
        ServerLevel sl = (ServerLevel) level;

        // Les boss sont marqués AUTHORIZED_TAG par spawnAuthorized (passent le garde). Leurs
        // invocations/compagnons passent tant que le combat est suivi (DungeonBossTracker.isTracked),
        // ce que register() ci-dessous rend vrai dès le premier boss posé.

        // Une BossEntry peut contenir PLUSIEURS ids séparés par des virgules (mode duo/vague) :
        // "slu:boss_ornstein,slu:boss_smough". On les éclate tous ici — sinon resolveType échouait
        // sur la chaîne à virgules et RIEN ne spawnait (bug « aucun boss à partir de l'étage 40 »,
        // les étages ×10 ≥ 40 étant presque tous des duos/vagues).
        int spawned = 0;
        int slot = 0;
        for (DungeonBossRoster.BossEntry entry : roster) {
            for (String rawId : entry.entityId().split(",")) {
                EntityType<?> type = resolveType(rawId.trim());
                if (type == null) continue;
                BlockPos spawnPos = pos.offset((slot % 2 == 0 ? -1 : 1) * (slot + 1), 2, 3 + slot * 3);
                slot++;
                // Dégage un espace d'air 1×3 au point de spawn (le boss ne naît pas encastré).
                for (int dy = 0; dy < 3; dy++) {
                    sl.setBlock(spawnPos.above(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
                var spawnedEntity = DungeonSpawnGuard.spawnAuthorized(
                        () -> type.spawn(sl, spawnPos, MobSpawnType.STRUCTURE));
                if (spawnedEntity != null) {
                    spawned++;
                    // Enregistre ce boss : l'étage ne se débloque qu'au clear complet (tous morts).
                    DungeonBossTracker.register(floor, spawnedEntity.getUUID());
                    // La difficulté L2 par étage est calée par DungeonSpawnGuard.onEntityJoinLevel.
                }
            }
        }

        if (spawned > 0) {
            level.setBlock(pos, state.setValue(ACTIVE, false), 3);
            sp.playNotifySound(ModSounds.DUNGEON_PORTAL_ENTER.get(), SoundSource.BLOCKS, 0.6f, 0.8f);
            sp.displayClientMessage(Component.literal("L'autel s'éveille — étage " + floor + " !"), false);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
            double y = pos.getY() + 1.0D + random.nextDouble() * 0.6D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.8D;
            level.addParticle(ParticleTypes.ENCHANT,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.02D,
                    random.nextDouble() * 0.04D,
                    (random.nextDouble() - 0.5D) * 0.02D);
        }
    }

    private static EntityType<?> resolveType(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) return null; // pas de fallback Pig silencieux
        return BuiltInRegistries.ENTITY_TYPE.get(loc);
    }
}
