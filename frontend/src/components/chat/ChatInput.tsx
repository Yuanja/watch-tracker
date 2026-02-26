import {
  useState,
  useCallback,
  useRef,
  type KeyboardEvent,
  type FormEvent,
} from 'react';
import { Send } from 'lucide-react';
import { clsx } from 'clsx';
import { LoadingSpinner } from '../common/LoadingSpinner';

interface ChatInputProps {
  onSend: (content: string) => void;
  disabled: boolean;
}

export function ChatInput({ onSend, disabled }: ChatInputProps) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = useCallback(
    (e?: FormEvent) => {
      e?.preventDefault();
      const trimmed = value.trim();
      if (!trimmed || disabled) return;
      onSend(trimmed);
      setValue('');
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    },
    [value, disabled, onSend]
  );

  const handleKeyDown = useCallback(
    (e: KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSubmit();
      }
    },
    [handleSubmit]
  );

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setValue(e.target.value);
      const el = e.target;
      el.style.height = 'auto';
      el.style.height = `${Math.min(el.scrollHeight, 144)}px`;
    },
    []
  );

  return (
    <form
      onSubmit={handleSubmit}
      className="flex items-end gap-2 border-t border-gray-200 bg-white px-4 py-3"
    >
      <textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        rows={1}
        placeholder="Ask a question... (Enter to send, Shift+Enter for newline)"
        disabled={disabled}
        aria-label="Message input"
        className={clsx(
          'flex-1 resize-none rounded-xl border border-gray-200 bg-gray-50 px-4 py-2.5',
          'text-sm text-gray-900 placeholder-gray-400 outline-none',
          'transition-colors focus:border-blue-400 focus:bg-white focus:ring-2 focus:ring-blue-100',
          'disabled:cursor-not-allowed disabled:opacity-60',
          'min-h-[42px] max-h-36 leading-relaxed'
        )}
      />
      <button
        type="submit"
        disabled={disabled || !value.trim()}
        aria-label="Send message"
        className={clsx(
          'flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition-colors',
          'bg-blue-600 text-white hover:bg-blue-700',
          'disabled:cursor-not-allowed disabled:opacity-40',
          'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1'
        )}
      >
        {disabled ? (
          <LoadingSpinner size="sm" className="border-white border-t-blue-300" />
        ) : (
          <Send className="h-4 w-4" aria-hidden="true" />
        )}
      </button>
    </form>
  );
}
