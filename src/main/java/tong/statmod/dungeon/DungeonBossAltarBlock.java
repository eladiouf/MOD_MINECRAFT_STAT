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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.dungeon.ai.DungeonAiActor;
import tong.statmod.dungeon.ai.DungeonTacticalGoals;
import tong.statmod.dungeon.ai.DungeonTacticalRolePolicy;
import tong.statmod.dungeon.party.AdventurerPartyHelper;
import tong.statmod.dungeon.party.DungeonAdventurerRolePolicy;
import tong.statmod.sound.ModSounds;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DungeonBossAltarBlock extends Block {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public DungeonBossAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, true));
    }

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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        int floor = DungeonTeleportHandler.floorAtPos(pos.getX(), pos.getZ());
        ServerLevel sl = (ServerLevel) level;

        AtomicLong cooldownTick = new AtomicLong(0);
        sp.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(data -> {
            cooldownTick.set(data.getBossCooldown(floor));
        });

        boolean cooldownActive = cooldownTick.get() > 0 && sl.getGameTime() < cooldownTick.get();

        if (!state.getValue(ACTIVE)) {
            if (cooldownActive) {
                sp.displayClientMessage(Component.literal("Cet autel a déjà été utilisé."), true);
                return InteractionResult.CONSUME;
            }
            level.setBlock(pos, state.setValue(ACTIVE, true), 3);
        } else if (cooldownActive) {
            long remaining = (cooldownTick.get() - sl.getGameTime()) / 20;
            String msg = "Autel en recharge : " + (remaining / 60) + "m " + (remaining % 60) + "s";
            sp.displayClientMessage(Component.literal(msg), true);
            return InteractionResult.CONSUME;
        }

        List<DungeonBossRoster.BossEntry> roster = DungeonBossRoster.forFloor(floor);

        BlockPos spawnCenter = DungeonTeleportHandler.floorSpawnPos(floor);
        int cleared = 0;
        for (Mob m : sl.getEntitiesOfClass(Mob.class,
                new AABB(spawnCenter).inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS),
                m -> m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                        && !DungeonBossTracker.isTrackedBoss(floor, m.getUUID()))) {
            m.discard();
            cleared++;
        }

        int spawned = 0;
        int maxSpawn = Math.min(3, roster.isEmpty() ? 1 : roster.stream()
                .mapToInt(e -> (int) e.entityId().chars().filter(c -> c == ',').count() + 1).sum());
        int slot = 0;
        for (DungeonBossRoster.BossEntry entry : roster) {
            for (String rawId : entry.entityId().split(",")) {
                if (spawned >= maxSpawn) break;
                EntityType<?> type = resolveType(rawId.trim());
                if (type == null) continue;
                BlockPos spawnPos = pos.offset((slot % 2 == 0 ? -1 : 1) * (slot + 1), 2, 3 + slot * 3);
                slot++;
                for (int dy = 0; dy < 3; dy++) {
                    sl.setBlock(spawnPos.above(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
                var spawnedEntity = DungeonSpawnGuard.spawnAuthorized(
                        () -> type.spawn(sl, spawnPos, MobSpawnType.STRUCTURE));
                if (spawnedEntity != null) {
                    if (spawnedEntity instanceof tong.statmod.entity.AdventurerEntity adventurer) {
                        AdventurerPartyHelper.configureRole(adventurer,
                                DungeonAdventurerRolePolicy.roleFor(floor, 0, spawned, true), floor);
                    }
                    spawned++;
                    spawnedEntity.getPersistentData().putString(
                            DungeonMobScaling.ROLE_TAG, DungeonMobScaling.MobRole.BOSS.id());
                    if (spawnedEntity instanceof Mob bossMob) {
                        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(bossMob.getType()).toString();
                        var faction = DungeonAiActor.factionFor(entityId);
                        var tacticalRole = DungeonTacticalRolePolicy.roleFor(
                                faction, floor, 0, spawned, true);
                        DungeonAiActor.initialize(bossMob, faction,
                                floor, floor + ":boss", tacticalRole);
                        DungeonTacticalGoals.ensureAttached(bossMob);
                    }
                    DungeonBossTracker.register(floor, spawnedEntity.getUUID());
                }
            }
        }

        if (spawned > 0) {
            level.setBlock(pos, state.setValue(ACTIVE, false), 3);
            sp.playNotifySound(ModSounds.DUNGEON_PORTAL_ENTER.get(), SoundSource.BLOCKS, 0.6f, 0.8f);
            String bossName = roster.isEmpty() ? "Boss" : firstBossName(roster.get(0).entityId());
            DungeonProgress.title(sp, Component.translatable("dungeon.title.boss", bossName),
                    Component.translatable("dungeon.title.boss.sub"));
            sp.playNotifySound(net.minecraft.sounds.SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.7f, 1.0f);
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
        if (loc != null && BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
            return BuiltInRegistries.ENTITY_TYPE.get(loc);
        }
        tong.statmod.StatMod.LOGGER.info("[TrialDungeon] Custom boss '{}' is not registered (missing mod). Using vanilla fallback.", id);
        if (id.contains("wither") || id.contains("lich") || id.contains("demon") || id.contains("watcher") || id.contains("lord") || id.contains("gundyr")) {
            return BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft:wither"));
        } else if (id.contains("dragon") || id.contains("harbinger") || id.contains("yeti") || id.contains("smough") || id.contains("radahn") || id.contains("beast") || id.contains("titan")) {
            return BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft:warden"));
        } else {
            return BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft:elder_guardian"));
        }
    }

    private static String firstBossName(String entry) {
        String id = entry.contains(",") ? entry.substring(0, entry.indexOf(',')) : entry;
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (path.startsWith("boss_")) path = path.substring(5);
        String[] words = path.replace('_', ' ').trim().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.length() == 0 ? "Boss" : sb.toString();
    }
}
