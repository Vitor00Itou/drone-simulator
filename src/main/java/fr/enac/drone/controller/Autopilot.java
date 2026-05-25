package fr.enac.drone.controller;

import java.util.List;

public class Autopilot {
    private final List<double[]> waypoints;
    private int currentIdx = 0;
    private static final double WAYPOINT_RADIUS = 5.0;
    public Autopilot(List<double[]> waypoints) {
        this.waypoints = waypoints;
    }

    public boolean isActive() {
        return currentIdx < waypoints.size();
    }

    public DroneControls computeControls(double currentX, double currentZ, double currentYawDeg) {
        if (!isActive()) return new DroneControls(0.0, 0.0, 0.0);

        double[] target = waypoints.get(currentIdx);
        double dx = target[0] - currentX;
        double dz = target[1] - currentZ;
        double dist = Math.hypot(dx, dz);

        if (dist < WAYPOINT_RADIUS) {
            currentIdx++;
            if (!isActive()) return new DroneControls(0.0, 0.0, 0.0);
            target = waypoints.get(currentIdx);
            dx = target[0] - currentX;
            dz = target[1] - currentZ;
            dist = Math.hypot(dx, dz);
        }

        double desiredAngle = Math.toDegrees(Math.atan2(dx, dz));
        double angleError = desiredAngle - currentYawDeg;

        while (angleError > 180) angleError -= 360;
        while (angleError <= -180) angleError += 360;

        // Yaw rotates the camera smoothly to look at the target
        double yawCmd = Math.max(-1.0, Math.min(1.0, angleError / 45.0));

        double power = 1.0;

        // Brake smoothly at the final destination
        if (currentIdx == waypoints.size() - 1) {
            if (dist < 15.0) {
                power = Math.max(0.0, Math.min(1.0, dist / 10.0));
            }
        } else {
            // Smart Corner Braking: Check geometric sharpness of the upcoming corner
            if (dist < 20.0) {
                double[] nextTarget = waypoints.get(currentIdx + 1);
                
                double inAngle = Math.toDegrees(Math.atan2(target[0] - currentX, target[1] - currentZ));
                double outAngle = Math.toDegrees(Math.atan2(nextTarget[0] - target[0], nextTarget[1] - target[1]));
                
                double cornerDiff = Math.abs(inAngle - outAngle);
                while (cornerDiff > 180) cornerDiff -= 360;
                
                // If the next corner is sharper than 30 degrees, slow down to drift safely
                if (Math.abs(cornerDiff) > 30.0) {
                    power = Math.max(0.25, dist / 20.0);
                }
            }
        }

        // TRUE Omnidirectional flight! Move sideways and forwards simultaneously!
        double radError = Math.toRadians(angleError);
        double pitchCmd = Math.cos(radError) * power;
        double rollCmd  = Math.sin(radError) * power;

        return new DroneControls(yawCmd, pitchCmd, rollCmd);
    }
}