package com.example.appentregafacil.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appentregafacil.presentation.detail.DeliveryDetailScreen
import com.example.appentregafacil.presentation.form.DeliveryFormScreen
import com.example.appentregafacil.presentation.list.DeliveryListScreen

@Composable
fun EntregaFacilNavHost() {
    val navController = rememberNavController()
    val idArgument = listOf(navArgument("deliveryId") { type = NavType.LongType })
    NavHost(navController = navController, startDestination = "deliveries") {
        composable("deliveries") {
            DeliveryListScreen(onNew = { navController.navigate("delivery/new") { launchSingleTop = true } },
                onDetails = { navController.navigate("delivery/$it") { launchSingleTop = true } })
        }
        composable("delivery/new") {
            DeliveryFormScreen(onBack = { navController.popBackStack() }, onSaved = { id ->
                navController.navigate("delivery/$id") {
                    popUpTo("delivery/new") { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable("delivery/{deliveryId}", arguments = idArgument) {
            DeliveryDetailScreen(onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("delivery/$it/edit") { launchSingleTop = true } },
                onDeleted = { navController.popBackStack("deliveries", inclusive = false) })
        }
        composable("delivery/{deliveryId}/edit", arguments = idArgument) {
            DeliveryFormScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
    }
}
