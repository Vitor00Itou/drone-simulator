package fr.enac.drone.navigation;

import fr.enac.drone.model.WorldPosition2D;

import java.util.List;

public class Autopilot {
    private static final double WAYPOINT_RADIUS = 5.0;
    private static final double FINAL_BRAKE_DISTANCE = 15.0;
    private static final double FINAL_BRAKE_FULL_STOP_DISTANCE = 10.0;
    private static final double CORNER_BRAKE_DISTANCE = 20.0;
    private static final double SHARP_CORNER_DEGREES = 30.0;
    private static final double MIN_CORNER_POWER = 0.25;

    private final List<WorldPosition2D> waypoints;
    private int currentIdx = 0;

    public Autopilot(List<WorldPosition2D> waypoints) {
        this.waypoints = waypoints;
    }

    public boolean isActive() {
        return currentIdx < waypoints.size();
    }

    public FlightControlCommand computeControls(double currentX, double currentZ, double currentYawDeg) {
        if (!isActive()) return FlightControlCommand.NEUTRAL;

        WorldPosition2D target = waypoints.get(currentIdx);
        double dx = target.x() - currentX;
        double dz = target.z() - currentZ;
        double dist = Math.hypot(dx, dz);

        if (dist < WAYPOINT_RADIUS) {
            currentIdx++;
            if (!isActive()) return FlightControlCommand.NEUTRAL;
            target = waypoints.get(currentIdx);
            dx = target.x() - currentX;
            dz = target.z() - currentZ;
            dist = Math.hypot(dx, dz);
        }

        double desiredAngle = Math.toDegrees(Math.atan2(dx, dz));
        double angleError = normalizeDegrees(desiredAngle - currentYawDeg);
        double yawInput = Math.max(-1.0, Math.min(1.0, angleError / 45.0));
        double power = computePower(currentX, currentZ, target, dist);

        double radError = Math.toRadians(angleError);
        double forwardInput = Math.cos(radError) * power;
        double lateralInput = Math.sin(radError) * power;

        return new FlightControlCommand(yawInput, forwardInput, lateralInput);
    }

    private double computePower(double currentX, double currentZ, WorldPosition2D target, double dist) {
        if (currentIdx == waypoints.size() - 1) {
            if (dist < FINAL_BRAKE_DISTANCE) {
                return Math.max(0.0, Math.min(1.0, dist / FINAL_BRAKE_FULL_STOP_DISTANCE));
            }
            return 1.0;
        }

        if (dist >= CORNER_BRAKE_DISTANCE) {
            return 1.0;
        }

        WorldPosition2D nextTarget = waypoints.get(currentIdx + 1);
        double inAngle = Math.toDegrees(Math.atan2(target.x() - currentX, target.z() - currentZ));
        double outAngle = Math.toDegrees(Math.atan2(nextTarget.x() - target.x(), nextTarget.z() - target.z()));
        double cornerDiff = Math.abs(normalizeDegrees(inAngle - outAngle));

        if (cornerDiff > SHARP_CORNER_DEGREES) {
            return Math.max(MIN_CORNER_POWER, dist / CORNER_BRAKE_DISTANCE);
        }

        return 1.0;
    }

    private double normalizeDegrees(double angle) {
        while (angle > 180) angle -= 360;
        while (angle <= -180) angle += 360;
        return angle;
    }
}
