import { useState } from 'react';
import './InteractableItem.css';

export interface BadgeContent {
  label: string;
  tone: 'progress' | 'complete' | 'empty';
}

interface Props {
  id: string;
  label: string;
  x: number;
  y: number;
  children: React.ReactNode;
  onClick: () => void;
  /** Optional progress badge overlaid on the top-right of the icon. */
  badge?: BadgeContent;
  /** Hover text for the badge (title + aria-label). Falls back to "${label}: ${badge.label}". */
  badgeTooltip?: string;
}

export function InteractableItem({ id, label, x, y, children, onClick, badge, badgeTooltip }: Props) {
  const [hovered, setHovered] = useState(false);

  return (
    <button
      className={`interactable-item ${hovered ? 'interactable-item--hovered' : ''}`}
      style={{ left: `${x}%`, top: `${y}%` }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onClick={onClick}
      data-item={id}
    >
      <div className="interactable-item__glow" />
      <div className="interactable-item__content">
        {children}
        {badge && (
          <span
            className={`interactable-item__badge interactable-item__badge--${badge.tone}`}
            title={badgeTooltip}
            aria-label={badgeTooltip || `${label}: ${badge.label}`}
          >
            {badge.label}
          </span>
        )}
      </div>
      <div className={`interactable-item__tooltip ${hovered ? 'interactable-item__tooltip--visible' : ''}`}>
        <span className="tooltip-icon">&#9733;</span>
        {label}
      </div>
    </button>
  );
}
