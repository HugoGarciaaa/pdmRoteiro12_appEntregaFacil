package com.example.appentregafacil

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appentregafacil.data.notification.AndroidDeliveryNotifier
import com.example.appentregafacil.domain.model.Delivery
import com.example.appentregafacil.domain.model.DeliveryStatus
import com.example.appentregafacil.domain.notification.NotificationResult
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DeliveryNotificationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun createsRealNotificationChannelOnAndroid8OrLater() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        AndroidDeliveryNotifier(context).createChannel()
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(AndroidDeliveryNotifier.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(context.getString(R.string.delivery_channel_name), channel.name.toString())
        assertEquals(context.getString(R.string.delivery_channel_description), channel.description)
    }

    @Test fun publishesNotificationWithIconTitleDescriptionAndTapAction() {
        val notifier = AndroidDeliveryNotifier(context)
        notifier.createChannel()
        // Não concede permissões silenciosamente nem altera as configurações do aparelho.
        assumeTrue("Ative as notificações do app antes deste teste.", notifier.areNotificationsEnabled())
        val id = Long.MAX_VALUE
        val delivery = Delivery(id, "Teste de notificação", "Livros", "01001000", "Praça da Sé", "100", "",
            "Sé", "São Paulo", "SP", DeliveryStatus.IN_TRANSIT, Instant.now(), Instant.now())
        val manager = context.getSystemService(NotificationManager::class.java)
        val tag = "delivery:$id"
        try {
            assertEquals(NotificationResult.POSTED, notifier.notifyStatusChanged(delivery))
            val deadline = System.currentTimeMillis() + 5_000
            var posted = manager.activeNotifications.find { it.tag == tag }
            while (posted == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(100)
                posted = manager.activeNotifications.find { it.tag == tag }
            }
            val notification = checkNotNull(posted).notification
            assertEquals(R.drawable.ic_delivery_notification, notification.smallIcon.resId)
            assertEquals(context.getString(R.string.delivery_notification_title, id.toString()), notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertEquals(context.getString(R.string.delivery_notification_description, delivery.recipientName,
                context.getString(R.string.delivery_status_in_transit)), notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertNotNull(notification.contentIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) assertEquals(AndroidDeliveryNotifier.CHANNEL_ID, notification.channelId)
        } finally { manager.cancel(tag, 0) }
    }
}
