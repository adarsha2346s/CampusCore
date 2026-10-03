import type { ReactNode } from 'react'

export interface LegalSection {
  id: string
  heading: string
  body: ReactNode
}

/**
 * Shared shell for the public legal documents. Reuses the CampusCore surface
 * tokens, typography and motion conventions rather than introducing a new
 * visual language.
 */
export function LegalDocument({
  title,
  summary,
  sections,
  updated = 'October 2026',
}: {
  title: string
  summary: ReactNode
  sections: LegalSection[]
  updated?: string
}) {
  return (
    <article className="legal-doc">
      <header className="legal-doc__header">
        <p className="page-header__eyebrow">Legal</p>
        <h1 className="legal-doc__title">{title}</h1>
        <p className="legal-doc__summary">{summary}</p>
        <p className="legal-doc__updated">Last updated: {updated}</p>
      </header>

      <nav className="legal-doc__toc" aria-label={`${title} contents`}>
        <p className="legal-doc__toc-title">Contents</p>
        <ol className="legal-doc__toc-list">
          {sections.map((section, index) => (
            <li key={section.id}>
              <a href={`#${section.id}`}>{`${index + 1}. ${section.heading}`}</a>
            </li>
          ))}
        </ol>
      </nav>

      <div className="legal-doc__body">
        {sections.map((section) => (
          <section className="legal-doc__section" key={section.id} id={section.id}>
            <h2 className="legal-doc__heading">{section.heading}</h2>
            {section.body}
          </section>
        ))}
      </div>
    </article>
  )
}

/** Paragraph helper so every clause keeps the same measure and rhythm. */
export function LegalParagraph({ children }: { children: ReactNode }) {
  return <p className="legal-doc__p">{children}</p>
}

/** Unordered clause list used for enumerations inside a section. */
export function LegalList({ children }: { children: ReactNode }) {
  return <ul className="legal-doc__list">{children}</ul>
}

export function LegalListItem({ children }: { children: ReactNode }) {
  return <li className="legal-doc__item">{children}</li>
}