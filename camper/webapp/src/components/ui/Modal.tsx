import { useEffect } from 'react';
import type { ReactNode } from 'react';
import '../Modal.css';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  flavor?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  closable?: boolean;
  className?: string;
  children: ReactNode;
}

export function Modal({ isOpen, onClose, title, flavor, size = 'md', closable = true, className, children }: ModalProps) {
  useEffect(() => {
    if (!isOpen || !closable) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isOpen, closable, onClose]);

  if (!isOpen) return null;

  const sizeClass = size === 'sm' ? 'modal-content--sm' : size === 'lg' ? 'modal-content--lg' : size === 'xl' ? 'modal-content--xl' : '';

  return (
    <div className="modal-overlay" onClick={closable ? onClose : undefined}>
      <div className={`modal-content ${sizeClass} ${className || ''}`} onClick={e => e.stopPropagation()}>
        {closable && (
          <button className="modal-close-btn" onClick={onClose} title="Close">
            <svg width="20" height="20" viewBox="0 0 20 20">
              <line x1="4" y1="4" x2="16" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              <line x1="16" y1="4" x2="4" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        )}
        {(title || flavor) && (
          <div className="modal-header-section">
            {title && <h2 className="modal-title">{title}</h2>}
            {flavor && <p className="modal-flavor">{flavor}</p>}
          </div>
        )}
        {children}
      </div>
    </div>
  );
}
