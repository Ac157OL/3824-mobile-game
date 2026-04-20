# 3824 Multiplayer Drawing Game

This project is essentially a multiplayer mobile game based on the "3824" gameplay concept, implemented as an Android application. Players collaborate or compete in real-time by drawing numbers on their screens, which are then recognized by an integrated on-device AI model (PyTorch with MNIST) and synchronized across clients via a socket server.

## Game Rules & Concept (3824)

- **Objective**: Players connect in real-time and are presented with numerical tasks or topics via the server. 
- **Gameplay**: 
  - Players must manually sketch numbers (e.g., 3, 8, 2, 4) on the blank canvas provided by the app.
  - After drawing, the app uses a PyTorch (MNIST) model deployed on the device to automatically recognize the handwritten digit.
  - The recognized number is then sent over the network to the server, interacting with other players' inputs to score points or progress through the game rounds until the "GAMEOVER" state is triggered.

## Project Features

- **Custom Drawing View**: A customizable canvas (`DrawView.java`) that records user sketching and calculates bounding boxes for efficient resizing.
- **Network Service**: A background service (`NetworkService.java`) managing raw socket connections with a remote server, handling two-way data streaming for multiplayer features.
- **On-device AI Inference**: Integrated with `org.pytorch:pytorch_android` to perform edge inference on user-drawn digits using a pre-trained MNIST model.
- **Real-time Collaboration**: Uses a custom communication protocol (like `SERVER_MSG:`, `NUMBERS:`, `PLAYER:`, `GAMEOVER:`, `SCORE:`) to synchronize drawing states, game commands, and player scores in real-time.

## Code Structure

- `app/src/main/java/com/example/drawingapp/`
  - `MainActivity.java`: Main game loop, drawing logic, PyTorch inference, score updating, and socket listeners binding.
  - `LoginActivity.java`: Performs server validation and connects `NetworkService` before navigation.
  - `DrawView.java`: Custom canvas engine logic holding drawing paths.
  - `NetworkService.java`: Abstraction providing an API over the low-level `java.net.Socket`.

## How to Build and Package (APK)

To compile this code and package it into an installable Android Application (`.apk`):

**Method 1: Using Android Studio (Recommended)**
1. Open this project directory (`3824`) in Android Studio.
2. Wait for the initial Gradle sync to complete and resolve `org.pytorch` dependencies.
3. From the top menu, navigate to `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. Once built, you can find your APK via the popup notification or navigate to `app/build/outputs/apk/debug/app-debug.apk`.

**Method 2: Using Command Line (Gradle)**
1. Ensure you have the Java command-line tools and Android SDK correctly set in your environment.
2. Open your terminal at the root of the project.
3. Execute the Gradle wrapper:
   - On Linux/Mac: `chmod +x gradlew && ./gradlew assembleDebug`
   - On Windows: `gradlew.bat assembleDebug`
4. The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Running the Application

1. **Install**: Transfer the generated `app-debug.apk` to your Android device or drag-and-drop it into your running Android Emulator to install.
2. **Launch**: Open the newly installed app on your device.
3. **Connect**: In the login screen, enter your username and the IP address/Port of the dedicated socket game server you wish to connect to. 
4. **Play**: Once connected, draw on the canvas based on prompts and hit recognize/send to play against others.
