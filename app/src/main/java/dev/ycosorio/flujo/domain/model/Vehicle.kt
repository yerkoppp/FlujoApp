package dev.ycosorio.flujo.domain.model
import com.google.firebase.firestore.DocumentId

data class Vehicle(
    @DocumentId
    val id: String = "",
    val plate: String = "",
    val description: String = "",
    val userIds: List<String> = emptyList(), // Lista de UIDs de los usuarios asignados
    val maxUsers: Int = 6
)