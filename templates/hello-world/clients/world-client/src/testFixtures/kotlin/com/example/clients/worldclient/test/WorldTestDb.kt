package com.example.clients.worldclient.test

import com.example.databases.helloworlddb.MigrationRunner

object WorldTestDb {

    fun migrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.migrate(jdbcUrl, username, password)
    }

    fun cleanAndMigrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.cleanAndMigrate(jdbcUrl, username, password)
    }
}
