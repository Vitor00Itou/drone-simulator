package fr.enac.drone.model;

import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;

import java.util.*;

public class PathPlanner {
    private static final double CELL_SIZE = 10.0;
    private final int gridWidth, gridHeight;
    private boolean[][] blocked;
    private final double worldHalfSize;
    private final WorldConfiguration config;

    // Classe interne classique (pas de record, compatible Java 11)
    private static class Node {
        final int i, j;
        Node(int i, int j) { this.i = i; this.j = j; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node other = (Node) o;
            return i == other.i && j == other.j;
        }

        @Override
        public int hashCode() { return 31 * i + j; }
    }

    public PathPlanner(WorldConfiguration config) {
        this.config = config;
        double worldSize = computeWorldSize(config);
        this.gridWidth = (int) Math.ceil(worldSize / CELL_SIZE);
        this.gridHeight = gridWidth;
        this.worldHalfSize = worldSize / 2.0;
        this.blocked = new boolean[gridWidth][gridHeight];
        buildBlockedMap();
    }

    private double computeWorldSize(WorldConfiguration cfg) {
        double max = 5000.0;
        for (WorldObject obj : cfg.getObjects()) {
            if ("plane".equals(obj.getType()))
                max = Math.max(max, Math.max(obj.getSizeX(), obj.getSizeZ()));
            double hx = Math.abs(obj.getPosX()) + obj.getSizeX() / 2.0;
            double hz = Math.abs(obj.getPosZ()) + obj.getSizeZ() / 2.0;
            max = Math.max(max, 2.0 * Math.max(hx, hz));
        }
        return max;
    }

    private void buildBlockedMap() {
        for (int i = 0; i < gridWidth; i++) Arrays.fill(blocked[i], false);

        for (WorldObject obj : config.getObjects()) {
            if ("plane".equals(obj.getType())) continue;

            double halfX = obj.getSizeX() / 2.0;
            double halfZ = "cylinder".equals(obj.getType()) ? obj.getSizeX() / 2.0 : obj.getSizeZ() / 2.0;
            double minX = obj.getPosX() - halfX;
            double maxX = obj.getPosX() + halfX;
            double minZ = obj.getPosZ() - halfZ;
            double maxZ = obj.getPosZ() + halfZ;

            int colMin = worldToGridI(minX);
            int colMax = worldToGridI(maxX);
            int rowMin = worldToGridJ(minZ);
            int rowMax = worldToGridJ(maxZ);

            colMin = clamp(colMin, 0, gridWidth - 1);
            colMax = clamp(colMax, 0, gridWidth - 1);
            rowMin = clamp(rowMin, 0, gridHeight - 1);
            rowMax = clamp(rowMax, 0, gridHeight - 1);

            for (int i = colMin; i <= colMax; i++)
                for (int j = rowMin; j <= rowMax; j++)
                    blocked[i][j] = true;
        }
    }

    private int worldToGridI(double worldX) { return (int) ((worldX + worldHalfSize) / CELL_SIZE); }
    private int worldToGridJ(double worldZ) { return (int) ((worldZ + worldHalfSize) / CELL_SIZE); }

    private double gridToWorldX(int i) { return (i + 0.5) * CELL_SIZE - worldHalfSize; }
    private double gridToWorldZ(int j) { return (j + 0.5) * CELL_SIZE - worldHalfSize; }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    public List<double[]> findPath(double startX, double startZ, double targetX, double targetZ) {
        buildBlockedMap();

        int si = clamp(worldToGridI(startX), 0, gridWidth - 1);
        int sj = clamp(worldToGridJ(startZ), 0, gridHeight - 1);
        int ti = clamp(worldToGridI(targetX), 0, gridWidth - 1);
        int tj = clamp(worldToGridJ(targetZ), 0, gridHeight - 1);

        if (blocked[si][sj] || blocked[ti][tj]) return Collections.emptyList();

        Node start = new Node(si, sj);
        Node target = new Node(ti, tj);

        Map<Node, Double> gScores = new HashMap<>();
        Map<Node, Double> fScores = new HashMap<>();   // déclaré avant la queue
        Map<Node, Node> cameFrom = new HashMap<>();

        PriorityQueue<Node> open = new PriorityQueue<>(
            Comparator.comparingDouble(n -> fScores.getOrDefault(n, Double.MAX_VALUE))
        );

        gScores.put(start, 0.0);
        fScores.put(start, heuristic(start, target));
        open.add(start);

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.equals(target)) {
                LinkedList<Node> pathNodes = new LinkedList<>();
                Node p = current;
                while (p != null) {
                    pathNodes.addFirst(p);
                    p = cameFrom.get(p);
                }
                List<double[]> waypoints = new ArrayList<>();
                for (Node n : pathNodes)
                    waypoints.add(new double[]{gridToWorldX(n.i), gridToWorldZ(n.j)});
                return waypoints;
            }

            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (di == 0 && dj == 0) continue;
                    int ni = current.i + di;
                    int nj = current.j + dj;
                    if (ni < 0 || ni >= gridWidth || nj < 0 || nj >= gridHeight) continue;
                    if (blocked[ni][nj]) continue;

                    Node neighbor = new Node(ni, nj);
                    double newG = gScores.get(current) + Math.hypot(di, dj);
                    if (newG < gScores.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        cameFrom.put(neighbor, current);
                        gScores.put(neighbor, newG);
                        fScores.put(neighbor, newG + heuristic(neighbor, target));
                        if (!open.contains(neighbor)) open.add(neighbor);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private double heuristic(Node a, Node b) {
        return Math.hypot(a.i - b.i, a.j - b.j);
    }
}