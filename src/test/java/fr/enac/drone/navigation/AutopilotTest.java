package fr.enac.drone.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.enac.drone.model.WorldPosition2D;

class AutopilotTest {

    private static final double EPSILON = 1e-9;

    @Test
    void computesForwardInputWhenWaypointIsAhead() {
        Autopilot autopilot = new Autopilot(List.of(new WorldPosition2D(0.0, 20.0)));

        FlightControlCommand controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertControlCommand(controls, 0.0, 1.0, 0.0);
        assertTrue(autopilot.isActive());
    }

    @Test
    void yawsAndMovesLaterallyTowardWaypointWhenHeadingIsOffTarget() {
        Autopilot autopilot = new Autopilot(List.of(new WorldPosition2D(10.0, 0.0)));

        FlightControlCommand controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertControlCommand(controls, 1.0, 0.0, 1.0);
    }

    @Test
    void returnsZeroControlsAndDeactivatesAfterLastWaypointReached() {
        Autopilot autopilot = new Autopilot(List.of(new WorldPosition2D(1.0, 1.0)));

        FlightControlCommand controls = autopilot.computeControls(0.0, 0.0, 0.0);

        assertControlCommand(controls, 0.0, 0.0, 0.0);
        assertFalse(autopilot.isActive());
    }

    private static void assertControlCommand(
            FlightControlCommand command,
            double yawInput,
            double forwardInput,
            double lateralInput
    ) {
        assertEquals(yawInput, command.yawInput(), EPSILON);
        assertEquals(forwardInput, command.forwardInput(), EPSILON);
        assertEquals(lateralInput, command.lateralInput(), EPSILON);
    }
}
