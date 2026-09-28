import type { ComponentProps } from 'react'

type SelectFieldProps = ComponentProps<'select'> & {
  label: string
  error?: string
  children: React.ReactNode
}

export function SelectField({ label, error, id, name, children, ...props }: SelectFieldProps) {
  const fieldId = id ?? name
  const errorId = fieldId ? `${fieldId}-error` : undefined
  return (
    <div className="field">
      <label className="field__label" htmlFor={fieldId}>{label}</label>
      <select id={fieldId} name={name} className={`input select${error ? ' input--error' : ''}`} aria-invalid={Boolean(error)} aria-describedby={error ? errorId : props['aria-describedby']} {...props}>
        {children}
      </select>
      {error && <p id={errorId} className="field__error">{error}</p>}
    </div>
  )
}
