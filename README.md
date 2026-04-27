# CS3300 Project - VibeVision
Team "CS is just our hobby"

Intro to Software Engineering Group Project - Android App interface for ML mood model

## Project Statement
This project an Android application that performs real‑time camera capture and on‑device image analysis
to infer emotional state and confidence from a user’s face. The app presents a camera preview with
live overlay showing detected emotion and confidence level.


## Tools Used:
- Android Studio IDE
  - Kotlin
  - Gradle
- Jupyter Notebook (Google Colab Hardware)
  - Python

## How to Run Application
I suggest running on a computer unless you have an Android phone and have installed APKs on that phone before.

Lab computers with Android Studio Installed can be found in EPC/COH Open Labs and COLU 209 (Macs Only)
### On Computer
1. Download Android Studio
   - Go to: https://developer.android.com/studio
   - Click Download Android Studio.
     
2. Install Android Studio
   - **Windows** 
     - Double‑click the downloaded .exe file. 
     - Follow the installer prompts. 
     - Keep default options checked (Android SDK, Android Virtual Device). 
     - Complete the installation. 
   - **macOS** 
     - Open the downloaded .dmg file. 
     - Drag Android Studio into the Applications folder. 
     - Launch Android Studio from Applications.

3. Launch Android Studio and wait for the Welcome to Android Studio screen to appear.
   - This is the screen with options like New Project, Open, and More Actions.
   - No project needs to be opened for this process.

4. Open the Device Manager
   - On the welcome screen, click More Actions.
   - Select Device Manager from the menu. 
     - The Device Manager window will open on the right side.

5. Create an Emulator (If One Does Not Exist)
   - In Device Manager, click Create Device.
   - Choose a phone model (for example, Pixel 7 - Like Joshua's phone).
   - Click Next.
   - Select an Android system image.
   - Download one if required.
   - Click Next and Finish.
 
6. Start the Emulator
   - In Device Manager, find the emulator you want to use.
   - Click the Play (Run) button next to the device.
   - Wait until:
     - The emulator boots fully
     - The Android home screen is visible 
     - The device is unlocked and idle

7. Drag and Drop the APK
   - Locate the release .apk file on your computer.
   - Click and drag the APK file.
   - Drop it directly onto the running emulator window. 
   - Android will automatically begin installing the app.

8. Open the App
   - Wait for the “App installed” message to appear.
   - On the emulator:
     - Open the app drawer
     - Find the installed app
     - Tap the app icon to launch it

**The release APK is now installed and running on the emulator.**

### On Android Phone
1. Download APK from Releases
    - Can be found on [GitHub Repo](https://github.com/jleisman/cs3300)

2. Enable phone for APK content
   - **Android 8.0 (Oreo) and newer**
     - Open Settings
     - Go to Security or Privacy
     - Open Install unknown apps
     - Select the app you will use to open the APK (Chrome, Files, etc.)
     - Enable Allow from this source
   - **Android 7.x and older**
     - Open Settings
     - Go to Security
     - Enable Unknown sources
     - Acknowledge the warning dialog
       Transfer the APK to the Phone (if needed)

3. Download APK to phone
   - Download the APK directly on the phone OR transfer the APK via USB cable OR transfer via cloud storage (Drive, Dropbox, etc.)

4. Install the APK
   - Open a File Manager app
   - Navigate to the folder containing the APK
   - Tap the APK file
   - Review requested permissions
   - Tap Install
   - Wait for the installation to complete

5. Verify Installation
   - Confirm the “App installed” message appears
   - Tap Open or find the app in the app drawer
   - Launch the app to ensure it starts correctly

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

    assets (./app/src/assets)
    - CS3300-ML-Model.tflite            // ML model compiled in tflite

    Python code (./)
    - ML model.py                       // Configures environment and model. Trains, evelautates and exports model
    - Legacy ML code.py                 // Original ML model config. and training code.