import { useEffect, useState } from 'react';
import { Spinner } from '@radix-ui/themes';
import './RouteFallback.css';

/**
 * Suspense fallback for lazy routes. Delayed by ~150ms so a fast (cached
 * or small) chunk load never flashes a spinner — only a load that's
 * actually slow enough to notice shows one.
 */
export function RouteFallback() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setVisible(true), 150);
    return () => window.clearTimeout(timer);
  }, []);

  if (!visible) return null;

  return (
    <div className="route-fallback" role="status" aria-label="Loading">
      <Spinner size="3" />
    </div>
  );
}
