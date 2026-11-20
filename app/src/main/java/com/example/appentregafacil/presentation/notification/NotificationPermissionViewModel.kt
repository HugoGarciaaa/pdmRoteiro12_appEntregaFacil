package com.example.appentregafacil.presentation.notification

import androidx.lifecycle.ViewModel
import com.example.appentregafacil.domain.notification.DeliveryNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(private val notifier: DeliveryNotifier) : ViewModel() {
    private val mutableEnabled = MutableStateFlow(notifier.areNotificationsEnabled())
    val enabled = mutableEnabled.asStateFlow()

    fun refresh() { mutableEnabled.value = notifier.areNotificationsEnabled() }
}
