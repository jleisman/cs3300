# CS3300 Project - VibeVision
Team "CS is just our hobby"

Intro to Software Engineering Group Project - Android App interface for ML mood model

[//]: # (TODO: Finish and add ML bits and pieces)

## Project Statement
This project an Android application that performs real‑time camera capture and on‑device image analysis
to infer emotional state and confidence from a user’s face. The app presents a camera preview with
live overlay showing detected emotion and confidence level.

## Authors
- Jared Abels
- Syndeed Ayman
- Lydia Easter
- Joshua Leisman

## Tools Used:
- Android Studio IDE
  - Kotlin
  - Gradle
- ML IDE
  - Python

## Project Structure
    com.vibevision (./app/src/main/java)
    - feature.camera
        |- CameraOverlays.kt            // Overlay from ML model
        |- CameraPermissionsScreens.kt  // Screens shown if user denies camera permission
        |- CameraPreview.kt             // Camera Preview and capturing images 
        |- CameraScreen.kt              // Main UI handling showing preview page + overlays or permissions
        |- CameraViewModel.kt           // Frame processing
        |- ImageRepository.kt           // Save and Delete Images
        |- YuvToRgbConverter.kt         // YUV → Bitmap conversion
    - onboarding
        |- OnboardingScreen.kt          // Screens to show on first app startup
    - ui.theme
        |- Color.kt                     // Project colors
        |- Theme.kt                     // Matches light and dark mode to phone
        |- Type.kt                      // Fonts of app
    - MainActivity                      // Entry point to app. Decides to show onboarding or camera