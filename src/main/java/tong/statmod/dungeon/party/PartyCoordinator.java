package tong.statmod.dungeon.party;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonTeleportHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cerveau du groupe d'aventuriers : à intervalle fixe, coordonne le ciblage des membres face au(x)
 * joueur(s) présent(s) et réattache leur IA (perdue au reload). C'est ce qui rend le groupe
 * <b>complémentaire</b> et intelligent contre un groupe : concentration de dégâts (focus-fire),
 * l'assassin isole une proie, le tank intercepte la menace qui vise le backline, le soigneur soigne.
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class PartyCoordinator {

    private static final int PERIOD = 10;
    /** Aligné sur DungeonMobSpawner.FLOOR_SCAN_RADIUS (package-private là-bas). */
    private static final double SCAN_RADIUS = 85.0;
    private static int tick;

    private PartyCoordinator() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tick % PERIOD != 0) return;

        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null || lv.players().isEmpty()) return;

        List<ServerPlayer> active = new ArrayList<>();
        Set<Integer> floors = new HashSet<>();
        for (ServerPlayer p : lv.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            int f = DungeonTeleportHandler.floorAtPos(p.getBlockX(), p.getBlockZ());
            if (f > 0) {
                active.add(p);
                floors.add(f);
            }
        }
        if (floors.isEmpty()) return;
        for (int floor : floors) {
            try {
                coordinateFloor(lv, floor, active);
            } catch (RuntimeException e) {
                StatMod.LOGGER.warn("[Party] coordination étage {} : {}", floor, e.getMessage());
            }
        }
    }

    private static void coordinateFloor(ServerLevel lv, int floor, List<ServerPlayer> active) {
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB box = new AABB(sp).inflate(SCAN_RADIUS);

        List<Mob> party = lv.getEntitiesOfClass(Mob.class, box,
                m -> m.isAlive() && m.getPersistentData().contains(PartyRole.TAG));
        if (party.isEmpty()) return;

        // Réattache l'IA à chaque cycle : les goals ne sont pas sérialisés, un reload les efface.
        for (Mob m : party) {
            AdventurerPartyHelper.ensureRoleAi(m);
        }

        List<ServerPlayer> foes = new ArrayList<>();
        for (ServerPlayer p : active) {
            if (DungeonTeleportHandler.floorAtPos(p.getBlockX(), p.getBlockZ()) == floor) {
                foes.add(p);
            }
        }
        if (foes.isEmpty()) return;

        // Centre du groupe et centre du backline (mage + soigneur) à protéger.
        double cx = 0, cz = 0;
        double bx = 0, bz = 0;
        int bn = 0;
        for (Mob m : party) {
            cx += m.getX();
            cz += m.getZ();
            String r = m.getPersistentData().getString(PartyRole.TAG);
            if ("MAGE".equals(r) || "HEALER".equals(r)) {
                bx += m.getX();
                bz += m.getZ();
                bn++;
            }
        }
        cx /= party.size();
        cz /= party.size();
        if (bn > 0) {
            bx /= bn;
            bz /= bn;
        } else {
            bx = cx;
            bz = cz;
        }

        int n = foes.size();
        double[][] pos = new double[n][2];
        double[] hp = new double[n];
        for (int i = 0; i < n; i++) {
            ServerPlayer p = foes.get(i);
            pos[i][0] = p.getX();
            pos[i][1] = p.getZ();
            hp[i] = (p.getHealth() + p.getAbsorptionAmount()) / Math.max(1.0f, p.getMaxHealth());
        }

        int focus = clamp(PartyTargeting.focusIndex(hp, pos, cx, cz), n);
        int isolated = clamp(PartyTargeting.isolatedIndex(pos), n);
        int backThreat = clamp(PartyTargeting.nearestToPoint(pos, bx, bz), n);

        LivingEntity focusP = foes.get(focus);
        LivingEntity isolatedP = foes.get(isolated);
        LivingEntity backP = foes.get(backThreat);

        // Mode EXECUTE : la cible focus est presque morte → tout le monde se rabat dessus pour
        // sécuriser le kill (au lieu d'étaler les cibles). Comportement d'équipe « finish ».
        boolean execute = hp[focus] < 0.30;

        for (Mob m : party) {
            String r = m.getPersistentData().getString(PartyRole.TAG);
            LivingEntity want;
            if (execute && !"HEALER".equals(r)) {
                want = focusP;
            } else {
                want = switch (r) {
                    case "TANK" -> backP;         // intercepte la menace qui vise le backline
                    case "ASSASSIN" -> isolatedP; // pique la proie isolée
                    case "MAGE" -> focusP;        // concentre le burst sur le focus
                    default -> null;              // HEALER : ne cible pas, il soigne (HealPartyGoal)
                };
            }
            if (want != null && m.getTarget() != want) {
                m.setTarget(want);
            }
        }
    }

    private static int clamp(int idx, int n) {
        if (idx < 0) return 0;
        if (idx >= n) return n - 1;
        return idx;
    }
}
