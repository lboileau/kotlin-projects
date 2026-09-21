import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Spinner, Text } from '@radix-ui/themes';
import { PageHeader } from '../../components/PageHeader';
import { QueryErrorState } from '../../components/QueryErrorState';
import { ApiError } from '../../api/http';
import { useAcceptInvite } from '../../queries/plans';
import { setSelectedPlanId } from '../../lib/selectedPlan';
import { toast } from '../../lib/toastStore';
import { router } from '../../router';
import './JoinPlanPage.css';

type JoinStatus = 'joining' | 'not-found' | 'error';

/**
 * /join/:token — accepts a share link and lands on the plan. Runs the
 * accept exactly once per token: `attemptedRef` tracks the last
 * `token:retryNonce` this instance has already fired, so React 18
 * StrictMode's dev-only double-invoke of this effect (mount, cleanup,
 * mount again — same component instance, so the ref survives) never
 * double-toasts or double-accepts. `retryNonce` is the only way to fire
 * it again deliberately, from the error state's Retry button.
 */
export function JoinPlanPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const acceptInvite = useAcceptInvite();
  const [status, setStatus] = useState<JoinStatus>('joining');
  const [retryNonce, setRetryNonce] = useState(0);
  const attemptedRef = useRef<string | null>(null);
  // The accept can take a moment; if the user navigates away before it
  // resolves, this unmounts. The handlers below must then not touch
  // component state. A successful join still goes to the plan either way.
  const mountedRef = useRef(true);
  useEffect(() => {
    // Set on every mount, not just via the ref's initial value: StrictMode
    // mounts, unmounts and remounts in development, and a cleanup-only
    // effect would leave this false for the whole life of the component.
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!token) return;
    const key = `${token}:${retryNonce}`;
    if (attemptedRef.current === key) return;
    attemptedRef.current = key;
    if (mountedRef.current) setStatus('joining');

    acceptInvite.mutate(token, {
      onSuccess: (result) => {
        // Harmless either way — a localStorage write, plus
        // useAcceptInvite's own onSuccess already invalidates
        // ['plans','mine'] unconditionally — so both happen regardless
        // of mount state; only the navigation/status update below needs
        // guarding.
        setSelectedPlanId(result.mealPlanId);
        const message = result.alreadyMember ? 'You already have this plan' : `You joined ${result.name}`;
        toast.info(message);
        // Opening a join link always ends on the plan that was joined (the
        // user's explicit requirement), even if this page has unmounted in
        // the meantime — hence the router instance as the fallback.
        if (mountedRef.current) {
          navigate(`/plans/${result.mealPlanId}`, { replace: true });
        } else {
          void router.navigate(`/plans/${result.mealPlanId}`);
        }
      },
      onError: (error: unknown) => {
        if (!mountedRef.current) {
          toast.error("Couldn't join this plan.");
          return;
        }
        setStatus(error instanceof ApiError && error.status === 404 ? 'not-found' : 'error');
      },
    });
    // Only re-run when the token or an explicit retry changes — acceptInvite
    // is a fresh mutation object every render and must not retrigger this.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, retryNonce]);

  function handleRetry() {
    setRetryNonce((n) => n + 1);
  }

  if (status === 'not-found') {
    return (
      <div className="join-plan-page">
        <PageHeader title="Join plan" />
        <div className="join-plan-page__message">
          <Text size="4" weight="medium">
            This link doesn&apos;t work
          </Text>
          <Text color="gray" size="2">
            The plan may have been deleted.
          </Text>
          <Button asChild size="3" variant="solid">
            <Link to="/plans">Back to Plans</Link>
          </Button>
        </div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="join-plan-page">
        <PageHeader title="Join plan" />
        <QueryErrorState message="Couldn't join this plan." onRetry={handleRetry} />
      </div>
    );
  }

  return (
    <div className="join-plan-page">
      <PageHeader title="Join plan" />
      <div className="join-plan-page__message" aria-busy="true" aria-label="Joining plan">
        <Spinner size="3" />
        <Text color="gray" size="2">
          Joining…
        </Text>
      </div>
    </div>
  );
}
