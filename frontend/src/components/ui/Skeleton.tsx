/** Preview-style shimmer skeleton block. */
export function Skeleton({ width, radius, className = '' }: { width?: string | number; radius?: string | number; className?: string }) {
  return <span className={`sk ${className}`.trim()} style={{ width, borderRadius: radius }} aria-hidden="true" />
}

/**
 * Table-shaped loading state. Mirrors the preview skeleton rows so directory
 * pages keep their layout while data is in flight.
 */
export function TableSkeleton({ rows = 6, columns = 5, label = 'Loading records' }: { rows?: number; columns?: number; label?: string }) {
  const widths = [62, 44, 26, 30, 52, 38]
  return (
    <div className="table-skeleton" role="status" aria-label={label}>
      {Array.from({ length: rows }, (_, row) => (
        <div className="table-skeleton__row" key={row} aria-hidden="true">
          {Array.from({ length: columns }, (_, column) => (
            <Skeleton
              key={column}
              width={`${widths[(row + column) % widths.length] + (row % 2 === 0 ? 0 : 6)}%`}
            />
          ))}
        </div>
      ))}
      <span className="sr-only" role="status">{label}…</span>
    </div>
  )
}