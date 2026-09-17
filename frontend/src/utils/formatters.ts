export function formatCurrency(amountCents: number, currency = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency.toUpperCase(),
    minimumFractionDigits: 2,
  }).format(amountCents / 100);
}

export function formatDate(dateStringOrInstant?: string | null): string {
  if (!dateStringOrInstant) return '—';
  try {
    const date = new Date(dateStringOrInstant);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(date);
  } catch {
    return dateStringOrInstant;
  }
}

export function formatDateTime(dateStringOrInstant?: string | null): string {
  if (!dateStringOrInstant) return '—';
  try {
    const date = new Date(dateStringOrInstant);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    }).format(date);
  } catch {
    return dateStringOrInstant;
  }
}
