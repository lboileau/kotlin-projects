package com.example.databases.helloworlddb

import org.flywaydb.core.Flyway

object MigrationRunner {

    fun migrate(jdbcUrl: String, username: String, password: String) {
        val migrationsPath = resolveMigrationsPath()
        Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations("filesystem:$migrationsPath")
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }

    fun cleanAndMigrate(jdbcUrl: String, username: String, password: String) {
        val migrationsPath = resolveMigrationsPath()
        Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations("filesystem:$migrationsPath")
            .cleanDisabled(false)
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    private fun resolveMigrationsPath(): String {
        val projectRoot = System.getProperty("project.root", "../..")
        return "$projectRoot/databases/hello-world-db/migrations"
    }
}
