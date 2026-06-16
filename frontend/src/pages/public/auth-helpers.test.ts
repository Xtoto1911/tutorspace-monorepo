import { describe, expect, it } from 'vitest';
import {
  runRegistrationValidations,
  validateEmail,
  validateName,
  validatePassword,
  validatePasswordMatch,
  validateSubjects,
} from './auth-helpers';

describe('validateName', () => {
  it('rejects an empty name', () => {
    expect(validateName('')).toEqual({ valid: false, message: 'Введите имя' });
  });

  it('rejects a whitespace-only name', () => {
    expect(validateName('   ')).toEqual({ valid: false, message: 'Введите имя' });
  });

  it('rejects a name with one character', () => {
    expect(validateName('A')).toEqual({
      valid: false,
      message: 'Имя должно содержать минимум 2 символа',
    });
  });

  it('accepts a two-character name', () => {
    expect(validateName('Ан')).toEqual({ valid: true, message: null });
  });

  it('accepts a long name with surrounding spaces after trim', () => {
    expect(validateName('  Иван Петров  ')).toEqual({ valid: true, message: null });
  });
});

describe('validateEmail', () => {
  it('rejects an empty email', () => {
    expect(validateEmail('')).toEqual({ valid: false, message: 'Введите email' });
  });

  it('rejects a malformed email', () => {
    expect(validateEmail('user@')).toEqual({
      valid: false,
      message: 'Некорректный формат email',
    });
    expect(validateEmail('user@host')).toEqual({
      valid: false,
      message: 'Некорректный формат email',
    });
    expect(validateEmail('plainstring')).toEqual({
      valid: false,
      message: 'Некорректный формат email',
    });
  });

  it('accepts a standard email', () => {
    expect(validateEmail('user@example.com')).toEqual({ valid: true, message: null });
  });

  it('accepts a plus-suffix email', () => {
    expect(validateEmail('user+tag@example.co.uk')).toEqual({ valid: true, message: null });
  });
});

describe('validatePassword', () => {
  it('rejects an empty password', () => {
    expect(validatePassword('', 6)).toEqual({ valid: false, message: 'Введите пароль' });
  });

  it('rejects a password shorter than minLength', () => {
    expect(validatePassword('12345', 6)).toEqual({
      valid: false,
      message: 'Минимум 6 символов',
    });
    expect(validatePassword('1234567', 8)).toEqual({
      valid: false,
      message: 'Минимум 8 символов',
    });
  });

  it('accepts a password at exactly minLength', () => {
    expect(validatePassword('123456', 6)).toEqual({ valid: true, message: null });
    expect(validatePassword('12345678', 8)).toEqual({ valid: true, message: null });
  });
});

describe('validatePasswordMatch', () => {
  it('rejects when confirm is empty', () => {
    expect(validatePasswordMatch('123456', '')).toEqual({
      valid: false,
      message: 'Повторите пароль',
    });
  });

  it('rejects when values differ', () => {
    expect(validatePasswordMatch('123456', '123457')).toEqual({
      valid: false,
      message: 'Пароли не совпадают',
    });
  });

  it('accepts identical values', () => {
    expect(validatePasswordMatch('123456', '123456')).toEqual({
      valid: true,
      message: null,
    });
  });
});

describe('validateSubjects', () => {
  it('rejects an empty selection', () => {
    expect(validateSubjects([])).toEqual({
      valid: false,
      message: 'Выберите хотя бы один предмет',
    });
  });

  it('accepts a non-empty selection', () => {
    expect(validateSubjects(['Математика'])).toEqual({ valid: true, message: null });
  });
});

describe('runRegistrationValidations', () => {
  it('reports errors for all invalid fields and leaves valid fields empty', () => {
    const result = runRegistrationValidations({
      name: 'A',
      email: 'broken',
      password: '12345',
      subjects: [],
    });
    expect(result.name).toBeTruthy();
    expect(result.email).toBeTruthy();
    expect(result.password).toBeTruthy();
    expect(result.subjects).toBeTruthy();
  });

  it('returns nulls for every field when the values are all valid', () => {
    const result = runRegistrationValidations({
      name: 'Иван',
      email: 'i@example.com',
      password: '123456',
      subjects: ['Математика'],
    });
    expect(result).toEqual({ name: null, email: null, password: null, subjects: null });
  });
});
