package com.acme.contextualsettings

// ──────────────────────────────────────────────────────────────────
// Resolution policies define HOW a setting resolves across scopes.
// Each behaviour can have its own policy — this is the key insight
// that makes the system flexible without being chaotic.
// ──────────────────────────────────────────────────────────────────

/**
 * Defines how a contextual setting resolves across scope levels.
 *
 * @param scopes Ordered list of scope vectors to check during resolution.
 *               Each vector is a list of context dimensions that define a scope level.
 *               Resolution walks this list in order — first match wins.
 *
 * @param strategy Whether to prefer overrides (most specific first)
 *                 or defaults (least specific first, overrides layer on top).
 */
data class ResolutionPolicy(
    val scopes: List<List<ScopeDimension>>,
    val strategy: ResolutionStrategy = ResolutionStrategy.OVERRIDE_WINS,
)

enum class ResolutionStrategy {
    /**
     * Most specific scope wins entirely.
     * Walk scopes in order, return the first match.
     * Use for: settings where a specific context should fully replace the default.
     * Example: recipient_details — a location+channel override replaces the merchant default entirely.
     */
    OVERRIDE_WINS,

    /**
     * Start from the broadest scope, layer narrower scopes on top.
     * Merge values — narrower scopes override individual fields, not the whole object.
     * Use for: settings where you want sparse overrides on top of rich defaults.
     * Example: branding — merchant sets the base, location can override specific fields.
     */
    DEFAULTS_WITH_OVERRIDES,
}
