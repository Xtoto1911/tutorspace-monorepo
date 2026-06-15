import { describe, expect, it } from 'vitest';
import {
  formatPrice,
  formatRating,
  parsePrice,
  parseRating,
  pluralizeTutors,
  PRICE_MAX_LEN,
  PRICE_MAX_VALUE,
  RATING_MAX,
  RATING_MAX_LEN,
  sanitizePriceInput,
  sanitizeRatingInput,
} from './catalog-filter-helpers';

describe('sanitizeRatingInput', () => {
  it('replaces RU comma with dot', () => {
    expect(sanitizeRatingInput('4,5')).toBe('4.5');
  });

  it('strips non-digit characters', () => {
    expect(sanitizeRatingInput('4a.b5')).toBe('4.5');
  });

  it('keeps at most one decimal point and merges additional dots', () => {
    expect(sanitizeRatingInput('4..5')).toBe('4.5');
  });

  it('limits to 2 decimal digits', () => {
    expect(sanitizeRatingInput('4.857')).toBe('4.85');
  });

  it('caps length to RATING_MAX_LEN', () => {
    expect(sanitizeRatingInput('12345.6')).toBe('1234');
  });
});

describe('sanitizePriceInput', () => {
  it('keeps only digits', () => {
    expect(sanitizePriceInput('1 500 ₽')).toBe('1500');
  });

  it('strips leading zeros and decimals', () => {
    expect(sanitizePriceInput('01500.99')).toBe('01500');
  });

  it('caps length to PRICE_MAX_LEN', () => {
    expect(sanitizePriceInput('1234567890')).toBe(PRICE_MAX_LEN === 5 ? '12345' : '1234567');
  });
});

describe('parseRating', () => {
  it('returns undefined for empty / sentinel values', () => {
    expect(parseRating('')).toBeUndefined();
    expect(parseRating('.')).toBeUndefined();
    expect(parseRating('-')).toBeUndefined();
  });

  it('parses comma-decimal values', () => {
    expect(parseRating('4,5')).toBe(4.5);
  });

  it('rounds to 2 decimal places', () => {
    expect(parseRating('4.857')).toBe(4.86);
  });

  it('clamps to RATING_MAX', () => {
    expect(parseRating('7.5')).toBe(RATING_MAX);
    expect(parseRating('5.5')).toBe(RATING_MAX);
  });

  it('clamps to 0 for negatives', () => {
    expect(parseRating('-1')).toBe(0);
  });

  it('treats "0", "0.0", "0.00" as 0', () => {
    expect(parseRating('0')).toBe(0);
    expect(parseRating('0.0')).toBe(0);
    expect(parseRating('0.00')).toBe(0);
  });

  it('returns undefined for fully non-numeric input', () => {
    expect(parseRating('abc')).toBeUndefined();
  });

  it('clamps scientific notation to RATING_MAX (sanitizer should strip "e" first)', () => {
    expect(parseRating('1e2')).toBe(5);
  });
});

describe('parsePrice', () => {
  it('returns undefined for empty string', () => {
    expect(parsePrice('')).toBeUndefined();
  });

  it('strips leading zeros', () => {
    expect(parsePrice('01500')).toBe(1500);
  });

  it('strips spaces and currency symbols', () => {
    expect(parsePrice('1 500')).toBe(1500);
  });

  it('concatenates digits when "." survives (sanitizer should remove "." first)', () => {
    expect(parsePrice('15.50')).toBe(1550);
  });

  it('caps at PRICE_MAX_VALUE', () => {
    expect(parsePrice('9999999')).toBe(PRICE_MAX_VALUE);
  });

  it('verifies PRICE_MAX_VALUE matches PRICE_MAX_LEN digit count', () => {
    expect(String(PRICE_MAX_VALUE).length).toBe(PRICE_MAX_LEN);
  });

  it('sanitizer strips the minus sign so negative input becomes positive', () => {
    expect(parsePrice('-100')).toBe(100);
  });

  it('strips scientific notation characters so the digits survive', () => {
    expect(parsePrice('1e5')).toBe(15);
  });

  it('returns undefined for fully non-numeric input', () => {
    expect(parsePrice('abc')).toBeUndefined();
  });
});

describe('formatRating / formatPrice', () => {
  it('renders rating as integer when value is whole (4.0 -> 4)', () => {
    expect(formatRating(4.0)).toBe('4');
    expect(formatRating(4)).toBe('4');
  });

  it('keeps the decimal for fractional rating', () => {
    expect(formatRating(4.5)).toBe('4.5');
  });

  it('keeps 0 visible in the input (user intent preserved)', () => {
    expect(formatRating(0)).toBe('0');
    expect(formatPrice(0)).toBe('0');
  });

  it('returns empty string for undefined (filter is off, field is empty)', () => {
    expect(formatRating(undefined)).toBe('');
    expect(formatPrice(undefined)).toBe('');
  });

  it('renders price as plain integer', () => {
    expect(formatPrice(1500)).toBe('1500');
  });
});

describe('pluralizeTutors', () => {
  it('uses singular form for 1', () => {
    expect(pluralizeTutors(1)).toBe('1 репетитор');
  });

  it('uses genitive singular for 2, 3, 4', () => {
    expect(pluralizeTutors(2)).toBe('2 репетитора');
    expect(pluralizeTutors(3)).toBe('3 репетитора');
    expect(pluralizeTutors(4)).toBe('4 репетитора');
  });

  it('uses genitive plural for 0, 5..20', () => {
    expect(pluralizeTutors(0)).toBe('0 репетиторов');
    expect(pluralizeTutors(5)).toBe('5 репетиторов');
    expect(pluralizeTutors(10)).toBe('10 репетиторов');
    expect(pluralizeTutors(11)).toBe('11 репетиторов');
    expect(pluralizeTutors(12)).toBe('12 репетиторов');
    expect(pluralizeTutors(13)).toBe('13 репетиторов');
    expect(pluralizeTutors(14)).toBe('14 репетиторов');
    expect(pluralizeTutors(15)).toBe('15 репетиторов');
    expect(pluralizeTutors(20)).toBe('20 репетиторов');
  });

  it('keeps correct form across hundreds', () => {
    expect(pluralizeTutors(101)).toBe('101 репетитор');
    expect(pluralizeTutors(102)).toBe('102 репетитора');
    expect(pluralizeTutors(105)).toBe('105 репетиторов');
    expect(pluralizeTutors(111)).toBe('111 репетиторов');
    expect(pluralizeTutors(121)).toBe('121 репетитор');
    expect(pluralizeTutors(122)).toBe('122 репетитора');
  });

  it('falls back to 0 form for null / undefined / NaN / negative', () => {
    expect(pluralizeTutors(null)).toBe('0 репетиторов');
    expect(pluralizeTutors(undefined)).toBe('0 репетиторов');
    expect(pluralizeTutors(Number.NaN)).toBe('0 репетиторов');
    expect(pluralizeTutors(-3)).toBe('0 репетиторов');
  });

  it('truncates fractional input', () => {
    expect(pluralizeTutors(2.9)).toBe('2 репетитора');
  });
});
