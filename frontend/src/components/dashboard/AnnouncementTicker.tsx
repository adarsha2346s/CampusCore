interface AnnouncementTickerProps {
  items: string[]
  label?: string
}

/**
 * Marquee of live, API-derived campus notices. The list is duplicated once so
 * the loop is seamless, pauses on hover/focus and never animates under
 * prefers-reduced-motion.
 */
export function AnnouncementTicker({ items, label = 'Campus notices' }: AnnouncementTickerProps) {
  if (items.length === 0) return null

  return (
    <section className="ticker-bar" aria-label={label}>
      <span className="ticker__label">{label}</span>
      <div className="ticker">
        <div className="ticker__track" aria-hidden="true">
          {[0, 1].map((copy) => (
            <span className="ticker__group" key={copy}>
              {items.map((item) => <span className="ticker__item" key={`${copy}-${item}`}>{item}</span>)}
            </span>
          ))}
        </div>
      </div>
      <ul className="sr-only">
        {items.map((item) => <li key={item}>{item}</li>)}
      </ul>
    </section>
  )
}