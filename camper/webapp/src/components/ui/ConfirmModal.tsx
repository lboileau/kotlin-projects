import { useLayoutEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Modal } from './Modal';
import { Button } from './Button';
import './ConfirmModal.css';

export interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string | ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: 'danger' | 'default';
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
  /** Optional flavor copy under the title; matches Modal's existing `flavor` prop. */
  flavor?: string;
  /** Optional icon SVG (ReactNode) rendered above the title. */
  icon?: ReactNode;
}

export function ConfirmModal({
  isOpen,
  title,
  message,
  confirmLabel,
  cancelLabel = 'Cancel',
  tone = 'danger',
  onConfirm,
  onCancel,
  flavor,
  icon,
}: ConfirmModalProps) {
  const [pending, setPending] = useState(false);
  const actionsRef = useRef<HTMLDivElement>(null);

  // Auto-focus the cancel button (first in the actions bar) when modal opens.
  useLayoutEffect(() => {
    if (!isOpen) return;
    const cancelBtn = actionsRef.current?.querySelector<HTMLButtonElement>('button');
    cancelBtn?.focus();
  }, [isOpen]);

  // Reset pending state if modal is closed externally.
  useLayoutEffect(() => {
    if (!isOpen) setPending(false);
  }, [isOpen]);

  const handleConfirm = async () => {
    const result = onConfirm();
    if (result instanceof Promise) {
      setPending(true);
      try {
        await result;
      } finally {
        setPending(false);
      }
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onCancel}
      size="sm"
      closable={!pending}
      className="confirm-modal"
    >
      {icon && <div className="confirm-modal__icon">{icon}</div>}
      <h2 className="modal-title">{title}</h2>
      {flavor && <p className="modal-flavor">{flavor}</p>}
      <div className="confirm-modal__message">{message}</div>
      <div ref={actionsRef} className="modal-actions">
        <Button
          variant="secondary"
          size="sm"
          disabled={pending}
          onClick={onCancel}
        >
          {cancelLabel}
        </Button>
        <Button
          variant={tone === 'default' ? 'primary' : 'danger'}
          size="sm"
          loading={pending}
          onClick={handleConfirm}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
