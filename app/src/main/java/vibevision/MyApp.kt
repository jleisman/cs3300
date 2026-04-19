package com.vibevision

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


/* Joshua Leisman
 * Application class for the VibeVision app.
 *
 * This class is the entry point for application-wide initialization.
 * It is created before any Activity, Service, or BroadcastReceiver.
 *
 * Annotating this class with @HiltAndroidApp triggers Hilt’s
 * code generation and sets up the dependency injection container
 * for the entire app.
 *
 * All Hilt-injected components (ViewModels, Activities, etc.)
 * rely on this Application class being present.
 */
@HiltAndroidApp
class MyApp : Application()