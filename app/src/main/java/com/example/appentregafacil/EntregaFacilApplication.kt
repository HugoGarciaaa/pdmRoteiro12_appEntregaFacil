package com.example.appentregafacil

import android.app.Application
import com.example.appentregafacil.data.notification.AndroidDeliveryNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EntregaFacilApplication : Application() {
    @Inject lateinit var notifier: AndroidDeliveryNotifier

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
    }
}
