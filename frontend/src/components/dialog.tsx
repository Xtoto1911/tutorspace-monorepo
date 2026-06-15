import {
  createContext,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Button } from './primitives';

type AlertOptions = { title?: string; message: string; confirmLabel?: string };
type ConfirmOptions = { title?: string; message: string; confirmLabel?: string; cancelLabel?: string };
type PromptOptions = {
  title?: string;
  message: string;
  defaultValue?: string;
  placeholder?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  inputType?: 'text' | 'number';
};

type DialogApi = {
  alert: (message: string, options?: Omit<AlertOptions, 'message'>) => Promise<void>;
  confirm: (message: string, options?: Omit<ConfirmOptions, 'message'>) => Promise<boolean>;
  prompt: (message: string, options?: Omit<PromptOptions, 'message'>) => Promise<string | null>;
};

type DialogState =
  | { kind: 'none' }
  | { kind: 'alert'; title: string; message: string; confirmLabel: string; resolve: () => void }
  | {
      kind: 'confirm';
      title: string;
      message: string;
      confirmLabel: string;
      cancelLabel: string;
      resolve: (value: boolean) => void;
    }
  | {
      kind: 'prompt';
      title: string;
      message: string;
      defaultValue: string;
      placeholder: string;
      confirmLabel: string;
      cancelLabel: string;
      inputType: 'text' | 'number';
      resolve: (value: string | null) => void;
    };

const DialogContext = createContext<DialogApi | null>(null);

export function DialogProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<DialogState>({ kind: 'none' });
  const inputRef = useRef<HTMLInputElement | null>(null);

  const api = useMemo<DialogApi>(
    () => ({
      alert: (message, options) =>
        new Promise<void>((resolve) => {
          setState({
            kind: 'alert',
            title: options?.title ?? 'Внимание',
            message,
            confirmLabel: options?.confirmLabel ?? 'Ок',
            resolve,
          });
        }),
      confirm: (message, options) =>
        new Promise<boolean>((resolve) => {
          setState({
            kind: 'confirm',
            title: options?.title ?? 'Подтвердите действие',
            message,
            confirmLabel: options?.confirmLabel ?? 'Подтвердить',
            cancelLabel: options?.cancelLabel ?? 'Отмена',
            resolve,
          });
        }),
      prompt: (message, options) =>
        new Promise<string | null>((resolve) => {
          setState({
            kind: 'prompt',
            title: options?.title ?? 'Введите значение',
            message,
            defaultValue: options?.defaultValue ?? '',
            placeholder: options?.placeholder ?? '',
            confirmLabel: options?.confirmLabel ?? 'Сохранить',
            cancelLabel: options?.cancelLabel ?? 'Отмена',
            inputType: options?.inputType ?? 'text',
            resolve,
          });
        }),
    }),
    [],
  );

  const handleConfirm = () => {
    if (state.kind === 'alert') {
      state.resolve();
      setState({ kind: 'none' });
    } else if (state.kind === 'confirm') {
      state.resolve(true);
      setState({ kind: 'none' });
    } else if (state.kind === 'prompt') {
      const value = inputRef.current?.value ?? '';
      state.resolve(value);
      setState({ kind: 'none' });
    }
  };

  const handleCancel = () => {
    if (state.kind === 'alert') {
      state.resolve();
    } else if (state.kind === 'confirm') {
      state.resolve(false);
    } else if (state.kind === 'prompt') {
      state.resolve(null);
    }
    setState({ kind: 'none' });
  };

  if (state.kind === 'none') {
    return <DialogContext.Provider value={api}>{children}</DialogContext.Provider>;
  }

  return (
    <DialogContext.Provider value={api}>
      {children}
      <div
        className="modal-backdrop"
        role="presentation"
        onClick={(event) => {
          if (event.target === event.currentTarget) {
            handleCancel();
          }
        }}
      >
        <div className="modal" role="dialog" aria-modal="true" aria-labelledby="tutor-dialog-title">
          <h3 id="tutor-dialog-title">{state.title}</h3>
          <p className="modal-hint">{state.message}</p>
          {state.kind === 'prompt' ? (
            <input
              ref={inputRef}
              className="modal-input"
              type={state.inputType}
              defaultValue={state.defaultValue}
              placeholder={state.placeholder}
              autoFocus
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault();
                  handleConfirm();
                }
              }}
            />
          ) : null}
          <div className="modal-actions">
            {state.kind !== 'alert' ? (
              <Button variant="ghost" onClick={handleCancel}>
                {state.cancelLabel}
              </Button>
            ) : null}
            <Button onClick={handleConfirm} autoFocus={state.kind === 'alert'}>
              {state.confirmLabel}
            </Button>
          </div>
        </div>
      </div>
    </DialogContext.Provider>
  );
}

export function useDialog(): DialogApi {
  const ctx = useContext(DialogContext);
  if (!ctx) {
    throw new Error('useDialog must be used within DialogProvider');
  }
  return ctx;
}
