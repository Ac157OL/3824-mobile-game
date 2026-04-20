# Collaborative Drawing & Recognition App

This is an Android application that enables collaborative drawing, real-time communication via a socket service, and an integrated PyTorch model for handwritten digit recognition (MNIST). The project formerly used the temporary namespace `com.example.myapplication2` which has been refactored to a more formal `com.example.drawingapp`.

## Project Features

- **Custom Drawing View (`DrawView.java`)**: A customizable View that records user sketching and calculates bounding boxes for efficient resizing.
- **Network Service (`NetworkService.java`)**: A background bound service managing raw socket connections with a remote server, handling two-way data streaming.
- **On-device AI Inference**: Integrated with `org.pytorch:pytorch_android` to perform edge inference on user-drawn digits using a pre-trained MNIST model (`mnist_rgb_model.pt`).
- **Real-time Collaboration (`MainActivity.java`)**: Uses a custom communication protocol (like `SERVER_MSG:`, `NUMBERS:`, `PLAYER:`) to synchronize drawing states or game commands in real-time.

## Recent Fixes & Improvements

1. **Package Name Refactor**: The project structure and the namespace in `build.gradle`, `<manifest>`, layouts, and all `*.java` files have been renamed to `com.example.drawingapp` for a more professional structure.
2. **Theme Renaming Check**: Migrated all style references from `Theme.MyApplication2` to `Theme.DrawingApp`.
3. **Redundant Imports**: Cleaned up duplicated imports that existed in Activity classes, enhancing code manageability.
4. **Service Lifecycle Bug Checking**: Ensured that the MediaPlayer and NetworkService appropriately initialize in `onStart` and clean up tightly in `onStop/onDestroy` without leaking state between activities transitioning.

## Structure

- `app/src/main/java/com/example/drawingapp/`
  - `MainActivity.java`: Main game loop, drawing logic, PyTorch inference, and socket listeners binding.
  - `LoginActivity.java`: Performs server authentication and connects `NetworkService` before navigation.
  - `DrawView.java`: Custom canvas engine logic holding drawing paths.
  - `NetworkService.java`: Abstraction providing an API over the low-level `java.net.Socket`.

## Setup & Build

1. Open this project directory in **Android Studio**.
2. Run Gradle Sync.
3. Click "Run" (Shift + F10) to deploy the APK to your Emulator or physical device.

*(Make sure your server instance is running at the designated IP/Port before deploying the Client app for the multiplayer aspect.)*

