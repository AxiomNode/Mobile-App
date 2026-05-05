package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_languages")
data class CatalogLanguageEntity(
    @PrimaryKey
    val code: String,
    val name: String,
)
