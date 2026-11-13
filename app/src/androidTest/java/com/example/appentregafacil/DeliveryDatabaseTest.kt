package com.example.appentregafacil

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appentregafacil.data.local.*
import com.example.appentregafacil.data.repository.RoomDeliveryRepository
import com.example.appentregafacil.domain.model.DeliveryStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DeliveryDatabaseTest {
    private lateinit var database: DeliveryDatabase
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, DeliveryDatabase::class.java).build()
    }
    @After fun close() { database.close() }

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
