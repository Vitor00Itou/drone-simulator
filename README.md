# FPV Drone Simulator

A desktop simulator for piloting a first-person-view (FPV) drone through configurable 3D environments. Built with Java and JavaFX, the project combines real-time flight controls, collision detection, route planning, and persistent worlds in a single interactive application.

> Academic project developed at **ENAC** (École Nationale de l'Aviation Civile).

## Highlights

- Real-time drone flight model with configurable horizontal speed, vertical speed, and yaw sensitivity.
- Immersive 3D FPV camera, heads-up display, telemetry, flight trail, battery indicator, and proximity warnings.
- Collision detection against scene objects.
- Click-to-fly autopilot: select a destination on the minimap and an A* path planner computes an obstacle-aware route.
- Return-to-home behavior with a safe altitude computed from world obstacles.
- Keyboard and game-controller support, including QWERTY and AZERTY keyboard layouts.
- Configurable worlds stored as readable JSON files, with included obstacle-course and maze scenarios.
- Automated unit tests for navigation, autopilot behavior, collision detection, and persistence.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Desktop UI & 3D rendering | JavaFX 21 |
| Build & dependency management | Maven |
| World serialization | Jackson |
| Controller input | Jamepad |
| Testing | JUnit 5 |

## Getting started

### Prerequisites

- JDK 21
- Maven 3.8 or later

### Run locally

```bash
# Clone the repository
git clone https://github.com/Vitor00Itou/drone-simulator.git
cd drone-simulator

# Run the automated test suite
mvn test

# Launch the simulator
mvn javafx:run
```

The application opens in a maximized window and loads `saves/world_default.json` by default.

## Controls

The simulator can be flown using a keyboard or a compatible game controller. The active controls are also displayed inside the application.

| Action | Keyboard |
| --- | --- |
| Start, pause, resume | `Space` / `Enter` |
| Reset current session | `R` |
| Take off / arm | `O` |
| Land | `L` |
| Emergency stop | `F` |
| Return to home | `H` |
| Altitude | `W` / `S` (`Z` / `S` on AZERTY) |
| Yaw | `A` / `D` (`Q` / `D` on AZERTY) |
| Forward / backward | `↑` / `↓` |
| Lateral movement | `←` / `→` |
| Exit application | `Esc` |

Use the settings button in the upper-right corner to adjust flight parameters, choose a keyboard layout, toggle the in-app control guide, and load or generate worlds.

## Worlds and customization

Worlds are JSON files in [`saves/`](saves). The repository includes several ready-to-fly environments:

- `world_default` — open default environment.
- `obstacle_course` — obstacle-navigation scenario.
- `spiral_maze` — maze-style navigation challenge.
- `world_diferent_obstacles` — environment with varied obstacle types.

You can load a saved world from the settings panel or add a new JSON file to the `saves/` directory. A world contains the drone spawn point, 3D objects, and scene settings. Example:

```json
{
  "worldName": "Training Ground",
  "droneSpawn": {
    "posX": 0.0,
    "posY": -30.0,
    "posZ": 0.0,
    "yaw": 0.0
  },
  "objects": [
    {
      "name": "Ground",
      "type": "plane",
      "posX": 0.0,
      "posY": 0.0,
      "posZ": 0.0,
      "sizeX": 5000.0,
      "sizeY": 1.0,
      "sizeZ": 5000.0,
      "color": "#228B22",
      "texture": "ground_grass.png"
    },
    {
      "name": "Training Tower",
      "type": "cylinder",
      "posX": 150.0,
      "posY": -70.0,
      "posZ": 500.0,
      "sizeX": 30.0,
      "sizeY": 200.0,
      "sizeZ": 30.0,
      "color": "#FFA500"
    }
  ],
  "environment": {
    "skyColor": "#87CEEB",
    "showGrid": true
  }
}
```

Supported object types are `plane`, `box`, and `cylinder`. The optional `texture` value references an image in [`src/main/resources/textures/`](src/main/resources/textures).

## Project structure

```text
src/main/java/fr/enac/drone/
├── controller/    # Input handling and flight commands
├── input/         # Keyboard and controller mappings
├── model/         # Drone state, world model, physics, collisions
├── navigation/    # Grid map, A* planner, autopilot
├── persistence/   # JSON world storage
└── view/          # JavaFX 3D scene, HUD, minimap, settings

src/test/java/     # Unit tests
saves/             # World configurations
```

## Verification

```bash
mvn test
```

## Team

- André Vitor Oliveira Brito
- Bahaeddine Aouanet
- Gabriela Gomes Cavalcanti Alves Monteiro
- João Victor Silva Bezerra Nascimento

## License

Distributed under the [MIT License](LICENSE.txt).
