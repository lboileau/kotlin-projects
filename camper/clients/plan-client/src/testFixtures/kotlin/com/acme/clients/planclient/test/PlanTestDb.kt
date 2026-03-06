package com.acme.clients.planclient.test

import com.acme.databases.camperdb.MigrationRunner

object PlanTestDb {

    fun migrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.migrate(jdbcUrl, username, password)
    }

    fun cleanAndMigrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.cleanAndMigrate(jdbcUrl, username, password)
    }
}
