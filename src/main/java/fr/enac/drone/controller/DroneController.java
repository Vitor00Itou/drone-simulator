package fr.enac.drone.controller;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;
import fr.enac.drone.model.drone.DroneModel;

public class DroneController {
    private final DroneModel model;
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private Autopilot autopilot;
    private Runnable onAutopilotFinished; // callback pour effacer la cible

    public DroneController(DroneModel model) {
        this.model = model;
    }

    public void setAutopilot(Autopilot autopilot) {
        this.autopilot = autopilot;
        if (autopilot == null) {
            model.clearTargetYaw();
        }
    }

    public void setOnAutopilotFinished(Runnable callback) {
        this.onAutopilotFinished = callback;
    }

    public void addKey(KeyCode code) { activeKeys.add(code); }
    public void removeKey(KeyCode code) { activeKeys.remove(code); }

    public void update(double deltaTime) {
        double pitchInput = 0, rollInput = 0, throttleInput = 0;

        // --- Si un autopilote est actif, on regarde d'abord si l'utilisateur veut reprendre la main ---
        if (autopilot != null && autopilot.isActive()) {
            if (!activeKeys.isEmpty()) {
                // L'utilisateur a appuyé sur une touche → annulation immédiate de l'autopilote
                if (onAutopilotFinished != null) onAutopilotFinished.run();
                setAutopilot(null);
                // On tombe dans le mode manuel ci‑dessous
            } else {
                // Pas de touche → on suit l'autopilote
                double[] cmd = autopilot.computeControls(model.getX(), model.getZ(), model.getYaw());
                double yawCmd = cmd[0];        // -1..1, >0 = droite
                double pitchCmd = cmd[1];      // -1..1, >0 = avant

                double desiredYawRate = yawCmd * 40.0; // °/s
                model.setTargetYawVelocity(desiredYawRate);
                model.updateYaw(deltaTime);

                pitchInput = pitchCmd;
                model.updatePhysics(pitchInput, 0, throttleInput, deltaTime);
                return;
            }
        }

        // --- Fin normale de l'autopilote (arrivé à destination) ---
        if (autopilot != null && !autopilot.isActive()) {
            if (onAutopilotFinished != null) onAutopilotFinished.run();
            setAutopilot(null);
        }

        // ----- Mode manuel (touches du clavier) -----
        model.clearTargetYaw();

        if (activeKeys.contains(KeyCode.A)) model.yawLeft(deltaTime);
        if (activeKeys.contains(KeyCode.D)) model.yawRight(deltaTime);
        model.updateYaw(deltaTime);

        if (activeKeys.contains(KeyCode.W)) throttleInput -= 1;
        if (activeKeys.contains(KeyCode.S)) throttleInput += 1;
        if (activeKeys.contains(KeyCode.UP)) pitchInput += 1;
        if (activeKeys.contains(KeyCode.DOWN)) pitchInput -= 1;
        if (activeKeys.contains(KeyCode.RIGHT)) rollInput += 1;
        if (activeKeys.contains(KeyCode.LEFT)) rollInput -= 1;

        model.updatePhysics(pitchInput, rollInput, throttleInput, deltaTime);
    }
}