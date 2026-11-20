package com.example.appentregafacil

import com.example.appentregafacil.domain.model.Delivery
import com.example.appentregafacil.domain.notification.DeliveryNotifier
import com.example.appentregafacil.domain.notification.NotificationResult

internal class RecordingDeliveryNotifier : DeliveryNotifier {
    val deliveries = mutableListOf<Delivery>()
    var enabled = true
    var result = NotificationResult.POSTED
    override fun areNotificationsEnabled() = enabled
    override fun notifyStatusChanged(delivery: Delivery): NotificationResult {
        deliveries += delivery
        return result
    }
}
