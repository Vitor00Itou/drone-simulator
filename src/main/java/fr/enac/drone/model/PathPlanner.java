package fr.enac.drone.model;

import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;

import java.util.*;

public class PathPlanner {
    private static final double CELL_SIZE = 4.0;
    private static final double DRONE_HEIGHT_MARGIN = 2.0;
    private static final double HARD_MARGIN = 5.0;  // Increased to absorb grid quantization error
    private static final double SOFT_MARGIN = 12.0; // Preferred clearance to prevent drift and scraping

    private final int gridWidth, gridHeight;
    private final double worldHalfSize;
    private final WorldConfiguration config;

    private static class Node {
        final int i, j;
        Node(int i, int j) { this.i = i; this.j = j; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return i == node.i && j == node.j;
        }
        @Override
        public int hashCode() { return 31 * i + j; }
    }

    private static class GridMap {
        boolean[][] hardBlocked;
        double[][] penalty;
    }

    public PathPlanner(WorldConfiguration config) {
        this.config = config;
        double worldSize = computeWorldSize(config);
        this.worldHalfSize = worldSize / 2.0;
        this.gridWidth = (int) Math.ceil(worldSize / CELL_SIZE);
        this.gridHeight = gridWidth;
    }

    private double computeWorldSize(WorldConfiguration cfg) {
        double max = 5000.0;
        for (WorldObject obj : cfg.getObjects()) {
            if ("plane".equals(obj.getType())) {
                max = Math.max(max, Math.max(obj.getSizeX(), obj.getSizeZ()));
            }
        }
        return max;
    }

    private GridMap buildGridMap(double currentY) {
        GridMap map = new GridMap();
        map.hardBlocked = new boolean[gridWidth][gridHeight];
        map.penalty = new double[gridWidth][gridHeight];

        for (WorldObject obj : config.getObjects()) {
            if ("plane".equals(obj.getType())) continue;

            double objTop = obj.getPosY() - obj.getSizeY() / 2.0;
            double objBottom = obj.getPosY() + obj.getSizeY() / 2.0;

            if (currentY < objTop - DRONE_HEIGHT_MARGIN || currentY > objBottom + DRONE_HEIGHT_MARGIN) {
                continue; // Obstacle is safely above or below the drone
            }

            boolean isCylinder = "cylinder".equals(obj.getType());
            double halfX = isCylinder ? obj.getSizeX() : obj.getSizeX() / 2.0;
            double halfZ = isCylinder ? obj.getSizeX() : obj.getSizeZ() / 2.0;

            // Calculate bounding box for the maximum influence area
            double maxInfluence = HARD_MARGIN + SOFT_MARGIN;
            int colMin = clamp(worldToGridI(obj.getPosX() - halfX - maxInfluence), 0, gridWidth - 1);
            int colMax = clamp(worldToGridI(obj.getPosX() + halfX + maxInfluence), 0, gridWidth - 1);
            int rowMin = clamp(worldToGridJ(obj.getPosZ() - halfZ - maxInfluence), 0, gridHeight - 1);
            int rowMax = clamp(worldToGridJ(obj.getPosZ() + halfZ + maxInfluence), 0, gridHeight - 1);

            for (int i = colMin; i <= colMax; i++) {
                for (int j = rowMin; j <= rowMax; j++) {
                    double cellX = gridToWorldX(i);
                    double cellZ = gridToWorldZ(j);
                    
                    double dx = Math.max(0, Math.abs(cellX - obj.getPosX()) - halfX);
                    double dz = Math.max(0, Math.abs(cellZ - obj.getPosZ()) - halfZ);
                    double dist = isCylinder ? 
                                  Math.hypot(cellX - obj.getPosX(), cellZ - obj.getPosZ()) - halfX : 
                                  Math.hypot(dx, dz);

                    if (dist <= HARD_MARGIN) {
                        map.hardBlocked[i][j] = true;
                    } else if (dist <= maxInfluence) {
                        double penaltyFactor = 1.0 - ((dist - HARD_MARGIN) / SOFT_MARGIN);
                        // Max penalty is 50, decreases to 0 at the edge of SOFT_MARGIN
                        map.penalty[i][j] = Math.max(map.penalty[i][j], penaltyFactor * 50.0); 
                    }
                }
            }
        }
        return map;
    }

    private int worldToGridI(double worldX) { return (int) ((worldX + worldHalfSize) / CELL_SIZE); }
    private int worldToGridJ(double worldZ) { return (int) ((worldZ + worldHalfSize) / CELL_SIZE); }

    private double gridToWorldX(int i) { return (i + 0.5) * CELL_SIZE - worldHalfSize; }
    private double gridToWorldZ(int j) { return (j + 0.5) * CELL_SIZE - worldHalfSize; }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private List<double[]> simplifyPath(List<Node> pathNodes, GridMap map) {
        if (pathNodes.size() <= 2) {
            return pathNodes.stream().map(n -> new double[]{gridToWorldX(n.i), gridToWorldZ(n.j)}).toList();
        }

        List<double[]> simplified = new ArrayList<>();
        simplified.add(new double[]{gridToWorldX(pathNodes.get(0).i), gridToWorldZ(pathNodes.get(0).j)});

        Node current = pathNodes.get(0);
        for (int i = 1; i < pathNodes.size() - 1; i++) {
            Node next = pathNodes.get(i + 1);
            if (hasLineOfSight(current, next, map)) {
                continue;
            } else {
                current = pathNodes.get(i);
                simplified.add(new double[]{gridToWorldX(current.i), gridToWorldZ(current.j)});
            }
        }
        
        Node last = pathNodes.get(pathNodes.size() - 1);
        simplified.add(new double[]{gridToWorldX(last.i), gridToWorldZ(last.j)});
        return simplified;
    }

    private boolean hasLineOfSight(Node a, Node b, GridMap map) {
        double x0 = a.i + 0.5;
        double y0 = a.j + 0.5;
        double x1 = b.i + 0.5;
        double y1 = b.j + 0.5;
        
        double dist = Math.hypot(x1 - x0, y1 - y0);
        int steps = (int) Math.ceil(dist * 3.0); 
        
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int cx = clamp((int) (x0 + t * (x1 - x0)), 0, gridWidth - 1);
            int cy = clamp((int) (y0 + t * (y1 - y0)), 0, gridHeight - 1);
            
            // Thick raycast (3x3 cells) to strictly prevent corner shaving on wide objects
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = clamp(cx + dx, 0, gridWidth - 1);
                    int ny = clamp(cy + dy, 0, gridHeight - 1);
                    if (map.hardBlocked[nx][ny]) return false;
                }
            }
        }
        return true;
    }

    private Node findNearestUnblocked(int startI, int startJ, GridMap map) {
        Queue<Node> queue = new LinkedList<>();
        Set<Node> visited = new HashSet<>();
        Node start = new Node(startI, startJ);
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            if (!map.hardBlocked[curr.i][curr.j]) {
                return curr;
            }
            if (Math.abs(curr.i - startI) > 20 || Math.abs(curr.j - startJ) > 20) {
                return null; // Give up if we search too far into an infinitely thick wall
            }
            
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    int ni = curr.i + di;
                    int nj = curr.j + dj;
                    if (ni >= 0 && ni < gridWidth && nj >= 0 && nj < gridHeight) {
                        Node n = new Node(ni, nj);
                        if (visited.add(n)) {
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return null;
    }

    public List<double[]> findPath(double startX, double startY, double startZ, double targetX, double targetZ) {
        GridMap map = buildGridMap(startY);

        int si = clamp(worldToGridI(startX), 0, gridWidth - 1);
        int sj = clamp(worldToGridJ(startZ), 0, gridHeight - 1);
        int ti = clamp(worldToGridI(targetX), 0, gridWidth - 1);
        int tj = clamp(worldToGridJ(targetZ), 0, gridHeight - 1);

        // Always allow drone to escape if it is currently inside a hard block
        map.hardBlocked[si][sj] = false; 
        
        if (map.hardBlocked[ti][tj]) {
            Node nearest = findNearestUnblocked(ti, tj, map);
            if (nearest == null) return Collections.emptyList();
            ti = nearest.i;
            tj = nearest.j;
        }

        Node start = new Node(si, sj);
        Node target = new Node(ti, tj);

        Map<Node, Double> gScores = new HashMap<>();
        Map<Node, Double> fScores = new HashMap<>(); 
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
                return simplifyPath(pathNodes, map);
            }

            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (di == 0 && dj == 0) continue;
                    int ni = current.i + di;
                    int nj = current.j + dj;
                    if (ni < 0 || ni >= gridWidth || nj < 0 || nj >= gridHeight) continue;
                    if (map.hardBlocked[ni][nj]) continue;

                    // Don't cut corners through blocks
                    if (di != 0 && dj != 0) {
                        if (map.hardBlocked[current.i + di][current.j] || map.hardBlocked[current.i][current.j + dj]) {
                            continue;
                        }
                    }

                    Node neighbor = new Node(ni, nj);
                    double baseCost = (di != 0 && dj != 0) ? 1.414 : 1.0;
                    // Combine grid distance cost with obstacle proximity penalty
                    double moveCost = baseCost + (map.penalty[ni][nj] * baseCost);
                    double newG = gScores.get(current) + moveCost;

                    if (newG < gScores.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        cameFrom.put(neighbor, current);
                        gScores.put(neighbor, newG);
                        fScores.put(neighbor, newG + heuristic(neighbor, target));
                        if (!open.contains(neighbor)) {
                            open.add(neighbor);
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private double heuristic(Node a, Node b) {
        int dx = Math.abs(a.i - b.i);
        int dy = Math.abs(a.j - b.j);
        return 1.0 * Math.max(dx, dy) + (1.414 - 1.0) * Math.min(dx, dy);
    }
}