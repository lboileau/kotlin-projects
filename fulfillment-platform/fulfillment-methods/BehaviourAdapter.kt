package com.acme.fulfillmentmethods

import com.acme.contextualsettings.GuestIdentifierProto
import com.acme.contextualsettings.OrderStatusTrackingProto
import com.acme.contextualsettings.PrepTimeProto
import com.acme.contextualsettings.RecipientDetailsProto
import com.acme.contextualsettings.SchedulingProto

// ──────────────────────────────────────────────────────────────────
// Behaviour Adapter
//
// Converts raw contextually-resolved settings (proto models from REDB)
// into client-facing behaviour objects.
//
// This is where the composition happens for complex behaviours.
// For example, RecipientDetailsBehaviour is composed from TWO
// independently-resolved settings: recipient_details + guest_identifier.
//
// The adapter is also where we can apply business rules on top of
// the raw configuration — e.g., if scheduling is enabled but prep
// time is zero, auto-set a minimum lead time.
// ──────────────────────────────────────────────────────────────────

class BehaviourAdapter {

    /**
     * Compose recipient details from two independently-resolved settings.
     *
     * This is the key example for the interview:
     * - RecipientDetailsProto defines which fields to collect (name, phone, etc.)
     * - GuestIdentifierProto is a generic "identify the guest" field
     *   that powers DIFFERENT use cases depending on context:
     *
     *   Drive-thru → displayText: "Car Details"  (make, model, color)
     *   Dine-in   → displayText: "Table Number"
     *   Delivery  → displayText: "Delivery Notes"
     *
     * Same generic model, different configuration per context.
     * No DriveThroughRecipientDetails class. No switch statement.
     */
    fun adaptRecipientDetails(
        details: RecipientDetailsProto?,
        guestIdentifier: GuestIdentifierProto?,
    ): RecipientDetailsBehaviour? {
        details ?: return null

        return RecipientDetailsBehaviour(
            fields = details.fields.map { field ->
                FieldRequirement(
                    fieldName = field.fieldName,
                    required = field.required,
                    visible = field.visible,
                )
            },
            guestIdentifier = guestIdentifier?.let {
                GuestIdentifierBehaviour(
                    clientId = it.clientId,
                    value = it.value,
                    displayText = it.displayText,
                )
            },
        )
    }

    fun adaptScheduling(settings: SchedulingProto?): SchedulingBehaviour? {
        settings ?: return null

        return SchedulingBehaviour(
            enabled = settings.enabled,
            minLeadTimeMinutes = settings.minLeadTimeMinutes,
            maxDaysAhead = settings.maxDaysAhead,
        )
    }

    fun adaptPrepTime(settings: PrepTimeProto?): PrepTimeBehaviour? {
        settings ?: return null

        return PrepTimeBehaviour(
            defaultMinutes = settings.defaultMinutes,
            showEstimateToCustomer = settings.showEstimateToCustomer,
        )
    }

    fun adaptOrderStatusTracking(settings: OrderStatusTrackingProto?): OrderStatusTrackingBehaviour? {
        settings ?: return null

        return OrderStatusTrackingBehaviour(
            showWindowNumber = settings.showWindowNumber,
            showDriverLocation = settings.showDriverLocation,
            statusUpdatesEnabled = settings.statusUpdatesEnabled,
        )
    }
}
