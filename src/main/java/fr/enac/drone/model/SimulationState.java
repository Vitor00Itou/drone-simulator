package fr.enac.drone.model;

/**
 * Lifecycle state of the current simulation session.
 */
public enum SimulationState {
    /** The simulator is initialized and waiting for the first start input. */
    READY,
    /** The simulation loop is applying controls and physics updates. */
    RUNNING,
    /** The simulation is stopped temporarily without resetting model state. */
    PAUSED
}
