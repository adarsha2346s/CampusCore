import type { ReactNode } from 'react'
import { Dialog } from '../../components/ui/Dialog'

export function RecordDetailsDialog({ open, onOpenChange, title, description, fields, notice }: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  fields: { label: string; value: ReactNode }[]
  notice?: ReactNode
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={title} description={description}>
      {notice}
      <dl className="record-details">
        {fields.map(({ label, value }) => <div key={label}><dt>{label}</dt><dd>{value === null || value === undefined || value === '' ? <span className="muted">Not provided</span> : value}</dd></div>)}
      </dl>
    </Dialog>
  )
}
