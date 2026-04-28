// Toast list renderer — consumes ToastContext state; no business logic here.
// Rendered once at the top of the React tree (sibling to <Routes> in App.tsx).

import { useToastContext } from '../../context/ToastContext';
import type { ToastItem } from '../../context/toastReducer';
import './Toast.css';

// ─── Container ───────────────────────────────────────────────────────────────

export function Toast() {
  const { toasts } = useToastContext();
  if (toasts.length === 0) return null;

  return (
    <div
      className="toast-container"
      role="region"
      aria-label="Notifications"
      aria-live="polite"
      aria-atomic="false"
    >
      {toasts.map(toast => (
        <ToastItemComponent key={toast.id} toast={toast} />
      ))}
    </div>
  );
}

// ─── Individual toast item ────────────────────────────────────────────────────

interface ToastItemProps {
  toast: ToastItem;
}

function ToastItemComponent({ toast }: ToastItemProps) {
  const { dismiss, pause, resume } = useToastContext();

  const handleAction = () => {
    toast.action?.onClick();
    dismiss(toast.id);
  };

  return (
    <div
      className={`toast-item toast-item--${toast.variant}`}
      role="alert"
      aria-live={toast.variant === 'error' ? 'assertive' : 'polite'}
      onMouseEnter={() => pause(toast.id)}
      onMouseLeave={() => resume(toast.id)}
    >
      {/* Variant icon */}
      <span className="toast-icon" aria-hidden="true">
        {toast.variant === 'success' && <SuccessIcon />}
        {toast.variant === 'error'   && <ErrorIcon />}
        {toast.variant === 'info'    && <InfoIcon />}
      </span>

      <div className="toast-body">
        <span className="toast-message">{toast.message}</span>
        {toast.action && (
          <button type="button" className="toast-action" onClick={handleAction}>
            {toast.action.label}
          </button>
        )}
      </div>

      {/* Close button */}
      <button
        type="button"
        className="toast-close"
        aria-label="Dismiss notification"
        onClick={() => dismiss(toast.id)}
      >
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
          <line x1="1" y1="1" x2="11" y2="11" />
          <line x1="11" y1="1" x2="1"  y2="11" />
        </svg>
      </button>
    </div>
  );
}

// ─── Icons (inline SVG, 18×18, stroke currentColor) ─────────────────────────

function SuccessIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="9" r="7.5" />
      <polyline points="5.5,9 7.5,11.5 12.5,6.5" />
    </svg>
  );
}

function ErrorIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
      <circle cx="9" cy="9" r="7.5" />
      <line x1="9" y1="5.5" x2="9" y2="10" />
      <circle cx="9" cy="12.5" r="0.75" fill="currentColor" stroke="none" />
    </svg>
  );
}

function InfoIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
      <circle cx="9" cy="9" r="7.5" />
      <line x1="9" y1="8" x2="9" y2="12.5" />
      <circle cx="9" cy="5.5" r="0.75" fill="currentColor" stroke="none" />
    </svg>
  );
}
