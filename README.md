# FPV Drone Flight Simulator

A 3D First-Person View (FPV) drone flight simulator developed in Java. This project provides a simplified 3D environment to simulate drone physics and test interaction concepts without real-world safety constraints. 

## Architecture & Technologies

This project strictly follows the **Model-View-Controller (MVC)** design pattern to ensure separation of concerns between the drone's physical data, the user input, and the 3D rendering system.

* **Language:** Java 21
* **Graphics Engine:** JavaFX 3D
* **Build Tool:** Maven
* **Design Pattern:** MVC 

## Project Structure

The source code is organized under `src/main/java/fr/enac/drone/`:
* `/model`: Contains the core logic, spatial coordinates, and physics state of the drone.
* `/controller`: Handles user keyboard inputs and maps them to movement commands.
* `/view`: Manages the 3D environment, rendering, and FPV camera logic using JavaFX `SubScene`.

## How to Run

1. Clone this repository to your local machine.
2. Open a terminal at the root directory of the project (where the `pom.xml` file is located).
3. Execute the following Maven command to compile and run the application:

    ```bash
    mvn clean javafx:run
    ```

## Flight Controls (Mode 2 Configuration)

The keyboard inputs are mapped to simulate a standard Mode 2 radio controller, which is the industry standard for real drones:

* **Left Analog (Throttle / Yaw):**
  * `W` : Ascend (Increase Altitude)
  * `S` : Descend (Decrease Altitude)
  * `A` : Rotate Left (Yaw)
  * `D` : Rotate Right (Yaw)

* **Right Analog (Pitch / Roll):**
  * `Up Arrow` : Pitch Forward
  * `Down Arrow` : Pitch Backward
  * `Left Arrow` : Roll Left
  * `Right Arrow` : Roll Right