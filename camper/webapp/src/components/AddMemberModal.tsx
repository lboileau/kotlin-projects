import { useState } from 'react';
import './Modal.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (email: string) => Promise<void>;
}

export function AddMemberModal({ isOpen, onClose, onAdd }: Props) {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    setLoading(true);
    setError('');
    try {
      await onAdd(email.trim());
      setEmail('');
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to invite adventurer');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-icon-large">
          <svg width="48" height="48" viewBox="0 0 48 48">
            <circle cx="24" cy="16" r="10" fill="var(--lavender)" />
            <rect x="14" y="28" width="20" height="16" rx="6" fill="var(--lavender)" />
            <circle cx="38" cy="12" r="8" fill="none" stroke="var(--mint)" strokeWidth="2" />
            <line x1="38" y1="8" x2="38" y2="16" stroke="var(--mint)" strokeWidth="2" strokeLinecap="round" />
            <line x1="34" y1="12" x2="42" y2="12" stroke="var(--mint)" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </div>
        <h2 className="modal-title">Invite an Adventurer</h2>
        <p className="modal-subtitle">Enter their email to summon them to the campfire.</p>

        <form onSubmit={handleSubmit} className="modal-form">
          <input
            type="email"
            className="modal-input"
            placeholder="fellow.adventurer@email.com"
            value={email}
            onChange={e => setEmail(e.target.value)}
            autoFocus
            required
          />
          {error && <p className="modal-error">{error}</p>}
          <div className="modal-actions">
            <button type="button" className="modal-btn modal-btn--secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="modal-btn" disabled={loading || !email.trim()}>
              {loading ? 'Summoning...' : 'Send Invitation'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
