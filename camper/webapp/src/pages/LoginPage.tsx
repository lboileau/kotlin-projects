import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ParallaxBackground } from '../components/ParallaxBackground';
import './LoginPage.css';

export function LoginPage() {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  if (isAuthenticated) {
    navigate('/', { replace: true });
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const user = mode === 'login'
        ? await api.login(email)
        : await api.register(email, username || undefined);
      login(user);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <ParallaxBackground variant="night" />

      <div className="login-content">
        {/* Title card */}
        <div className="login-header">
          <div className="login-emblem">
            <svg width="64" height="64" viewBox="0 0 64 64">
              <polygon points="32,4 8,56 56,56" fill="none" stroke="var(--ember)" strokeWidth="2" />
              <polygon points="32,14 18,50 46,50" fill="none" stroke="var(--butter)" strokeWidth="1.5" opacity="0.6" />
              {/* Campfire icon */}
              <path d="M26,42 Q28,32 32,28 Q36,32 38,42" fill="var(--ember)" opacity="0.8" />
              <path d="M28,42 Q30,35 32,32 Q34,35 36,42" fill="var(--butter)" opacity="0.9" />
              {/* Stars */}
              <circle cx="20" cy="18" r="1.5" fill="var(--starlight)" />
              <circle cx="44" cy="22" r="1" fill="var(--starlight)" opacity="0.7" />
              <circle cx="14" cy="30" r="1" fill="var(--starlight)" opacity="0.5" />
            </svg>
          </div>
          <h1 className="login-title">Camper</h1>
          <p className="login-tagline">Plan your next great adventure</p>
        </div>

        {/* Form card */}
        <div className="login-card">
          <div className="login-card-header">
            <button
              className={`login-tab ${mode === 'login' ? 'login-tab--active' : ''}`}
              onClick={() => { setMode('login'); setError(''); }}
            >
              Sign In
            </button>
            <button
              className={`login-tab ${mode === 'register' ? 'login-tab--active' : ''}`}
              onClick={() => { setMode('register'); setError(''); }}
            >
              Register
            </button>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            <div className="login-field">
              <label className="login-label">Email</label>
              <input
                type="email"
                className="login-input"
                placeholder="adventurer@wildlands.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                autoFocus
              />
            </div>

            {mode === 'register' && (
              <div className="login-field login-field--appear">
                <label className="login-label">Trail Name <span className="login-optional">(optional)</span></label>
                <input
                  type="text"
                  className="login-input"
                  placeholder="What shall we call you?"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                />
              </div>
            )}

            {error && <p className="login-error">{error}</p>}

            <button type="submit" className="login-submit" disabled={loading || !email.trim()}>
              {loading ? (
                <span className="login-loading">Journeying forth...</span>
              ) : (
                <span>{mode === 'login' ? 'Begin Adventure' : 'Join the Expedition'}</span>
              )}
            </button>
          </form>

          <p className="login-footer">
            {mode === 'login' ? "New around here? " : "Already an adventurer? "}
            <button
              className="login-switch"
              onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }}
            >
              {mode === 'login' ? 'Register' : 'Sign in'}
            </button>
          </p>
        </div>

        {/* Decorative bottom */}
        <div className="login-decoration">
          <svg width="200" height="30" viewBox="0 0 200 30">
            <path d="M0,15 Q25,5 50,15 Q75,25 100,15 Q125,5 150,15 Q175,25 200,15" fill="none" stroke="var(--ember)" strokeWidth="1" opacity="0.3" />
            <circle cx="100" cy="15" r="3" fill="var(--ember)" opacity="0.4" />
          </svg>
        </div>
      </div>
    </div>
  );
}
