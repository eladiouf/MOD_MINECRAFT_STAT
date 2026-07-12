package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.l2hostility.L2HostilityBridge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Mission M6 — Spawn des mobs de donjon, déclenché à l'entrée du joueur.
 *
 * <p><b>Problème résolu (invisibilité)</b> : si les mobs spawnent pendant la <i>génération</i> de
 * l'île (qui peut arriver depuis l'overworld via {@code /statdungeon regen}, ou avant le
 * {@code teleportTo}), ils naissent dans une dimension qu'aucun joueur ne suit → le client ne
 * reçoit jamais leur paquet d'apparition → <b>mobs présents mais invisibles</b>.
 *
 * <p><b>Fix</b> : la génération ne pose plus que des blocs. Les mobs sont demandés via
 * {@link #requestWave(ServerLevel, int)} <i>depuis {@code enterFloor}, après le teleport</i>,
 * donc quand le joueur est réellement dans le donjon et suit les chunks. Le spawn effectif est
 * encore différé de quelques ticks pour laisser le tracking se stabiliser.
 *
 * <p>Garde anti-doublon : {@link #requestWave} ne fait rien si des mobs autorisés sont encore
 * vivants sur l'étage, ou si une vague est déjà en attente pour cet étage.
 */
public final class DungeonMobSpawner {

    /** Délai avant spawn effectif : le joueur est déjà présent, un court délai suffit. */
    private static final int SPAWN_DELAY_TICKS = 10;
    /** Rayon (blocs) où l'on cherche des mobs déjà vivants sur l'étage. */
    static final int FLOOR_SCAN_RADIUS = 85;

    /**
     * Délai avant d'appliquer le niveau L2 Hostility. Doit passer APRÈS l'init de L2 (qui calcule
     * une difficulté régionale par distance) pour que notre valeur par étage soit autoritaire —
     * sinon L2 écrase notre niveau (ex : étage 10 à 1800 blocs → L2 forcerait ~27, non monotone).
     */
    private static final int L2_APPLY_DELAY_TICKS = 12;

    private record Pending(ServerLevel level, BlockPos pos, EntityType<?> type, String originalId, int floor, long dueTick) {}

    private record L2Pending(LivingEntity mob, int floor, long dueTick) {}

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static final List<L2Pending> L2_QUEUE = new ArrayList<>();
    private static final Set<Integer> PENDING_FLOORS = new HashSet<>();
    private static long serverTick = 0L;

    private DungeonMobSpawner() {}

    /**
     * Programme l'application (différée) du niveau L2 Hostility sur {@code mob}, calé sur
     * {@code floor}. À appeler juste après le spawn d'un mob de donjon (vague ou boss). L'apply
     * réel a lieu {@value #L2_APPLY_DELAY_TICKS} ticks plus tard, après l'init de L2, pour être
     * la dernière écriture (autoritaire).
     */
    public static void scheduleL2(LivingEntity mob, int floor) {
        if (mob == null || !L2HostilityBridge.loaded()) return;
        L2_QUEUE.add(new L2Pending(mob, floor, serverTick + L2_APPLY_DELAY_TICKS));
    }

    /**
     * Demande la vague de mobs de l'étage {@code floor}. À appeler depuis {@code enterFloor}
     * après le teleport. No-op si des mobs sont déjà vivants ou en attente pour cet étage.
     */
    public static void requestWave(ServerLevel lv, int floor) {
        if (!isCombatFloor(floor)) return; // boss (×10) / trésor (×5) : pas de vague
        if (PENDING_FLOORS.contains(floor)) return;
        if (countAlive(lv, floor) > 0) return;

        int players = Math.max(1, DungeonTeleportHandler.playersOnFloor(lv, floor).size());
        int full = waveSizeForFloor(floor, players);
        int queued = enqueueWave(lv, floor, full);
        STATMod.LOGGER.info("[TrialDungeon] Vague étage {} demandée : {} mobs en file ({} joueur(s))",
                floor, queued, players);
    }

    /**
     * Taille de la vague de combat selon l'étage — donjon DENSE (2026-07-05, à la demande).
     * Minimum 30 mobs dès l'étage 1, jusqu'à 48 en profondeur, soit ~3-4 mobs par pièce sur les
     * ~11 pièces. En co-op, +50 % de mobs par joueur supplémentaire présent sur l'étage
     * (audit multi 2026-07-09), plafond global 72 pour garder le tick serveur sain.
     * (S'y ajoute le mini-boss de thème.)
     */
    static int waveSizeForFloor(int floor, int players) {
        int n = 30 + floor / 5;         // base 30, +1 mob tous les 5 étages
        int solo = Math.min(48, Math.max(30, n));
        double coopMult = 1.0 + 0.5 * Math.max(0, players - 1);
        return (int) Math.min(72, Math.round(solo * coopMult));
    }

    /** {@code true} si l'étage a une vague de combat (ni boss ni trésor). */
    private static boolean isCombatFloor(int floor) {
        return floor > 0 && floor % 10 != 0 && floor % 5 != 0;
    }

    /** Nombre de mobs autorisés (nos mobs) encore vivants sur l'étage. */
    private static int countAlive(ServerLevel lv, int floor) {
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(FLOOR_SCAN_RADIUS);
        return lv.getEntitiesOfClass(Mob.class, area,
                m -> m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                        && !m.getPersistentData().getBoolean(DungeonMerchant.MERCHANT_TAG)).size();
    }

    /**
     * Supprime TOUS les mobs autorisés de l'étage (vague + mini-boss + invocations) et purge
     * la file d'attente de cet étage. À appeler avant de faire réessayer un étage au joueur (mort)
     * pour éviter qu'il réapparaisse au milieu de la horde précédente → mort instantanée en boucle.
     *
     * <p><b>No-op sur un combat de boss suivi</b> ({@link DungeonBossTracker#isTracked}) : discard le
     * boss ici le tuerait sans jamais réactiver l'autel ({@link DungeonBossAltarBlock}), rendant
     * l'étage définitivement infranchissable après une seule mort du joueur (softlock observé,
     * 2026-07-09). Le combat continue plutôt tel quel — le joueur (ou ses coéquipiers en multi)
     * retrouve le boss vivant en retournant dans l'arène.
     */
    public static void clearFloorMobs(ServerLevel lv, int floor) {
        if (DungeonBossTracker.isTracked(floor)) {
            STATMod.LOGGER.info(
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
        DungeonBossTracker.clear(floor);
        if (removed > 0) {
            STATMod.LOGGER.info("[TrialDungeon] Étage {} purgé : {} mobs retirés (retry)", floor, removed);
        }
    }

    /**
     * Met en file jusqu'à {@code want} mobs de la vague de l'étage, placés dans l'anneau de combat
     * sur des positions valides. Marque l'étage comme « en attente ». Retourne le nombre
     * effectivement mis en file.
     */
    private static int enqueueWave(ServerLevel lv, int floor, int want) {
        if (want <= 0) return 0;
        List<EntityType<?>> pool = ModdedMobPool.getCombinedPool(floor); // vanilla+mods, ou pool thématique
        if (pool.isEmpty()) return 0;

        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        PENDING_FLOORS.add(floor);
        // Les mobs sont marqués AUTHORIZED_TAG par spawnAuthorized → ils passent le garde.

        // Get combat rooms specifically to map locations to room archetypes
        List<DungeonLayout.Room> combatRooms = new ArrayList<>();
        for (DungeonLayout.Room r : DungeonLayout.rooms()) {
            if (r.isFirst()) continue; // pas de mobs dans la pièce d'apparition
            if (r.index() == DungeonLayout.ROOM_COUNT / 2) continue; // havre de paix
            combatRooms.add(r);
        }
        if (combatRooms.isEmpty()) return 0;

        // Mini-boss du thème dans la DERNIÈRE pièce (celle de sortie), en gardien du téléporteur.
        DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
        EntityType<?> miniBoss = firstAvailable(theme.miniBoss());
        if (miniBoss != null) {
            DungeonLayout.Room lastRoom = DungeonLayout.rooms().get(DungeonLayout.ROOM_COUNT - 1);
            int lastY = DungeonRoomChain.roomYOffset(lastRoom.index(), floor);
            BlockPos bossPos = sp.offset(lastRoom.centerX(), lastY, lastRoom.centerZ() - 3);
            QUEUE.add(new Pending(lv, bossPos, miniBoss, null, floor, serverTick + SPAWN_DELAY_TICKS));
        }

        int spawned = 0;
        int attempts = 0;
        while (spawned < want && attempts < want * 6) { // Max 6 essais par mob
            attempts++;

            // Choisit une pièce de combat aléatoire
            DungeonLayout.Room room = combatRooms.get(lv.random.nextInt(combatRooms.size()));
            int yOffset = DungeonRoomChain.roomYOffset(room.index(), floor);
            BlockPos roomCenter = sp.offset(room.centerX(), yOffset, room.centerZ());

            // Dispersion augmentée car les pièces font 53x61 blocs (colossales)
            int dx = lv.random.nextInt(25) - 12;
            int dz = lv.random.nextInt(25) - 12;
            BlockPos pos = roomCenter.offset(dx, 0, dz);

            BlockPos targetPos = pos;
            // 35% de chance de spawner sur la mezzanine si une plateforme y est présente
            if (lv.random.nextFloat() < 0.35f && isValidSpawnPosition(lv, pos.above(5))) {
                targetPos = pos.above(5);
            } else {
                if (!isValidSpawnPosition(lv, pos)) continue;
            }

            // Dégage une colonne d'air 1×2 au point de spawn
            lv.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
            lv.setBlock(targetPos.above(), Blocks.AIR.defaultBlockState(), 3);

            // Choix du mob typé selon l'archétype de la pièce
            int archetype = Math.floorMod(floor * 13 + room.index() * 29, 6);

            // ══ MAGES DU DONJON : spawn direct (fix 2026-07-12) ══
            // L'ancienne « conversion 20 % des zombies/squelettes » était du code mort avec le
            // modpack complet : chooseMobForArchetype retourne toujours un mob moddé (nécromancien,
            // bonescaller…), jamais un zombie/squelette vanilla brut → aucun mage ne spawnait.
            // Désormais : étage 3+, 15 % par slot (30 % dans les Archives), Mage du Wither étage 15+.
            // L'escorte (chevalier + archer) plus bas s'applique → ils arrivent TOUJOURS en groupe.
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

            // Conversion de secours (si un zombie/squelette brut sort quand même du pool)
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

            QUEUE.add(new Pending(lv, targetPos, type, originalId, floor, serverTick + SPAWN_DELAY_TICKS));
            spawned++;

            // IA DE GROUPE RPG : Si un mage (Clerc, Cryo, Électro, Wither, Pyromancien) spawn,
            // on ajoute automatiquement son escouade (1 Chevalier tank + 1 Archer à distance)
            boolean isMage = "statmod:cleric_mob".equals(originalId)
                          || "statmod:pyromancer_mob".equals(originalId)
                          || "statmod:cryomancer_mob".equals(originalId)
                          || "statmod:electromancer_mob".equals(originalId)
                          || "statmod:wither_mage_mob".equals(originalId);

            if (isMage && spawned < want) {
                // 1. LE CHEVALIER (Tank)
                BlockPos pos1 = targetPos.offset(2, 0, 2);
                if (isValidSpawnPosition(lv, pos1)) {
                    if (net.neoforged.fml.ModList.get().isLoaded("slu")) {
                        QUEUE.add(new Pending(lv, pos1, ModdedMobPool.resolve("slu:knight"), null, floor, serverTick + SPAWN_DELAY_TICKS));
                    } else {
                        QUEUE.add(new Pending(lv, pos1, EntityType.SKELETON, "statmod:dungeon_knight_fallback", floor, serverTick + SPAWN_DELAY_TICKS));
                    }
                    spawned++;
                }

                // 2. L'ARCHER (Dégâts physiques distance)
                if (spawned < want) {
                    BlockPos pos2 = targetPos.offset(-2, 0, -2);
                    if (isValidSpawnPosition(lv, pos2)) {
                        QUEUE.add(new Pending(lv, pos2, EntityType.SKELETON, null, floor, serverTick + SPAWN_DELAY_TICKS));
                        spawned++;
                    }
                }
            }
        }
        return spawned;
    }

    private static EntityType<?> chooseMobForArchetype(ServerLevel lv, int archetype, int floor) {
        // List of candidate custom mobs per archetype, falling back to standard vanilla choices if mods are absent
        List<String> candidates;
        EntityType<?> fallback;

        switch (archetype) {
            case 0 -> { // Archives / Library (Magic/Evokers)
                candidates = List.of("irons_spellbooks:necromancer", "irons_spellbooks:cryomancer", "irons_spellbooks:pyromancer", "minecraft:evoker");
                fallback = EntityType.WITCH;
            }
            case 1 -> { // Great Forge (Fire/Industrial)
                candidates = List.of("born_in_chaos_v1:firelight", "born_in_chaos_v1:withered_corpse", "minecraft:blaze");
                fallback = EntityType.HUSK;
            }
            case 2 -> { // Crypt (Undead/Skeletons)
                candidates = List.of("born_in_chaos_v1:bonescaller", "born_in_chaos_v1:decaying_zombie", "minecraft:wither_skeleton");
                fallback = EntityType.SKELETON;
            }
            case 3 -> { // Prison (Stray/Stalker)
                candidates = List.of("born_in_chaos_v1:nightmare_stalker", "born_in_chaos_v1:dark_vortex", "minecraft:stray");
                fallback = EntityType.ZOMBIE_VILLAGER;
            }
            case 4 -> { // Greenhouse (Spiders/Beasts)
                candidates = List.of("alexsmobs:tarantula_hawk", "alexsmobs:centipede_head", "minecraft:cave_spider");
                fallback = EntityType.SPIDER;
            }
            default -> { // Treasury (Guards)
                candidates = List.of("born_in_chaos_v1:lord_of_depths", "minecraft:piglin_brute", "minecraft:vindicator");
                fallback = EntityType.PILLAGER;
            }
        }

        for (String id : candidates) {
            EntityType<?> t = ModdedMobPool.resolve(id);
            if (t != null) return t;
        }
        return fallback;
    }

    /** Premier EntityType présent parmi une liste d'IDs candidats, ou {@code null}. */
    private static EntityType<?> firstAvailable(List<String> ids) {
        for (String id : ids) {
            EntityType<?> t = ModdedMobPool.resolve(id);
            if (t != null) return t;
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
                    if (entity instanceof Mob mob && p.originalId != null) {
                        mob.getPersistentData().putString("statmod_custom_mage_type", p.originalId);
                    }
                }
            } catch (RuntimeException e) {
                STATMod.LOGGER.warn("[TrialDungeon] Spawn différé fail {} @ {}: {}",
                        p.type, p.pos, e.getMessage());
            }
        }

        // Retire des PENDING_FLOORS les étages dont la file est vidée.
        if (spawned > 0) {
            Set<Integer> stillQueued = new HashSet<>();
            for (Pending p : QUEUE) stillQueued.add(p.floor);
            PENDING_FLOORS.retainAll(stillQueued);
            STATMod.LOGGER.info("[TrialDungeon] {} mobs spawnés (joueur présent, visibles)", spawned);
        }
    }

    /** Vérifie si une position est valide pour le spawn d'un mob. */
    private static boolean isValidSpawnPosition(ServerLevel lv, BlockPos pos) {
        // Check que le sol est solide (pas l'air, pas de blocs non-walkables)
        if (lv.getBlockState(pos.below()).isAir()) return false;
        if (!lv.getBlockState(pos.below()).isSolidRender(lv, pos.below())) return false;

        // Check qu'il y a de l'espace pour le mob (2 blocs de hauteur)
        if (!lv.getBlockState(pos).isAir()) return false;
        if (!lv.getBlockState(pos.above()).isAir()) return false;

        // Check qu'il n'y a pas d'autre mob trop proche (éviter stacking)
        AABB checkArea = new AABB(pos).inflate(2.0);
        if (!lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, checkArea).isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Applique les niveaux L2 échus (après l'init de L2 → autoritaire).
     *
     * <p><b>Anti-CME</b> : on extrait d'abord les entrées dues dans une liste locale, PUIS on
     * applique. {@code applyFloorLevel} peut faire naître des entités (parties de boss, invocations)
     * → {@code EntityJoinLevelEvent} → {@code DungeonSpawnGuard.scheduleL2} → {@code L2_QUEUE.add}.
     * Si on appliquait pendant l'itération de {@code L2_QUEUE}, cet ajout ré-entrant provoquerait un
     * {@link java.util.ConcurrentModificationException} (crash observé au spawn du boss étage 10).
     */
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

        // Application HORS itération : un scheduleL2 ré-entrant s'ajoute sans risque à L2_QUEUE.
        for (L2Pending p : due) {
            if (p.mob.isAlive()) {
                L2HostilityBridge.applyFloorLevel(p.mob, p.floor);
            }
        }
    }
}
