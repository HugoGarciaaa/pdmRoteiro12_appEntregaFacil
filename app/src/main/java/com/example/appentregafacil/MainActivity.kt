package com.example.appentregafacil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appentregafacil.presentation.navigation.EntregaFacilNavHost
import com.example.appentregafacil.ui.theme.AppEntregaFacilTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppEntregaFacilTheme { EntregaFacilNavHost() } }
    }
}
