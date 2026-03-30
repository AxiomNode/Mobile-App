package es.sebas1705.axiomnode.data.datasources

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import es.sebas1705.axiomnode.CountriesDatabase
import org.w3c.dom.Worker

class JsDriverFactory : DriverFactory {
    override fun createDriver(): SqlDriver {
        val driver = WebWorkerDriver(
            Worker("worker.js") // Debes crear este pequeño script JS
        )
        CountriesDatabase.Schema.create(driver)
        return driver
    }
}