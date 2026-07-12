package tong.statmod.dungeon.city;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * File de jobs de construction de la cité, drainée à budget fixe par tick serveur.
 *
 * <p>PURE (aucun import Minecraft) : testable en JUnit. Un job = une unité de travail bornée
 * (une bande de terrain, un monument). Un job qui lève est loggé puis sauté — la construction
 * ne doit jamais se bloquer sur un segment défectueux.
 */
public final class CityBuildQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(CityBuildQueue.class);

    /** Unité de travail bornée (quelques milliers de blocs max). */
    @FunctionalInterface
    public interface Job {
        void run();
    }

    private final List<Job> jobs = new ArrayList<>();
    private int cursor = 0;

    public void add(Job job) { jobs.add(job); }

    public boolean isDone() { return cursor >= jobs.size(); }

    public int totalJobs() { return jobs.size(); }

    public int completedJobs() { return cursor; }

    public int progressPercent() {
        return jobs.isEmpty() ? 100 : (int) (100L * cursor / jobs.size());
    }

    /**
     * Exécute jusqu'à {@code budget} jobs (dans l'ordre d'ajout).
     *
     * @return le nombre de jobs réellement exécutés.
     */
    public int tick(int budget) {
        int ran = 0;
        while (ran < budget && cursor < jobs.size()) {
            Job job = jobs.get(cursor++);
            try {
                job.run();
            } catch (RuntimeException e) {
                LOGGER.error("[City] Job de construction {} en échec — segment sauté", cursor - 1, e);
            }
            ran++;
        }
        return ran;
    }

    public void clear() {
        jobs.clear();
        cursor = 0;
    }
}
