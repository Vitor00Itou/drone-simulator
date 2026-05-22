package fr.enac.drone.model;

import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import fr.enac.drone.utils.Vector3D;

import java.util.*;

/**
 * A* path planner that finds obstacle-free paths in the world.
 * Uses a grid-based discretization of the world with configurable cell size.
 * The planner builds a binary occupancy map and finds the shortest path
 * while respecting obstacle safety margins.
 */
public class PathPlanner {
    
    /** Size of each grid cell in world units (meters) */
    private static final double CELL_SIZE = 10.0;
    
    /** Reference to the world configuration containing all obstacles */
    private final WorldConfiguration world;
    
    /** Number of cells in the grid along the X axis */
    private final int gridWidth;
    
    /** Number of cells in the grid along the Z axis */
    private final int gridHeight;
    
    /** 2D boolean array marking blocked cells (true = obstacle) */
    private boolean[][] blocked;

    /**
     * Internal Node class for A* pathfinding.
     * Represents a cell in the grid with cost values and parent reference.
     */
    private static class Node {
        int x, z;           // Grid coordinates
        double g, f;        // Cost values: g = cost from start, f = g + heuristic
        Node parent;        // Parent node for path reconstruction
        
        /**
         * Creates a new node at the specified grid position.
         * 
         * @param x the X grid coordinate
         * @param z the Z grid coordinate
         */
        Node(int x, int z) { 
            this.x = x; 
            this.z = z; 
        }
        
        @Override 
        public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node other = (Node) o;
            return x == other.x && z == other.z;
        }
        
        @Override 
        public int hashCode() { 
            return 31 * x + z; 
        }
    }

    /**
     * Constructs a new PathPlanner for the given world configuration.
     * Automatically computes the grid size and builds the obstacle map.
     * 
     * @param world the world configuration containing obstacles
     */
    public PathPlanner(WorldConfiguration world) {
        this.world = world;
        double worldSize = computeWorldSize();
        this.gridWidth = (int) Math.ceil(worldSize / CELL_SIZE);
        this.gridHeight = gridWidth;
        this.blocked = new boolean[gridWidth][gridHeight];
        buildBlockedMap();
    }

    /**
     * Computes the required world size to contain all obstacles.
     * The size is determined by the farthest obstacle plus a safety margin.
     * 
     * @return the world size in world units (meters)
     */
    private double computeWorldSize() {
        double max = 5000.0;
        for (WorldObject obj : world.getObjects()) {
            if ("plane".equals(obj.getType())) {
                max = Math.max(max, Math.max(obj.getSizeX(), obj.getSizeZ()));
            }
            double hx = Math.abs(obj.getPosX()) + obj.getSizeX() / 2.0;
            double hz = Math.abs(obj.getPosZ()) + obj.getSizeZ() / 2.0;
            max = Math.max(max, 2.0 * Math.max(hx, hz));
        }
        return max + 200;
    }

    /**
     * Builds the binary occupancy map marking all cells that are blocked by obstacles.
     * Adds a safety margin around obstacles to prevent collisions.
     */
    private void buildBlockedMap() {
        // Initialize all cells as free
        for (int i = 0; i < gridWidth; i++) {
            Arrays.fill(blocked[i], false);
        }
        
        double halfSize = computeWorldSize() / 2.0;
        double safetyMargin = 5.0;  // 5 meters safety margin around obstacles

        // Mark cells occupied by each obstacle
        for (WorldObject obj : world.getObjects()) {
            if ("plane".equals(obj.getType())) continue;

            // Calculate obstacle bounding box with safety margin
            double minX = obj.getPosX() - obj.getSizeX() / 2.0 - safetyMargin;
            double maxX = obj.getPosX() + obj.getSizeX() / 2.0 + safetyMargin;
            double minZ = obj.getPosZ() - obj.getSizeZ() / 2.0 - safetyMargin;
            double maxZ = obj.getPosZ() + obj.getSizeZ() / 2.0 + safetyMargin;

            // Convert world coordinates to grid indices
            int colMin = worldToGridX(minX);
            int colMax = worldToGridX(maxX);
            int rowMin = worldToGridZ(minZ);
            int rowMax = worldToGridZ(maxZ);

            // Clamp to grid boundaries
            colMin = Math.max(0, Math.min(gridWidth - 1, colMin));
            colMax = Math.max(0, Math.min(gridWidth - 1, colMax));
            rowMin = Math.max(0, Math.min(gridHeight - 1, rowMin));
            rowMax = Math.max(0, Math.min(gridHeight - 1, rowMax));

            // Mark the bounding box cells as blocked
            for (int c = colMin; c <= colMax; c++) {
                for (int r = rowMin; r <= rowMax; r++) {
                    blocked[c][r] = true;
                }
            }
        }
    }

    /**
     * Converts a world X coordinate to a grid column index.
     * 
     * @param x the world X coordinate
     * @return the grid column index
     */
    private int worldToGridX(double x) {
        double halfSize = computeWorldSize() / 2.0;
        return (int) ((x + halfSize) / CELL_SIZE);
    }

    /**
     * Converts a world Z coordinate to a grid row index.
     * 
     * @param z the world Z coordinate
     * @return the grid row index
     */
    private int worldToGridZ(double z) {
        double halfSize = computeWorldSize() / 2.0;
        return (int) ((z + halfSize) / CELL_SIZE);
    }

    /**
     * Converts a grid column index to a world X coordinate.
     * Returns the center of the grid cell.
     * 
     * @param col the grid column index
     * @return the world X coordinate
     */
    private double gridToWorldX(int col) {
        double halfSize = computeWorldSize() / 2.0;
        return (col + 0.5) * CELL_SIZE - halfSize;
    }

    /**
     * Converts a grid row index to a world Z coordinate.
     * Returns the center of the grid cell.
     * 
     * @param row the grid row index
     * @return the world Z coordinate
     */
    private double gridToWorldZ(int row) {
        double halfSize = computeWorldSize() / 2.0;
        return (row + 0.5) * CELL_SIZE - halfSize;
    }

    /**
     * Converts a Vector3D world position to grid coordinates.
     * 
     * @param pos the world position
     * @return array containing [col, row] grid indices
     */
    private int[] worldToGrid(Vector3D pos) {
        return new int[]{worldToGridX(pos.x), worldToGridZ(pos.z)};
    }

    /**
     * Converts grid coordinates to a Vector3D world position.
     * 
     * @param col the grid column index
     * @param row the grid row index
     * @return the world position as Vector3D
     */
    private Vector3D gridToWorld(int col, int row) {
        return new Vector3D(gridToWorldX(col), 0, gridToWorldZ(row));
    }

    /**
     * Finds a path from start to target using the A* algorithm.
     * The path consists of waypoints at the centers of grid cells.
     * 
     * @param start the starting position (world coordinates)
     * @param target the target position (world coordinates)
     * @return a list of waypoints as Vector3D, or empty list if no path exists
     */
    public List<Vector3D> findPath(Vector3D start, Vector3D target) {
        // Rebuild the obstacle map to ensure it's up to date
        buildBlockedMap();
        
        // Convert start and target to grid coordinates
        int[] startIdx = worldToGrid(start);
        int[] targetIdx = worldToGrid(target);
        
        // Check if start or target is inside an obstacle
        if (blocked[startIdx[0]][startIdx[1]] || blocked[targetIdx[0]][targetIdx[1]]) {
            System.out.println("Start or target position is inside an obstacle!");
            return Collections.emptyList();
        }

        // Initialize start and target nodes
        Node startNode = new Node(startIdx[0], startIdx[1]);
        Node targetNode = new Node(targetIdx[0], targetIdx[1]);
        
        // Open set (priority queue ordered by f-score)
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        
        // Maps each node to its best parent for path reconstruction
        Map<Node, Node> cameFrom = new HashMap<>();
        
        // Maps each node to its current g-score (cost from start)
        Map<Node, Double> gScore = new HashMap<>();

        // Initialize start node
        startNode.g = 0;
        startNode.f = heuristic(startNode, targetNode);
        openSet.add(startNode);
        gScore.put(startNode, 0.0);

        // Main A* loop
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            // Goal reached - reconstruct the path
            if (current.equals(targetNode)) {
                List<Node> pathNodes = new ArrayList<>();
                Node cur = current;
                while (cur != null) {
                    pathNodes.add(0, cur);
                    cur = cameFrom.get(cur);
                }
                
                // Convert nodes to world waypoints
                List<Vector3D> waypoints = new ArrayList<>();
                for (Node n : pathNodes) {
                    waypoints.add(gridToWorld(n.x, n.z));
                }
                return waypoints;
            }
            
            // Explore all 8-connected neighbors (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    
                    int nx = current.x + dx;
                    int nz = current.z + dz;
                    
                    // Check grid boundaries
                    if (nx < 0 || nx >= gridWidth || nz < 0 || nz >= gridHeight) continue;
                    
                    // Skip blocked cells (obstacles)
                    if (blocked[nx][nz]) continue;
                    
                    Node neighbor = new Node(nx, nz);
                    
                    // Cost calculation: diagonal moves cost sqrt(2), orthogonal cost 1
                    double stepCost = (dx != 0 && dz != 0) ? 1.414 : 1.0;
                    double tentativeG = gScore.get(current) + stepCost;
                    
                    // If we found a better path to this neighbor
                    if (tentativeG < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        cameFrom.put(neighbor, current);
                        gScore.put(neighbor, tentativeG);
                        neighbor.g = tentativeG;
                        neighbor.f = tentativeG + heuristic(neighbor, targetNode);
                        
                        if (!openSet.contains(neighbor)) {
                            openSet.add(neighbor);
                        }
                    }
                }
            }
        }
        
        // No path found
        System.out.println("No path found!");
        return Collections.emptyList();
    }

    /**
     * Heuristic function for A* (Euclidean distance).
     * Returns the estimated cost from node a to node b.
     * 
     * @param a the source node
     * @param b the target node
     * @return the Euclidean distance between nodes
     */
    private double heuristic(Node a, Node b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }
}