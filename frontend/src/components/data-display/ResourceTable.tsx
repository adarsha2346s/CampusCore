import type { KeyboardEvent, ReactNode } from 'react'

export interface ResourceColumn<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  mobileHidden?: boolean
}

interface ResourceTableProps<T> {
  columns: ResourceColumn<T>[]
  rows: T[]
  getRowKey: (row: T) => string | number
  actions?: (row: T) => ReactNode
  caption: string
  /** Makes a whole row act as the primary control for the record. */
  onRowActivate?: (row: T) => void
}

export function ResourceTable<T>({ columns, rows, getRowKey, actions, caption, onRowActivate }: ResourceTableProps<T>) {
  const activate = (row: T) => {
    if (!onRowActivate) return
    onRowActivate(row)
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLTableRowElement>, row: T) => {
    if (!onRowActivate || event.target !== event.currentTarget) return
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      activate(row)
    }
  }

  return (
    <div className="resource-table-scroll">
      <table className="resource-table">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr>
            {columns.map((column) => <th key={column.key} className={column.mobileHidden ? 'resource-table__optional' : undefined} scope="col">{column.header}</th>)}
            {actions && <th scope="col"><span className="sr-only">Actions</span></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={getRowKey(row)}
              className={onRowActivate ? 'is-clickable' : undefined}
              tabIndex={onRowActivate ? 0 : undefined}
              onClick={onRowActivate ? () => activate(row) : undefined}
              onKeyDown={onRowActivate ? (event) => handleKeyDown(event, row) : undefined}
            >
              {columns.map((column, index) => <td key={column.key} data-label={column.header} className={`${column.mobileHidden ? 'resource-table__optional' : ''}${index === 0 ? ' resource-table__primary' : ''}`.trim()}>{column.render(row)}</td>)}
              {actions && <td className="resource-table__actions">{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}