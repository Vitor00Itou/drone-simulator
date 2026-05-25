package fr.enac.drone.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AutopilotTest {

    private static final double EPSILON = 1e-9;

    @Test
    void computesForwardPitchWhenWaypointIsAhead() {
        Autopilot autopilot = new Autopilot(List.of(new double[] {0.0, 20.0}));

        DroneControls controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertEquals(0.0, controls.yaw(), EPSILON);
        assertEquals(1.0, controls.pitch(), EPSILON);
        assertEquals(0.0, controls.roll(), EPSILON);
        assertTrue(autopilot.isActive());
    }

    @Test
    void yawsTowardWaypointWhenHeadingIsOffTarget() {
        Autopilot autopilot = new Autopilot(List.of(new double[] {10.0, 0.0}));

        DroneControls controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertEquals(1.0, controls.yaw(), EPSILON);
        assertEquals(0.0, controls.pitch(), EPSILON);
        assertEquals(1.0, controls.roll(), EPSILON);
    }

    @Test
    void returnsZeroControlsAndDeactivatesAfterLastWaypointReached() {
        Autopilot autopilot = new Autopilot(List.of(new double[] {1.0, 1.0}));

        DroneControls controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertEquals(0.0, controls.yaw(), EPSILON);
        assertEquals(0.0, controls.pitch(), EPSILON);
        assertEquals(0.0, controls.roll(), EPSILON);
        assertFalse(autopilot.isActive());
    }

    @Test
    void slowsDownForSharpCorners() {
        // Drone at (0,0), next waypoint is (0, 15), then an immediate 90-degree turn to (15, 15)
        Autopilot autopilot = new Autopilot(List.of(new double[] {0.0, 15.0}, new double[] {15.0, 15.0}));

        DroneControls controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertEquals(0.0, controls.yaw(), EPSILON);
        assertEquals(0.75, controls.pitch(), EPSILON);
        assertEquals(0.0, controls.roll(), EPSILON);
    }
}
