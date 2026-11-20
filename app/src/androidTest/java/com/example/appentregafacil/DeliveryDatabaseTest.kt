package com.example.appentregafacil

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appentregafacil.data.local.*
import com.example.appentregafacil.data.repository.RoomDeliveryRepository
import com.example.appentregafacil.domain.model.DeliveryStatus
import com.example.appentregafacil.domain.model.DeliveryDraft
import com.example.appentregafacil.domain.usecase.SaveDelivery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DeliveryDatabaseTest {
    private lateinit var database: DeliveryDatabase
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, DeliveryDatabase::class.java).build()
    }
    @After fun close() { database.close() }

    @Test fun persistsRegistrationAndEditsAfterClosingAndReopeningDatabase() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "delivery-persistence-test-${UUID.randomUUID()}.db"
        val now = Instant.parse("2026-11-19T15:00:00Z")
        val draft = DeliveryDraft(recipientName = "Teste de persistência", packageDescription = "Livros",
            postalCode = "01001000", street = "Praça da Sé", number = "100", complement = "Sala 2",
            neighborhood = "Sé", city = "São Paulo", state = "SP")
        fun reopen() {
            database.close()
            database = Room.databaseBuilder(context, DeliveryDatabase::class.java, name).build()
        }
        try {
            reopen()
            val id = SaveDelivery(RoomDeliveryRepository(database.deliveryDao()), Clock.fixed(now, ZoneOffset.UTC))(draft, "01001000")
            reopen()
            val repository = RoomDeliveryRepository(database.deliveryDao())
            assertEquals("100", repository.getById(id)!!.number)
            assertEquals("Sala 2", repository.getById(id)!!.complement)
            val updatedId = SaveDelivery(repository, Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC))(
                draft.copy(number = "200", complement = "Sala 3"), "01001000", id)
            assertEquals(id, updatedId)
            reopen()
            val restored = database.deliveryDao().getById(id)!!
            assertEquals("200", restored.number)
            assertEquals("Sala 3", restored.complement)
            assertEquals(now, restored.createdAt)
            assertEquals(now.plusSeconds(60), restored.updatedAt)
            assertEquals(1, database.deliveryDao().observeActive().first().size)
        } finally {
            database.close()
            context.deleteDatabase(name) // Somente o banco de teste, nunca entregafacil.db.
        }
    }

    @Test fun softDeleteHidesRowAndPreservesItPhysically() = runTest {
        val dao = database.deliveryDao()
        val now = Instant.ofEpochMilli(1_800_000_000_000)
        val id = dao.insert(DeliveryEntity(recipientName = "Ana", packageDescription = "Livros", postalCode = "01001000",
            street = "Praça da Sé", number = "100", complement = "", neighborhood = "Sé", city = "São Paulo", state = "SP",
            status = DeliveryStatus.PENDING, createdAt = now, updatedAt = now))
        val repository = RoomDeliveryRepository(dao)
        val stale = repository.getById(id)!!
        repository.changeStatus(id, DeliveryStatus.DELIVERED, now.plusSeconds(1))
        assertEquals(DeliveryStatus.DELIVERED, dao.getById(id)!!.status)
        repository.softDelete(id, now.plusSeconds(2))
        assertNull(dao.getById(id))
        assertNull(dao.observeById(id).first())
        assertTrue(dao.observeActive().first().isEmpty())
        database.openHelper.readableDatabase.query("SELECT deletedAt, updatedAt, createdAt FROM deliveries WHERE id = $id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(now.plusSeconds(2).toEpochMilli(), cursor.getLong(0))
            assertEquals(cursor.getLong(0), cursor.getLong(1))
            assertEquals(now.toEpochMilli(), cursor.getLong(2))
        }
        // Uma edição com dados anteriores à exclusão não deve ressuscitar o registro.
        try { repository.update(stale); fail() } catch (_: com.example.appentregafacil.domain.model.DeliveryNotFoundException) { }
    }
}
