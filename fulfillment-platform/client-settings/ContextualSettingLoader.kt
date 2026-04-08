package com.acme.clientsettings

import com.acme.contextualsettings.ContextualSettingDefinition
import com.acme.contextualsettings.HierarchicalResolver
import com.acme.contextualsettings.SettingsContext
import com.acme.contextualsettings.SettingsDefinitions

// ──────────────────────────────────────────────────────────────────
// Base implementation for contextual settings loaders.
//
// Classes annotated with @RegisteredContextualSetting get all of
// this for free — they just declare the setting definition reference
// and the base class handles:
//   1. Looking up the definition from SettingsDefinitions
//   2. Calling the HierarchicalResolver with the right policy
//   3. Returning the deserialized, fully-resolved value
//
// This is the glue between the annotation system and the resolution
// engine. A new contextual setting = one annotated class, zero
// boilerplate for resolution logic.
// ──────────────────────────────────────────────────────────────────

/**
 * Base class for loaders backed by @RegisteredContextualSetting.
 * Subclasses just need the annotation — resolution is automatic.
 */
abstract class ContextualSettingLoader<T : Any>(
    private val resolver: HierarchicalResolver,
) : Loader<T> {

    // Lazily resolved: annotation provides the REDB reference (proto descriptor),
    // the definition (with resolution policy) lives in SettingsDefinitions.
    private val definition: ContextualSettingDefinition<T> by lazy {
        val annotation = this::class.java.getAnnotation(RegisteredContextualSetting::class.java)
            ?: throw IllegalStateException(
                "${this::class.simpleName} extends ContextualSettingLoader " +
                "but is missing @RegisteredContextualSetting"
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
     */
    override fun load(context: SettingsContext, dependencies: DependencyMap): T {
        return resolver.resolve(definition, context)
            ?: throw IllegalStateException(
                "No value found for setting '${definition.name}' in context $context"
            )
    }
}
