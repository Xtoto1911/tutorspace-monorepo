export function formatCurrency(value: number) {
  return new Intl.NumberFormat('ru-RU').format(value) + ' ₽';
}

export function formatRating(value: number) {
  return value.toFixed(1).replace('.', ',');
}

export function formatDate(value: string | Date) {
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(typeof value === 'string' ? new Date(value) : value);
}

export function formatShortDate(value: string | Date) {
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'short',
  }).format(typeof value === 'string' ? new Date(value) : value);
}

