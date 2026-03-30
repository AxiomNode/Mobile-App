// Importamos el script que proporciona SQLDelight para manejar el worker
importScripts("https://cdnjs.cloudflare.com/ajax/libs/sql.js/1.8.0/sql-wasm.js");

// Esta es la parte mágica: SQLDelight tiene un paquete que ya sabe
// cómo escuchar los mensajes desde Kotlin y ejecutarlos en SQLite.
import { setupWorker } from "@cashapp/sqldelight-sqljs-worker";

setupWorker();