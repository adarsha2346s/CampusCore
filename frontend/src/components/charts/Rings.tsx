import type { ReactNode } from 'react'

export function Rings({ label, layout = 'flex', children }: { label: string; layout?: 'flex' | 'grid'; children: ReactNode }) {
  return <div className={`rings${layout === 'grid' ? ' rings--grid' : ''}`} role="group" aria-label={label}>{children}</div>
}
