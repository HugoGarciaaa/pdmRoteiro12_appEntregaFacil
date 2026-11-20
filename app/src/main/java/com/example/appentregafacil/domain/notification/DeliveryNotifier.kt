package com.example.appentregafacil.domain.notification

import com.example.appentregafacil.domain.model.Delivery

enum class NotificationResult { POSTED, DISABLED, FAILED }

/** Contrato independente do Android. O aviso é opcional e não desfaz uma gravação. */
interface DeliveryNotifier {
    fun areNotificationsEnabled(): Boolean
    fun notifyStatusChanged(delivery: Delivery): NotificationResult
}
