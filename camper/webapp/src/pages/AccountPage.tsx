import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
import { AvatarHead } from '../components/AvatarHead';
import { AppHeader } from '../components/AppHeader';
import { ProfileForm } from '../components/ProfileForm';
import './AccountPage.css';

export function AccountPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="account-page">
      <ParallaxBackground variant="dusk" />

      <div className="account-content">
        <AppHeader pageTitle="Your Camp Profile" />

        <div className="account-card-wrapper">
          <div className="account-card">
            <button className="account-close" onClick={() => navigate(-1)} title="Close">
              <svg width="20" height="20" viewBox="0 0 20 20">
                <line x1="4" y1="4" x2="16" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <line x1="16" y1="4" x2="4" y2="16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </button>
            <div className="account-avatar">
              <AvatarHead avatar={user?.avatar} size={72} />
            </div>
            <h1 className="account-title">Your Camp Profile</h1>

            {user && (
              <ProfileForm
                user={user}
                onSave={login}
                onAvatarChange={login}
                submitLabel="Update Profile"
                showEmail
              />
            )}

            <p className="account-meta">
              Adventurer since {user ? new Date(user.createdAt).toLocaleDateString('en-US', {
                month: 'long', day: 'numeric', year: 'numeric'
              }) : ''}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
