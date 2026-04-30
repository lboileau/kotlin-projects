/**
 * W19 — Pending vs registered member visual distinction: AvatarHead
 *
 * Verifies:
 *  (1) With invitationStatus="pending", renders avatar-head--pending class on the SVG
 *  (2) Without invitationStatus (undefined), renders without avatar-head--pending class
 *  (3) With invitationStatus=null, renders without avatar-head--pending class
 *  (4) With invitationStatus="registered", renders without avatar-head--pending class
 *  (5) With invitationStatus="sent" (non-null, non-registered), renders pending class
 *
 * W12 lesson — AssignmentsModal integration:
 *  (6) AssignmentsModal: pending plan member is excluded from the add-member panel;
 *      the panel shows only registered members (no ghost/pending entries offered for
 *      assignment, since unregistered members cannot be placed in tent/canoe groups).
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AvatarHead } from './AvatarHead';
import { AssignmentsModal } from './AssignmentsModal';
import type { AssignmentDetail, PlanMember } from '../api/client';

// ─── Mocks ────────────────────────────────────────────────────────────────────

vi.mock('./AvatarHair', () => ({
  AvatarHair: () => <g data-testid="avatar-hair" />,
}));

// api mock for AssignmentsModal integration test
const mockApi = vi.hoisted(() => ({
  getAssignments: vi.fn(),
  getAssignment: vi.fn(),
  deleteAssignment: vi.fn(),
  removeAssignmentMember: vi.fn(),
  addAssignmentMember: vi.fn(),
  createAssignment: vi.fn(),
  updateAssignment: vi.fn(),
}));

vi.mock('../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../api/client')>();
  return { ...original, api: mockApi };
});

// ─── AvatarHead unit tests ────────────────────────────────────────────────────

describe('AvatarHead — invitationStatus → pending class (W19)', () => {
  it('(1) invitationStatus="pending" → renders avatar-head--pending on SVG', () => {
    const { container } = render(<AvatarHead invitationStatus="pending" />);
    expect(container.querySelector('svg')).toHaveClass('avatar-head--pending');
  });

  it('(2) no invitationStatus → no avatar-head--pending class', () => {
    const { container } = render(<AvatarHead />);
    expect(container.querySelector('svg')).not.toHaveClass('avatar-head--pending');
  });

  it('(3) invitationStatus=null → no avatar-head--pending class', () => {
    const { container } = render(<AvatarHead invitationStatus={null} />);
    expect(container.querySelector('svg')).not.toHaveClass('avatar-head--pending');
  });

  it('(4) invitationStatus="registered" → no avatar-head--pending class', () => {
    const { container } = render(<AvatarHead invitationStatus="registered" />);
    expect(container.querySelector('svg')).not.toHaveClass('avatar-head--pending');
  });

  it('(5) invitationStatus="sent" (non-null, non-registered) → pending class', () => {
    const { container } = render(<AvatarHead invitationStatus="sent" />);
    expect(container.querySelector('svg')).toHaveClass('avatar-head--pending');
  });
});

// ─── AssignmentsModal integration (W12 lesson) ───────────────────────────────

const PLAN_ID = 'plan-1';
const PLAN_OWNER_ID = 'owner-1';
const REGISTERED_MEMBER_ID = 'member-2';
const PENDING_MEMBER_ID = 'member-3';
const ASSIGNMENT_ID = 'assign-1';

const planMembersWithPending: PlanMember[] = [
  {
    planId: PLAN_ID,
    userId: PLAN_OWNER_ID,
    username: 'RangerAlice',
    role: 'owner',
    invitationStatus: 'delivered',
    invitedBy: null,
    avatarSeed: null,
    avatar: null,
    email: 'alice@example.com',
    createdAt: '2026-01-01T00:00:00Z',
  },
  {
    planId: PLAN_ID,
    userId: REGISTERED_MEMBER_ID,
    username: 'TrailBob',
    role: 'member',
    invitationStatus: 'delivered',
    invitedBy: PLAN_OWNER_ID,
    avatarSeed: null,
    avatar: null,
    email: 'bob@example.com',
    createdAt: '2026-01-01T00:00:00Z',
  },
  {
    planId: PLAN_ID,
    userId: PENDING_MEMBER_ID,
    username: null, // pending — hasn't registered yet
    role: 'member',
    invitationStatus: 'sent',
    invitedBy: PLAN_OWNER_ID,
    avatarSeed: null,
    avatar: null,
    email: 'pending@example.com',
    createdAt: '2026-01-01T00:00:00Z',
  },
];

const assignmentWithOwnerOnly: AssignmentDetail = {
  id: ASSIGNMENT_ID,
  planId: PLAN_ID,
  type: 'tent',
  name: 'Base Camp',
  maxOccupancy: 4,
  ownerId: PLAN_OWNER_ID,
  members: [
    { assignmentId: ASSIGNMENT_ID, userId: PLAN_OWNER_ID, username: 'RangerAlice', createdAt: '2026-01-01T00:00:00Z' },
  ],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('AssignmentsModal — pending member excluded from add-member panel (W19 integration)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApi.getAssignments.mockResolvedValue([{ id: ASSIGNMENT_ID, type: 'tent', name: 'Base Camp' }]);
    mockApi.getAssignment.mockResolvedValue(assignmentWithOwnerOnly);
  });

  it('(6) add-member panel lists registered members only; pending member not offered', async () => {
    const user = userEvent.setup();
    render(
      <AssignmentsModal
        isOpen={true}
        onClose={vi.fn()}
        planId={PLAN_ID}
        planOwnerId={PLAN_OWNER_ID}
        currentUserId={PLAN_OWNER_ID}
        members={planMembersWithPending}
      />,
    );

    // Wait for the assignment to load
    await screen.findByText('Base Camp');

    // Open the add-member (Invite) panel
    const inviteBtn = screen.getByRole('button', { name: 'Invite' });
    await user.click(inviteBtn);

    // TrailBob (registered) should appear somewhere in the modal (may appear multiple times:
    // once in "unassigned" bar and once in the add-member list)
    expect(screen.getAllByText('TrailBob').length).toBeGreaterThan(0);

    // The add-member panel should list TrailBob as an option button
    const addMemberList = document.querySelector('.assign-add-member-list');
    expect(addMemberList).toBeInTheDocument();
    expect(addMemberList?.textContent).toContain('TrailBob');

    // The pending member's email should NOT appear anywhere in the modal
    // (no username → filtered by addablemembers, which requires m.username to be truthy)
    expect(screen.queryByText('pending@example.com')).not.toBeInTheDocument();

    // Nor "Pending Adventurer" as a button option inside the add-member panel
    const pendingAdventurerInPanel = addMemberList?.textContent?.includes('Pending Adventurer');
    expect(pendingAdventurerInPanel).toBeFalsy();
  });
});
