/** Inline attendance percentage with the preview's thin progress bar. */
export function AttendanceBar({ value, className = '' }: { value: number | null; className?: string }) {
  if (value === null || !Number.isFinite(value)) return <span className={className}>No records</span>
  const clamped = Math.min(100, Math.max(0, value))
  const tone = clamped < 80 ? ' bar--lo' : clamped < 90 ? ' bar--md' : ''
  return (
    <span className={className}>
      <span className={`bar${tone}`} aria-hidden="true"><i className="bar__fill" style={{ width: `${clamped}%` }} /></span>
      <span className="bar-value">{Math.round(clamped)}%</span>
    </span>
  )
}
