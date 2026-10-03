interface SparklineProps {
  values: number[]
  label: string
  className?: string
}

/**
 * Minimal SVG trend line. The path is revealed with a stroke-dash draw so the
 * shape reads as being plotted rather than pasted in.
 */
export function Sparkline({ values, label, className = '' }: SparklineProps) {
  const points = values.length > 1 ? values : values.length === 1 ? [values[0], values[0]] : []
  if (points.length < 2) return null
  const min = Math.min(...points)
  const max = Math.max(...points)
  const span = max - min || 1
  const path = points
    .map((value, index) => {
      const x = (index / (points.length - 1)) * 100
      const y = 20 - ((value - min) / span) * 17
      return `${index === 0 ? 'M' : 'L'}${x.toFixed(1)} ${y.toFixed(1)}`
    })
    .join(' ')

  return (
    <svg className={`spark ${className}`.trim()} viewBox="0 0 100 22" role="img" aria-label={label} preserveAspectRatio="none">
      <path pathLength={1} d={path} />
    </svg>
  )
}
