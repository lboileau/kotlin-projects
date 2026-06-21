package com.acme.clientsettings

import com.acme.contextualsettings.ContextualSettingDefinition
import com.acme.contextualsettings.HierarchicalResolver
import com.acme.contextualsettings.SettingsContext
import com.acme.contextualsettings.SettingsDefinitions

// ──────────────────────────────────────────────────────────────────
// Base implementation for contextual settings data loaders.
//
// Classes annotated with @SettingsDataLoader get all of this for
// free — they just declare the REDB reference (proto descriptor)
// and the base class handles:
//   1. Looking up the definition from SettingsDefinitions (by REDB key)
//   2. Calling the HierarchicalResolver with the right policy
//   3. Returning the deserialized, fully-resolved value with audit info
//
// A new contextual setting = one annotated class, zero boilerplate.
// ──────────────────────────────────────────────────────────────────

/**
 * Base class for data loaders backed by @SettingsDataLoader.
 * Subclasses just need the annotation — resolution is automatic.
 */
abstract class ContextualSettingLoader<T : Any>(
    private val resolver: HierarchicalResolver,
) : Loader<ResolvedSetting<T>> {

    // Lazily resolved: annotation provides the REDB reference (proto descriptor),
    // the definition (with resolution policy) lives in SettingsDefinitions.
    private val definition: ContextualSettingDefinition<T> by lazy {
        val annotation = this::class.java.getAnnotation(SettingsDataLoader::class.java)
            ?: throw IllegalStateException(
                "${this::class.simpleName} extends ContextualSettingLoader " +
                "but is missing @SettingsDataLoader"
            )

        @Suppress("UNCHECKED_CAST")
        SettingsDefinitions.ALL.find { it.redbTypeKey == annotation.redbReference }
            as? ContextualSettingDefinition<T>
            ?: throw IllegalStateException(
                "No setting definition found for REDB reference '${annotation.redbReference}'"
            )
    }

    /**
     * Loads by delegating to the hierarchical resolver.
     * The resolver uses the definition's REDB key and resolution policy
     * to fetch and resolve the setting for the given context.
     *
     * Returns the resolved value along with audit info (query scope
     * and matching scope) so the ConfigManager can match results back
     * to their corresponding fulfillment method.
     */
    override fun load(context: SettingsContext, dependencies: DataResourceMap): ResolvedSetting<T> {
        return resolver.resolveWithAudit(definition, context)
            ?: throw IllegalStateException(
                "No value found for setting '${definition.name}' in context $context"
            )
    }
}

/**
 * A resolved setting value with audit information.
 * The audit info shows what we queried for and what scope actually matched,
 * enabling the ConfigManager to match results back to their fulfillment method
 * (since fulfillment_method_id is part of the query context).
 */
data class ResolvedSetting<T>(
    val value: T,
    val queryContext: Map<String, String>,    // what we queried for
    val matchingContext: Map<String, String>, // what scope actually matched
)
