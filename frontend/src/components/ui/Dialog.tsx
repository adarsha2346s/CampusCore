import * as DialogPrimitive from '@radix-ui/react-dialog'
import type { PropsWithChildren, ReactNode } from 'react'
import { X } from 'lucide-react'

interface DialogProps extends PropsWithChildren {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  footer?: ReactNode
}

export function Dialog({ open, onOpenChange, title, description, footer, children }: DialogProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="dialog-overlay" />
        <DialogPrimitive.Content className="dialog-content">
          <div className="dialog-content__header">
            <div>
              <DialogPrimitive.Title className="dialog-content__title">{title}</DialogPrimitive.Title>
              {description && <DialogPrimitive.Description className="muted">{description}</DialogPrimitive.Description>}
            </div>
            <DialogPrimitive.Close className="icon-button" aria-label="Close dialog">
              <X size={18} aria-hidden="true" />
            </DialogPrimitive.Close>
          </div>
          <div className="dialog-content__body">{children}</div>
          {footer && <div className="dialog-content__footer">{footer}</div>}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
