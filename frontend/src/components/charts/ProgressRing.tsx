interface ProgressRingProps {
  /** 0 – 1 */
  value: number
  label: string
  caption: string
}

export function ProgressRing({ value, label, caption }: ProgressRingProps) {
  const clamped = Math.min(1, Math.max(0, Number.isFinite(value) ? value : 0))
  return (
    <figure className="rings__item">
      <svg className="rings__chart" viewBox="0 0 90 90" role="img" aria-label={`${caption} ${label}`}>
        <circle className="rings__track" cx={45} cy={45} r={36} />
        <circle
          className="rings__value"
          pathLength={1}
          cx={45}
          cy={45}
          r={36}
          style={{ '--ring-value': clamped } as React.CSSProperties}
        />
      </svg>
      <figcaption className="rings__caption">
        <b>{caption}</b>
        {label}
      </figcaption>
    </figure>
  )
}
