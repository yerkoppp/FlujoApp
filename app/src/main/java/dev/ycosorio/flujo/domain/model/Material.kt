package dev.ycosorio.flujo.domain.model

import com.google.firebase.firestore.DocumentId

data class Material(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = ""
)