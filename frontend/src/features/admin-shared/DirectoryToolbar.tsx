import type { ReactNode } from 'react'
import { Search } from 'lucide-react'
import { InputField } from '../../components/ui/InputField'

export function DirectoryToolbar({ search, onSearch, searchLabel = 'Search records', filters, countLabel }: {
  search: string
  onSearch: (value: string) => void
  searchLabel?: string
  filters?: ReactNode
  countLabel?: string
}) {
  return (
    <div className="directory-toolbar">
      <div className="directory-toolbar__search"><Search size={17} aria-hidden="true" /><InputField label={searchLabel} type="search" value={search} onChange={(event) => onSearch(event.target.value)} placeholder="Search by name, code or keyword" /></div>
      {filters && <div className="directory-toolbar__filters">{filters}</div>}
      {countLabel && <p className="directory-toolbar__count">{countLabel}</p>}
    </div>
  )
}
