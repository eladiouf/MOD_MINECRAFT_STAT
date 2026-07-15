package tong.statmod.dungeon;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import tong.statmod.StatMod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mission M6 — Barre de vie des boss du donjon (2026-07-05).
 *
 * <p>Beaucoup de boss moddés (SLU notamment) n'affichent aucune barre de vie → le joueur ne voit
 * pas sa progression pendant le combat. On crée une {@link ServerBossEvent} par boss suivi
 * ({@link DungeonBossTracker}), mise à jour depuis ses PV, visible par les joueurs proches. Retirée
 * quand le boss meurt/disparaît. Rafraîchie toutes les {@value #UPDATE_TICKS} ticks (léger).
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonBossBar {

    /** Intervalle de rafraîchissement (ticks) — la barre n'a pas besoin d'être au tick près. */
    private static final int UPDATE_TICKS = 5;
    /** Rayon (blocs) où un joueur voit la barre d'un boss. */
    private static final double WATCH_RADIUS = 80.0;

    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static int tick = 0;

    private DungeonBossBar() {}

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (++tick % UPDATE_TICKS != 0) return;
        MinecraftServer server = event.getServer();
        ServerLevel lv = server.getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null) return;

        Set<UUID> tracked = DungeonBossTracker.allTracked();

        // Retire les barres des boss qui ne sont plus suivis (morts, purgés).
        Iterator<Map.Entry<UUID, ServerBossEvent>> it = BARS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ServerBossEvent> e = it.next();
            if (!tracked.contains(e.getKey())) {
                e.getValue().removeAllPlayers();
                it.remove();
            }
        }

        // Crée/rafraîchit une barre par boss suivi vivant.
        for (UUID id : tracked) {
            Entity entity = lv.getEntity(id);
            if (!(entity instanceof LivingEntity boss) || !boss.isAlive()) {
                ServerBossEvent gone = BARS.remove(id);
                if (gone != null) gone.removeAllPlayers();
                continue;
            }
            ServerBossEvent bar = BARS.computeIfAbsent(id, k ->
                    new ServerBossEvent(boss.getDisplayName(),
                            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10));
            bar.setName(boss.getDisplayName());
            float max = boss.getMaxHealth();
            bar.setProgress(max > 0 ? Math.max(0f, Math.min(1f, boss.getHealth() / max)) : 0f);

            // Spectateurs : joueurs proches du boss (on remet la liste à jour à chaque passe).
            bar.removeAllPlayers();
            for (ServerPlayer p : lv.players()) {
                if (p.distanceToSqr(boss) <= WATCH_RADIUS * WATCH_RADIUS) bar.addPlayer(p);
            }
        }
    }
}
