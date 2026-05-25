package fr.enac.drone.model.world;

import fr.enac.drone.model.drone.DroneModel;

final class TestDrone extends DroneModel {
    private final double x;
    private final double y;
    private final double z;

    TestDrone(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }
}
