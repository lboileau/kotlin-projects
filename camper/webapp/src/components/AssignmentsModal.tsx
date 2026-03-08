import { useState, useEffect, useCallback, useRef } from 'react';
import { api, type AssignmentDetail, type PlanMember } from '../api/client';
import './Modal.css';
import './AssignmentsModal.css';

interface AssignmentsModalProps {
  isOpen: boolean;
  onClose: () => void;
  planId: string;
  planOwnerId: string;
  currentUserId: string;
  members: PlanMember[];
}

const MEMBER_COLORS = [
  'var(--lavender)',
  'var(--sage)',
  'var(--rose)',
  'var(--mint)',
  'var(--butter)',
  'var(--tan)',
];

function getMemberColor(userId: string, allMembers: PlanMember[]): string {
  const idx = allMembers.findIndex(m => m.userId === userId);
  return MEMBER_COLORS[(idx === -1 ? 0 : idx) % MEMBER_COLORS.length];
}

function getMemberName(userId: string, members: PlanMember[]): string {
  const m = members.find(m => m.userId === userId);
  return m?.username || 'Pending Adventurer';
}

interface CreateFormData {
  name: string;
  maxOccupancy: number;
}

interface EditFormData {
  name: string;
  maxOccupancy: number;
}

interface AssignmentCardProps {
  assignment: AssignmentDetail;
  planOwnerId: string;
  currentUserId: string;
  planMembers: PlanMember[];
  onJoin: (assignmentId: string) => void;
  onLeave: (assignmentId: string) => void;
  onRemoveMember: (assignmentId: string, userId: string) => void;
  onDelete: (assignmentId: string) => void;
  onUpdate: (assignmentId: string, name: string, maxOccupancy: number) => void;
}

function AssignmentCard({
  assignment,
  planOwnerId,
  currentUserId,
  planMembers,
  onJoin,
  onLeave,
  onRemoveMember,
  onDelete,
  onUpdate,
}: AssignmentCardProps) {
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState<EditFormData>({ name: assignment.name, maxOccupancy: assignment.maxOccupancy });
  const inputRef = useRef<HTMLInputElement>(null);

  const isAssignmentOwner = assignment.ownerId === currentUserId;
  const isPlanOwner = planOwnerId === currentUserId;
  const canManage = isAssignmentOwner || isPlanOwner;
  const isMember = assignment.members.some(m => m.userId === currentUserId);
  const spotsUsed = assignment.members.length;
  const spotsAvailable = assignment.maxOccupancy - spotsUsed;
  const ownerName = getMemberName(assignment.ownerId, planMembers);

  useEffect(() => {
    if (editing && inputRef.current) inputRef.current.focus();
  }, [editing]);

  const handleStartEdit = () => {
    setEditForm({ name: assignment.name, maxOccupancy: assignment.maxOccupancy });
    setEditing(true);
  };

  const handleSaveEdit = () => {
    if (editForm.name.trim()) {
      onUpdate(assignment.id, editForm.name.trim(), editForm.maxOccupancy);
      setEditing(false);
    }
  };

  const handleCancelEdit = () => {
    setEditForm({ name: assignment.name, maxOccupancy: assignment.maxOccupancy });
    setEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSaveEdit();
    if (e.key === 'Escape') handleCancelEdit();
  };

  const fillPct = assignment.maxOccupancy > 0 ? (spotsUsed / assignment.maxOccupancy) * 100 : 0;

  return (
    <div className="assign-card">
      {/* Card header */}
      <div className="assign-card-header">
        {editing ? (
          <input
            ref={inputRef}
            className="assign-card-edit-name"
            value={editForm.name}
            onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))}
            onKeyDown={handleKeyDown}
            placeholder="Group name"
          />
        ) : (
          <h4 className="assign-card-name">{assignment.name}</h4>
        )}
        {canManage && !editing && (
          <div className="assign-card-actions">
            <button className="assign-action-btn" title="Edit" onClick={handleStartEdit}>
              <svg width="14" height="14" viewBox="0 0 14 14">
                <path d="M10,2 L12,4 L5,11 L3,11 L3,9 Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
              </svg>
            </button>
            <button className="assign-action-btn assign-action-btn--danger" title="Delete" onClick={() => onDelete(assignment.id)}>
              <svg width="14" height="14" viewBox="0 0 14 14">
                <path d="M3,4 L11,4 L10,12 L4,12 Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
                <line x1="2" y1="4" x2="12" y2="4" stroke="currentColor" strokeWidth="1.2" />
                <line x1="5" y1="2" x2="9" y2="2" stroke="currentColor" strokeWidth="1.2" />
              </svg>
            </button>
          </div>
        )}
        {editing && (
          <div className="assign-card-actions">
            <button className="assign-save-btn" onClick={handleSaveEdit}>&#10003;</button>
            <button className="assign-cancel-btn" onClick={handleCancelEdit}>&#10005;</button>
          </div>
        )}
      </div>

      {/* Owner badge */}
      <span className="assign-card-owner">
        Created by {assignment.ownerId === currentUserId ? 'you' : ownerName}
      </span>

      {/* Occupancy */}
      <div className="assign-card-occupancy">
        {editing ? (
          <div className="assign-card-edit-occupancy">
            <span className="assign-card-edit-label">Max spots:</span>
            <div className="assign-occupancy-stepper">
              <button
                type="button"
                className="assign-qty-btn"
                onClick={() => setEditForm(f => ({ ...f, maxOccupancy: Math.max(1, f.maxOccupancy - 1) }))}
              >
                &#8722;
              </button>
              <span className="assign-qty-value">{editForm.maxOccupancy}</span>
              <button
                type="button"
                className="assign-qty-btn"
                onClick={() => setEditForm(f => ({ ...f, maxOccupancy: f.maxOccupancy + 1 }))}
              >
                +
              </button>
            </div>
          </div>
        ) : (
          <>
            <span className="assign-card-spots">{spotsUsed}/{assignment.maxOccupancy} spots</span>
            <div className="assign-occupancy-bar">
              <div className="assign-occupancy-fill" style={{ width: `${fillPct}%` }} />
            </div>
          </>
        )}
      </div>

      {/* Members list */}
      <div className="assign-card-members">
        {assignment.members.map(member => {
          const color = getMemberColor(member.userId, planMembers);
          const name = member.username || 'Pending Adventurer';
          const isMe = member.userId === currentUserId;
          const isOwnerMember = member.userId === assignment.ownerId;
          const canRemove = canManage && !isOwnerMember && !isMe;
          return (
            <div key={member.userId} className="assign-member">
              <span className="assign-member-dot" style={{ background: color }} />
              <span className="assign-member-name">{name}{isMe ? ' (You)' : ''}</span>
              {isOwnerMember && <span className="assign-member-badge">owner</span>}
              {canRemove && (
                <button
                  className="assign-member-remove"
                  title={`Remove ${name}`}
                  onClick={() => onRemoveMember(assignment.id, member.userId)}
                >
                  <svg width="12" height="12" viewBox="0 0 12 12">
                    <path d="M3,3 L9,9 M9,3 L3,9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                  </svg>
                </button>
              )}
            </div>
          );
        })}
      </div>

      {/* Join / Leave buttons */}
      <div className="assign-card-footer">
        {!isMember && spotsAvailable > 0 && (
          <button className="assign-join-btn" onClick={() => onJoin(assignment.id)}>
            Join
          </button>
        )}
        {!isMember && spotsAvailable <= 0 && (
          <span className="assign-full-badge">Full</span>
        )}
        {isMember && !isAssignmentOwner && (
          <button className="assign-leave-btn" onClick={() => onLeave(assignment.id)}>
            Leave
          </button>
        )}
      </div>
    </div>
  );
}

export function AssignmentsModal({ isOpen, onClose, planId, planOwnerId, currentUserId, members }: AssignmentsModalProps) {
  const [assignments, setAssignments] = useState<AssignmentDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState<'tent' | 'canoe'>('tent');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createForm, setCreateForm] = useState<CreateFormData>({ name: '', maxOccupancy: 4 });
  const [creating, setCreating] = useState(false);
  const createInputRef = useRef<HTMLInputElement>(null);

  const loadAssignments = useCallback(async () => {
    try {
      setError('');
      // Fetch all assignments for the plan, then get details for each
      const list = await api.getAssignments(planId);
      const details = await Promise.all(
        list.map(a => api.getAssignment(planId, a.id))
      );
      setAssignments(details);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load assignments');
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      loadAssignments();
    }
  }, [isOpen, loadAssignments]);

  useEffect(() => {
    if (showCreateForm && createInputRef.current) {
      createInputRef.current.focus();
    }
  }, [showCreateForm]);

  // Reset create form defaults when tab changes
  useEffect(() => {
    setCreateForm({ name: '', maxOccupancy: activeTab === 'tent' ? 4 : 2 });
    setShowCreateForm(false);
  }, [activeTab]);

  if (!isOpen) return null;

  const filteredAssignments = assignments.filter(a => a.type === activeTab);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.name.trim()) return;
    setCreating(true);
    setError('');
    try {
      await api.createAssignment(planId, {
        name: createForm.name.trim(),
        type: activeTab,
        maxOccupancy: createForm.maxOccupancy,
      });
      setCreateForm({ name: '', maxOccupancy: activeTab === 'tent' ? 4 : 2 });
      setShowCreateForm(false);
      await loadAssignments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create group');
    } finally {
      setCreating(false);
    }
  };

  const handleJoin = async (assignmentId: string) => {
    setError('');
    try {
      await api.addAssignmentMember(planId, assignmentId, currentUserId);
      await loadAssignments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to join group');
    }
  };

  const handleLeave = async (assignmentId: string) => {
    setError('');
    try {
      await api.removeAssignmentMember(planId, assignmentId, currentUserId);
      await loadAssignments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to leave group');
    }
  };

  const handleRemoveMember = async (assignmentId: string, userId: string) => {
    setError('');
    try {
      await api.removeAssignmentMember(planId, assignmentId, userId);
      await loadAssignments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove member');
    }
  };

  const handleDelete = async (assignmentId: string) => {
    setError('');
    try {
      await api.deleteAssignment(planId, assignmentId);
      await loadAssignments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete group');
    }
  };

  const handleUpdate = async (assignmentId: string, name: string, maxOccupancy: number) => {
    setError('');
    try {
      await api.updateAssignment(planId, assignmentId, { name, maxOccupancy });
      await loadAssignments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update group');
    }
  };

  const emptyText = activeTab === 'tent'
    ? 'No tents set up yet \u2014 pitch the first one!'
    : 'No canoes launched yet \u2014 paddle up!';

  return (
    <div className="modal-overlay assign-modal-overlay" onClick={onClose}>
      <div className="assign-modal" onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="assign-modal-header">
          <div className="assign-modal-header-left">
            <div className="assign-modal-icon">
              <svg width="36" height="36" viewBox="0 0 48 48">
                <polygon points="24,4 6,36 42,36" fill="var(--rose)" stroke="var(--rose-deep)" strokeWidth="2" />
                <polygon points="24,4 24,36 42,36" fill="var(--rose-deep)" opacity="0.4" />
                <path d="M19,36 Q22,24 24,20 Q26,24 29,36" fill="var(--rose-deep)" />
                <path d="M6,44 Q16,38 24,44 Q32,38 42,44" fill="none" stroke="var(--sage-deep)" strokeWidth="2.5" strokeLinecap="round" />
              </svg>
            </div>
            <div>
              <h2 className="assign-modal-title">Camp Assignments</h2>
              <p className="assign-modal-subtitle">
                {assignments.length === 0
                  ? 'Organize your crew into groups'
                  : `${assignments.length} group${assignments.length !== 1 ? 's' : ''} set up`}
              </p>
            </div>
          </div>
          <button className="assign-modal-close" onClick={onClose} aria-label="Close">
            <svg width="20" height="20" viewBox="0 0 20 20">
              <path d="M5,5 L15,15 M15,5 L5,15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        {/* Tab bar */}
        <div className="assign-tabs">
          <button
            className={`assign-tab ${activeTab === 'tent' ? 'assign-tab--active' : ''}`}
            onClick={() => setActiveTab('tent')}
          >
            <span className="assign-tab-icon">{'\u25B3'}</span>
            Tents
          </button>
          <button
            className={`assign-tab ${activeTab === 'canoe' ? 'assign-tab--active' : ''}`}
            onClick={() => setActiveTab('canoe')}
          >
            <span className="assign-tab-icon">{'\u25E0'}</span>
            Canoes
          </button>
        </div>

        {/* Body */}
        <div className="assign-modal-body">
          {loading ? (
            <div className="assign-loading">
              <div className="assign-loading-tent" />
              <p>Scouting camp sites...</p>
            </div>
          ) : (
            <>
              {error && <p className="assign-error">{error}</p>}

              {filteredAssignments.length === 0 && !showCreateForm && (
                <div className="assign-empty">
                  <svg width="64" height="64" viewBox="0 0 64 64" className="assign-empty-icon">
                    {activeTab === 'tent' ? (
                      <>
                        <polygon points="32,10 8,52 56,52" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" strokeDasharray="4,3" />
                        <path d="M26,52 Q30,38 32,32 Q34,38 38,52" fill="none" stroke="var(--tan-deep)" strokeWidth="1" strokeDasharray="3,2" />
                      </>
                    ) : (
                      <>
                        <path d="M8,36 Q32,16 56,36" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" strokeDasharray="4,3" />
                        <line x1="16" y1="32" x2="16" y2="24" stroke="var(--tan-deep)" strokeWidth="1" strokeDasharray="3,2" />
                        <line x1="48" y1="32" x2="48" y2="24" stroke="var(--tan-deep)" strokeWidth="1" strokeDasharray="3,2" />
                      </>
                    )}
                  </svg>
                  <p className="assign-empty-text">{emptyText}</p>
                </div>
              )}

              {filteredAssignments.map(assignment => (
                <AssignmentCard
                  key={assignment.id}
                  assignment={assignment}
                  planOwnerId={planOwnerId}
                  currentUserId={currentUserId}
                  planMembers={members}
                  onJoin={handleJoin}
                  onLeave={handleLeave}
                  onRemoveMember={handleRemoveMember}
                  onDelete={handleDelete}
                  onUpdate={handleUpdate}
                />
              ))}

              {/* Inline create form */}
              {showCreateForm && (
                <form className="assign-create-form" onSubmit={handleCreate}>
                  <h4 className="assign-create-title">
                    New {activeTab === 'tent' ? 'Tent' : 'Canoe'} Group
                  </h4>
                  <input
                    ref={createInputRef}
                    className="assign-create-input"
                    value={createForm.name}
                    onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))}
                    placeholder={activeTab === 'tent' ? 'e.g. Big Agnes Copper Spur' : 'e.g. Old Town Discovery'}
                    disabled={creating}
                  />
                  <div className="assign-create-occupancy">
                    <span className="assign-create-label">Max spots:</span>
                    <div className="assign-occupancy-stepper">
                      <button
                        type="button"
                        className="assign-qty-btn"
                        onClick={() => setCreateForm(f => ({ ...f, maxOccupancy: Math.max(1, f.maxOccupancy - 1) }))}
                        disabled={creating}
                      >
                        &#8722;
                      </button>
                      <span className="assign-qty-value">{createForm.maxOccupancy}</span>
                      <button
                        type="button"
                        className="assign-qty-btn"
                        onClick={() => setCreateForm(f => ({ ...f, maxOccupancy: f.maxOccupancy + 1 }))}
                        disabled={creating}
                      >
                        +
                      </button>
                    </div>
                  </div>
                  <div className="assign-create-actions">
                    <button
                      type="button"
                      className="modal-btn modal-btn--secondary"
                      onClick={() => { setShowCreateForm(false); setCreateForm({ name: '', maxOccupancy: activeTab === 'tent' ? 4 : 2 }); }}
                      disabled={creating}
                    >
                      Cancel
                    </button>
                    <button type="submit" className="modal-btn" disabled={creating || !createForm.name.trim()}>
                      {creating ? 'Creating...' : 'Create'}
                    </button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="assign-modal-footer">
          {!showCreateForm && !loading && (
            <button className="modal-btn assign-create-btn" onClick={() => setShowCreateForm(true)}>
              <svg width="14" height="14" viewBox="0 0 14 14">
                <line x1="7" y1="2" x2="7" y2="12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <line x1="2" y1="7" x2="12" y2="7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              Create Group
            </button>
          )}
          <button className="modal-btn modal-btn--secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
