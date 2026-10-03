import type { ReactNode } from 'react'

export interface MetricStripItem {
  label: string
  value: ReactNode
  detail?: ReactNode
}

export function MetricStrip({ items, label }: { items: MetricStripItem[]; label: string }) {
  return (
    <section className="metric-strip" aria-label={label}>
      {items.map((item) => (
        <div className="metric-strip__item" key={item.label}>
          <span className="metric-strip__value">{item.value}</span>
          <span className="metric-strip__label">{item.label}</span>
          {item.detail && <span className="kpi__detail">{item.detail}</span>}
        </div>
      ))}
    </section>
  )
}
