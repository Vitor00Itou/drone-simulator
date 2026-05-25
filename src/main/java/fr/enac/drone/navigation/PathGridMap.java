package fr.enac.drone.navigation;

final class PathGridMap {
    private final boolean[][] hardBlocked;
    private final double[][] penalty;

    PathGridMap(int width, int height) {
        this.hardBlocked = new boolean[width][height];
        this.penalty = new double[width][height];
    }

    boolean[][] hardBlocked() {
        return hardBlocked;
    }

    double[][] penalty() {
        return penalty;
    }
}
