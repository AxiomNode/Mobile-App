package es.sebas1705.axiomnode.data.datasources

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import es.sebas1705.axiomnode.CountriesDatabase

class JvmDriverFactory : DriverFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY) // O "jdbc:sqlite:countries.db"
        CountriesDatabase.Schema.create(driver)
        return driver
    }
}