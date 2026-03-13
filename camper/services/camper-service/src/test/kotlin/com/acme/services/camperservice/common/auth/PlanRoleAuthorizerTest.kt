package com.acme.services.camperservice.common.auth

import com.acme.clients.common.Result
import com.acme.clients.planclient.fake.FakePlanClient
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class PlanRoleAuthorizerTest {

    private val fakePlanClient = FakePlanClient()
    private val authorizer = PlanRoleAuthorizer(fakePlanClient)

    private val planId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val nonMemberId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakePlanClient.reset()
        fakePlanClient.seedPlan(
            Plan(id = planId, name = "Test Plan", visibility = "private", ownerId = ownerId, createdAt = Instant.now(), updatedAt = Instant.now())
        )
        fakePlanClient.seedMember(
            PlanMember(planId = planId, userId = ownerId, role = "member", createdAt = Instant.now()),
            PlanMember(planId = planId, userId = managerId, role = "manager", createdAt = Instant.now()),
            PlanMember(planId = planId, userId = memberId, role = "member", createdAt = Instant.now())
        )
    }

    @Nested
    inner class RoleResolution {

        @Test
        fun `owner resolves to OWNER regardless of plan_members role`() {
            val result = authorizer.authorize(planId, ownerId, setOf(PlanRole.OWNER))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            val context = (result as Result.Success).value
            assertThat(context.role).isEqualTo(PlanRole.OWNER)
            assertThat(context.planId).isEqualTo(planId)
            assertThat(context.userId).isEqualTo(ownerId)
        }

        @Test
        fun `manager resolves to MANAGER`() {
            val result = authorizer.authorize(planId, managerId, setOf(PlanRole.MANAGER))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value.role).isEqualTo(PlanRole.MANAGER)
        }

        @Test
        fun `member resolves to MEMBER`() {
            val result = authorizer.authorize(planId, memberId, setOf(PlanRole.MEMBER))

            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).value.role).isEqualTo(PlanRole.MEMBER)
        }

        @Test
        fun `non-member returns failure with null actualRole`() {
            val result = authorizer.authorize(planId, nonMemberId, setOf(PlanRole.MEMBER))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error.actualRole).isNull()
            assertThat(error.planId).isEqualTo(planId)
            assertThat(error.userId).isEqualTo(nonMemberId)
        }
    }

    @Nested
    inner class RoleChecking {

        @Test
        fun `owner passes OWNER-only check`() {
            val result = authorizer.authorize(planId, ownerId, setOf(PlanRole.OWNER))
            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

        @Test
        fun `owner passes OWNER or MANAGER check`() {
            val result = authorizer.authorize(planId, ownerId, setOf(PlanRole.OWNER, PlanRole.MANAGER))
            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

        @Test
        fun `manager passes OWNER or MANAGER check`() {
            val result = authorizer.authorize(planId, managerId, setOf(PlanRole.OWNER, PlanRole.MANAGER))
            assertThat(result).isInstanceOf(Result.Success::class.java)
        }

        @Test
        fun `member fails OWNER or MANAGER check`() {
            val result = authorizer.authorize(planId, memberId, setOf(PlanRole.OWNER, PlanRole.MANAGER))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error.actualRole).isEqualTo(PlanRole.MEMBER)
            assertThat(error.requiredRoles).containsExactlyInAnyOrder(PlanRole.OWNER, PlanRole.MANAGER)
        }

        @Test
        fun `manager fails OWNER-only check`() {
            val result = authorizer.authorize(planId, managerId, setOf(PlanRole.OWNER))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error.actualRole).isEqualTo(PlanRole.MANAGER)
        }

        @Test
        fun `member passes any-member check`() {
            val result = authorizer.authorize(planId, memberId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER))
            assertThat(result).isInstanceOf(Result.Success::class.java)
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `non-existent plan returns failure`() {
            val result = authorizer.authorize(UUID.randomUUID(), ownerId, setOf(PlanRole.OWNER))

            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).error.actualRole).isNull()
        }
    }
}
