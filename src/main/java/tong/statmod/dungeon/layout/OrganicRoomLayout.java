package tong.statmod.dungeon.layout;

import tong.statmod.dungeon.noise.OpenSimplex2S;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phase 2 — Layout de pièces organique piloté par {@link OpenSimplex2S}.
 *
 * <p>Remplace la grille fixe 5×4 de {@link tong.statmod.dungeon.DungeonLayout} par un placement
 * de pièces généré par bruit 2D. Chaque étage a une disposition unique, avec un nombre variable
 * de pièces (10-26), garanties sans chevauchement et connectées.
 *
 * <p>Algorithme : grille initiale de cellules (5×4 = 20) → agglomération des cellules adjacentes
 * dont les valeurs de bruit sont les plus proches, jusqu'à atteindre 10-16 pièces.
 * Résultat : rectangle-partition (pas de chevauchement), connectivité naturelle par grille.
 */
public final class OrganicRoomLayout implements RoomProvider {

    private static final int BASE_COLS = 5;
    private static final int BASE_ROWS = 4;
    private static final int MIN_ROOMS = 10;
    private static final int MAX_ROOMS = 18;

    private final List<Room> rooms;
    private final Room spawn;
    private final Room exit;

    private OrganicRoomLayout(List<Room> rooms, Room spawn, Room exit) {
        this.rooms = rooms;
        this.spawn = spawn;
        this.exit = exit;
    }

    /** Crée le layout organique pour un étage (déterministe : même floor → même layout). */
    public static OrganicRoomLayout forFloor(int floor, int hx, int hz) {
        long seed = seedFor(floor);
        int cols = BASE_COLS;
        int rows = BASE_ROWS;
        int cellW = (2 * hx) / cols;
        int cellD = (2 * hz) / rows;

        // Générer les valeurs de bruit pour chaque cellule de la grille 5×4
        double[][] noise = new double[cols][rows];
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                double wx = (c - (cols - 1) / 2.0) * cellW;
                double wz = (r - (rows - 1) / 2.0) * cellD;
                noise[c][r] = OpenSimplex2S.noise2(seed, wx * 0.015, wz * 0.015);
            }
        }

        // Chaque cellule = une pièce initiale
        List<MutableRoom> mutable = new ArrayList<>(cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                MutableRoom mr = new MutableRoom();
                mr.minX = c * cellW - hx;
                mr.maxX = (c + 1) * cellW - hx - 1;
                mr.minZ = r * cellD - hz;
                mr.maxZ = (r + 1) * cellD - hz - 1;
                mr.noiseValue = noise[c][r];
                mr.neighborIndices = new ArrayList<>();
                mutable.add(mr);
            }
        }

        // Compute geometric adjacency once before starting to merge
        recomputeAdjacency(mutable);

        // Fusionner les paires les plus similaires jusqu'à ce qu'on atteigne la cible
        // ou qu'il ne reste plus que MIN_ROOMS
        int target = 10 + Math.abs((int) ((seed >> 16) & 0x0F));
        if (target < MIN_ROOMS) target = MIN_ROOMS;
        if (target > mutable.size()) target = mutable.size();

        while (mutable.size() > target && mutable.size() > MIN_ROOMS) {
            int bestA = -1, bestB = -1;
            double bestDiff = Double.MAX_VALUE;

            for (int i = 0; i < mutable.size(); i++) {
                MutableRoom a = mutable.get(i);
                for (int nb : a.neighborIndices) {
                    if (nb > i) {
                        double diff = Math.abs(a.noiseValue - mutable.get(nb).noiseValue);
                        if (diff < bestDiff) { bestDiff = diff; bestA = i; bestB = nb; }
                    }
                }
            }

            if (bestA == -1 || bestB == -1) break;

            MutableRoom a = mutable.get(bestA);
            MutableRoom b = mutable.get(bestB);
            int mergedMinX = Math.min(a.minX, b.minX);
            int mergedMaxX = Math.max(a.maxX, b.maxX);
            int mergedMinZ = Math.min(a.minZ, b.minZ);
            int mergedMaxZ = Math.max(a.maxZ, b.maxZ);

            // Rejeter la fusion si la bounding box unifiée chevauche une autre pièce
            boolean overlaps = false;
            for (int k = 0; k < mutable.size(); k++) {
                if (k == bestA || k == bestB) continue;
                MutableRoom other = mutable.get(k);
                if (mergedMinX < other.maxX && mergedMaxX > other.minX
                        && mergedMinZ < other.maxZ && mergedMaxZ > other.minZ) {
                    overlaps = true;
                    break;
                }
            }

            if (overlaps) {
                // Marquer ces deux pièces comme non-fusionnables en les supprimant de leurs listes de voisins
                a.neighborIndices.remove((Integer) bestB);
                b.neighborIndices.remove((Integer) bestA);
                continue;
            }

            a.minX = mergedMinX;
            a.maxX = mergedMaxX;
            a.minZ = mergedMinZ;
            a.maxZ = mergedMaxZ;
            a.noiseValue = (a.noiseValue + b.noiseValue) / 2.0;

            // bestA < bestB always since we only check nb > i
            mutable.remove(bestB);
            recomputeAdjacency(mutable);
        }

        // Si encore trop, forcer des fusions jusqu'à MAX_ROOMS
        while (mutable.size() > MAX_ROOMS) {
            int bestA = -1, bestB = -1;
            double bestDiff = Double.MAX_VALUE;
            for (int i = 0; i < mutable.size(); i++) {
                MutableRoom a = mutable.get(i);
                for (int nb : a.neighborIndices) {
                    if (nb > i) {
                        double diff = Math.abs(a.noiseValue - mutable.get(nb).noiseValue);
                        if (diff < bestDiff) { bestDiff = diff; bestA = i; bestB = nb; }
                    }
                }
            }
            if (bestA == -1) break;
            MutableRoom a = mutable.get(bestA);
            MutableRoom b = mutable.get(bestB);
            int mergedMinX = Math.min(a.minX, b.minX);
            int mergedMaxX = Math.max(a.maxX, b.maxX);
            int mergedMinZ = Math.min(a.minZ, b.minZ);
            int mergedMaxZ = Math.max(a.maxZ, b.maxZ);

            boolean overlaps = false;
            for (int k = 0; k < mutable.size(); k++) {
                if (k == bestA || k == bestB) continue;
                MutableRoom other = mutable.get(k);
                if (mergedMinX < other.maxX && mergedMaxX > other.minX
                        && mergedMinZ < other.maxZ && mergedMaxZ > other.minZ) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) {
                a.neighborIndices.remove((Integer) bestB);
                b.neighborIndices.remove((Integer) bestA);
                continue;
            }

            a.minX = mergedMinX;
            a.maxX = mergedMaxX;
            a.minZ = mergedMinZ;
            a.maxZ = mergedMaxZ;
            a.noiseValue = (a.noiseValue + b.noiseValue) / 2.0;
            mutable.remove(bestB);
            recomputeAdjacency(mutable);
        }

        // Convertir en Room immutables avec adjacence finale
        recomputeAdjacency(mutable);
        List<Room> roomList = new ArrayList<>(mutable.size());
        for (int i = 0; i < mutable.size(); i++) {
            MutableRoom mr = mutable.get(i);
            Room room = new Room(i, mr.minX, mr.maxX, mr.minZ, mr.maxZ);
            room.connectedTo = mr.neighborIndices.stream().filter(n -> n >= 0 && n < mutable.size()).mapToInt(Integer::intValue).toArray();
            roomList.add(room);
        }

        // Salle de spawn = la plus proche du centre de la forteresse
        Room spawn = roomList.stream().min(Comparator.comparingDouble(
                r -> Math.sqrt((double) r.centerX() * r.centerX() + (double) r.centerZ() * r.centerZ())))
                .orElse(roomList.get(0));

        // Salle de sortie = la plus éloignée (par Dijkstra)
        int spawnIdx = roomList.indexOf(spawn);
        Room exit = roomList.get(farthestReachable(roomList, spawnIdx));

        spawn.markFirst();
        exit.markLast();

        return new OrganicRoomLayout(roomList, spawn, exit);
    }

    /**
     * Recalcule l'adjacence géométrique entre toutes les pièces mutables.
     * Deux pièces sont adjacentes si leurs rectangles partagent une bordure contiguë
     * (un côté commun d'au moins 1 bloc).
     */
    private static List<MutableRoom> recomputeAdjacency(List<MutableRoom> rooms) {
        for (int i = 0; i < rooms.size(); i++) {
            MutableRoom a = rooms.get(i);
            a.neighborIndices.clear();
            for (int j = 0; j < rooms.size(); j++) {
                if (i == j) continue;
                MutableRoom b = rooms.get(j);
                // Adjacent en X : Z se chevauchent, X se touchent
                boolean xAdj = (a.maxX + 1 == b.minX || b.maxX + 1 == a.minX)
                        && a.minZ < b.maxZ && a.maxZ > b.minZ;
                // Adjacent en Z : X se chevauchent, Z se touchent
                boolean zAdj = (a.maxZ + 1 == b.minZ || b.maxZ + 1 == a.minZ)
                        && a.minX < b.maxX && a.maxX > b.minX;
                if (xAdj || zAdj) a.neighborIndices.add(j);
            }
        }
        return rooms;
    }

    private static long seedFor(int floor) {
        long z = floor * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static int farthestReachable(List<Room> rooms, int start) {
        int n = rooms.size();
        double[] dist = new double[n];
        boolean[] visited = new boolean[n];
        for (int i = 0; i < n; i++) dist[i] = Double.MAX_VALUE;
        dist[start] = 0;
        for (int iter = 0; iter < n; iter++) {
            int u = -1;
            double best = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!visited[i] && dist[i] < best) { best = dist[i]; u = i; }
            }
            if (u == -1) break;
            visited[u] = true;
            Room ru = rooms.get(u);
            for (int v : ru.connectedTo) {
                if (!visited[v]) {
                    Room rv = rooms.get(v);
                    double w = Math.sqrt(Math.pow(ru.centerX() - rv.centerX(), 2)
                            + Math.pow(ru.centerZ() - rv.centerZ(), 2));
                    if (dist[u] + w < dist[v]) dist[v] = dist[u] + w;
                }
            }
        }
        int farthest = start;
        for (int i = 0; i < n; i++) {
            if (dist[i] > dist[farthest] && dist[i] < Double.MAX_VALUE) farthest = i;
        }
        return farthest;
    }

    @Override public List<? extends RoomLike> rooms() { return rooms; }
    @Override public RoomLike spawnRoom() { return spawn; }
    @Override public RoomLike exitRoom() { return exit; }
    @Override public int roomCount() { return rooms.size(); }

    /** Données mutables pendant l'agglomération. */
    private static final class MutableRoom {
        int minX, maxX, minZ, maxZ;
        double noiseValue;
        List<Integer> neighborIndices;
    }

    public static final class Room implements RoomLike {
        private final int index;
        private final int minX, maxX, minZ, maxZ;
        private boolean first, last;
        private int[] connectedTo;

        private Room(int index, int minX, int maxX, int minZ, int maxZ) {
            this.index = index;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.connectedTo = new int[0];
        }

        void markFirst() { this.first = true; }
        void markLast() { this.last = true; }

        @Override public int index() { return index; }
        @Override public int minX() { return minX; }
        @Override public int maxX() { return maxX; }
        @Override public int minZ() { return minZ; }
        @Override public int maxZ() { return maxZ; }
        @Override public int centerX() { return (minX + maxX) / 2; }
        @Override public int centerZ() { return (minZ + maxZ) / 2; }
        @Override public boolean isFirst() { return first; }
        @Override public boolean isLast() { return last; }
        @Override public int[] connectedTo() { return connectedTo; }
    }
}
