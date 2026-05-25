package fr.enac.drone.navigation;

/**
 * Immutable grid coordinate used by the path planner.
 *
 * @param i grid column index
 * @param j grid row index
 */
record GridNode(int i, int j) {
}
