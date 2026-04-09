package com.acme.clientsettings

// ──────────────────────────────────────────────────────────────────
// Annotations for loader registration.
//
// @ConfigManager: Top-level handler that builds the computed response
// from pre-loaded data resources. Receives all data after the DAG
// has executed.
//
// @SettingsDataLoader: Specialized data loader that leverages the
// hierarchical settings service. Annotated with a domain identifier
// and the REDB proto descriptor reference. Resolution policy is NOT
// in the annotation — it lives in code in the contextual settings
// module (SettingsDefinitions).
// ──────────────────────────────────────────────────────────────────

/**
 * Marks a class as a ConfigManager — the top-level handler that
 * builds the final computed response from pre-loaded data.
 *
 * The client settings service discovers the ConfigManager for the
 * requested domain and passes it all the data loaded by the DAG.
 *
 * @param domain The settings domain this config manager handles.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigManager(
    val domain: SettingsDomain,
)

/**
 * Marks a class as a settings data loader in the DAG.
 *
 * Each data loader declares:
 *   - domain: its identifier in the DAG
 *   - redbReference: the full proto descriptor name for REDB lookup
 *   - dependsOn: other data loader domain identifiers that must
 *     load before this one
 *
 * The base implementation (ContextualSettingLoader) automatically
 * wires up REDB fetching, hierarchical resolution, and proto
 * deserialization. The resolution POLICY lives in the contextual
 * settings module (SettingsDefinitions), not here.
 *
 * @param domain This loader's identifier in the DAG.
 * @param redbReference Full proto descriptor name (e.g., "com.acme.fulfillment.RecipientDetailsSettings")
 * @param dependsOn Domain identifiers of loaders that must complete first.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsDataLoader(
    val domain: SettingsDomain,
    val redbReference: String,
    val dependsOn: Array<SettingsDomain> = [],
)
