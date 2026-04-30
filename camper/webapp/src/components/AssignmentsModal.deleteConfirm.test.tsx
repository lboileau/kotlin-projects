/**
 * W23 — AssignmentsModal delete + remove-member: ConfirmModal flows
 *
 * Verifies:
 *  (1) "Break this camp?" confirm modal appears when plan owner clicks delete on a card.
 *  (2) Confirming the break-camp modal calls onDelete with the correct assignment id.
 *  (3) Cancelling the break-camp modal does NOT call onDelete.
 *  (4) "Remove from group?" confirm modal appears when plan owner removes a member.
 *  (5) Confirming the remove-member modal calls onRemoveMember with correct ids.
 *  (6) Self-leave (Leave button) bypasses ConfirmModal entirely — immediate action.
 *  (7) Plan owner sees "Invite" button (not "Add Member") — W23 verb standardization.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AssignmentsModal } from './AssignmentsModal';
import type { AssignmentDetail, PlanMember } from '../api/client';

// ─── Mock api ─────────────────────────────────────────────────────────────────

const mockApi = vi.hoisted(() => ({
  getAssignments: vi.fn(),
  getAssignment:  vi.fn(),
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

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const PLAN_ID = 'plan-1';
const PLAN_OWNER_ID = 'owner-1';
const MEMBER_ID = 'member-2';
const ASSIGNMENT_ID = 'assign-1';

const planMembers: PlanMember[] = [
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
    userId: MEMBER_ID,
    username: 'TrailBob',
    role: 'member',
    invitationStatus: 'delivered',
    invitedBy: PLAN_OWNER_ID,
    avatarSeed: null,
    avatar: null,
    email: 'bob@example.com',
    createdAt: '2026-01-01T00:00:00Z',
  },
];

const assignmentDetail: AssignmentDetail = {
  id: ASSIGNMENT_ID,
  planId: PLAN_ID,
  type: 'tent',
  name: 'Alpine Two-Man',
  maxOccupancy: 4,
  ownerId: PLAN_OWNER_ID,
  members: [
    { assignmentId: ASSIGNMENT_ID, userId: PLAN_OWNER_ID, username: 'RangerAlice', createdAt: '2026-01-01T00:00:00Z' },
    { assignmentId: ASSIGNMENT_ID, userId: MEMBER_ID,     username: 'TrailBob',    createdAt: '2026-01-01T00:00:00Z' },
  ],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

function setupApiMocks() {
  mockApi.getAssignments.mockResolvedValue([{ id: ASSIGNMENT_ID, type: 'tent', name: 'Alpine Two-Man' }]);
  mockApi.getAssignment.mockResolvedValue(assignmentDetail);
  mockApi.deleteAssignment.mockResolvedValue(undefined);
  mockApi.removeAssignmentMember.mockResolvedValue(undefined);
}

function renderModal() {
  return render(
    <AssignmentsModal
      isOpen={true}
      onClose={vi.fn()}
      planId={PLAN_ID}
      planOwnerId={PLAN_OWNER_ID}
      currentUserId={PLAN_OWNER_ID}
      members={planMembers}
    />
  );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('AssignmentsModal — delete + remove-member via ConfirmModal (W23)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupApiMocks();
  });

  it('delete card: shows "Break this camp?" confirm modal — does NOT immediately delete', async () => {
    const user = userEvent.setup();
    renderModal();

    // Wait for the assignment card to load
    await screen.findByText('Alpine Two-Man');

    // Hover to reveal the actions, then click the delete button
    const deleteBtn = await screen.findByRole('button', { name: /Delete this group/i });
    await user.click(deleteBtn);

    // ConfirmModal should appear
    expect(screen.getByText('Break this camp?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Break Camp/i })).toBeInTheDocument();

    // deleteAssignment should NOT have been called yet
    expect(mockApi.deleteAssignment).not.toHaveBeenCalled();
  });

  it('delete card: confirming calls deleteAssignment with correct id', async () => {
    const user = userEvent.setup();
    // After delete, re-fetch returns empty
    mockApi.getAssignments.mockResolvedValueOnce([{ id: ASSIGNMENT_ID, type: 'tent', name: 'Alpine Two-Man' }])
      .mockResolvedValue([]);
    renderModal();

    await screen.findByText('Alpine Two-Man');
    const deleteBtn = await screen.findByRole('button', { name: /Delete this group/i });
    await user.click(deleteBtn);

    const confirmBtn = screen.getByRole('button', { name: /Break Camp/i });
    await user.click(confirmBtn);

    await waitFor(() => {
      expect(mockApi.deleteAssignment).toHaveBeenCalledWith(PLAN_ID, ASSIGNMENT_ID);
    });
  });

  it('delete card: cancelling does NOT call deleteAssignment', async () => {
    const user = userEvent.setup();
    renderModal();

    await screen.findByText('Alpine Two-Man');
    const deleteBtn = await screen.findByRole('button', { name: /Delete this group/i });
    await user.click(deleteBtn);

    // Modal open — cancel it
    const cancelBtn = screen.getByRole('button', { name: /Cancel/i });
    await user.click(cancelBtn);

    expect(mockApi.deleteAssignment).not.toHaveBeenCalled();
    expect(screen.queryByText('Break this camp?')).not.toBeInTheDocument();
  });

  it('remove member: shows "Remove from group?" confirm modal for plan owner removing another member', async () => {
    const user = userEvent.setup();
    renderModal();

    await screen.findByText('Alpine Two-Man');

    // The remove button is for TrailBob (not the owner, not self)
    const removeBtn = await screen.findByRole('button', { name: /Remove TrailBob from group/i });
    await user.click(removeBtn);

    expect(screen.getByRole('heading', { name: /Remove from group\?/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Remove$/i })).toBeInTheDocument();
    // The modal message mentions the member name
    expect(document.querySelector('.confirm-modal__message')?.textContent).toContain('TrailBob');

    // removeAssignmentMember should NOT have been called yet
    expect(mockApi.removeAssignmentMember).not.toHaveBeenCalled();
  });

  it('remove member: confirming calls removeAssignmentMember with correct ids', async () => {
    const user = userEvent.setup();
    // After remove, re-fetch returns assignment with only the owner
    const afterDetail: AssignmentDetail = {
      ...assignmentDetail,
      members: [{ assignmentId: ASSIGNMENT_ID, userId: PLAN_OWNER_ID, username: 'RangerAlice', createdAt: '2026-01-01T00:00:00Z' }],
    };
    mockApi.getAssignment
      .mockResolvedValueOnce(assignmentDetail)
      .mockResolvedValue(afterDetail);

    renderModal();
    await screen.findByText('Alpine Two-Man');

    const removeBtn = await screen.findByRole('button', { name: /Remove TrailBob from group/i });
    await user.click(removeBtn);

    const confirmBtn = screen.getByRole('button', { name: /^Remove$/i });
    await user.click(confirmBtn);

    await waitFor(() => {
      expect(mockApi.removeAssignmentMember).toHaveBeenCalledWith(PLAN_ID, ASSIGNMENT_ID, MEMBER_ID);
    });
  });

  it('plan owner sees "Invite" button (not "Add Member") — W23 verb standardization', async () => {
    const user = userEvent.setup();
    renderModal();

    await screen.findByText('Alpine Two-Man');

    // Open the add-member panel so the Invite button is rendered
    const inviteBtn = screen.getByRole('button', { name: 'Invite' });
    expect(inviteBtn).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add Member' })).not.toBeInTheDocument();

    // Suppress unused-variable warning
    await user.click(inviteBtn);
  });

  it('self-leave (Leave button) is immediate — no ConfirmModal shown', async () => {
    const user = userEvent.setup();
    // Render as the member (not plan owner) so the Leave button is visible
    mockApi.getAssignment.mockResolvedValue({
      ...assignmentDetail,
      // Keep same members but render as TrailBob
    });

    render(
      <AssignmentsModal
        isOpen={true}
        onClose={vi.fn()}
        planId={PLAN_ID}
        planOwnerId={PLAN_OWNER_ID}
        currentUserId={MEMBER_ID}
        members={planMembers}
      />
    );

    await screen.findByText('Alpine Two-Man');

    const leaveBtn = await screen.findByRole('button', { name: /Leave/i });
    await user.click(leaveBtn);

    // No ConfirmModal should appear
    expect(screen.queryByText('Break this camp?')).not.toBeInTheDocument();
    expect(screen.queryByText('Remove from group?')).not.toBeInTheDocument();

    // removeAssignmentMember called immediately
    await waitFor(() => {
      expect(mockApi.removeAssignmentMember).toHaveBeenCalledWith(PLAN_ID, ASSIGNMENT_ID, MEMBER_ID);
    });
  });
});
