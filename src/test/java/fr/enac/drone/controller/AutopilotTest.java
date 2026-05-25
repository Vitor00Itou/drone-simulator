package fr.enac.drone.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AutopilotTest {

    private static final double EPSILON = 1e-9;

    @Test
    void computesForwardPitchWhenWaypointIsAhead() {
        Autopilot autopilot = new Autopilot(List.of(new double[] {0.0, 20.0}));

        double[] controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertArrayEquals(new double[] {0.0, 1.0, 0.0}, controls, EPSILON);
        assertTrue(autopilot.isActive());
    }

    @Test
    void yawsTowardWaypointWhenHeadingIsOffTarget() {
        Autopilot autopilot = new Autopilot(List.of(new double[] {10.0, 0.0}));

        double[] controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertArrayEquals(new double[] {1.0, 0.0, 0.0}, controls, EPSILON);
    }

    @Test
    void returnsZeroControlsAndDeactivatesAfterLastWaypointReached() {
        Autopilot autopilot = new Autopilot(List.of(new double[] {1.0, 1.0}));

        double[] controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertArrayEquals(new double[] {0.0, 0.0, 0.0}, controls, EPSILON);
        assertFalse(autopilot.isActive());
    }
}
