package com.hexel.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.hexel.core.gesture.GestureClassifier
import com.hexel.core.gesture.GestureConfig
import com.hexel.launcher.ui.HexelRoot
import com.hexel.launcher.ui.theme.HexelTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HexelViewModel by viewModels()

    /**
     * Gesture thresholds. Tune the values in [GestureConfig]; debug builds log every decision
     * under the `HexelGesture` tag (`adb logcat -s HexelGesture`).
     */
    private val classifier = GestureClassifier(
        config = GestureConfig(debugLog = BuildConfig.DEBUG),
        logger = { Log.d("HexelGesture", it) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Paper is always light: dark system-bar icons regardless of the system dark mode.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)
        setContent {
            HexelTheme {
                HexelRoot(viewModel, classifier)
            }
        }
    }

    /** Pressing Home while Hexel is already in front closes the editor; the map stays where it is. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) viewModel.closeEditor()
    }
}
