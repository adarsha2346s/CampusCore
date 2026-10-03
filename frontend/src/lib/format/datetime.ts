const dateFormatter = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
const dateTimeFormatter = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })

function parse(value?: string | null) {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

/** `28 Aug 2026` — null when the value is missing or unparseable. */
export function formatDate(value?: string | null): string | null {
  const parsed = parse(value)
  return parsed ? dateFormatter.format(parsed) : null
}

/** `28 Aug 2026, 20:44` — null when the value is missing or unparseable. */
export function formatDateTime(value?: string | null): string | null {
  const parsed = parse(value)
  return parsed ? dateTimeFormatter.format(parsed) : null
}