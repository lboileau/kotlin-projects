import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeftIcon, LightningBoltIcon } from '@radix-ui/react-icons';

interface WorkoutHeaderProps {
  title: string;
  /** Where the back button goes; omit it on the root screen. */
  backTo?: string;
  backLabel?: string;
  /** Controls at the right of the bar. */
  trailing?: ReactNode;
}

/**
 * The workout screens' static top, in the shape of the app's HeaderBar (icon chip, title, a
 * control at the right) but on the page's dark surface. The root screen shows the chip; deeper
 * screens show a back button naming where it goes, as PageHeader does.
 */
export function WorkoutHeader({ title, backTo, backLabel = 'Workout', trailing }: WorkoutHeaderProps) {
  return (
    <header className="workout-header">
      <div className="workout-header__leading">
        {backTo ? (
          <Link to={backTo} className="workout-header__back" aria-label={`Back to ${backLabel}`}>
            <ChevronLeftIcon />
            <span>{backLabel}</span>
          </Link>
        ) : (
          <>
            <span className="workout-header__chip" aria-hidden="true">
              <LightningBoltIcon />
            </span>
            <h1 className="workout-header__title">{title}</h1>
          </>
        )}
      </div>
      {trailing && <div className="workout-header__trailing">{trailing}</div>}
    </header>
  );
}
