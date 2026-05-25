package fr.enac.drone.model;

/**
 * Immutable numeric range that stores a minimum, maximum, and default value.
 */
public final class Range {

    private final double min;
    private final double max;
    private final double defaultValue;

    /**
     * Creates a validated numeric range.
     *
     * @param min lowest accepted value
     * @param max highest accepted value
     * @param defaultValue value used when no explicit setting is provided
     * @throws IllegalArgumentException if the range is invalid or the default is outside it
     */
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

    /**
     * Returns the lowest accepted value.
     *
     * @return minimum range value
     */
    public double getMin() {
        return min;
    }

    /**
     * Returns the highest accepted value.
     *
     * @return maximum range value
     */
    public double getMax() {
        return max;
    }

    /**
     * Returns the recommended value for a fresh configuration.
     *
     * @return default range value
     */
    public double getDefaultValue() {
        return defaultValue;
    }

    /**
     * Restricts a value so it stays inside this range.
     *
     * @param value value to restrict
     * @return {@code value} clamped between {@link #getMin()} and {@link #getMax()}
     */
    public double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }
}
