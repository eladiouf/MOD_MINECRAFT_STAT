package tong.statmod.integration.ironspells;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Preuve d'impact serveur par cast (Tâche 3 — magic security hardening).
 *
 * <p>Remplace l'heuristique provisoire « coût de mana ≥ 25 % » : la progression
 * bancable (maîtrise + Magic Points) n'est accordée que si CE cast précis a produit
 * des dégâts de sort positifs côté serveur ({@code SpellDamageEvent}). Un cast validé
 * sans preuve ne touche que la récompense d'entraînement (Tâche 2).
 *
 * <p>Cycle : {@link #begin} enregistre un cast accepté (après validation sort appris +
 * mana) ; {@link #markImpact} marque le cast en attente le plus récent du même sort
 * quand des dégâts positifs sont observés ; {@link #consume} retire le plus ancien
 * cast en attente du sort et dit s'il était marqué. L'état est borné par joueur/sort
 * ({@link #MAX_PENDING_PER_SPELL}) et expire en ticks de jeu ({@link #PENDING_TTL_TICKS}).
 *
 * <p>Accès depuis le thread serveur uniquement (events NeoForge + {@code server.execute}) —
 * pas de synchronisation nécessaire. Cœur pur, testé sans mocks Minecraft.
 */
public final class CastImpactTracker {

    /** Durée de vie d'un cast en attente : 200 ticks (10 s) — couvre projectiles lents et sorts canalisés. */
    public static final long PENDING_TTL_TICKS = 200L;

    /** Nombre maximal de casts en attente par joueur et par sort (anti-spam / fuite mémoire). */
    public static final int MAX_PENDING_PER_SPELL = 4;

    private static final class PendingCast {
        final long tick;
        boolean marked;

        PendingCast(long tick) {
            this.tick = tick;
        }
    }

    private final Map<UUID, Map<String, ArrayDeque<PendingCast>>> pending = new HashMap<>();

    /** Enregistre un cast accepté (à appeler seulement après validation sort appris + mana). */
    public void begin(UUID playerId, String spellId, long gameTick) {
        if (playerId == null || spellId == null) return;
        ArrayDeque<PendingCast> queue = pending
                .computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(spellId, k -> new ArrayDeque<>());
        queue.addLast(new PendingCast(gameTick));
        while (queue.size() > MAX_PENDING_PER_SPELL) {
            queue.removeFirst();
        }
    }

    /**
     * Marque le cast en attente LE PLUS RÉCENT non encore marqué de ce sort — les dégâts
     * observés suivent toujours le dernier cast émis. À appeler uniquement sur des dégâts
     * de sort strictement positifs.
     */
    public void markImpact(UUID playerId, String spellId) {
        ArrayDeque<PendingCast> queue = queueOf(playerId, spellId);
        if (queue == null) return;
        var it = queue.descendingIterator();
        while (it.hasNext()) {
            PendingCast cast = it.next();
            if (!cast.marked) {
                cast.marked = true;
                return;
            }
        }
    }

    /**
     * Retire le plus ancien cast en attente de ce sort et indique s'il portait une preuve
     * d'impact. Les entrées expirées ({@code gameTick - tick > TTL}) sont purgées et ne
     * fournissent jamais de preuve.
     */
    public boolean consume(UUID playerId, String spellId, long gameTick) {
        ArrayDeque<PendingCast> queue = queueOf(playerId, spellId);
        if (queue == null) return false;
        while (!queue.isEmpty() && gameTick - queue.peekFirst().tick > PENDING_TTL_TICKS) {
            queue.removeFirst();
        }
        PendingCast cast = queue.pollFirst();
        cleanup(playerId, spellId, queue);
        return cast != null && cast.marked;
    }

    /** Purge tout l'état d'un joueur (logout, clone, respawn). */
    public void clear(UUID playerId) {
        if (playerId != null) pending.remove(playerId);
    }

    private ArrayDeque<PendingCast> queueOf(UUID playerId, String spellId) {
        if (playerId == null || spellId == null) return null;
        Map<String, ArrayDeque<PendingCast>> bySpell = pending.get(playerId);
        return bySpell == null ? null : bySpell.get(spellId);
    }

    private void cleanup(UUID playerId, String spellId, ArrayDeque<PendingCast> queue) {
        if (!queue.isEmpty()) return;
        Map<String, ArrayDeque<PendingCast>> bySpell = pending.get(playerId);
        if (bySpell != null) {
            bySpell.remove(spellId);
            if (bySpell.isEmpty()) pending.remove(playerId);
        }
    }
}
