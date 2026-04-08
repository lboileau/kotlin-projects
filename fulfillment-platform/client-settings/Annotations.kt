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
 * This annotation wires up:
 *   1. REDB entity fetching (via the proto descriptor reference)
 *   2. Proto deserialization
 *
 * The resolution POLICY is NOT in the annotation — it's defined
 * statically in the contextual settings module (SettingsDefinitions)
 * alongside the setting definition. This keeps policy logic in code
 * where it can be reviewed and tested, not scattered across annotations.
 *
 * @param redbReference The full proto descriptor name used as the REDB
 *                      type key (e.g., "com.acme.fulfillment.RecipientDetailsSettings")
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegisteredContextualSetting(
    val redbReference: String, // full proto descriptor name
)
