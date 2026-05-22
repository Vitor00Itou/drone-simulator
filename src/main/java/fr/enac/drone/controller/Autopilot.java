package fr.enac.drone.controller;

import java.util.List;

/**
 * Autonomous pilot that follows a sequence of waypoints.
 * Provides smooth yaw control and progressive speed management.
 * The autopilot automatically transitions between waypoints and
 * slows down when approaching the final destination.
 */
public class Autopilot {
    
    /** List of waypoints to follow (each waypoint is [x, z] in world coordinates) */
    private final List<double[]> waypoints;
    
    /** Index of the current waypoint being followed */
    private int currentIdx = 0;
    
    /** Distance threshold to consider a waypoint reached (meters) */
    private static final double WAYPOINT_RADIUS = 15.0;
    
    /** Proportional gain for yaw (heading) control */
    private static final double YAW_P_GAIN = 1.5;
    
    /** Maximum yaw command value (clamped to [-MAX_YAW_CMD, MAX_YAW_CMD]) */
    private static final double MAX_YAW_CMD = 0.8;
    
    /** Maximum pitch command (forward speed) value */
    private static final double MAX_PITCH_CMD = 0.8;
    
    /** Angle tolerance (degrees) - only move forward when yaw error is within this range */
    private static final double ANGLE_TOLERANCE = 60.0;
    
    /** Distance at which to start slowing down for smooth arrival (meters) */
    private static final double SLOW_DOWN_DISTANCE = 50.0;

    /**
     * Constructs a new Autopilot with the given waypoints.
     * 
     * @param waypoints list of waypoints as [x, z] double arrays
     */
    public Autopilot(List<double[]> waypoints) {
        this.waypoints = waypoints;
    }

    /**
     * Checks if the autopilot is still active (has remaining waypoints).
     * 
     * @return true if there are waypoints left to follow
     */
    public boolean isActive() {
        return currentIdx < waypoints.size();
    }

    /**
     * Computes control commands based on current drone state.
     * Implements proportional yaw control and progressive speed management.
     * 
     * @param currentX current X position in world coordinates (meters)
     * @param currentZ current Z position in world coordinates (meters)
     * @param currentYawDeg current yaw angle in degrees (0 = north, positive = clockwise)
     * @return array [yawCommand, pitchCommand, 0] where commands are in range [-1, 1]
     */
    public double[] computeControls(double currentX, double currentZ, double currentYawDeg) {
        if (!isActive()) return new double[]{0, 0, 0};

        // Get current target waypoint
        double[] target = waypoints.get(currentIdx);
        double dx = target[0] - currentX;
        double dz = target[1] - currentZ;
        double distance = Math.hypot(dx, dz);

        // Move to next waypoint if current is reached
        if (distance < WAYPOINT_RADIUS) {
            currentIdx++;
            if (!isActive()) return new double[]{0, 0, 0};
            target = waypoints.get(currentIdx);
            dx = target[0] - currentX;
            dz = target[1] - currentZ;
        }

        // Calculate desired heading angle to target
        double desiredAngle = Math.toDegrees(Math.atan2(dx, dz));
        double angleError = desiredAngle - currentYawDeg;
        
        // Normalize angle error to [-180, 180] range
        while (angleError > 180) angleError -= 360;
        while (angleError <= -180) angleError += 360;

        // Yaw command: proportional control with saturation
        double yawCmd = Math.max(-MAX_YAW_CMD, Math.min(MAX_YAW_CMD, YAW_P_GAIN * angleError / 180.0));
        
        // Pitch command: only move forward when roughly aligned with target
        double pitchCmd = 0;
        if (Math.abs(angleError) < ANGLE_TOLERANCE) {
            pitchCmd = 1.0;  // Full speed command (will be scaled by MAX_PITCH_CMD)
            
            // Slow down when approaching target for smooth arrival
            if (distance < SLOW_DOWN_DISTANCE) {
                pitchCmd = pitchCmd * (distance / SLOW_DOWN_DISTANCE);
            }
            
            // Apply maximum speed limit
            pitchCmd = Math.min(MAX_PITCH_CMD, pitchCmd);
        }
        
        return new double[]{yawCmd, pitchCmd, 0};
    }
}