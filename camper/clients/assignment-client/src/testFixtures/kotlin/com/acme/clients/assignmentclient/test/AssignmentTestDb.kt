package com.acme.clients.assignmentclient.test

import com.acme.databases.camperdb.MigrationRunner

object AssignmentTestDb {

    fun migrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.migrate(jdbcUrl, username, password)
    }

    fun cleanAndMigrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.cleanAndMigrate(jdbcUrl, username, password)
    }
}
