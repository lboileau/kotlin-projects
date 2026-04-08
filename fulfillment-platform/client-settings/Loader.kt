package com.acme.clientsettings

import com.acme.contextualsettings.SettingsContext

// ──────────────────────────────────────────────────────────────────
// Loader interface — the contract for all settings loaders.
//
// Each loader receives the request context and a map of already-loaded
// dependencies (from upstream loaders in the DAG).
// ──────────────────────────────────────────────────────────────────

/**
 * A settings loader that produces a result of type T.
 * Loaders are discovered via @SettingsLoader annotation and
 * executed in dependency order by the LoaderDag.
 */
interface Loader<T> {
    fun load(context: SettingsContext, dependencies: DependencyMap): T
}

/**
 * Type-safe container for dependency results from upstream loaders.
 * Populated by the DAG executor as it walks the dependency graph.
 */
class DependencyMap(
    private val results: Map<SettingsDomain, Any>,
) {
    /**
     * Retrieve a dependency result by domain.
     * Throws if the dependency was not declared in @SettingsLoader.dependsOn.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(domain: SettingsDomain): T {
        return results[domain] as? T
            ?: throw IllegalStateException(
                "Dependency $domain not found. Did you declare it in @SettingsLoader.dependsOn?"
            )
    }
}
