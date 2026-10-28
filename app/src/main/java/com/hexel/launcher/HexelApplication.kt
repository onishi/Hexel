package com.hexel.launcher

import android.app.Application
import com.hexel.launcher.apps.AppLauncher
import com.hexel.launcher.apps.AppRepository
import com.hexel.launcher.data.HexelStore

/** Holds the app-wide singletons (manual DI; the MVP does not need a DI framework). */
class HexelApplication : Application() {
    val appRepository by lazy { AppRepository(this) }
    val appLauncher by lazy { AppLauncher(this) }
    val store by lazy { HexelStore(this) }
}
