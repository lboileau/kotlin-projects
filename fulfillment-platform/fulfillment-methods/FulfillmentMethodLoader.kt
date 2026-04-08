package com.acme.fulfillmentmethods

import com.acme.clientsettings.DependencyMap
import com.acme.clientsettings.Loader
import com.acme.clientsettings.SettingsDomain
import com.acme.clientsettings.SettingsLoader
import com.acme.contextualsettings.HierarchicalResolver
import com.acme.contextualsettings.SettingsContext
import com.acme.contextualsettings.SettingsDefinitions

// ──────────────────────────────────────────────────────────────────
// Fulfillment Method Loader
//
// This is the @SettingsLoader that the client settings service
// invokes when a client requests CONTEXTUAL_FULFILLMENT_METHODS.
//
// The DAG ensures MERCHANT_INFO and LOCATION_INFO are loaded first
// (declared in dependsOn). This loader then:
//   1. Fetches the list of fulfillment methods for the merchant
//   2. For each method, resolves all declared behaviours
//   3. Adapts the resolved settings into client-facing behaviour models
//   4. Returns the fully composed FulfillmentMethod objects
//
// This is where the three services come together:
//   - Client Settings Service (DAG orchestration)
//   - Contextual Settings Service (hierarchical resolution)
//   - REDB (entity storage)
// ──────────────────────────────────────────────────────────────────

@SettingsLoader(
    domain = SettingsDomain.CONTEXTUAL_FULFILLMENT_METHODS,
    dependsOn = [SettingsDomain.MERCHANT_INFO, SettingsDomain.LOCATION_INFO],
)
class FulfillmentMethodLoader(
    private val resolver: HierarchicalResolver,
    private val behaviourAdapter: BehaviourAdapter,
) : Loader<List<FulfillmentMethod>> {

    override fun load(
        context: SettingsContext,
        dependencies: DependencyMap,
    ): List<FulfillmentMethod> {
        // Dependencies are available from the DAG
        val merchantInfo = dependencies.get<MerchantInfo>(SettingsDomain.MERCHANT_INFO)
        val locationInfo = dependencies.get<LocationInfo>(SettingsDomain.LOCATION_INFO)

        // Fetch the raw fulfillment method entities for this merchant
        val methodEntities = fetchMethodEntities(context.merchantId)

        // For each method, resolve all behaviours for the given context
        return methodEntities.map { entity ->
            val methodContext = context.copy(fulfillmentMethodId = entity.id)
            resolveFulfillmentMethod(entity, methodContext)
        }
    }

    private fun resolveFulfillmentMethod(
        entity: FulfillmentMethodEntity,
        context: SettingsContext,
    ): FulfillmentMethod {
        // Resolve each declared behaviour using the contextual settings service
        var recipientDetails: RecipientDetailsBehaviour? = null
        var scheduling: SchedulingBehaviour? = null
        var prepTime: PrepTimeBehaviour? = null
        var orderStatusTracking: OrderStatusTrackingBehaviour? = null

        for (behaviourType in entity.behaviours) {
            when (behaviourType) {
                BehaviourType.RECIPIENT_DETAILS -> {
                    // This behaviour is composed of TWO settings:
                    // recipient_details + guest_identifier
                    val details = resolver.resolve(SettingsDefinitions.RECIPIENT_DETAILS, context)
                    val guestId = resolver.resolve(SettingsDefinitions.GUEST_IDENTIFIER, context)
                    recipientDetails = behaviourAdapter.adaptRecipientDetails(details, guestId)
                }
                BehaviourType.SCHEDULING -> {
                    val settings = resolver.resolve(SettingsDefinitions.SCHEDULING, context)
                    scheduling = behaviourAdapter.adaptScheduling(settings)
                }
                BehaviourType.PREP_TIME -> {
                    val settings = resolver.resolve(SettingsDefinitions.PREP_TIME, context)
                    prepTime = behaviourAdapter.adaptPrepTime(settings)
                }
                BehaviourType.ORDER_STATUS_TRACKING -> {
                    val settings = resolver.resolve(SettingsDefinitions.ORDER_STATUS_TRACKING, context)
                    orderStatusTracking = behaviourAdapter.adaptOrderStatusTracking(settings)
                }
            }
        }

        return FulfillmentMethod(
            id = entity.id,
            name = entity.name,
            behaviours = entity.behaviours,
            recipientDetails = recipientDetails,
            scheduling = scheduling,
            prepTime = prepTime,
            orderStatusTracking = orderStatusTracking,
        )
    }

    private fun fetchMethodEntities(merchantId: String): List<FulfillmentMethodEntity> {
        // In practice: query REDB for all fulfillment_method_v1 entities for this merchant
        // Stubbed here for walkthrough
        return emptyList()
    }
}

// Stub types for DAG dependencies
data class MerchantInfo(val id: String, val name: String)
data class LocationInfo(val id: String, val name: String, val timezone: String)
