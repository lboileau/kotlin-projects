import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AppHeader.css';
import type { ReactNode } from 'react';

interface AppHeaderProps {
  pageTitle: string;
  pageIcon?: ReactNode;
  actions?: ReactNode;
}

export function AppHeader({ pageTitle, pageIcon, actions }: AppHeaderProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="app-header">
      <div className="app-header__left">
        <svg width="24" height="24" viewBox="0 0 28 28" className="app-header__logo">
          <polygon points="14,2 4,24 24,24" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
          <path d="M11,19 Q13,13 14,11 Q15,13 17,19" fill="var(--ember)" opacity="0.7" />
        </svg>
        <span className="app-header__brand">Camper</span>
      </div>

      <div className="app-header__center">
        {pageIcon && <span className="app-header__page-icon">{pageIcon}</span>}
        <span className="app-header__page-title">{pageTitle}</span>
      </div>

      <div className="app-header__right">
        {actions}
        <button className="app-header__user-btn" onClick={() => navigate('/account')}>
          <svg width="28" height="28" viewBox="0 0 28 28" className="app-header__avatar">
            <defs><clipPath id="avatar-clip-header"><circle cx="14" cy="14" r="13" /></clipPath></defs>
            <circle cx="14" cy="14" r="13" fill="var(--sage)" stroke="var(--sage-deep)" strokeWidth="1.5" />
            <g clipPath="url(#avatar-clip-header)">
              <circle cx="14" cy="11" r="5" fill="var(--parchment)" />
              <ellipse cx="14" cy="24" rx="8" ry="6" fill="var(--parchment)" />
            </g>
          </svg>
          <span className="app-header__user-name">{user?.username || user?.email}</span>
        </button>
        <button className="app-header__logout" onClick={logout}>Log Out</button>
      </div>
    </header>
  );
}
