import { useEffect, useRef } from 'react'

const glyphs = 'abcdefghijklmnopqrstuvwxyz'

/**
 * Page title that scrambles into place, mirroring the preview's title reveal.
 * The glyphs are written straight to the DOM so the animation never triggers a
 * React render, and it resolves instantly under prefers-reduced-motion.
 */
export function ScrambleTitle({ text, className }: { text: string; className?: string }) {
  const titleRef = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    const element = titleRef.current
    if (!element) return

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      element.textContent = text
      return
    }

    let frame = 0
    const startedAt = performance.now()
    const step = (now: number) => {
      const progress = Math.min((now - startedAt) / 460, 1)
      const settled = Math.floor(progress * text.length)
      element.textContent = [...text]
        .map((character, index) => (character === ' ' || index < settled ? character : glyphs[Math.random() * 26 | 0]))
        .join('')
      if (progress < 1) frame = requestAnimationFrame(step)
    }
    frame = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frame)
  }, [text])

  return <h1 ref={titleRef} className={className} aria-label={text}>{text}</h1>
}
