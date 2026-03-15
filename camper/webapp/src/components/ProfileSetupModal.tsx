import type { User } from '../api/client';
import { AvatarHead } from './AvatarHead';
import { ProfileForm } from './ProfileForm';
import { Modal } from './ui/Modal';
import './ProfileSetupModal.css';

interface Props {
  isOpen: boolean;
  user: User;
  onComplete: (updatedUser: User) => void;
  onClose?: () => void;
}

export function ProfileSetupModal({ isOpen, user, onComplete, onClose }: Props) {
  const isEditMode = user.profileCompleted;

  return (
    <Modal isOpen={isOpen} onClose={onClose || (() => {})} closable={isEditMode && !!onClose} className="setup-modal">
        <div className="setup-header">
          {isEditMode ? (
            <AvatarHead avatar={user.avatar} size={56} />
          ) : (
            <svg width="48" height="48" viewBox="0 0 48 48" className="setup-icon">
              <polygon points="24,4 8,40 40,40" fill="none" stroke="var(--ember)" strokeWidth="2" />
              <path d="M20,32 Q22,24 24,20 Q26,24 28,32" fill="var(--ember)" opacity="0.7" />
              <circle cx="16" cy="14" r="1.5" fill="var(--butter)" opacity="0.6" />
              <circle cx="34" cy="18" r="1" fill="var(--butter)" opacity="0.4" />
            </svg>
          )}
          <h2 className="modal-title">{isEditMode ? 'Camp Profile' : 'Welcome, Adventurer!'}</h2>
          <p className="modal-flavor">{isEditMode ? 'Update your camp profile.' : 'Set up your camp profile before heading out.'}</p>
        </div>

        <div className="modal-divider">
          <svg width="120" height="12" viewBox="0 0 120 12">
            <path d="M0,6 Q30,0 60,6 Q90,12 120,6" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" />
          </svg>
        </div>

        <div className="setup-form">
          <ProfileForm
            user={user}
            onSave={onComplete}
            submitLabel={isEditMode ? 'Save Profile' : 'Start Adventuring'}
            markProfileCompleted
          />
        </div>
    </Modal>
  );
}
