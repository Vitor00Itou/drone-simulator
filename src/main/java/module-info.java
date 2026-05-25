/**
 * Java module definition for the FPV drone simulator application.
 */
@SuppressWarnings("module")
module fr.enac.drone {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires com.fasterxml.jackson.databind;
    requires jamepad;
    requires java.logging;
    
    exports fr.enac.drone;
    exports fr.enac.drone.input;
    exports fr.enac.drone.model;
    exports fr.enac.drone.model.drone;
    exports fr.enac.drone.model.world;
    exports fr.enac.drone.navigation;
    exports fr.enac.drone.persistence;
    exports fr.enac.drone.controller;
    exports fr.enac.drone.view;
    exports fr.enac.drone.view.settings;
    exports fr.enac.drone.utils;
}
