import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { AvatarHead } from './AvatarHead';
import './AppHeader.css';
import type { ReactNode } from 'react';

interface AppHeaderProps {
  pageTitle: string;
  pageIcon?: ReactNode;
  actions?: ReactNode;
  onProfileClick?: () => void;
}

export function AppHeader({ pageTitle, pageIcon, actions, onProfileClick }: AppHeaderProps) {
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
        <button className="app-header__user-btn" onClick={onProfileClick || (() => navigate('/account'))}>
          <AvatarHead avatar={user?.avatar} size={28} />
          <span className="app-header__user-name">{user?.username || user?.email}</span>
        </button>
        <button className="app-header__logout" onClick={logout}>Log Out</button>
      </div>
    </header>
  );
}
