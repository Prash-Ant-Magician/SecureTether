package com.example.securetether.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.securetether.domain.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    val lockEvent: SharedFlow<Unit> = sessionManager.lockEvent
}
