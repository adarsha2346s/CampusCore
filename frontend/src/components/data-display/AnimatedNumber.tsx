import { useEffect, useRef } from 'react'

interface AnimatedNumberProps {
  value: number
  duration?: number
  precision?: number
  className?: string
  /** Rendered after the digits, outside the animated group (for example "%"). */
  suffix?: string
}

const reducedMotion = () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

function formatNumber(value: number, precision = 0) {
  const [whole, fraction] = value.toFixed(precision).split('.')
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return fraction ? `${grouped}.${fraction}` : grouped
}

/**
 * Counts up to the supplied value and renders every character as an animated
 * digit, matching the preview's KPI counter treatment. Assistive technology
 * receives the settled value only.
 */
export function AnimatedNumber({ value, duration = 620, precision = 0, className, suffix }: AnimatedNumberProps) {
  const visualRef = useRef<HTMLSpanElement>(null)
  const previousValue = useRef(0)

  useEffect(() => {
    const host = visualRef.current
    if (!host) return

    const from = previousValue.current
    const to = value
    const text = `${formatNumber(to, precision)}${suffix ?? ''}`

    if (reducedMotion() || from === to) {
      host.textContent = text
      previousValue.current = to
      return
    }

    const group = document.createElement('span')
    group.className = 't-digit-group is-animating'
    group.setAttribute('aria-hidden', 'true')
    for (const [index, character] of [...text].entries()) {
      const digit = document.createElement('span')
      digit.className = 't-digit'
      digit.style.setProperty('--s', String(index))
      digit.textContent = character
      group.append(digit)
    }
    host.replaceChildren(group)

    let frame = 0
    const startedAt = performance.now()
    const tick = (now: number) => {
      const progress = Math.min(1, (now - startedAt) / duration)
      const eased = 1 - (1 - progress) ** 4
      const current = from + (to - from) * eased
      const factor = 10 ** precision
      host.textContent = `${formatNumber(Math.round(current * factor) / factor, precision)}${suffix ?? ''}`
      if (progress < 1) frame = requestAnimationFrame(tick)
      else previousValue.current = to
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [duration, precision, suffix, value])

  return (
    <>
      <span className="sr-only">{formatNumber(value, precision)}{suffix ?? ''}</span>
      <span ref={visualRef} className={className} aria-hidden="true" />
    </>
  )
}