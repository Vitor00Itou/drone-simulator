package fr.enac.drone.navigation;

/**
 * Stores obstacle occupancy and soft penalties for path-planning cells.
 */
final class PathGridMap {
    private final boolean[][] hardBlocked;
    private final double[][] penalty;

    /**
     * Creates an empty grid map.
     *
     * @param width number of grid columns
     * @param height number of grid rows
     */
    PathGridMap(int width, int height) {
        this.hardBlocked = new boolean[width][height];
        this.penalty = new double[width][height];
    }

    /**
     * Returns the hard-blocked cell matrix.
     *
     * @return {@code true} for cells that cannot be crossed
     */
    boolean[][] hardBlocked() {
        return hardBlocked;
    }

    /**
     * Returns the movement penalty matrix.
     *
     * @return per-cell traversal penalty values
     */
    double[][] penalty() {
        return penalty;
    }
}
