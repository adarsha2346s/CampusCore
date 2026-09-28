import type { ReactNode } from 'react'

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
}

export function ResourceTable<T>({ columns, rows, getRowKey, actions, caption }: ResourceTableProps<T>) {
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
            <tr key={getRowKey(row)}>
              {columns.map((column, index) => <td key={column.key} data-label={column.header} className={`${column.mobileHidden ? 'resource-table__optional' : ''}${index === 0 ? ' resource-table__primary' : ''}`.trim()}>{column.render(row)}</td>)}
              {actions && <td className="resource-table__actions">{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
