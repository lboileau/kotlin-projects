import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Callout, Card, Heading, Text, TextField } from '@radix-ui/themes';
import { useAuth } from '../../auth/useAuth';
import { register, signIn } from '../../api/auth';
import { ApiError } from '../../api/http';
import { safeNext } from '../../lib/safeNext';
import './SignInPage.css';

/**
 * Sign in and register. If the server says the email needs to register
 * (404 NOT_FOUND or 403 REGISTRATION_REQUIRED), the same screen reveals
 * a name field and submits registration instead — branched on the
 * error's status/code, never on its message text.
 */
export function SignInPage() {
  const { user, isLoading, signIn: setSignedInUser } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [needsRegistration, setNeedsRegistration] = useState(searchParams.get('mode') === 'register');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const nextPath = safeNext(searchParams.get('next'));

  useEffect(() => {
    if (!isLoading && user) {
      navigate(nextPath, { replace: true });
    }
    // Only re-check when auth state settles — nextPath is derived from
    // the initial URL and shouldn't retrigger this redirect.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading, user]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (submitting) return;
    setError(null);

    if (!email.trim()) {
      setError('Enter your email.');
      return;
    }

    setSubmitting(true);
    try {
      if (needsRegistration) {
        if (!username.trim()) {
          setError('Enter your name.');
          setSubmitting(false);
          return;
        }
        const registered = await register(email.trim(), username.trim());
        setSignedInUser(registered);
      } else {
        const authed = await signIn(email.trim());
        setSignedInUser(authed);
      }
      navigate(nextPath, { replace: true });
    } catch (err) {
      if (err instanceof ApiError && (err.status === 404 || err.code === 'REGISTRATION_REQUIRED')) {
        setNeedsRegistration(true);
        setError(null);
      } else if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Something went wrong. Try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="sign-in-page">
      <Card size="4" className="sign-in-page__card">
        <Heading as="h1" size="6" align="center" className="sign-in-page__title">
          Meal Planner
        </Heading>
        <Text as="p" align="center" color="gray" size="2" className="sign-in-page__subtitle">
          {needsRegistration ? 'Tell us your name to finish signing up.' : 'Sign in with your email.'}
        </Text>

        <form onSubmit={handleSubmit} className="sign-in-page__form">
          <label className="sign-in-page__field">
            <Text as="span" size="2" weight="medium">
              Email
            </Text>
            <TextField.Root
              type="email"
              inputMode="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              size="3"
              required
            />
          </label>

          {needsRegistration && (
            <label className="sign-in-page__field">
              <Text as="span" size="2" weight="medium">
                Your name
              </Text>
              <TextField.Root
                type="text"
                autoComplete="name"
                placeholder="Jamie Rivers"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                size="3"
                required
              />
            </label>
          )}

          {error && (
            <Callout.Root color="red" variant="surface" size="1">
              <Callout.Text>{error}</Callout.Text>
            </Callout.Root>
          )}

          <Button type="submit" size="3" variant="solid" loading={submitting} className="sign-in-page__submit">
            {needsRegistration ? 'Create account' : 'Continue'}
          </Button>
        </form>
      </Card>
    </div>
  );
}
