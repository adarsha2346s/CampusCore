import { useEffect, useRef } from 'react'

/**
 * Topographic "data field" backdrop. Ported from the CampusCore preview: a
 * marching-squares contour render on canvas with a pointer-driven lens.
 * The canvas is decorative, pauses when off-screen and draws a single static
 * frame when the visitor prefers reduced motion.
 */

const GRID = 16
const MAX_HEIGHT = 2400
const SEGMENTS: Record<number, number[][]> = {
  1: [[3, 2]],
  2: [[2, 1]],
  3: [[3, 1]],
  4: [[0, 1]],
  5: [[0, 3], [2, 1]],
  6: [[0, 2]],
  7: [[0, 3]],
  8: [[0, 3]],
  9: [[0, 2]],
  10: [[0, 1], [3, 2]],
  11: [[0, 1]],
  12: [[3, 1]],
  13: [[2, 1]],
  14: [[3, 2]],
}

const FONT = '"JetBrains Mono", ui-monospace, monospace'

function readPalette() {
  const styles = getComputedStyle(document.documentElement)
  const read = (name: string, fallback: string) => styles.getPropertyValue(name).trim() || fallback
  return {
    muted: read('--mu', '#64748b'),
    accent: read('--pr2', '#4d7cff'),
    ink: read('--ink', '#0f172a'),
  }
}

export function FieldCanvas({ className = '' }: { className?: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const host = canvas?.parentElement
    if (!canvas || !host) return

    const context = canvas.getContext('2d')
    if (!context) return

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    let palette = readPalette()
    let width = 0
    let height = 0
    let columns = 0
    let rows = 0
    let values = new Float32Array(0)
    let frameHandle = 0
    let tick = 0
    let pointerX = -999
    let pointerY = -999
    let onScreen = true

    const fieldAt = (x: number, y: number, time: number) => {
      let value = Math.sin(x * 0.008 + time * 0.00012) * Math.cos(y * 0.009 - time * 0.0001) * 0.5
        + Math.sin((x + y) * 0.0045 + time * 0.00007) * 0.35
        + Math.sin(x * 0.016 - y * 0.011 + 1.7) * 0.15
      if (pointerX > -900) {
        const dx = x - pointerX
        const dy = y - pointerY
        const squared = dx * dx + dy * dy
        if (squared < 100_000) value += 1.1 * Math.exp(-squared / 33_800)
      }
      return value
    }

    const draw = (time: number) => {
      if (!width) return
      if (tick++ % 60 === 0) palette = readPalette()

      const nextColumns = Math.ceil(width / GRID) + 1
      const nextRows = Math.ceil(height / GRID) + 1
      if (!values.length || nextColumns !== columns || nextRows !== rows) {
        columns = nextColumns
        rows = nextRows
        values = new Float32Array(columns * rows)
      }
      for (let row = 0; row < rows; row += 1) {
        for (let column = 0; column < columns; column += 1) {
          values[row * columns + column] = fieldAt(column * GRID, row * GRID, time)
        }
      }

      const base = new Path2D()
      const major = new Path2D()
      const focus = new Path2D()
      for (let band = 0; band < 9; band += 1) {
        const level = -1 + band * 0.3
        const strong = band % 3 === 0
        for (let row = 0; row < rows - 1; row += 1) {
          for (let column = 0; column < columns - 1; column += 1) {
            const a = values[row * columns + column]
            const b = values[row * columns + column + 1]
            const c = values[(row + 1) * columns + column + 1]
            const d = values[(row + 1) * columns + column]
            const mask = (a > level ? 8 : 0) | (b > level ? 4 : 0) | (c > level ? 2 : 0) | (d > level ? 1 : 0)
            if (mask === 0 || mask === 15) continue
            const x0 = column * GRID
            const y0 = row * GRID
            const x1 = x0 + GRID
            const y1 = y0 + GRID
            const point = (edge: number): [number, number] => {
              if (edge === 0) return [x0 + ((level - a) / (b - a)) * GRID, y0]
              if (edge === 1) return [x1, y0 + ((level - b) / (c - b)) * GRID]
              if (edge === 2) return [x0 + ((level - d) / (c - d)) * GRID, y1]
              return [x0, y0 + ((level - a) / (d - a)) * GRID]
            }
            for (const [from, to] of SEGMENTS[mask] ?? []) {
              const start = point(from)
              const end = point(to)
              const midX = (start[0] + end[0]) / 2 - pointerX
              const midY = (start[1] + end[1]) / 2 - pointerY
              const path = midX * midX + midY * midY < 28_900 ? focus : strong ? major : base
              path.moveTo(start[0], start[1])
              path.lineTo(end[0], end[1])
            }
          }
        }
      }

      context.clearRect(0, 0, width, height)
      context.lineJoin = 'round'
      context.globalAlpha = 0.3
      context.lineWidth = 0.9
      context.strokeStyle = palette.muted
      context.stroke(base)
      context.globalAlpha = 0.45
      context.lineWidth = 1.6
      context.stroke(major)
      context.strokeStyle = palette.accent
      context.lineWidth = 1.5
      context.globalAlpha = 0.75
      context.stroke(focus)
      context.globalAlpha = 1

      if (pointerX > -900) {
        const elevation = (50 + fieldAt(pointerX, pointerY, time) * 40).toFixed(1)
        context.strokeStyle = palette.ink
        context.globalAlpha = 0.55
        context.lineWidth = 1
        context.beginPath()
        context.moveTo(pointerX - 9, pointerY)
        context.lineTo(pointerX + 9, pointerY)
        context.moveTo(pointerX, pointerY - 9)
        context.lineTo(pointerX, pointerY + 9)
        context.stroke()
        context.globalAlpha = 1
        context.fillStyle = palette.ink
        context.font = `500 11px ${FONT}`
        context.fillText(`El ${elevation} m`, pointerX + 13, pointerY - 12)
      }
    }

    const render = () => {
      if (reducedMotion || !onScreen) return
      draw(performance.now())
    }

    const loop = () => {
      render()
      frameHandle = requestAnimationFrame(loop)
    }

    const fit = () => {
      const ratio = Math.min(window.devicePixelRatio || 1, 2)
      const rect = host.getBoundingClientRect()
      width = rect.width
      height = Math.min(rect.height, MAX_HEIGHT)
      if (!width || !height) return
      canvas.width = Math.round(width * ratio)
      canvas.height = Math.round(height * ratio)
      canvas.style.width = `${width}px`
      canvas.style.height = `${height}px`
      context.setTransform(ratio, 0, 0, ratio, 0, 0)
      values = new Float32Array(0)
      if (reducedMotion) draw(0)
    }

    const handlePointerMove = (event: PointerEvent) => {
      const rect = host.getBoundingClientRect()
      pointerX = event.clientX - rect.left
      pointerY = event.clientY - rect.top
      if (reducedMotion) draw(0)
    }

    const handlePointerLeave = () => {
      pointerX = -999
      pointerY = -999
      if (reducedMotion) draw(0)
    }

    const themeObserver = new MutationObserver(() => {
      palette = readPalette()
      if (reducedMotion) draw(0)
    })
    themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'data-ds'] })

    const visibilityObserver = new IntersectionObserver(([entry]) => { onScreen = entry.isIntersecting })
    visibilityObserver.observe(host)

    const resizeObserver = new ResizeObserver(fit)
    resizeObserver.observe(host)
    host.addEventListener('pointermove', handlePointerMove, { passive: true })
    host.addEventListener('pointerleave', handlePointerLeave, { passive: true })
    fit()
    if (!reducedMotion) frameHandle = requestAnimationFrame(loop)

    return () => {
      cancelAnimationFrame(frameHandle)
      resizeObserver.disconnect()
      visibilityObserver.disconnect()
      themeObserver.disconnect()
      host.removeEventListener('pointermove', handlePointerMove)
      host.removeEventListener('pointerleave', handlePointerLeave)
    }
  }, [])

  return <canvas ref={canvasRef} className={`field-canvas ${className}`.trim()} aria-hidden="true" />
}