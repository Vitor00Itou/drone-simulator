package fr.enac.drone.controller;

import java.util.List;

public class Autopilot {
    private final List<double[]> waypoints;
    private int currentIdx = 0;
    private static final double WAYPOINT_RADIUS = 5.0;
    private static final double YAW_P_GAIN = 2.0;
    private static final double PITCH_P_GAIN = 0.5;

    public Autopilot(List<double[]> waypoints) {
        this.waypoints = waypoints;
    }

    public boolean isActive() {
        return currentIdx < waypoints.size();
    }

    public double[] computeControls(double currentX, double currentZ, double currentYawDeg) {
        if (!isActive()) return new double[]{0, 0, 0};

        double[] target = waypoints.get(currentIdx);
        double dx = target[0] - currentX;
        double dz = target[1] - currentZ;
        double dist = Math.hypot(dx, dz);

        if (dist < WAYPOINT_RADIUS) {
            currentIdx++;
            if (!isActive()) return new double[]{0, 0, 0};
            target = waypoints.get(currentIdx);
            dx = target[0] - currentX;
            dz = target[1] - currentZ;
        }

        double desiredAngle = Math.toDegrees(Math.atan2(dx, dz));
        double angleError = desiredAngle - currentYawDeg;
        while (angleError > 180) angleError -= 360;
        while (angleError <= -180) angleError += 360;

        double yawCmd = Math.max(-1, Math.min(1, YAW_P_GAIN * angleError / 180.0));
        double pitchCmd = 0;
        if (Math.abs(angleError) < 30) {
            pitchCmd = Math.min(1.0, PITCH_P_GAIN * dist / 10.0);
        }
        return new double[]{yawCmd, pitchCmd, 0};
    }
}