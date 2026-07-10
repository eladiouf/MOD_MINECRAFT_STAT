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
import tong.statmod.STATMod;
import tong.statmod.storage.ModAttachments;
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

        int floor = DungeonTeleportHandler.floorAtPos(pos.getX(), pos.getZ());
        ServerLevel sl = (ServerLevel) level;

        // ══ LOGIQUE DE RE-SUMMON (cooldown 2h) ══
        // Si l'autel est inactif mais que le cooldown est expiré → le réactive pour re-summon.
        // Si l'autel est inactif et le cooldown court encore → rejet.
        // Si l'autel est actif et le cooldown court → rejet (cas improbable : tick système
        //   avant le set ACTIVE=false, mais protège du double-click).
        long cooldownTick = sp.getData(ModAttachments.STATS).getBossCooldown(floor);
        boolean cooldownActive = cooldownTick > 0 && sl.getGameTime() < cooldownTick;

        if (!state.getValue(ACTIVE)) {
            if (cooldownActive) {
                sp.displayClientMessage(Component.literal("Cet autel a déjà été utilisé."), true);
                return InteractionResult.CONSUME;
            }
            // Cooldown expiré → réactive l'autel pour permettre un nouveau combat.
            level.setBlock(pos, state.setValue(ACTIVE, true), 3);
            STATMod.LOGGER.info("[TrialDungeon] Autel étage {} réactivé (cooldown expiré)", floor);
        } else if (cooldownActive) {
            long remaining = (cooldownTick - sl.getGameTime()) / 20;
            String msg = "Autel en recharge : " + (remaining / 60) + "m " + (remaining % 60) + "s";
            sp.displayClientMessage(Component.literal(msg), true);
            return InteractionResult.CONSUME;
        }

        List<DungeonBossRoster.BossEntry> roster = DungeonBossRoster.forFloor(floor);

        // ══ NETTOYAGE DÉFENSIF de l'étage avant summon ══
        // On supprime tous les mobs autorisés résiduels (morts vivants d'une session précédente,
        // adds de boss d'un ancien combat, mini-boss de thème égaré) pour éviter l'accumulation.
        // On garde les boss suivis par DungeonBossTracker (s'ils existent encore).
        // (Fix « trop de mobs spawn avec le boss » 2026-07-10.)
        BlockPos spawnCenter = DungeonTeleportHandler.floorSpawnPos(floor);
        int cleared = 0;
        for (Mob m : sl.getEntitiesOfClass(Mob.class,
                new AABB(spawnCenter).inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS),
                m -> m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                        && !DungeonBossTracker.isTrackedBoss(floor, m.getUUID()))) {
            m.discard();
            cleared++;
        }
        if (cleared > 0) {
            STATMod.LOGGER.info("[TrialDungeon] Autel étage {} : {} mobs résiduels nettoyés avant summon",
                    floor, cleared);
        }

        // ══ SPAWN DU BOSS ══
        // Une BossEntry peut contenir PLUSIEURS ids séparés par des virgules (mode duo/vague) :
        // "slu:boss_ornstein,slu:boss_smough". On les éclate tous ici — sinon resolveType échouait
        // sur la chaîne à virgules et RIEN ne spawnait (bug « aucun boss à partir de l'étage 40 »,
        // les étages ×10 ≥ 40 étant presque tous des duos/vagues).
        // Plafond de sécurité : max 3 boss par summon (évite le crash si le roster est corrompu).
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
            // Titre dramatique à l'apparition du boss (nom du roster) — moment fort du donjon.
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
        if (loc == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) return null; // pas de fallback Pig silencieux
        return BuiltInRegistries.ENTITY_TYPE.get(loc);
    }

    /** Nom lisible à partir d'un id d'entrée de roster (« slu:boss_malenia,... » → « Malenia »). */
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
