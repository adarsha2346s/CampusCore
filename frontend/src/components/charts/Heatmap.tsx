export interface HeatCell {
  /** 0 – 100, or null when no classes were held. */
  value: number | null
  title: string
}

const DAY_INITIALS = ['M', 'T', 'W', 'T', 'F', 'S', 'S']

export function Heatmap({ cells, label, caption }: { cells: HeatCell[]; label: string; caption: string }) {
  return (
    <div>
      <div className="heat" role="img" aria-label={label}>
        {DAY_INITIALS.map((initial, index) => <span className="heat__day" key={`${initial}-${index}`}>{initial}</span>)}
        {cells.map((cell, index) => (
          <span
            className={`heat__cell${cell.value === null ? ' heat__cell--empty' : ''}`}
            key={index}
            style={{ '--level': cell.value ?? 0, '--i': index % 7 + Math.floor(index / 7) } as React.CSSProperties}
            title={cell.title}
          />
        ))}
      </div>
      <p className="heat-legend"><span>{caption}</span><span className="heat-legend__scale" aria-hidden="true" /></p>
    </div>
  )
}
