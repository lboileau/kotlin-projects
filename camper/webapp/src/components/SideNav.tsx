import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './SideNav.css';

export function SideNav() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const go = (path: string) => {
    navigate(path);
    setOpen(false);
  };

  const isActive = (path: string) => location.pathname === path;
  const isFoodActive = location.pathname === '/recipes' || location.pathname === '/ingredients';

  return (
    <>
      {/* Hamburger — always rendered, fades out when nav is open */}
      <button
        className={`sidenav-toggle ${open ? 'sidenav-toggle--hidden' : ''}`}
        onClick={() => setOpen(true)}
        aria-label="Open navigation"
        tabIndex={open ? -1 : 0}
      >
        <span className="sidenav-toggle__bar" />
        <span className="sidenav-toggle__bar" />
        <span className="sidenav-toggle__bar" />
      </button>

      <nav className={`sidenav ${open ? 'sidenav--open' : ''}`}>
        {/* Brand + close button */}
        <div className="sidenav__header">
          <div className="sidenav__brand">
            <svg width="24" height="24" viewBox="0 0 28 28">
              <polygon points="14,2 4,24 24,24" fill="none" stroke="var(--ember)" strokeWidth="1.5" />
              <path d="M11,19 Q13,13 14,11 Q15,13 17,19" fill="var(--ember)" opacity="0.7" />
            </svg>
            <span>Camper</span>
          </div>
          <button className="sidenav__close" onClick={() => setOpen(false)} aria-label="Close navigation">
            <svg width="18" height="18" viewBox="0 0 18 18">
              <line x1="4" y1="4" x2="14" y2="14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              <line x1="14" y1="4" x2="4" y2="14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        <ul className="sidenav__list">
          <li>
            <button
              className={`sidenav__item ${isActive('/') ? 'sidenav__item--active' : ''}`}
              onClick={() => go('/')}
            >
              <svg width="18" height="18" viewBox="0 0 18 18" className="sidenav__icon">
                <path d="M3,15 L3,8 L9,3 L15,8 L15,15 Z" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round" />
              </svg>
              Trips
            </button>
          </li>
          <li>
            <div className={`sidenav__group ${isFoodActive ? 'sidenav__group--active' : ''}`}>
              <span className="sidenav__group-label">
                <svg width="18" height="18" viewBox="0 0 18 18" className="sidenav__icon">
                  <rect x="3" y="4" width="12" height="10" rx="1.5" fill="none" stroke="currentColor" strokeWidth="1.3" />
                  <line x1="6" y1="7" x2="12" y2="7" stroke="currentColor" strokeWidth="1" strokeLinecap="round" opacity="0.7" />
                  <line x1="6" y1="9.5" x2="12" y2="9.5" stroke="currentColor" strokeWidth="1" strokeLinecap="round" opacity="0.5" />
                  <line x1="6" y1="12" x2="10" y2="12" stroke="currentColor" strokeWidth="1" strokeLinecap="round" opacity="0.3" />
                </svg>
                Food
              </span>
              <ul className="sidenav__sublist">
                <li>
                  <button
                    className={`sidenav__item sidenav__item--nested ${isActive('/recipes') ? 'sidenav__item--active' : ''}`}
                    onClick={() => go('/recipes')}
                  >
                    Recipes
                  </button>
                </li>
                <li>
                  <button
                    className={`sidenav__item sidenav__item--nested ${isActive('/ingredients') ? 'sidenav__item--active' : ''}`}
                    onClick={() => go('/ingredients')}
                  >
                    Ingredients
                  </button>
                </li>
              </ul>
            </div>
          </li>
        </ul>
      </nav>
    </>
  );
}
