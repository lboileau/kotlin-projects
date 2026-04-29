import { useState, useEffect, useRef, useId } from 'react';
import { Button } from './Button';
import './InfoPopover.css';

interface InfoPopoverProps {
  items: string[];
  ariaLabel: string;
  triggerSize?: 'sm' | 'md';
}

export function InfoPopover({ items, ariaLabel, triggerSize = 'sm' }: InfoPopoverProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isPinned, setIsPinned] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Outside click: close when click is outside the container while open.
  useEffect(() => {
    if (!isOpen) return;

    function handleMouseDown(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
        setIsPinned(false);
      }
    }

    document.addEventListener('mousedown', handleMouseDown);
    return () => document.removeEventListener('mousedown', handleMouseDown);
  }, [isOpen]);

  // Escape: close when Escape is pressed while open.
  useEffect(() => {
    if (!isOpen) return;

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setIsOpen(false);
        setIsPinned(false);
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  function handleMouseEnter() {
    setIsOpen(true);
  }

  function handleMouseLeave() {
    // Only close if not pinned open by a click.
    if (!isPinned) {
      setIsOpen(false);
    }
  }

  function handleClick() {
    if (isOpen && isPinned) {
      // Second click: unpin and close.
      setIsOpen(false);
      setIsPinned(false);
    } else {
      // First click: pin open.
      setIsOpen(true);
      setIsPinned(true);
    }
  }

  const uid = useId();
  const popoverId = `info-popover-${uid}`;

  return (
    <div
      ref={containerRef}
      className="info-popover"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <Button
        variant="icon"
        size={triggerSize}
        type="button"
        className="info-popover__trigger"
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        aria-label={ariaLabel}
        aria-controls={isOpen ? popoverId : undefined}
        onClick={handleClick}
      >
        {/* Inline SVG: circle with "i" */}
        <svg
          width="16"
          height="16"
          viewBox="0 0 16 16"
          fill="none"
          aria-hidden="true"
        >
          <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1.5" />
          <line x1="8" y1="6.5" x2="8" y2="11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          <circle cx="8" cy="4.5" r="0.8" fill="currentColor" />
        </svg>
      </Button>

      {isOpen && (
        <div
          id={popoverId}
          role="dialog"
          aria-label={ariaLabel}
          className="info-popover__panel"
        >
          <div className="info-popover__pointer" aria-hidden="true" />
          <ul className="info-popover__list">
            {items.map((item, i) => (
              <li key={i} className="info-popover__item">
                {item}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
