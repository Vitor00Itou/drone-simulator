package fr.enac.drone.model.drone;

/**
 * Internal phase of the return-to-home flight state.
 */
enum ReturnToHomePhase {
    /** The drone is climbing to the safe return altitude. */
    ASCENDING,
    /** The drone is traveling horizontally toward the home position. */
    CRUISING
}
