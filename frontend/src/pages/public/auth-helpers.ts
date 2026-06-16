const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const NAME_MIN_LENGTH = 2;

export type FieldValidation = { valid: boolean; message: string | null };

export function validateName(name: string): FieldValidation {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    return { valid: false, message: 'Введите имя' };
  }
  if (trimmed.length < NAME_MIN_LENGTH) {
    return {
      valid: false,
      message: `Имя должно содержать минимум ${NAME_MIN_LENGTH} символа`,
    };
  }
  return { valid: true, message: null };
}

export function validateEmail(email: string): FieldValidation {
  const trimmed = email.trim();
  if (trimmed.length === 0) {
    return { valid: false, message: 'Введите email' };
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    return { valid: false, message: 'Некорректный формат email' };
  }
  return { valid: true, message: null };
}

export function validatePassword(password: string, minLength: number): FieldValidation {
  if (password.length === 0) {
    return { valid: false, message: 'Введите пароль' };
  }
  if (password.length < minLength) {
    return { valid: false, message: `Минимум ${minLength} символов` };
  }
  return { valid: true, message: null };
}

export function validatePasswordMatch(password: string, confirmPassword: string): FieldValidation {
  if (confirmPassword.length === 0) {
    return { valid: false, message: 'Повторите пароль' };
  }
  if (password !== confirmPassword) {
    return { valid: false, message: 'Пароли не совпадают' };
  }
  return { valid: true, message: null };
}

export function validateSubjects(subjects: readonly string[]): FieldValidation {
  if (subjects.length === 0) {
    return { valid: false, message: 'Выберите хотя бы один предмет' };
  }
  return { valid: true, message: null };
}

export type RegistrationValues = {
  name: string;
  email: string;
  password: string;
  subjects: readonly string[];
};

export type RegistrationErrors = Record<keyof RegistrationValues, string | null>;

export function runRegistrationValidations(values: RegistrationValues): RegistrationErrors {
  return {
    name: validateName(values.name).message,
    email: validateEmail(values.email).message,
    password: validatePassword(values.password, 6).message,
    subjects: validateSubjects(values.subjects).message,
  };
}
