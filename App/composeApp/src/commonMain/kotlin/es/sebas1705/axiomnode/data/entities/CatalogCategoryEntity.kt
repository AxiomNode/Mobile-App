package es.sebas1705.axiomnode.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_categories")
data class CatalogCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
)
