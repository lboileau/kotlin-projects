import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeftIcon, PersonIcon } from '@radix-ui/react-icons';
import { usePageTitle } from '../lib/usePageTitle';
import './PageHeader.css';

interface PageHeaderProps {
  title: string;
  /** Path to navigate to when the back button is tapped. Omit to hide it. */
  backTo?: string;
  actions?: ReactNode;
  /** Set false to hide the account avatar button (e.g. on the Account page itself). */
  showAccount?: boolean;
}

/** Also sets document.title to match — every page using this gets that for free. */
export function PageHeader({ title, backTo, actions, showAccount = true }: PageHeaderProps) {
  const navigate = useNavigate();
  usePageTitle(title);

  return (
    <header className="page-header">
      <div className="page-header__leading">
        {backTo !== undefined && (
          <button
            type="button"
            className="page-header__back"
            aria-label="Back"
            onClick={() => navigate(backTo)}
          >
            <ArrowLeftIcon />
          </button>
        )}
        <h1 className="page-header__title">{title}</h1>
      </div>
      <div className="page-header__trailing">
        {actions}
        {showAccount && (
          <Link to="/account" className="page-header__account" aria-label="Account">
            <PersonIcon />
          </Link>
        )}
      </div>
    </header>
  );
}
