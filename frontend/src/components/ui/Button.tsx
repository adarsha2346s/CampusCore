import { cloneElement, isValidElement } from 'react'
import type { ButtonHTMLAttributes, ReactElement, ReactNode, Ref } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md'
  asChild?: boolean
  ref?: Ref<HTMLButtonElement>
  children: ReactNode
}

export function Button({
  variant = 'primary',
  size = 'md',
  asChild = false,
  ref,
  className = '',
  children,
  ...props
}: ButtonProps) {
  const classes = `button button--${variant} button--${size} ${className}`.trim()
  if (asChild && isValidElement<{ className?: string }>(children)) {
    // Links can receive the shared button treatment without adding another dependency.
    const child = children as ReactElement<{ className?: string }>
    return cloneElement(child, {
      className: `${classes} ${child.props.className ?? ''}`.trim(),
    })
  }
  return <button ref={ref} className={classes} {...props}>{children}</button>
}
