package fr.enac.drone.navigation;

import fr.enac.drone.model.WorldPosition2D;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Builds obstacle-aware 2D paths over the world X/Z plane using an A* grid search.
 */
public class PathPlanner {
    private static final double CELL_SIZE = 4.0;
    private static final double DRONE_HEIGHT_MARGIN = 2.0;
    private static final double HARD_MARGIN = 5.0;
    private static final double SOFT_MARGIN = 12.0;
    private static final double MAX_PENALTY = 50.0;
    private static final int MAX_UNBLOCK_SEARCH_CELLS = 20;
    private static final double DIAGONAL_COST = 1.414;

    private final int gridWidth;
    private final int gridHeight;
    private final double worldHalfSize;
    private final WorldConfiguration config;

    /**
     * Creates a planner for the supplied world configuration.
     *
     * @param config world configuration used to build obstacle grids
     */
    public PathPlanner(WorldConfiguration config) {
        this.config = config;
        double worldSize = computeWorldSize(config);
        this.worldHalfSize = worldSize / 2.0;
        this.gridWidth = (int) Math.ceil(worldSize / CELL_SIZE);
        this.gridHeight = gridWidth;
    }

    /**
     * Computes the square world size needed to contain the configured objects.
     *
     * @param cfg source world configuration
     * @return world size in meters
     */
    private double computeWorldSize(WorldConfiguration cfg) {
        double max = 5000.0;
        for (WorldObject obj : cfg.getObjects()) {
            if (obj.isPlane()) {
                max = Math.max(max, Math.max(obj.getSizeX(), obj.getSizeZ()));
            }

            double hx = Math.abs(obj.getPosX()) + obj.getFootprintHalfWidth();
            double hz = Math.abs(obj.getPosZ()) + obj.getFootprintHalfDepth();
            max = Math.max(max, 2.0 * Math.max(hx, hz));
        }
        return max;
    }

    /**
     * Builds a grid map for the drone's current altitude.
     *
     * @param currentY current drone Y coordinate
     * @return occupancy and penalty map
     */
    private PathGridMap buildGridMap(double currentY) {
        PathGridMap map = new PathGridMap(gridWidth, gridHeight);

        for (WorldObject obj : config.getObjects()) {
            if (obj.isPlane()) continue;

            if (currentY < obj.getTopY() - DRONE_HEIGHT_MARGIN
                    || currentY > obj.getBottomY() + DRONE_HEIGHT_MARGIN) {
                continue;
            }

            double halfX = obj.getFootprintHalfWidth();
            double halfZ = obj.getFootprintHalfDepth();
            double maxInfluence = HARD_MARGIN + SOFT_MARGIN;
            int colMin = clamp(worldToGridI(obj.getPosX() - halfX - maxInfluence), 0, gridWidth - 1);
            int colMax = clamp(worldToGridI(obj.getPosX() + halfX + maxInfluence), 0, gridWidth - 1);
            int rowMin = clamp(worldToGridJ(obj.getPosZ() - halfZ - maxInfluence), 0, gridHeight - 1);
            int rowMax = clamp(worldToGridJ(obj.getPosZ() + halfZ + maxInfluence), 0, gridHeight - 1);

            for (int i = colMin; i <= colMax; i++) {
                for (int j = rowMin; j <= rowMax; j++) {
                    double cellX = gridToWorldX(i);
                    double cellZ = gridToWorldZ(j);
                    double dist = distanceToFootprint(cellX, cellZ, obj, halfX, halfZ);

                    if (dist <= HARD_MARGIN) {
                        map.hardBlocked()[i][j] = true;
                    } else if (dist <= maxInfluence) {
                        double penaltyFactor = 1.0 - ((dist - HARD_MARGIN) / SOFT_MARGIN);
                        map.penalty()[i][j] = Math.max(map.penalty()[i][j], penaltyFactor * MAX_PENALTY);
                    }
                }
            }
        }
        return map;
    }

    /**
     * Computes the horizontal distance from a cell center to an object's footprint.
     *
     * @param cellX cell center X coordinate
     * @param cellZ cell center Z coordinate
     * @param obj obstacle object
     * @param halfX object footprint half-width
     * @param halfZ object footprint half-depth
     * @return signed distance to the footprint boundary
     */
    private double distanceToFootprint(double cellX, double cellZ, WorldObject obj, double halfX, double halfZ) {
        if (obj.isCylinder()) {
            return Math.hypot(cellX - obj.getPosX(), cellZ - obj.getPosZ()) - obj.getRadius();
        }

        double dx = Math.max(0.0, Math.abs(cellX - obj.getPosX()) - halfX);
        double dz = Math.max(0.0, Math.abs(cellZ - obj.getPosZ()) - halfZ);
        return Math.hypot(dx, dz);
    }

    /**
     * Converts a world X coordinate to a grid column.
     *
     * @param worldX world X coordinate
     * @return grid column index
     */
    private int worldToGridI(double worldX) { return (int) ((worldX + worldHalfSize) / CELL_SIZE); }

    /**
     * Converts a world Z coordinate to a grid row.
     *
     * @param worldZ world Z coordinate
     * @return grid row index
     */
    private int worldToGridJ(double worldZ) { return (int) ((worldZ + worldHalfSize) / CELL_SIZE); }

    /**
     * Converts a grid column to the world X coordinate at the cell center.
     *
     * @param i grid column index
     * @return world X coordinate
     */
    private double gridToWorldX(int i) { return (i + 0.5) * CELL_SIZE - worldHalfSize; }

    /**
     * Converts a grid row to the world Z coordinate at the cell center.
     *
     * @param j grid row index
     * @return world Z coordinate
     */
    private double gridToWorldZ(int j) { return (j + 0.5) * CELL_SIZE - worldHalfSize; }

    /**
     * Restricts an integer to a closed range.
     *
     * @param v value to restrict
     * @param min minimum accepted value
     * @param max maximum accepted value
     * @return clamped value
     */
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    /**
     * Removes unnecessary intermediate nodes while preserving obstacle clearance.
     *
     * @param pathNodes raw A* path nodes
     * @param map occupancy map used for visibility checks
     * @return simplified path in world coordinates
     */
    private List<WorldPosition2D> simplifyPath(List<GridNode> pathNodes, PathGridMap map) {
        if (pathNodes.size() <= 2) {
            return pathNodes.stream().map(this::toWorldPosition).toList();
        }

        List<WorldPosition2D> simplified = new ArrayList<>();
        simplified.add(toWorldPosition(pathNodes.get(0)));

        GridNode current = pathNodes.get(0);
        for (int i = 1; i < pathNodes.size() - 1; i++) {
            GridNode next = pathNodes.get(i + 1);
            if (hasLineOfSight(current, next, map)) {
                continue;
            }

            current = pathNodes.get(i);
            simplified.add(toWorldPosition(current));
        }

        simplified.add(toWorldPosition(pathNodes.get(pathNodes.size() - 1)));
        return simplified;
    }

    /**
     * Converts a grid node to a world X/Z coordinate.
     *
     * @param node grid node
     * @return world coordinate at the node center
     */
    private WorldPosition2D toWorldPosition(GridNode node) {
        return new WorldPosition2D(gridToWorldX(node.i()), gridToWorldZ(node.j()));
    }

    /**
     * Checks whether two grid nodes can be connected without crossing blocked cells.
     *
     * @param a start grid node
     * @param b end grid node
     * @param map occupancy map
     * @return {@code true} when the straight segment is clear
     */
    private boolean hasLineOfSight(GridNode a, GridNode b, PathGridMap map) {
        double x0 = a.i() + 0.5;
        double y0 = a.j() + 0.5;
        double x1 = b.i() + 0.5;
        double y1 = b.j() + 0.5;

        double dist = Math.hypot(x1 - x0, y1 - y0);
        int steps = (int) Math.ceil(dist * 3.0);

        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int cx = clamp((int) (x0 + t * (x1 - x0)), 0, gridWidth - 1);
            int cy = clamp((int) (y0 + t * (y1 - y0)), 0, gridHeight - 1);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = clamp(cx + dx, 0, gridWidth - 1);
                    int ny = clamp(cy + dy, 0, gridHeight - 1);
                    if (map.hardBlocked()[nx][ny]) return false;
                }
            }
        }
        return true;
    }

    /**
     * Finds the closest unblocked grid cell near a blocked target cell.
     *
     * @param startI target grid column
     * @param startJ target grid row
     * @param map occupancy map
     * @return nearest unblocked node, or {@code null} when none is found nearby
     */
    private GridNode findNearestUnblocked(int startI, int startJ, PathGridMap map) {
        Queue<GridNode> queue = new LinkedList<>();
        Set<GridNode> visited = new HashSet<>();
        GridNode start = new GridNode(startI, startJ);
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            GridNode curr = queue.poll();
            if (!map.hardBlocked()[curr.i()][curr.j()]) {
                return curr;
            }
            if (Math.abs(curr.i() - startI) > MAX_UNBLOCK_SEARCH_CELLS
                    || Math.abs(curr.j() - startJ) > MAX_UNBLOCK_SEARCH_CELLS) {
                return null;
            }

            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    int ni = curr.i() + di;
                    int nj = curr.j() + dj;
                    if (ni >= 0 && ni < gridWidth && nj >= 0 && nj < gridHeight) {
                        GridNode n = new GridNode(ni, nj);
                        if (visited.add(n)) {
                            queue.add(n);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds a safe path from the drone position to a target X/Z coordinate.
     *
     * @param startX current drone X coordinate
     * @param startY current drone Y coordinate
     * @param startZ current drone Z coordinate
     * @param targetX target X coordinate
     * @param targetZ target Z coordinate
     * @return simplified waypoint path, or an empty list when no path exists
     */
    public List<WorldPosition2D> findPath(
            double startX,
            double startY,
            double startZ,
            double targetX,
            double targetZ
    ) {
        PathGridMap map = buildGridMap(startY);

        int si = clamp(worldToGridI(startX), 0, gridWidth - 1);
        int sj = clamp(worldToGridJ(startZ), 0, gridHeight - 1);
        int ti = clamp(worldToGridI(targetX), 0, gridWidth - 1);
        int tj = clamp(worldToGridJ(targetZ), 0, gridHeight - 1);

        map.hardBlocked()[si][sj] = false;

        if (map.hardBlocked()[ti][tj]) {
            GridNode nearest = findNearestUnblocked(ti, tj, map);
            if (nearest == null) return Collections.emptyList();
            ti = nearest.i();
            tj = nearest.j();
        }

        GridNode start = new GridNode(si, sj);
        GridNode target = new GridNode(ti, tj);

        Map<GridNode, Double> gScores = new HashMap<>();
        Map<GridNode, Double> fScores = new HashMap<>();
        Map<GridNode, GridNode> cameFrom = new HashMap<>();

        PriorityQueue<GridNode> open = new PriorityQueue<>(
                Comparator.comparingDouble(n -> fScores.getOrDefault(n, Double.MAX_VALUE))
        );

        gScores.put(start, 0.0);
        fScores.put(start, heuristic(start, target));
        open.add(start);

        while (!open.isEmpty()) {
            GridNode current = open.poll();
            if (current.equals(target)) {
                LinkedList<GridNode> pathNodes = new LinkedList<>();
                GridNode p = current;
                while (p != null) {
                    pathNodes.addFirst(p);
                    p = cameFrom.get(p);
                }
                return simplifyPath(pathNodes, map);
            }

            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (di == 0 && dj == 0) continue;
                    int ni = current.i() + di;
                    int nj = current.j() + dj;
                    if (ni < 0 || ni >= gridWidth || nj < 0 || nj >= gridHeight) continue;
                    if (map.hardBlocked()[ni][nj]) continue;

                    if (di != 0 && dj != 0
                            && (map.hardBlocked()[current.i() + di][current.j()]
                            || map.hardBlocked()[current.i()][current.j() + dj])) {
                        continue;
                    }

                    GridNode neighbor = new GridNode(ni, nj);
                    double baseCost = (di != 0 && dj != 0) ? DIAGONAL_COST : 1.0;
                    double moveCost = baseCost + (map.penalty()[ni][nj] * baseCost);
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

    /**
     * Estimates remaining grid distance for A* using octile movement costs.
     *
     * @param a source node
     * @param b target node
     * @return admissible grid-distance estimate
     */
    private double heuristic(GridNode a, GridNode b) {
        int dx = Math.abs(a.i() - b.i());
        int dy = Math.abs(a.j() - b.j());
        return Math.max(dx, dy) + (DIAGONAL_COST - 1.0) * Math.min(dx, dy);
    }
}
