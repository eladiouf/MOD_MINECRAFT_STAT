package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.dungeon.party.AdventurerPartyHelper;
import tong.statmod.dungeon.party.DungeonAdventurerRolePolicy;
import tong.statmod.dungeon.party.PartyRole;
import tong.statmod.integration.l2hostility.L2HostilityBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonMobSpawner {

    private static final int SPAWN_DELAY_TICKS = 10;
    static final int FLOOR_SCAN_RADIUS = 85;
    private static final int L2_APPLY_DELAY_TICKS = 12;

    private record Pending(ServerLevel level, BlockPos pos, EntityType<?> type, String originalId,
                           int floor, long dueTick, DungeonMobScaling.MobRole role, int roomIndex) {}

    private record L2Pending(LivingEntity mob, int floor, long dueTick) {}

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static final List<L2Pending> L2_QUEUE = new ArrayList<>();
    private static final Set<Integer> PENDING_FLOORS = new HashSet<>();
    private static long serverTick = 0L;

    private static final Map<Integer, List<Integer>> FLOOR_COMBAT_ROOMS = new HashMap<>();
    private static final Map<Integer, Integer> FLOOR_ACTIVE_ROOM_IDX = new HashMap<>();
    private static final Map<Integer, Integer> FLOOR_MOBS_PER_ROOM = new HashMap<>();
    private static final Set<Integer> FLOOR_PARTY_SPAWNED = new HashSet<>();

    private DungeonMobSpawner() {}

    public static void scheduleL2(LivingEntity mob, int floor) {
        if (mob == null) return;
        L2_QUEUE.add(new L2Pending(mob, floor, serverTick + L2_APPLY_DELAY_TICKS));
    }

    /**
     * Demande la vague de mobs de l'étage {@code floor}.
     * Ne spawn QUE la première pièce de combat ; les suivantes sont débloquées
     * pièce par pièce au fur et à mesure que le joueur nettoie.
     */
    public static void requestWave(ServerLevel lv, int floor) {
        if (!isCombatFloor(floor)) return;
        if (PENDING_FLOORS.contains(floor)) return;
        if (countAlive(lv, floor) > 0) return;

        int players = Math.max(1, DungeonTeleportHandler.playersOnFloor(lv, floor).size());
        int want = waveSizeForFloor(floor, players);

        // Construit la liste ordonnée des pièces de combat (exclut spawn + safehouse)
        List<Integer> combatRooms = new ArrayList<>();
        for (DungeonLayout.Room r : DungeonLayout.rooms()) {
            if (r.isFirst()) continue;
            if (r.index() == DungeonLayout.ROOM_COUNT / 2) continue;
            combatRooms.add(r.index());
        }
        if (combatRooms.isEmpty()) return;

        int roomCount = combatRooms.size();
        int perRoom = Math.max(1, want / roomCount);

        FLOOR_COMBAT_ROOMS.put(floor, combatRooms);
        FLOOR_ACTIVE_ROOM_IDX.put(floor, 0);
        FLOOR_MOBS_PER_ROOM.put(floor, perRoom);
        FLOOR_PARTY_SPAWNED.remove(floor);

        // Spawn les mobs de la PREMIÈRE pièce uniquement
        int firstRoom = combatRooms.get(0);
        int firstCount = perRoom + (want % roomCount); // le reste va dans la 1ʳᵉ pièce
        int queued = enqueueWave(lv, floor, firstCount, firstRoom);
        if (queued > 0) {
            PENDING_FLOORS.add(floor);
            StatMod.LOGGER.info("[TrialDungeon] Vague initiale étage {} : {} mobs en pièce {} ({} joueur(s))",
                    floor, queued, firstRoom, players);
        }
    }

    public static boolean requestRoomWave(ServerLevel lv, int floor, int roomIndex) {
        return false;
    }

    static int waveSizeForFloor(int floor, int players) {
        int n = 60 + floor / 2;
        int solo = Math.min(100, Math.max(60, n));
        double coopMult = 1.0 + 0.5 * Math.max(0, players - 1);
        return (int) Math.min(150, Math.round(solo * coopMult));
    }

    private static boolean isCombatFloor(int floor) {
        return floor > 0 && floor % 10 != 0 && floor % 5 != 0;
    }

    /** Nombre de mobs autorisés encore vivants sur TOUT l'étage. */
    private static int countAlive(ServerLevel lv, int floor) {
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(FLOOR_SCAN_RADIUS);
        return lv.getEntitiesOfClass(Mob.class, area,
                m -> m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                        && !m.getPersistentData().getBoolean(DungeonMerchant.MERCHANT_TAG)).size();
    }

    /** Compte les mobs autorisés vivants dans la pièce {@code roomIndex} uniquement. */
    public static int countAliveInRoom(ServerLevel lv, int floor, int roomIndex, LivingEntity exclude) {
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(FLOOR_SCAN_RADIUS);
        return lv.getEntitiesOfClass(Mob.class, area,
                m -> m != exclude && m.isAlive()
                        && m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                        && m.getPersistentData().getInt("statmod_dungeon_room") == roomIndex).size();
    }

    /** Pièce de combat actuellement active pour l'étage. */
    public static int getActiveRoom(int floor) {
        List<Integer> rooms = FLOOR_COMBAT_ROOMS.get(floor);
        if (rooms == null) return -1;
        int idx = FLOOR_ACTIVE_ROOM_IDX.getOrDefault(floor, 0);
        if (idx < 0 || idx >= rooms.size()) return -1;
        return rooms.get(idx);
    }

    /** True si la pièce active est la DERNIÈRE pièce de combat de l'étage. */
    public static boolean isLastCombatRoom(int floor) {
        List<Integer> rooms = FLOOR_COMBAT_ROOMS.get(floor);
        if (rooms == null) return false;
        int idx = FLOOR_ACTIVE_ROOM_IDX.getOrDefault(floor, 0);
        return idx >= rooms.size() - 1;
    }

    /**
     * Passe à la pièce suivante et spawn ses mobs.
     * Si le spawn échoue (0 mobs), avance silencieusement à la suivante.
     */
    public static void advanceToNextRoom(ServerLevel lv, int floor) {
        List<Integer> rooms = FLOOR_COMBAT_ROOMS.get(floor);
        if (rooms == null) return;

        int maxIdx = rooms.size();
        for (int attempt = 0; attempt < maxIdx; attempt++) {
            int idx = FLOOR_ACTIVE_ROOM_IDX.getOrDefault(floor, 0) + 1;
            if (idx >= maxIdx) return;

            FLOOR_ACTIVE_ROOM_IDX.put(floor, idx);
            int roomIndex = rooms.get(idx);
            int count = FLOOR_MOBS_PER_ROOM.getOrDefault(floor, 1);

            int queued = enqueueWave(lv, floor, count, roomIndex);
            if (queued > 0) {
                StatMod.LOGGER.info("[TrialDungeon] Étage {} -> pièce {} : {} mobs", floor, roomIndex, queued);
                return;
            }
            // 0 mobs → passer à la pièce suivante silencieusement
        }
    }

    public static void clearFloorMobs(ServerLevel lv, int floor) {
        if (DungeonBossTracker.isTracked(floor)) {
            StatMod.LOGGER.info(
                    "[TrialDungeon] Étage {} : combat de boss suivi, purge annulée (le combat continue)",
                    floor);
            return;
        }
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(FLOOR_SCAN_RADIUS);
        int removed = 0;
        for (Mob m : lv.getEntitiesOfClass(Mob.class, area,
                m -> m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                        && !m.getPersistentData().getBoolean(DungeonMerchant.MERCHANT_TAG))) {
            m.discard();
            removed++;
        }
        QUEUE.removeIf(p -> p.floor() == floor);
        PENDING_FLOORS.remove(floor);
        FLOOR_COMBAT_ROOMS.remove(floor);
        FLOOR_ACTIVE_ROOM_IDX.remove(floor);
        FLOOR_MOBS_PER_ROOM.remove(floor);
        FLOOR_PARTY_SPAWNED.remove(floor);
        DungeonRoomEncounterDirector.resetActiveRoom(floor);
        DungeonBossTracker.clear(floor);
        if (removed > 0) {
            StatMod.LOGGER.info("[TrialDungeon] Étage {} purgé : {} mobs retirés (retry)", floor, removed);
        }
    }

    private static int enqueueWave(ServerLevel lv, int floor, int want, int roomIndex) {
        if (want <= 0) return 0;
        List<EntityType<?>> pool = ModdedMobPool.getCombinedPool(floor);
        if (pool.isEmpty()) return 0;

        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);

        List<DungeonLayout.Room> combatRooms = new ArrayList<>();
        for (DungeonLayout.Room r : DungeonLayout.rooms()) {
            if (r.isFirst()) continue;
            if (r.index() == DungeonLayout.ROOM_COUNT / 2) continue;
            if (roomIndex != -1 && r.index() != roomIndex) continue;
            combatRooms.add(r);
        }
        if (combatRooms.isEmpty()) {
            PENDING_FLOORS.remove(floor);
            return 0;
        }

        // Mini-boss du thème dans la DERNIÈRE pièce (celle de sortie)
        DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
        EntityType<?> miniBoss = firstAvailable(theme.miniBoss());
        if (miniBoss != null && roomIndex == DungeonLayout.ROOM_COUNT - 1) {
            DungeonLayout.Room lastRoom = DungeonLayout.rooms().get(DungeonLayout.ROOM_COUNT - 1);
            int lastY = DungeonRoomChain.roomYOffset(lastRoom.index(), floor);
            BlockPos bossPos = sp.offset(lastRoom.centerX(), lastY, lastRoom.centerZ() - 3);
            QUEUE.add(new Pending(lv, bossPos, miniBoss, null, floor,
                    serverTick + SPAWN_DELAY_TICKS, DungeonMobScaling.MobRole.ELITE, roomIndex));
        }

        int spawned = 0;
        int attempts = 0;
        while (spawned < want && attempts < want * 6) {
            attempts++;

            DungeonLayout.Room room = combatRooms.get(lv.random.nextInt(combatRooms.size()));
            int yOffset = DungeonRoomChain.roomYOffset(room.index(), floor);
            BlockPos roomCenter = sp.offset(room.centerX(), yOffset, room.centerZ());

            int dx = lv.random.nextInt(25) - 12;
            int dz = lv.random.nextInt(25) - 12;
            BlockPos pos = roomCenter.offset(dx, 0, dz);

            BlockPos targetPos = pos;
            if (lv.random.nextFloat() < 0.35f && isValidSpawnPosition(lv, pos.above(5))) {
                targetPos = pos.above(5);
            } else {
                if (!isValidSpawnPosition(lv, pos)) continue;
            }

            lv.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
            lv.setBlock(targetPos.above(), Blocks.AIR.defaultBlockState(), 3);

            int archetype = Math.floorMod(floor * 13 + room.index() * 29, 6);

            EntityType<?> type = null;
            String originalId = null;
            float mageChance = floor >= 3 ? (archetype == 0 ? 0.30f : 0.15f) : 0.0f;
            if (lv.random.nextFloat() < mageChance) {
                if (floor >= 15 && lv.random.nextFloat() < 0.25f) {
                    type = EntityType.WITHER_SKELETON;
                    originalId = "statmod:wither_mage_mob";
                } else if (lv.random.nextBoolean()) {
                    type = EntityType.ZOMBIE;
                    originalId = lv.random.nextFloat() < 0.25f ? "statmod:cleric_mob" : "statmod:pyromancer_mob";
                } else {
                    type = EntityType.SKELETON;
                    originalId = lv.random.nextBoolean() ? "statmod:cryomancer_mob" : "statmod:electromancer_mob";
                }
            }
            if (type == null) {
                type = chooseMobForArchetype(lv, archetype, floor);
            }
            if (type == null) {
                type = pool.get(lv.random.nextInt(pool.size()));
            }

            if (originalId == null && type == EntityType.ZOMBIE) {
                if (lv.random.nextFloat() < 0.20f) {
                    originalId = lv.random.nextFloat() < 0.25f ? "statmod:cleric_mob" : "statmod:pyromancer_mob";
                }
            } else if (originalId == null && type == EntityType.SKELETON) {
                if (lv.random.nextFloat() < 0.20f) {
                    originalId = lv.random.nextBoolean() ? "statmod:cryomancer_mob" : "statmod:electromancer_mob";
                }
            } else if (originalId == null && type == EntityType.WITHER_SKELETON) {
                if (lv.random.nextFloat() < 0.20f) {
                    originalId = "statmod:wither_mage_mob";
                }
            } else if (originalId == null) {
                String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                if (entityId.startsWith("slu:") && lv.random.nextFloat() < 0.20f) {
                    if (entityId.equals("slu:hollow") || entityId.equals("slu:armed_hollow") || entityId.equals("slu:thief") || entityId.equals("slu:hollow_soldier_sword") || entityId.equals("slu:hollow_soldier_spear")) {
                        originalId = "statmod:hollow_witch_mob";
                    } else if (entityId.equals("slu:knight") || entityId.equals("slu:elite_knight") || entityId.equals("slu:dungeon_knight") || entityId.equals("slu:castle_guard") || entityId.equals("slu:noble_knight")) {
                        originalId = "statmod:mage_knight_mob";
                    } else if (entityId.equals("slu:dark_knight") || entityId.equals("slu:wither_skeleton_knight") || entityId.equals("slu:ringed_knight") || entityId.equals("slu:mad_knight")) {
                        originalId = "statmod:void_knight_mob";
                    }
                }
            }

            QUEUE.add(new Pending(lv, targetPos, type, originalId, floor,
                    serverTick + SPAWN_DELAY_TICKS, DungeonMobScaling.MobRole.NORMAL, roomIndex));
            spawned++;

            boolean isMage = "statmod:cleric_mob".equals(originalId)
                          || "statmod:pyromancer_mob".equals(originalId)
                          || "statmod:cryomancer_mob".equals(originalId)
                          || "statmod:electromancer_mob".equals(originalId)
                          || "statmod:wither_mage_mob".equals(originalId);

            if (isMage && spawned < want) {
                BlockPos pos1 = targetPos.offset(2, 0, 2);
                if (isValidSpawnPosition(lv, pos1)) {
                    if (net.minecraftforge.fml.ModList.get().isLoaded("slu")) {
                        QUEUE.add(new Pending(lv, pos1, ModdedMobPool.resolve("slu:knight"), null, floor,
                                serverTick + SPAWN_DELAY_TICKS, DungeonMobScaling.MobRole.NORMAL, roomIndex));
                    } else {
                        QUEUE.add(new Pending(lv, pos1, EntityType.SKELETON, "statmod:dungeon_knight_fallback", floor,
                                serverTick + SPAWN_DELAY_TICKS, DungeonMobScaling.MobRole.NORMAL, roomIndex));
                    }
                    spawned++;
                }

                if (spawned < want) {
                    BlockPos pos2 = targetPos.offset(-2, 0, -2);
                    if (isValidSpawnPosition(lv, pos2)) {
                        QUEUE.add(new Pending(lv, pos2, EntityType.SKELETON, null, floor,
                                serverTick + SPAWN_DELAY_TICKS, DungeonMobScaling.MobRole.NORMAL, roomIndex));
                        spawned++;
                    }
                }
            }
        }

        // Groupe d'aventuriers : une seule fois par étage, dans sa pièce dédiée
        if (AdventurerPartyHelper.isPartyFloor(floor) && !FLOOR_PARTY_SPAWNED.contains(floor)) {
            FLOOR_PARTY_SPAWNED.add(floor);
            AdventurerPartyHelper.spawnParty(lv, sp, floor);
        }

        return spawned;
    }

    private static EntityType<?> chooseMobForArchetype(ServerLevel lv, int archetype, int floor) {
        List<String> candidates;
        EntityType<?> fallback;

        switch (archetype) {
            case 0 -> {
                candidates = List.of("irons_spellbooks:necromancer", "irons_spellbooks:cryomancer", "irons_spellbooks:pyromancer", "minecraft:evoker");
                fallback = EntityType.WITCH;
            }
            case 1 -> {
                candidates = List.of("minecraft:blaze", "minecraft:zombie", "irons_spellbooks:pyromancer", "minecraft:blaze", "minecraft:magma_cube");
                fallback = EntityType.HUSK;
            }
            case 2 -> {
                candidates = List.of("minecraft:skeleton", "minecraft:zombie", "irons_spellbooks:necromancer", "slu:hollow_soldier_spear", "minecraft:wither_skeleton");
                fallback = EntityType.SKELETON;
            }
            case 3 -> {
                candidates = List.of("statmod:adventurer", "irons_spellbooks:necromancer",
                        "epic_mobs:lost_wanderer", "minecraft:stray");
                fallback = EntityType.ZOMBIE_VILLAGER;
            }
            case 4 -> {
                candidates = List.of("alexsmobs:tarantula_hawk", "alexsmobs:centipede_head", "minecraft:silverfish", "minecraft:cave_spider", "minecraft:spider");
                fallback = EntityType.SPIDER;
            }
            default -> {
                candidates = List.of("statmod:adventurer", "irons_spellbooks:magehunter_vindicator",
                        "epic_mobs:nameless_knight", "minecraft:piglin_brute", "minecraft:vindicator");
                fallback = EntityType.PILLAGER;
            }
        }

        for (String id : candidates) {
            EntityType<?> t = ModdedMobPool.resolve(id);
            if (t != null) return t;
        }
        return fallback;
    }

    private static EntityType<?> firstAvailable(List<String> ids) {
        for (String id : ids) {
            EntityType<?> t = ModdedMobPool.resolve(id);
            if (t != null) return t;
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        serverTick++;
        flushL2Queue();

        if (QUEUE.isEmpty()) return;

        int spawned = 0;
        Iterator<Pending> it = QUEUE.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (serverTick < p.dueTick) continue;
            it.remove();
            try {
                var entity = DungeonSpawnGuard.spawnAuthorized(
                        () -> p.type.spawn(p.level, p.pos, MobSpawnType.STRUCTURE));
                if (entity != null) {
                    spawned++;
                    entity.getPersistentData().putString(DungeonMobScaling.ROLE_TAG, p.role().id());
                    if (p.roomIndex() >= 0) {
                        entity.getPersistentData().putInt("statmod_dungeon_room", p.roomIndex());
                    }
                    if (entity instanceof tong.statmod.entity.AdventurerEntity adventurer) {
                        long positionKey = p.pos().asLong();
                        int ordinal = Math.floorMod((int) (positionKey ^ (positionKey >>> 32)),
                                PartyRole.values().length);
                        PartyRole role = DungeonAdventurerRolePolicy.roleFor(
                                p.floor(), p.roomIndex(), ordinal,
                                p.role() == DungeonMobScaling.MobRole.BOSS);
                        AdventurerPartyHelper.configureRole(adventurer, role, p.floor());
                    }
                    if (entity instanceof Mob mob && p.originalId != null) {
                        mob.getPersistentData().putString("statmod_custom_mage_type", p.originalId);
                    }
                }
            } catch (RuntimeException e) {
                StatMod.LOGGER.warn("[TrialDungeon] Spawn différé fail {} @ {}: {}",
                        p.type, p.pos, e.getMessage());
            }
        }

        if (spawned > 0) {
            Set<Integer> stillQueued = new HashSet<>();
            for (Pending p : QUEUE) stillQueued.add(p.floor);
            PENDING_FLOORS.retainAll(stillQueued);
            StatMod.LOGGER.info("[TrialDungeon] {} mobs spawnés (joueur présent, visibles)", spawned);
        }
    }

    private static boolean isValidSpawnPosition(ServerLevel lv, BlockPos pos) {
        if (lv.getBlockState(pos.below()).isAir()) return false;
        if (!lv.getBlockState(pos.below()).isSolidRender(lv, pos.below())) return false;
        if (!lv.getBlockState(pos).isAir()) return false;
        if (!lv.getBlockState(pos.above()).isAir()) return false;
        AABB checkArea = new AABB(pos).inflate(2.0);
        if (!lv.getEntitiesOfClass(Mob.class, checkArea).isEmpty()) {
            return false;
        }
        return true;
    }

    private static void flushL2Queue() {
        if (L2_QUEUE.isEmpty()) return;

        List<L2Pending> due = new ArrayList<>();
        Iterator<L2Pending> it = L2_QUEUE.iterator();
        while (it.hasNext()) {
            L2Pending p = it.next();
            if (serverTick < p.dueTick) continue;
            it.remove();
            due.add(p);
        }

        for (L2Pending p : due) {
            if (p.mob.isAlive()) {
                if (L2HostilityBridge.loaded()) {
                    L2HostilityBridge.applyFloorLevel(p.mob, p.floor);
                }
                DungeonMobScaling.applyFloorScaling(p.mob, p.floor);
            }
        }
    }
}
