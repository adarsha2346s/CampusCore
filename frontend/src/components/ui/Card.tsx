import { useCallback, useEffect, useRef } from 'react'
import type { HTMLAttributes, PropsWithChildren } from 'react'

type CardProps = PropsWithChildren<HTMLAttributes<HTMLElement>> & {
  as?: 'section' | 'article' | 'div'
}

/**
 * Surface primitive. Adds a staggered reveal on scroll and a pointer spotlight,
 * both of which are skipped for reduced-motion and coarse pointers.
 */
export function Card({ as: Element = 'section', className = '', children, ...props }: CardProps) {
  const cardRef = useRef<HTMLElement | null>(null)
  const setCardRef = useCallback((node: HTMLElement | null) => { cardRef.current = node }, [])

  useEffect(() => {
    const element = cardRef.current
    if (!element) return

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    let observer: IntersectionObserver | undefined
    if (!reducedMotion && 'IntersectionObserver' in window) {
      element.classList.add('motion-reveal')
      observer = new IntersectionObserver(([entry]) => {
        if (entry.isIntersecting) {
          element.classList.add('motion-reveal--visible')
          observer?.unobserve(element)
        }
      }, { threshold: 0.08, rootMargin: '0px 0px -24px 0px' })
      observer.observe(element)
    }

    const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)').matches
    if (!reducedMotion && finePointer) {
      element.classList.add('card--spotlight')
      let frame = 0
      const move = (event: PointerEvent) => {
        if (frame) cancelAnimationFrame(frame)
        frame = requestAnimationFrame(() => {
          const bounds = element.getBoundingClientRect()
          element.style.setProperty('--spotlight-x', `${event.clientX - bounds.left}px`)
          element.style.setProperty('--spotlight-y', `${event.clientY - bounds.top}px`)
        })
      }
      const leave = () => {
        if (frame) cancelAnimationFrame(frame)
        element.style.removeProperty('--spotlight-x')
        element.style.removeProperty('--spotlight-y')
      }
      element.addEventListener('pointermove', move, { passive: true })
      element.addEventListener('pointerleave', leave, { passive: true })
      return () => {
        observer?.disconnect()
        if (frame) cancelAnimationFrame(frame)
        element.removeEventListener('pointermove', move)
        element.removeEventListener('pointerleave', leave)
      }
    }

    return () => observer?.disconnect()
  }, [])

  return <Element ref={setCardRef} className={`card ${className}`.trim()} {...props}>{children}</Element>
}