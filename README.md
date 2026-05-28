# Simulateur de pilotage de drone (FPV Drone Simulator)

## Description

This project is an FPV Drone Simulator built using Java and JavaFX. It features a physics-based flight model, an autopilot path planning system, a 3D simulation view, a minimap, and customizable world environments.

The simulator allows you to fly a virtual drone in different scenarios. The project uses Maven for dependency management and building.

## Features

- Physics-based drone flight model.
- 3D first-person view (FPV) and a minimap.
- Autopilot path planner.
- World customization via JSON files.
- Persistent saves in the `saves` directory.

## Members

- André Vitor Oliveira Brito
- Bahaeddine Aouanet
- Gabriela Gomes Cavalcanti Alves Monteiro
- João Victor Silva Bezerra Nascimento

## Requirements

- Java Development Kit (JDK) 21
- Maven 3.8+

## Build and Run

To compile and run the project using Maven, use the following commands:

```bash
# Compile the project
mvn clean compile

# Run the simulator
mvn javafx:run
```

## World Configuration using JSON

The simulator loads its world and environments from JSON files located in the `saves/` directory. By default, it creates and loads `world_default.json` if it does not exist.

You can create or modify these JSON files to change the drone's spawn position, add obstacles, and change environmental settings (like gravity and colors).

### JSON Structure and Example

Here is an example of a valid world configuration JSON file:

```json
{
  "worldName" : "Default World",
  "droneSpawn" : {
    "posX" : 0.0,
    "posY" : -30.0,
    "posZ" : 0.0,
    "yaw" : 0.0
  },
  "objects" : [
    {
      "name" : "Ground",
      "type" : "plane",
      "posX" : 0.0,
      "posY" : 0.0,
      "posZ" : 0.0,
      "sizeX" : 5000.0,
      "sizeY" : 1.0,
      "sizeZ" : 5000.0,
      "color" : "#228B22"
    },
    {
      "name" : "Reference Tower",
      "type" : "cylinder",
      "posX" : 150.0,
      "posY" : -70.0,
      "posZ" : 500.0,
      "sizeX" : 30.0,
      "sizeY" : 200.0,
      "sizeZ" : 30.0,
      "color" : "#FFA500"
    }
  ],
  "environment" : {
    "skyColor" : "#87CEEB",
    "groundColor" : "#228B22",
    "gravity" : 9.81,
    "showGrid" : true,
    "showTower" : true
  }
}
```

### Explanation of Properties

- **`worldName`**: The name of the world displayed in the application.
- **`droneSpawn`**: The starting coordinates `(posX, posY, posZ)` and orientation `yaw` for the drone. Note that the Y-axis might be inverted depending on the 3D engine used.
- **`objects`**: An array of objects to spawn in the world.
  - `name`: Identifier for the object.
  - `type`: Shape of the object ["plane", "cylinder", "box"].
  - `posX`, `posY`, `posZ`: Position of the object.
  - `sizeX`, `sizeY`, `sizeZ`: Dimensions of the object.
  - `color`: Hex code representing the object's color.
- **`environment`**: General settings for the simulation physics and visuals.
  - `skyColor`: Hex code for the sky.
  - `groundColor`: Hex code for the default ground if a custom one is not provided.
  - `gravity`: Float value for gravitational force.
  - `showGrid`: Boolean to show or hide the reference grid.
  - `showTower`: Boolean to show or hide a default reference tower.

### Loading Custom Worlds

1. Create a new `.json` file inside the `saves/` folder following the structure above (e.g., `saves/my_custom_world.json`).
2. Run the simulator.
3. In the menu bar at the top, navigate to **Configuration > World Configuration**.
4. Select your custom world from the list and load it.
