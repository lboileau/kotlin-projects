package com.acme.clientsettings

import com.acme.contextualsettings.SettingsContext

// ──────────────────────────────────────────────────────────────────
// DAG-Based Loader Executor
//
// At startup, the client settings service:
//   1. Discovers all @SettingsLoader-annotated classes
//   2. Builds a directed acyclic graph from their dependency declarations
//   3. Validates no cycles exist
//
// At request time:
//   1. Client sends a domain key + context
//   2. The DAG executor identifies the target loader and all transitive deps
//   3. Executes loaders in topological order
//   4. Each loader receives results from its upstream dependencies
//   5. Returns the final result
//
// This means adding a new settings loader is just:
//   1. Annotate a class with @SettingsLoader
//   2. Declare dependencies
//   3. Implement the load() method
// The DAG takes care of ordering and dependency injection.
// ──────────────────────────────────────────────────────────────────

class LoaderDag(
    private val loaders: Map<SettingsDomain, LoaderNode>,
) {
    data class LoaderNode(
        val domain: SettingsDomain,
        val loader: Loader<*>,
        val dependsOn: List<SettingsDomain>,
    )

    companion object {
        /**
         * Build the DAG from a list of discovered loaders.
         * Called once at startup — validates the graph is acyclic.
         */
        fun build(loaderInstances: List<Loader<*>>): LoaderDag {
            val nodes = mutableMapOf<SettingsDomain, LoaderNode>()

            for (loader in loaderInstances) {
                val annotation = loader::class.java.getAnnotation(SettingsLoader::class.java)
                    ?: throw IllegalStateException(
                        "${loader::class.simpleName} implements Loader but is missing @SettingsLoader"
                    )

                nodes[annotation.domain] = LoaderNode(
                    domain = annotation.domain,
                    loader = loader,
                    dependsOn = annotation.dependsOn.toList(),
                )
            }

            // Validate: no cycles, all dependencies exist
            validateDag(nodes)

            return LoaderDag(nodes)
        }

        private fun validateDag(nodes: Map<SettingsDomain, LoaderNode>) {
            // Topological sort with cycle detection
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
     * Execute the loader DAG for a target domain.
     * Loads all transitive dependencies first, then the target.
     */
    fun execute(targetDomain: SettingsDomain, context: SettingsContext): Any {
        val results = mutableMapOf<SettingsDomain, Any>()
        val executed = mutableSetOf<SettingsDomain>()

        fun executeNode(domain: SettingsDomain) {
            if (domain in executed) return

            val node = loaders[domain]
                ?: throw IllegalStateException("No loader registered for $domain")

            // Execute dependencies first (topological order)
            for (dep in node.dependsOn) {
                executeNode(dep)
            }

            // Execute this loader with its resolved dependencies
            val dependencyMap = DependencyMap(
                results.filterKeys { it in node.dependsOn }
            )
            val result = node.loader.load(context, dependencyMap)
                ?: throw IllegalStateException("Loader for $domain returned null")

            results[domain] = result
            executed.add(domain)
        }

        executeNode(targetDomain)
        return results[targetDomain]!!
    }
}
