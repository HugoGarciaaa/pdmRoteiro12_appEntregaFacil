package com.example.appentregafacil

import com.example.appentregafacil.domain.model.DeliveryNotFoundException
import com.example.appentregafacil.domain.model.DeliveryStatus
import com.example.appentregafacil.domain.notification.DeliveryNotifier
import com.example.appentregafacil.domain.notification.NotificationResult
import com.example.appentregafacil.domain.model.Delivery
import com.example.appentregafacil.domain.repository.DeliveryRepository
import com.example.appentregafacil.domain.usecase.ChangeDeliveryStatus
import com.example.appentregafacil.domain.usecase.DeleteDelivery
import com.example.appentregafacil.domain.usecase.SaveDelivery
import com.example.appentregafacil.presentation.notification.NotificationPermissionAction
import com.example.appentregafacil.presentation.notification.NotificationPermissionViewModel
import com.example.appentregafacil.presentation.notification.notificationPermissionAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DeliveryNotificationsTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneOffset.UTC)

    @Test fun notifiesWithPersistedStatusAndIdentity() = runTest {
        val repository = FakeDeliveryRepository()
        val id = SaveDelivery(repository, clock)(validDraft(), "01001000")
        val notifier = object : DeliveryNotifier {
            override fun areNotificationsEnabled() = true
            override fun notifyStatusChanged(delivery: Delivery): NotificationResult {
                // O banco já deve conter o novo status quando o aviso for solicitado.
                assertEquals(DeliveryStatus.IN_TRANSIT, repository.rows.value.single().status)
                assertEquals(id, delivery.id)
                assertEquals("Ana Souza", delivery.recipientName)
                assertEquals(DeliveryStatus.IN_TRANSIT, delivery.status)
                assertEquals(clock.instant(), delivery.updatedAt)
                return NotificationResult.POSTED
            }
        }
        assertEquals(NotificationResult.POSTED, ChangeDeliveryStatus(repository, clock, notifier)(id, DeliveryStatus.IN_TRANSIT))
    }

    @Test fun sameStatusDoesNotSendDuplicateNotification() = runTest {
        val repository = FakeDeliveryRepository()
        val id = SaveDelivery(repository, clock)(validDraft(), "01001000")
        val notifier = RecordingDeliveryNotifier()
        assertNull(ChangeDeliveryStatus(repository, clock, notifier)(id, DeliveryStatus.PENDING))
        assertTrue(notifier.deliveries.isEmpty())
    }

    @Test fun deniedPermissionDoesNotPreventDatabaseUpdate() = runTest {
        val repository = FakeDeliveryRepository()
        val id = SaveDelivery(repository, clock)(validDraft(), "01001000")
        val notifier = RecordingDeliveryNotifier().apply { enabled = false; result = NotificationResult.DISABLED }
        assertEquals(NotificationResult.DISABLED, ChangeDeliveryStatus(repository, clock, notifier)(id, DeliveryStatus.DELIVERED))
        assertEquals(DeliveryStatus.DELIVERED, repository.getById(id)!!.status)
    }

    @Test fun notificationFailureDoesNotUndoDatabaseUpdate() = runTest {
        val repository = FakeDeliveryRepository()
        val id = SaveDelivery(repository, clock)(validDraft(), "01001000")
        val notifier = RecordingDeliveryNotifier().apply { result = NotificationResult.FAILED }
        assertEquals(NotificationResult.FAILED, ChangeDeliveryStatus(repository, clock, notifier)(id, DeliveryStatus.DELIVERED))
        assertEquals(DeliveryStatus.DELIVERED, repository.getById(id)!!.status)
    }

    @Test fun databaseFailureNeverSendsNotification() = runTest {
        val repository = FakeDeliveryRepository()
        val id = SaveDelivery(repository, clock)(validDraft(), "01001000")
        val failing = object : DeliveryRepository by repository {
            override suspend fun changeStatus(id: Long, status: DeliveryStatus, at: Instant) { throw IOException("Falha de gravação") }
        }
        val notifier = RecordingDeliveryNotifier()
        try { ChangeDeliveryStatus(failing, clock, notifier)(id, DeliveryStatus.DELIVERED); fail() } catch (_: IOException) { }
        assertTrue(notifier.deliveries.isEmpty())
        assertEquals(DeliveryStatus.PENDING, repository.getById(id)!!.status)
    }

    @Test fun deletedDeliveryCannotChangeStatusOrSendNotification() = runTest {
        val repository = FakeDeliveryRepository()
        val id = SaveDelivery(repository, clock)(validDraft(), "01001000")
        DeleteDelivery(repository, clock)(id)
        val notifier = RecordingDeliveryNotifier()
        try { ChangeDeliveryStatus(repository, clock, notifier)(id, DeliveryStatus.DELIVERED); fail() } catch (_: DeliveryNotFoundException) { }
        assertTrue(notifier.deliveries.isEmpty())
        assertNotNull(repository.rows.value.single().deletedAt)
    }

    @Test fun requestsRuntimePermissionOnlyOnAndroid13OrLaterWhenMissing() {
        assertEquals(NotificationPermissionAction.REQUEST, notificationPermissionAction(33, granted = false, denied = false))
        assertEquals(NotificationPermissionAction.REQUEST, notificationPermissionAction(37, granted = false, denied = false))
        listOf(24, 25, 26, 32).forEach { sdk ->
            assertEquals(NotificationPermissionAction.OPEN_SETTINGS, notificationPermissionAction(sdk, granted = false, denied = false))
        }
    }

    @Test fun grantedOrDeniedPermissionLeadsToSettingsInsteadOfAnotherPrompt() {
        assertEquals(NotificationPermissionAction.OPEN_SETTINGS, notificationPermissionAction(33, granted = true, denied = false))
        assertEquals(NotificationPermissionAction.OPEN_SETTINGS, notificationPermissionAction(33, granted = false, denied = true))
    }

    @Test fun refreshesAvailabilityAfterReturningFromAndroidSettings() {
        val notifier = RecordingDeliveryNotifier().apply { enabled = false }
        val viewModel = NotificationPermissionViewModel(notifier)
        assertFalse(viewModel.enabled.value)
        notifier.enabled = true
        viewModel.refresh()
        assertTrue(viewModel.enabled.value)
        notifier.enabled = false
        viewModel.refresh()
        assertFalse(viewModel.enabled.value)
    }
}
