package com.acme.clients.recipeclient.test

import com.acme.databases.camperdb.MigrationRunner

/**
 * Test database helper for `recipe-client` integration tests.
 *
 * Thin wrapper over the `camper-db` module's [MigrationRunner] so integration tests
 * run against the real migrations rather than a hand-maintained test schema.
 */
object RecipeTestDb {

    fun migrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.migrate(jdbcUrl, username, password)
    }

    fun cleanAndMigrate(jdbcUrl: String, username: String, password: String) {
        MigrationRunner.cleanAndMigrate(jdbcUrl, username, password)
    }
}
