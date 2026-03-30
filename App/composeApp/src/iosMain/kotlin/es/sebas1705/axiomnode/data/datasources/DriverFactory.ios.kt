package es.sebas1705.axiomnode.data.datasources

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import es.sebas1705.axiomnode.CountriesDatabase

class IosDriverFactory : DriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(CountriesDatabase.Schema, "countries.db")
    }
}