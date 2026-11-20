package com.example.appentregafacil.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.appentregafacil.MainActivity
import com.example.appentregafacil.R
import com.example.appentregafacil.domain.model.Delivery
import com.example.appentregafacil.domain.model.DeliveryStatus
import com.example.appentregafacil.domain.notification.DeliveryNotifier
import com.example.appentregafacil.domain.notification.NotificationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidDeliveryNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeliveryNotifier {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.delivery_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.delivery_channel_description)
            }
            // Recriar um canal existente preserva as preferências escolhidas pelo usuário.
            manager.createNotificationChannel(channel)
        }
    }

    override fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (!manager.areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    override fun notifyStatusChanged(delivery: Delivery): NotificationResult {
        if (delivery.deletedAt != null) return NotificationResult.DISABLED
        return try {
            createChannel()
            // Verifica também imediatamente antes do envio: a permissão pode ser revogada.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return NotificationResult.DISABLED
            }
            if (!areNotificationsEnabled()) return NotificationResult.DISABLED
            val status = context.getString(when (delivery.status) {
                DeliveryStatus.PENDING -> R.string.delivery_status_pending
                DeliveryStatus.IN_TRANSIT -> R.string.delivery_status_in_transit
                DeliveryStatus.DELIVERED -> R.string.delivery_status_delivered
            })
            val description = context.getString(R.string.delivery_notification_description,
                delivery.recipientName, status)
            val openApp = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentIntent = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_delivery_notification)
                .setContentTitle(context.getString(R.string.delivery_notification_title, delivery.id.toString()))
                .setContentText(description)
                .setStyle(NotificationCompat.BigTextStyle().bigText(description))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
            // A tag usa o Long completo. Atualizações da mesma entrega substituem seu aviso.
            manager.notify("delivery:${delivery.id}", 0, notification)
            NotificationResult.POSTED
        } catch (_: SecurityException) {
            NotificationResult.DISABLED
        } catch (error: RuntimeException) {
            Log.w("DeliveryNotifier", "Não foi possível publicar o aviso de status.", error)
            NotificationResult.FAILED
        }
    }

    companion object {
        const val CHANNEL_ID = "delivery_status_updates"
    }
}
