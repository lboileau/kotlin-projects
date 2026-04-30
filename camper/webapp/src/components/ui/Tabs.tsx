import { Children, isValidElement, useRef } from 'react';
import type { KeyboardEvent, ReactNode } from 'react';
import './Tabs.css';

export interface TabsProps {
  value: string;
  onChange: (value: string) => void;
  children: ReactNode;          // Expected: <Tab value label icon? />[]
  ariaLabel?: string;           // for the tablist's aria-label
  className?: string;           // host can add layout classes (e.g., login-card-header)
}

export interface TabProps {
  value: string;
  label: string;
  icon?: ReactNode;             // optional leading icon (SVG or character)
  disabled?: boolean;
}

/**
 * Declarative config child — Tabs introspects its props; renders nothing.
 * Usage: <Tab value="login" label="Sign In" />
 */
export function Tab(_props: TabProps) {
  return <></>;
}

/**
 * WAI-ARIA tablist primitive with roving tabindex and full keyboard navigation.
 *
 * Keyboard support:
 *   Arrow Left/Right — cycle through enabled tabs (wraps around)
 *   Home / End       — jump to first / last enabled tab
 *   Enter / Space    — activate focused tab (native button behavior)
 *   Click            — activate tab
 *
 * Disabled tabs are skipped by arrow-key cycling and are not clickable.
 */
export function Tabs({ value, onChange, children, ariaLabel, className }: TabsProps) {
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);

  // Introspect children to collect Tab configs
  const tabs: TabProps[] = [];
  Children.forEach(children, (child) => {
    if (isValidElement(child) && child.type === Tab) {
      tabs.push(child.props as TabProps);
    }
  });

  // Indices of tabs that can receive focus and activation
  const enabledIndices = tabs
    .map((t, i) => (!t.disabled ? i : -1))
    .filter((i): i is number => i !== -1);

  const handleKeyDown = (e: KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (enabledIndices.length === 0) return;

    const currentPos = enabledIndices.indexOf(index);
    let targetIndex: number | undefined;

    switch (e.key) {
      case 'ArrowRight':
        e.preventDefault();
        targetIndex = enabledIndices[(currentPos + 1) % enabledIndices.length];
        break;
      case 'ArrowLeft':
        e.preventDefault();
        targetIndex = enabledIndices[
          (currentPos - 1 + enabledIndices.length) % enabledIndices.length
        ];
        break;
      case 'Home':
        e.preventDefault();
        targetIndex = enabledIndices[0];
        break;
      case 'End':
        e.preventDefault();
        targetIndex = enabledIndices[enabledIndices.length - 1];
        break;
    }

    if (targetIndex !== undefined) {
      tabRefs.current[targetIndex]?.focus();
      onChange(tabs[targetIndex].value);
    }
  };

  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      className={`ui-tabs${className ? ` ${className}` : ''}`}
    >
      {tabs.map((tab, i) => {
        const isActive = tab.value === value;
        return (
          <button
            key={tab.value}
            ref={el => { tabRefs.current[i] = el; }}
            role="tab"
            aria-selected={isActive}
            tabIndex={isActive ? 0 : -1}
            className={`ui-tab${isActive ? ' ui-tab--active' : ''}`}
            disabled={tab.disabled}
            onClick={() => {
              if (!tab.disabled) onChange(tab.value);
            }}
            onKeyDown={e => handleKeyDown(e, i)}
          >
            {tab.icon !== undefined && (
              <span className="ui-tab-icon">{tab.icon}</span>
            )}
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
