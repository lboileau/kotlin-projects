package com.acme.fulfillmentmethods

import com.acme.clientsettings.ConfigHandler
import com.acme.clientsettings.ConfigManager
import com.acme.clientsettings.ContextualSettingLoader
import com.acme.clientsettings.DataResourceMap
import com.acme.clientsettings.ResolvedSetting
import com.acme.clientsettings.SettingsDataLoader
import com.acme.clientsettings.SettingsDomain
import com.acme.contextualsettings.GuestIdentifierProto
import com.acme.contextualsettings.HierarchicalResolver
import com.acme.contextualsettings.OrderStatusTrackingProto
import com.acme.contextualsettings.PrepTimeProto
import com.acme.contextualsettings.RecipientDetailsProto
import com.acme.contextualsettings.SchedulingProto
import com.acme.contextualsettings.SettingsContext

// ──────────────────────────────────────────────────────────────────
// Fulfillment Method Config Manager + Data Loaders
//
// This is where the three services come together:
//   - Client Settings Service (DAG orchestration)
//   - Contextual Settings Service (hierarchical resolution)
//   - REDB (entity storage)
//
// The dependency chain flows in phases:
//   Phase 1: Profile → list of methods for this context
//   Phase 2: Methods → entities with behaviour references
//   Phase 3: Behaviour settings → resolved for the context
//   Then:    ConfigManager → composes the final response
// ──────────────────────────────────────────────────────────────────

// ── Data Loaders (execute in DAG order) ──────────────────────────

// Phase 1: Load the profile — which fulfillment methods are available for this context
@SettingsDataLoader(
    domain = SettingsDomain.FM_PROFILE,
    redbReference = "com.acme.fulfillment.FulfillmentMethodProfile",
    dependsOn = [],
)
class FulfillmentMethodProfileDataLoader(
    resolver: HierarchicalResolver,
) : ContextualSettingLoader<FulfillmentMethodProfile>(resolver)

// Phase 2: Load the fulfillment method entities (contain behaviour references)
@SettingsDataLoader(
    domain = SettingsDomain.FM_METHODS,
    redbReference = "com.acme.fulfillment.FulfillmentMethod",
    dependsOn = [SettingsDomain.FM_PROFILE],
)
class FulfillmentMethodDataLoader(
    resolver: HierarchicalResolver,
) : ContextualSettingLoader<FulfillmentMethodEntity>(resolver)

// Phase 3: Load individual behaviour settings (all depend on methods)
@SettingsDataLoader(
    domain = SettingsDomain.RECIPIENT_DETAILS,
    redbReference = "com.acme.fulfillment.RecipientDetailsSettings",
    dependsOn = [SettingsDomain.FM_METHODS],
)
class RecipientDetailsDataLoader(
    resolver: HierarchicalResolver,
) : ContextualSettingLoader<RecipientDetailsProto>(resolver)

@SettingsDataLoader(
    domain = SettingsDomain.GUEST_IDENTIFIER,
    redbReference = "com.acme.fulfillment.GuestIdentifierSettings",
    dependsOn = [SettingsDomain.FM_METHODS],
)
class GuestIdentifierDataLoader(
    resolver: HierarchicalResolver,
) : ContextualSettingLoader<GuestIdentifierProto>(resolver)

@SettingsDataLoader(
    domain = SettingsDomain.SCHEDULING,
    redbReference = "com.acme.fulfillment.SchedulingSettings",
    dependsOn = [SettingsDomain.FM_METHODS],
)
class SchedulingDataLoader(
    resolver: HierarchicalResolver,
) : ContextualSettingLoader<SchedulingProto>(resolver)

@SettingsDataLoader(
    domain = SettingsDomain.PREP_TIME,
    redbReference = "com.acme.fulfillment.PrepTimeSettings",
    dependsOn = [SettingsDomain.FM_METHODS],
)
class PrepTimeDataLoader(
    resolver: HierarchicalResolver,
) : ContextualSettingLoader<PrepTimeProto>(resolver)

@SettingsDataLoader(
    domain = SettingsDomain.ORDER_STATUS_TRACKING,
    redbReference = "com.acme.fulfillment.OrderStatusTrackingSettings",
    dependsOn = [SettingsDomain.FM_METHODS],
)
class OrderStatusTrackingDataLoader(
    resolver: HierarchicalResolver,
) : ContextualSettingLoader<OrderStatusTrackingProto>(resolver)

// ── Config Manager ───────────────────────────────────────────────

/**
 * Builds the final computed fulfillment methods from pre-loaded data.
 *
 * All data has been fetched and resolved by the DAG before this runs.
 * Each resolved setting includes audit info (query context + matching
 * context), which lets us match setting results to their corresponding
 * fulfillment method (since fulfillment_method_id is part of the
 * query context).
 */
@ConfigManager(domain = SettingsDomain.CONTEXTUAL_FULFILLMENT_METHODS)
class FulfillmentMethodConfigManager(
    private val behaviourAdapter: BehaviourAdapter,
) : ConfigHandler<List<FulfillmentMethod>> {

    override fun handle(
        context: SettingsContext,
        data: DataResourceMap,
    ): List<FulfillmentMethod> {
        // All data already loaded and resolved — just compose
        val methods = data.get<ResolvedSetting<List<FulfillmentMethodEntity>>>(SettingsDomain.FM_METHODS)
        val recipientDetails = data.get<ResolvedSetting<RecipientDetailsProto>>(SettingsDomain.RECIPIENT_DETAILS)
        val guestIdentifier = data.get<ResolvedSetting<GuestIdentifierProto>>(SettingsDomain.GUEST_IDENTIFIER)
        val scheduling = data.get<ResolvedSetting<SchedulingProto>>(SettingsDomain.SCHEDULING)
        val prepTime = data.get<ResolvedSetting<PrepTimeProto>>(SettingsDomain.PREP_TIME)
        val orderStatusTracking = data.get<ResolvedSetting<OrderStatusTrackingProto>>(SettingsDomain.ORDER_STATUS_TRACKING)

        return methods.value.map { entity ->
            composeFulfillmentMethod(
                entity = entity,
                recipientDetails = recipientDetails,
                guestIdentifier = guestIdentifier,
                scheduling = scheduling,
                prepTime = prepTime,
                orderStatusTracking = orderStatusTracking,
            )
        }
    }

    private fun composeFulfillmentMethod(
        entity: FulfillmentMethodEntity,
        recipientDetails: ResolvedSetting<RecipientDetailsProto>,
        guestIdentifier: ResolvedSetting<GuestIdentifierProto>,
        scheduling: ResolvedSetting<SchedulingProto>,
        prepTime: ResolvedSetting<PrepTimeProto>,
        orderStatusTracking: ResolvedSetting<OrderStatusTrackingProto>,
    ): FulfillmentMethod {
        return FulfillmentMethod(
            id = entity.id,
            name = entity.name,
            behaviours = entity.behaviours,
            recipientDetails = if (BehaviourType.RECIPIENT_DETAILS in entity.behaviours) {
                behaviourAdapter.adaptRecipientDetails(recipientDetails.value, guestIdentifier.value)
            } else null,
            scheduling = if (BehaviourType.SCHEDULING in entity.behaviours) {
                behaviourAdapter.adaptScheduling(scheduling.value)
            } else null,
            prepTime = if (BehaviourType.PREP_TIME in entity.behaviours) {
                behaviourAdapter.adaptPrepTime(prepTime.value)
            } else null,
            orderStatusTracking = if (BehaviourType.ORDER_STATUS_TRACKING in entity.behaviours) {
                behaviourAdapter.adaptOrderStatusTracking(orderStatusTracking.value)
            } else null,
        )
    }
}

// Stub types
data class MerchantInfo(val id: String, val name: String)
data class LocationInfo(val id: String, val name: String, val timezone: String)
data class FulfillmentMethodProfile(val methodIds: List<String>)
