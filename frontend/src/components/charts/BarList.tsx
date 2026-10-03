export interface BarDatum {
  label: string
  meta?: string
  value: number
}

export function BarList({ items, label, caption }: { items: BarDatum[]; label: string; caption: string }) {
  const max = Math.max(1, ...items.map((item) => item.value))
  return (
    <div className="bar-list" role="group" aria-label={label}>
      {items.map((item, index) => (
        <div className="bar-list__row" key={`${item.label}-${index}`}>
          <span className="bar-list__label">
            {item.label}
            {item.meta && <small>{item.meta}</small>}
          </span>
          <span className="bar-list__track">
            <i className="bar-list__fill" style={{ '--width': `${Math.round((item.value / max) * 100)}%`, '--i': index } as React.CSSProperties} />
          </span>
          <b className="bar-list__value">{item.value.toLocaleString()}</b>
        </div>
      ))}
      <p className="form-hint">{caption}</p>
    </div>
  )
}
