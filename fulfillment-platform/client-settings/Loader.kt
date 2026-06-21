package com.acme.clientsettings

import com.acme.contextualsettings.SettingsContext

// ──────────────────────────────────────────────────────────────────
// Loader interfaces — contracts for ConfigManagers and DataLoaders.
// ──────────────────────────────────────────────────────────────────

/**
 * A settings data loader that produces a result of type T.
 * Loaders are discovered via @SettingsDataLoader annotation and
 * executed in dependency order by the DAG.
 */
interface Loader<T> {
    fun load(context: SettingsContext, dependencies: DataResourceMap): T
}

/**
 * A config manager that builds the final computed response from
 * all pre-loaded data resources.
 */
interface ConfigHandler<T> {
    fun handle(context: SettingsContext, data: DataResourceMap): T
}

/**
 * Type-safe container for data resources loaded by the DAG.
 * Populated as the DAG executor walks the dependency graph.
 */
class DataResourceMap(
    private val results: Map<SettingsDomain, Any>,
) {
    /**
     * Retrieve a data resource by domain.
     * Throws if the dependency was not declared in @SettingsDataLoader.dependsOn.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(domain: SettingsDomain): T {
        return results[domain] as? T
            ?: throw IllegalStateException(
                "Data resource $domain not found. Did you declare it in dependsOn?"
            )
    }
}
