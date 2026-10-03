import type { ReactNode } from 'react'
import { Card } from '../ui/Card'

interface BentoCardProps {
  /** Visual weight inside the bento grid. */
  size?: 'default' | 'big' | 'wide' | 'full'
  /** Uses the primary gradient treatment reserved for the highlighted KPI. */
  highlighted?: boolean
  title?: string
  hint?: string
  index?: number
  footer?: ReactNode
  className?: string
  children: ReactNode
  as?: 'section' | 'article' | 'div'
}

/**
 * Single bento tile. Owns the staggered entrance, pointer spotlight and the
 * highlighted ("hot") gradient variant.
 */
export function BentoCard({
  size = 'default',
  highlighted = false,
  title,
  hint,
  index = 0,
  footer,
  className = '',
  children,
  as = 'section',
}: BentoCardProps) {
  const cellClass = [
    'bento__cell',
    size === 'big' ? 'bento__cell--big' : '',
    size === 'wide' ? 'bento__cell--wide' : '',
    size === 'full' ? 'bento__cell--full' : '',
  ].filter(Boolean).join(' ')

  const classes = ['card', 'bento-card', highlighted ? 'bento-card--hot' : '', className].filter(Boolean).join(' ')

  return (
    <div className={cellClass}>
      <Card
        as={as}
        className={classes}
        style={{ '--i': index } as React.CSSProperties}
      >
        {title && <h2 className="bento-card__title">{title}</h2>}
        {hint && <p className="bento-card__hint">{hint}</p>}
        {children}
        {footer && <div className="bento-card__footer">{footer}</div>}
      </Card>
    </div>
  )
}