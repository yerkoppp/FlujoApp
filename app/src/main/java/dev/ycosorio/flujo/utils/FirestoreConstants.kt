package dev.ycosorio.flujo.utils

object FirestoreConstants {
    // Colecciones
    const val USERS_COLLECTION = "users"
    const val INVENTORY_COLLECTION = "inventory_items"
    const val MATERIAL_REQUESTS_COLLECTION = "material_requests"
    const val DOCUMENT_TEMPLATES_COLLECTION = "document_templates"
    const val DOCUMENT_ASSIGNMENTS_COLLECTION = "document_assignments"
    const val VEHICLES_COLLECTION = "vehicles"
    const val WAREHOUSES_COLLECTION = "warehouses"
    const val MATERIALS_COLLECTION = "materials"
    const val STOCK_SUBCOLLECTION = "stock"


    // Campos comunes
    const val FIELD_ID = "id"
    const val FIELD_ROLE = "role"
    const val FIELD_STATUS = "status"
    const val FIELD_WORKER_ID = "workerId"
    const val FIELD_POSITION = "position"
    const val FIELD_REQUEST_DATE = "requestDate"
    const val FIELD_ASSIGNED_DATE = "assignedDate"

    // Storage paths
    const val SIGNATURES_PATH = "signatures"
}