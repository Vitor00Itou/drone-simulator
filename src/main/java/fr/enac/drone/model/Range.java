package fr.enac.drone.model;

/**
 * Immutable numeric range with a default value.
 */
public final class Range {

    private final double min;
    private final double max;
    private final double defaultValue;

    public Range(double min, double max, double defaultValue) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum cannot exceed maximum");
        }

        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default value must be inside range");
        }

        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }
}
