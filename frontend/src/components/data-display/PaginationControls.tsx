import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '../ui/Button'

interface PaginationControlsProps {
  page: number
  size: number
  totalElements: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function PaginationControls({ page, size, totalElements, totalPages, onPageChange }: PaginationControlsProps) {
  if (totalPages <= 1) return null
  const firstItem = page * size + 1
  const lastItem = Math.min((page + 1) * size, totalElements)

  return (
    <nav className="pagination-controls" aria-label="Table pagination">
      <p aria-live="polite">Showing {firstItem}–{lastItem} of {totalElements}</p>
      <div className="pagination-controls__actions">
        <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => onPageChange(page - 1)} aria-label="Go to previous page">
          <ChevronLeft size={16} aria-hidden="true" /> Previous
        </Button>
        <span aria-current="page">Page {page + 1} of {totalPages}</span>
        <Button size="sm" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)} aria-label="Go to next page">
          Next <ChevronRight size={16} aria-hidden="true" />
        </Button>
      </div>
    </nav>
  )
}
