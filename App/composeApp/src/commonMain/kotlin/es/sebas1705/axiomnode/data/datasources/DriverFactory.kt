package es.sebas1705.axiomnode.data.datasources

import app.cash.sqldelight.db.SqlDriver

interface DriverFactory {
    fun createDriver(): SqlDriver
}