import type { CSSProperties, PropsWithChildren } from 'react'

export type BorderGlowProps = PropsWithChildren<{
  /** Elevation of the edge light. Kept subtle by design. */
  intensity?: 'low' | 'medium'
  /** Optional accent hue. Defaults to the neutral brand accent. */
  tone?: 'accent' | 'cool'
  as?: 'div' | 'section' | 'article' | 'aside'
  className?: string
  style?: CSSProperties
}>

/**
 * Restrained edge illumination for selected surfaces.
 *
 * Intent: a soft ambient light along the element edge, the way a screen bezel
 * catches room light. Deliberately NOT neon - no hue rotation, no rainbow stops,
 * no animated RGB sweep, no saturated bloom. The glow is a single low-opacity
 * cool tone built from the existing design tokens, so it stays inside the
 * CampusCore palette and reads as neutral in both light and dark themes.
 *
 * Applied selectively. Global use would turn every surface into a light source
 * and flatten the hierarchy, so this is opted into only where a surface needs to
 * read as the current focus: the active navigation item, the primary record
 * surface, and the mobile drawer.
 *
 * Respects `prefers-reduced-motion`: the transition is removed entirely.
 */
export function BorderGlow({
  intensity = 'low',
  tone = 'cool',
  as: Element = 'div',
  className = '',
  style,
  children,
}: BorderGlowProps) {
  return (
    <Element
      className={`border-glow border-glow--${intensity} border-glow--${tone} ${className}`.trim()}
      style={style}
    >
      {children}
    </Element>
  )
}
