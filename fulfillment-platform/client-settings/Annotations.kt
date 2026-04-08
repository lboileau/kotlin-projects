package com.acme.clientsettings

import com.acme.contextualsettings.ContextualSettingDefinition

// ──────────────────────────────────────────────────────────────────
// Annotations for loader registration.
//
// @SettingsLoader: General-purpose loader — declares which domain
// it serves and what it depends on. The client settings service
// builds a DAG from these annotations and loads in topological order.
//
// @RegisteredContextualSetting: Specialized annotation for settings
// backed by REDB + contextual resolution. Wires up all the
// resolution internals automatically — you just annotate a class
// and get hierarchical resolution for free.
// ──────────────────────────────────────────────────────────────────

/**
 * Marks a class as a settings loader for a given domain.
 * The client settings service discovers these at startup,
 * builds a DAG from the dependency declarations, and executes
 * loaders in topological order.
 *
 * @param domain The settings domain this loader serves.
 * @param dependsOn Domains that must be loaded before this one.
 *                  Their results are available via the DependencyMap.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsLoader(
    val domain: SettingsDomain,
    val dependsOn: Array<SettingsDomain> = [],
)

/**
 * Marks a class as a contextual setting backed by REDB.
 *
 * This is the "magic" annotation — it wires up:
 *   1. REDB entity fetching (via the redbTypeKey)
 *   2. Hierarchical resolution (via the setting definition's policy)
 *   3. Proto deserialization
 *
 * The loader only needs to annotate a class and provide the
 * setting definition reference. All resolution internals are
 * handled by the base implementation.
 *
 * @param definition Reference to the ContextualSettingDefinition
 *                   in SettingsDefinitions that defines the REDB key
 *                   and resolution policy for this setting.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisteredContextualSetting(
    val definitionName: String, // references ContextualSettingDefinition.name
)
