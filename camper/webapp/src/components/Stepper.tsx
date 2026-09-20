import { MinusIcon, PlusIcon } from '@radix-ui/react-icons';
import './Stepper.css';

interface StepperProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  ariaLabel: string;
}

/** A small +/- stepper (servings, quantities). 44px touch targets on both buttons. */
export function Stepper({ value, onChange, min = 1, max, ariaLabel }: StepperProps) {
  const atMin = value <= min;
  const atMax = max !== undefined && value >= max;

  return (
    <div className="stepper" role="group" aria-label={ariaLabel}>
      <button
        type="button"
        className="stepper__button"
        aria-label="Decrease"
        disabled={atMin}
        onClick={() => onChange(Math.max(min, value - 1))}
      >
        <MinusIcon />
      </button>
      <span className="stepper__value" aria-live="polite">
        {value}
      </span>
      <button
        type="button"
        className="stepper__button"
        aria-label="Increase"
        disabled={atMax}
        onClick={() => onChange(max !== undefined ? Math.min(max, value + 1) : value + 1)}
      >
        <PlusIcon />
      </button>
    </div>
  );
}
