import { useState, useRef } from 'react';
import { Button } from './ui/Button';
import { Input } from './ui/Input';
import { Modal } from './ui/Modal';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (email: string) => Promise<void>;
}

export function AddMemberModal({ isOpen, onClose, onAdd }: Props) {
  const [emails, setEmails] = useState<string[]>(['']);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<number, string>>({});
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const addRow = () => {
    setEmails([...emails, '']);
    setTimeout(() => inputRefs.current[emails.length]?.focus(), 0);
  };

  const removeRow = (index: number) => {
    if (emails.length === 1) return;
    setEmails(emails.filter((_, i) => i !== index));
    setErrors(prev => {
      const next = { ...prev };
      delete next[index];
      // Re-key errors above the removed index
      const reKeyed: Record<number, string> = {};
      for (const [k, v] of Object.entries(next)) {
        const ki = Number(k);
        reKeyed[ki > index ? ki - 1 : ki] = v;
      }
      return reKeyed;
    });
  };

  const updateEmail = (index: number, value: string) => {
    const updated = [...emails];
    updated[index] = value;
    setEmails(updated);
    if (errors[index]) {
      setErrors(prev => {
        const next = { ...prev };
        delete next[index];
        return next;
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = emails.map(em => em.trim()).filter(Boolean);
    if (trimmed.length === 0) return;

    setLoading(true);
    setErrors({});

    const newErrors: Record<number, string> = {};
    for (let i = 0; i < emails.length; i++) {
      const em = emails[i].trim();
      if (!em) continue;
      try {
        await onAdd(em);
      } catch (err) {
        newErrors[i] = err instanceof Error ? err.message : 'Failed to invite';
      }
    }

    setLoading(false);
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      // Remove succeeded rows
      const failedEmails: string[] = [];
      const failedErrors: Record<number, string> = {};
      let j = 0;
      for (let i = 0; i < emails.length; i++) {
        if (newErrors[i]) {
          failedEmails.push(emails[i]);
          failedErrors[j] = newErrors[i];
          j++;
        }
      }
      setEmails(failedEmails.length > 0 ? failedEmails : ['']);
      setErrors(failedEmails.length > 0 ? failedErrors : {});
      if (failedEmails.length === 0) {
        onClose();
      }
    } else {
      setEmails(['']);
      onClose();
    }
  };

  const hasValidEmail = emails.some(em => em.trim().length > 0);

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
        <div className="modal-icon-large">
          <svg width="48" height="48" viewBox="0 0 48 48">
            <circle cx="24" cy="16" r="10" fill="var(--lavender)" />
            <rect x="14" y="28" width="20" height="16" rx="6" fill="var(--lavender)" />
            <circle cx="38" cy="12" r="8" fill="none" stroke="var(--mint)" strokeWidth="2" />
            <line x1="38" y1="8" x2="38" y2="16" stroke="var(--mint)" strokeWidth="2" strokeLinecap="round" />
            <line x1="34" y1="12" x2="42" y2="12" stroke="var(--mint)" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </div>
        <h2 className="modal-title">Invite Adventurers</h2>
        <p className="modal-subtitle">Enter their emails to summon them to the campfire.</p>

        <form onSubmit={handleSubmit} className="modal-form">
          {emails.map((email, i) => (
            <div key={i} className="modal-email-row">
              <div className="modal-email-input-wrap">
                <Input
                  ref={el => { inputRefs.current[i] = el; }}
                  type="email"
                  placeholder="fellow.adventurer@email.com"
                  value={email}
                  onChange={e => updateEmail(i, e.target.value)}
                  autoFocus={i === 0}
                  required={emails.length === 1}
                />
                {emails.length > 1 && (
                  <button
                    type="button"
                    className="modal-email-remove"
                    onClick={() => removeRow(i)}
                    title="Remove"
                  >
                    ×
                  </button>
                )}
              </div>
              {errors[i] && <p className="modal-error">{errors[i]}</p>}
            </div>
          ))}
          <button type="button" className="modal-add-email" onClick={addRow}>
            + Add another email
          </button>
          <div className="modal-actions">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading || !hasValidEmail}>
              {loading ? 'Summoning...' : emails.filter(e => e.trim()).length > 1 ? 'Send Invitations' : 'Send Invitation'}
            </Button>
          </div>
        </form>
    </Modal>
  );
}
