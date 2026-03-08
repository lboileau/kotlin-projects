import { useState } from 'react';
import './InteractableItem.css';

interface Props {
  id: string;
  label: string;
  x: number;
  y: number;
  children: React.ReactNode;
  onClick: () => void;
}

export function InteractableItem({ id, label, x, y, children, onClick }: Props) {
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
      </div>
      <div className={`interactable-item__tooltip ${hovered ? 'interactable-item__tooltip--visible' : ''}`}>
        <span className="tooltip-icon">&#9733;</span>
        {label}
      </div>
    </button>
  );
}
