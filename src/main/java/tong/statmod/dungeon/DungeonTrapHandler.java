package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Déclencheur des pièges à effet du donjon (100 % Java, aucun command block). Voir {@link DungeonTraps}.
 * STEP : joueur qui marche sur une tuile-piège. CHEST : ouverture d'un coffre piégé. Effets codés,
 * scalés par étage, touchant tous les joueurs à portée (co-op), avec cooldown par tuile.
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonTrapHandler {

    private static final int PERIOD = 4;
    private static final long COOLDOWN_TICKS = 80L; // 4 s par tuile
    private static final double STEP_RADIUS = 0.75;

    private static final Map<Long, Long> COOLDOWNS = new HashMap<>(); // packedPos -> tick de réarmement
    private static int tickAcc;

    private DungeonTrapHandler() {}

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickAcc % PERIOD != 0) return;

        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null || lv.players().isEmpty()) return;
        long now = lv.getGameTime();

        for (ServerPlayer p : lv.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            int floor = DungeonTeleportHandler.floorAtPos(p.getBlockX(), p.getBlockZ());
            if (floor <= 0) continue;
            List<DungeonTraps.Trap> traps = DungeonTraps.traps(floor);
            if (traps.isEmpty()) continue;

            for (DungeonTraps.Trap trap : traps) {
                if (trap.trigger() != DungeonTraps.Trigger.STEP) continue;
                BlockPos tp = trap.pos();
                if (Math.abs(p.getX() - (tp.getX() + 0.5)) > STEP_RADIUS) continue;
                if (Math.abs(p.getZ() - (tp.getZ() + 0.5)) > STEP_RADIUS) continue;
                if (Math.abs(p.getY() - tp.getY()) > 1.6) continue;
                if (onCooldown(tp, now)) continue;
                fire(lv, trap, floor, now);
            }
        }
    }

    @SubscribeEvent
    public static void onChestOpen(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getLevel() instanceof ServerLevel lv)) return;
        if (!lv.dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;
        int floor = DungeonTeleportHandler.floorAtPos(event.getPos().getX(), event.getPos().getZ());
        if (floor <= 0) return;
        long now = lv.getGameTime();
        for (DungeonTraps.Trap trap : DungeonTraps.traps(floor)) {
            if (trap.trigger() == DungeonTraps.Trigger.CHEST && trap.pos().equals(event.getPos())
                    && !onCooldown(trap.pos(), now)) {
                fire(lv, trap, floor, now); // le coffre s'ouvre quand même
                return;
            }
        }
    }

    private static boolean onCooldown(BlockPos pos, long now) {
        Long next = COOLDOWNS.get(pos.asLong());
        return next != null && now < next;
    }

    private static void fire(ServerLevel lv, DungeonTraps.Trap trap, int floor, long now) {
        COOLDOWNS.put(trap.pos().asLong(), now + COOLDOWN_TICKS);
        BlockPos p = trap.pos();
        double cx = p.getX() + 0.5, cy = p.getY(), cz = p.getZ() + 0.5;

        lv.playSound(null, cx, cy, cz, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.HOSTILE, 1.2f, 0.7f);

        switch (trap.kind()) {
            case FANG_BURST -> fangRing(lv, cx, cy, cz, floor);
            case DART_VOLLEY -> dartVolley(lv, cx, cy + 0.5, cz, floor);
            case POISON_GAS -> gas(lv, cx, cy, cz, 3.5, floor, ParticleTypes.SNEEZE,
                    new MobEffectInstance(MobEffects.POISON, 120 + floor, floor >= 30 ? 1 : 0));
            case SNARE -> snare(lv, p, cx, cy, cz, floor);
            case FLAME -> flame(lv, cx, cy, cz, floor);
            case AMBUSH -> ambush(lv, p, floor >= 20 ? EntityType.SILVERFISH : EntityType.CAVE_SPIDER, 3);
            case FROST -> gas(lv, cx, cy, cz, 3.0, floor, ParticleTypes.SNOWFLAKE,
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2),
                    new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1));
            case CONFUSION -> gas(lv, cx, cy, cz, 3.5, floor, ParticleTypes.WITCH,
                    new MobEffectInstance(MobEffects.CONFUSION, 200, 0),
                    new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            case WITHER_GAS -> gas(lv, cx, cy, cz, 3.5, floor, ParticleTypes.SMOKE,
                    new MobEffectInstance(MobEffects.WITHER, 100 + floor, 1));
            case HEX -> gas(lv, cx, cy, cz, 3.5, floor, ParticleTypes.WITCH,
                    new MobEffectInstance(MobEffects.WEAKNESS, 200, 1),
                    new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1),
                    new MobEffectInstance(MobEffects.POISON, 100, 1));
            case VEX_AMBUSH -> ambush(lv, p, EntityType.VEX, 2);
        }
    }

    // ── Effets ──────────────────────────────────────────────────────────────────────────────

    /** Anneau de crocs d'évocateur jaillissant autour de la tuile (télégraphié par le warmup). */
    private static void fangRing(ServerLevel lv, double cx, double cy, double cz, int floor) {
        int n = 8;
        for (int i = 0; i < n; i++) {
            double a = (Math.PI * 2 * i) / n;
            double fx = cx + Math.cos(a) * 2.0;
            double fz = cz + Math.sin(a) * 2.0;
            EvokerFangs fangs = new EvokerFangs(lv, fx, cy, fz, (float) a, i % 4, null);
            lv.addFreshEntity(fangs);
        }
    }

    /** Salve de fléchettes tirées d'un anneau vers la tuile. */
    private static void dartVolley(ServerLevel lv, double cx, double cy, double cz, int floor) {
        int n = 6;
        double dmg = 2.0 + floor * 0.12;
        for (int i = 0; i < n; i++) {
            double a = (Math.PI * 2 * i) / n;
            double sx = cx + Math.cos(a) * 4.0;
            double sz = cz + Math.sin(a) * 4.0;
            Arrow arrow = new Arrow(lv, sx, cy, sz);
            arrow.setBaseDamage(dmg);
            arrow.setNoGravity(true);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.shoot(cx - sx, 0.0, cz - sz, 1.6f, 1.0f);
            lv.addFreshEntity(arrow);
        }
    }

    /** Nuage d'effet : applique les effets (et particules) à tous les joueurs à portée. */
    private static void gas(ServerLevel lv, double cx, double cy, double cz, double radius, int floor,
                            net.minecraft.core.particles.ParticleOptions particle,
                            MobEffectInstance... effects) {
        lv.sendParticles(particle, cx, cy + 0.5, cz, 40, radius * 0.4, 0.6, radius * 0.4, 0.02);
        for (Player pl : playersInRange(lv, cx, cy, cz, radius)) {
            for (MobEffectInstance e : effects) {
                pl.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration(), e.getAmplifier()));
            }
        }
    }

    /** Filet : toiles autour de la tuile + entrave sur les joueurs à portée. */
    private static void snare(ServerLevel lv, BlockPos p, double cx, double cy, double cz, int floor) {
        int[][] cells = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] c : cells) {
            BlockPos wp = p.offset(c[0], 1, c[1]);
            if (lv.getBlockState(wp).canBeReplaced()) {
                lv.setBlock(wp, Blocks.COBWEB.defaultBlockState(), 3);
            }
        }
        for (Player pl : playersInRange(lv, cx, cy, cz, 2.5)) {
            pl.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            pl.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        }
    }

    /** Jet de flammes : embrase et blesse les joueurs à portée. */
    private static void flame(ServerLevel lv, double cx, double cy, double cz, int floor) {
        lv.sendParticles(ParticleTypes.FLAME, cx, cy + 0.5, cz, 60, 1.2, 0.5, 1.2, 0.05);
        float dmg = 3.0f + floor * 0.12f;
        for (Player pl : playersInRange(lv, cx, cy, cz, 2.5)) {
            pl.setSecondsOnFire(4 + floor / 25);
            pl.hurt(lv.damageSources().inFire(), dmg);
        }
    }

    /** Embuscade : quelques mobs autorisés (persistants, disciplinés) surgissent. */
    private static void ambush(ServerLevel lv, BlockPos p, EntityType<?> type, int amount) {
        for (int i = 0; i < amount; i++) {
            BlockPos sp = p.offset((i % 2 == 0 ? 1 : -1) * (1 + i / 2), 1, (i % 2 == 0 ? -1 : 1));
            DungeonSpawnGuard.spawnAuthorized(() -> type.spawn(lv, sp, net.minecraft.world.entity.MobSpawnType.TRIGGERED));
        }
        lv.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5,
                20, 0.8, 0.4, 0.8, 0.03);
    }

    private static List<Player> playersInRange(ServerLevel lv, double cx, double cy, double cz, double radius) {
        AABB box = new AABB(cx - radius, cy - 3, cz - radius, cx + radius, cy + 3, cz + radius);
        return lv.getEntitiesOfClass(Player.class, box,
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative()
                        && pl.distanceToSqr(new Vec3(cx, pl.getY(), cz)) <= radius * radius);
    }
}
