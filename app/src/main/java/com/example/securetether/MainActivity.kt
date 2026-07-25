package com.example.securetether

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.securetether.domain.repository.SettingsRepository
import com.example.securetether.domain.security.SessionManager
import com.example.securetether.ui.navigation.SecureTetherNavHost
import com.example.securetether.ui.theme.SecureTetherTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var sessionManager: SessionManager

    private var backgroundTimestamp: Long = 0

    override fun onPause() {
        super.onPause()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (backgroundTimestamp != 0L) {
            lifecycleScope.launch {
                val timer = settingsRepository.autoLockTimer.first()
                if (timer > 0 && System.currentTimeMillis() - backgroundTimestamp > timer) {
                    sessionManager.lock()
                }
                backgroundTimestamp = 0
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            settingsRepository.stealthModeEnabled.collectLatest { enabled ->
                if (enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = 0)
            
            val isDarkTheme = when (themeMode) {
                1 -> false // Light
                2 -> true // Dark
                else -> isSystemInDarkTheme() // System
            }

            SecureTetherTheme(darkTheme = isDarkTheme) {
                SecureTetherNavHost()
            }
        }
    }
}

@Composable
fun TetheringDashboardPlaceholder(modifier: Modifier = Modifier) {
    Text(
        text = "SecureTether Dashboard coming soon!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    SecureTetherTheme {
        TetheringDashboardPlaceholder()
    }
}
