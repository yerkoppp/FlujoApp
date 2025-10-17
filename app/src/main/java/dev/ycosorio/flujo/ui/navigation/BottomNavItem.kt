package dev.ycosorio.flujo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Description // Icono para Documentos

import androidx.compose.material.icons.filled.Inventory // Icono para Inventario


sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : BottomNavItem("dashboard", Icons.Default.Home, "Inicio")
    object Documents : BottomNavItem("documents", Icons.Default.Description, "Documentos")
    object Inventory : BottomNavItem("inventory", Icons.Default.List, "Inventario")

}