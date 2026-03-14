import { forwardRef } from 'react';
import './ui.css';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ error, className, ...props }, ref) => {
    const classes = ['ui-input', error ? 'ui-input--error' : '', className].filter(Boolean).join(' ');
    return (
      <>
        <input ref={ref} className={classes} {...props} />
        {error && <span className="ui-input__error">{error}</span>}
      </>
    );
  }
);

Input.displayName = 'Input';
