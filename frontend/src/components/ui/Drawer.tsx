import * as DialogPrimitive from '@radix-ui/react-dialog'
import type { PropsWithChildren, ReactNode } from 'react'
import { X } from 'lucide-react'

interface DrawerProps extends PropsWithChildren {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  footer?: ReactNode
  label?: string
}

/**
 * Right-hand slide-over used for record detail views (for example Student 360).
 * Radix handles the focus trap, escape handling and scroll locking.
 */
export function Drawer({ open, onOpenChange, title, description, footer, label, children }: DrawerProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="dialog-overlay" />
        <DialogPrimitive.Content className="drawer-content" aria-describedby={description ? undefined : label}>
          <div className="drawer-content__header">
            <div>
              <DialogPrimitive.Title className="drawer-content__title">{title}</DialogPrimitive.Title>
              {description && <DialogPrimitive.Description className="drawer-content__description">{description}</DialogPrimitive.Description>}
            </div>
            <DialogPrimitive.Close className="icon-button" aria-label="Close panel">
              <X size={19} aria-hidden="true" />
            </DialogPrimitive.Close>
          </div>
          <div className="drawer-content__body">{children}</div>
          {footer && <div className="drawer-content__footer">{footer}</div>}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}