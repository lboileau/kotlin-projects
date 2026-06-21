package com.acme.fulfillmentmethods

// ──────────────────────────────────────────────────────────────────
// Fulfillment Method Templates
//
// Pre-canned templates with sensible defaults.
// The system is fully flexible, but merchants don't need to
// understand the EAV model to get started.
//
// Full flexibility under the hood.
// Controlled surface area for merchants.
// ──────────────────────────────────────────────────────────────────

object FulfillmentMethodTemplates {

    /**
     * Drive-through: collect car details via generic guest identifier,
     * short prep time, show window number in status tracking.
     */
    val DRIVE_THROUGH = CreateFulfillmentMethodParams(
        merchantId = "", // set per merchant
        name = "Drive Through",
        behaviours = listOf(
            BehaviourType.RECIPIENT_DETAILS,
            BehaviourType.PREP_TIME,
            BehaviourType.ORDER_STATUS_TRACKING,
        ),
        behaviourDefaults = listOf(
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.RecipientDetailsSettings",
                defaultValues = mapOf(
                    "customer_name_required" to true,
                    "phone_number_required" to false,
                ),
            ),
            // Generic guest identifier → specific drive-thru UX
            // No DriveThroughGuestIdentifier class needed
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.GuestIdentifierSettings",
                defaultValues = mapOf(
                    "client_id" to "car_details",
                    "display_text" to "Car Details",
                    "value" to "",
                ),
            ),
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.PrepTimeSettings",
                defaultValues = mapOf(
                    "default_minutes" to 5,
                    "show_estimate_to_customer" to true,
                ),
            ),
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.OrderStatusTrackingSettings",
                defaultValues = mapOf(
                    "show_window_number" to true,
                    "show_driver_location" to false,
                    "status_updates_enabled" to true,
                ),
            ),
        ),
    )

    /**
     * Delivery: collect address, enable scheduling, show driver location.
     */
    val DELIVERY = CreateFulfillmentMethodParams(
        merchantId = "",
        name = "Delivery",
        behaviours = listOf(
            BehaviourType.RECIPIENT_DETAILS,
            BehaviourType.SCHEDULING,
            BehaviourType.PREP_TIME,
            BehaviourType.ORDER_STATUS_TRACKING,
        ),
        behaviourDefaults = listOf(
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.RecipientDetailsSettings",
                defaultValues = mapOf(
                    "customer_name_required" to true,
                    "phone_number_required" to true,
                    "email_required" to false,
                ),
            ),
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.SchedulingSettings",
                defaultValues = mapOf(
                    "enabled" to true,
                    "min_lead_time_minutes" to 30,
                    "max_days_ahead" to 7,
                ),
            ),
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.PrepTimeSettings",
                defaultValues = mapOf(
                    "default_minutes" to 30,
                    "show_estimate_to_customer" to true,
                ),
            ),
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.OrderStatusTrackingSettings",
                defaultValues = mapOf(
                    "show_window_number" to false,
                    "show_driver_location" to true,
                    "status_updates_enabled" to true,
                ),
            ),
        ),
    )

    /**
     * Pickup (in-store): basic name collection, no scheduling by default.
     */
    val PICKUP = CreateFulfillmentMethodParams(
        merchantId = "",
        name = "Pickup",
        behaviours = listOf(
            BehaviourType.RECIPIENT_DETAILS,
            BehaviourType.PREP_TIME,
        ),
        behaviourDefaults = listOf(
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.RecipientDetailsSettings",
                defaultValues = mapOf(
                    "customer_name_required" to true,
                ),
            ),
            BehaviourDefault(
                redbTypeKey = "com.acme.fulfillment.PrepTimeSettings",
                defaultValues = mapOf(
                    "default_minutes" to 15,
                    "show_estimate_to_customer" to true,
                ),
            ),
        ),
    )
}
