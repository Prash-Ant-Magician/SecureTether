package com.example.securetether.domain.security

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _lockEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lockEvent: SharedFlow<Unit> = _lockEvent.asSharedFlow()

    fun lock() {
        _lockEvent.tryEmit(Unit)
    }
}
