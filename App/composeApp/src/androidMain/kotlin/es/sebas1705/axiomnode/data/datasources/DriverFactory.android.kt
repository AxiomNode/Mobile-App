package es.sebas1705.axiomnode.data.datasources

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import es.sebas1705.axiomnode.CountriesDatabase

class AndroidDriverFactory(private val context: Context) : DriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(CountriesDatabase.Schema, context, "countries.db")
    }
}