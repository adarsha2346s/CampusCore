import { Inbox } from 'lucide-react'

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="state-panel state-panel--empty">
      <span className="state-panel__icon"><Inbox size={22} aria-hidden="true" /></span>
      <h2>{title}</h2>
      <p>{description}</p>
    </div>
  )
}
