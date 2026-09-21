import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Spinner } from '@radix-ui/themes';
import { PageLoader } from './PageLoader';
import { areaForPath } from '../lib/areas';
import './RouteFallback.css';

/**
 * Suspense fallback for lazy routes. Delayed by ~150ms so a fast (cached
 * or small) chunk load never flashes anything — only a load that's
 * actually slow enough to notice shows the area's loading icon (or a plain
 * spinner outside the three tab areas).
 */
export function RouteFallback() {
  const [visible, setVisible] = useState(false);
  const { pathname } = useLocation();

  useEffect(() => {
    const timer = window.setTimeout(() => setVisible(true), 150);
    return () => window.clearTimeout(timer);
  }, []);

  if (!visible) return null;

  const area = areaForPath(pathname);
  if (area && area !== 'account') return <PageLoader area={area} label="Loading" />;

  return (
    <div className="route-fallback" role="status" aria-label="Loading">
      <Spinner size="3" />
    </div>
  );
}
