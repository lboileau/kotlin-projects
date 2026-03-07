package com.acme.services.camperservice.features.plan.service

import com.acme.clients.common.Result
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.userclient.fake.FakeUserClient
import com.acme.clients.userclient.model.User
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class PlanServiceTest {

    private val fakePlanClient = FakePlanClient()
    private val fakeUserClient = FakeUserClient()
    private val planService = PlanService(fakePlanClient, fakeUserClient)

    private val ownerId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakePlanClient.reset()
        fakeUserClient.reset()
        fakeUserClient.seed(
            User(id = ownerId, email = "owner@example.com", username = "owner", createdAt = Instant.now(), updatedAt = Instant.now()),
            User(id = otherUserId, email = "other@example.com", username = "other", createdAt = Instant.now(), updatedAt = Instant.now())
        )
    }

    @Nested
    inner class Create {
        @Test
        fun `create returns success and auto-adds creator as member`() {
            val result = planService.create(CreatePlanParam(name = "Trip", userId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val plan = (result as Result.Success).value
            assertThat(plan.name).isEqualTo("Trip")
            assertThat(plan.visibility).isEqualTo("private")
            assertThat(plan.ownerId).isEqualTo(ownerId)

            // Verify auto-membership
            val members = planService.getMembers(GetPlanMembersParam(plan.id))
            assertThat(members.isSuccess).isTrue()
            assertThat((members as Result.Success).value).hasSize(1)
            assertThat(members.value[0].userId).isEqualTo(ownerId)
        }

        @Test
        fun `create returns Invalid when name is blank`() {
            val result = planService.create(CreatePlanParam(name = "", userId = ownerId))

            assertThat(result.isFailure).isTrue()
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(PlanError.Invalid::class.java)
        }
    }

    @Nested
    inner class GetPlans {
        @Test
        fun `getPlans returns user plans and public plans`() {
            // Create a private plan for owner
            val privatePlan = (planService.create(CreatePlanParam(name = "Private", userId = ownerId)) as Result.Success).value

            // Create a public plan by another user (seed directly into fake)
            val publicPlanId = UUID.randomUUID()
            fakePlanClient.seedPlan(
                com.acme.clients.planclient.model.Plan(
                    id = publicPlanId, name = "Public", visibility = "public",
                    ownerId = otherUserId, createdAt = Instant.now(), updatedAt = Instant.now()
                )
            )

            val result = planService.getPlans(GetPlansParam(userId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val plans = (result as Result.Success).value
            assertThat(plans).hasSize(2)
            assertThat(plans.map { it.name }).containsExactlyInAnyOrder("Private", "Public")
        }

        @Test
        fun `getPlans deduplicates plans user is member of`() {
            // Create a public plan and add owner as member
            val publicPlanId = UUID.randomUUID()
            fakePlanClient.seedPlan(
                com.acme.clients.planclient.model.Plan(
                    id = publicPlanId, name = "Public", visibility = "public",
                    ownerId = otherUserId, createdAt = Instant.now(), updatedAt = Instant.now()
                )
            )
            fakePlanClient.seedMember(
                com.acme.clients.planclient.model.PlanMember(planId = publicPlanId, userId = ownerId, createdAt = Instant.now())
            )

            val result = planService.getPlans(GetPlansParam(userId = ownerId))

            assertThat(result.isSuccess).isTrue()
            val plans = (result as Result.Success).value
            assertThat(plans).hasSize(1)
            assertThat(plans[0].name).isEqualTo("Public")
        }

        @Test
        fun `getPlans returns empty when user has no plans and no public plans exist`() {
            val result = planService.getPlans(GetPlansParam(userId = ownerId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `update returns success when owner updates`() {
            val created = (planService.create(CreatePlanParam(name = "Old", userId = ownerId)) as Result.Success).value

            val result = planService.update(UpdatePlanParam(planId = created.id, name = "New", userId = ownerId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("New")
        }

        @Test
        fun `update returns NotOwner when non-owner updates`() {
            val created = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value

            val result = planService.update(UpdatePlanParam(planId = created.id, name = "Renamed", userId = otherUserId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.NotOwner::class.java)
        }

        @Test
        fun `update returns NotFound when plan does not exist`() {
            val result = planService.update(UpdatePlanParam(planId = UUID.randomUUID(), name = "Nope", userId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.NotFound::class.java)
        }

        @Test
        fun `update returns Invalid when name is blank`() {
            val created = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value

            val result = planService.update(UpdatePlanParam(planId = created.id, name = "", userId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.Invalid::class.java)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `delete returns success when owner deletes`() {
            val created = (planService.create(CreatePlanParam(name = "Doomed", userId = ownerId)) as Result.Success).value

            val result = planService.delete(DeletePlanParam(planId = created.id, userId = ownerId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `delete returns NotOwner when non-owner deletes`() {
            val created = (planService.create(CreatePlanParam(name = "Protected", userId = ownerId)) as Result.Success).value

            val result = planService.delete(DeletePlanParam(planId = created.id, userId = otherUserId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.NotOwner::class.java)
        }

        @Test
        fun `delete returns NotFound when plan does not exist`() {
            val result = planService.delete(DeletePlanParam(planId = UUID.randomUUID(), userId = ownerId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.NotFound::class.java)
        }
    }

    @Nested
    inner class AddMember {
        @Test
        fun `addMember adds user by email`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value

            val result = planService.addMember(AddPlanMemberParam(planId = plan.id, email = "other@example.com"))

            assertThat(result.isSuccess).isTrue()
            val member = (result as Result.Success).value
            assertThat(member.userId).isEqualTo(otherUserId)
            assertThat(member.planId).isEqualTo(plan.id)
            assertThat(member.username).isEqualTo("other")
        }

        @Test
        fun `addMember creates user when email does not exist`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value

            val result = planService.addMember(AddPlanMemberParam(planId = plan.id, email = "newuser@example.com"))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `addMember returns AlreadyMember for duplicate`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value
            planService.addMember(AddPlanMemberParam(planId = plan.id, email = "other@example.com"))

            val result = planService.addMember(AddPlanMemberParam(planId = plan.id, email = "other@example.com"))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.AlreadyMember::class.java)
        }

        @Test
        fun `addMember returns Invalid when email is blank`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value

            val result = planService.addMember(AddPlanMemberParam(planId = plan.id, email = ""))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.Invalid::class.java)
        }
    }

    @Nested
    inner class RemoveMember {
        @Test
        fun `removeMember allows self-remove`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value
            planService.addMember(AddPlanMemberParam(planId = plan.id, email = "other@example.com"))

            val result = planService.removeMember(RemovePlanMemberParam(planId = plan.id, userId = otherUserId, requestingUserId = otherUserId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `removeMember allows owner to remove another user`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value
            planService.addMember(AddPlanMemberParam(planId = plan.id, email = "other@example.com"))

            val result = planService.removeMember(RemovePlanMemberParam(planId = plan.id, userId = otherUserId, requestingUserId = ownerId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `removeMember returns NotOwner when non-owner tries to remove another user`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value
            planService.addMember(AddPlanMemberParam(planId = plan.id, email = "other@example.com"))
            val thirdUserId = UUID.randomUUID()
            fakeUserClient.seed(
                User(id = thirdUserId, email = "third@example.com", username = "third", createdAt = Instant.now(), updatedAt = Instant.now())
            )
            planService.addMember(AddPlanMemberParam(planId = plan.id, email = "third@example.com"))

            val result = planService.removeMember(RemovePlanMemberParam(planId = plan.id, userId = thirdUserId, requestingUserId = otherUserId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(PlanError.NotOwner::class.java)
        }
    }

    @Nested
    inner class GetMembers {
        @Test
        fun `getMembers returns all members of a plan`() {
            val plan = (planService.create(CreatePlanParam(name = "Trip", userId = ownerId)) as Result.Success).value
            planService.addMember(AddPlanMemberParam(planId = plan.id, email = "other@example.com"))

            val result = planService.getMembers(GetPlanMembersParam(plan.id))

            assertThat(result.isSuccess).isTrue()
            val members = (result as Result.Success).value
            assertThat(members).hasSize(2) // owner auto-added + other
            val usernames = members.map { it.username }
            assertThat(usernames).containsExactlyInAnyOrder("owner", "other")
        }

        @Test
        fun `getMembers returns empty for plan with no members`() {
            // Seed a plan directly without auto-membership
            val planId = UUID.randomUUID()
            fakePlanClient.seedPlan(
                com.acme.clients.planclient.model.Plan(
                    id = planId, name = "Empty", visibility = "private",
                    ownerId = ownerId, createdAt = Instant.now(), updatedAt = Instant.now()
                )
            )

            val result = planService.getMembers(GetPlanMembersParam(planId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isEmpty()
        }
    }
}
