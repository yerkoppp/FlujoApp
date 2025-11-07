package dev.ycosorio.flujo.domain.model

import com.google.firebase.firestore.DocumentId

data class Warehouse(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val type: WarehouseType = WarehouseType.MOBILE
)