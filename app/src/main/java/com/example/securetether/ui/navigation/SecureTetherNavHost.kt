package com.example.securetether.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.securetether.ui.screens.AuthScreen
import com.example.securetether.ui.screens.SettingsScreen
import com.example.securetether.ui.screens.SplashScreen
import com.example.securetether.ui.screens.VaultScreen
import com.example.securetether.ui.viewmodel.AuthViewModel
import com.example.securetether.ui.viewmodel.SettingsViewModel
import com.example.securetether.ui.viewmodel.SplashViewModel
import com.example.securetether.ui.viewmodel.VaultViewModel

@Composable
fun SecureTetherNavHost(
    modifier: Modifier = Modifier,
    viewModel: NavViewModel = hiltViewModel()
) {
    val backStack = rememberNavBackStack(Route.Splash)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.lockEvent.collect {
            // Navigate back to Auth if we are in Vault or Settings
            if (backStack.contains(Route.Vault) || backStack.contains(Route.Settings)) {
                backStack.removeLastOrNull() // Remove current
                // If we were in Settings, we might need to remove Vault too to get back to Auth
                while (backStack.lastOrNull() !is Route.Auth && backStack.isNotEmpty()) {
                    backStack.removeLastOrNull()
                }
                if (backStack.isEmpty()) {
                    backStack.add(Route.Auth)
                }
            }
        }
    }
    
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is Route.Splash -> NavEntry(key) {
                    val viewModel: SplashViewModel = hiltViewModel()
                    SplashScreen(
                        viewModel = viewModel,
                        onNavigateNext = {
                            backStack.removeLastOrNull() // Remove Splash from stack
                            backStack.add(Route.Auth)
                        }
                    )
                }
                is Route.Auth -> NavEntry(key) {
                    val viewModel: AuthViewModel = hiltViewModel()
                    AuthScreen(
                        viewModel = viewModel,
                        onAuthenticated = {
                            backStack.removeLastOrNull() // Remove Auth from stack
                            backStack.add(Route.Vault)
                        }
                    )
                }
                is Route.Vault -> NavEntry(key) {
                    val viewModel: VaultViewModel = hiltViewModel()
                    VaultScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { backStack.add(Route.Settings) }
                    )
                }
                is Route.Settings -> NavEntry(key) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                else -> NavEntry(key) {
                    val viewModel: VaultViewModel = hiltViewModel()
                    VaultScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { backStack.add(Route.Settings) }
                    )
                }
            }
        }
    )
}
