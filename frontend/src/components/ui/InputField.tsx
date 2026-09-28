import type { ComponentProps } from 'react'

type InputFieldProps = ComponentProps<'input'> & {
  label: string
  error?: string
}

export function InputField({ label, error, id, name, ...props }: InputFieldProps) {
  const fieldId = id ?? name
  const errorId = fieldId ? `${fieldId}-error` : undefined
  return (
    <div className="field">
      <label className="field__label" htmlFor={fieldId}>{label}</label>
      <input
        id={fieldId}
        name={name}
        className={`input${error ? ' input--error' : ''}`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : props['aria-describedby']}
        {...props}
      />
      {error && <p id={errorId} className="field__error">{error}</p>}
    </div>
  )
}
