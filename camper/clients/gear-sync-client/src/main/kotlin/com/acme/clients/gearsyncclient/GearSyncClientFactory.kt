package com.acme.clients.gearsyncclient

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.gearsyncclient.api.GearSyncClient
import com.acme.clients.gearsyncclient.internal.DefaultGearSyncClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.planclient.api.PlanClient

fun createGearSyncClient(
    assignmentClient: AssignmentClient,
    itemClient: ItemClient,
    planClient: PlanClient,
): GearSyncClient = DefaultGearSyncClient(assignmentClient, itemClient, planClient)
