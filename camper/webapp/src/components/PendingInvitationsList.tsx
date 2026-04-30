import { useState } from 'react';
import type { PlanMember } from '../api/client';
import { Button } from './ui/Button';
import { ConfirmModal } from './ui/ConfirmModal';
import { AvatarHead } from './AvatarHead';
import { useToast } from '../context/ToastContext';
import './PendingInvitationsList.css';

const RESEND_ENABLED_STATUSES = new Set(['pending', 'failed', 'bounced']);

function statusLabel(status: string | null): string {
  switch (status) {
    case 'pending':   return 'Pending';
    case 'sent':      return 'Sent';
    case 'delivered': return 'Delivered';
    case 'delayed':   return 'Delayed';
    case 'failed':    return 'Failed';
    case 'bounced':   return 'Bounced';
    case 'complained': return 'Marked as spam';
    default:          return 'Unknown';
  }
}

function passiveLabel(status: string | null): string | null {
  switch (status) {
    case 'sent':
    case 'delivered':
      return 'Already delivered — ask them to check spam';
    case 'delayed':
      return 'Delivery delayed by recipient mail server';
    case 'complained':
      return 'Recipient flagged as spam — try a different email';
    default:
      return null;
  }
}

export interface PendingInvitationsListProps {
  members: PlanMember[];
  planId: string;
  onResend: (email: string) => Promise<void>;
  onRemove: (memberId: string) => Promise<void>;
}

export function PendingInvitationsList({
  members,
  onResend,
  onRemove,
}: PendingInvitationsListProps) {
  const toast = useToast();
  const pending = members.filter(m => !m.username && m.email);
  const [resending, setResending] = useState<string | null>(null);
  const [removingMember, setRemovingMember] = useState<PlanMember | null>(null);

  if (pending.length === 0) return null;

  const handleResend = async (member: PlanMember) => {
    if (!member.email) return;
    setResending(member.userId);
    try {
      await onResend(member.email);
      toast.success(`Invitation resent to ${member.email}`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to resend invitation');
    } finally {
      setResending(null);
    }
  };

  return (
    <div className="pending-invitations">
      <span className="manage-plan-setting-label">
        Pending invitations ({pending.length})
      </span>
      <div className="pending-invitations__list">
        {pending.map(member => {
          const canResend = RESEND_ENABLED_STATUSES.has(member.invitationStatus ?? '');
          const passive = passiveLabel(member.invitationStatus);
          const sent = new Date(member.createdAt).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
          });
          return (
            <div key={member.userId} className="pending-invitation-row">
              <AvatarHead
                avatar={member.avatar}
                invitationStatus={member.invitationStatus}
              />
              <div className="pending-invitation-row__info">
                <span className="pending-invitation-row__email">{member.email}</span>
                <span className="pending-invitation-row__meta">
                  Invited {sent} · {statusLabel(member.invitationStatus)}
                </span>
                {passive && (
                  <span className="pending-invitation-row__passive">{passive}</span>
                )}
              </div>
              <div className="pending-invitation-row__actions">
                {canResend && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => handleResend(member)}
                    disabled={resending === member.userId}
                  >
                    {resending === member.userId ? 'Sending…' : 'Resend invite'}
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setRemovingMember(member)}
                  aria-label={`Remove invitation for ${member.email}`}
                >
                  Remove
                </Button>
              </div>
            </div>
          );
        })}
      </div>

      <ConfirmModal
        isOpen={!!removingMember}
        title="Cancel this invitation?"
        message={`The invitation to ${removingMember?.email ?? ''} will be canceled. You can re-send later.`}
        confirmLabel="Cancel Invite"
        cancelLabel="Keep It"
        tone="danger"
        onConfirm={async () => {
          if (removingMember) {
            await onRemove(removingMember.userId);
            setRemovingMember(null);
          }
        }}
        onCancel={() => setRemovingMember(null)}
      />
    </div>
  );
}
