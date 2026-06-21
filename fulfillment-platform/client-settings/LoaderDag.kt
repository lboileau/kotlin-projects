package com.acme.clientsettings

import com.acme.contextualsettings.SettingsContext

// ──────────────────────────────────────────────────────────────────
// DAG-Based Data Loading
//
// At startup, the client settings service:
//   1. Discovers all @SettingsDataLoader-annotated classes
//   2. Discovers the @ConfigManager for each domain
//   3. Builds a directed acyclic graph from dependency declarations
//   4. Validates no cycles exist
//
// At request time:
//   1. Client sends a domain key + context
//   2. The DAG executor identifies the ConfigManager and all transitive deps
//   3. Executes data loaders in topological order (multi-phase)
//   4. All data is fetched BEFORE processing
//   5. Passes all loaded data to the ConfigManager
//   6. Returns the final computed response
//
// Adding a new data loader:
//   1. Annotate a class with @SettingsDataLoader
//   2. Declare dependencies (other loader domain identifiers)
//   3. Extend ContextualSettingLoader (or implement Loader directly)
// The DAG takes care of ordering and dependency injection.
// ──────────────────────────────────────────────────────────────────

class LoaderDag(
    private val loaders: Map<SettingsDomain, LoaderNode>,
    private val configManagers: Map<SettingsDomain, ConfigHandler<*>>,
) {
    data class LoaderNode(
        val domain: SettingsDomain,
        val loader: Loader<*>,
        val dependsOn: List<SettingsDomain>,
    )

    companion object {
        /**
         * Build the DAG from discovered loaders and config managers.
         * Called once at startup — validates the graph is acyclic.
         */
        fun build(
            loaderInstances: List<Loader<*>>,
            configManagerInstances: List<ConfigHandler<*>>,
        ): LoaderDag {
            val nodes = mutableMapOf<SettingsDomain, LoaderNode>()

            for (loader in loaderInstances) {
                val annotation = loader::class.java.getAnnotation(SettingsDataLoader::class.java)
                    ?: throw IllegalStateException(
                        "${loader::class.simpleName} implements Loader but is missing @SettingsDataLoader"
                    )

                nodes[annotation.domain] = LoaderNode(
                    domain = annotation.domain,
                    loader = loader,
                    dependsOn = annotation.dependsOn.toList(),
                )
            }

            val managers = mutableMapOf<SettingsDomain, ConfigHandler<*>>()
            for (manager in configManagerInstances) {
                val annotation = manager::class.java.getAnnotation(ConfigManager::class.java)
                    ?: throw IllegalStateException(
                        "${manager::class.simpleName} implements ConfigHandler but is missing @ConfigManager"
                    )
                managers[annotation.domain] = manager
            }

            // Validate: no cycles, all dependencies exist
            validateDag(nodes)

            return LoaderDag(nodes, managers)
        }

        private fun validateDag(nodes: Map<SettingsDomain, LoaderNode>) {
            val visited = mutableSetOf<SettingsDomain>()
            val inProgress = mutableSetOf<SettingsDomain>()

            fun visit(domain: SettingsDomain) {
                if (domain in inProgress) {
                    throw IllegalStateException("Cycle detected in loader DAG involving $domain")
                }
                if (domain in visited) return

                inProgress.add(domain)
                val node = nodes[domain]
                    ?: throw IllegalStateException("Unknown dependency: $domain")
                for (dep in node.dependsOn) {
                    visit(dep)
                }
                inProgress.remove(domain)
                visited.add(domain)
            }

            nodes.keys.forEach { visit(it) }
        }
    }

    /**
     * Execute the full load cycle for a target domain:
     * 1. Load all data dependencies in topological order
     * 2. Pass all loaded data to the ConfigManager
     * 3. Return the computed response
     */
    fun <T> execute(targetDomain: SettingsDomain, context: SettingsContext): T {
        val results = mutableMapOf<SettingsDomain, Any>()
        val executed = mutableSetOf<SettingsDomain>()

        // Phase: load all data dependencies
        fun executeNode(domain: SettingsDomain) {
            if (domain in executed) return

            val node = loaders[domain]
                ?: throw IllegalStateException("No data loader registered for $domain")

            // Execute dependencies first (topological order)
            for (dep in node.dependsOn) {
                executeNode(dep)
            }

            // Execute this loader with its resolved dependencies
            val dataMap = DataResourceMap(
                results.filterKeys { it in node.dependsOn }
            )
            val result = node.loader.load(context, dataMap)
                ?: throw IllegalStateException("Data loader for $domain returned null")

            results[domain] = result
            executed.add(domain)
        }

        // Load all data
        loaders.keys.forEach { executeNode(it) }

        // Pass all data to the ConfigManager
        val manager = configManagers[targetDomain]
            ?: throw IllegalStateException("No ConfigManager registered for $targetDomain")

        @Suppress("UNCHECKED_CAST")
        return manager.handle(context, DataResourceMap(results)) as T
    }
}
