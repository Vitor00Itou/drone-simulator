module fr.enac.drone {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires com.fasterxml.jackson.databind;
    
    exports fr.enac.drone;
    exports fr.enac.drone.model.drone;
    exports fr.enac.drone.model.world;
    exports fr.enac.drone.controller;
    exports fr.enac.drone.view;
    exports fr.enac.drone.utils;
}
