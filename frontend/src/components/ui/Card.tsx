import type { HTMLAttributes, PropsWithChildren } from 'react'

type CardProps = PropsWithChildren<HTMLAttributes<HTMLElement>> & {
  as?: 'section' | 'article' | 'div'
}

export function Card({ as: Element = 'section', className = '', children, ...props }: CardProps) {
  return <Element className={`card ${className}`.trim()} {...props}>{children}</Element>
}
