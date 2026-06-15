export const RATING_MAX_LEN = 4;
export const PRICE_MAX_LEN = 5;
export const PRICE_MAX_VALUE = 99999;
export const RATING_MAX = 5;

export function sanitizeRatingInput(raw: string): string {
  const normalized = raw.replace(',', '.').replace(/[^\d.]/g, '').slice(0, RATING_MAX_LEN);
  const firstDot = normalized.indexOf('.');
  if (firstDot === -1) {
    return normalized;
  }
  const intPart = normalized.slice(0, firstDot);
  const fractionPart = normalized.slice(firstDot + 1).replace(/\./g, '').slice(0, 2);
  return `${intPart}.${fractionPart}`;
}

export function sanitizePriceInput(raw: string): string {
  return raw.replace(/[^\d]/g, '').slice(0, PRICE_MAX_LEN);
}

export function parseRating(raw: string): number | undefined {
  const trimmed = raw.trim();
  if (trimmed === '' || trimmed === '.' || trimmed === '-') {
    return undefined;
  }
  const num = Number(trimmed.replace(',', '.'));
  if (!Number.isFinite(num)) {
    return undefined;
  }
  const clamped = Math.max(0, Math.min(RATING_MAX, num));
  return Math.round(clamped * 100) / 100;
}

export function parsePrice(raw: string): number | undefined {
  const digits = raw.replace(/[^\d]/g, '').replace(/^0+(?=\d)/, '');
  if (digits === '') {
    return undefined;
  }
  const num = Number(digits);
  if (!Number.isFinite(num)) {
    return undefined;
  }
  return Math.max(0, Math.min(PRICE_MAX_VALUE, Math.trunc(num)));
}

export function formatRating(value: number | undefined): string {
  if (value === undefined) {
    return '';
  }
  return Number.isInteger(value) ? String(value) : String(value);
}

export function formatPrice(value: number | undefined): string {
  if (value === undefined) {
    return '';
  }
  return String(value);
}

const TUTOR_FORMS = ['репетитор', 'репетитора', 'репетиторов'] as const;

export function pluralizeTutors(count: number | null | undefined): string {
  const safe = Number.isFinite(count) && count !== null ? Math.max(0, Math.trunc(count as number)) : 0;
  const mod100 = safe % 100;
  const mod10 = safe % 10;
  let form: (typeof TUTOR_FORMS)[number];
  if (mod100 >= 11 && mod100 <= 14) {
    form = TUTOR_FORMS[2];
  } else if (mod10 === 1) {
    form = TUTOR_FORMS[0];
  } else if (mod10 >= 2 && mod10 <= 4) {
    form = TUTOR_FORMS[1];
  } else {
    form = TUTOR_FORMS[2];
  }
  return `${safe} ${form}`;
}
