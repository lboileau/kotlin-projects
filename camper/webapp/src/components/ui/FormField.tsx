import './ui.css';

interface FormFieldProps {
  label: string;
  children: React.ReactNode;
  className?: string;
}

export function FormField({ label, children, className }: FormFieldProps) {
  const classes = ['form-field', className].filter(Boolean).join(' ');
  return (
    <div className={classes}>
      <label className="form-field__label">{label}</label>
      {children}
    </div>
  );
}
