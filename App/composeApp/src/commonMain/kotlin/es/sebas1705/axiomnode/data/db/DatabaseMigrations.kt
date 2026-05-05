package es.sebas1705.axiomnode.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {
    /**
     * v4 -> v5:
     * - split legacy `games` cache table into mode-specific tables
     * - migrate existing rows preserving ids/content/timestamps
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `quiz_games_cache` (
                    `id` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `categoryName` TEXT NOT NULL,
                    `language` TEXT NOT NULL,
                    `questionsJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `wordpass_games_cache` (
                    `id` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `categoryName` TEXT NOT NULL,
                    `language` TEXT NOT NULL,
                    `questionsJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            connection.execSQL(
                """
                INSERT OR REPLACE INTO `quiz_games_cache` (
                    `id`, `categoryId`, `categoryName`, `language`, `questionsJson`, `createdAt`
                )
                SELECT
                    `id`, `categoryId`, `categoryName`, `language`, `questionsJson`, `createdAt`
                FROM `games`
                WHERE UPPER(`gameType`) = 'QUIZ'
                """.trimIndent(),
            )

            connection.execSQL(
                """
                INSERT OR REPLACE INTO `wordpass_games_cache` (
                    `id`, `categoryId`, `categoryName`, `language`, `questionsJson`, `createdAt`
                )
                SELECT
                    `id`, `categoryId`, `categoryName`, `language`, `questionsJson`, `createdAt`
                FROM `games`
                WHERE UPPER(`gameType`) IN ('WORDPASS', 'WORD_PASS', 'WORD-PASS')
                """.trimIndent(),
            )
        }
    }

    /**
     * v5 -> v6:
     * - remove FK from played_games that depended on legacy `games`
     * - drop legacy `games` table
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `played_games_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `gameId` TEXT NOT NULL,
                    `playedAt` INTEGER NOT NULL,
                    `outcome` TEXT NOT NULL,
                    `score` INTEGER NOT NULL,
                    `playerFirebaseUid` TEXT
                )
                """.trimIndent(),
            )

            connection.execSQL(
                """
                INSERT INTO `played_games_new` (
                    `id`, `gameId`, `playedAt`, `outcome`, `score`, `playerFirebaseUid`
                )
                SELECT `id`, `gameId`, `playedAt`, `outcome`, `score`, `playerFirebaseUid`
                FROM `played_games`
                """.trimIndent(),
            )

            connection.execSQL("DROP TABLE IF EXISTS `played_games`")
            connection.execSQL("ALTER TABLE `played_games_new` RENAME TO `played_games`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_played_games_gameId` ON `played_games`(`gameId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_played_games_playedAt` ON `played_games`(`playedAt`)")

            connection.execSQL("DROP TABLE IF EXISTS `games`")
        }
    }
}
