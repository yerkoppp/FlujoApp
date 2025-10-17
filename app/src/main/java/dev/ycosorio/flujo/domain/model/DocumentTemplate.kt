package dev.ycosorio.flujo.domain.model

/**
 * Representa una plantilla de documento que puede ser asignada.
 *
 * @property id El ID único de la plantilla.
 * @property title El título del documento (ej: "Política de Seguridad").
 * @property fileUrl La URL donde está almacenado el archivo del documento (ej: en Firebase Storage).
 */
data class DocumentTemplate(
    val id: String,
    val title: String,
    val fileUrl: String
)