package com.example.appentregafacil.di

import android.content.Context
import androidx.room.Room
import com.example.appentregafacil.BuildConfig
import com.example.appentregafacil.data.local.DeliveryDatabase
import com.example.appentregafacil.data.notification.AndroidDeliveryNotifier
import com.example.appentregafacil.domain.notification.DeliveryNotifier
import com.example.appentregafacil.data.remote.BrasilApiService
import com.example.appentregafacil.data.repository.BrasilApiAddressRepository
import com.example.appentregafacil.data.repository.RoomDeliveryRepository
import com.example.appentregafacil.domain.repository.AddressRepository
import com.example.appentregafacil.domain.repository.DeliveryRepository
import com.example.appentregafacil.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): DeliveryDatabase =
        Room.databaseBuilder(context, DeliveryDatabase::class.java, "entregafacil.db").build()
    @Provides fun dao(database: DeliveryDatabase) = database.deliveryDao()

    @Provides @Singleton fun httpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().apply {
                // BASIC registra método/URL/status, sem corpos com dados de endereço.
                level = HttpLoggingInterceptor.Level.BASIC
            })
        }.build()

    @Provides @Singleton fun api(client: OkHttpClient): BrasilApiService = Retrofit.Builder()
        .baseUrl("https://brasilapi.com.br/")
        .client(client).addConverterFactory(GsonConverterFactory.create())
        .build().create(BrasilApiService::class.java)

    @Provides fun deliveryRepository(impl: RoomDeliveryRepository): DeliveryRepository = impl
    @Provides fun addressRepository(impl: BrasilApiAddressRepository): AddressRepository = impl
    @Provides fun deliveryNotifier(impl: AndroidDeliveryNotifier): DeliveryNotifier = impl
    @Provides fun clock(): Clock = Clock.systemUTC()
    @Provides fun observe(repository: DeliveryRepository) = ObserveDeliveries(repository)
    @Provides fun get(repository: DeliveryRepository) = GetDelivery(repository)
    @Provides fun lookup(repository: AddressRepository) = LookupPostalCode(repository)
    @Provides fun save(repository: DeliveryRepository, clock: Clock) = SaveDelivery(repository, clock)
    @Provides fun changeStatus(repository: DeliveryRepository, clock: Clock, notifier: DeliveryNotifier) =
        ChangeDeliveryStatus(repository, clock, notifier)
    @Provides fun delete(repository: DeliveryRepository, clock: Clock) = DeleteDelivery(repository, clock)
}
