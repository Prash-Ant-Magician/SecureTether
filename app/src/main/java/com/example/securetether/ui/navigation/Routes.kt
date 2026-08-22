package com.example.securetether.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Splash : Route

    @Serializable
    data object Auth : Route

    @Serializable
    data object Vault : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data class Transfer(val fileIds: List<String> = emptyList()) : Route
}
